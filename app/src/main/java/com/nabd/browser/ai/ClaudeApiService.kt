package com.nabd.browser.ai

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * واجهة Retrofit للتواصل مع Claude API
 */
interface ClaudeApiService {
    
    /**
     * إرسال رسالة إلى Claude
     */
    @POST("v1/messages")
    @Headers(
        "Content-Type: application/json",
        "anthropic-version: 2023-06-01"
    )
    suspend fun sendMessage(
        @Header("x-api-key") apiKey: String,
        @Body request: ClaudeRequest
    ): Response<ClaudeResponse>
}
