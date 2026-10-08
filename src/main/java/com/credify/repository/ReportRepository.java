package com.credify.repository;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.credify.model.Report;

public interface ReportRepository extends MongoRepository<Report, String> {
    List<Report> findByStatusOrderByCreatedAtDesc(String status);
    long countByStatus(String status);
}