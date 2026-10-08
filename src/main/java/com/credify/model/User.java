package com.credify.model;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "users")
public class User {
    @Id public String id;
    @Indexed(unique = true) public String email;
    public String passwordHash;
    public String firstName;
    public String lastName;
    public String role = "USER";
    public String accountStatus = "ACTIVE";
    public double reputationScore = 0;
    public Instant createdAt = Instant.now();
}