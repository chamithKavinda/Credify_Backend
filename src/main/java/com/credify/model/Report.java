package com.credify.model;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "reports")
public class Report {
    @Id public String id;
    @Indexed public String reviewId;
    public String reporterUserId;
    public String reason;
    @Indexed public String status = "OPEN";
    public Instant createdAt = Instant.now();
    public Instant updatedAt = Instant.now();
}