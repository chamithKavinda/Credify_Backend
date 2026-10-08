package com.credify.repository;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.credify.model.Badge;

public interface BadgeRepository extends MongoRepository<Badge, String> {
    boolean existsByBadgeNameIgnoreCase(String badgeName);
    Optional<Badge> findByBadgeNameIgnoreCase(String badgeName);
}