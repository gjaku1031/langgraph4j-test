package com.example.langgraph4j.examples.stategraph.controller;

import com.example.langgraph4j.examples.stategraph.model.MenuState;
import com.example.langgraph4j.examples.stategraph.service.StateGraphService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * StateGraph 예제를 위한 REST API 컨트롤러
 * 
 * Python 예제의 StateGraph 기능을 웹 API로 제공합니다.
 * 
 * @author Claude AI Assistant
 * @since 2025-07-31
 */
@Slf4j
@RestController
@RequestMapping("/api/examples/stategraph")
@RequiredArgsConstructor
public class StateGraphController {

    private final StateGraphService stateGraphService;

    /**
     * 기본 StateGraph 실행 (선형 흐름)
     * 
     * @return 메뉴 추천 결과
     */
    @PostMapping("/basic")
    public ResponseEntity<StateGraphResponse> executeBasicStateGraph() {
        log.info("기본 StateGraph 실행 요청");
        
        try {
            MenuState result = stateGraphService.executeBasicStateGraph();
            
            StateGraphResponse response = StateGraphResponse.builder()
                .success(true)
                .type("basic")
                .state(result)
                .summary(String.format(
                    "선호도: %s → 추천 메뉴: %s → 정보: %s",
                    result.getUserPreference(),
                    result.getRecommendedMenu(), 
                    result.getMenuInfo()
                ))
                .timestamp(LocalDateTime.now())
                .build();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("기본 StateGraph 실행 중 오류 발생", e);
            
            StateGraphResponse errorResponse = StateGraphResponse.builder()
                .success(false)
                .type("basic")
                .error("StateGraph 실행 중 오류가 발생했습니다: " + e.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 고급 StateGraph 실행 (조건부 라우팅)
     * 
     * @param request 사용자 질문이 포함된 요청
     * @return 질의응답 결과
     */
    @PostMapping("/advanced")
    public ResponseEntity<StateGraphResponse> executeAdvancedStateGraph(
            @RequestBody StateGraphRequest request) {
        log.info("고급 StateGraph 실행 요청: {}", request.getQuery());
        
        try {
            MenuState result = stateGraphService.executeAdvancedStateGraph(request.getQuery());
            
            String routingPath = Boolean.TRUE.equals(result.getIsMenuRelated()) ? 
                "메뉴 관련 처리" : "일반 질문 처리";
            
            StateGraphResponse response = StateGraphResponse.builder()
                .success(true)
                .type("advanced")
                .query(request.getQuery())
                .state(result)
                .summary(String.format(
                    "질문: %s → 분석: %s → %s → 응답: %s",
                    result.getUserQuery(),
                    result.getIsMenuRelated() ? "메뉴 관련" : "일반 질문",
                    routingPath,
                    result.getFinalAnswer() != null ? 
                        (result.getFinalAnswer().length() > 50 ? 
                            result.getFinalAnswer().substring(0, 50) + "..." : 
                            result.getFinalAnswer()) : "응답 없음"
                ))
                .timestamp(LocalDateTime.now())
                .build();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("고급 StateGraph 실행 중 오류 발생", e);
            
            StateGraphResponse errorResponse = StateGraphResponse.builder()
                .success(false)
                .type("advanced")
                .query(request.getQuery())
                .error("StateGraph 실행 중 오류가 발생했습니다: " + e.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 사용 가능한 메뉴 목록 조회
     * 
     * @return 메뉴 목록
     */
    @GetMapping("/menus")
    public ResponseEntity<Map<String, String>> getAvailableMenus() {
        log.info("사용 가능한 메뉴 목록 조회");
        return ResponseEntity.ok(stateGraphService.getAvailableMenus());
    }

    /**
     * 메뉴 정보 데이터베이스 조회
     * 
     * @return 메뉴 정보
     */
    @GetMapping("/menu-info")
    public ResponseEntity<Map<String, String>> getMenuInfo() {
        log.info("메뉴 정보 데이터베이스 조회");
        return ResponseEntity.ok(stateGraphService.getMenuInfoDatabase());
    }

    /**
     * StateGraph 요청 모델
     */
    @Data
    public static class StateGraphRequest {
        private String query;
    }

    /**
     * StateGraph 응답 모델
     */
    @Data
    @lombok.Builder
    public static class StateGraphResponse {
        private boolean success;
        private String type;
        private String query;
        private MenuState state;
        private String summary;
        private String error;
        private LocalDateTime timestamp;
    }
}