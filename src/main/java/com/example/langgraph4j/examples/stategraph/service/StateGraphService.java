package com.example.langgraph4j.examples.stategraph.service;

import com.example.langgraph4j.examples.stategraph.model.MenuState;
// LangChain4j imports removed - using Spring AI instead
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
// VectorStore will be implemented later if needed
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * StateGraph 기반 메뉴 추천 서비스
 * 
 * Python 예제의 StateGraph 로직을 Java로 구현한 서비스입니다.
 * 기본적인 선형 흐름과 조건부 라우팅을 모두 지원합니다.
 * 
 * @author Claude AI Assistant
 * @since 2025-07-31
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StateGraphService {

    @Autowired
    private ChatClient chatClient;
    
    // 메뉴 데이터베이스 (실제로는 Vector Store나 DB에서 가져옴)
    private static final Map<String, String> MENU_DATABASE = Map.of(
        "육류", "스테이크",
        "해산물", "연어구이", 
        "채식", "퀴노아 샐러드",
        "아무거나", "오늘의 추천 파스타"
    );
    
    private static final Map<String, String> MENU_INFO_DATABASE = Map.of(
        "스테이크", "최상급 소고기로 만든 juicy한 스테이크입니다. 가격: 35,000원",
        "연어구이", "신선한 연어에 허브를 곁들인 건강한 요리입니다. 가격: 28,000원",
        "퀴노아 샐러드", "영양가 높은 퀴노아와 신선한 채소의 조합입니다. 가격: 18,000원", 
        "오늘의 추천 파스타", "셰프가 특별히 준비한 오늘의 파스타입니다. 가격: 22,000원"
    );

    /**
     * 기본 StateGraph 실행: 선형 흐름으로 메뉴 추천
     * 
     * @return 완성된 MenuState
     */
    public MenuState executeBasicStateGraph() {
        log.info("=== 기본 StateGraph 시작 ===");
        
        // 초기 상태 생성
        MenuState state = MenuState.builder()
            .currentStep("start")
            .build();
        
        // 1단계: 사용자 선호도 생성
        state = getUserPreference(state);
        
        // 2단계: 메뉴 추천
        state = recommendMenu(state);
        
        // 3단계: 메뉴 정보 제공
        state = provideMenuInfo(state);
        
        log.info("=== 기본 StateGraph 완료 ===");
        return state;
    }

    /**
     * 고급 StateGraph 실행: 조건부 라우팅으로 메뉴 질의응답
     * 
     * @param userQuery 사용자 질문
     * @return 완성된 MenuState
     */
    public MenuState executeAdvancedStateGraph(String userQuery) {
        log.info("=== 고급 StateGraph 시작: {} ===", userQuery);
        
        // 초기 상태 생성
        MenuState state = MenuState.builder()
            .userQuery(userQuery)
            .currentStep("start")
            .build();
        
        // 1단계: 입력 분석
        state = analyzeInput(state);
        
        // 2단계: 조건부 라우팅
        if (Boolean.TRUE.equals(state.getIsMenuRelated())) {
            // 메뉴 관련 질문 처리
            state = searchMenuInfo(state);
            state = generateMenuResponse(state);
        } else {
            // 일반 질문 처리
            state = generateGeneralResponse(state);
        }
        
        log.info("=== 고급 StateGraph 완료 ===");
        return state;
    }

    /**
     * 1단계: 랜덤하게 사용자 선호도 생성
     */
    private MenuState getUserPreference(MenuState state) {
        log.info("---랜덤 사용자 선호도 생성---");
        
        String[] preferences = {"육류", "해산물", "채식", "아무거나"};
        String randomPreference = preferences[ThreadLocalRandom.current().nextInt(preferences.length)];
        
        log.info("생성된 선호도: {}", randomPreference);
        
        state.setUserPreference(randomPreference);
        state.setCurrentStep("preference_generated");
        
        return state;
    }

    /**
     * 2단계: 선호도를 기반으로 메뉴 추천
     */
    private MenuState recommendMenu(MenuState state) {
        log.info("---메뉴 추천---");
        
        String recommendedMenu = MENU_DATABASE.get(state.getUserPreference());
        if (recommendedMenu == null) {
            recommendedMenu = "오늘의 추천 파스타"; // 기본값
        }
        
        log.info("추천 메뉴: {}", recommendedMenu);
        
        state.setRecommendedMenu(recommendedMenu);
        state.setCurrentStep("menu_recommended");
        
        return state;
    }

    /**
     * 3단계: 추천 메뉴에 대한 상세 정보 제공
     */
    private MenuState provideMenuInfo(MenuState state) {
        log.info("---메뉴 정보 제공---");
        
        String menuInfo = MENU_INFO_DATABASE.get(state.getRecommendedMenu());
        if (menuInfo == null) {
            menuInfo = "죄송합니다. 해당 메뉴 정보를 찾을 수 없습니다.";
        }
        
        log.info("메뉴 정보: {}", menuInfo);
        
        state.setMenuInfo(menuInfo);
        state.setCurrentStep("completed");
        
        return state;
    }

    /**
     * 고급 버전 1단계: 사용자 입력이 메뉴 관련인지 분석
     */
    private MenuState analyzeInput(MenuState state) {
        log.info("---입력 분석 중---");
        
        try {
            String prompt = String.format(
                "다음 질문이 레스토랑 메뉴와 관련된 질문인지 판단해주세요. " +
                "메뉴, 음식, 가격, 재료, 추천 등과 관련된 질문이면 'YES', " +
                "그렇지 않으면 'NO'라고만 답해주세요.\n\n질문: %s",
                state.getUserQuery()
            );

            String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

            boolean isMenuRelated = response.trim().toUpperCase().contains("YES");
            
            log.info("메뉴 관련 여부: {}", isMenuRelated);
            
            state.setIsMenuRelated(isMenuRelated);
            state.setCurrentStep("input_analyzed");
            
        } catch (Exception e) {
            log.error("입력 분석 중 오류 발생", e);
            // 에러 시 메뉴 관련으로 간주
            state.setIsMenuRelated(true);
        }
        
        return state;
    }

    /**
     * 고급 버전 2단계: 메뉴 정보 검색 (간단한 키워드 매칭)
     */
    private MenuState searchMenuInfo(MenuState state) {
        log.info("---메뉴 정보 검색---");
        
        List<String> searchResults = new ArrayList<>();
        String query = state.getUserQuery().toLowerCase();
        
        // 간단한 키워드 매칭으로 검색
        MENU_INFO_DATABASE.forEach((menu, info) -> {
            if (query.contains(menu.toLowerCase()) || 
                info.toLowerCase().contains(query) ||
                query.contains("메뉴") || query.contains("추천")) {
                searchResults.add(String.format("%s: %s", menu, info));
            }
        });
        
        if (searchResults.isEmpty()) {
            searchResults.add("관련 메뉴 정보를 찾을 수 없습니다.");
        }
        
        log.info("검색 결과: {} 개", searchResults.size());
        
        state.setSearchResults(searchResults);
        state.setCurrentStep("menu_searched");
        
        return state;
    }

    /**
     * 고급 버전 3단계: 검색 결과를 기반으로 메뉴 응답 생성
     */
    private MenuState generateMenuResponse(MenuState state) {
        log.info("---메뉴 응답 생성---");
        
        try {
            String searchContext = String.join("\n", state.getSearchResults());
            
            String prompt = String.format(
                "다음은 레스토랑 메뉴 정보입니다:\n%s\n\n" +
                "사용자 질문: %s\n\n" +
                "위 메뉴 정보를 바탕으로 사용자의 질문에 친절하고 정확하게 답변해주세요.",
                searchContext, state.getUserQuery()
            );

            String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();
            
            log.info("메뉴 응답 생성 완료");
            
            state.setFinalAnswer(response);
            state.setCurrentStep("menu_response_generated");
            
        } catch (Exception e) {
            log.error("메뉴 응답 생성 중 오류 발생", e);
            state.setFinalAnswer("죄송합니다. 메뉴 정보를 처리하는 중 오류가 발생했습니다.");
        }
        
        return state;
    }

    /**
     * 고급 버전 3단계: 일반 질문에 대한 응답 생성
     */
    private MenuState generateGeneralResponse(MenuState state) {
        log.info("---일반 응답 생성---");
        
        try {
            String response = chatClient.prompt()
                .user(state.getUserQuery())
                .call()
                .content();
            
            log.info("일반 응답 생성 완료");
            
            state.setFinalAnswer(response);
            state.setCurrentStep("general_response_generated");
            
        } catch (Exception e) {
            log.error("일반 응답 생성 중 오류 발생", e);
            state.setFinalAnswer("죄송합니다. 답변을 생성하는 중 오류가 발생했습니다.");
        }
        
        return state;
    }

    /**
     * 사용 가능한 메뉴 목록 반환
     */
    public Map<String, String> getAvailableMenus() {
        return MENU_DATABASE;
    }

    /**
     * 메뉴 정보 데이터베이스 반환
     */
    public Map<String, String> getMenuInfoDatabase() {
        return MENU_INFO_DATABASE;
    }
}