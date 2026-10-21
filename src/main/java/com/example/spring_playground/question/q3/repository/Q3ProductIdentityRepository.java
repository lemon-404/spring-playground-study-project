package com.example.spring_playground.question.q3.repository;

import com.example.spring_playground.question.q3.entity.ProductIdentity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface Q3ProductIdentityRepository extends JpaRepository<ProductIdentity, Long> {
}
