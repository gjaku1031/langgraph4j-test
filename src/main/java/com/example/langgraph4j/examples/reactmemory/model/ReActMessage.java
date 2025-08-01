package com.example.langgraph4j.examples.reactmemory.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ReAct 시스템의 메시지 모델
 * 
 * Python 예제의 다양한 메시지 타입을 Java로 구현한 클래스입니다.
 * 
 * @author Claude AI Assistant
 * @since 2025-07-31
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReActMessage {
    
    /**
     * 메시지 내용
     */
    private String content;
    
    /**
     * 메시지 타입
     */
    private MessageType type;
    
    /**
     * 생성 시간
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    
    /**
     * 도구 호출 정보 (AI 메시지의 경우)
     */
    private List<ToolCall> toolCalls;
    
    /**
     * 소스 정보 (도구 결과의 경우)
     */
    private String source;
    
    /**
     * 추가 메타데이터
     */
    private String metadata;
    
    /**
     * 메시지 타입 열거형
     */
    public enum MessageType {
        USER,           // 사용자 입력
        AI,             // AI 응답 (추론 포함)
        TOOL_CALL,      // 도구 호출
        TOOL_RESULT,    // 도구 실행 결과
        SYSTEM          // 시스템 메시지
    }
    
    /**
     * 사용자 메시지 생성 헬퍼
     */
    public static ReActMessage createUserMessage(String content) {
        return ReActMessage.builder()
            .content(content)
            .type(MessageType.USER)
            .build();
    }
    
    /**
     * AI 메시지 생성 헬퍼
     */
    public static ReActMessage createAiMessage(String content) {
        return ReActMessage.builder()
            .content(content)
            .type(MessageType.AI)
            .build();
    }
    
    /**
     * 도구 호출 메시지 생성 헬퍼
     */
    public static ReActMessage createToolCallMessage(String toolName, String parameters) {
        String content = String.format("Tool Call: %s\nParameters: %s", toolName, parameters);
        return ReActMessage.builder()
            .content(content)
            .type(MessageType.TOOL_CALL)
            .build();
    }
    
    /**
     * 도구 결과 메시지 생성 헬퍼
     */
    public static ReActMessage createToolResultMessage(String result, String source) {
        return ReActMessage.builder()
            .content(result)
            .type(MessageType.TOOL_RESULT)
            .source(source)
            .build();
    }
    
    /**
     * 시스템 메시지 생성 헬퍼
     */
    public static ReActMessage createSystemMessage(String content) {
        return ReActMessage.builder()
            .content(content)
            .type(MessageType.SYSTEM)
            .build();
    }
    
    /**
     * 메시지를 문자열로 포맷팅
     */
    public String format() {
        StringBuilder sb = new StringBuilder();
        
        switch (type) {
            case USER:
                sb.append("Human: ").append(content);
                break;
            case AI:
                sb.append("Assistant: ").append(content);
                break;
            case TOOL_CALL:
                sb.append("Tool Call: ").append(content);
                break;
            case TOOL_RESULT:
                sb.append("Tool Result: ").append(content);
                if (source != null) {
                    sb.append("\n[Source: ").append(source).append("]");
                }
                break;
            case SYSTEM:
                sb.append("System: ").append(content);
                break;
        }
        
        return sb.toString();
    }
    
    /**
     * 도구 호출이 포함되어 있는지 확인
     */
    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }
}