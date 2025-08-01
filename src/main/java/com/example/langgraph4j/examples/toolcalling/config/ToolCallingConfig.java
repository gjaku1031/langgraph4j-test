package com.example.langgraph4j.examples.toolcalling.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Tool Calling 예제를 위한 Spring Configuration
 * 
 * 이 설정 클래스는 다음을 구성합니다:
 * - ChatClient: AI 모델과의 대화를 위한 클라이언트
 * - VectorStore: 문서 임베딩 저장 및 검색
 * - RestTemplate: 외부 API 호출용
 * 
 * @author Claude AI Assistant
 * @since 2025-07-31
 */
@Configuration
public class ToolCallingConfig {

    @Value("${tavily.api.key}")
    private String tavilyApiKey;

    /**
     * ChatClient 빈 생성
     * Spring AI의 ChatModel을 사용하여 AI와 대화하는 클라이언트를 생성합니다.
     */
    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem("당신은 도움이 되는 AI 어시스턴트입니다.")
                .build();
    }

    // VectorStore는 추후 구현
    // RestTemplate은 FunctionConfig에서 제공됨
}