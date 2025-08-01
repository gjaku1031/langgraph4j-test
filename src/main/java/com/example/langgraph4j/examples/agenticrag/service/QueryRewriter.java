package com.example.langgraph4j.examples.agenticrag.service;

import com.example.langgraph4j.examples.agenticrag.model.QueryRewriteResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 쿼리 재작성 및 의도 분석 서비스
 * 
 * Python 예제의 query rewriting을 Java로 구현한 서비스입니다.
 * 사용자 질문을 분석하고 검색에 적합한 형태로 재작성합니다.
 * 
 * @author Claude AI Assistant
 * @since 2025-07-31
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueryRewriter {
    
    @Autowired
    private ChatClient chatClient;
    
    // 키워드 패턴 매핑
    private static final Map<QueryRewriteResult.QueryIntent, List<String>> INTENT_KEYWORDS = Map.of(
        QueryRewriteResult.QueryIntent.MENU_SEARCH, List.of("메뉴", "음식", "요리", "스테이크", "파스타", "샐러드"),
        QueryRewriteResult.QueryIntent.WINE_PAIRING, List.of("와인", "페어링", "어울리는", "추천", "매칭"),
        QueryRewriteResult.QueryIntent.PRICE_INQUIRY, List.of("가격", "얼마", "비용", "원", "달러"),
        QueryRewriteResult.QueryIntent.RECIPE_INQUIRY, List.of("레시피", "만드는법", "요리법", "재료"),
        QueryRewriteResult.QueryIntent.COMPARISON, List.of("비교", "차이", "vs", "어떤게", "뭐가 더"),
        QueryRewriteResult.QueryIntent.RECOMMENDATION, List.of("추천", "좋은", "best", "인기", "맛있는")
    );
    
    // 시스템 프롬프트
    private static final String QUERY_REWRITE_PROMPT = """
        당신은 레스토랑 정보 검색을 위한 쿼리 재작성 전문가입니다.
        
        사용자의 질문을 분석하여 다음 작업을 수행하세요:
        1. 검색에 적합한 키워드 추출
        2. 모호한 표현을 구체적으로 변환
        3. 검색 성능을 높이는 쿼리로 재작성
        
        다음 형식으로 응답하세요:
        REWRITTEN_QUERY: [재작성된 쿼리]
        KEYWORDS: [키워드1, 키워드2, 키워드3]
        REASON: [재작성 이유]
        
        예시:
        원본: "맛있는 음식 추천해줘"
        REWRITTEN_QUERY: 인기 메뉴 추천 맛있는 음식
        KEYWORDS: 인기, 메뉴, 추천, 맛있는
        REASON: 모호한 "맛있는 음식"을 "인기 메뉴"로 구체화하고 검색 키워드 보강
        """;
    
    /**
     * 쿼리 재작성 수행
     */
    public QueryRewriteResult rewriteQuery(String originalQuery) {
        if (originalQuery == null || originalQuery.trim().isEmpty()) {
            return createEmptyResult(originalQuery);
        }
        
        log.debug("쿼리 재작성 시작: '{}'", originalQuery);
        
        try {
            // 1. 룰 기반 빠른 처리
            QueryRewriteResult ruleBasedResult = applyRuleBasedRewriting(originalQuery);
            if (ruleBasedResult.getConfidenceScore() >= 0.8) {
                log.debug("룰 기반 재작성 완료: {}", ruleBasedResult.getSummary());
                return ruleBasedResult;
            }
            
            // 2. AI 기반 재작성 (테스트 모드에서는 룰 기반 결과 사용)
            if (isTestMode()) {
                log.info("테스트 모드에서 룰 기반 결과 사용");
                return ruleBasedResult;
            }
            
            // 3. AI 기반 재작성
            QueryRewriteResult aiResult = performAiRewriting(originalQuery);
            if (aiResult.isValid()) {
                log.debug("AI 기반 재작성 완료: {}", aiResult.getSummary());
                return aiResult;
            }
            
            // 4. 실패 시 룰 기반 결과 반환
            log.warn("AI 재작성 실패, 룰 기반 결과 사용");
            return ruleBasedResult;
            
        } catch (Exception e) {
            log.error("쿼리 재작성 중 오류 발생", e);
            return createErrorResult(originalQuery, e.getMessage());
        }
    }
    
    /**
     * 룰 기반 쿼리 재작성
     */
    private QueryRewriteResult applyRuleBasedRewriting(String query) {
        String normalizedQuery = query.toLowerCase().trim();
        
        // 키워드 추출
        List<String> keywords = extractKeywords(normalizedQuery);
        
        // 의도 분석
        QueryRewriteResult.QueryIntent intent = classifyIntent(normalizedQuery, keywords);
        
        // 쿼리 재작성
        String rewrittenQuery = enhanceQuery(query, intent, keywords);
        
        // 신뢰도 계산
        double confidence = calculateConfidence(query, rewrittenQuery, keywords, intent);
        
        return QueryRewriteResult.builder()
            .originalQuery(query)
            .rewrittenQuery(rewrittenQuery)
            .extractedKeywords(keywords)
            .intent(intent)
            .confidenceScore(confidence)
            .reason(generateRewriteReason(intent, keywords))
            .build();
    }
    
    /**
     * AI 기반 쿼리 재작성
     */
    private QueryRewriteResult performAiRewriting(String query) {
        try {
            String prompt = QUERY_REWRITE_PROMPT + "\n\n원본 쿼리: " + query;
            
            String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();
            
            return parseAiResponse(query, response);
            
        } catch (Exception e) {
            log.error("AI 재작성 중 오류", e);
            return createErrorResult(query, "AI 재작성 실패: " + e.getMessage());
        }
    }
    
    /**
     * AI 응답 파싱
     */
    private QueryRewriteResult parseAiResponse(String originalQuery, String response) {
        try {
            String rewrittenQuery = extractValue(response, "REWRITTEN_QUERY:");
            String keywordsStr = extractValue(response, "KEYWORDS:");
            String reason = extractValue(response, "REASON:");
            
            List<String> keywords = Arrays.stream(keywordsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
            
            QueryRewriteResult.QueryIntent intent = classifyIntent(originalQuery.toLowerCase(), keywords);
            
            return QueryRewriteResult.builder()
                .originalQuery(originalQuery)
                .rewrittenQuery(rewrittenQuery)
                .extractedKeywords(keywords)
                .intent(intent)
                .confidenceScore(0.9) // AI 기반은 높은 신뢰도
                .reason(reason)
                .build();
                
        } catch (Exception e) {
            log.error("AI 응답 파싱 실패", e);
            return createErrorResult(originalQuery, "응답 파싱 실패");
        }
    }
    
    /**
     * 응답에서 특정 값 추출
     */
    private String extractValue(String response, String key) {
        String[] lines = response.split("\n");
        for (String line : lines) {
            if (line.startsWith(key)) {
                return line.substring(key.length()).trim();
            }
        }
        return "";
    }
    
    /**
     * 키워드 추출
     */
    private List<String> extractKeywords(String query) {
        List<String> keywords = new ArrayList<>();
        
        // 한글 단어 추출 (2글자 이상)
        Pattern koreanPattern = Pattern.compile("[가-힣]{2,}");
        java.util.regex.Matcher koreanMatcher = koreanPattern.matcher(query);
        while (koreanMatcher.find()) {
            keywords.add(koreanMatcher.group());
        }
        
        // 영문 단어 추출 (3글자 이상)
        Pattern englishPattern = Pattern.compile("[a-zA-Z]{3,}");
        java.util.regex.Matcher englishMatcher = englishPattern.matcher(query);
        while (englishMatcher.find()) {
            keywords.add(englishMatcher.group().toLowerCase());
        }
        
        // 중복 제거 및 정렬
        return keywords.stream()
            .distinct()
            .sorted()
            .collect(Collectors.toList());
    }
    
    /**
     * 의도 분류
     */
    private QueryRewriteResult.QueryIntent classifyIntent(String query, List<String> keywords) {
        Map<QueryRewriteResult.QueryIntent, Integer> intentScores = new HashMap<>();
        
        // 키워드 기반 스코어링
        for (Map.Entry<QueryRewriteResult.QueryIntent, List<String>> entry : INTENT_KEYWORDS.entrySet()) {
            int score = 0;
            for (String intentKeyword : entry.getValue()) {
                if (query.contains(intentKeyword) || keywords.contains(intentKeyword)) {
                    score++;
                }
            }
            if (score > 0) {
                intentScores.put(entry.getKey(), score);
            }
        }
        
        // 최고 점수 의도 반환
        return intentScores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(QueryRewriteResult.QueryIntent.GENERAL_QUESTION);
    }
    
    /**
     * 쿼리 향상
     */
    private String enhanceQuery(String originalQuery, QueryRewriteResult.QueryIntent intent, List<String> keywords) {
        StringBuilder enhanced = new StringBuilder(originalQuery);
        
        // 의도별 키워드 추가
        switch (intent) {
            case MENU_SEARCH:
                if (!containsAny(originalQuery, "메뉴", "음식")) {
                    enhanced.append(" 메뉴");
                }
                break;
            case WINE_PAIRING:
                if (!containsAny(originalQuery, "와인", "페어링")) {
                    enhanced.append(" 와인 페어링");
                }
                break;
            case RECOMMENDATION:
                if (!containsAny(originalQuery, "추천", "좋은")) {
                    enhanced.append(" 추천");
                }
                break;
        }
        
        return enhanced.toString().trim();
    }
    
    /**
     * 문자열에 특정 단어들이 포함되어 있는지 확인
     */
    private boolean containsAny(String text, String... words) {
        String lowerText = text.toLowerCase();
        return Arrays.stream(words).anyMatch(lowerText::contains);
    }
    
    /**
     * 신뢰도 계산
     */
    private double calculateConfidence(String original, String rewritten, List<String> keywords, 
                                    QueryRewriteResult.QueryIntent intent) {
        double score = 0.5; // 기본 점수
        
        // 키워드 개수에 따른 가산점
        score += Math.min(keywords.size() * 0.1, 0.3);
        
        // 의도가 명확한 경우 가산점
        if (intent != QueryRewriteResult.QueryIntent.GENERAL_QUESTION) {
            score += 0.2;
        }
        
        // 쿼리가 향상된 경우 가산점
        if (!original.equals(rewritten)) {
            score += 0.1;
        }
        
        return Math.min(score, 1.0);
    }
    
    /**
     * 재작성 이유 생성
     */
    private String generateRewriteReason(QueryRewriteResult.QueryIntent intent, List<String> keywords) {
        StringBuilder reason = new StringBuilder();
        
        reason.append("의도 분류: ").append(getIntentDescription(intent));
        
        if (!keywords.isEmpty()) {
            reason.append(", 추출 키워드: ").append(String.join(", ", keywords));
        }
        
        return reason.toString();
    }
    
    /**
     * 의도 설명 반환
     */
    private String getIntentDescription(QueryRewriteResult.QueryIntent intent) {
        switch (intent) {
            case MENU_SEARCH: return "메뉴 검색";
            case WINE_PAIRING: return "와인 페어링";
            case RECIPE_INQUIRY: return "레시피 문의";
            case PRICE_INQUIRY: return "가격 문의";
            case COMPARISON: return "비교 질문";
            case RECOMMENDATION: return "추천 요청";
            default: return "일반 질문";
        }
    }
    
    /**
     * 테스트 모드 확인
     */
    private boolean isTestMode() {
        // API 키가 demo 키인 경우 테스트 모드
        return System.getenv("OPENAI_API_KEY") == null || 
               "demo-key".equals(System.getenv("OPENAI_API_KEY"));
    }
    
    /**
     * 빈 결과 생성
     */
    private QueryRewriteResult createEmptyResult(String query) {
        return QueryRewriteResult.builder()
            .originalQuery(query)
            .rewrittenQuery(query != null ? query : "")
            .extractedKeywords(Collections.emptyList())
            .intent(QueryRewriteResult.QueryIntent.GENERAL_QUESTION)
            .confidenceScore(0.0)
            .reason("빈 쿼리")
            .build();
    }
    
    /**
     * 오류 결과 생성
     */
    private QueryRewriteResult createErrorResult(String query, String errorMessage) {
        return QueryRewriteResult.builder()
            .originalQuery(query)
            .rewrittenQuery(query)
            .extractedKeywords(extractKeywords(query != null ? query.toLowerCase() : ""))
            .intent(QueryRewriteResult.QueryIntent.GENERAL_QUESTION)
            .confidenceScore(0.3)
            .reason("오류 발생: " + errorMessage)
            .build();
    }
}