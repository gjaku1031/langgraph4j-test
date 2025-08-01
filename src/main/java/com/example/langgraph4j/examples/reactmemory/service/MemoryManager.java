package com.example.langgraph4j.examples.reactmemory.service;

import com.example.langgraph4j.examples.reactmemory.model.ReActState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ReAct 시스템의 메모리 관리 서비스
 * 
 * Python 예제의 MemorySaver를 Java로 구현한 클래스입니다.
 * 스레드 기반 대화 세션 관리 및 체크포인트 저장/복원을 담당합니다.
 * 
 * @author Claude AI Assistant
 * @since 2025-07-31
 */
@Slf4j
@Service
public class MemoryManager {
    
    // 스레드별 상태 저장소 (실제로는 Redis나 DB 사용 권장)
    private final Map<String, ReActState> threadStates = new ConcurrentHashMap<>();
    
    // 체크포인트 저장소
    private final Map<String, ReActState> checkpoints = new ConcurrentHashMap<>();
    
    /**
     * 새로운 스레드 생성
     * 
     * @return 생성된 스레드 ID
     */
    public String createThread() {
        String threadId = "thread_" + UUID.randomUUID().toString().substring(0, 8);
        
        ReActState initialState = ReActState.builder()
            .threadId(threadId)
            .currentStep("start")
            .checkpointId("checkpoint_0")
            .timestamp(LocalDateTime.now())
            .build();
        
        threadStates.put(threadId, initialState);
        log.info("새로운 스레드 생성: {}", threadId);
        
        return threadId;
    }
    
    /**
     * 스레드 상태 가져오기
     * 
     * @param threadId 스레드 ID
     * @return 스레드 상태 (없으면 새로 생성)
     */
    public ReActState getThreadState(String threadId) {
        if (threadId == null) {
            threadId = createThread();
        }
        
        ReActState state = threadStates.get(threadId);
        if (state == null) {
            log.info("스레드 {} 가 존재하지 않아 새로 생성합니다.", threadId);
            state = ReActState.builder()
                .threadId(threadId)
                .currentStep("start")
                .checkpointId("checkpoint_0")
                .timestamp(LocalDateTime.now())
                .build();
            threadStates.put(threadId, state);
        }
        
        return state;
    }
    
    /**
     * 스레드 상태 저장
     * 
     * @param state 저장할 상태
     */
    public void saveThreadState(ReActState state) {
        if (state.getThreadId() == null) {
            log.error("스레드 ID가 없는 상태는 저장할 수 없습니다.");
            return;
        }
        
        // 체크포인트 생성
        String checkpointId = createCheckpoint(state);
        state.setCheckpointId(checkpointId);
        state.setTimestamp(LocalDateTime.now());
        
        threadStates.put(state.getThreadId(), state);
        log.debug("스레드 상태 저장: {} (체크포인트: {})", 
            state.getThreadId(), checkpointId);
    }
    
    /**
     * 체크포인트 생성
     * 
     * @param state 체크포인트할 상태
     * @return 체크포인트 ID
     */
    public String createCheckpoint(ReActState state) {
        String checkpointId = "checkpoint_" + System.currentTimeMillis();
        
        // 상태 복사본 생성 (깊은 복사)
        ReActState checkpoint = ReActState.builder()
            .messages(state.getMessages() != null ? 
                java.util.List.copyOf(state.getMessages()) : null)
            .currentStep(state.getCurrentStep())
            .threadId(state.getThreadId())
            .toolCalls(state.getToolCalls() != null ? 
                java.util.List.copyOf(state.getToolCalls()) : null)
            .reasoning(state.getReasoning())
            .action(state.getAction())
            .observation(state.getObservation())
            .checkpointId(checkpointId)
            .timestamp(LocalDateTime.now())
            .build();
        
        checkpoints.put(checkpointId, checkpoint);
        log.debug("체크포인트 생성: {}", checkpointId);
        
        return checkpointId;
    }
    
    /**
     * 체크포인트에서 상태 복원
     * 
     * @param checkpointId 체크포인트 ID
     * @return 복원된 상태 (없으면 null)
     */
    public ReActState restoreFromCheckpoint(String checkpointId) {
        ReActState checkpoint = checkpoints.get(checkpointId);
        if (checkpoint == null) {
            log.warn("체크포인트 {}를 찾을 수 없습니다.", checkpointId);
            return null;
        }
        
        log.info("체크포인트에서 상태 복원: {}", checkpointId);
        return checkpoint;
    }
    
    /**
     * 스레드의 대화 히스토리 가져오기
     * 
     * @param threadId 스레드 ID
     * @return 대화 히스토리를 포함한 상태
     */
    public ReActState getConversationHistory(String threadId) {
        ReActState state = getThreadState(threadId);
        
        if (state.getMessages().isEmpty()) {
            log.info("스레드 {}에 대화 히스토리가 없습니다.", threadId);
        } else {
            log.info("스레드 {} 대화 히스토리: {} 개 메시지", 
                threadId, state.getMessages().size());
        }
        
        return state;
    }
    
    /**
     * 스레드 삭제
     * 
     * @param threadId 삭제할 스레드 ID
     */
    public void deleteThread(String threadId) {
        ReActState removedState = threadStates.remove(threadId);
        if (removedState != null) {
            // 관련 체크포인트도 정리
            checkpoints.entrySet().removeIf(entry -> 
                entry.getValue().getThreadId().equals(threadId));
            log.info("스레드 삭제: {}", threadId);
        }
    }
    
    /**
     * 전체 스레드 목록 가져오기
     * 
     * @return 스레드 ID 목록
     */
    public java.util.Set<String> getAllThreadIds() {
        return threadStates.keySet();
    }
    
    /**
     * 메모리 상태 정보
     * 
     * @return 메모리 사용 현황
     */
    public String getMemoryStatus() {
        return String.format(
            "활성 스레드: %d개, 체크포인트: %d개", 
            threadStates.size(), 
            checkpoints.size()
        );
    }
    
    /**
     * 메모리 정리 (오래된 항목 삭제)
     * 
     * @param hoursToKeep 유지할 시간 (시간 단위)
     */
    public void cleanupOldMemories(int hoursToKeep) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(hoursToKeep);
        
        java.util.concurrent.atomic.AtomicInteger removedThreads = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicInteger removedCheckpoints = new java.util.concurrent.atomic.AtomicInteger(0);
        
        // 오래된 스레드 정리
        threadStates.entrySet().removeIf(entry -> {
            if (entry.getValue().getTimestamp().isBefore(cutoffTime)) {
                removedThreads.incrementAndGet();
                return true;
            }
            return false;
        });
        
        // 오래된 체크포인트 정리
        checkpoints.entrySet().removeIf(entry -> {
            if (entry.getValue().getTimestamp().isBefore(cutoffTime)) {
                removedCheckpoints.incrementAndGet();
                return true;
            }
            return false;
        });
        
        log.info("메모리 정리 완료: 스레드 {}개, 체크포인트 {}개 삭제", 
            removedThreads.get(), removedCheckpoints.get());
    }
}