package com.labelscanner.app

import android.graphics.Bitmap
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import com.labelscanner.app.ui.theme.LabelScannerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "LabelScanner"
    }

    // State exposed to Compose
    val uiStatus = mutableStateOf("Initializing…")
    val capturedBitmap = mutableStateOf<Bitmap?>(null)
    val modelOutput = mutableStateOf("")
    val flaggedResults = mutableStateOf<List<Pair<String, String>>>(emptyList())
    val isProcessing = mutableStateOf(false)

    private var inferenceEngine: InferenceEngine? = null
    private var ingredientChecker: IngredientChecker? = null

    // Camera: capture a thumbnail bitmap
    val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            capturedBitmap.value = bitmap
            analyzeImage(bitmap)
        } else {
            uiStatus.value = "Camera cancelled"
        }
    }

    // Gallery: pick an image
    val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    capturedBitmap.value = bitmap
                    analyzeImage(bitmap)
                } else {
                    uiStatus.value = "Failed to decode image"
                }
            } catch (e: Exception) {
                uiStatus.value = "Error loading image: ${e.message}"
                Log.e(TAG, "Error loading image", e)
            }
        } else {
            uiStatus.value = "Gallery selection cancelled"
        }
    }

    // Camera permission
    val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            takePictureLauncher.launch(null)
        } else {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize TTS and IngredientChecker
        ingredientChecker = IngredientChecker(this)

        // Initialize the model in background
        uiStatus.value = "Loading model… (this may take 10-30 seconds)"
        lifecycleScope.launch {
            try {
                val engine = withContext(Dispatchers.IO) {
                    InferenceEngine(this@MainActivity)
                }
                inferenceEngine = engine
                uiStatus.value = "✅ Model loaded — ready to scan!"
                Log.i(TAG, "Model loaded successfully")
            } catch (e: Exception) {
                uiStatus.value = "❌ Model failed to load: ${e.message}"
                Log.e(TAG, "Model loading failed", e)
            }
        }

        setContent {
            LabelScannerTheme {
                LabelScannerScreen(activity = this)
            }
        }
    }

    private fun analyzeImage(bitmap: Bitmap) {
        val engine = inferenceEngine
        if (engine == null) {
            uiStatus.value = "❌ Model not loaded yet — please wait"
            return
        }

        isProcessing.value = true
        uiStatus.value = "🔍 Analyzing food label…"
        modelOutput.value = ""
        flaggedResults.value = emptyList()

        lifecycleScope.launch {
            try {
                val rawOutput = withContext(Dispatchers.IO) {
                    engine.analyzeLabel(bitmap)
                }

                modelOutput.value = rawOutput
                Log.i(TAG, "Model output:\n$rawOutput")

                // Check for flagged ingredients
                val checker = ingredientChecker
                if (checker != null) {
                    val matches = checker.checkIngredients(rawOutput)
                    flaggedResults.value = matches

                    if (matches.isNotEmpty()) {
                        uiStatus.value = "⚠️ Found ${matches.size} flagged ingredient(s)!"
                        // Speak alerts
                        checker.speakAlerts(matches)
                    } else {
                        uiStatus.value = "✅ No flagged ingredients detected"
                    }
                } else {
                    uiStatus.value = "Analysis complete"
                }
            } catch (e: Exception) {
                uiStatus.value = "❌ Analysis failed: ${e.message}"
                modelOutput.value = "Error: ${e.message}"
                Log.e(TAG, "Analysis failed", e)
            } finally {
                isProcessing.value = false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ingredientChecker?.shutdown()
        inferenceEngine?.close()
    }
}
