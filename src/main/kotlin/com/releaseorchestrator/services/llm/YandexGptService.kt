package com.releaseorchestrator.services.llm

import com.releaseorchestrator.models.Commit
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeoutException

class YandexGptService {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = false
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }
    
    companion object {
        private const val YANDEX_GPT_API_URL = "https://llm.api.cloud.yandex.net/foundationModels/v1/completion"
        private val API_KEY = System.getenv("YANDEX_GPT_API_KEY") ?: throw IllegalStateException("YANDEX_GPT_API_KEY environment variable is not set")
        private val FOLDER_ID = System.getenv("YANDEX_GPT_FOLDER_ID") ?: throw IllegalStateException("YANDEX_GPT_FOLDER_ID environment variable is not set")
        private val MODEL_URI get() = "gpt://$FOLDER_ID/yandexgpt-lite"
    }
    
    suspend fun generateReleaseNotes(commits: List<Commit>, version: String): String {
        val prompt = buildReleaseNotesPrompt(commits, version)
        
        val requestBody = buildJsonObject {
            put("modelUri", MODEL_URI)
            put("messages", buildJsonArray {
                addJsonObject {
                    put("role", "user")
                    put("text", prompt)
                }
            })
        }
        
        return try {
            logger.info("Sending request to Yandex GPT for release notes generation")
            logger.info("API URL: $YANDEX_GPT_API_URL")
            logger.info("Folder ID: $FOLDER_ID")
            logger.info("Request body: $requestBody")
            
            val response = client.post(YANDEX_GPT_API_URL) {
                header("Authorization", "Api-Key $API_KEY")
                header("x-folder-id", FOLDER_ID)
                header("Content-Type", "application/json")
                setBody(requestBody)
            }
            
            logger.info("Response status: ${response.status}")
            
            if (response.status.isSuccess()) {
                val responseBody = response.body<JsonObject>()
                logger.info("Response body: $responseBody")
                
                // Детально логируем структуру ответа
                logger.info("Response keys: ${responseBody.keys}")
                val responseResult = responseBody["result"]
                if (responseResult != null) {
                    logger.info("Result type: ${responseResult::class.simpleName}")
                    if (responseResult is JsonObject) {
                        logger.info("Result keys: ${responseResult.keys}")
                        val alternatives = responseResult["alternatives"]
                        if (alternatives != null) {
                            logger.info("Alternatives type: ${alternatives::class.simpleName}")
                            if (alternatives is JsonArray) {
                                logger.info("Alternatives size: ${alternatives.size}")
                                alternatives.forEachIndexed { index, alt ->
                                    logger.info("Alternative $index: $alt")
                                }
                            }
                        }
                    }
                }
                
                // Используем структуру ответа согласно документации Yandex GPT
                val result = responseBody["result"]?.jsonObject
                if (result != null) {
                    val alternatives = result["alternatives"]?.jsonArray
                    if (alternatives != null && alternatives.isNotEmpty()) {
                        val alternative = alternatives[0].jsonObject
                        val message = alternative["message"]?.jsonObject
                        if (message != null) {
                            val generatedNotes = message["text"]?.jsonPrimitive?.content
                            if (generatedNotes != null && generatedNotes.isNotBlank()) {
                                logger.info("Successfully generated release notes from Yandex GPT")
                                logger.info("Generated text: ${generatedNotes.take(200)}...")
                                return generatedNotes.trim()
                            }
                        }
                    }
                }
                
                logger.warn("No valid release notes generated from Yandex GPT, using fallback")
                val fallbackNotes = generateFallbackReleaseNotes(commits, version)
                logger.info("Generated fallback release notes: ${fallbackNotes.take(200)}...")
                return fallbackNotes
                
            } else {
                val errorBody = response.body<String>()
                logger.error("Yandex GPT API error: ${response.status} - $errorBody")
                logger.warn("Using fallback release notes due to API error")
                generateFallbackReleaseNotes(commits, version)
            }
            
        } catch (e: TimeoutException) {
            logger.error("Timeout while waiting for Yandex GPT response")
            generateFallbackReleaseNotes(commits, version)
        } catch (e: Exception) {
            logger.error("Error calling Yandex GPT API", e)
            generateFallbackReleaseNotes(commits, version)
        }
    }
    
    private fun buildReleaseNotesPrompt(commits: List<Commit>, version: String): String {
        val commitsText = if (commits.isEmpty()) {
            "Нет новых коммитов для анализа"
        } else {
            commits.joinToString("\n") { commit ->
                "- ${commit.message} (${commit.author})"
            }
        }
        
        return """
            Создай краткие и понятные release notes для версии $version Android-приложения.
            
            $commitsText
            
            Требования:
            1. Используй русский язык
            2. Сделай текст понятным для пользователей
            3. Если коммитов нет, опиши что это первая версия приложения
            4. Используй markdown формат
            
            Формат ответа:
            ## Что нового в версии $version
            
            ### 📱 Описание
            Краткое описание версии
            
            ### 🔧 Технические детали
            - детали если есть
        """.trimIndent()
    }
    
    private fun generateFallbackReleaseNotes(commits: List<Commit>, version: String): String {
        val commitsText = if (commits.isEmpty()) {
            "Это первая версия приложения"
        } else {
            commits.joinToString("\n") { commit ->
                "- ${commit.message} (${commit.author})"
            }
        }
        
        return """
            ## Что нового в версии $version
            
            ### 📱 Описание
            $commitsText
            
            *Release notes сгенерированы автоматически*
        """.trimIndent()
    }
    
    fun close() {
        client.close()
    }
}
