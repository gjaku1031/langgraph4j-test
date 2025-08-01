package com.example.langgraph4j.examples.toolcalling.config;

import org.springframework.context.annotation.Configuration;

/**
 * AI 모델 설정
 * 
 * Spring AI를 사용하여 AI 모델들을 구성합니다.
 * 
 * @author Claude AI Assistant
 * @since 2025-07-31
 */
@Configuration
public class AiConfig {
    // Spring AI AutoConfiguration이 자동으로 ChatModel과 EmbeddingModel을 구성합니다.
    // application.properties의 설정을 통해 모델이 자동 구성됩니다.
}