package com.example.langgraph4j.examples.agenticrag.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Agentic RAG 시스템의 상태를 나타내는 클래스
 * 
 * Python 예제의 GraphState를 Java로 구현한 클래스입니다.
 * 문서 검색, 쿼리 개선, 답변 생성 과정을 관리합니다.
 * 
 * @author Claude AI Assistant
 * @since 2025-07-31
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgenticRAGState {
    
    /**
     * 원본 사용자 질문
     */
    private String originalQuery;
    
    /**
     * 개선된 질문 (쿼리 재작성 결과)
     */
    private String refinedQuery;
    
    /**
     * 검색된 문서들
     */
    @Builder.Default
    private List<Document> documents = new ArrayList<>();
    
    /**
     * 선택된 관련 문서들
     */
    @Builder.Default
    private List<Document> relevantDocuments = new ArrayList<>();
    
    /**
     * 생성된 답변
     */
    private String answer;
    
    /**
     * 답변 품질 점수
     */
    private Double qualityScore;
    
    /**
     * 현재 처리 단계
     */
    private ProcessingStep currentStep;
    
    /**
     * 검색 쿼리 목록 (다양한 검색 시도)
     */
    @Builder.Default
    private List<String> searchQueries = new ArrayList<>();
    
    /**
     * 답변 생성 시도 횟수
     */
    @Builder.Default
    private Integer generationAttempts = 0;
    
    /**
     * 최대 허용 시도 횟수
     */
    @Builder.Default
    private Integer maxAttempts = 3;
    
    /**
     * 오류 메시지
     */
    private String errorMessage;
    
    /**
     * 처리 시작 시간
     */
    @Builder.Default
    private LocalDateTime startTime = LocalDateTime.now();
    
    /**
     * 처리 완료 시간
     */
    private LocalDateTime endTime;
    
    /**
     * 세션 ID (대화 추적용)
     */
    private String sessionId;
    
    /**
     * 처리 단계 열거형
     */
    public enum ProcessingStep {
        STARTED,              // 시작
        QUERY_ANALYSIS,       // 쿼리 분석
        QUERY_REFINEMENT,     // 쿼리 개선
        DOCUMENT_RETRIEVAL,   // 문서 검색
        RELEVANCE_FILTERING,  // 관련성 필터링
        ANSWER_GENERATION,    // 답변 생성
        QUALITY_EVALUATION,   // 품질 평가
        COMPLETED,            // 완료
        FAILED                // 실패
    }
    
    /**
     * 문서 추가
     */
    public void addDocument(Document document) {
        if (this.documents == null) {
            this.documents = new ArrayList<>();
        }
        this.documents.add(document);
    }
    
    /**
     * 관련 문서 추가
     */
    public void addRelevantDocument(Document document) {
        if (this.relevantDocuments == null) {
            this.relevantDocuments = new ArrayList<>();
        }
        this.relevantDocuments.add(document);
    }
    
    /**
     * 검색 쿼리 추가
     */
    public void addSearchQuery(String query) {
        if (this.searchQueries == null) {
            this.searchQueries = new ArrayList<>();
        }
        this.searchQueries.add(query);
    }
    
    /**
     * 시도 횟수 증가
     */
    public void incrementAttempts() {
        if (this.generationAttempts == null) {
            this.generationAttempts = 0;
        }
        this.generationAttempts++;
    }
    
    /**
     * 최대 시도 횟수 도달 여부 확인
     */
    public boolean hasReachedMaxAttempts() {
        return generationAttempts != null && maxAttempts != null && 
               generationAttempts >= maxAttempts;
    }
    
    /**
     * 처리 완료 표시
     */
    public void markCompleted() {
        this.currentStep = ProcessingStep.COMPLETED;
        this.endTime = LocalDateTime.now();
    }
    
    /**
     * 처리 실패 표시
     */
    public void markFailed(String errorMessage) {
        this.currentStep = ProcessingStep.FAILED;
        this.errorMessage = errorMessage;
        this.endTime = LocalDateTime.now();
    }
    
    /**
     * 처리 시간 계산 (초 단위)
     */
    public long getProcessingTimeSeconds() {
        if (startTime == null) {
            return 0;
        }
        
        LocalDateTime end = endTime != null ? endTime : LocalDateTime.now();
        return java.time.Duration.between(startTime, end).getSeconds();
    }
    
    /**
     * 상태 요약 정보 생성
     */
    public String getSummary() {
        return String.format(
            "단계: %s, 문서: %d개, 관련문서: %d개, 시도: %d/%d, 처리시간: %d초",
            currentStep,
            documents != null ? documents.size() : 0,
            relevantDocuments != null ? relevantDocuments.size() : 0,
            generationAttempts != null ? generationAttempts : 0,
            maxAttempts != null ? maxAttempts : 0,
            getProcessingTimeSeconds()
        );
    }
    
    /**
     * 진행률 계산 (백분율)
     */
    public double getProgressPercentage() {
        if (currentStep == null) {
            return 0.0;
        }
        
        switch (currentStep) {
            case STARTED: return 0.0;
            case QUERY_ANALYSIS: return 10.0;
            case QUERY_REFINEMENT: return 25.0;
            case DOCUMENT_RETRIEVAL: return 50.0;
            case RELEVANCE_FILTERING: return 65.0;
            case ANSWER_GENERATION: return 80.0;
            case QUALITY_EVALUATION: return 95.0;
            case COMPLETED: return 100.0;
            case FAILED: return 0.0;
            default: return 0.0;
        }
    }
    
    /**
     * 검색 결과가 충분한지 확인
     */
    public boolean hasSufficientDocuments() {
        return relevantDocuments != null && relevantDocuments.size() >= 1;
    }
    
    /**
     * 답변 품질이 충분한지 확인
     */
    public boolean hasSufficientQuality() {
        return qualityScore != null && qualityScore >= 0.7; // 70% 이상
    }
    
    /**
     * 상태 초기화 (재시도용)
     */
    public void reset() {
        this.documents.clear();
        this.relevantDocuments.clear();
        this.searchQueries.clear();
        this.answer = null;
        this.qualityScore = null;
        this.generationAttempts = 0;
        this.errorMessage = null;
        this.currentStep = ProcessingStep.STARTED;
        this.startTime = LocalDateTime.now();
        this.endTime = null;
    }
}