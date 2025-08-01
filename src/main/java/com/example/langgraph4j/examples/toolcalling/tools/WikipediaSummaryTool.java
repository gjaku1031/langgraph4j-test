package com.example.langgraph4j.examples.toolcalling.tools;

import org.springframework.ai.chat.client.ChatClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Wikipedia 요약 도구
 * 
 * Python 예제의 wiki_summary 도구를 Java로 구현한 클래스입니다.
 * Wikipedia API를 사용하여 문서를 검색하고 요약합니다.
 * 
 * @author Claude AI Assistant
 * @since 2025-07-31
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WikipediaSummaryTool {

    private final RestTemplate restTemplate;
    private final ChatClient chatClient;
    
    private static final String WIKIPEDIA_API_URL = "https://ko.wikipedia.org/w/api.php";

    /**
     * Wikipedia에서 정보를 검색하고 요약합니다.
     * 
     * @param query 검색할 쿼리
     * @return 요약된 Wikipedia 정보
     */
    public String searchAndSummarizeWikipedia(String query) {
        log.debug("Wikipedia 검색 및 요약: {}", query);
        
        try {
            // 1. Wikipedia 검색
            String searchResults = searchWikipedia(query);
            if (searchResults == null || searchResults.isEmpty()) {
                return "Wikipedia에서 관련 정보를 찾을 수 없습니다.";
            }
            
            // 2. 첫 번째 검색 결과의 내용 가져오기
            String pageContent = getWikipediaPageContent(searchResults);
            if (pageContent == null || pageContent.isEmpty()) {
                return "Wikipedia 페이지 내용을 가져올 수 없습니다.";
            }
            
            // 3. AI 모델을 사용하여 요약
            return summarizeContent(pageContent);
            
        } catch (Exception e) {
            log.error("Wikipedia 검색 중 오류 발생", e);
            return "Wikipedia 검색 중 오류가 발생했습니다.";
        }
    }

    /**
     * Wikipedia API를 사용하여 검색합니다.
     */
    private String searchWikipedia(String query) {
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
            
            String url = UriComponentsBuilder.fromHttpUrl(WIKIPEDIA_API_URL)
                .queryParam("action", "query")
                .queryParam("format", "json")
                .queryParam("list", "search")
                .queryParam("srsearch", encodedQuery)
                .queryParam("srlimit", "1")
                .build()
                .toUriString();
            
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                Map<String, Object> queryResult = (Map<String, Object>) body.get("query");
                List<Map<String, Object>> searchResults = (List<Map<String, Object>>) queryResult.get("search");
                
                if (!searchResults.isEmpty()) {
                    return (String) searchResults.get(0).get("title");
                }
            }
        } catch (Exception e) {
            log.error("Wikipedia 검색 API 호출 실패", e);
        }
        
        return null;
    }

    /**
     * Wikipedia 페이지의 내용을 가져옵니다.
     */
    private String getWikipediaPageContent(String pageTitle) {
        try {
            String encodedTitle = URLEncoder.encode(pageTitle, StandardCharsets.UTF_8.toString());
            
            String url = UriComponentsBuilder.fromHttpUrl(WIKIPEDIA_API_URL)
                .queryParam("action", "query")
                .queryParam("format", "json")
                .queryParam("prop", "extracts")
                .queryParam("exintro", "true")
                .queryParam("explaintext", "true")
                .queryParam("titles", encodedTitle)
                .build()
                .toUriString();
            
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                Map<String, Object> queryResult = (Map<String, Object>) body.get("query");
                Map<String, Object> pages = (Map<String, Object>) queryResult.get("pages");
                
                for (Object pageData : pages.values()) {
                    Map<String, Object> page = (Map<String, Object>) pageData;
                    String extract = (String) page.get("extract");
                    if (extract != null && !extract.isEmpty()) {
                        return String.format("<Document source=\"Wikipedia: %s\"/>\n%s\n</Document>", 
                            pageTitle, extract);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Wikipedia 페이지 내용 가져오기 실패", e);
        }
        
        return null;
    }

    /**
     * AI 모델을 사용하여 내용을 요약합니다.
     */
    private String summarizeContent(String content) {
        String prompt = String.format(
            "다음 텍스트를 간결하게 요약해주세요:\n\n%s\n\n요약:",
            content
        );
        
        try {
            return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
        } catch (Exception e) {
            log.error("내용 요약 중 오류 발생", e);
            // 요약 실패 시 원본 반환
            return content;
        }
    }
}