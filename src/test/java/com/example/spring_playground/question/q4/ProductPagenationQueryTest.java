package com.example.spring_playground.question.q4;

import com.example.spring_playground.domain.commerce.entity.Product;
import com.example.spring_playground.domain.commerce.repository.ProductQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.*;

import java.util.ArrayList;
import java.util.List;

@DataJpaTest(showSql = false)
public class ProductPagenationQueryTest {
    @Autowired
    private ProductQueryRepository repository;

    @BeforeEach
    void setUp() {
        List<Product> products = new ArrayList<>();

        for (int i = 1; i <= 10; i++) {
            products.add(new Product(
                    "상품 " + i,
                    i * 1000,
                    100
            ));
        }

        repository.saveAll(products);
    }

    @Test
    void pageImplCreationTest() {
        PageRequest pageRequest = PageRequest.of(0, 3);

        List<Product> products = repository.findAll();
        PageImpl<Product> products1 = new PageImpl<>(products, pageRequest, products.size());
        System.out.println("데이터1:" + products1.getSize());
    }

    /**
     * List: 단순 리스트 조회
     */
    @Test
    void paginationReturnTypeComparisonTest() {
        Pageable pageable = PageRequest.of(0, 3, Sort.by("price"));

        System.out.println("===== LIST =====");
        List<Product> list =
                repository.findListBy(pageable);
        System.out.println("조회 건수: " + list.size());

        System.out.println("===== SLICE =====");
        Slice<Product> slice =
                repository.findSliceBy(pageable);
        System.out.println("다음 페이지: " + slice.hasNext());

        System.out.println("===== PAGE =====");
        Page<Product> page =
                repository.findPageBy(pageable);
        System.out.println("전체 데이터: " + page.getTotalElements());
    }

    /**
     * 전체 데이터/페이지의 갯수  + 페이지 네이션
     * => 게시판 페이지
     */
    @Test
    void pageSequentialQueryTest() {
        Pageable pageable = PageRequest.of(
                0,
                3,
                Sort.by("id").descending()
        );

        Page<Product> page = repository.findAll(pageable);

        System.out.println("데이터: " + page.getContent());
        System.out.println("현재 페이지: " + page.getNumber());
        System.out.println("페이지 크기: " + page.getSize());
        System.out.println("전체 데이터: " + page.getTotalElements());
        System.out.println("전체 데이터: " + page.getTotalPages());
        System.out.println("전체 페이지: " + page.getTotalPages());
        System.out.println("다음 페이지: " + page.hasNext());
        System.out.println("이전 페이지: " + page.hasPrevious());
        System.out.println("조회 개수: " + page.getNumberOfElements());

        while (page.hasNext()) {
            page =
                    repository.findAll(
                            page.nextPageable()
                    );
            System.out.println("데이터: " + page.getContent());
            System.out.println("현재 페이지: " + page.getNumber());
            System.out.println("페이지 크기: " + page.getSize());
            System.out.println("다음 페이지: " + page.hasNext());
            System.out.println("이전 페이지: " + page.hasPrevious());
            System.out.println("조회 개수: " + page.getNumberOfElements());

        }
    }

    @Test
    void pageCountQueryOmissionTest() {
        Pageable pageable = PageRequest.of(
                0,
                20,
                Sort.by("id").descending()
        );

        Page<Product> page = repository.findAll(pageable);

        System.out.println("데이터: " + page.getContent());
        System.out.println("현재 페이지: " + page.getNumber());
        System.out.println("페이지 크기: " + page.getSize());
        System.out.println("전체 데이터: " + page.getTotalElements());
        System.out.println("전체 데이터: " + page.getTotalPages());
        System.out.println("전체 페이지: " + page.getTotalPages());
        System.out.println("다음 페이지: " + page.hasNext());
        System.out.println("이전 페이지: " + page.hasPrevious());
        System.out.println("조회 개수: " + page.getNumberOfElements());

    }

    /**
     * 다음 페이지 유무 + 페이지 네이션
     * => 무한 스크롤
     */
    @Test
    void sliceSequentialQueryTest() {
        Pageable pageable = PageRequest.of(0, 3, Sort.by("id").descending());

        Slice<Product> slice = repository.findAll(pageable);

        System.out.println("데이터: " + slice.getContent());
        System.out.println("현재 페이지: " + slice.getNumber());
        System.out.println("페이지 크기: " + slice.getSize());
        System.out.println("다음 페이지: " + slice.hasNext());
        System.out.println("이전 페이지: " + slice.hasPrevious());
        System.out.println("조회 개수: " + slice.getNumberOfElements());

        while (slice.hasNext()) {
            slice =
                    repository.findAll(
                            slice.nextPageable()
                    );
            System.out.println("데이터: " + slice.getContent());
            System.out.println("현재 페이지: " + slice.getNumber());
            System.out.println("페이지 크기: " + slice.getSize());
            System.out.println("다음 페이지: " + slice.hasNext());
            System.out.println("이전 페이지: " + slice.hasPrevious());
            System.out.println("조회 개수: " + slice.getNumberOfElements());

        }

    }



}
