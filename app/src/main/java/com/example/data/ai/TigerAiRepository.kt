package com.example.data.ai

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

private const val TAG = "TigerAiRepo"

/**
 * Streams real Gemini 3.8 Flash responses via Firebase AI Logic (`Firebase.ai`)
 * supporting text streaming, multimodal photo/document analysis, and Cloud Drive insights.
 */
class TigerAiRepository {

    private val model by lazy {
        Firebase.ai.generativeModel(modelName = "gemini-3.8-flash")
    }

    fun streamAssistantResponse(
        prompt: String,
        isArabic: Boolean,
        attachedBitmap: Bitmap? = null,
        driveContext: String? = null
    ): Flow<String> = flow {
        val langDirective = if (isArabic) {
            "You are TIGER AI inside TIGER CONNECT super-app. Respond clearly and helpfully in Arabic unless the user writes in English."
        } else {
            "You are TIGER AI inside TIGER CONNECT super-app. Respond clearly, concisely, and helpfully in English."
        }
        val fullPrompt = buildString {
            append(langDirective)
            if (!driveContext.isNullOrBlank()) {
                append("\n\nUser Cloud Drive Summary:\n")
                append(driveContext)
            }
            append("\n\nUser request:\n")
            append(prompt)
        }

        val accumulated = StringBuilder()
        try {
            withTimeout(60_000L) {
                val flowChunks = if (attachedBitmap != null) {
                    model.generateContentStream(
                        content {
                            image(attachedBitmap)
                            text(fullPrompt)
                        }
                    )
                } else {
                    model.generateContentStream(fullPrompt)
                }
                flowChunks.collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        emit(accumulated.toString())
                    }
                }
            }
            if (accumulated.isBlank()) {
                throw IllegalStateException("Model returned an empty response.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Firebase AI stream timed out", e)
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI stream failed", e)
            throw e
        }
    }
}
