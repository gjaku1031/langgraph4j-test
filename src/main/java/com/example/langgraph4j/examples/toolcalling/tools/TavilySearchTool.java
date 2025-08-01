package com.example.langgraph4j.examples.toolcalling.tools;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
// Spring AI Function으로 등록됨
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Tavily 웹 검색 도구
 * 
 * Python 예제의 TavilySearchResults를 Java로 구현한 클래스입니다.
 * 웹 검색을 수행하고 결과를 반환합니다.
 * 
 * @author Claude AI Assistant
 * @since 2025-07-31
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TavilySearchTool {

    private final RestTemplate restTemplate;
    
    @Value("${tavily.api.key}")
    private String tavilyApiKey;
    
    private static final String TAVILY_API_URL = "https://api.tavily.com/search";

    /**
     * 웹 검색을 수행하는 도구 메소드
     * 
     * @param query 검색할 쿼리
     * @return 검색 결과를 포맷팅한 문자열
     */
    public String searchWeb(String query) {
        log.debug("Tavily 검색 실행: {}", query);
        
        try {
            // API 요청 생성
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("api_key", tavilyApiKey);
            requestBody.put("query", query);
            requestBody.put("search_depth", "basic");
            requestBody.put("max_results", 2);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            // API 호출
            ResponseEntity<TavilyResponse> response = restTemplate.exchange(
                TAVILY_API_URL,
                HttpMethod.POST,
                request,
                TavilyResponse.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return formatSearchResults(response.getBody());
            }
            
        } catch (Exception e) {
            log.error("Tavily 검색 중 오류 발생", e);
        }
        
        return "관련 정보를 찾을 수 없습니다.";
    }

    /**
     * 검색 결과를 포맷팅합니다.
     */
    private String formatSearchResults(TavilyResponse response) {
        if (response.getResults() == null || response.getResults().isEmpty()) {
            return "검색 결과가 없습니다.";
        }
        
        return response.getResults().stream()
            .map(result -> String.format(
                "<Document href=\"%s\"/>\n%s\n</Document>",
                result.getUrl(),
                result.getContent()
            ))
            .collect(Collectors.joining("\n---\n"));
    }

    /**
     * Tavily API 응답 모델
     */
    @Data
    public static class TavilyResponse {
        private String query;
        private List<SearchResult> results;
        
        @Data
        public static class SearchResult {
            private String title;
            private String url;
            private String content;
            private float score;
        }
    }
}