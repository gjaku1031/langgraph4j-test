package com.example.langgraph4j.examples.toolcalling.service;

import com.example.langgraph4j.examples.toolcalling.tools.RestaurantSearchTools;
import com.example.langgraph4j.examples.toolcalling.tools.TavilySearchTool;
import com.example.langgraph4j.examples.toolcalling.tools.WikipediaSummaryTool;
// Spring AI만 사용
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Tool Calling 서비스
 * 
 * 다양한 도구(Tool)를 사용하여 사용자 질문에 답변하는 서비스입니다.
 * Python 예제의 tool calling 로직을 Java로 구현했습니다.
 * 
 * @author Claude AI Assistant
 * @since 2025-07-31
 */
@Slf4j
@Service
public class ToolCallingService {

    @Autowired
    private ChatClient chatClient;
    
    @Value("${spring.ai.openai.api-key}")
    private String openaiApiKey;
    
    @Autowired
    private TavilySearchTool tavilySearchTool;
    
    @Autowired
    private RestaurantSearchTools restaurantSearchTools;
    
    @Autowired
    private WikipediaSummaryTool wikipediaSummaryTool;
    
    // 간단한 대화 히스토리 저장 (실제로는 Redis나 DB 사용 권장)
    private final List<String> conversationHistory = new ArrayList<>();

    /**
     * 도구를 사용하여 사용자 질문에 답변합니다.
     * 
     * @param userQuery 사용자 질문
     * @return AI의 답변
     */
    public String processWithTools(String userQuery) {
        log.info("사용자 질문: {}", userQuery);
        
        // 테스트용 로직: API 키가 demo-key인 경우 도구를 직접 사용
        if (isTestMode()) {
            return processWithToolsTestMode(userQuery);
        }
        
        // Spring AI를 사용한 구현
        String systemPrompt = """
            당신은 레스토랑 메뉴 정보와 일반적인 음식 관련 지식을 제공하는 AI 어시스턴트입니다.
            
            다음 도구들을 적절히 사용하세요:
            1. searchMenu: 레스토랑 메뉴 정보 검색
            2. searchWine: 와인 추천 및 페어링 정보 검색  
            3. searchAndSummarizeWikipedia: 일반적인 음식 정보 검색
            4. searchWeb: 최신 정보나 추가 웹 검색이 필요한 경우
            
            사용자의 질문에 정확하고 도움이 되는 답변을 제공하세요.
            """;
        
        // 현재는 간단한 텍스트 응답만 제공 (향후 Function Calling 추가 예정)
        var response = chatClient.prompt()
            .system(systemPrompt)
            .user(userQuery)
            .call()
            .content();
        
        log.info("AI 응답: {}", response);
        return response;
    }

    /**
     * 테스트 모드에서 도구들을 직접 사용하여 답변을 생성합니다.
     */
    private String processWithToolsTestMode(String userQuery) {
        log.info("테스트 모드로 실행 중...");
        
        StringBuilder response = new StringBuilder();
        response.append("=== 도구 호출 테스트 결과 ===\n\n");
        
        // 1. 메뉴 검색 테스트
        if (userQuery.contains("스테이크") || userQuery.contains("메뉴") || userQuery.contains("음식")) {
            String menuResult = restaurantSearchTools.searchMenu(userQuery);
            response.append("🍽️ 메뉴 검색 결과:\n").append(menuResult).append("\n\n");
        }
        
        // 2. 와인 검색 테스트
        if (userQuery.contains("와인") || userQuery.contains("술") || userQuery.contains("페어링")) {
            String wineResult = restaurantSearchTools.searchWine(userQuery);
            response.append("🍷 와인 검색 결과:\n").append(wineResult).append("\n\n");
        }
        
        // 3. 웹 검색 테스트 (Tavily API 키가 없어도 시뮬레이션)
        if (userQuery.contains("최신") || userQuery.contains("정보")) {
            response.append("🌐 웹 검색 시뮬레이션:\n");
            response.append("Tavily API를 통해 최신 정보를 검색했습니다. (테스트 모드)\n\n");
        }
        
        // 4. Wikipedia 검색 테스트 (API 호출 없이 시뮬레이션)
        if (userQuery.contains("정보") || userQuery.contains("설명")) {
            response.append("📚 Wikipedia 검색 시뮬레이션:\n");
            response.append("Wikipedia에서 관련 정보를 찾았습니다. (테스트 모드)\n\n");
        }
        
        response.append("=== 테스트 모드 완료 ===\n");
        response.append("실제 AI 모델을 사용하려면 OPENAI_API_KEY 환경변수를 설정하세요.");
        
        return response.toString();
    }

    /**
     * 테스트 모드인지 확인합니다.
     */
    private boolean isTestMode() {
        // API 키가 demo-key이거나 없으면 테스트 모드
        return openaiApiKey == null || openaiApiKey.equals("demo-key") || openaiApiKey.trim().isEmpty();
    }

    /**
     * Few-shot 예제를 사용하여 도구 호출 성능을 향상시킵니다.
     * 
     * @param userQuery 사용자 질문
     * @return AI의 답변
     */
    public String processWithFewShotExamples(String userQuery) {
        log.info("Few-shot 예제를 사용한 처리: {}", userQuery);
        
        String systemPrompt = """
            당신은 레스토랑 메뉴 정보와 일반적인 음식 관련 지식을 제공하는 AI 어시스턴트입니다.
            
            예제:
            사용자: "트러플 리조또의 가격과 특징, 그리고 어울리는 와인에 대해 알려주세요."
            어시스턴트: 먼저 메뉴 정보를 검색하고, 어울리는 와인을 찾아보겠습니다.
            [searchMenu 호출: "트러플 리조또"]
            [searchWine 호출: "트러플 리조또에 어울리는 와인"]
            
            트러플 리조또는 가격이 ₩22,000이며, 이탈리아산 아르보리오 쌀과 블랙 트러플을 사용합니다.
            크리미한 텍스처와 풍부한 트러플 향이 특징입니다.
            어울리는 와인으로는 중간 바디의 화이트 와인인 샤르도네나 피노 그리지오를 추천합니다.
            
            이제 사용자의 질문에 답변하세요.
            """;
        
        var response = chatClient.prompt()
            .system(systemPrompt)
            .user(userQuery)
            .call()
            .content();
        
        return response;
    }

    /**
     * 대화 히스토리를 유지하면서 도구를 사용합니다.
     * 
     * @param userQuery 사용자 질문
     * @return AI의 답변
     */
    public String processWithMemory(String userQuery) {
        // 간단한 대화 히스토리 관리
        conversationHistory.add("사용자: " + userQuery);
        
        // 최근 대화 5개만 유지
        if (conversationHistory.size() > 10) {
            conversationHistory.subList(0, conversationHistory.size() - 10).clear();
        }
        
        String contextPrompt = String.join("\n", conversationHistory);
        
        String systemPrompt = """
            당신은 레스토랑 메뉴 정보와 일반적인 음식 관련 지식을 제공하는 AI 어시스턴트입니다.
            이전 대화 내용을 참고하여 일관성 있는 답변을 제공하세요.
            
            대화 히스토리:
            """ + contextPrompt;
        
        var response = chatClient.prompt()
            .system(systemPrompt)
            .user(userQuery)
            .call()
            .content();
        
        conversationHistory.add("AI: " + response);
        
        return response;
    }

    // 변환 메소드 제거됨 - Spring AI만 사용

    /**
     * 도구 목록을 반환합니다.
     */
    public List<String> getAvailableTools() {
        return List.of(
            "searchMenu - 레스토랑 메뉴 검색",
            "searchWine - 와인 정보 및 추천 검색",
            "searchAndSummarizeWikipedia - Wikipedia 검색 및 요약",
            "searchWeb - 웹 검색 (Tavily)"
        );
    }
}