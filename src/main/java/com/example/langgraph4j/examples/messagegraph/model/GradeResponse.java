package com.example.langgraph4j.examples.messagegraph.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 응답 품질 평가 결과
 * 
 * Python 예제의 GradeResponse Pydantic 모델을 Java로 구현한 클래스입니다.
 * 
 * @author Claude AI Assistant
 * @since 2025-07-31
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeResponse {
    
    /**
     * 품질 점수 (0.0 ~ 1.0)
     */
    private Double score;
    
    /**
     * 점수에 대한 설명
     */
    private String explanation;
    
    /**
     * 점수가 유효한지 검증
     */
    public boolean isValidScore() {
        return score != null && score >= 0.0 && score <= 1.0;
    }
    
    /**
     * 고품질 응답인지 확인 (0.7 이상)
     */
    public boolean isHighQuality() {
        return isValidScore() && score >= 0.7;
    }
}