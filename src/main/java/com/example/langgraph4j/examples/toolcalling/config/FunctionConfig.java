package com.example.langgraph4j.examples.toolcalling.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Spring AI Function 설정
 * 
 * 기본 Bean들을 등록합니다.
 * 
 * @author Claude AI Assistant
 * @since 2025-07-31
 */
@Slf4j
@Configuration
public class FunctionConfig {

    /**
     * ObjectMapper Bean 등록 (JSR310 모듈 포함)
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    /**
     * RestTemplate Bean 등록
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}