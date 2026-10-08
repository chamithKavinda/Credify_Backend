package com.credify.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "badges")
public class Badge {
    @Id public String id;
    @Indexed(unique = true) public String badgeName;
    public String description;
    public String earningCriteria;
}