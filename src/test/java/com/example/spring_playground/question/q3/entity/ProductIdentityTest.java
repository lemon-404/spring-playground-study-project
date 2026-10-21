package com.example.spring_playground.question.q3.entity;

import com.example.spring_playground.question.q3.repository.Q3ProductIdentityRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
class ProductIdentityTest {
    @Autowired
    private Q3ProductIdentityRepository productRepository;
    @PersistenceContext
    private EntityManager em;

    @Test
    @DisplayName("Product 100_000건 Batch Insert 테스트")
    void batchInsertTest() {

        // Given
        List<ProductIdentity> products = new ArrayList<>();

        for (int i = 0; i < 100_000; i++) {
            products.add(new ProductIdentity(
                    "Product " + i,
                    10000,
                    100
            ));
        }

        // When
        long start = System.nanoTime();

        productRepository.saveAll(products);

        em.flush();
        em.clear();

        long end = System.nanoTime();

        // Then
        long elapsedMs = (end - start) / 1_000_000;

        System.out.println("저장 시간: " + elapsedMs + "ms");
        assertThat(productRepository.count()).isEqualTo(100_000);
    }
}