package com.example.spring_playground.domain.commerce.repository;

import com.example.spring_playground.domain.commerce.entity.Product;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductQueryRepository extends JpaRepository<Product, Long> {
    List<Product> findListBy(Pageable pageable);

    Slice<Product> findSliceBy(Pageable pageable);

    Page<Product> findPageBy(Pageable pageable);

    // Window - Offset
    Window<Product> findFirst3ByOrderByIdAsc(
            OffsetScrollPosition position
    );

    // Window - Keyset
    Window<Product> findFirst3ByOrderByIdDesc(
            KeysetScrollPosition position
    );
}
