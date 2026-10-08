package com.credify.repository;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.credify.model.Reaction;

public interface ReactionRepository extends MongoRepository<Reaction, String> {
    Optional<Reaction> findByReviewIdAndUserId(String reviewId, String userId);
    long countByUserId(String userId);
}