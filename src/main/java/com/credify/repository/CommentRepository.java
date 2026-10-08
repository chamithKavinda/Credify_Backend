package com.credify.repository;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.credify.model.Comment;

public interface CommentRepository extends MongoRepository<Comment, String> {
    List<Comment> findByReviewIdOrderByCreatedAtAsc(String reviewId);
}