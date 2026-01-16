package com.nabd.browser.ai

import com.google.gson.annotations.SerializedName

/**
 * نموذج طلب Claude API
 */
data class ClaudeRequest(
    @SerializedName("model")
    val model: String = "claude-sonnet-4-20250514",
    
    @SerializedName("max_tokens")
    val maxTokens: Int = 1024,
    
    @SerializedName("messages")
    val messages: List<ClaudeMessage>
)

/**
 * نموذج رسالة Claude
 */
data class ClaudeMessage(
    @SerializedName("role")
    val role: String, // "user" or "assistant"
    
    @SerializedName("content")
    val content: String
)

/**
 * نموذج استجابة Claude API
 */
data class ClaudeResponse(
    @SerializedName("id")
    val id: String?,
    
    @SerializedName("type")
    val type: String?,
    
    @SerializedName("role")
    val role: String?,
    
    @SerializedName("content")
    val content: List<ClaudeContent>?,
    
    @SerializedName("model")
    val model: String?,
    
    @SerializedName("stop_reason")
    val stopReason: String?,
    
    @SerializedName("usage")
    val usage: ClaudeUsage?,
    
    @SerializedName("error")
    val error: ClaudeError?
)

/**
 * محتوى الاستجابة
 */
data class ClaudeContent(
    @SerializedName("type")
    val type: String?, // "text"
    
    @SerializedName("text")
    val text: String?
)

/**
 * معلومات الاستخدام
 */
data class ClaudeUsage(
    @SerializedName("input_tokens")
    val inputTokens: Int?,
    
    @SerializedName("output_tokens")
    val outputTokens: Int?
)

/**
 * نموذج خطأ Claude
 */
data class ClaudeError(
    @SerializedName("type")
    val type: String?,
    
    @SerializedName("message")
    val message: String?
)

/**
 * أنواع طلبات AI المتاحة
 */
enum class AiRequestType {
    SUMMARIZE,          // تلخيص الصفحة
    EXPLAIN_SELECTION,  // شرح النص المحدد
    ASK_ABOUT_PAGE      // سؤال عن الصفحة
}

/**
 * نموذج رسالة الدردشة داخل التطبيق
 */
data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
