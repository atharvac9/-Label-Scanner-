package com.labelscanner.app

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * Checks model output against a hardcoded list of flagged ingredients
 * and speaks alert notes aloud using Android's TextToSpeech API.
 */
class IngredientChecker(context: Context) {

    companion object {
        private const val TAG = "IngredientChecker"

        /**
         * Hardcoded flagged ingredients.
         * Key: list of keyword variants (case-insensitive substring match)
         * Value: the alert note to display and speak
         */
        val FLAGGED_INGREDIENTS: Map<List<String>, String> = mapOf(
            listOf("titanium dioxide", "e171") to
                "Restricted in the EU since 2022 as unsafe; still permitted in India.",
            listOf("sugar") to
                "placeholder note, replace later",
            listOf("salt") to
                "placeholder note, replace later"
        )
    }

    private var tts: TextToSpeech? = null
    private var ttsReady = false

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.US)
                ttsReady = result != TextToSpeech.LANG_MISSING_DATA &&
                           result != TextToSpeech.LANG_NOT_SUPPORTED
                if (ttsReady) {
                    Log.i(TAG, "TTS initialized successfully")
                } else {
                    Log.w(TAG, "TTS language not supported: $result")
                }
            } else {
                Log.e(TAG, "TTS initialization failed: $status")
            }
        }
    }

    /**
     * Check the raw model output for flagged ingredients.
     *
     * @param rawOutput The text returned by the LLM (ingredient list)
     * @return List of (matched ingredient, alert note) pairs
     */
    fun checkIngredients(rawOutput: String): List<Pair<String, String>> {
        val matches = mutableListOf<Pair<String, String>>()
        val lowerOutput = rawOutput.lowercase()

        for ((keywords, note) in FLAGGED_INGREDIENTS) {
            for (keyword in keywords) {
                if (lowerOutput.contains(keyword.lowercase())) {
                    matches.add(Pair(keyword, note))
                    Log.i(TAG, "FLAGGED: '$keyword' → $note")
                    break // Only match once per flagged group
                }
            }
        }

        Log.i(TAG, "Checked ingredients: ${matches.size} match(es) found")
        return matches
    }

    /**
     * Speak flagged ingredient alerts aloud using TTS.
     */
    fun speakAlerts(matches: List<Pair<String, String>>) {
        if (!ttsReady) {
            Log.w(TAG, "TTS not ready, skipping speech")
            return
        }

        for ((ingredient, note) in matches) {
            val utterance = "Warning: $ingredient detected. $note"
            tts?.speak(utterance, TextToSpeech.QUEUE_ADD, null, "alert_$ingredient")
            Log.i(TAG, "Speaking: $utterance")
        }
    }

    /**
     * Release TTS resources.
     */
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        Log.i(TAG, "IngredientChecker shut down")
    }
}
