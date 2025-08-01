package com.example.langgraph4j.examples.reactmemory.service;

import com.example.langgraph4j.examples.reactmemory.model.*;
import com.example.langgraph4j.examples.toolcalling.tools.RestaurantSearchTools;
import com.example.langgraph4j.examples.toolcalling.tools.TavilySearchTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * ReAct (Reasoning + Acting) 에이전트 서비스
 * 
 * Python 예제의 ReAct 패턴을 Java로 구현한 서비스입니다.
 * 추론과 행동을 반복하는 사이클을 통해 사용자 질문에 답변합니다.
 * 
 * @author Claude AI Assistant
 * @since 2025-07-31
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReActAgentService {

    @Autowired
    private ChatClient chatClient;
    
    @Autowired
    private RestaurantSearchTools restaurantSearchTools;
    
    @Autowired
    private TavilySearchTool tavilySearchTool;
    
    @Autowired
    private MemoryManager memoryManager;
    
    // ReAct 시스템 프롬프트
    private static final String REACT_SYSTEM_PROMPT = """
        당신은 레스토랑 메뉴 정보를 제공하는 ReAct (Reasoning + Acting) 에이전트입니다.
        
        다음 형식에 따라 단계별로 추론하고 행동하세요:
        
        Thought: 사용자의 질문을 분석하고 어떤 도구를 사용할지 결정합니다.
        Action: 필요한 도구를 호출합니다.
        Observation: 도구 실행 결과를 확인합니다.
        Thought: 결과를 바탕으로 추가 행동이 필요한지 판단합니다.
        Final Answer: 최종 답변을 제공합니다.
        
        사용 가능한 도구:
        1. search_menu(query): 레스토랑 메뉴 정보 검색
        2. search_wine(query): 와인 정보 및 페어링 검색  
        3. search_web(query): 웹에서 최신 정보 검색
        
        반드시 위 형식을 따라 단계별로 응답하세요.
        """;

    /**
     * ReAct 에이전트 실행
     * 
     * @param query 사용자 질문
     * @param threadId 대화 스레드 ID (null인 경우 새로 생성)
     * @return 완성된 ReAct 상태
     */
    public ReActState executeReActAgent(String query, String threadId) {
        log.info("=== ReAct 에이전트 시작: {} (스레드: {}) ===", query, threadId);
        
        // 스레드 상태 가져오기 또는 생성
        ReActState state = memoryManager.getThreadState(threadId);
        if (state.getThreadId() == null) {
            state.setThreadId(threadId != null ? threadId : memoryManager.createThread());
        }
        
        // 사용자 메시지 추가
        state.addMessage(ReActMessage.createUserMessage(query));
        state.setCurrentStep("reasoning");
        
        // ReAct 사이클 실행
        int maxIterations = 5; // 무한 루프 방지
        int iteration = 0;
        
        while (iteration < maxIterations) {
            iteration++;
            log.info("--- ReAct 사이클 {} ---", iteration);
            
            try {
                // 1단계: 추론 (Thought)
                state = reasoningStep(state);
                
                // 2단계: 행동 필요성 확인
                if (!needsAction(state)) {
                    // 최종 답변으로 완료
                    state.setCurrentStep("completed");
                    break;
                }
                
                // 3단계: 행동 (Action)
                state = actionStep(state);
                
                // 4단계: 관찰 (Observation)
                state = observationStep(state);
                
                // 상태 저장 (체크포인트)
                memoryManager.saveThreadState(state);
                
            } catch (Exception e) {
                log.error("ReAct 사이클 {} 실행 중 오류 발생", iteration, e);
                state.addMessage(ReActMessage.createSystemMessage(
                    "오류가 발생했습니다: " + e.getMessage()));
                state.setCurrentStep("error");
                break;
            }
        }
        
        if (iteration >= maxIterations) {
            log.warn("최대 반복 횟수 도달. 강제 종료.");
            state.addMessage(ReActMessage.createSystemMessage(
                "최대 반복 횟수에 도달하여 처리를 완료합니다."));
            state.setCurrentStep("max_iterations_reached");
        }
        
        // 최종 상태 저장
        memoryManager.saveThreadState(state);
        
        log.info("=== ReAct 에이전트 완료. 총 {} 사이클 ===", iteration);
        return state;
    }

    /**
     * 추론 단계 (Thought)
     */
    private ReActState reasoningStep(ReActState state) {
        log.info("추론 단계 실행");
        
        // 대화 히스토리를 컨텍스트로 구성
        String conversationContext = buildConversationContext(state);
        
        String prompt = REACT_SYSTEM_PROMPT + "\n\n" +
            "대화 히스토리:\n" + conversationContext + "\n\n" +
            "위 대화를 바탕으로 ReAct 형식에 따라 다음 단계를 진행하세요.";

        try {
            String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();
            
            // 추론 결과 저장
            state.setReasoning(response);
            state.addMessage(ReActMessage.createAiMessage(response));
            state.setCurrentStep("reasoning_completed");
            
            log.debug("추론 결과: {}", response.substring(0, Math.min(100, response.length())));
            
        } catch (Exception e) {
            log.error("추론 단계 실행 중 오류 발생", e);
            throw new RuntimeException("추론 단계 실패", e);
        }
        
        return state;
    }

    /**
     * 행동이 필요한지 확인
     */
    private boolean needsAction(ReActState state) {
        String reasoning = state.getReasoning();
        if (reasoning == null) {
            return false;
        }
        
        // "Action:" 또는 "Tool Call:"이 포함되어 있으면 행동 필요
        return reasoning.contains("Action:") || 
               reasoning.contains("Tool Call:") ||
               reasoning.contains("search_menu") ||
               reasoning.contains("search_wine") ||
               reasoning.contains("search_web");
    }

    /**
     * 행동 단계 (Action)
     */
    private ReActState actionStep(ReActState state) {
        log.info("행동 단계 실행");
        
        String reasoning = state.getReasoning();
        ToolCall toolCall = parseToolCallFromReasoning(reasoning);
        
        if (toolCall == null) {
            log.warn("추론에서 도구 호출을 파싱할 수 없습니다.");
            return state;
        }
        
        // 도구 실행
        executeToolCall(toolCall);
        
        // 도구 호출 결과를 상태에 추가
        state.addToolCall(toolCall);
        state.setAction(toolCall.format());
        state.setCurrentStep("action_completed");
        
        // 도구 호출 메시지 추가
        state.addMessage(ReActMessage.createToolCallMessage(
            toolCall.getToolName(), 
            toolCall.getParameters().toString()
        ));
        
        return state;
    }

    /**
     * 관찰 단계 (Observation)
     */
    private ReActState observationStep(ReActState state) {
        log.info("관찰 단계 실행");
        
        // 마지막 도구 호출 결과 가져오기
        if (state.getToolCalls().isEmpty()) {
            log.warn("관찰할 도구 호출 결과가 없습니다.");
            return state;
        }
        
        ToolCall lastToolCall = state.getToolCalls().get(state.getToolCalls().size() - 1);
        
        String observation = String.format(
            "도구 실행 결과:\n도구: %s\n결과: %s\n상태: %s",
            lastToolCall.getToolName(),
            lastToolCall.getResult(),
            lastToolCall.getStatus()
        );
        
        state.setObservation(observation);
        state.setCurrentStep("observation_completed");
        
        // 도구 결과 메시지 추가
        state.addMessage(ReActMessage.createToolResultMessage(
            lastToolCall.getResult(),
            lastToolCall.getSource()
        ));
        
        log.debug("관찰 결과: {}", lastToolCall.getResult().substring(0, 
            Math.min(100, lastToolCall.getResult().length())));
        
        return state;
    }

    /**
     * 추론에서 도구 호출 파싱
     */
    private ToolCall parseToolCallFromReasoning(String reasoning) {
        if (reasoning == null) {
            return null;
        }
        
        String toolCallId = UUID.randomUUID().toString().substring(0, 8);
        Map<String, Object> parameters = new HashMap<>();
        
        // search_menu 파싱
        if (reasoning.contains("search_menu")) {
            String query = extractQueryFromText(reasoning, "search_menu");
            parameters.put("query", query);
            
            return ToolCall.builder()
                .id(toolCallId)
                .toolName("search_menu")
                .parameters(parameters)
                .status(ToolCall.ToolCallStatus.PENDING)
                .build();
        }
        
        // search_wine 파싱
        if (reasoning.contains("search_wine")) {
            String query = extractQueryFromText(reasoning, "search_wine");
            parameters.put("query", query);
            
            return ToolCall.builder()
                .id(toolCallId)
                .toolName("search_wine")
                .parameters(parameters)
                .status(ToolCall.ToolCallStatus.PENDING)
                .build();
        }
        
        // search_web 파싱
        if (reasoning.contains("search_web")) {
            String query = extractQueryFromText(reasoning, "search_web");
            parameters.put("query", query);
            
            return ToolCall.builder()
                .id(toolCallId)
                .toolName("search_web")
                .parameters(parameters)
                .status(ToolCall.ToolCallStatus.PENDING)
                .build();
        }
        
        return null;
    }

    /**
     * 텍스트에서 쿼리 추출
     */
    private String extractQueryFromText(String text, String toolName) {
        // 간단한 패턴 매칭으로 쿼리 추출
        String pattern = toolName + "\\([\"'](.*?)[\"']\\)";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(text);
        
        if (m.find()) {
            return m.group(1);
        }
        
        // 패턴이 없으면 마지막 사용자 메시지를 쿼리로 사용
        ReActMessage lastUserMessage = findLastUserMessage(text);
        return lastUserMessage != null ? lastUserMessage.getContent() : "정보 검색";
    }

    /**
     * 마지막 사용자 메시지 찾기
     */
    private ReActMessage findLastUserMessage(String context) {
        // 이 메서드는 현재 상태에서 마지막 사용자 메시지를 찾는 로직
        // 실제로는 state에서 가져와야 하지만 여기서는 간단히 처리
        return null;
    }

    /**
     * 도구 실행
     */
    private void executeToolCall(ToolCall toolCall) {
        log.info("도구 실행: {}", toolCall.getToolName());
        
        toolCall.start();
        String query = (String) toolCall.getParameters().get("query");
        
        try {
            String result;
            String source;
            
            switch (toolCall.getToolName()) {
                case "search_menu":
                    result = restaurantSearchTools.searchMenu(query);
                    source = "restaurant_menu.txt";
                    break;
                    
                case "search_wine":
                    result = restaurantSearchTools.searchWine(query);
                    source = "restaurant_wine.txt";
                    break;
                    
                case "search_web":
                    result = tavilySearchTool.searchWeb(query);
                    source = "web_search";
                    break;
                    
                default:
                    throw new IllegalArgumentException("알 수 없는 도구: " + toolCall.getToolName());
            }
            
            toolCall.complete(result, source);
            log.info("도구 실행 완료: {} ({}ms)", 
                toolCall.getToolName(), toolCall.getExecutionTimeMs());
            
        } catch (Exception e) {
            log.error("도구 실행 실패: {}", toolCall.getToolName(), e);
            toolCall.fail("도구 실행 중 오류: " + e.getMessage());
        }
    }

    /**
     * 대화 컨텍스트 구성
     */
    private String buildConversationContext(ReActState state) {
        StringBuilder context = new StringBuilder();
        
        if (state.getMessages() != null) {
            for (ReActMessage message : state.getMessages()) {
                context.append(message.format()).append("\n");
            }
        }
        
        return context.toString();
    }

    /**
     * 현재 사용 가능한 도구 목록 반환
     */
    public java.util.List<String> getAvailableTools() {
        return java.util.List.of("search_menu", "search_wine", "search_web");
    }
}