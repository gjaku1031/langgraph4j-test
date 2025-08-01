package com.example.langgraph4j.examples.messagegraph.model;

import lombok.NoArgsConstructor;

/**
 * AI 응답 메시지 클래스
 * 
 * Python 예제의 AIMessage를 Java로 구현한 클래스입니다.
 * 
 * @author Claude AI Assistant
 * @since 2025-07-31
 */
@NoArgsConstructor
public class AiMessage extends Message {
    
    public AiMessage(String content) {
        super(content);
    }
    
    @Override
    public MessageType getType() {
        return MessageType.AI;
    }
}