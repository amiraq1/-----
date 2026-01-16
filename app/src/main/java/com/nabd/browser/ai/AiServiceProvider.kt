package com.nabd.browser.ai

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * مزود خدمة AI
 * يدير الاتصال بـ Claude API
 * 
 * ⚠️ مهم جداً:
 * لا تضع API Key مباشرة في الكود!
 * استخدم أحد الخيارات التالية:
 * 1. Backend Proxy: الأفضل - أرسل الطلبات عبر خادمك الخاص
 * 2. Firebase Remote Config: لتخزين المفتاح بأمان
 * 3. BuildConfig: للتطوير فقط (أضفه في local.properties)
 */
object AiServiceProvider {
    
    private const val BASE_URL = "https://api.anthropic.com/"
    private const val TIMEOUT_SECONDS = 60L
    
    /**
     * ⚠️ PLACEHOLDER - لا تستخدمه مباشرة
     * استبدله بمفتاحك من خلال:
     * - Backend proxy (موصى به للإنتاج)
     * - Remote Config
     * - BuildConfig للتطوير
     */
    const val AI_API_KEY = "AI_API_KEY_PLACEHOLDER"
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    /**
     * خدمة Claude API
     */
    val claudeService: ClaudeApiService = retrofit.create(ClaudeApiService::class.java)
}

/**
 * Repository للتعامل مع خدمة AI
 */
class AiRepository {
    
    private val apiService = AiServiceProvider.claudeService
    
    /**
     * تلخيص نص الصفحة
     */
    suspend fun summarizePage(
        pageContent: String,
        apiKey: String = AiServiceProvider.AI_API_KEY
    ): Result<String> {
        return sendRequest(
            prompt = """قم بتلخيص المحتوى التالي بشكل مختصر وواضح باللغة العربية. 
                       |ركز على النقاط الرئيسية والأفكار المهمة.
                       |
                       |المحتوى:
                       |$pageContent""".trimMargin(),
            apiKey = apiKey
        )
    }
    
    /**
     * شرح النص المحدد
     */
    suspend fun explainSelection(
        selectedText: String,
        pageContext: String? = null,
        apiKey: String = AiServiceProvider.AI_API_KEY
    ): Result<String> {
        val contextInfo = if (pageContext != null) {
            "\n\nسياق الصفحة (للمرجعية):\n$pageContext"
        } else ""
        
        return sendRequest(
            prompt = """اشرح النص التالي بشكل مبسط وواضح باللغة العربية.
                       |إذا كان يحتوي على مصطلحات تقنية، اشرحها.
                       |
                       |النص المحدد:
                       |$selectedText
                       |$contextInfo""".trimMargin(),
            apiKey = apiKey
        )
    }
    
    /**
     * سؤال وجواب عن الصفحة
     */
    suspend fun askAboutPage(
        question: String,
        pageContent: String,
        previousMessages: List<ChatMessage> = emptyList(),
        apiKey: String = AiServiceProvider.AI_API_KEY
    ): Result<String> {
        // بناء سياق المحادثة
        val conversationContext = if (previousMessages.isNotEmpty()) {
            val history = previousMessages.takeLast(6).joinToString("\n") { msg ->
                if (msg.isUser) "سؤال: ${msg.content}" else "إجابة: ${msg.content}"
            }
            "\n\nسياق المحادثة السابقة:\n$history"
        } else ""
        
        return sendRequest(
            prompt = """أنت مساعد ذكي للإجابة على الأسئلة المتعلقة بمحتوى صفحة الويب.
                       |أجب بناءً على المحتوى المعطى فقط. إذا لم تجد الإجابة في المحتوى، قل ذلك.
                       |
                       |محتوى الصفحة:
                       |${pageContent.take(10000)}
                       |$conversationContext
                       |
                       |السؤال: $question""".trimMargin(),
            apiKey = apiKey
        )
    }
    
    /**
     * إرسال طلب عام
     */
    private suspend fun sendRequest(
        prompt: String,
        apiKey: String
    ): Result<String> {
        return try {
            val request = ClaudeRequest(
                messages = listOf(
                    ClaudeMessage(role = "user", content = prompt)
                )
            )
            
            val response = apiService.sendMessage(apiKey, request)
            
            if (response.isSuccessful) {
                val body = response.body()
                val text = body?.content?.firstOrNull()?.text
                
                if (text != null) {
                    Result.success(text)
                } else if (body?.error != null) {
                    Result.failure(Exception(body.error.message ?: "خطأ غير معروف"))
                } else {
                    Result.failure(Exception("لم يتم استلام رد"))
                }
            } else {
                val errorMessage = when (response.code()) {
                    401 -> "مفتاح API غير صالح"
                    429 -> "تم تجاوز حد الطلبات، حاول لاحقاً"
                    500 -> "خطأ في الخادم"
                    else -> "خطأ: ${response.code()}"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
