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

import kotlinx.coroutines.flow.Flow

/**
 * LLM API 服务接口
 * 
 * 提供本地 LLM 模型推理的核心 API，支持：
 * - 文本生成
 * - 流式响应
 * - 参数配置
 * - 错误处理
 */
interface LLMApiService {

  /**
   * 生成文本响应
   * 
   * @param prompt 输入提示词
   * @param params 生成参数配置
   * @return 生成的文本响应
   */
  suspend fun generateText(
    prompt: String,
    params: GenerationParams = GenerationParams()
  ): TextResponse

  /**
   * 流式生成文本
   * 
   * 实时返回生成过程中的文本片段，适合长文本生成场景
   * 
   * @param prompt 输入提示词
   * @param params 生成参数配置
   * @return 文本片段流
   */
  fun generateTextStream(
    prompt: String,
    params: GenerationParams = GenerationParams()
  ): Flow<TextStreamResponse>

  /**
   * 多轮对话
   * 
   * @param messages 对话消息列表
   * @param params 生成参数配置
   * @return 模型响应
   */
  suspend fun chat(
    messages: List<ChatMessage>,
    params: GenerationParams = GenerationParams()
  ): ChatResponse

  /**
   * 流式多轮对话
   * 
   * @param messages 对话消息列表
   * @param params 生成参数配置
   * @return 响应文本流
   */
  fun chatStream(
    messages: List<ChatMessage>,
    params: GenerationParams = GenerationParams()
  ): Flow<ChatStreamResponse>

  /**
   * 获取当前加载的模型信息
   * 
   * @return 模型信息
   */
  suspend fun getModelInfo(): ModelInfo

  /**
   * 取消当前进行中的生成操作
   */
  suspend fun cancelGeneration()
}

/**
 * 文本生成参数配置
 * 
 * @param temperature 采样温度 (0.0-2.0)，控制输出的随机性
 * @param topK 仅从前 K 个最有可能的下一个词中采样
 * @param topP 核采样参数，仅从概率累积达 topP 的词中采样
 * @param maxOutputTokens 最大输出 token 数
 * @param stopSequences 停止序列列表
 */
data class GenerationParams(
  val temperature: Float = 0.7f,
  val topK: Int = 40,
  val topP: Float = 0.95f,
  val maxOutputTokens: Int = 512,
  val stopSequences: List<String> = emptyList()
) {
  init {
    require(temperature >= 0.0f && temperature <= 2.0f) { "temperature must be between 0.0 and 2.0" }
    require(topK > 0) { "topK must be positive" }
    require(topP in 0.0f..1.0f) { "topP must be between 0.0 and 1.0" }
    require(maxOutputTokens > 0) { "maxOutputTokens must be positive" }
  }
}

/**
 * 文本生成响应
 * 
 * @param text 生成的文本
 * @param stopReason 停止原因
 * @param inputTokens 输入 token 数
 * @param outputTokens 输出 token 数
 */
data class TextResponse(
  val text: String,
  val stopReason: String,
  val inputTokens: Int,
  val outputTokens: Int
)

/**
 * 文本流响应
 * 
 * @param chunk 文本片段
 * @param isComplete 是否完成生成
 * @param chunkIndex 片段索引
 */
data class TextStreamResponse(
  val chunk: String,
  val isComplete: Boolean,
  val chunkIndex: Int
)

/**
 * 对话消息
 * 
 * @param role 消息角色 ("user", "assistant", "system")
 * @param content 消息内容
 */
data class ChatMessage(
  val role: String,
  val content: String
)

/**
 * 对话响应
 * 
 * @param message 模型响应消息
 * @param inputTokens 输入 token 数
 * @param outputTokens 输出 token 数
 * @param stopReason 停止原因
 */
data class ChatResponse(
  val message: ChatMessage,
  val inputTokens: Int,
  val outputTokens: Int,
  val stopReason: String
)

/**
 * 对话流响应
 * 
 * @param delta 增量内容
 * @param isComplete 是否完成
 * @param index 片段索引
 */
data class ChatStreamResponse(
  val delta: String,
  val isComplete: Boolean,
  val index: Int
)

/**
 * 模型信息
 * 
 * @param name 模型名称
 * @param version 模型版本
 * @param contextWindow 上下文窗口大小
 * @param maxTokens 最大 token 数
 * @param supportedLanguages 支持的语言列表
 */
data class ModelInfo(
  val name: String,
  val version: String,
  val contextWindow: Int,
  val maxTokens: Int,
  val supportedLanguages: List<String>
)

/**
 * LLM API 异常
 */
sealed class LLMException(message: String, cause: Throwable? = null) :
  Exception(message, cause) {
  class ModelNotLoadedException(message: String = "Model not loaded") : LLMException(message)
  class GenerationFailedException(message: String, cause: Throwable? = null) :
    LLMException(message, cause)
  class InvalidParametersException(message: String) : LLMException(message)
  class InsufficientMemoryException(message: String = "Insufficient device memory") :
    LLMException(message)
}
