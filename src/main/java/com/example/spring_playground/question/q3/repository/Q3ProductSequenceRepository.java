package com.example.spring_playground.question.q3.repository;

import com.example.spring_playground.question.q3.entity.ProductSequence;
import org.springframework.data.jpa.repository.JpaRepository;

public interface Q3ProductSequenceRepository extends JpaRepository<ProductSequence, Long> {
}
