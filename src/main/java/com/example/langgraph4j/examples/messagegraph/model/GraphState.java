package com.example.langgraph4j.examples.messagegraph.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * MessageGraph의 상태를 나타내는 클래스
 * 
 * Python 예제의 GraphState(MessagesState)를 Java로 구현한 클래스입니다.
 * 메시지 기반 대화 흐름과 품질 제어 기능을 포함합니다.
 * 
 * @author Claude AI Assistant
 * @since 2025-07-31
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphState {
    
    /**
     * 대화 메시지 목록 (사용자와 AI 메시지가 축적됨)
     */
    @Builder.Default
    private List<Message> messages = new ArrayList<>();
    
    /**
     * 검색된 문서 목록
     */
    @Builder.Default
    private List<String> documents = new ArrayList<>();
    
    /**
     * 응답 품질 점수 (0.0 ~ 1.0)
     */
    private Double grade;
    
    /**
     * 응답 생성 시도 횟수
     */
    @Builder.Default
    private Integer numGeneration = 0;
    
    /**
     * 현재 처리 단계
     */
    private String currentStep;
    
    /**
     * 품질 평가 설명
     */
    private String gradeExplanation;
    
    /**
     * 메시지 추가 (add_messages 리듀서 패턴)
     */
    public void addMessage(Message message) {
        if (this.messages == null) {
            this.messages = new ArrayList<>();
        }
        this.messages.add(message);
    }
    
    /**
     * 메시지 리스트 추가 (여러 메시지 한 번에)
     */
    public void addMessages(List<Message> newMessages) {
        if (this.messages == null) {
            this.messages = new ArrayList<>();
        }
        this.messages.addAll(newMessages);
    }
    
    /**
     * 문서 추가 (축적 패턴)
     */
    public void addDocument(String document) {
        if (this.documents == null) {
            this.documents = new ArrayList<>();
        }
        this.documents.add(document);
    }
    
    /**
     * 문서 리스트 추가
     */
    public void addDocuments(List<String> newDocuments) {
        if (this.documents == null) {
            this.documents = new ArrayList<>();
        }
        this.documents.addAll(newDocuments);
    }
    
    /**
     * 마지막 사용자 메시지 가져오기
     */
    public Message getLastHumanMessage() {
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message message = messages.get(i);
            if (message instanceof HumanMessage) {
                return message;
            }
        }
        return null;
    }
    
    /**
     * 마지막 AI 메시지 가져오기
     */
    public Message getLastAiMessage() {
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message message = messages.get(i);
            if (message instanceof AiMessage) {
                return message;
            }
        }
        return null;
    }
    
    /**
     * 생성 횟수 증가
     */
    public void incrementGeneration() {
        if (this.numGeneration == null) {
            this.numGeneration = 0;
        }
        this.numGeneration++;
    }
}