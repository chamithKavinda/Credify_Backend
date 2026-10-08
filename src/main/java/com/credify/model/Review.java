package com.credify.model;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "reviews")
public class Review {
    @Id public String id;
    @Indexed public String userId;
    public String productName;
    public String category = "General";
    public String headline = "";
    public String reviewText;
    public int ratingValue;
    public String authenticityStatus = "PENDING";
    public String sentiment = "UNKNOWN";
    public String status = "PENDING";
    public double authenticityScore = 0;
    public double credibilityScore = 0;
    public int helpfulVoteCount = 0;
    @Indexed public Instant createdAt = Instant.now();
    public Instant updatedAt = Instant.now();
}