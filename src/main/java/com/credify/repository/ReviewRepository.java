package com.credify.repository;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.credify.model.Review;

public interface ReviewRepository extends MongoRepository<Review, String> {
    List<Review> findByStatusOrderByCreatedAtDesc(String status);
    List<Review> findByUserIdOrderByCreatedAtDesc(String userId);
    List<Review> findByProductNameIgnoreCaseAndStatusOrderByCreatedAtDesc(String productName, String status);
    long countByStatus(String status);
    long countByAuthenticityStatus(String authenticityStatus);
}