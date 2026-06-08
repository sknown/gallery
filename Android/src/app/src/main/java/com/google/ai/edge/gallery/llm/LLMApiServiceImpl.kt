/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ai.edge.gallery.llm

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * LLM API 服务的默认实现
 * 
 * 功能特性：
 * - 基于 LiteRT 的本地 LLM 推理
 * - 支持文本生成和流式响应
 * - 线程安全的并发控制
 * - 完整的错误处理
 * - 隐私第一：所有处理都在设备本地完成
 */
@HiltViewModel
class LLMApiServiceImpl
@Inject
constructor(
  @ApplicationContext private val context: Context,
) : LLMApiService {

  private val mutex = Mutex()
  private var isGenerating = false
  private var currentModelInfo: ModelInfo? = null

  override suspend fun generateText(
    prompt: String,
    params: GenerationParams
  ): TextResponse {
    return mutex.withLock {
      try {
        validateInput(prompt)
        isGenerating = true

        // 模拟 LLM 推理过程
        // 实际实现中应使用 LiteRT/TensorFlow Lite 运行模型
        val text = performInference(prompt, params)

        TextResponse(
          text = text,
          stopReason = "stop_sequence",
          inputTokens = estimateTokens(prompt),
          outputTokens = estimateTokens(text)
        )
      } catch (e: Exception) {
        throw when (e) {
          is CancellationException -> throw e
          is LLMException -> throw e
          else -> LLMException.GenerationFailedException("Text generation failed", e)
        }
      } finally {
        isGenerating = false
      }
    }
  }

  override fun generateTextStream(
    prompt: String,
    params: GenerationParams
  ): Flow<TextStreamResponse> = flow {
    try {
      validateInput(prompt)
      isGenerating = true

      // 流式生成文本片段
      var chunkIndex = 0
      val chunks = performStreamingInference(prompt, params)

      for (chunk in chunks) {
        emit(
          TextStreamResponse(
            chunk = chunk,
            isComplete = false,
            chunkIndex = chunkIndex++
          )
        )
      }

      // 发送完成信号
      emit(
        TextStreamResponse(
          chunk = "",
          isComplete = true,
          chunkIndex = chunkIndex
        )
      )
    } catch (e: Exception) {
      if (e !is CancellationException) {
        throw LLMException.GenerationFailedException("Stream generation failed", e)
      }
    } finally {
      isGenerating = false
    }
  }

  override suspend fun chat(
    messages: List<ChatMessage>,
    params: GenerationParams
  ): ChatResponse {
    return mutex.withLock {
      try {
        validateChatMessages(messages)
        isGenerating = true

        val prompt = formatChatMessages(messages)
        val responseText = performInference(prompt, params)

        ChatResponse(
          message = ChatMessage(role = "assistant", content = responseText),
          inputTokens = estimateTokens(prompt),
          outputTokens = estimateTokens(responseText),
          stopReason = "stop_sequence"
        )
      } catch (e: Exception) {
        throw when (e) {
          is CancellationException -> throw e
          is LLMException -> throw e
          else -> LLMException.GenerationFailedException("Chat generation failed", e)
        }
      } finally {
        isGenerating = false
      }
    }
  }

  override fun chatStream(
    messages: List<ChatMessage>,
    params: GenerationParams
  ): Flow<ChatStreamResponse> = flow {
    try {
      validateChatMessages(messages)
      isGenerating = true

      val prompt = formatChatMessages(messages)
      var index = 0
      val chunks = performStreamingInference(prompt, params)

      for (chunk in chunks) {
        emit(
          ChatStreamResponse(
            delta = chunk,
            isComplete = false,
            index = index++
          )
        )
      }

      emit(
        ChatStreamResponse(
          delta = "",
          isComplete = true,
          index = index
        )
      )
    } catch (e: Exception) {
      if (e !is CancellationException) {
        throw LLMException.GenerationFailedException("Chat stream failed", e)
      }
    } finally {
      isGenerating = false
    }
  }

  override suspend fun getModelInfo(): ModelInfo {
    return currentModelInfo
      ?: throw LLMException.ModelNotLoadedException("No model currently loaded")
  }

  override suspend fun cancelGeneration() {
    mutex.withLock {
      isGenerating = false
    }
  }

  // ==================== 私有方法 ====================

  private fun validateInput(prompt: String) {
    if (prompt.isBlank()) {
      throw LLMException.InvalidParametersException("Prompt cannot be empty")
    }
  }

  private fun validateChatMessages(messages: List<ChatMessage>) {
    if (messages.isEmpty()) {
      throw LLMException.InvalidParametersException("Chat messages cannot be empty")
    }
    messages.forEach { msg ->
      if (msg.role.isBlank() || msg.content.isBlank()) {
        throw LLMException.InvalidParametersException("Invalid chat message format")
      }
    }
  }

  private fun formatChatMessages(messages: List<ChatMessage>): String {
    return messages.joinToString("\n") { "${it.role}: ${it.content}" }
  }

  /**
   * 执行本地 LLM 推理
   * 
   * 实际实现应调用 LiteRT 模型推理
   */
  private fun performInference(prompt: String, params: GenerationParams): String {
    // 模拟推理延迟
    Thread.sleep(100)

    // 这是一个占位符实现
    // 实际应使用：
    // val interpreter = Interpreter(modelBuffer)
    // val output = interpreter.run(inputArray, outputArray)
    
    return """根据提示词 "$prompt" 生成的文本。
      |温度: ${params.temperature}
      |TopK: ${params.topK}
      |TopP: ${params.topP}""".trimMargin()
  }

  /**
   * 执行流式 LLM 推理
   */
  private fun performStreamingInference(
    prompt: String,
    params: GenerationParams
  ): List<String> {
    val words =
      listOf("这是", "一个", "流式", "生成", "的", "示例", "文本。", "它", "展示了", "实时", "返回", "文本", "片段", "的", "能力。")
    return words
  }

  /**
   * 粗略估算 token 数
   */
  private fun estimateTokens(text: String): Int {
    // 简单的启发式估算：汉字和标点符号作为一个 token，英文单词平均 1.3 个 token
    val charCount = text.length
    val wordCount = text.split("\\s+".toRegex()).size
    return (charCount + wordCount * 0.3).toInt()
  }

  companion object {
    const val TAG = "LLMApiService"
  }
}

/**
 * LLM API 服务工厂类
 */
class LLMApiServiceFactory(private val context: Context) : ViewModelProvider.Factory {
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    @Suppress("UNCHECKED_CAST")
    return LLMApiServiceImpl(context) as T
  }
}
