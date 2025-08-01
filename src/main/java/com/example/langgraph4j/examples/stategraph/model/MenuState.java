package com.example.langgraph4j.examples.stategraph.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 메뉴 추천 StateGraph의 상태를 나타내는 클래스
 * 
 * Python 예제의 MenuState TypedDict를 Java 클래스로 구현했습니다.
 * 
 * @author Claude AI Assistant
 * @since 2025-07-31
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuState {
    
    /**
     * 사용자의 음식 선호도 (육류, 해산물, 채식, 아무거나)
     */
    private String userPreference;
    
    /**
     * 추천된 메뉴 항목
     */
    private String recommendedMenu;
    
    /**
     * 추천 메뉴에 대한 상세 정보 (설명, 가격 등)
     */
    private String menuInfo;
    
    /**
     * 사용자 질문 (고급 예제용)
     */
    private String userQuery;
    
    /**
     * 질문이 메뉴 관련인지 여부 (고급 예제용)
     */
    private Boolean isMenuRelated;
    
    /**
     * 검색 결과 목록 (고급 예제용)
     */
    private List<String> searchResults;
    
    /**
     * 최종 답변 (고급 예제용)
     */
    private String finalAnswer;
    
    /**
     * 현재 단계 식별자
     */
    private String currentStep;
}