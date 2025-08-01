package com.example.langgraph4j.examples.agenticrag.controller;

import com.example.langgraph4j.examples.agenticrag.model.AgenticRAGState;
import com.example.langgraph4j.examples.agenticrag.model.Document;
import com.example.langgraph4j.examples.agenticrag.service.AgenticRAGService;
import com.example.langgraph4j.examples.agenticrag.service.DocumentRetriever;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Agentic RAG 시스템의 REST API 컨트롤러
 * 
 * Python 예제의 Agentic RAG를 Java로 구현한 컨트롤러입니다.
 * 고급 검색, 쿼리 개선, 품질 평가 기능을 제공합니다.
 * 
 * @author Claude AI Assistant
 * @since 2025-07-31
 */
@Slf4j
@RestController
@RequestMapping("/api/agentic-rag")
@RequiredArgsConstructor
public class AgenticRAGController {

    private final AgenticRAGService agenticRAGService;
    private final DocumentRetriever documentRetriever;

    /**
     * Agentic RAG 질문 처리
     * 
     * @param request 사용자 요청
     * @return 처리 결과
     */
    @PostMapping("/ask")
    public ResponseEntity<Map<String, Object>> ask(@RequestBody AskRequest request) {
        log.info("=== Agentic RAG 질문 요청: {} (세션: {}) ===", 
            request.getQuery(), request.getSessionId());
        
        try {
            // Agentic RAG 실행
            AgenticRAGState result = agenticRAGService.executeAgenticRAG(
                request.getQuery(), 
                request.getSessionId()
            );
            
            // 응답 생성
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("sessionId", result.getSessionId());
            response.put("originalQuery", result.getOriginalQuery());
            response.put("refinedQuery", result.getRefinedQuery() != null ? result.getRefinedQuery() : "");
            response.put("answer", result.getAnswer() != null ? result.getAnswer() : "답변을 생성할 수 없습니다.");
            response.put("qualityScore", result.getQualityScore() != null ? result.getQualityScore() : 0.0);
            response.put("currentStep", result.getCurrentStep());
            response.put("documentCount", result.getDocuments().size());
            response.put("relevantDocumentCount", result.getRelevantDocuments().size());
            response.put("generationAttempts", result.getGenerationAttempts());
            response.put("processingTimeSeconds", result.getProcessingTimeSeconds());
            response.put("summary", result.getSummary());
            response.put("searchQueries", result.getSearchQueries());
            response.put("progressPercentage", result.getProgressPercentage());
            response.put("success", result.getCurrentStep() == AgenticRAGState.ProcessingStep.COMPLETED);
            
            log.info("Agentic RAG 처리 완료: {}", result.getSummary());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Agentic RAG 처리 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "error", "Agentic RAG 처리 실패: " + e.getMessage(),
                    "success", false
                ));
        }
    }

    /**
     * 문서 검색 (RAG 없이 단순 검색)
     * 
     * @param query 검색 쿼리
     * @param maxResults 최대 결과 수
     * @return 검색 결과
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchDocuments(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int maxResults) {
        
        try {
            List<Document> documents = documentRetriever.searchDocuments(query, maxResults);
            
            Map<String, Object> response = Map.of(
                "query", query,
                "documents", documents.stream().map(doc -> Map.of(
                    "id", doc.getId(),
                    "title", doc.getTitle(),
                    "content", doc.getSummary(),
                    "source", doc.getSource(),
                    "type", doc.getType(),
                    "relevanceScore", doc.getRelevanceScore() != null ? doc.getRelevanceScore() : 0.0
                )).toList(),
                "totalCount", documents.size(),
                "success", true
            );
            
            log.info("문서 검색 완료: '{}' → {}개 결과", query, documents.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("문서 검색 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "error", "문서 검색 실패: " + e.getMessage(),
                    "success", false
                ));
        }
    }

    /**
     * 특정 문서 상세 조회
     * 
     * @param documentId 문서 ID
     * @return 문서 상세 정보
     */
    @GetMapping("/documents/{documentId}")
    public ResponseEntity<Map<String, Object>> getDocument(@PathVariable String documentId) {
        try {
            // 유사 문서 검색을 통해 문서 정보 가져오기
            List<Document> similarDocs = documentRetriever.findSimilarDocuments(documentId, 1);
            
            if (similarDocs.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Document document = similarDocs.get(0);
            
            Map<String, Object> response = Map.of(
                "id", document.getId(),
                "title", document.getTitle(),
                "content", document.getContent(),
                "source", document.getSource(),
                "type", document.getType(),
                "metadata", document.getMetadata() != null ? document.getMetadata() : Map.of(),
                "createdAt", document.getCreatedAt()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("문서 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "error", "문서 조회 실패: " + e.getMessage(),
                    "success", false
                ));
        }
    }

    /**
     * 문서 타입별 검색
     * 
     * @param type 문서 타입
     * @param query 검색 쿼리
     * @param maxResults 최대 결과 수
     * @return 검색 결과
     */
    @GetMapping("/search/type/{type}")
    public ResponseEntity<Map<String, Object>> searchByType(
            @PathVariable String type,
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int maxResults) {
        
        try {
            Document.DocumentType docType = Document.DocumentType.valueOf(type.toUpperCase());
            List<Document> documents = documentRetriever.searchDocumentsByType(query, docType, maxResults);
            
            Map<String, Object> response = Map.of(
                "query", query,
                "type", type,
                "documents", documents.stream().map(doc -> Map.of(
                    "id", doc.getId(),
                    "title", doc.getTitle(),
                    "content", doc.getSummary(),
                    "source", doc.getSource(),
                    "relevanceScore", doc.getRelevanceScore() != null ? doc.getRelevanceScore() : 0.0
                )).toList(),
                "totalCount", documents.size(),
                "success", true
            );
            
            log.info("타입별 검색 완료: {} '{}' → {}개 결과", type, query, documents.size());
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "error", "유효하지 않은 문서 타입: " + type,
                    "availableTypes", List.of("MENU", "WINE", "RECIPE", "REVIEW", "GENERAL"),
                    "success", false
                ));
        } catch (Exception e) {
            log.error("타입별 검색 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "error", "타입별 검색 실패: " + e.getMessage(),
                    "success", false
                ));
        }
    }

    /**
     * 시스템 상태 및 통계 조회
     * 
     * @return 시스템 상태
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSystemStatus() {
        try {
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("documentCount", documentRetriever.getDocumentCount());
            response.put("documentsByType", documentRetriever.getDocumentCountByType());
            response.put("indexStatus", documentRetriever.getIndexStatus());
            response.put("systemTime", java.time.LocalDateTime.now());
            response.put("availableDocumentTypes", List.of("MENU", "WINE", "RECIPE", "REVIEW", "GENERAL"));
            response.put("features", List.of(
                "Query Rewriting",
                "Multi-step Retrieval", 
                "Quality Evaluation",
                "Iterative Improvement"
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("시스템 상태 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "error", "시스템 상태 조회 실패: " + e.getMessage(),
                    "success", false
                ));
        }
    }

    /**
     * 유사 문서 검색
     * 
     * @param documentId 기준 문서 ID
     * @param maxResults 최대 결과 수
     * @return 유사 문서 목록
     */
    @GetMapping("/documents/{documentId}/similar")
    public ResponseEntity<Map<String, Object>> findSimilarDocuments(
            @PathVariable String documentId,
            @RequestParam(defaultValue = "5") int maxResults) {
        
        try {
            List<Document> similarDocs = documentRetriever.findSimilarDocuments(documentId, maxResults);
            
            Map<String, Object> response = Map.of(
                "baseDocumentId", documentId,
                "similarDocuments", similarDocs.stream().map(doc -> Map.of(
                    "id", doc.getId(),
                    "title", doc.getTitle(),
                    "content", doc.getSummary(),
                    "type", doc.getType(),
                    "relevanceScore", doc.getRelevanceScore() != null ? doc.getRelevanceScore() : 0.0
                )).toList(),
                "count", similarDocs.size(),
                "success", true
            );
            
            log.info("유사 문서 검색 완료: {} → {}개 결과", documentId, similarDocs.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("유사 문서 검색 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "error", "유사 문서 검색 실패: " + e.getMessage(),
                    "success", false
                ));
        }
    }

    /**
     * 간단한 테스트 엔드포인트
     * 
     * @return 테스트 응답
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> test() {
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("message", "Agentic RAG 시스템이 정상적으로 작동 중입니다.");
        response.put("documentCount", documentRetriever.getDocumentCount());
        response.put("indexStatus", documentRetriever.getIndexStatus());
        response.put("timestamp", java.time.LocalDateTime.now());
        response.put("features", List.of(
            "Multi-step Retrieval",
            "Query Rewriting", 
            "Quality Evaluation",
            "Agentic Processing"
        ));
        
        return ResponseEntity.ok(response);
    }

    /**
     * 질문 요청 DTO
     */
    public static class AskRequest {
        private String query;
        private String sessionId;

        // 기본 생성자
        public AskRequest() {}

        // 매개변수 생성자
        public AskRequest(String query, String sessionId) {
            this.query = query;
            this.sessionId = sessionId;
        }

        // Getter/Setter
        public String getQuery() {
            return query;
        }

        public void setQuery(String query) {
            this.query = query;
        }

        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }
    }
}