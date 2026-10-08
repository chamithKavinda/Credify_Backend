package com.credify.config;

import com.credify.model.Badge;
import com.credify.repository.BadgeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SeedData {
    @Bean
    CommandLineRunner seedDefaultBadges(BadgeRepository badges) {
        return args -> {
            createIfMissing(badges, "Helpful Contributor",
                    "Receives helpful votes from the community", "Awarded by an administrator");
            createIfMissing(badges, "Trusted Reviewer",
                    "Writes detailed reviews with a strong review history", "Awarded after moderator review");
            createIfMissing(badges, "Community Voice",
                    "Consistently contributes constructive comments", "Awarded by an administrator");
        };
    }

    private void createIfMissing(BadgeRepository badges, String name, String description, String criteria) {
        if (!badges.existsByBadgeNameIgnoreCase(name)) {
            Badge badge = new Badge();
            badge.badgeName = name;
            badge.description = description;
            badge.earningCriteria = criteria;
            badges.save(badge);
        }
    }
}