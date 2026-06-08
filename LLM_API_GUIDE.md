# LLM API 模块使用指南

## 概述

LLM API 模块为 Google AI Edge Gallery 应用提供本地大语言模型推理的统一接口。所有推理过程都在设备本地完成，确保 100% 隐私。

## 核心特性

- **本地推理**：所有 LLM 计算都在设备上进行，无需网络连接
- **隐私第一**：用户数据永不离开设备
- **流式生成**：支持实时流式文本生成，改善用户体验
- **多轮对话**：支持完整的多轮对话历史管理
- **参数控制**：灵活的生成参数配置（温度、TopK、TopP等）
- **错误处理**：完整的异常处理机制

## API 接口

### LLMApiService

核心服务接口，提供以下方法：

```kotlin
// 单次文本生成
suspend fun generateText(
  prompt: String,
  params: GenerationParams = GenerationParams()
): TextResponse

// 流式文本生成
fun generateTextStream(
  prompt: String,
  params: GenerationParams = GenerationParams()
): Flow<TextStreamResponse>

// 多轮对话
suspend fun chat(
  messages: List<ChatMessage>,
  params: GenerationParams = GenerationParams()
): ChatResponse

// 流式多轮对话
fun chatStream(
  messages: List<ChatMessage>,
  params: GenerationParams = GenerationParams()
): Flow<ChatStreamResponse>

// 获取模型信息
suspend fun getModelInfo(): ModelInfo

// 取消生成操作
suspend fun cancelGeneration()
```

## 使用示例

### 1. 单次文本生成

```kotlin
class MyViewModel @Inject constructor(
  private val llmService: LLMApiService
) : ViewModel() {
  
  fun generateText() {
    viewModelScope.launch {
      try {
        val response = llmService.generateText(
          prompt = "写一首关于春天的诗",
          params = GenerationParams(
            temperature = 0.7f,
            maxOutputTokens = 512
          )
        )
        // 使用生成的文本
        println(response.text)
      } catch (e: LLMException) {
        // 处理错误
        println("生成失败: ${e.message}")
      }
    }
  }
}
```

### 2. 流式文本生成

```kotlin
fun generateTextStream() {
  viewModelScope.launch {
    try {
      llmService.generateTextStream(
        prompt = "解释什么是人工智能"
      ).collect { response ->
        if (!response.isComplete) {
          // 实时接收文本片段
          println("Chunk ${response.chunkIndex}: ${response.chunk}")
        } else {
          println("生成完成")
        }
      }
    } catch (e: LLMException) {
      println("流生成失败: ${e.message}")
    }
  }
}
```

### 3. 多轮对话

```kotlin
fun chat() {
  viewModelScope.launch {
    try {
      val messages = listOf(
        ChatMessage(role = "user", content = "你好，你叫什么名字?"),
      )
      
      val response = llmService.chat(messages)
      
      val updatedMessages = messages + response.message
      
      // 继续对话
      val response2 = llmService.chat(
        messages = updatedMessages + ChatMessage(
          role = "user",
          content = "你能帮我写代码吗?"
        )
      )
    } catch (e: LLMException) {
      println("对话失败: ${e.message}")
    }
  }
}
```

### 4. 使用 ViewModel 和 Compose UI

```kotlin
@Composable
fun ChatScreen(
  viewModel: LLMViewModel = hiltViewModel()
) {
  val chatMessages by viewModel.chatMessages.collectAsState()
  val generationState by viewModel.generationState.collectAsState()
  
  Column {
    LazyColumn(Modifier.weight(1f)) {
      items(chatMessages) { message ->
        ChatMessageItem(message)
      }
    }
    
    TextField(
      value = inputText,
      onValueChange = { inputText = it }
    )
    
    Button(
      onClick = {
        viewModel.sendChatMessageStream(inputText)
      }
    ) {
      Text("发送")
    }
  }
}
```

## 生成参数说明

### GenerationParams

```kotlin
data class GenerationParams(
  // 采样温度 (0.0-2.0)
  // 值越低输出越确定性，值越高输出越随机
  val temperature: Float = 0.7f,
  
  // 仅从前 K 个最有可能的词中采样
  val topK: Int = 40,
  
  // 核采样，仅从概率累积达到此值的词中采样 (0.0-1.0)
  val topP: Float = 0.95f,
  
  // 最大输出 token 数
  val maxOutputTokens: Int = 512,
  
  // 停止序列列表
  val stopSequences: List<String> = emptyList()
)
```

## 异常处理

API 提供了结构化的异常类型：

```kotlin
sealed class LLMException(message: String) : Exception(message) {
  // 模型未加载
  class ModelNotLoadedException(message: String) : LLMException(message)
  
  // 生成失败
  class GenerationFailedException(message: String) : LLMException(message)
  
  // 无效参数
  class InvalidParametersException(message: String) : LLMException(message)
  
  // 内存不足
  class InsufficientMemoryException(message: String) : LLMException(message)
}
```

## 依赖注入

模块使用 Hilt 进行依赖注入。在您的 Activity 或 ViewModel 中自动注入：

```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
  private val llmService: LLMApiService
) : ViewModel() {
  // 使用 llmService
}
```

## UI 组件

### LLMChatScreen

多轮对话界面，包含：
- 消息列表显示
- 参数控制面板
- 实时输入框
- 流式文本显示
- 加载和错误状态

### LLMGenerateScreen

文本生成界面，包含：
- 提示词输入
- 参数设置
- 流式/直接生成选项
- 结果显示

## 性能建议

1. **使用流式生成**：对于长文本生成，使用流式 API 获得更好的用户体验
2. **参数调优**：根据用例调整温度和 TopK 参数
3. **Token 限制**：合理设置 `maxOutputTokens` 以避免过长生成
4. **内存管理**：避免在内存受限设备上使用过大的模型

## 已知限制

- 上下文窗口受模型大小限制
- 流式生成可能有延迟
- GPU 加速在某些设备上的精度可能不同

## 支持和反馈

如有问题或建议，请在 GitHub 上提交 Issue。

---

详细 API 文档请查看代码注释。
