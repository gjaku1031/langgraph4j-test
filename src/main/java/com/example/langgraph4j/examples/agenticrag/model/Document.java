package com.example.langgraph4j.examples.agenticrag.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 문서를 나타내는 모델 클래스
 * 
 * Python 예제의 Document 클래스를 Java로 구현한 클래스입니다.
 * 
 * @author Claude AI Assistant
 * @since 2025-07-31
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {
    
    /**
     * 문서 고유 ID
     */
    private String id;
    
    /**
     * 문서 내용
     */
    private String content;
    
    /**
     * 문서 제목
     */
    private String title;
    
    /**
     * 문서 소스 (파일명, URL 등)
     */
    private String source;
    
    /**
     * 문서 타입
     */
    private DocumentType type;
    
    /**
     * 문서 메타데이터
     */
    private Map<String, Object> metadata;
    
    /**
     * 관련도 점수 (검색 시 사용)
     */
    private Double relevanceScore;
    
    /**
     * 생성 시간
     */
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    /**
     * 마지막 수정 시간
     */
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    /**
     * 문서 타입 열거형
     */
    public enum DocumentType {
        MENU,           // 메뉴 문서
        WINE,           // 와인 문서
        RECIPE,         // 레시피 문서
        REVIEW,         // 리뷰 문서
        GENERAL         // 일반 문서
    }
    
    /**
     * 문서 내용 요약 (처음 100자)
     */
    public String getSummary() {
        if (content == null) {
            return "";
        }
        return content.length() > 100 ? content.substring(0, 100) + "..." : content;
    }
    
    /**
     * 문서가 비어있는지 확인
     */
    public boolean isEmpty() {
        return content == null || content.trim().isEmpty();
    }
    
    /**
     * 문서 정보를 문자열로 포맷팅
     */
    public String format() {
        StringBuilder sb = new StringBuilder();
        sb.append("Document ID: ").append(id).append("\n");
        if (title != null) {
            sb.append("Title: ").append(title).append("\n");
        }
        sb.append("Source: ").append(source).append("\n");
        sb.append("Type: ").append(type).append("\n");
        if (relevanceScore != null) {
            sb.append("Relevance Score: ").append(String.format("%.2f", relevanceScore)).append("\n");
        }
        sb.append("Content: ").append(getSummary());
        return sb.toString();
    }
    
    /**
     * 키워드로 문서 내용 검색
     */
    public boolean containsKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return false;
        }
        
        String lowerKeyword = keyword.toLowerCase();
        
        // 제목에서 검색
        if (title != null && title.toLowerCase().contains(lowerKeyword)) {
            return true;
        }
        
        // 내용에서 검색
        if (content != null && content.toLowerCase().contains(lowerKeyword)) {
            return true;
        }
        
        // 소스에서 검색
        if (source != null && source.toLowerCase().contains(lowerKeyword)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 문서 복사본 생성
     */
    public Document copy() {
        return Document.builder()
            .id(this.id)
            .content(this.content)
            .title(this.title)
            .source(this.source)
            .type(this.type)
            .metadata(this.metadata != null ? Map.copyOf(this.metadata) : null)
            .relevanceScore(this.relevanceScore)
            .createdAt(this.createdAt)
            .updatedAt(LocalDateTime.now())
            .build();
    }
}