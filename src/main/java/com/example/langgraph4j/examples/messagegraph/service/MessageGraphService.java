package com.example.langgraph4j.examples.messagegraph.service;

import com.example.langgraph4j.examples.messagegraph.model.*;
import com.example.langgraph4j.examples.toolcalling.tools.RestaurantSearchTools;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * MessageGraph 기반 품질 제어 RAG 서비스
 * 
 * Python 예제의 MessageGraph 로직을 Java로 구현한 서비스입니다.
 * 메시지 기반 대화 흐름과 자동 품질 제어 및 재시도 기능을 제공합니다.
 * 
 * @author Claude AI Assistant
 * @since 2025-07-31
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageGraphService {

    @Autowired
    private ChatClient chatClient;
    
    @Autowired
    private RestaurantSearchTools restaurantSearchTools;
    
    // 최대 재시도 횟수
    private static final int MAX_GENERATIONS = 3;
    
    // 품질 임계값
    private static final double QUALITY_THRESHOLD = 0.7;

    /**
     * MessageGraph 실행: 품질 제어가 있는 RAG 시스템
     * 
     * @param userQuery 사용자 질문
     * @return 완성된 GraphState
     */
    public GraphState executeMessageGraph(String userQuery) {
        log.info("=== MessageGraph 시작: {} ===", userQuery);
        
        // 초기 상태 생성
        GraphState state = GraphState.builder()
            .currentStep("start")
            .build();
        
        // 사용자 메시지 추가
        state.addMessage(new HumanMessage(userQuery));
        
        // 품질이 만족스러울 때까지 반복
        while (shouldContinue(state)) {
            // 1단계: 문서 검색 및 응답 생성
            state = retrieveAndRespond(state);
            
            // 2단계: 응답 품질 평가
            state = gradeAnswer(state);
            
            // 3단계: 재시도 여부 결정
            if (!shouldRetry(state)) {
                break;
            }
            
            log.info("품질이 낮아 재시도합니다. 현재 시도: {}, 점수: {}", 
                state.getNumGeneration(), state.getGrade());
        }
        
        state.setCurrentStep("completed");
        log.info("=== MessageGraph 완료. 최종 점수: {} ===", state.getGrade());
        
        return state;
    }

    /**
     * 1단계: 문서 검색 및 응답 생성
     */
    private GraphState retrieveAndRespond(GraphState state) {
        log.info("---문서 검색 및 응답 생성---");
        
        Message lastHumanMessage = state.getLastHumanMessage();
        if (lastHumanMessage == null) {
            log.error("사용자 메시지를 찾을 수 없습니다.");
            return state;
        }
        
        String query = lastHumanMessage.getContent();
        
        try {
            // 1. 문서 검색 (메뉴 및 와인 정보)
            List<String> retrievedDocs = retrieveDocuments(query);
            state.addDocuments(retrievedDocs);
            
            // 2. RAG 기반 응답 생성
            String response = generateRagResponse(query, retrievedDocs);
            state.addMessage(new AiMessage(response));
            
            state.incrementGeneration();
            state.setCurrentStep("response_generated");
            
            log.info("응답 생성 완료. 시도 횟수: {}", state.getNumGeneration());
            
        } catch (Exception e) {
            log.error("문서 검색 및 응답 생성 중 오류 발생", e);
            state.addMessage(new AiMessage("죄송합니다. 응답 생성 중 오류가 발생했습니다."));
            state.incrementGeneration();
        }
        
        return state;
    }

    /**
     * 2단계: 응답 품질 평가
     */
    private GraphState gradeAnswer(GraphState state) {
        log.info("---응답 품질 평가---");
        
        Message lastHumanMessage = state.getLastHumanMessage();
        Message lastAiMessage = state.getLastAiMessage();
        
        if (lastHumanMessage == null || lastAiMessage == null) {
            log.error("평가할 메시지를 찾을 수 없습니다.");
            state.setGrade(0.0);
            return state;
        }
        
        try {
            String question = lastHumanMessage.getContent();
            String answer = lastAiMessage.getContent();
            String context = String.join("\n", state.getDocuments());
            
            GradeResponse gradeResponse = evaluateAnswerQuality(question, answer, context);
            
            state.setGrade(gradeResponse.getScore());
            state.setGradeExplanation(gradeResponse.getExplanation());
            state.setCurrentStep("answer_graded");
            
            log.info("품질 평가 완료. 점수: {}, 설명: {}", 
                gradeResponse.getScore(), gradeResponse.getExplanation());
            
        } catch (Exception e) {
            log.error("품질 평가 중 오류 발생", e);
            state.setGrade(0.5); // 기본값
            state.setGradeExplanation("평가 중 오류가 발생했습니다.");
        }
        
        return state;
    }

    /**
     * 문서 검색 수행
     */
    private List<String> retrieveDocuments(String query) {
        List<String> documents = new ArrayList<>();
        
        // 메뉴 검색
        String menuResults = restaurantSearchTools.searchMenu(query);
        if (!menuResults.contains("찾을 수 없습니다")) {
            documents.add("메뉴 정보: " + menuResults);
        }
        
        // 와인 검색
        String wineResults = restaurantSearchTools.searchWine(query);
        if (!wineResults.contains("찾을 수 없습니다")) {
            documents.add("와인 정보: " + wineResults);
        }
        
        if (documents.isEmpty()) {
            documents.add("관련 정보를 찾을 수 없습니다.");
        }
        
        return documents;
    }

    /**
     * RAG 기반 응답 생성
     */
    private String generateRagResponse(String query, List<String> documents) {
        String context = String.join("\n\n", documents);
        
        String prompt = String.format(
            "다음은 레스토랑 관련 정보입니다:\n%s\n\n" +
            "사용자 질문: %s\n\n" +
            "위 정보를 바탕으로 사용자의 질문에 정확하고 친절하게 답변해주세요. " +
            "정보가 부족하면 일반적인 지식을 활용하되, 가능한 한 제공된 정보를 우선적으로 사용하세요.",
            context, query
        );

        return chatClient.prompt()
            .user(prompt)
            .call()
            .content();
    }

    /**
     * 응답 품질 평가
     */
    private GradeResponse evaluateAnswerQuality(String question, String answer, String context) {
        String prompt = String.format(
            "다음 질문-답변 쌍의 품질을 0.0에서 1.0 사이의 점수로 평가해주세요.\n\n" +
            "질문: %s\n\n" +
            "제공된 컨텍스트: %s\n\n" +
            "답변: %s\n\n" +
            "평가 기준:\n" +
            "- 답변이 질문에 직접적으로 대답하는가? (0.3점)\n" +
            "- 제공된 컨텍스트 정보를 적절히 활용했는가? (0.3점)\n" +
            "- 답변이 정확하고 유용한가? (0.2점)\n" +
            "- 답변이 친절하고 이해하기 쉬운가? (0.2점)\n\n" +
            "응답 형식:\n" +
            "점수: [0.0-1.0]\n" +
            "설명: [평가 이유]",
            question, context, answer
        );

        try {
            String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();
            
            return parseGradeResponse(response);
            
        } catch (Exception e) {
            log.error("품질 평가 요청 실패", e);
            return GradeResponse.builder()
                .score(0.5)
                .explanation("평가 시스템 오류")
                .build();
        }
    }

    /**
     * 품질 평가 응답 파싱
     */
    private GradeResponse parseGradeResponse(String response) {
        try {
            String[] lines = response.split("\n");
            Double score = null;
            String explanation = "";
            
            for (String line : lines) {
                if (line.startsWith("점수:")) {
                    String scoreStr = line.substring(3).trim();
                    score = Double.parseDouble(scoreStr);
                } else if (line.startsWith("설명:")) {
                    explanation = line.substring(3).trim();
                }
            }
            
            if (score == null) {
                // 응답에서 점수를 찾을 수 없으면 기본값 사용
                score = 0.5;
                explanation = "점수를 파싱할 수 없습니다: " + response;
            }
            
            return GradeResponse.builder()
                .score(Math.max(0.0, Math.min(1.0, score))) // 0.0-1.0 범위로 제한
                .explanation(explanation)
                .build();
                
        } catch (Exception e) {
            log.error("품질 응답 파싱 실패: {}", response, e);
            return GradeResponse.builder()
                .score(0.5)
                .explanation("파싱 오류: " + e.getMessage())
                .build();
        }
    }

    /**
     * 계속 진행할지 결정
     */
    private boolean shouldContinue(GraphState state) {
        return state.getNumGeneration() == 0; // 최소 한 번은 실행
    }

    /**
     * 재시도할지 결정
     */
    private boolean shouldRetry(GraphState state) {
        // 최대 재시도 횟수 초과
        if (state.getNumGeneration() >= MAX_GENERATIONS) {
            log.info("최대 재시도 횟수 도달: {}", state.getNumGeneration());
            return false;
        }
        
        // 품질이 충분히 높음
        if (state.getGrade() != null && state.getGrade() >= QUALITY_THRESHOLD) {
            log.info("품질 기준 충족: {}", state.getGrade());
            return false;
        }
        
        return true;
    }

    /**
     * 현재 설정 정보 반환
     */
    public String getConfiguration() {
        return String.format(
            "최대 재시도 횟수: %d, 품질 임계값: %.1f",
            MAX_GENERATIONS, QUALITY_THRESHOLD
        );
    }
}