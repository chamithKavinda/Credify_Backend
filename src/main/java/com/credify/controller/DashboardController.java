package com.credify.controller;

import com.credify.model.Badge;
import com.credify.model.Review;
import com.credify.model.User;
import com.credify.model.UserBadge;
import com.credify.repository.BadgeRepository;
import com.credify.repository.ReviewRepository;
import com.credify.repository.UserBadgeRepository;
import com.credify.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/users")
public class DashboardController {
    private final UserRepository users;
    private final ReviewRepository reviews;
    private final UserBadgeRepository userBadges;
    private final BadgeRepository badges;

    public DashboardController(
            UserRepository users,
            ReviewRepository reviews,
            UserBadgeRepository userBadges,
            BadgeRepository badges) {
        this.users = users;
        this.reviews = reviews;
        this.userBadges = userBadges;
        this.badges = badges;
    }

    public record UpdateProfileRequest(
            @NotBlank @Size(max = 60) String firstName,
            @NotBlank @Size(max = 60) String lastName) {}

    @GetMapping("/me/dashboard")
    public Map<String, Object> dashboard(@RequestAttribute("userId") String userId) {
        User user = findUser(userId);
        List<Review> ownReviews = reviews.findByUserIdOrderByCreatedAtDesc(userId);
        int helpfulVotes = ownReviews.stream().mapToInt(review -> review.helpfulVoteCount).sum();

        Map<String, Object> result = new HashMap<>();
        result.put("user", safeUser(user));
        result.put("reviewCount", ownReviews.size());
        result.put("helpfulReactions", helpfulVotes);
        result.put("reputationScore", user.reputationScore);
        result.put("badgeCount", userBadges.findByUserId(userId).size());
        result.put("badges", earnedBadges(userId));
        result.put("reviews", ownReviews.stream().map(this::reviewSummary).toList());
        return result;
    }

    @GetMapping("/me/profile")
    public Map<String, Object> myProfile(@RequestAttribute("userId") String userId) {
        User user = findUser(userId);
        Map<String, Object> result = safeUser(user);
        result.put("badges", earnedBadges(userId));
        return result;
    }

    @PutMapping("/me/profile")
    public Map<String, Object> updateProfile(
            @RequestAttribute("userId") String userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        User user = findUser(userId);
        user.firstName = request.firstName().trim();
        user.lastName = request.lastName().trim();
        return safeUser(users.save(user));
    }

    @GetMapping("/{id}")
    public Map<String, Object> publicProfile(@PathVariable String id) {
        User user = findUser(id);
        List<Map<String, Object>> publicReviews = reviews.findByUserIdOrderByCreatedAtDesc(id).stream()
                .filter(review -> "PUBLISHED".equals(review.status))
                .map(this::reviewSummary)
                .toList();
        Map<String, Object> result = safeUser(user);
        result.put("reviewCount", publicReviews.size());
        result.put("reviews", publicReviews);
        result.put("badges", earnedBadges(id));
        return result;
    }

    private User findUser(String id) {
        return users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private Map<String, Object> safeUser(User user) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", user.id);
        result.put("firstName", user.firstName);
        result.put("lastName", user.lastName);
        result.put("role", user.role);
        result.put("reputationScore", user.reputationScore);
        result.put("createdAt", user.createdAt);
        return result;
    }

    private List<Map<String, Object>> earnedBadges(String userId) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (UserBadge award : userBadges.findByUserId(userId)) {
            badges.findById(award.badgeId).ifPresent(badge -> {
                Map<String, Object> item = new HashMap<>();
                item.put("id", badge.id);
                item.put("name", badge.badgeName);
                item.put("description", badge.description);
                item.put("awardedAt", award.awardedAt);
                result.add(item);
            });
        }
        return result;
    }

    private Map<String, Object> reviewSummary(Review review) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", review.id);
        item.put("productName", review.productName);
        item.put("category", review.category);
        item.put("headline", review.headline);
        item.put("ratingValue", review.ratingValue);
        item.put("reviewText", review.reviewText);
        item.put("authenticityStatus", review.authenticityStatus);
        item.put("credibilityScore", review.credibilityScore);
        item.put("helpfulVoteCount", review.helpfulVoteCount);
        item.put("status", review.status);
        item.put("createdAt", review.createdAt);
        item.put("updatedAt", review.updatedAt == null ? Instant.EPOCH : review.updatedAt);
        return item;
    }
}