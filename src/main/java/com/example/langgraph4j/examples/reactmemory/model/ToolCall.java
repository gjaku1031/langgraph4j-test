package com.example.langgraph4j.examples.reactmemory.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 도구 호출을 나타내는 모델 클래스
 * 
 * Python 예제의 tool call 정보를 Java로 구현한 클래스입니다.
 * 
 * @author Claude AI Assistant
 * @since 2025-07-31
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolCall {
    
    /**
     * 도구 호출 ID (추적용)
     */
    private String id;
    
    /**
     * 호출할 도구 이름
     */
    private String toolName;
    
    /**
     * 도구에 전달할 매개변수
     */
    private Map<String, Object> parameters;
    
    /**
     * 도구 실행 결과
     */
    private String result;
    
    /**
     * 호출 상태
     */
    private ToolCallStatus status;
    
    /**
     * 호출 시작 시간
     */
    @Builder.Default
    private LocalDateTime startTime = LocalDateTime.now();
    
    /**
     * 호출 완료 시간
     */
    private LocalDateTime endTime;
    
    /**
     * 오류 메시지 (실패 시)
     */
    private String errorMessage;
    
    /**
     * 소스 정보 (결과의 출처)
     */
    private String source;
    
    /**
     * 도구 호출 상태 열거형
     */
    public enum ToolCallStatus {
        PENDING,    // 대기 중
        RUNNING,    // 실행 중
        SUCCESS,    // 성공
        FAILED      // 실패
    }
    
    /**
     * 도구 호출 시작
     */
    public void start() {
        this.status = ToolCallStatus.RUNNING;
        this.startTime = LocalDateTime.now();
    }
    
    /**
     * 도구 호출 성공 완료
     */
    public void complete(String result, String source) {
        this.status = ToolCallStatus.SUCCESS;
        this.result = result;
        this.source = source;
        this.endTime = LocalDateTime.now();
    }
    
    /**
     * 도구 호출 실패
     */
    public void fail(String errorMessage) {
        this.status = ToolCallStatus.FAILED;
        this.errorMessage = errorMessage;
        this.endTime = LocalDateTime.now();
    }
    
    /**
     * 실행 시간 계산 (밀리초)
     */
    public long getExecutionTimeMs() {
        if (startTime == null) {
            return 0;
        }
        
        LocalDateTime end = endTime != null ? endTime : LocalDateTime.now();
        return java.time.Duration.between(startTime, end).toMillis();
    }
    
    /**
     * 도구 호출을 문자열로 포맷팅
     */
    public String format() {
        StringBuilder sb = new StringBuilder();
        sb.append("Tool: ").append(toolName);
        
        if (parameters != null && !parameters.isEmpty()) {
            sb.append("\nParameters: ").append(parameters);
        }
        
        if (result != null) {
            sb.append("\nResult: ").append(result);
        }
        
        if (source != null) {
            sb.append("\nSource: ").append(source);
        }
        
        sb.append("\nStatus: ").append(status);
        sb.append("\nExecution Time: ").append(getExecutionTimeMs()).append("ms");
        
        return sb.toString();
    }
    
    /**
     * 성공적으로 완료되었는지 확인
     */
    public boolean isSuccess() {
        return status == ToolCallStatus.SUCCESS;
    }
    
    /**
     * 실패했는지 확인
     */
    public boolean isFailed() {
        return status == ToolCallStatus.FAILED;
    }
    
    /**
     * 실행 중인지 확인
     */
    public boolean isRunning() {
        return status == ToolCallStatus.RUNNING;
    }
}