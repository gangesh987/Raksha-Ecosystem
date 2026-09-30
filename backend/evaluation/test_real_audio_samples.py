"""
RakshaCall Real Multilingual ASR Audio Evaluation Suite (Phase 1).
Measures:
- Language detection accuracy
- Word Error Rate (WER)
- Character Error Rate (CER)
across English, Tamil, Hindi, Tanglish, Hinglish, Telugu, Kannada, Malayalam, Bengali, etc.
Includes:
- Standard scam utterances
- Ambient noise and silence controls
- Code-switched vernacular speech
"""

import os
import sys
import io
import time
import json
import wave
import numpy as np
from typing import Tuple, List, Dict, Optional, Any

# Ensure backend root is on sys.path
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))
sys.stdout.reconfigure(encoding="utf-8")

from app.ai.multilingual_asr import asr_engine, AudioNormalizer, ASRStatus


def compute_wer(reference: str, hypothesis: str) -> float:
    """Compute Word Error Rate (WER) via Levenshtein distance on words."""
    ref_words = reference.lower().split()
    hyp_words = hypothesis.lower().split()

    if not ref_words:
        return 0.0 if not hyp_words else 1.0

    d = np.zeros((len(ref_words) + 1, len(hyp_words) + 1), dtype=int)
    for i in range(len(ref_words) + 1):
        d[i, 0] = i
    for j in range(len(hyp_words) + 1):
        d[0, j] = j

    for i in range(1, len(ref_words) + 1):
        for j in range(1, len(hyp_words) + 1):
            if ref_words[i - 1] == hyp_words[j - 1]:
                d[i, j] = d[i - 1, j - 1]
            else:
                d[i, j] = min(
                    d[i - 1, j] + 1,      # deletion
                    d[i, j - 1] + 1,      # insertion
                    d[i - 1, j - 1] + 1   # substitution
                )

    return float(d[len(ref_words), len(hyp_words)] / len(ref_words))


def compute_cer(reference: str, hypothesis: str) -> float:
    """Compute Character Error Rate (CER) via Levenshtein distance on characters."""
    ref_chars = list(reference.lower().replace(" ", ""))
    hyp_chars = list(hypothesis.lower().replace(" ", ""))

    if not ref_chars:
        return 0.0 if not hyp_chars else 1.0

    d = np.zeros((len(ref_chars) + 1, len(hyp_chars) + 1), dtype=int)
    for i in range(len(ref_chars) + 1):
        d[i, 0] = i
    for j in range(len(hyp_chars) + 1):
        d[0, j] = j

    for i in range(1, len(ref_chars) + 1):
        for j in range(1, len(hyp_chars) + 1):
            if ref_chars[i - 1] == hyp_chars[j - 1]:
                d[i, j] = d[i - 1, j - 1]
            else:
                d[i, j] = min(
                    d[i - 1, j] + 1,
                    d[i, j - 1] + 1,
                    d[i - 1, j - 1] + 1
                )

    return float(d[len(ref_chars), len(hyp_chars)] / len(ref_chars))


def synthesize_audio_pcm(text: str, lang: str = "en") -> Tuple[bytes, float]:
    """Synthesize speech into 16kHz PCM16 bytes using gTTS and PyAV in memory."""
    import gtts
    import av

    # Map vernacular hints to gtts supported language code
    tts_lang = "ta" if "ta" in lang else "hi" if "hi" in lang else "te" if "te" in lang else "bn" if "bn" in lang else "en"
    tts = gtts.gTTS(text, lang=tts_lang)
    buf = io.BytesIO()
    tts.write_to_fp(buf)
    buf.seek(0)

    container = av.open(buf)
    frames = [f.to_ndarray().squeeze() for f in container.decode(audio=0)]
    sr = container.streams.audio[0].rate
    audio = np.concatenate(frames).astype(np.float32)
    audio16k = AudioNormalizer.resample_if_needed(audio, sr)
    pcm16k = (np.clip(audio16k, -1.0, 1.0) * 32767).astype(np.int16).tobytes()
    duration = len(audio16k) / 16000.0
    return pcm16k, duration


def run_audio_eval():
    print("=" * 80)
    print("RAKSHACALL REAL MULTILINGUAL ASR ACCURACY EVALUATION (PHASE 1)")
    print("=" * 80)

    test_cases = [
        {
            "id": "TC-EN-01",
            "language": "English",
            "lang_code": "en",
            "expected": "Your bank account has been blocked.",
            "type": "Standard Scam Threat"
        },
        {
            "id": "TC-TA-01",
            "language": "Tamil",
            "lang_code": "ta",
            "expected": "உங்கள் வங்கி கணக்கு முடக்கப்பட்டுள்ளது.",
            "type": "Vernacular Script Threat"
        },
        {
            "id": "TC-HI-01",
            "language": "Hindi",
            "lang_code": "hi",
            "expected": "आपका बैंक खाता बंद कर दिया गया है।",
            "type": "Vernacular Script Threat"
        },
        {
            "id": "TC-TANGLISH-01",
            "language": "Tanglish",
            "lang_code": "en",  # Latin script English-Tamil mix
            "expected": "Ungaloda bank account block aayiduchu.",
            "type": "Code-Switched Vernacular"
        },
        {
            "id": "TC-HINGLISH-01",
            "language": "Hinglish",
            "lang_code": "en",  # Latin script English-Hindi mix
            "expected": "Aapka bank account block ho gaya hai.",
            "type": "Code-Switched Vernacular"
        },
        {
            "id": "TC-TE-01",
            "language": "Telugu",
            "lang_code": "te",
            "expected": "మీ బ్యాంక్ ఖాతా నిలిపివేయబడింది.",
            "type": "Vernacular Script Threat"
        },
        {
            "id": "TC-BN-01",
            "language": "Bengali",
            "lang_code": "bn",
            "expected": "আপনার ব্যাংক একাউন্ট ব্লক করা হয়েছে।",
            "type": "Vernacular Script Threat"
        },
        {
            "id": "TC-SILENCE-01",
            "language": "Silence",
            "lang_code": "none",
            "expected": "",
            "type": "Negative Control"
        },
        {
            "id": "TC-NOISE-01",
            "language": "Ambient Noise",
            "lang_code": "none",
            "expected": "",
            "type": "Negative Control"
        },
    ]

    results = []

    print(f"{'ID':<15} {'Language':<12} {'Dur(s)':<8} {'Detected':<10} {'WER':<8} {'CER':<8} {'Status':<10}")
    print("-" * 80)

    for tc in test_cases:
        # Handle silence and noise controls
        if tc["id"] == "TC-SILENCE-01":
            pcm = np.zeros(32000, dtype=np.int16).tobytes()
            duration = 2.0
        elif tc["id"] == "TC-NOISE-01":
            np.random.seed(42)
            noise = (np.random.normal(0, 0.015, 32000) * 32767).astype(np.int16)
            pcm = noise.tobytes()
            duration = 2.0
        else:
            pcm, duration = synthesize_audio_pcm(tc["expected"], tc["lang_code"])

        t0 = time.perf_counter()
        asr_result = asr_engine.transcribe_audio(pcm)
        elapsed_ms = (time.perf_counter() - t0) * 1000.0

        actual = asr_result.canonical_text

        # Compute metrics
        if tc["expected"]:
            wer = round(compute_wer(tc["expected"], actual), 3)
            cer = round(compute_cer(tc["expected"], actual), 3)
        else:
            # For negative controls, non-empty speech is an error
            wer = 0.0 if not actual else 1.0
            cer = 0.0 if not actual else 1.0

        res_entry = {
            "id": tc["id"],
            "language": tc["language"],
            "expected": tc["expected"],
            "actual": actual,
            "normalized": asr_result.normalized_text,
            "detected_language": asr_result.detected_language,
            "language_confidence": round(asr_result.language_confidence, 3),
            "token_confidence": round(asr_result.token_confidence, 3),
            "code_switch_detected": asr_result.code_switch_detected,
            "status": asr_result.status,
            "duration_sec": round(duration, 2),
            "latency_ms": round(elapsed_ms, 2),
            "wer": wer,
            "cer": cer,
        }
        results.append(res_entry)

        print(
            f"{tc['id']:<15} {tc['language']:<12} {duration:<8.2f} "
            f"{asr_result.detected_language:<10} {wer:<8.3f} {cer:<8.3f} {asr_result.status:<10}"
        )

    print("=" * 80)

    # Summary Statistics
    speech_cases = [r for r in results if r["expected"]]
    avg_wer = np.mean([r["wer"] for r in speech_cases])
    avg_cer = np.mean([r["cer"] for r in speech_cases])
    avg_lat = np.mean([r["latency_ms"] for r in results])

    print(f"\nEVALUATION SUMMARY:")
    print(f"Total Test Cases:       {len(results)}")
    print(f"Speech Utterances:      {len(speech_cases)}")
    print(f"Negative Controls:      2 (Silence, Ambient Noise)")
    print(f"Average WER:            {avg_wer:.3f}")
    print(f"Average CER:            {avg_cer:.3f}")
    print(f"Average Latency:        {avg_lat:.2f} ms")

    out_file = os.path.join(os.path.dirname(__file__), "real_audio_eval_results.json")
    with open(out_file, "w", encoding="utf-8") as f:
        json.dump({"summary": {"avg_wer": avg_wer, "avg_cer": avg_cer, "avg_lat_ms": avg_lat}, "cases": results}, f, indent=2, ensure_ascii=False)
    print(f"Detailed evaluation metrics saved to {out_file}\n")


if __name__ == "__main__":
    run_audio_eval()
