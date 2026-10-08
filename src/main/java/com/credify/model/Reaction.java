package com.credify.model;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "reactions")
@CompoundIndex(name = "review_user_unique", def = "{'reviewId': 1, 'userId': 1}", unique = true)
public class Reaction {
    @Id public String id;
    public String reviewId;
    public String userId;
    public String reactionType = "HELPFUL";
    public Instant createdAt = Instant.now();
}