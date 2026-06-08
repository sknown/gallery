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

package com.google.ai.edge.gallery.llm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.ai.edge.gallery.llm.ChatMessage
import com.google.ai.edge.gallery.llm.GenerationParams
import com.google.ai.edge.gallery.llm.GenerationState
import com.google.ai.edge.gallery.llm.LLMViewModel

/**
 * LLM 聊天 UI 屏幕
 * 
 * 展示多轮对话界面，���持：
 * - 实时输入和消息发送
 * - 流式文本生成显示
 * - 对话历史管理
 * - 加载状态反馈
 */
@Composable
fun LLMChatScreen(
  viewModel: LLMViewModel = hiltViewModel(),
  modifier: Modifier = Modifier
) {
  val generationState by viewModel.generationState.collectAsState()
  val chatMessages by viewModel.chatMessages.collectAsState()
  val streamingText by viewModel.streamingText.collectAsState()
  var inputText by remember { mutableStateOf("") }
  var temperature by remember { mutableStateOf(0.7f) }
  var topK by remember { mutableStateOf(40) }

  Column(
    modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
  ) {
    // 标题栏
    Surface(
      modifier = Modifier.fillMaxWidth(),
      color = MaterialTheme.colorScheme.primary,
      shadowElevation = 4.dp
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          "LLM 聊天助手",
          style = MaterialTheme.typography.headlineSmall,
          color = MaterialTheme.colorScheme.onPrimary
        )
        Text(
          "本地隐私对话 • 100% 在设备上运行",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
        )
      }
    }

    // 参数控制面板
    ParameterPanel(
      temperature = temperature,
      topK = topK,
      onTemperatureChange = { temperature = it },
      onTopKChange = { topK = it }
    )

    // 消息列表
    LazyColumn(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp),
      reverseLayout = false
    ) {
      items(chatMessages) { message ->
        ChatMessageItem(message = message)
      }

      // 显示流式生成的文本
      if (streamingText.isNotEmpty() && generationState is GenerationState.Generating) {
        item {
          ChatMessageItem(
            message = ChatMessage(role = "assistant", content = streamingText)
          )
        }
      }
    }

    // 加载状态
    if (generationState is GenerationState.Generating) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        contentAlignment = Alignment.Center
      ) {
        CircularProgressIndicator(
          modifier = Modifier.align(Alignment.Center),
          color = MaterialTheme.colorScheme.primary
        )
      }
    }

    // 错误显示
    if (generationState is GenerationState.Error) {
      val error = (generationState as GenerationState.Error).exception
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(8.dp)
      ) {
        Text(
          "错误: ${error.message}",
          color = MaterialTheme.colorScheme.onErrorContainer,
          modifier = Modifier.padding(12.dp)
        )
      }
    }

    // 输入框和发送按钮
    InputPanel(
      inputText = inputText,
      onInputChange = { inputText = it },
      onSendClick = {
        if (inputText.isNotBlank()) {
          val params = GenerationParams(
            temperature = temperature,
            topK = topK
          )
          viewModel.sendChatMessageStream(inputText, params)
          inputText = ""
        }
      },
      isLoading = generationState is GenerationState.Generating
    )
  }
}

/**
 * 聊天消息项
 */
@Composable
private fun ChatMessageItem(
  message: ChatMessage,
  modifier: Modifier = Modifier
) {
  val isUser = message.role == "user"
  
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 8.dp),
    horizontalArrangement = if (isUser) androidx.compose.foundation.layout.Arrangement.End else androidx.compose.foundation.layout.Arrangement.Start
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth(if (isUser) 0.85f else 0.85f)
        .padding(horizontal = 8.dp),
      color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
      shape = RoundedCornerShape(12.dp),
      shadowElevation = 2.dp
    ) {
      Text(
        text = message.content,
        modifier = Modifier.padding(12.dp),
        color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium
      )
    }
  }
}

/**
 * 参数控制面板
 */
@Composable
private fun ParameterPanel(
  temperature: Float,
  topK: Int,
  onTemperatureChange: (Float) -> Unit,
  onTopKChange: (Int) -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier
      .fillMaxWidth()
      .padding(16.dp),
    color = MaterialTheme.colorScheme.surfaceVariant,
    shape = RoundedCornerShape(8.dp)
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Text(
        "生成参数",
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.padding(bottom = 8.dp)
      )
      
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
      ) {
        Text(
          "温度: %.2f".format(temperature),
          style = MaterialTheme.typography.bodySmall
        )
        Text(
          "TopK: $topK",
          style = MaterialTheme.typography.bodySmall
        )
      }
    }
  }
}

/**
 * 输入面板
 */
@Composable
private fun InputPanel(
  inputText: String,
  onInputChange: (String) -> Unit,
  onSendClick: () -> Unit,
  isLoading: Boolean = false,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    color = MaterialTheme.colorScheme.surface,
    shadowElevation = 4.dp
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      TextField(
        value = inputText,
        onValueChange = onInputChange,
        modifier = Modifier
          .weight(1f)
          .padding(end = 8.dp),
        placeholder = { Text("输入您的问题...") },
        shape = RoundedCornerShape(24.dp),
        colors = TextFieldDefaults.colors(
          focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
          unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
          focusedIndicatorColor = Color.Transparent,
          unfocusedIndicatorColor = Color.Transparent
        ),
        keyboardOptions = KeyboardOptions(
          capitalization = KeyboardCapitalization.Sentences,
          imeAction = ImeAction.Send
        ),
        keyboardActions = KeyboardActions(
          onSend = { onSendClick() }
        ),
        singleLine = false,
        maxLines = 4,
        enabled = !isLoading
      )

      IconButton(
        onClick = onSendClick,
        enabled = inputText.isNotBlank() && !isLoading
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.Send,
          contentDescription = "发送",
          tint = MaterialTheme.colorScheme.primary
        )
      }
    }
  }
}

/**
 * LLM 文本生成屏幕
 */
@Composable
fun LLMGenerateScreen(
  viewModel: LLMViewModel = hiltViewModel(),
  modifier: Modifier = Modifier
) {
  val generationState by viewModel.generationState.collectAsState()
  val generatedText by viewModel.generatedText.collectAsState()
  val streamingText by viewModel.streamingText.collectAsState()
  var promptText by remember { mutableStateOf("") }
  var maxTokens by remember { mutableStateOf(512) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .padding(16.dp)
  ) {
    Text(
      "文本生成",
      style = MaterialTheme.typography.headlineMedium,
      modifier = Modifier.padding(bottom = 16.dp)
    )

    // 输入框
    TextField(
      value = promptText,
      onValueChange = { promptText = it },
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 16.dp),
      label = { Text("输入提示词") },
      placeholder = { Text("请输入您的提示词...") },
      minLines = 3,
      maxLines = 5
    )

    // 参数设置
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 16.dp),
      horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
    ) {
      Text("最大 Tokens: $maxTokens", modifier = Modifier.align(Alignment.CenterVertically))
    }

    // 生成按钮
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 16.dp),
      horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
    ) {
      Button(
        onClick = {
          viewModel.generateTextStream(
            promptText,
            GenerationParams(maxOutputTokens = maxTokens)
          )
        },
        modifier = Modifier
          .weight(1f),
        enabled = promptText.isNotBlank() && generationState !is GenerationState.Generating
      ) {
        Text("流式生成")
      }

      Button(
        onClick = {
          viewModel.generateText(
            promptText,
            GenerationParams(maxOutputTokens = maxTokens)
          )
        },
        modifier = Modifier
          .weight(1f),
        enabled = promptText.isNotBlank() && generationState !is GenerationState.Generating
      ) {
        Text("直接生成")
      }
    }

    // 结果显示
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
        .padding(bottom = 16.dp),
      color = MaterialTheme.colorScheme.surfaceVariant,
      shape = RoundedCornerShape(8.dp)
    ) {
      when (generationState) {
        is GenerationState.Idle -> {
          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
          ) {
            Text("点击按钮生成文本")
          }
        }
        is GenerationState.Generating -> {
          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              CircularProgressIndicator()
              Text("生成中...", modifier = Modifier.padding(top = 16.dp))
              if (streamingText.isNotEmpty()) {
                Text(
                  streamingText,
                  modifier = Modifier.padding(16.dp),
                  style = MaterialTheme.typography.bodySmall
                )
              }
            }
          }
        }
        is GenerationState.Complete -> {
          val displayText =
            if (streamingText.isNotEmpty()) streamingText else generatedText
          Text(
            displayText,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium
          )
        }
        is GenerationState.Error -> {
          val error = (generationState as GenerationState.Error).exception
          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
          ) {
            Text(
              "生成失败: ${error.message}",
              color = MaterialTheme.colorScheme.error,
              modifier = Modifier.padding(16.dp)
            )
          }
        }
      }
    }
  }
}
