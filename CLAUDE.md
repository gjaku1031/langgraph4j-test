# LangGraph4j Test Project - 버전 검증 및 설정 가이드

## 프로젝트 개요
이 프로젝트는 Python의 LangGraph 예제를 Java/Spring AI 환경으로 변환한 학습용 프로젝트입니다.

## 라이브러리 버전 검증 (2025년 7월 31일 기준)

### 1. Spring AI
- **최신 안정화 버전**: 1.0.0 (GA - General Availability)
- **출시일**: 2025년 5월 20일
- **검증 방법**: 
  - GitHub 릴리즈 페이지: https://github.com/spring-projects/spring-ai/releases
  - Spring 공식 블로그: https://spring.io/blog/2025/05/20/spring-ai-1-0-GA-released/
- **주요 특징**:
  - 20개 이상의 AI 모델 지원 (Anthropic, OpenAI, Google Vertex AI 등)
  - Tool Calling (Function Calling) 기능
  - 주요 벡터 데이터베이스 지원
  - Chat 대화 메모리 및 RAG (Retrieval Augmented Generation) 지원

### 2. LangChain4j
- **최신 안정화 버전**: 1.1.0
- **출시일**: 2025년 6월 19일
- **검증 방법**:
  - Maven Central: https://mvnrepository.com/artifact/dev.langchain4j/langchain4j
  - GitHub 릴리즈: https://github.com/langchain4j/langchain4j/releases
- **BOM 버전**: 1.1.0 (모든 모듈의 최신 버전 포함)
- **주요 모듈**:
  - langchain4j-core: 1.1.0
  - langchain4j-open-ai: 1.1.0
  - 커뮤니티 모듈: 1.1.0-beta7

### 3. LangGraph4j
- **최신 버전**: 1.6.0-rc2 (Release Candidate)
- **최신 안정화 버전**: 1.5.12
- **출시일**: 2025년 5월 28일 (core 모듈 기준)
- **검증 방법**:
  - Maven Central: https://mvnrepository.com/artifact/org.bsc.langgraph4j
  - GitHub: https://github.com/langgraph4j/langgraph4j
- **Java 버전 요구사항**: Java 17 이상
- **주요 모듈**:
  - langgraph4j-core: 상태 머신 및 그래프 구조 핵심 기능
  - langgraph4j-langchain4j: LangChain4j 통합
  - langgraph4j-spring-ai: Spring AI 통합

## 환경 설정

### build.gradle 설정
```gradle
ext {
    springAiVersion = '1.0.0'
    langchain4jVersion = '1.1.0'
    langgraph4jVersion = '1.6.0-rc2'
}

dependencyManagement {
    imports {
        mavenBom "org.springframework.ai:spring-ai-bom:${springAiVersion}"
        mavenBom "dev.langchain4j:langchain4j-bom:${langchain4jVersion}"
        mavenBom "org.bsc.langgraph4j:langgraph4j-bom:${langgraph4jVersion}"
    }
}
```

### 필수 환경 변수 (application.properties)
```properties
# OpenAI API 설정
spring.ai.openai.api-key=${OPENAI_API_KEY}
spring.ai.openai.chat.options.model=gpt-4o-mini

# Google Vertex AI 설정 (선택사항)
spring.ai.vertex.ai.gemini.project-id=${GOOGLE_PROJECT_ID}
spring.ai.vertex.ai.gemini.location=us-central1
spring.ai.vertex.ai.gemini.chat.options.model=gemini-1.5-flash

# Vector Store 설정 (Chroma)
spring.ai.vectorstore.chroma.collection-name=restaurant-docs
spring.ai.vectorstore.chroma.server-url=http://localhost:8000

# Tavily Search API (Tool Calling 예제용)
tavily.api.key=${TAVILY_API_KEY}
```

## 프로젝트 구조

```
langgraph4j_test/
├── src/main/java/com/example/
│   ├── config/          # Spring 설정 클래스
│   ├── controller/      # REST API 컨트롤러
│   ├── service/         # 비즈니스 로직
│   ├── examples/        # 각 예제별 구현
│   │   ├── toolcalling/     # PRJ_01: Tool Calling
│   │   ├── stategraph/      # PRJ_02: State Graph
│   │   ├── messagegraph/    # PRJ_03: Message Graph
│   │   ├── reactmemory/     # PRJ_04: ReAct + Memory
│   │   └── agenticrag/      # PRJ_05: Agentic RAG
│   └── util/            # 유틸리티 클래스
├── src/main/resources/
│   ├── application.properties
│   └── data/            # 레스토랑 메뉴 데이터
│       ├── restaurant_menu.txt
│       └── restaurant_wine.txt
└── CLAUDE.md            # 이 파일
```

## 예제 구현 목록

1. **PRJ_01_LangChain_ToolCalling**: Tool/Function Calling 기본 구현
   - Tavily 웹 검색 도구
   - 사용자 정의 도구
   - 벡터 저장소 검색
   - Few-shot 프롬프팅

2. **PRJ_02_LangGraph_StateGraph**: 상태 기반 그래프 구현
   - 상태 관리 및 전이
   - 조건부 엣지
   - 순환 그래프 처리

3. **PRJ_03_LangGraph_MessageGraph**: 메시지 기반 그래프 구현
   - 메시지 히스토리 관리
   - 멀티 에이전트 시스템

4. **PRJ_04_LangGraph_ReAct_Memory**: ReAct 패턴 + 메모리
   - 추론과 행동의 반복
   - 장기 메모리 저장

5. **PRJ_05_LangGraph_AgenticRAG**: 에이전틱 RAG 구현
   - 문서 검색 및 인덱싱
   - 질의 재작성
   - 답변 생성 및 검증

## 실행 방법

1. 환경 변수 설정
```bash
export OPENAI_API_KEY="your-api-key"
export TAVILY_API_KEY="your-tavily-key"
```

2. 프로젝트 빌드
```bash
./gradlew clean build
```

3. 애플리케이션 실행
```bash
./gradlew bootRun
```

4. API 테스트
```bash
# Tool Calling 예제
curl -X POST http://localhost:8080/api/examples/toolcalling \
  -H "Content-Type: application/json" \
  -d '{"query": "스테이크와 어울리는 와인 추천"}'
```

## 참고 자료

- [Spring AI 공식 문서](https://docs.spring.io/spring-ai/reference/)
- [LangChain4j 공식 문서](https://docs.langchain4j.dev/)
- [LangGraph4j GitHub](https://github.com/langgraph4j/langgraph4j)
- [원본 Python 예제](https://github.com/langchain-ai/langgraph)

## 업데이트 기록

- 2025-07-31: 초기 프로젝트 설정 및 최신 버전 검증