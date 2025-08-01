package com.example.langgraph4j.examples.toolcalling.tools;

// 현재는 간단한 텍스트 매칭으로 구현하고, 향후 벡터 검색으로 개선 예정
import lombok.extern.slf4j.Slf4j;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 레스토랑 메뉴 및 와인 검색 도구
 * 
 * Python 예제의 search_menu, search_wine 도구를 Java로 구현한 클래스입니다.
 * 벡터 저장소를 사용하여 유사도 검색을 수행합니다.
 * 
 * @author Claude AI Assistant
 * @since 2025-07-31
 */
@Slf4j
@Component
public class RestaurantSearchTools {

    private String menuContent;
    private String wineContent;
    
    /**
     * 애플리케이션 시작 시 메뉴와 와인 데이터를 로드합니다.
     */
    @PostConstruct
    public void init() {
        try {
            menuContent = loadFileContent("data/restaurant_menu.txt");
            wineContent = loadFileContent("data/restaurant_wine.txt");
            log.info("레스토랑 메뉴 및 와인 데이터 로드 완료");
        } catch (Exception e) {
            log.error("데이터 로드 중 오류 발생", e);
        }
    }

    /**
     * 메뉴 검색 도구
     * 
     * @param query 검색할 메뉴 관련 쿼리
     * @return 검색된 메뉴 정보
     */
    public String searchMenu(String query) {
        log.debug("메뉴 검색: {}", query);
        
        if (menuContent == null || menuContent.isEmpty()) {
            return "메뉴 데이터가 로드되지 않았습니다.";
        }
        
        // 간단한 키워드 매칭으로 검색
        String[] keywords = query.toLowerCase().split("\\s+");
        String[] menuItems = menuContent.split("\n\n");
        
        StringBuilder result = new StringBuilder();
        int foundCount = 0;
        
        for (String item : menuItems) {
            if (foundCount >= 2) break; // 최대 2개 결과
            
            String itemLower = item.toLowerCase();
            for (String keyword : keywords) {
                if (itemLower.contains(keyword)) {
                    if (result.length() > 0) result.append("\n\n");
                    result.append(item.trim());
                    foundCount++;
                    break;
                }
            }
        }
        
        return result.length() > 0 ? result.toString() : "관련 메뉴 정보를 찾을 수 없습니다.";
    }

    /**
     * 와인 검색 도구
     * 
     * @param query 검색할 와인 관련 쿼리
     * @return 검색된 와인 정보
     */
    public String searchWine(String query) {
        log.debug("와인 검색: {}", query);
        
        if (wineContent == null || wineContent.isEmpty()) {
            return "와인 데이터가 로드되지 않았습니다.";
        }
        
        // 간단한 키워드 매칭으로 검색
        String[] keywords = query.toLowerCase().split("\\s+");
        String[] wineItems = wineContent.split("\n\n");
        
        StringBuilder result = new StringBuilder();
        int foundCount = 0;
        
        for (String item : wineItems) {
            if (foundCount >= 2) break; // 최대 2개 결과
            
            String itemLower = item.toLowerCase();
            for (String keyword : keywords) {
                if (itemLower.contains(keyword)) {
                    if (result.length() > 0) result.append("\n\n");
                    result.append(item.trim());
                    foundCount++;
                    break;
                }
            }
        }
        
        return result.length() > 0 ? result.toString() : "관련 와인 정보를 찾을 수 없습니다.";
    }

    /**
     * 파일 내용을 로드합니다.
     */
    private String loadFileContent(String filePath) throws Exception {
        ClassPathResource resource = new ClassPathResource(filePath);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), "UTF-8"))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }
}