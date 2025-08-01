package com.example.langgraph4j.examples.messagegraph.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 메시지 기본 클래스
 * 
 * Python 예제의 AnyMessage를 Java로 구현한 클래스입니다.
 * 
 * @author Claude AI Assistant
 * @since 2025-07-31
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class Message {
    
    /**
     * 메시지 내용
     */
    private String content;
    
    /**
     * 메시지 생성 시간
     */
    private LocalDateTime timestamp;
    
    /**
     * 메시지 타입
     */
    public abstract MessageType getType();
    
    /**
     * 메시지 타입 열거형
     */
    public enum MessageType {
        HUMAN, AI, SYSTEM
    }
    
    public Message(String content) {
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }
}