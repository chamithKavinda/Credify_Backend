package com.credify.model;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "user_badges")
@CompoundIndex(name = "user_badge_unique", def = "{'userId': 1, 'badgeId': 1}", unique = true)
public class UserBadge {
    @Id public String id;
    public String userId;
    public String badgeId;
    public Instant awardedAt = Instant.now();
}