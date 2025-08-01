# PRJ_05: LangGraph Agentic RAG

## =� �

Python LangGraphX Agentic RAG (Retrieval-Augmented Generation) (4D Java\ l\ ���. �� ��1, ��� 8 ��, �� � , �  D �t ��� ��D �1X� �	 RAG ܤ\���.

## <� D�M�

```
agenticrag/
   controller/
      AgenticRAGController.java    # REST API ���x�
   service/
      AgenticRAGService.java      # Agentic RAG Tx \�
      DocumentRetriever.java      # 8 �� D�
      QueryRewriter.java          # �� ��1 D�
   model/
       Document.java               # 8 �x
       AgenticRAGState.java       # RAG �� �x
       QueryRewriteResult.java    # �� ��1 ��
```

## = Agentic RAG �� \�

### � t|x

```mermaid
graph TD
    A[��� �8] --> B[�� �]
    B --> C[�� ��1]
    C --> D[8 ��]
    D --> E[ (1 D0�]
    E --> F[�� �1]
    F --> G[�� � ]
    
    G --> H{�� ��?}
    H -->|YES e0.7| I[D�]
    H -->|NO <0.7| J{���  �?}
    
    J -->|YES <3�| K[��  ]
    J -->|NO e3�| L[ D�]
    
    K --> D
    L --> I
    
    style A fill:#e1f5fe
    style I fill:#c8e6c9
    style H fill:#ffeb3b
    style J fill:#ffeb3b
    style K fill:#ffcdd2
```

### 8 �� ܤ\

```mermaid
graph LR
    A[�� ��] --> B[��� ��]
    B --> C[��x ��]
    C --> D[TF-IDF �T��]
    D --> E[� K  �]
    
    F[�0 ��)] --> G[ �� İ]
    G --> H[Xt�� �T�]
    
    E --> H
    H --> I[\� 8 �i]
    
    style A fill:#e3f2fd
    style I fill:#c8e6c9
```

## =� u� l

### 1. Agentic RAG �� �x

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgenticRAGState {
    private String originalQuery;                     // �� �8
    private String refinedQuery;                     //   �8
    @Builder.Default
    private List<Document> documents = new ArrayList<>();        // �� 8
    @Builder.Default
    private List<Document> relevantDocuments = new ArrayList<>(); //  ( 8
    private String answer;                           // �1 ��
    private Double qualityScore;                     // �� 
    private ProcessingStep currentStep;              // � ��
    @Builder.Default
    private List<String> searchQueries = new ArrayList<>();      // �� ���
    @Builder.Default
    private Integer generationAttempts = 0;          // �� �
    @Builder.Default
    private Integer maxAttempts = 3;                 // \  ��
    private String sessionId;                        // 8X ID
    
    public enum ProcessingStep {
        STARTED,              // ܑ
        QUERY_ANALYSIS,       // �� �
        QUERY_REFINEMENT,     // ��  
        DOCUMENT_RETRIEVAL,   // 8 ��
        RELEVANCE_FILTERING,  //  (1 D0�
        ANSWER_GENERATION,    // �� �1
        QUALITY_EVALUATION,   // �� � 
        COMPLETED,            // D�
        FAILED                // �(
    }
}
```

### 2. 8 �� D�

```java
@Service
public class DocumentRetriever {
    
    // 8  ��
    private final Map<String, Document> documentStore = new ConcurrentHashMap<>();
    
    // ��x (� -> 8 ID �])
    private final Map<String, Set<String>> invertedIndex = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void initializeDocuments() {
        loadRestaurantDocuments();
        log.info("8 xq� D�: {} 8", documentStore.size());
    }
    
    // TF-IDF 0 8 ��
    public List<Document> searchDocuments(String query, int maxResults) {
        Set<String> queryWords = extractWords(query);
        Map<String, Double> documentScores = calculateDocumentScores(queryWords);
        
        return documentScores.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(maxResults)
            .map(entry -> {
                Document doc = documentStore.get(entry.getKey()).copy();
                doc.setRelevanceScore(entry.getValue());
                return doc;
            })
            .collect(Collectors.toList());
    }
    
    // TF-IDF �T� İ
    private Map<String, Double> calculateDocumentScores(Set<String> queryWords) {
        Map<String, Double> scores = new HashMap<>();
        int totalDocuments = documentStore.size();
        
        for (String word : queryWords) {
            Set<String> documentIds = invertedIndex.get(word);
            if (documentIds == null) continue;
            
            // IDF İ
            double idf = Math.log((double) totalDocuments / documentIds.size());
            
            for (String docId : documentIds) {
                Document doc = documentStore.get(docId);
                if (doc == null) continue;
                
                // TF İ
                long tf = countWordOccurrences(doc.getContent(), word);
                
                // TF-IDF �T�
                double tfidf = tf * idf;
                scores.merge(docId, tfidf, Double::sum);
            }
        }
        
        return scores;
    }
}
```

### 3. �� ��1 D�

```java
@Service
public class QueryRewriter {
    
    // X�� ��� �Q
    private static final Map<QueryIntent, List<String>> INTENT_KEYWORDS = Map.of(
        QueryIntent.MENU_SEARCH, List.of("Tt", "L�", "��", "�Ltl"),
        QueryIntent.WINE_PAIRING, List.of("@x", "���", "����", "��"),
        QueryIntent.PRICE_INQUIRY, List.of(" �", "��", "D�", "�")
    );
    
    public QueryRewriteResult rewriteQuery(String originalQuery) {
        // 1. � 0 `x ��
        QueryRewriteResult ruleBasedResult = applyRuleBasedRewriting(originalQuery);
        if (ruleBasedResult.getConfidenceScore() >= 0.8) {
            return ruleBasedResult;
        }
        
        // 2. AI 0 ��1 (L�� ���� � 0 ��)
        if (isTestMode()) {
            return ruleBasedResult;
        }
        
        return performAiRewriting(originalQuery);
    }
    
    private QueryRewriteResult applyRuleBasedRewriting(String query) {
        String normalizedQuery = query.toLowerCase().trim();
        
        // ��� ��
        List<String> keywords = extractKeywords(normalizedQuery);
        
        // X� �
        QueryIntent intent = classifyIntent(normalizedQuery, keywords);
        
        // �� ��1
        String rewrittenQuery = enhanceQuery(query, intent, keywords);
        
        return QueryRewriteResult.builder()
            .originalQuery(query)
            .rewrittenQuery(rewrittenQuery)
            .extractedKeywords(keywords)
            .intent(intent)
            .confidenceScore(calculateConfidence(query, rewrittenQuery, keywords, intent))
            .reason(generateRewriteReason(intent, keywords))
            .build();
    }
}
```

### 4. Agentic RAG Tx D�

```java
@Service
public class AgenticRAGService {
    
    public AgenticRAGState executeAgenticRAG(String query, String sessionId) {
        log.info("=== Agentic RAG ܑ: {} ===", query);
        
        AgenticRAGState state = AgenticRAGState.builder()
            .originalQuery(query)
            .sessionId(sessionId != null ? sessionId : generateSessionId())
            .currentStep(ProcessingStep.STARTED)
            .build();
        
        try {
            // 1��: �� �  ��1
            state = performQueryAnalysis(state);
            
            // 2��: 8 ��
            state = performDocumentRetrieval(state);
            
            // 3��:  (1 D0�
            state = performRelevanceFiltering(state);
            
            // 4��: �� �1 (\  3� ��)
            while (!state.hasSufficientQuality() && !state.hasReachedMaxAttempts()) {
                state = performAnswerGeneration(state);
                state = performQualityEvaluation(state);
                state.incrementAttempts();
                
                if (!state.hasSufficientQuality() && !state.hasReachedMaxAttempts()) {
                    state = improveQueryAndRetry(state);
                }
            }
            
            state.markCompleted();
            
        } catch (Exception e) {
            state.markFailed(e.getMessage());
        }
        
        return state;
    }
}
```

### 5. �� �  ܤ\

```java
private AgenticRAGState performQualityEvaluation(AgenticRAGState state) {
    if (isTestMode()) {
        double score = evaluateTestQuality(state);
        state.setQualityScore(score);
        return state;
    }
    
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
    
    return state;
}

private double evaluateTestQuality(AgenticRAGState state) {
    double score = 0.5;
    
    // �� 8t� 0x  �
    if (state.getAnswer() != null) {
        int length = state.getAnswer().length();
        if (length > 100) score += 0.2;
        if (length > 200) score += 0.1;
    }
    
    //  ( 8 � 0x  �
    score += Math.min(state.getRelevantDocuments().size() * 0.1, 0.2);
    
    return Math.min(score, 1.0);
}
```

## < API ���x�

### 1. �8 �� (RAG t|x)

```bash
POST /api/agentic-rag/ask
Content-Type: application/json

{
  "query": "ۈ� �Ltl Tt ��",
  "sessionId": "test_session_02"
}
```

**Q� �:**
```json
{
  "sessionId": "test_session_02",
  "originalQuery": "ۈ� �Ltl Tt ��",
  "refinedQuery": "ۈ� �Ltl Tt ��",
  "answer": "'ۈ� �Ltl Tt ��'�  \ �| >X���...",
  "qualityScore": 1.0,
  "currentStep": "COMPLETED",
  "documentCount": 2,
  "relevantDocumentCount": 2,
  "generationAttempts": 1,
  "processingTimeSeconds": 0,
  "summary": "��: COMPLETED, 8: 2,  (8: 2, ��: 1/3, ���: 0",
  "searchQueries": ["ۈ� �Ltl Tt ��"],
  "progressPercentage": 100.0,
  "success": true
}
```

### 2. 8 ��

```bash
GET /api/agentic-rag/search?query=@x&maxResults=5
```

**Q� �:**
```json
{
  "query": "@x",
  "documents": [
    {
      "id": "wine_1",
      "title": "� �� 2015",
      "content": "�t� T� ��X ��� @x<\...",
      "source": "restaurant_wine.txt",
      "type": "WINE",
      "relevanceScore": 0.89
    }
  ],
  "totalCount": 5,
  "success": true
}
```

### 3. ܤ\ ��

```bash
GET /api/agentic-rag/status
```

**Q� �:**
```json
{
  "documentCount": 20,
  "documentsByType": {
    "MENU": 10,
    "WINE": 10
  },
  "indexStatus": "8: 20, xq� �: 468, �� 8� �: 1.7",
  "systemTime": "2025-08-01T09:16:47",
  "availableDocumentTypes": ["MENU", "WINE", "RECIPE", "REVIEW", "GENERAL"],
  "features": [
    "Query Rewriting",
    "Multi-step Retrieval",
    "Quality Evaluation",
    "Iterative Improvement"
  ]
}
```

## =� 8 xq� ܤ\

### xq� lp

```
8  �� (Document Store)
   menu_1: {id, content, title, type=MENU}
   menu_2: {id, content, title, type=MENU}
   wine_1: {id, content, title, type=WINE}
   wine_2: {id, content, title, type=WINE}

��x (Inverted Index)
   "�Ltl" � [menu_1, menu_3]
   "@x" � [wine_1, wine_2, wine_3]
   "�" � [wine_1, wine_4]
   " �" � [menu_1, menu_2, wine_1]

8 H� (Document Frequency)
   "�Ltl": 2
   "@x": 3
   "�": 2
   " �": 3
```

### TF-IDF İ �

```
��: "� @x"

8 1 (wine_1):
- TF("�") = 2 (8 � � �)
- IDF("�") = log(20/2) = 2.3
- TF-IDF("�") = 2 * 2.3 = 4.6

- TF("@x") = 3
- IDF("@x") = log(20/3) = 1.9
- TF-IDF("@x") = 3 * 1.9 = 5.7

 �T� = 4.6 + 5.7 = 10.3
```

## <� �� ��

### 1. �� ��1
- **X� �**: MENU_SEARCH, WINE_PAIRING, RECOMMENDATION �
- **��� ��**: \ /8 �� ��
- **��  **: �8\ \D l�<\ �X

### 2. Xt�� ��
- **��� �m**: `x U �m
- **TF-IDF �T��**:  (� 0 
- **��� D0�**: 8 ��� �� ��

### 3. �� �
- **�� �� � **: 0.0-1.0  ��
- **�  **: �� �� � ���
- **�� U�**: �  ���\ ��  

### 4. ��� ��
- **t|x D�M�**:  ��� Ž ��
- **ĉ` �**: �� ĉ �i �
- **�� �l**: ��� �( ��

## =� 1� �\

| �\ |  | $� |
|------|----|----|
| **8 xq�** | 20 8, 468 � | 0 \� � |
| **�� �� �** | <100ms | TF-IDF �T�� �h |
| **�� ��1 �** | <50ms | � 0 �� |
| **� Q� �** | <1 | L�� �� 0  |
| **�� ** | 90-100% | �� ��  |
| **���(** | ~30% | ��  D \ ��� |

## =' U�  �1

### 1. �0 �� � 

```java
// ��) 0  �� ��
@Service
public class VectorSearchService {
    
    @Autowired
    private EmbeddingModel embeddingModel;
    
    public List<Document> vectorSearch(String query, int k) {
        float[] queryEmbedding = embeddingModel.embed(query);
        
        return documentStore.values().stream()
            .map(doc -> {
                float[] docEmbedding = getOrComputeEmbedding(doc);
                double similarity = cosineSimilarity(queryEmbedding, docEmbedding);
                doc.setRelevanceScore(similarity);
                return doc;
            })
            .sorted((d1, d2) -> Double.compare(
                d2.getRelevanceScore(), 
                d1.getRelevanceScore()
            ))
            .limit(k)
            .collect(Collectors.toList());
    }
}
```

### 2. @�� ��

```java
// t�� + M�� ��
public class MultimodalRetriever {
    
    public List<Document> searchWithImage(String textQuery, byte[] imageData) {
        // CLIP �xD ��\ t��-M�� �m
        float[] imageEmbedding = clipModel.encodeImage(imageData);
        float[] textEmbedding = clipModel.encodeText(textQuery);
        
        // Xt�� �T� İ
        return calculateHybridScores(imageEmbedding, textEmbedding);
    }
}
```

### 3. ��� Q�

```java
// Server-Sent Events| �\ �� Q�
@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<ServerSentEvent<String>> streamRAG(@RequestParam String query) {
    return Flux.create(sink -> {
        AgenticRAGState state = AgenticRAGState.builder()
            .originalQuery(query)
            .build();
        
        //  ��� t�� �
        sink.next(createEvent("�� � ...", "QUERY_ANALYSIS"));
        state = performQueryAnalysis(state);
        
        sink.next(createEvent("8 �� ...", "DOCUMENT_RETRIEVAL"));
        state = performDocumentRetrieval(state);
        
        // ... �8� ���
        
        sink.complete();
    });
}
```

## = 8 t�

### 1. T�� �q
**8**:  � 8 \� � OutOfMemoryError
**t�**: 
```java
// 8 �t�  LRU �� �
@Configuration
public class CacheConfig {
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("documents") {
            @Override
            protected Cache createConcurrentMapCache(String name) {
                return new ConcurrentMapCache(name, 
                    CacheBuilder.newBuilder()
                        .maximumSize(1000)
                        .expireAfterAccess(10, TimeUnit.MINUTES)
                        .build()
                        .asMap(), 
                    false);
            }
        };
    }
}
```

### 2. �� 1�  X
**8**: 8  � � 0x �� ��  X
**t�**: �, ��  xq� \T
```java
// �, ���D ��\ �� \T
public List<Document> searchDocumentsParallel(String query, int maxResults) {
    return documentStore.values().parallelStream()
        .map(doc -> scoreDocument(doc, query))
        .filter(pair -> pair.getValue() > 0)
        .sorted(Map.Entry.<Document, Double>comparingByValue().reversed())
        .limit(maxResults)
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());
}
```

## =� �� �

```java
// \�� )� 8�
@Autowired
private AgenticRAGService ragService;

// �\ �8
AgenticRAGState result = ragService.executeAgenticRAG(
    "$�X �� Tt�?", 
    "session_123"
);

System.out.println("��: " + result.getAnswer());
System.out.println("�� : " + result.getQualityScore());
System.out.println("�� 8: " + result.getRelevantDocuments().size());

// ��\ �8 (��� �  �)
AgenticRAGState complexResult = ragService.executeAgenticRAG(
    "30,000� tXX @x� � ���� �Ltl Tt| �8� $�t�8�", 
    "session_456"
);

System.out.println("�� �: " + complexResult.getGenerationAttempts());
System.out.println("�� ���: " + complexResult.getSearchQueries());
```

## =  ( T�

- [AgenticRAGService.java](../src/main/java/com/example/langgraph4j/examples/agenticrag/service/AgenticRAGService.java)
- [DocumentRetriever.java](../src/main/java/com/example/langgraph4j/examples/agenticrag/service/DocumentRetriever.java)
- [QueryRewriter.java](../src/main/java/com/example/langgraph4j/examples/agenticrag/service/QueryRewriter.java)
- [AgenticRAGState.java](../src/main/java/com/example/langgraph4j/examples/agenticrag/model/AgenticRAGState.java)

---

t 8� ��<\ �pt�)��. �8t�  �m@ GitHub Issues\ Ht �8�.