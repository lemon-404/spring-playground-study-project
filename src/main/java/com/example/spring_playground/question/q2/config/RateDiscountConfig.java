package com.example.spring_playground.question.q2.config;

import com.example.spring_playground.question.q2.policy.DiscountPolicy;
import com.example.spring_playground.question.q2.policy.RateDiscountPolicy;
import com.example.spring_playground.question.q2.service.Q2OrderService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
//@Profile("rate")
public class RateDiscountConfig {

    @Bean
    public DiscountPolicy discountPolicy() {
        return new RateDiscountPolicy();
    }

    @Bean
    public Q2OrderService orderService(DiscountPolicy discountPolicy) {
        return new Q2OrderService(discountPolicy);
    }
}
