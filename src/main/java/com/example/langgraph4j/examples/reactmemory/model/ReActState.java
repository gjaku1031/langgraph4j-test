package com.example.langgraph4j.examples.reactmemory.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ReAct + Memory 시스템의 상태를 나타내는 클래스
 * 
 * Python 예제의 GraphState(MessagesState)를 Java로 구현한 클래스입니다.
 * 추론(Reasoning)과 행동(Acting) 사이클을 관리합니다.
 * 
 * @author Claude AI Assistant
 * @since 2025-07-31
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReActState {
    
    /**
     * 대화 메시지 목록 (사용자 입력, AI 응답, 도구 호출 결과)
     */
    @Builder.Default
    private List<ReActMessage> messages = new ArrayList<>();
    
    /**
     * 현재 처리 단계
     */
    private String currentStep;
    
    /**
     * 스레드 ID (대화 세션 식별자)
     */
    private String threadId;
    
    /**
     * 도구 호출 결과들
     */
    @Builder.Default
    private List<ToolCall> toolCalls = new ArrayList<>();
    
    /**
     * 추론 단계 정보
     */
    private String reasoning;
    
    /**
     * 행동 단계 정보  
     */
    private String action;
    
    /**
     * 관찰 결과
     */
    private String observation;
    
    /**
     * 메모리 체크포인트 정보
     */
    private String checkpointId;
    
    /**
     * 상태 생성 시간
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    
    /**
     * 메시지 추가
     */
    public void addMessage(ReActMessage message) {
        if (this.messages == null) {
            this.messages = new ArrayList<>();
        }
        this.messages.add(message);
    }
    
    /**
     * 도구 호출 추가
     */
    public void addToolCall(ToolCall toolCall) {
        if (this.toolCalls == null) {
            this.toolCalls = new ArrayList<>();
        }
        this.toolCalls.add(toolCall);
    }
    
    /**
     * 마지막 메시지 가져오기
     */
    public ReActMessage getLastMessage() {
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        return messages.get(messages.size() - 1);
    }
    
    /**
     * 마지막 사용자 메시지 가져오기
     */
    public ReActMessage getLastUserMessage() {
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        
        for (int i = messages.size() - 1; i >= 0; i--) {
            ReActMessage message = messages.get(i);
            if (message.getType() == ReActMessage.MessageType.USER) {
                return message;
            }
        }
        return null;
    }
    
    /**
     * 도구 호출이 필요한지 확인
     */
    public boolean needsToolCall() {
        ReActMessage lastMessage = getLastMessage();
        return lastMessage != null && 
               lastMessage.getType() == ReActMessage.MessageType.AI &&
               lastMessage.getContent().contains("Tool Call:");
    }
    
    /**
     * 상태 요약 정보 생성
     */
    public String getSummary() {
        int messageCount = messages != null ? messages.size() : 0;
        int toolCallCount = toolCalls != null ? toolCalls.size() : 0;
        
        return String.format("메시지: %d개, 도구 호출: %d회, 단계: %s", 
            messageCount, toolCallCount, currentStep);
    }
}