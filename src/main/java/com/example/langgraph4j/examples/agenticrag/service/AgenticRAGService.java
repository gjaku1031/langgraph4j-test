package com.example.langgraph4j.examples.agenticrag.service;

import com.example.langgraph4j.examples.agenticrag.model.AgenticRAGState;
import com.example.langgraph4j.examples.agenticrag.model.Document;
import com.example.langgraph4j.examples.agenticrag.model.QueryRewriteResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Agentic RAG (Retrieval-Augmented Generation) 서비스
 * 
 * Python 예제의 Agentic RAG 패턴을 Java로 구현한 서비스입니다.
 * 다단계 검색, 쿼리 개선, 답변 생성 및 품질 평가를 수행합니다.
 * 
 * @author Claude AI Assistant
 * @since 2025-07-31
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgenticRAGService {
    
    @Autowired
    private ChatClient chatClient;
    
    @Autowired
    private DocumentRetriever documentRetriever;
    
    @Autowired
    private QueryRewriter queryRewriter;
    
    // RAG 시스템 프롬프트
    private static final String RAG_SYSTEM_PROMPT = """
        당신은 레스토랑 정보 전문 AI 어시스턴트입니다.
        
        제공된 문서들을 바탕으로 사용자의 질문에 정확하고 도움이 되는 답변을 제공하세요.
        
        답변 규칙:
        1. 반드시 제공된 문서 내용을 기반으로 답변하세요
        2. 문서에 없는 정보는 추측하지 마세요
        3. 가격, 메뉴, 와인 페어링 등 구체적인 정보를 포함하세요
        4. 답변은 친근하고 전문적인 톤으로 작성하세요
        5. 필요시 추가 질문을 유도하세요
        
        답변 형식:
        - 직접적이고 명확한 답변
        - 관련 세부 정보 제공
        - 출처 문서 언급 (선택사항)
        """;
    
    // 품질 평가 프롬프트
    private static final String QUALITY_EVALUATION_PROMPT = """
        다음 답변의 품질을 0.0-1.0 점수로 평가하세요.
        
        평가 기준:
        1. 정확성: 문서 내용과 일치하는가?
        2. 완성도: 질문에 충분히 답변했는가?
        3. 유용성: 사용자에게 도움이 되는가?
        4. 명확성: 이해하기 쉬운가?
        
        SCORE: [0.0-1.0 점수]
        REASON: [평가 이유]
        
        질문: {question}
        답변: {answer}
        참고 문서: {documents}
        """;
    
    /**
     * Agentic RAG 실행
     */
    public AgenticRAGState executeAgenticRAG(String query, String sessionId) {
        log.info("=== Agentic RAG 시작: {} (세션: {}) ===", query, sessionId);
        
        // 상태 초기화
        AgenticRAGState state = AgenticRAGState.builder()
            .originalQuery(query)
            .sessionId(sessionId != null ? sessionId : generateSessionId())
            .currentStep(AgenticRAGState.ProcessingStep.STARTED)
            .build();
        
        try {
            // 1단계: 쿼리 분석 및 재작성
            state = performQueryAnalysis(state);
            
            // 2단계: 문서 검색
            state = performDocumentRetrieval(state);
            
            // 3단계: 관련성 필터링
            state = performRelevanceFiltering(state);
            
            // 4단계: 답변 생성 (최대 3회 시도)
            while (!state.hasSufficientQuality() && !state.hasReachedMaxAttempts()) {
                state = performAnswerGeneration(state);
                
                if (state.getAnswer() != null) {
                    state = performQualityEvaluation(state);
                }
                
                state.incrementAttempts();
                
                // 품질이 부족하면 쿼리 개선 후 재시도
                if (!state.hasSufficientQuality() && !state.hasReachedMaxAttempts()) {
                    log.info("답변 품질 부족 ({}), 재시도 중...", state.getQualityScore());
                    state = improveQueryAndRetry(state);
                }
            }
            
            // 최종 완료 처리
            if (state.hasSufficientQuality()) {
                state.markCompleted();
                log.info("Agentic RAG 성공 완료: 품질 점수 {}", state.getQualityScore());
            } else {
                state.markFailed("최대 시도 횟수 도달, 품질 기준 미달");
                log.warn("Agentic RAG 품질 기준 미달로 완료: 최종 점수 {}", state.getQualityScore());
            }
            
        } catch (Exception e) {
            log.error("Agentic RAG 실행 중 오류 발생", e);
            state.markFailed("처리 중 오류 발생: " + e.getMessage());
        }
        
        log.info("=== Agentic RAG 완료: {} ===", state.getSummary());
        return state;
    }
    
    /**
     * 1단계: 쿼리 분석 및 재작성
     */
    private AgenticRAGState performQueryAnalysis(AgenticRAGState state) {
        log.info("쿼리 분석 단계 실행");
        state.setCurrentStep(AgenticRAGState.ProcessingStep.QUERY_ANALYSIS);
        
        try {
            QueryRewriteResult rewriteResult = queryRewriter.rewriteQuery(state.getOriginalQuery());
            
            if (rewriteResult.isValid()) {
                state.setRefinedQuery(rewriteResult.getRewrittenQuery());
                state.addSearchQuery(rewriteResult.getRewrittenQuery());
                
                log.debug("쿼리 재작성 완료: {} → {}", 
                    state.getOriginalQuery(), state.getRefinedQuery());
            } else {
                // 재작성 실패시 원본 쿼리 사용
                state.setRefinedQuery(state.getOriginalQuery());
                state.addSearchQuery(state.getOriginalQuery());
                
                log.warn("쿼리 재작성 실패, 원본 쿼리 사용");
            }
            
            state.setCurrentStep(AgenticRAGState.ProcessingStep.QUERY_REFINEMENT);
            
        } catch (Exception e) {
            log.error("쿼리 분석 중 오류", e);
            throw new RuntimeException("쿼리 분석 실패", e);
        }
        
        return state;
    }
    
    /**
     * 2단계: 문서 검색
     */
    private AgenticRAGState performDocumentRetrieval(AgenticRAGState state) {
        log.info("문서 검색 단계 실행");
        state.setCurrentStep(AgenticRAGState.ProcessingStep.DOCUMENT_RETRIEVAL);
        
        try {
            String searchQuery = state.getRefinedQuery() != null ? 
                state.getRefinedQuery() : state.getOriginalQuery();
            
            // 일반 검색
            List<Document> documents = documentRetriever.searchDocuments(searchQuery, 10);
            documents.forEach(state::addDocument);
            
            // 키워드 기반 빠른 검색으로 보완
            List<Document> quickResults = documentRetriever.quickSearch(searchQuery);
            quickResults.stream()
                .filter(doc -> !containsDocument(state.getDocuments(), doc.getId()))
                .forEach(state::addDocument);
            
            log.debug("문서 검색 완료: {}개 문서", state.getDocuments().size());
            
        } catch (Exception e) {
            log.error("문서 검색 중 오류", e);
            throw new RuntimeException("문서 검색 실패", e);
        }
        
        return state;
    }
    
    /**
     * 3단계: 관련성 필터링
     */
    private AgenticRAGState performRelevanceFiltering(AgenticRAGState state) {
        log.info("관련성 필터링 단계 실행");
        state.setCurrentStep(AgenticRAGState.ProcessingStep.RELEVANCE_FILTERING);
        
        try {
            // 관련도 점수 기반 필터링 (상위 5개)
            List<Document> relevantDocs = state.getDocuments().stream()
                .filter(doc -> doc.getRelevanceScore() != null && doc.getRelevanceScore() > 0.1)
                .sorted((d1, d2) -> Double.compare(
                    d2.getRelevanceScore() != null ? d2.getRelevanceScore() : 0.0,
                    d1.getRelevanceScore() != null ? d1.getRelevanceScore() : 0.0
                ))
                .limit(5)
                .collect(Collectors.toList());
            
            relevantDocs.forEach(state::addRelevantDocument);
            
            log.debug("관련성 필터링 완료: {}개 → {}개", 
                state.getDocuments().size(), state.getRelevantDocuments().size());
            
            if (!state.hasSufficientDocuments()) {
                log.warn("충분한 관련 문서를 찾지 못했습니다: {}개", 
                    state.getRelevantDocuments().size());
            }
            
        } catch (Exception e) {
            log.error("관련성 필터링 중 오류", e);
            throw new RuntimeException("관련성 필터링 실패", e);
        }
        
        return state;
    }
    
    /**
     * 4단계: 답변 생성
     */
    private AgenticRAGState performAnswerGeneration(AgenticRAGState state) {
        log.info("답변 생성 단계 실행 (시도 {})", state.getGenerationAttempts() + 1);
        state.setCurrentStep(AgenticRAGState.ProcessingStep.ANSWER_GENERATION);
        
        try {
            // 테스트 모드에서는 간단한 답변 생성
            if (isTestMode()) {
                state.setAnswer(generateTestAnswer(state));
                log.info("테스트 모드에서 답변 생성");
                return state;
            }
            
            // 문서 컨텍스트 구성
            String documentContext = buildDocumentContext(state.getRelevantDocuments());
            
            String prompt = RAG_SYSTEM_PROMPT + "\n\n" +
                "질문: " + state.getOriginalQuery() + "\n\n" +
                "참고 문서:\n" + documentContext + "\n\n" +
                "위 문서들을 바탕으로 질문에 답변해 주세요.";
            
            String answer = chatClient.prompt()
                .user(prompt)
                .call()
                .content();
            
            state.setAnswer(answer);
            log.debug("답변 생성 완료: {}자", answer.length());
            
        } catch (Exception e) {
            log.error("답변 생성 중 오류", e);
            // 테스트 답변으로 대체
            state.setAnswer(generateTestAnswer(state));
        }
        
        return state;
    }
    
    /**
     * 5단계: 품질 평가
     */
    private AgenticRAGState performQualityEvaluation(AgenticRAGState state) {
        log.info("품질 평가 단계 실행");
        state.setCurrentStep(AgenticRAGState.ProcessingStep.QUALITY_EVALUATION);
        
        try {
            // 테스트 모드에서는 간단한 품질 평가
            if (isTestMode()) {
                double score = evaluateTestQuality(state);
                state.setQualityScore(score);
                log.info("테스트 모드 품질 평가: {}", score);
                return state;
            }
            
            // AI 기반 품질 평가
            String prompt = QUALITY_EVALUATION_PROMPT
                .replace("{question}", state.getOriginalQuery())
                .replace("{answer}", state.getAnswer())
                .replace("{documents}", buildDocumentContext(state.getRelevantDocuments()));
            
            String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();
            
            double score = parseQualityScore(response);
            state.setQualityScore(score);
            
            log.debug("품질 평가 완료: {}", score);
            
        } catch (Exception e) {
            log.error("품질 평가 중 오류", e);
            // 기본 품질 점수 할당
            state.setQualityScore(0.6);
        }
        
        return state;
    }
    
    /**
     * 쿼리 개선 및 재시도
     */
    private AgenticRAGState improveQueryAndRetry(AgenticRAGState state) {
        log.info("쿼리 개선 및 재시도");
        
        // 추가 검색 쿼리 생성
        String improvedQuery = generateImprovedQuery(state);
        state.addSearchQuery(improvedQuery);
        
        // 추가 문서 검색
        List<Document> additionalDocs = documentRetriever.searchDocuments(improvedQuery, 5);
        additionalDocs.stream()
            .filter(doc -> !containsDocument(state.getDocuments(), doc.getId()))
            .forEach(state::addDocument);
        
        // 관련 문서 재선별
        state.getRelevantDocuments().clear();
        return performRelevanceFiltering(state);
    }
    
    /**
     * 개선된 쿼리 생성
     */
    private String generateImprovedQuery(AgenticRAGState state) {
        String original = state.getOriginalQuery();
        
        // 간단한 쿼리 확장
        if (original.contains("와인")) {
            return original + " 페어링 추천";
        } else if (original.contains("메뉴")) {
            return original + " 인기 음식";
        } else if (original.contains("가격")) {
            return original + " 비용 정보";
        } else {
            return original + " 추천 정보";
        }
    }
    
    /**
     * 문서 컨텍스트 구성
     */
    private String buildDocumentContext(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return "관련 문서가 없습니다.";
        }
        
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            context.append(String.format("[문서 %d]\n", i + 1));
            context.append("제목: ").append(doc.getTitle()).append("\n");
            context.append("내용: ").append(doc.getContent()).append("\n");
            if (doc.getRelevanceScore() != null) {
                context.append("관련도: ").append(String.format("%.2f", doc.getRelevanceScore())).append("\n");
            }
            context.append("\n");
        }
        
        return context.toString();
    }
    
    /**
     * 테스트 답변 생성
     */
    private String generateTestAnswer(AgenticRAGState state) {
        if (state.getRelevantDocuments().isEmpty()) {
            return "죄송합니다. '" + state.getOriginalQuery() + 
                "'에 대한 관련 정보를 찾을 수 없습니다. 다른 질문을 해보시겠어요?";
        }
        
        Document bestDoc = state.getRelevantDocuments().get(0);
        return String.format("'%s'에 대한 정보를 찾았습니다.\n\n%s\n\n추가 정보가 필요하시면 언제든 문의해 주세요!",
            state.getOriginalQuery(), bestDoc.getContent());
    }
    
    /**
     * 테스트 모드 품질 평가
     */
    private double evaluateTestQuality(AgenticRAGState state) {
        double score = 0.5; // 기본 점수
        
        // 답변 길이에 따른 가산점
        if (state.getAnswer() != null) {
            int length = state.getAnswer().length();
            if (length > 100) score += 0.2;
            if (length > 200) score += 0.1;
        }
        
        // 관련 문서 수에 따른 가산점
        score += Math.min(state.getRelevantDocuments().size() * 0.1, 0.2);
        
        return Math.min(score, 1.0);
    }
    
    /**
     * AI 응답에서 품질 점수 파싱
     */
    private double parseQualityScore(String response) {
        try {
            String[] lines = response.split("\n");
            for (String line : lines) {
                if (line.startsWith("SCORE:")) {
                    String scoreStr = line.substring(6).trim();
                    return Double.parseDouble(scoreStr);
                }
            }
        } catch (Exception e) {
            log.warn("품질 점수 파싱 실패", e);
        }
        return 0.6; // 기본값
    }
    
    /**
     * 문서 목록에 특정 ID의 문서가 포함되어 있는지 확인
     */
    private boolean containsDocument(List<Document> documents, String documentId) {
        return documents.stream().anyMatch(doc -> doc.getId().equals(documentId));
    }
    
    /**
     * 테스트 모드 확인
     */
    private boolean isTestMode() {
        return System.getenv("OPENAI_API_KEY") == null || 
               "demo-key".equals(System.getenv("OPENAI_API_KEY"));
    }
    
    /**
     * 세션 ID 생성
     */
    private String generateSessionId() {
        return "session_" + UUID.randomUUID().toString().substring(0, 8);
    }
}