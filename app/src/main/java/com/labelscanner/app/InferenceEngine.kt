package com.labelscanner.app

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.genai.llminference.GraphOptions
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import java.io.File

/**
 * Wraps the MediaPipe LLM Inference API for on-device multimodal inference.
 *
 * Follows Google's official "LLM Inference guide for Android":
 * https://ai.google.dev/edge/mediapipe/solutions/genai/llm_inference/android
 *
 * The model file must be pushed to the device before use:
 *   adb shell mkdir -p /data/local/tmp/llm/
 *   adb push <model>.task /data/local/tmp/llm/gemma3n.task
 */
class InferenceEngine(private val context: Context) {

    companion object {
        private const val TAG = "InferenceEngine"

        // Default path where model is pushed via ADB
        private const val MODEL_PATH = "/data/local/tmp/llm/gemma3n.task"

        // The prompt for ingredient extraction
        private const val LABEL_PROMPT =
            "List the ingredients visible in this food label photo, as plain text, one per line."
    }

    private val llmInference: LlmInference

    init {
        // Verify model file exists
        val modelFile = File(MODEL_PATH)
        if (!modelFile.exists()) {
            throw IllegalStateException(
                "Model file not found at $MODEL_PATH\n\n" +
                "Please push the Gemma 3n model to your device:\n" +
                "  adb shell mkdir -p /data/local/tmp/llm/\n" +
                "  adb push <model-file>.task /data/local/tmp/llm/gemma3n.task"
            )
        }

        Log.i(TAG, "Loading model from $MODEL_PATH …")

        // Create LlmInference with model options
        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(MODEL_PATH)
            .setMaxTokens(1024)
            .setMaxTopK(64)
            .setMaxNumImages(1)
            .build()

        llmInference = LlmInference.createFromOptions(context, options)
        Log.i(TAG, "Model loaded successfully")
    }

    /**
     * Analyze a food label image and return the extracted ingredient list.
     *
     * Must be called from a background thread — this is a blocking call
     * that may take several seconds on-device.
     */
    fun analyzeLabel(bitmap: Bitmap): String {
        Log.i(TAG, "Starting label analysis (${bitmap.width}x${bitmap.height})…")

        // Convert Bitmap to MPImage
        val mpImage = BitmapImageBuilder(bitmap).build()

        // Create a session with vision modality enabled
        val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
            .setTopK(10)
            .setTemperature(0.3f) // Low temperature for factual extraction
            .setGraphOptions(
                GraphOptions.builder()
                    .setEnableVisionModality(true)
                    .build()
            )
            .build()

        val session = LlmInferenceSession.createFromOptions(llmInference, sessionOptions)

        return try {
            // Add the text prompt and image
            session.addQueryChunk(LABEL_PROMPT)
            session.addImage(mpImage)

            // Generate response (blocking)
            val result = session.generateResponse()
            Log.i(TAG, "Inference complete, result length: ${result.length}")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Inference failed", e)
            throw e
        } finally {
            try {
                session.close()
            } catch (e: Exception) {
                Log.w(TAG, "Error closing session", e)
            }
        }
    }

    /**
     * Release model resources.
     */
    fun close() {
        try {
            llmInference.close()
            Log.i(TAG, "InferenceEngine closed")
        } catch (e: Exception) {
            Log.w(TAG, "Error closing LlmInference", e)
        }
    }
}
