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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * LLM API ViewModel
 * 
 * 管理 UI 层与 LLMApiService 之间的交互，包括：
 * - 生成文本和对话
 * - 管理 UI 状态
 * - 处理用户操作
 */
@HiltViewModel
class LLMViewModel
@Inject
constructor(private val llmService: LLMApiService) : ViewModel() {

  // ==================== 状态管理 ====================

  private val _generationState = MutableStateFlow<GenerationState>(GenerationState.Idle)
  val generationState: StateFlow<GenerationState> = _generationState.asStateFlow()

  private val _generatedText = MutableStateFlow("")
  val generatedText: StateFlow<String> = _generatedText.asStateFlow()

  private val _streamingText = MutableStateFlow("")
  val streamingText: StateFlow<String> = _streamingText.asStateFlow()

  private val _errorEvent = MutableSharedFlow<LLMException>(replay = 0)
  val errorEvent = _errorEvent.asSharedFlow()

  private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
  val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

  // ==================== 文本生成方法 ====================

  /**
   * 生成单次响应文本
   */
  fun generateText(prompt: String, params: GenerationParams = GenerationParams()) {
    viewModelScope.launch {
      try {
        _generationState.value = GenerationState.Generating
        val response = llmService.generateText(prompt, params)
        _generatedText.value = response.text
        _generationState.value = GenerationState.Complete(response)
      } catch (e: LLMException) {
        _generationState.value = GenerationState.Error(e)
        _errorEvent.emit(e)
      }
    }
  }

  /**
   * 流式生成文本
   */
  fun generateTextStream(prompt: String, params: GenerationParams = GenerationParams()) {
    viewModelScope.launch {
      try {
        _generationState.value = GenerationState.Generating
        _streamingText.value = ""

        llmService.generateTextStream(prompt, params).collect { response ->
          if (!response.isComplete) {
            _streamingText.value += response.chunk
          } else {
            _generationState.value = GenerationState.Complete(
              TextResponse(
                text = _streamingText.value,
                stopReason = "stream_complete",
                inputTokens = 0,
                outputTokens = 0
              )
            )
          }
        }
      } catch (e: LLMException) {
        _generationState.value = GenerationState.Error(e)
        _errorEvent.emit(e)
      }
    }
  }

  // ==================== 对话方法 ====================

  /**
   * 发送聊天消息
   */
  fun sendChatMessage(message: String, params: GenerationParams = GenerationParams()) {
    viewModelScope.launch {
      try {
        // 添加用户消息
        val userMessage = ChatMessage(role = "user", content = message)
        val updatedMessages = _chatMessages.value + userMessage
        _chatMessages.value = updatedMessages

        _generationState.value = GenerationState.Generating

        // 获取模型响应
        val response = llmService.chat(updatedMessages, params)
        val assistantMessage = response.message

        // 添加助手消息
        _chatMessages.value = updatedMessages + assistantMessage
        _generationState.value = GenerationState.Complete(response)
      } catch (e: LLMException) {
        _generationState.value = GenerationState.Error(e)
        _errorEvent.emit(e)
      }
    }
  }

  /**
   * 流式发送聊天消息
   */
  fun sendChatMessageStream(message: String, params: GenerationParams = GenerationParams()) {
    viewModelScope.launch {
      try {
        // 添加用户消息
        val userMessage = ChatMessage(role = "user", content = message)
        val updatedMessages = _chatMessages.value + userMessage
        _chatMessages.value = updatedMessages

        _generationState.value = GenerationState.Generating
        _streamingText.value = ""

        // 流式获取响应
        llmService.chatStream(updatedMessages, params).collect { response ->
          if (!response.isComplete) {
            _streamingText.value += response.delta
          } else {
            // 添加完整的助手消息
            val assistantMessage = ChatMessage(
              role = "assistant",
              content = _streamingText.value
            )
            _chatMessages.value = updatedMessages + assistantMessage
            _generationState.value = GenerationState.Complete(
              ChatResponse(
                message = assistantMessage,
                inputTokens = 0,
                outputTokens = 0,
                stopReason = "stream_complete"
              )
            )
          }
        }
      } catch (e: LLMException) {
        _generationState.value = GenerationState.Error(e)
        _errorEvent.emit(e)
      }
    }
  }

  /**
   * 清空对话历史
   */
  fun clearChat() {
    _chatMessages.value = emptyList()
    _streamingText.value = ""
    _generatedText.value = ""
    _generationState.value = GenerationState.Idle
  }

  /**
   * 取消当前生成操作
   */
  fun cancelGeneration() {
    viewModelScope.launch {
      llmService.cancelGeneration()
      _generationState.value = GenerationState.Idle
    }
  }

  /**
   * 获取模型信息
   */
  fun loadModelInfo(callback: (ModelInfo) -> Unit) {
    viewModelScope.launch {
      try {
        val modelInfo = llmService.getModelInfo()
        callback(modelInfo)
      } catch (e: LLMException) {
        _errorEvent.emit(e)
      }
    }
  }
}

/**
 * 生成状态
 */
sealed class GenerationState {
  object Idle : GenerationState()
  object Generating : GenerationState()
  data class Complete(val response: Any) : GenerationState() // TextResponse 或 ChatResponse
  data class Error(val exception: LLMException) : GenerationState()
}
