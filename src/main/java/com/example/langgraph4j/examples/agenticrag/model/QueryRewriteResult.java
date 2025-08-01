package com.example.langgraph4j.examples.agenticrag.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 쿼리 재작성 결과를 나타내는 모델 클래스
 * 
 * Python 예제의 query rewriting 결과를 Java로 구현한 클래스입니다.
 * 
 * @author Claude AI Assistant
 * @since 2025-07-31
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryRewriteResult {
    
    /**
     * 원본 쿼리
     */
    private String originalQuery;
    
    /**
     * 재작성된 쿼리
     */
    private String rewrittenQuery;
    
    /**
     * 재작성 이유
     */
    private String reason;
    
    /**
     * 추출된 키워드들
     */
    private List<String> extractedKeywords;
    
    /**
     * 쿼리 의도 분류
     */
    private QueryIntent intent;
    
    /**
     * 재작성 신뢰도 점수 (0.0 ~ 1.0)
     */
    private Double confidenceScore;
    
    /**
     * 생성 시간
     */
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    /**
     * 쿼리 의도 열거형
     */
    public enum QueryIntent {
        MENU_SEARCH,        // 메뉴 검색
        WINE_PAIRING,       // 와인 페어링
        RECIPE_INQUIRY,     // 레시피 문의
        PRICE_INQUIRY,      // 가격 문의
        GENERAL_QUESTION,   // 일반 질문
        COMPARISON,         // 비교 질문
        RECOMMENDATION      // 추천 요청
    }
    
    /**
     * 재작성이 유효한지 확인
     */
    public boolean isValid() {
        return rewrittenQuery != null && 
               !rewrittenQuery.trim().isEmpty() && 
               confidenceScore != null && 
               confidenceScore >= 0.5;
    }
    
    /**
     * 키워드가 포함되어 있는지 확인
     */
    public boolean hasKeywords() {
        return extractedKeywords != null && !extractedKeywords.isEmpty();
    }
    
    /**
     * 특정 의도인지 확인
     */
    public boolean hasIntent(QueryIntent targetIntent) {
        return intent != null && intent.equals(targetIntent);
    }
    
    /**
     * 결과를 문자열로 포맷팅
     */
    public String format() {
        StringBuilder sb = new StringBuilder();
        sb.append("원본 쿼리: ").append(originalQuery).append("\n");
        sb.append("재작성 쿼리: ").append(rewrittenQuery).append("\n");
        sb.append("의도: ").append(intent).append("\n");
        if (reason != null) {
            sb.append("재작성 이유: ").append(reason).append("\n");
        }
        if (hasKeywords()) {
            sb.append("추출 키워드: ").append(String.join(", ", extractedKeywords)).append("\n");
        }
        if (confidenceScore != null) {
            sb.append("신뢰도: ").append(String.format("%.2f", confidenceScore));
        }
        return sb.toString();
    }
    
    /**
     * 간단한 요약 정보
     */
    public String getSummary() {
        return String.format("%s → %s (신뢰도: %.2f, 의도: %s)", 
            originalQuery, 
            rewrittenQuery, 
            confidenceScore != null ? confidenceScore : 0.0,
            intent);
    }
}