package com.credify.config;

import com.credify.model.Badge;
import com.credify.model.Comment;
import com.credify.model.Report;
import com.credify.model.Review;
import com.credify.model.User;
import com.credify.repository.BadgeRepository;
import com.credify.repository.CommentRepository;
import com.credify.repository.ReportRepository;
import com.credify.repository.ReviewRepository;
import com.credify.repository.UserRepository;
import java.time.Instant;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SeedData {

    @Bean
    CommandLineRunner seedDatabase(
            UserRepository users,
            ReviewRepository reviews,
            BadgeRepository badges,
            CommentRepository comments,
            ReportRepository reports,
            PasswordEncoder passwordEncoder) {
        return args -> {
            // 1. Seed System Badges
            createBadgeIfMissing(badges, "Helpful Contributor",
                    "Receives helpful votes from the community for tech reviews", "Awarded by receiving 10+ helpful votes");
            createBadgeIfMissing(badges, "Trusted Reviewer",
                    "Writes detailed reviews with high credibility scores on digital devices", "Awarded after review audit");
            createBadgeIfMissing(badges, "Electronics Expert",
                    "Consistently evaluates hardware, power efficiency, and digital specs", "Awarded by community engagement");

            // 2. Seed Admin User
            users.findByEmail("admin@credify.com").orElseGet(() -> {
                User u = new User();
                u.email = "admin@credify.com";
                u.firstName = "Admin";
                u.lastName = "User";
                u.passwordHash = passwordEncoder.encode("Admin123!");
                u.role = "ADMIN";
                u.accountStatus = "ACTIVE";
                u.reputationScore = 100.0;
                return users.save(u);
            });

            // 3. Seed Sample Contributor Users
            User userElena = users.findByEmail("elena@example.com").orElseGet(() -> {
                User u = new User();
                u.email = "elena@example.com";
                u.firstName = "Elena";
                u.lastName = "Rostova";
                u.passwordHash = passwordEncoder.encode("User123!");
                u.role = "USER";
                u.accountStatus = "ACTIVE";
                u.reputationScore = 92.0;
                return users.save(u);
            });

            User userMarcus = users.findByEmail("marcus@example.com").orElseGet(() -> {
                User u = new User();
                u.email = "marcus@example.com";
                u.firstName = "Marcus";
                u.lastName = "Chen";
                u.passwordHash = passwordEncoder.encode("User123!");
                u.role = "USER";
                u.accountStatus = "ACTIVE";
                u.reputationScore = 88.0;
                return users.save(u);
            });

            // 4. Seed Electronic & Digital Devices Reviews if empty
            if (reviews.count() == 0) {
                Review r1 = new Review();
                r1.userId = userElena.id;
                r1.productName = "Sony WH-1000XM5 Wireless Noise-Cancelling Headphones";
                r1.category = "Audio & Wearables";
                r1.headline = "Exceptional active noise cancellation and 30-hour battery life";
                r1.reviewText = "After four months of daily train commutes and office use, the active noise cancellation remains top tier. Acoustic separation is clean, dual processors filter background chatter effectively, and USB-C fast charging gives 3 hours playback in 3 minutes.";
                r1.ratingValue = 5;
                r1.authenticityStatus = "VERIFIED";
                r1.sentiment = "POSITIVE";
                r1.status = "PUBLISHED";
                r1.authenticityScore = 0.96;
                r1.credibilityScore = 96.0;
                r1.helpfulVoteCount = 42;
                r1.createdAt = Instant.now().minusSeconds(86400 * 7);
                r1.updatedAt = r1.createdAt;
                reviews.save(r1);

                Review r2 = new Review();
                r2.userId = userMarcus.id;
                r2.productName = "Logitech MX Master 3S Ergonomic Wireless Mouse";
                r2.category = "Mice & Accessories";
                r2.headline = "Quiet 8,000 DPI tracking on glass with MagSpeed electromagnetic wheel";
                r2.reviewText = "Extremely comfortable ergonomic design for 8+ hour coding days. The 90% quieter clicks are satisfying, custom thumb wheel speeds up video timeline editing, and Bluetooth multi-device switching works flawlessly.";
                r2.ratingValue = 5;
                r2.authenticityStatus = "VERIFIED";
                r2.sentiment = "POSITIVE";
                r2.status = "PUBLISHED";
                r2.authenticityScore = 0.95;
                r2.credibilityScore = 95.0;
                r2.helpfulVoteCount = 38;
                r2.createdAt = Instant.now().minusSeconds(86400 * 6);
                r2.updatedAt = r2.createdAt;
                reviews.save(r2);

                Review r3 = new Review();
                r3.userId = userElena.id;
                r3.productName = "Sonos Era 300 Smart Bluetooth Speaker";
                r3.category = "Speakers & Acoustics";
                r3.headline = "Immersive Spatial Audio and Dolby Atmos performance for home setups";
                r3.reviewText = "Six optimally positioned drivers project sound wall-to-wall and ceiling-to-floor. Trueplay tuning customizes acoustic response to your room geometry, delivering deep bass and crystal clear vocals.";
                r3.ratingValue = 5;
                r3.authenticityStatus = "VERIFIED";
                r3.sentiment = "POSITIVE";
                r3.status = "PUBLISHED";
                r3.authenticityScore = 0.93;
                r3.credibilityScore = 93.0;
                r3.helpfulVoteCount = 29;
                r3.createdAt = Instant.now().minusSeconds(86400 * 5);
                r3.updatedAt = r3.createdAt;
                reviews.save(r3);

                Review r4 = new Review();
                r4.userId = userMarcus.id;
                r4.productName = "Apple MacBook Pro 16\" (M3 Max, 36GB RAM, 1TB SSD)";
                r4.category = "Computing & Laptops";
                r4.headline = "Monster performance for 4K video editing with zero fan noise";
                r4.reviewText = "Extremely impressive thermal efficiency under heavy 4K ProRes rendering. Liquid Retina XDR screen achieves 1600 nits peak brightness, and battery easily lasts 18+ hours under mixed dev workloads.";
                r4.ratingValue = 5;
                r4.authenticityStatus = "VERIFIED";
                r4.sentiment = "POSITIVE";
                r4.status = "PUBLISHED";
                r4.authenticityScore = 0.94;
                r4.credibilityScore = 94.0;
                r4.helpfulVoteCount = 31;
                r4.createdAt = Instant.now().minusSeconds(86400 * 4);
                r4.updatedAt = r4.createdAt;
                reviews.save(r4);

                Review r5 = new Review();
                r5.userId = userElena.id;
                r5.productName = "Keychron Q1 Pro Wireless Custom Mechanical Keyboard";
                r5.category = "Mice & Accessories";
                r5.headline = "CNC aluminum double-gasket design with tactile Gateron Jupiter switches";
                r5.reviewText = "Full QMK/VIA support allows remapping every key layer. The double-gasket structure reduces metallic ping and delivers a deep, acoustic sound profile right out of the box.";
                r5.ratingValue = 5;
                r5.authenticityStatus = "VERIFIED";
                r5.sentiment = "POSITIVE";
                r5.status = "PUBLISHED";
                r5.authenticityScore = 0.92;
                r5.credibilityScore = 92.0;
                r5.helpfulVoteCount = 26;
                r5.createdAt = Instant.now().minusSeconds(86400 * 3);
                r5.updatedAt = r5.createdAt;
                reviews.save(r5);

                Review r6 = new Review();
                r6.userId = userMarcus.id;
                r6.productName = "ASUS ROG Zephyrus G16 OLED Gaming Laptop (RTX 4080)";
                r6.category = "Computing & Laptops";
                r6.headline = "Ultra-thin CNC chassis with 240Hz ROG Nebula OLED display";
                r6.reviewText = "Vapor chamber cooling keeps Intel Core Ultra 9 and RTX 4080 running smoothly. 240Hz OLED panel provides sub-0.2ms response times with perfect contrast ratio for competitive gaming.";
                r6.ratingValue = 4;
                r6.authenticityStatus = "VERIFIED";
                r6.sentiment = "POSITIVE";
                r6.status = "PUBLISHED";
                r6.authenticityScore = 0.90;
                r6.credibilityScore = 90.0;
                r6.helpfulVoteCount = 21;
                r6.createdAt = Instant.now().minusSeconds(86400 * 2);
                r6.updatedAt = r6.createdAt;
                reviews.save(r6);

                Review r7 = new Review();
                r7.userId = userElena.id;
                r7.productName = "Bose QuietComfort Ultra Wireless Noise-Cancelling Earbuds";
                r7.category = "Audio & Wearables";
                r7.headline = "CustomTune acoustic personalization with Spatialized Audio modes";
                r7.reviewText = "Immersion mode creates a wide front stage sound field. Soft umbrella-shaped ear tips fit comfortably for hours without ear canal fatigue, and ANC isolates low frequency aircraft rumble.";
                r7.ratingValue = 4;
                r7.authenticityStatus = "VERIFIED";
                r7.sentiment = "POSITIVE";
                r7.status = "PUBLISHED";
                r7.authenticityScore = 0.88;
                r7.credibilityScore = 88.0;
                r7.helpfulVoteCount = 17;
                r7.createdAt = Instant.now().minusSeconds(86400 * 1);
                r7.updatedAt = r7.createdAt;
                reviews.save(r7);

                Review r8 = new Review();
                r8.userId = userMarcus.id;
                r8.productName = "SwiftCharge 100W GaN Pro 4-Port Fast Charger";
                r8.category = "Electric & Power Accessories";
                r8.headline = "Overheating issues under sustained dual-laptop 100W load";
                r8.reviewText = "Tested charging a MacBook Pro and iPad simultaneously. The charger housing gets uncomfortably hot (over 68°C) after 40 minutes of heavy power draw.";
                r8.ratingValue = 2;
                r8.authenticityStatus = "NEEDS_REVIEW";
                r8.sentiment = "NEGATIVE";
                r8.status = "PENDING";
                r8.authenticityScore = 0.45;
                r8.credibilityScore = 45.0;
                r8.helpfulVoteCount = 3;
                r8.createdAt = Instant.now().minusSeconds(3600 * 4);
                r8.updatedAt = r8.createdAt;
                reviews.save(r8);

                // Seed Comment
                Comment comment = new Comment();
                comment.reviewId = r1.id;
                comment.userId = userMarcus.id;
                comment.commentText = "Great hardware analysis! Did you test LDAC codec audio streaming on Android?";
                comment.createdAt = Instant.now().minusSeconds(86400 * 4);
                comments.save(comment);

                // Seed Report
                Report report = new Report();
                report.reviewId = r8.id;
                report.reporterUserId = userElena.id;
                report.reason = "Suspicious rating pattern flagged for temperature claims on GaN charger.";
                report.status = "OPEN";
                report.createdAt = Instant.now().minusSeconds(3600 * 2);
                report.updatedAt = report.createdAt;
                reports.save(report);
            }
        };
    }

    private Badge createBadgeIfMissing(BadgeRepository badges, String name, String description, String criteria) {
        return badges.findByBadgeNameIgnoreCase(name).orElseGet(() -> {
            Badge badge = new Badge();
            badge.badgeName = name;
            badge.description = description;
            badge.earningCriteria = criteria;
            return badges.save(badge);
        });
    }
}