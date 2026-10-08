package com.credify.controller;

import com.credify.model.Comment;
import com.credify.model.Badge;
import com.credify.model.Reaction;
import com.credify.model.Report;
import com.credify.model.Review;
import com.credify.model.UserBadge;
import com.credify.model.User;
import com.credify.repository.BadgeRepository;
import com.credify.repository.CommentRepository;
import com.credify.repository.ReactionRepository;
import com.credify.repository.ReportRepository;
import com.credify.repository.ReviewRepository;
import com.credify.repository.UserRepository;
import com.credify.repository.UserBadgeRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class ReviewController {
    private final ReviewRepository reviews;
    private final UserRepository users;
    private final CommentRepository comments;
    private final ReactionRepository reactions;
    private final ReportRepository reports;
    private final BadgeRepository badges;
    private final UserBadgeRepository userBadges;
    private final RestClient aiClient;

    public ReviewController(
            ReviewRepository reviews,
            UserRepository users,
            CommentRepository comments,
            ReactionRepository reactions,
            ReportRepository reports,
            BadgeRepository badges,
            UserBadgeRepository userBadges,
            @Value("${credify.ai.url}") String aiUrl) {
        this.reviews = reviews;
        this.users = users;
        this.comments = comments;
        this.reactions = reactions;
        this.reports = reports;
        this.badges = badges;
        this.userBadges = userBadges;
        this.aiClient = RestClient.builder().baseUrl(aiUrl).build();
    }

    public record NewReview(
            @NotBlank @Size(max = 120) String productName,
            @Size(max = 60) String category,
            @NotBlank @Size(max = 180) String headline,
            @Min(1) @Max(5) int ratingValue,
            @NotBlank @Size(min = 10, max = 5000) String reviewText) {}

    public record UpdateReview(
            @Size(max = 120) String productName,
            @Size(max = 60) String category,
            @Size(max = 180) String headline,
            @Min(1) @Max(5) Integer ratingValue,
            @Size(min = 10, max = 5000) String reviewText) {}

    public record NewComment(@NotBlank @Size(max = 1000) String commentText) {}
    public record NewReport(@NotBlank @Size(max = 500) String reason) {}

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "service", "credify-api");
    }

    /** Public feed: rating is satisfaction; credibility is the separate automated signal. */
    @GetMapping("/reviews")
    public List<Map<String, Object>> listReviews(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer minRating,
            @RequestParam(defaultValue = "newest") String sort) {
        Stream<Review> result = reviews.findByStatusOrderByCreatedAtDesc("PUBLISHED").stream();

        if (q != null && !q.isBlank()) {
            String term = q.trim().toLowerCase(Locale.ROOT);
            result = result.filter(review -> contains(review.productName, term)
                    || contains(review.headline, term)
                    || contains(review.reviewText, term));
        }
        if (category != null && !category.isBlank() && !"All categories".equalsIgnoreCase(category)) {
            result = result.filter(review -> category.equalsIgnoreCase(review.category));
        }
        if (minRating != null) {
            if (minRating < 1 || minRating > 5) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minRating must be between 1 and 5");
            }
            result = result.filter(review -> review.ratingValue >= minRating);
        }

        Comparator<Review> comparator = switch (sort.toLowerCase(Locale.ROOT)) {
            case "rating" -> Comparator.comparingInt(review -> review.ratingValue);
            case "helpful" -> Comparator.comparingInt(review -> review.helpfulVoteCount);
            case "credibility" -> Comparator.comparingDouble(review -> review.credibilityScore);
            case "newest" -> Comparator.comparing(review -> review.createdAt);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported sort option");
        };
        return result.sorted(comparator.reversed()).map(this::toReviewView).toList();
    }

    @GetMapping("/reviews/{id}")
    public Map<String, Object> getReview(@PathVariable String id) {
        Review review = reviews.findById(id)
                .filter(item -> "PUBLISHED".equals(item.status))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found"));
        return toReviewView(review);
    }

    @PostMapping("/reviews")
    public Map<String, Object> createReview(
            @RequestAttribute("userId") String userId,
            @Valid @RequestBody NewReview request) {
        Review review = new Review();
        review.userId = userId;
        review.productName = request.productName().trim();
        review.category = cleanOrDefault(request.category(), "General");
        review.headline = request.headline().trim();
        review.ratingValue = request.ratingValue();
        review.reviewText = request.reviewText().trim();
        review.createdAt = Instant.now();
        review.updatedAt = review.createdAt;
        analyze(review);
        review.status = requiresModeration(review) ? "PENDING" : "PUBLISHED";
        return toReviewView(reviews.save(review));
    }

    @PutMapping("/reviews/{id}")
    public Map<String, Object> updateReview(
            @PathVariable String id,
            @RequestAttribute("userId") String userId,
            @Valid @RequestBody UpdateReview request) {
        Review review = reviews.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found"));
        if (!Objects.equals(userId, review.userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only edit your own review");
        }
        if (request.productName() != null) review.productName = request.productName().trim();
        if (request.category() != null) review.category = cleanOrDefault(request.category(), "General");
        if (request.headline() != null) review.headline = request.headline().trim();
        if (request.ratingValue() != null) review.ratingValue = request.ratingValue();
        if (request.reviewText() != null) {
            review.reviewText = request.reviewText().trim();
            analyze(review);
            review.status = requiresModeration(review) ? "PENDING" : "PUBLISHED";
        }
        review.updatedAt = Instant.now();
        return toReviewView(reviews.save(review));
    }

    @GetMapping("/reviews/{id}/comments")
    public List<Comment> getComments(@PathVariable String id) {
        requireReview(id);
        return comments.findByReviewIdOrderByCreatedAtAsc(id);
    }

    @PostMapping("/reviews/{id}/comments")
    public Comment addComment(
            @PathVariable String id,
            @RequestAttribute("userId") String userId,
            @Valid @RequestBody NewComment request) {
        requireReview(id);
        Comment comment = new Comment();
        comment.reviewId = id;
        comment.userId = userId;
        comment.commentText = request.commentText().trim();
        comment.createdAt = Instant.now();
        return comments.save(comment);
    }

    @PostMapping("/reviews/{id}/helpful")
    public Map<String, Object> markHelpful(
            @PathVariable String id,
            @RequestAttribute("userId") String userId) {
        Review review = requireReview(id);
        if (Objects.equals(review.userId, userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot vote on your own review");
        }
        try {
            if (reactions.findByReviewIdAndUserId(id, userId).isEmpty()) {
                Reaction reaction = new Reaction();
                reaction.reviewId = id;
                reaction.userId = userId;
                reaction.reactionType = "HELPFUL";
                reaction.createdAt = Instant.now();
                reactions.save(reaction);
                review.helpfulVoteCount++;
                reviews.save(review);
                updateReviewerReputation(review.userId);
            }
        } catch (DuplicateKeyException ignored) {
            // The compound unique index makes repeated clicks idempotent, including concurrent requests.
        }
        Review latest = reviews.findById(id).orElse(review);
        return Map.of("helpfulVoteCount", latest.helpfulVoteCount);
    }

    @PostMapping("/reviews/{id}/reports")
    public Report reportReview(
            @PathVariable String id,
            @RequestAttribute("userId") String userId,
            @Valid @RequestBody NewReport request) {
        requireReview(id);
        Report report = new Report();
        report.reviewId = id;
        report.reporterUserId = userId;
        report.reason = request.reason().trim();
        report.status = "OPEN";
        report.createdAt = Instant.now();
        report.updatedAt = report.createdAt;
        return reports.save(report);
    }

    private Review requireReview(String id) {
        return reviews.findById(id)
                .filter(review -> "PUBLISHED".equals(review.status))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found"));
    }

    private boolean contains(String value, String term) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(term);
    }

    private String cleanOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private boolean requiresModeration(Review review) {
        return "PENDING".equals(review.authenticityStatus)
                || "NEEDS_REVIEW".equals(review.authenticityStatus);
    }

    /** If the Python service is offline, preserve the review and send it to manual moderation. */
    private void analyze(Review review) {
        try {
            Map<?, ?> result = aiClient.post()
                    .uri("/analyze")
                    .body(Map.of("text", review.reviewText))
                    .retrieve()
                    .body(Map.class);
            if (result == null || !(result.get("authenticityScore") instanceof Number score)) {
                setAnalysisPending(review);
                return;
            }
            double normalizedScore = Math.max(0, Math.min(1, score.doubleValue()));
            review.authenticityScore = normalizedScore;
            review.credibilityScore = Math.round(normalizedScore * 100.0);
            review.authenticityStatus = Objects.toString(result.get("authenticityStatus"), "PENDING");
            review.sentiment = Objects.toString(result.get("sentiment"), "UNKNOWN");
        } catch (Exception ignored) {
            setAnalysisPending(review);
        }
    }

    private void setAnalysisPending(Review review) {
        review.authenticityScore = 0;
        review.credibilityScore = 0;
        review.authenticityStatus = "PENDING";
        review.sentiment = "UNKNOWN";
    }

    private void updateReviewerReputation(String reviewerId) {
        User reviewer = users.findById(reviewerId).orElse(null);
        if (reviewer == null) return;
        int helpfulVotesReceived = reviews.findByUserIdOrderByCreatedAtDesc(reviewerId).stream()
                .mapToInt(item -> item.helpfulVoteCount)
                .sum();
        reviewer.reputationScore = Math.min(100, helpfulVotesReceived * 2.0);
        users.save(reviewer);

        if (helpfulVotesReceived >= 10) {
            badges.findByBadgeNameIgnoreCase("Helpful Contributor").ifPresent(badge -> awardIfMissing(reviewerId, badge));
        }
    }

    private void awardIfMissing(String userId, Badge badge) {
        if (userBadges.existsByUserIdAndBadgeId(userId, badge.id)) return;
        UserBadge award = new UserBadge();
        award.userId = userId;
        award.badgeId = badge.id;
        award.awardedAt = Instant.now();
        try {
            userBadges.save(award);
        } catch (DuplicateKeyException ignored) {
            // Safe when two helpful votes arrive at the same time.
        }
    }

    private Map<String, Object> toReviewView(Review review) {
        User author = users.findById(review.userId).orElse(null);
        String authorName = author == null ? "Member" : (author.firstName + " " + author.lastName).trim();
        Map<String, Object> view = new HashMap<>();
        view.put("id", review.id);
        view.put("userId", review.userId);
        view.put("author", authorName);
        view.put("productName", review.productName);
        view.put("category", review.category);
        view.put("headline", review.headline);
        view.put("ratingValue", review.ratingValue);
        view.put("reviewText", review.reviewText);
        view.put("authenticityStatus", review.authenticityStatus);
        view.put("authenticityScore", review.authenticityScore);
        view.put("credibilityScore", review.credibilityScore);
        view.put("sentiment", review.sentiment);
        view.put("helpfulVoteCount", review.helpfulVoteCount);
        view.put("status", review.status);
        view.put("createdAt", review.createdAt);
        view.put("updatedAt", review.updatedAt);
        return view;
    }
}