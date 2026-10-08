package com.credify.repository;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.credify.model.UserBadge;

public interface UserBadgeRepository extends MongoRepository<UserBadge, String> {
    List<UserBadge> findByUserId(String userId);
    boolean existsByUserIdAndBadgeId(String userId, String badgeId);
    long countByBadgeId(String badgeId);
}