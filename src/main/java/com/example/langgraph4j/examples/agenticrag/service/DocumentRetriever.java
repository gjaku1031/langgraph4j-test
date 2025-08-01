package com.example.langgraph4j.examples.agenticrag.service;

import com.example.langgraph4j.examples.agenticrag.model.Document;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 문서 검색 및 인덱싱 서비스
 * 
 * Python 예제의 vector store retrieval을 Java로 구현한 서비스입니다.
 * 간단한 키워드 기반 검색과 TF-IDF 스코어링을 사용합니다.
 * 
 * @author Claude AI Assistant
 * @since 2025-07-31
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentRetriever {
    
    // 인메모리 문서 저장소
    private final Map<String, Document> documentStore = new ConcurrentHashMap<>();
    
    // 역색인 (단어 -> 문서 ID 목록)
    private final Map<String, Set<String>> invertedIndex = new ConcurrentHashMap<>();
    
    // TF-IDF 계산용 문서 빈도수
    private final Map<String, Integer> documentFrequency = new ConcurrentHashMap<>();
    
    /**
     * 문서 저장소 초기화 (레스토랑 데이터 로드)
     */
    @PostConstruct
    public void initializeDocuments() {
        try {
            loadRestaurantDocuments();
            log.info("문서 인덱싱 완료: {}개 문서", documentStore.size());
        } catch (Exception e) {
            log.error("문서 로드 실패", e);
        }
    }
    
    /**
     * 레스토랑 메뉴 및 와인 데이터 로드
     */
    private void loadRestaurantDocuments() throws IOException {
        // 메뉴 데이터 로드
        String menuPath = "src/main/resources/data/restaurant_menu.txt";
        if (Files.exists(Paths.get(menuPath))) {
            String menuContent = Files.readString(Paths.get(menuPath));
            
            // 메뉴를 개별 아이템으로 분할
            String[] menuItems = menuContent.split("\\n\\s*\\n");
            
            for (int i = 0; i < menuItems.length; i++) {
                String item = menuItems[i].trim();
                if (!item.isEmpty()) {
                    Document menuDoc = Document.builder()
                        .id("menu_" + (i + 1))
                        .content(item)
                        .title(extractTitle(item))
                        .source("restaurant_menu.txt")
                        .type(Document.DocumentType.MENU)
                        .metadata(Map.of("category", "menu", "index", i))
                        .build();
                    
                    addDocument(menuDoc);
                }
            }
        }
        
        // 와인 데이터 로드
        String winePath = "src/main/resources/data/restaurant_wine.txt";
        if (Files.exists(Paths.get(winePath))) {
            String wineContent = Files.readString(Paths.get(winePath));
            
            // 와인을 개별 아이템으로 분할
            String[] wineItems = wineContent.split("\\n\\s*\\n");
            
            for (int i = 0; i < wineItems.length; i++) {
                String item = wineItems[i].trim();
                if (!item.isEmpty()) {
                    Document wineDoc = Document.builder()
                        .id("wine_" + (i + 1))
                        .content(item)
                        .title(extractTitle(item))
                        .source("restaurant_wine.txt")
                        .type(Document.DocumentType.WINE)
                        .metadata(Map.of("category", "wine", "index", i))
                        .build();
                    
                    addDocument(wineDoc);
                }
            }
        }
    }
    
    /**
     * 문서에서 제목 추출
     */
    private String extractTitle(String content) {
        if (content == null || content.isEmpty()) {
            return "Untitled";
        }
        
        // 첫 번째 줄을 제목으로 사용
        String[] lines = content.split("\\n");
        String firstLine = lines[0].trim();
        
        // 숫자나 특수문자로 시작하는 경우 정리
        if (firstLine.matches("^\\d+\\.\\s*(.+)")) {
            firstLine = firstLine.replaceFirst("^\\d+\\.\\s*", "");
        }
        
        return firstLine.length() > 50 ? firstLine.substring(0, 50) + "..." : firstLine;
    }
    
    /**
     * 문서 추가 및 인덱싱
     */
    public void addDocument(Document document) {
        if (document == null || document.getId() == null) {
            log.warn("유효하지 않은 문서입니다.");
            return;
        }
        
        // 문서 저장
        documentStore.put(document.getId(), document);
        
        // 역색인 업데이트
        updateInvertedIndex(document);
        
        log.debug("문서 추가됨: {} ({})", document.getId(), document.getTitle());
    }
    
    /**
     * 역색인 업데이트
     */
    private void updateInvertedIndex(Document document) {
        String content = document.getContent() + " " + 
                        (document.getTitle() != null ? document.getTitle() : "");
        
        // 텍스트를 단어로 분할하고 정규화
        Set<String> words = extractWords(content);
        
        for (String word : words) {
            // 역색인에 추가
            invertedIndex.computeIfAbsent(word, k -> ConcurrentHashMap.newKeySet())
                         .add(document.getId());
            
            // 문서 빈도수 업데이트
            documentFrequency.merge(word, 1, Integer::sum);
        }
    }
    
    /**
     * 텍스트에서 단어 추출 및 정규화
     */
    private Set<String> extractWords(String text) {
        if (text == null) {
            return Collections.emptySet();
        }
        
        return Arrays.stream(text.toLowerCase()
                .replaceAll("[^가-힣a-z0-9\\s]", " ") // 한글, 영문, 숫자만 유지
                .split("\\s+"))
                .filter(word -> word.length() >= 2) // 2글자 이상만
                .collect(Collectors.toSet());
    }
    
    /**
     * 쿼리로 문서 검색
     */
    public List<Document> searchDocuments(String query, int maxResults) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        log.debug("문서 검색: '{}' (최대 {}개)", query, maxResults);
        
        // 쿼리에서 키워드 추출
        Set<String> queryWords = extractWords(query);
        
        if (queryWords.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 문서별 스코어 계산
        Map<String, Double> documentScores = calculateDocumentScores(queryWords);
        
        // 스코어 기준으로 정렬하여 상위 문서 반환
        List<Document> results = documentScores.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(maxResults)
            .map(entry -> {
                Document doc = documentStore.get(entry.getKey()).copy();
                doc.setRelevanceScore(entry.getValue());
                return doc;
            })
            .collect(Collectors.toList());
        
        log.debug("검색 결과: {}개 문서", results.size());
        return results;
    }
    
    /**
     * TF-IDF 기반 문서 스코어 계산
     */
    private Map<String, Double> calculateDocumentScores(Set<String> queryWords) {
        Map<String, Double> scores = new HashMap<>();
        int totalDocuments = documentStore.size();
        
        for (String word : queryWords) {
            Set<String> documentIds = invertedIndex.get(word);
            if (documentIds == null) {
                continue;
            }
            
            // IDF 계산
            double idf = Math.log((double) totalDocuments / documentIds.size());
            
            for (String docId : documentIds) {
                Document doc = documentStore.get(docId);
                if (doc == null) {
                    continue;
                }
                
                // TF 계산 (간단한 단어 빈도)
                long tf = countWordOccurrences(doc.getContent() + " " + doc.getTitle(), word);
                
                // TF-IDF 스코어 계산
                double tfidf = tf * idf;
                
                scores.merge(docId, tfidf, Double::sum);
            }
        }
        
        return scores;
    }
    
    /**
     * 텍스트에서 특정 단어의 출현 빈도 계산
     */
    private long countWordOccurrences(String text, String word) {
        if (text == null || word == null) {
            return 0;
        }
        
        return Arrays.stream(text.toLowerCase().split("\\s+"))
            .filter(w -> w.equals(word))
            .count();
    }
    
    /**
     * 문서 타입별 검색
     */
    public List<Document> searchDocumentsByType(String query, Document.DocumentType type, int maxResults) {
        List<Document> allResults = searchDocuments(query, maxResults * 2);
        
        return allResults.stream()
            .filter(doc -> doc.getType() == type)
            .limit(maxResults)
            .collect(Collectors.toList());
    }
    
    /**
     * 유사한 문서 검색
     */
    public List<Document> findSimilarDocuments(String documentId, int maxResults) {
        Document targetDoc = documentStore.get(documentId);
        if (targetDoc == null) {
            return Collections.emptyList();
        }
        
        // 대상 문서의 내용을 쿼리로 사용하여 유사 문서 검색
        return searchDocuments(targetDoc.getContent(), maxResults + 1).stream()
            .filter(doc -> !doc.getId().equals(documentId)) // 자기 자신 제외
            .limit(maxResults)
            .collect(Collectors.toList());
    }
    
    /**
     * 키워드 기반 빠른 검색 (정확한 매칭)
     */
    public List<Document> quickSearch(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        return documentStore.values().stream()
            .filter(doc -> doc.containsKeyword(keyword))
            .sorted((d1, d2) -> {
                // 제목에 키워드가 있는 것을 우선
                boolean d1HasInTitle = d1.getTitle() != null && 
                    d1.getTitle().toLowerCase().contains(keyword.toLowerCase());
                boolean d2HasInTitle = d2.getTitle() != null && 
                    d2.getTitle().toLowerCase().contains(keyword.toLowerCase());
                
                if (d1HasInTitle && !d2HasInTitle) return -1;
                if (!d1HasInTitle && d2HasInTitle) return 1;
                return 0;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * 전체 문서 수 반환
     */
    public int getDocumentCount() {
        return documentStore.size();
    }
    
    /**
     * 문서 타입별 개수 반환
     */
    public Map<Document.DocumentType, Long> getDocumentCountByType() {
        return documentStore.values().stream()
            .collect(Collectors.groupingBy(
                Document::getType,
                Collectors.counting()
            ));
    }
    
    /**
     * 인덱스 상태 정보 반환
     */
    public String getIndexStatus() {
        return String.format(
            "문서: %d개, 인덱스 단어: %d개, 평균 문서당 단어: %.1f개",
            documentStore.size(),
            invertedIndex.size(),
            invertedIndex.values().stream()
                .mapToInt(Set::size)
                .average()
                .orElse(0.0)
        );
    }
}