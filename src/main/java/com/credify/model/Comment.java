package com.credify.model;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "comments")
public class Comment {
    @Id public String id;
    @Indexed public String reviewId;
    public String userId;
    public String commentText;
    public Instant createdAt = Instant.now();
}