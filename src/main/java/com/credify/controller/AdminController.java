package com.credify.controller;

import com.credify.model.Badge;
import com.credify.model.Report;
import com.credify.model.Review;
import com.credify.model.User;
import com.credify.model.UserBadge;
import com.credify.repository.BadgeRepository;
import com.credify.repository.ReportRepository;
import com.credify.repository.ReviewRepository;
import com.credify.repository.UserBadgeRepository;
import com.credify.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final ReviewRepository reviews;
    private final ReportRepository reports;
    private final UserRepository users;
    private final BadgeRepository badges;
    private final UserBadgeRepository userBadges;

    public AdminController(
            ReviewRepository reviews,
            ReportRepository reports,
            UserRepository users,
            BadgeRepository badges,
            UserBadgeRepository userBadges) {
        this.reviews = reviews;
        this.reports = reports;
        this.users = users;
        this.badges = badges;
        this.userBadges = userBadges;
    }

    public record BadgeRequest(
            @NotBlank @Size(max = 80) String badgeName,
            @NotBlank @Size(max = 300) String description,
            @NotBlank @Size(max = 500) String earningCriteria) {}

    /** The JWT filter requires the ADMIN role for every /api/admin/** route. */
    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() {
        return Map.of(
                "totalUsers", users.count(),
                "totalReviews", reviews.count(),
                "publishedReviews", reviews.countByStatus("PUBLISHED"),
                "pendingReviews", reviews.countByStatus("PENDING"),
                "openReports", reports.countByStatus("OPEN"));
    }

    @GetMapping("/reviews")
    public List<Review> reviewQueue() {
        return reviews.findAll().stream()
                .filter(review -> "PENDING".equals(review.status)
                        || "NEEDS_REVIEW".equals(review.authenticityStatus))
                .sorted((left, right) -> right.createdAt.compareTo(left.createdAt))
                .toList();
    }

    @PatchMapping("/reviews/{id}")
    public Review moderateReview(@PathVariable String id, @RequestParam String decision) {
        if (!List.of("PUBLISHED", "HIDDEN", "REMOVED").contains(decision)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Decision must be PUBLISHED, HIDDEN, or REMOVED");
        }
        Review review = reviews.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found"));
        review.status = decision;
        review.updatedAt = Instant.now();
        return reviews.save(review);
    }

    @GetMapping("/reports")
    public List<Report> reportQueue(@RequestParam(defaultValue = "OPEN") String status) {
        if (!List.of("OPEN", "REVIEWED", "DISMISSED").contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported report status");
        }
        return reports.findByStatusOrderByCreatedAtDesc(status);
    }

    @PatchMapping("/reports/{id}")
    public Report resolveReport(@PathVariable String id, @RequestParam String decision) {
        if (!List.of("REVIEWED", "DISMISSED").contains(decision)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Decision must be REVIEWED or DISMISSED");
        }
        Report report = reports.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));
        report.status = decision;
        report.updatedAt = Instant.now();
        return reports.save(report);
    }

    @GetMapping("/users")
    public List<Map<String, Object>> users() {
        return users.findAll().stream().map(this::safeUser).toList();
    }

    @PatchMapping("/users/{id}/status")
    public Map<String, Object> updateUserStatus(@PathVariable String id, @RequestParam String status) {
        if (!List.of("ACTIVE", "SUSPENDED").contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status must be ACTIVE or SUSPENDED");
        }
        User user = users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if ("ADMIN".equals(user.role)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Admin accounts cannot be suspended here");
        }
        user.accountStatus = status;
        return safeUser(users.save(user));
    }

    @GetMapping("/badges")
    public List<Badge> badges() {
        return badges.findAll();
    }

    @PostMapping("/badges")
    public Badge createBadge(@Valid @RequestBody BadgeRequest request) {
        String name = request.badgeName().trim();
        if (badges.existsByBadgeNameIgnoreCase(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Badge name already exists");
        }
        Badge badge = new Badge();
        badge.badgeName = name;
        badge.description = request.description().trim();
        badge.earningCriteria = request.earningCriteria().trim();
        try {
            return badges.save(badge);
        } catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Badge name already exists");
        }
    }

    @PostMapping("/users/{userId}/badges/{badgeId}")
    public Map<String, Object> awardBadge(@PathVariable String userId, @PathVariable String badgeId) {
        if (!users.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        Badge badge = badges.findById(badgeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Badge not found"));
        if (!userBadges.existsByUserIdAndBadgeId(userId, badgeId)) {
            UserBadge award = new UserBadge();
            award.userId = userId;
            award.badgeId = badgeId;
            award.awardedAt = Instant.now();
            try {
                userBadges.save(award);
            } catch (DuplicateKeyException ignored) {
                // A repeated admin click must not award a duplicate badge.
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("badgeId", badge.id);
        result.put("badgeName", badge.badgeName);
        result.put("awarded", true);
        return result;
    }

    private Map<String, Object> safeUser(User user) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", user.id);
        result.put("email", user.email);
        result.put("firstName", user.firstName);
        result.put("lastName", user.lastName);
        result.put("role", user.role);
        result.put("accountStatus", user.accountStatus);
        result.put("reputationScore", user.reputationScore);
        result.put("createdAt", user.createdAt);
        return result;
    }
}