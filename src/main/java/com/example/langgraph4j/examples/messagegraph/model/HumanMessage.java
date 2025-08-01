package com.example.langgraph4j.examples.messagegraph.model;

import lombok.NoArgsConstructor;

/**
 * 사용자 메시지 클래스
 * 
 * Python 예제의 HumanMessage를 Java로 구현한 클래스입니다.
 * 
 * @author Claude AI Assistant
 * @since 2025-07-31
 */
@NoArgsConstructor  
public class HumanMessage extends Message {
    
    public HumanMessage(String content) {
        super(content);
    }
    
    @Override
    public MessageType getType() {
        return MessageType.HUMAN;
    }
}