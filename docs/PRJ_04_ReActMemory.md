# PRJ_04: LangGraph ReAct Memory

## =� �

Python LangGraphX ReAct (Reasoning + Acting) (4D Java\ l\ ���. ��� �`� ��D �Xp,  T T��| �t �M��|  �X� �	 �t� ܤ\���.

## <� D�M�

```
reactmemory/
   controller/
      ReActMemoryController.java    # REST API ���x�
   service/
      ReActAgentService.java       # ReAct �t� \�
      MemoryManager.java           # T��  � D�
   model/
       ReActState.java              # ReAct �� �x
       ReActMessage.java            # T�� �x
       ToolCall.java                # �l 8� �x
```

## = ReAct �� \�

### ReAct �tt

```mermaid
graph TD
    A[��� �%] --> B[T�� \�]
    B --> C[Reasoning/�`]
    C --> D{�l 8� D�?}
    
    D -->|YES| E[Acting/��]
    E --> F[�l �]
    F --> G[Observation/ 0]
    G --> H{�\ �1?}
    
    D -->|NO| I[\� �� �1]
    
    H -->|NO| C
    H -->|YES| I
    
    I --> J[T��  �]
    J --> K[Q� X]
    
    style A fill:#e1f5fe
    style K fill:#c8e6c9
    style C fill:#fff3e0
    style E fill:#ffcdd2
    style G fill:#f8bbd0
```

### T�� ܤ\

```mermaid
graph LR
    A[Thread ID] --> B[Thread State]
    B --> C[Messages]
    B --> D[Tool Calls]
    B --> E[Reasoning History]
    
    F[Checkpoint] --> G[State Snapshot]
    G --> H[Timestamp]
    G --> I[Restore Point]
    
    style A fill:#e3f2fd
    style F fill:#f3e5f5
```

## =� u� l

### 1. ReAct �� �x

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReActState {
    private String threadId;                          //  T �� ID
    @Builder.Default
    private List<ReActMessage> messages = new ArrayList<>();  // T�� ����
    @Builder.Default
    private List<ToolCall> toolCalls = new ArrayList<>();     // �l 8� 0]
    private String currentStep;                       // � ��
    private String reasoning;                         // � �`
    private String action;                            // � ��
    private String observation;                       //  0 ��
    private String summary;                           // �}
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();     // ����
    
    // T�� � 
    public void addMessage(String role, String content) {
        this.messages.add(ReActMessage.builder()
            .role(role)
            .content(content)
            .timestamp(LocalDateTime.now())
            .build());
    }
    
    // �l 8� � 
    public void addToolCall(String tool, String args, String result) {
        this.toolCalls.add(ToolCall.builder()
            .toolName(tool)
            .arguments(args)
            .result(result)
            .timestamp(LocalDateTime.now())
            .status("SUCCESS")
            .build());
    }
}
```

### 2. ReAct �t� D�

```java
@Service
public class ReActAgentService {
    
    private static final int MAX_ITERATIONS = 5;  // \  �` �
    
    public ReActState executeReActAgent(String query, String threadId) {
        // 1. T��� �� \� � �\ �1
        ReActState state = memoryManager.getOrCreateThread(threadId);
        state.addMessage("user", query);
        
        // 2. ReAct �tt �
        int iteration = 0;
        while (iteration < MAX_ITERATIONS) {
            // Reasoning ��
            state = reasoningStep(state);
            
            if (state.getCurrentStep().equals("final_answer")) {
                break;
            }
            
            // Acting ��
            state = actionStep(state);
            
            // Observation ��
            state = observationStep(state);
            
            iteration++;
        }
        
        // 3. \� Q� �1
        if (!state.getCurrentStep().equals("final_answer")) {
            state = generateFinalAnswer(state);
        }
        
        // 4. T���  �
        memoryManager.saveThread(threadId, state);
        
        return state;
    }
}
```

### 3. Reasoning ��

```java
private ReActState reasoningStep(ReActState state) {
    String conversationContext = buildConversationContext(state);
    
    String prompt = REACT_SYSTEM_PROMPT + "\n\n" +
        " T �M��:\n" + conversationContext + "\n\n" +
        "�L ��| �X8�.";
    
    String reasoning = chatClient.prompt()
        .user(prompt)
        .call()
        .content();
    
    // �` �
    if (reasoning.contains("Thought:")) {
        String thought = extractBetween(reasoning, "Thought:", "Action:");
        state.setReasoning(thought.trim());
    }
    
    if (reasoning.contains("Final Answer:")) {
        String answer = extractAfter(reasoning, "Final Answer:");
        state.setCurrentStep("final_answer");
        state.setSummary(answer.trim());
    } else {
        state.setCurrentStep("need_action");
    }
    
    return state;
}
```

### 4. Action ��

```java
private ReActState actionStep(ReActState state) {
    String reasoning = state.getReasoning();
    
    if (reasoning.contains("Action:")) {
        String actionStr = extractBetween(reasoning, "Action:", "Action Input:");
        String actionInput = extractAfter(reasoning, "Action Input:");
        
        state.setAction(actionStr.trim() + ": " + actionInput.trim());
        
        // �l �
        String result = executeTool(actionStr.trim(), actionInput.trim());
        state.addToolCall(actionStr.trim(), actionInput.trim(), result);
        state.setObservation(result);
    }
    
    return state;
}

private String executeTool(String toolName, String input) {
    return switch (toolName.toLowerCase()) {
        case "search_menu" -> restaurantSearchTools.searchMenu(input);
        case "search_wine" -> restaurantSearchTools.searchWine(input);
        case "search_web" -> tavilySearchTool.searchWeb(input);
        default -> "Unknown tool: " + toolName;
    };
}
```

### 5. T��  �

```java
@Service
public class MemoryManager {
    
    // ��� ��  �
    private final ConcurrentHashMap<String, ReActState> threadStates = new ConcurrentHashMap<>();
    
    // �l�x�  �
    private final ConcurrentHashMap<String, ReActState> checkpoints = new ConcurrentHashMap<>();
    
    // �� �1 � \�
    public ReActState getOrCreateThread(String threadId) {
        return threadStates.computeIfAbsent(threadId, id -> 
            ReActState.builder()
                .threadId(id)
                .currentStep("started")
                .build()
        );
    }
    
    // �l�x� �1
    public void createCheckpoint(String threadId) {
        ReActState currentState = threadStates.get(threadId);
        if (currentState != null) {
            ReActState checkpoint = deepCopy(currentState);
            String checkpointId = threadId + "_" + System.currentTimeMillis();
            checkpoints.put(checkpointId, checkpoint);
        }
    }
    
    //  T ���� p�
    public ReActState getConversationHistory(String threadId) {
        ReActState state = threadStates.get(threadId);
        if (state == null) {
            throw new RuntimeException("Thread not found: " + threadId);
        }
        return state;
    }
}
```

## < API ���x�

### 1. �  T �� �1

```bash
POST /api/react-memory/threads
```

**Q� �:**
```json
{
  "threadId": "thread_d9bdb13b",
  "message": "�\�  T ��  �1ȵ��.",
  "success": true
}
```

### 2. ReAct  T

```bash
POST /api/react-memory/chat
Content-Type: application/json

{
  "message": "�Ltl@ ���� @xD ��t�8�",
  "threadId": "thread_d9bdb13b"
}
```

**Q� �:**
```json
{
  "threadId": "thread_d9bdb13b",
  "currentStep": "max_iterations_reached",
  "messageCount": 13,
  "toolCallCount": 1,
  "reasoning": "���� �Ltl@ ���� @x ���  \ �  ��� � �i���...",
  "action": "Tool: search_wine\nParameters: {query=�Ltl@ ���� @x}",
  "observation": "1. � �� 2015\n   "  �: �450,000\n   " �� ��: t�t$ �D�...",
  "summary": "T��: 13, �l 8�: 1�, ��: max_iterations_reached",
  "processingTimeSeconds": 0,
  "progressPercentage": 100.0,
  "lastMessage": "\  � �� ��X� ��| D�i��."
}
```

### 3.  T ���� p�

```bash
GET /api/react-memory/threads/{threadId}/history
```

**Q� �:**
```json
{
  "threadId": "thread_d9bdb13b",
  "messages": [
    {
      "role": "user",
      "content": "�Ltl@ ���� @xD ��t�8�",
      "timestamp": "2025-08-01T09:09:48"
    },
    {
      "role": "assistant",
      "content": "�Ltl@ ���� @xD >D�����.",
      "timestamp": "2025-08-01T09:09:49"
    }
  ],
  "toolCalls": [
    {
      "toolName": "search_wine",
      "arguments": "�Ltl@ ���� @x",
      "result": "� �� 2015...",
      "status": "SUCCESS",
      "timestamp": "2025-08-01T09:09:50"
    }
  ],
  "currentStep": "completed",
  "summary": "�Ltl@ ���� @x<\� t�t$ �D� 0X..."
}
```

### 4. ��  �\ �l �]

```bash
GET /api/react-memory/tools
```

**Q� �:**
```json
{
  "tools": ["search_menu", "search_wine", "search_web"],
  "description": {
    "search_menu": "��� Tt � ��",
    "search_wine": "@x �  ��� ��",
    "search_web": "�� \� � ��"
  }
}
```

## =� ReAct �` �

### 1�x �` �tt

```
���: "�Ltl@ ���� @x ��t�8�"

=== Iteration 1 ===
Thought: ���  �Ltl@ ���� @xD >� ����. 
        @x �� �l| ��t| i��.
Action: search_wine
Action Input: �Ltl@ ���� @x
Observation: � �� 2015, ��ttD 2018 �t �ȵ��.

=== Iteration 2 ===
Thought: @x �| >X���. t Tt� �Ltl �� 
        UxXt T �@ ��D `  �D � ���.
Action: search_menu
Action Input: �Ltl
Observation: ��Ș �Ltl (�35,000), \� ��...

=== Iteration 3 ===
Thought: ��\ �| ш���. \� ��D  Di��.
Final Answer: �Ltl@ ���� @x<\� �LD ��i��:
1. � �� 2015 - � �@x<\...
2. ��ttD 2018 - tȬD �| ,�x...
```

## <� �� ��

### 1. ReAct (4
- **Reasoning**: AI  � �iD �X� �L �� �
- **Acting**: D�\ �l|  �X� �
- **Observation**: �l � ��|  0X� �L �`� \�

### 2. T�� ܤ\
- **�� 0**:   T| Žx ��\  �
- **�l�x�**: �\ �X ��|  �X� ��  �
- **l  �**:  T ����@ �l 8� 0] �t

### 3. �l �i
- ** �\ �l U�**: �\� �l }� �   �
- **�l � �**: �� �l 8�� �� 0]
- **�� ��**: �l � �( � �D\ ��

### 4.  T �M��
- **� ���� \�**: t  T �� 8p
- **8� tt**:  ��, �ܴ �D ,t� t
- **��  T**: �� (@ �8-��  �

## =� 1� �\

| �\ |  | $� |
|------|----|----|
| **�� �` �** | 1-3 | �| �` �tt |
| **��  Q� �** | 3-15 | � ReAct \8� |
| **�� � �** | 2-3� | �\ �1L� |
| **�l 8� 1�`** | 100% | \� �l �� |
| **T�� ���** | ~10MB/�� | ��  T 0  |

## =' U�  �1

### 1. �\� �l � 

```java
// �( � �l � 
case "weather" -> weatherService.getWeather(input);

// İ0 �l �   
case "calculator" -> calculatorService.calculate(input);

// pt0�t� �� �l
case "db_search" -> databaseService.search(input);
```

### 2. �	 �` �

```java
// č � � 
private ReActState planningStep(ReActState state) {
    // ��\ ��D \ ��� č �
    String plan = generatePlan(state);
    state.setPlan(plan);
    return state;
}

// �0 1 � 
private ReActState reflectionStep(ReActState state) {
    // t ��X ��1 � 
    String reflection = evaluateActions(state);
    state.setReflection(reflection);
    return state;
}
```

### 3. l  ��

```java
// Redis 0 T��
@Autowired
private RedisTemplate<String, ReActState> redisTemplate;

public void saveToRedis(String threadId, ReActState state) {
    redisTemplate.opsForValue().set(
        "react:thread:" + threadId, 
        state,
        Duration.ofDays(7)
    );
}

// pt0�t�  �
@Entity
public class ReActConversation {
    @Id
    private String threadId;
    
    @Lob
    private String stateJson;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

## = 8 t�

### 1. 4\ �
**8**: ReAct �ttt ]�� JL
**t�**: MAX_ITERATIONS \ �

### 2. T�� 
**8**: $� ��  č T��� �L
**t�**: 0x T�� � �� � 

### 3. �l � �(
**8**: x� API ��D� � $X
**t�**: ��� \�  �1 ��

## =� �� �

```java
// \�� )� 8�
@Autowired
private ReActAgentService reactAgent;

// �  T ܑ
String threadId = memoryManager.createThread();

// � �� �8
ReActState result1 = reactAgent.executeReActAgent(
    "$�  A Tt ��t�8�", 
    threadId
);

// č �8 (�M��  �)
ReActState result2 = reactAgent.executeReActAgent(
    "� �  �t 3�� tXx �@?", 
    threadId
);

//  T ���� Ux
ReActState history = memoryManager.getConversationHistory(threadId);
history.getMessages().forEach(msg -> 
    System.out.println(msg.getRole() + ": " + msg.getContent())
);
```

## =  ( T�

- [ReActAgentService.java](../src/main/java/com/example/langgraph4j/examples/reactmemory/service/ReActAgentService.java)
- [MemoryManager.java](../src/main/java/com/example/langgraph4j/examples/reactmemory/service/MemoryManager.java)
- [ReActState.java](../src/main/java/com/example/langgraph4j/examples/reactmemory/model/ReActState.java)

---

t 8� ��<\ �pt�)��. �8t�  �m@ GitHub Issues\ Ht �8�.