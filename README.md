# Qunit - Label Scanner 🏷️
<br>Iqoo Hackathon

Minimal Android prototype that uses **Gemma 3n** (on-device, multimodal) to read food-label photos and flag restricted ingredients — entirely offline.

> **Status:** Feasibility test, not a production app.

---

## How It Works

1. **Snap** a photo of a food label (camera or gallery).
2. **Gemma 3n** running on-device via MediaPipe's LLM Inference API extracts the ingredients.
3. Extracted text is matched against a hardcoded watchlist:

   | Keyword | Alert |
   |---------|-------|
   | Titanium Dioxide / E171 | Restricted in the EU since 2022; still permitted in India |
   | Sugar | _(placeholder — replace later)_ |
   | Salt | _(placeholder — replace later)_ |

4. Matches are **spoken aloud** via Android TextToSpeech.

---

## Quick Start

### Prerequisites

- **Android Studio** Ladybug (2024.2+)
- **Physical device** with ≥ 8 GB RAM (Pixel 7+, Samsung S23+, or equivalent)
  — emulators do **not** reliably support MediaPipe LLM Inference
- **ADB** on your PATH

### 1 — Clone & Open

```bash
git clone https://github.com/atharvac9/-Label-Scanner-.git
```

Open the folder in Android Studio → let Gradle sync.

### 2 — Download the Model

Grab the Gemma 3n E2B `.task` file from Hugging Face:

| Variant | Link |
|---------|------|
| Official | [google/gemma-3n-E2B-it-litert-lm](https://huggingface.co/google/gemma-3n-E2B-it-litert-lm) |
| INT4 (community) | [realbyte/gemma-3n-E2B-it-int4-mediapipe](https://huggingface.co/realbyte/gemma-3n-E2B-it-int4-mediapipe) |

### 3 — Push Model to Device

```bash
adb shell mkdir -p /data/local/tmp/llm/
adb push <model-file>.task /data/local/tmp/llm/gemma3n.task
```

### 4 — Run

Hit **▶ Run** in Android Studio. Wait for _"✅ Model loaded"_ (~10-30 s), then tap **📷 Take Photo**.

---

## Project Structure

```
app/src/main/java/com/labelscanner/app/
├── MainActivity.kt         # Entry point — orchestrates the flow
├── LabelScannerScreen.kt   # Single Compose screen (camera, gallery, results)
├── InferenceEngine.kt      # MediaPipe LLM wrapper (vision + text → ingredients)
├── IngredientChecker.kt    # Hardcoded watchlist + TTS alerts
└── ui/theme/Theme.kt       # Material 3 dynamic-color theme
```

## Tech Stack

| Layer | Choice |
|-------|--------|
| Language | Kotlin |
| UI | Jetpack Compose · Material 3 |
| On-device LLM | MediaPipe `tasks-genai:0.10.27` |
| Model | Gemma 3n E2B — multimodal `.task` |
| Speech | Android `TextToSpeech` |

## Notes

- MediaPipe LLM Inference is in **maintenance mode**; for production consider [LiteRT-LM](https://ai.google.dev/edge/litert-lm).
- Camera uses `TakePicturePreview` (thumbnail). Swap to `TakePicture` for full-res in production.
- **Zero network calls** after the one-time model download.

## License

Educational and research use.
