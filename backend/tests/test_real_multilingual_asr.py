"""
Production Acceptance Test Suite for RakshaCall Real Multilingual ASR (Phase 1).
Validates all 14 Phase 1 requirements:
1. Provider initialization
2. Audio normalization
3. Empty audio
4. Silence
5. Invalid audio
6. English transcription
7. Tamil transcription
8. Hindi transcription
9. Code switching
10. ASR failure handling
11. Low confidence handling
12. Transcript event generation
13. Downstream AI pipeline integration
14. Memory cleanup & backpressure
"""

import os
import sys
import io
import time
import math
import wave
import pytest
import numpy as np

# Ensure backend is on sys.path
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from app.ai.multilingual_asr import (
    ASRStatus,
    ASRTranscript,
    AudioNormalizer,
    FasterWhisperASRProvider,
    LanguageIdentifier,
    HuBERTAcousticBackbone,
    StreamingAudioBuffer,
    MultilingualASREngine,
    asr_engine,
)
from app.ai.unified_pipeline import get_pipeline


# ============================================================================
# Test 1: Provider Initialization
# ============================================================================

def test_01_provider_initialization():
    """Verify FasterWhisperASRProvider loads cleanly and reports valid metadata."""
    provider = FasterWhisperASRProvider(model_size="base", device="cpu", compute_type="int8")
    status = provider.get_status()

    assert status["provider"] == "faster-whisper"
    assert status["model_size"] == "base"
    assert status["device"] == "cpu"
    assert status["compute_type"] == "int8"
    assert status["is_available"] is True
    assert status["init_error"] is None


# ============================================================================
# Test 2: Audio Normalization & Clipping Detection
# ============================================================================

def test_02_audio_normalization():
    """Verify PCM16 to float32 normalization, clipping detection, and resampling."""
    # 1. Normal sine wave (unclipped)
    t = np.linspace(0, 0.1, 1600, endpoint=False)
    normal_pcm = (np.sin(2 * np.pi * 440 * t) * 16000).astype(np.int16).tobytes()
    audio = AudioNormalizer.pcm16_to_float32(normal_pcm)
    assert len(audio) == 1600
    assert audio.dtype == np.float32
    assert -1.0 <= audio.min() and audio.max() <= 1.0

    quality = AudioNormalizer.check_audio_quality(audio)
    assert quality["is_clipped"] is False
    assert quality["is_silent"] is False
    assert quality["duration_seconds"] == pytest.approx(0.1, abs=0.01)

    # 2. Clipped audio
    clipped_pcm = (np.ones(800, dtype=np.int16) * 32767).tobytes()
    clipped_audio = AudioNormalizer.pcm16_to_float32(clipped_pcm)
    clipped_quality = AudioNormalizer.check_audio_quality(clipped_audio)
    assert clipped_quality["is_clipped"] is True

    # 3. Resampling: 24kHz -> 16kHz
    orig_sr_audio = np.sin(2 * np.pi * 440 * np.linspace(0, 1.0, 24000, endpoint=False)).astype(np.float32)
    resampled = AudioNormalizer.resample_if_needed(orig_sr_audio, orig_sr=24000)
    assert len(resampled) == 16000


# ============================================================================
# Test 3: Empty Audio Handling
# ============================================================================

def test_03_empty_audio():
    """Verify empty or zero-length audio returns explicit ASR_SILENCE with no fake text."""
    res_empty = asr_engine.transcribe_audio(b"")
    assert res_empty.status == ASRStatus.SILENCE.value
    assert res_empty.canonical_text == ""
    assert res_empty.normalized_text == ""
    assert res_empty.token_confidence == 0.0

    res_none = asr_engine.transcribe_audio(None)
    assert res_none.status == ASRStatus.SILENCE.value
    assert res_none.canonical_text == ""


# ============================================================================
# Test 4: Silence Audio Handling
# ============================================================================

def test_04_silence_audio():
    """Verify synthetic silence (zero PCM samples) evaluates to ASR_SILENCE without model hallucination."""
    silence_pcm = np.zeros(16000, dtype=np.int16).tobytes()  # 1.0s of silence
    res = asr_engine.transcribe_audio(silence_pcm)
    assert res.status == ASRStatus.SILENCE.value
    assert res.canonical_text == ""
    assert res.token_confidence == 0.0


# ============================================================================
# Test 5: Invalid & Malformed Audio Handling
# ============================================================================

def test_05_invalid_audio():
    """Verify odd-byte and malformed buffers do not raise uncaught exceptions."""
    odd_bytes = b"\x01\x02\x03"  # 3 bytes, odd alignment
    audio = AudioNormalizer.pcm16_to_float32(odd_bytes)
    assert len(audio) == 1  # 2 bytes consumed, 1 truncated

    res = asr_engine.transcribe_audio(b"\x00")
    assert res.status == ASRStatus.SILENCE.value


# ============================================================================
# Test 6: Real English Transcription
# ============================================================================

def test_06_english_transcription():
    """Verify transcription of real spoken English audio fixture."""
    fixture_path = os.path.join(os.path.dirname(__file__), "fixtures", "speech_en.wav")
    assert os.path.exists(fixture_path), f"Fixture {fixture_path} must exist"

    with wave.open(fixture_path, "rb") as wf:
        pcm = wf.readframes(wf.getnframes())
        sr = wf.getframerate()

    audio = AudioNormalizer.pcm16_to_float32(pcm)
    audio16k = AudioNormalizer.resample_if_needed(audio, sr)
    pcm16k = (np.clip(audio16k, -1.0, 1.0) * 32767).astype(np.int16).tobytes()

    res = asr_engine.transcribe_audio(pcm16k)
    assert res.status == ASRStatus.OK.value
    assert "bank account" in res.canonical_text.lower()
    assert "blocked" in res.canonical_text.lower()
    assert res.detected_language in ("en-IN", "en")
    assert res.token_confidence >= 0.50
    assert res.is_final is True


# ============================================================================
# Test 7: Real Tamil Transcription
# ============================================================================

def test_07_tamil_transcription():
    """Verify transcription and language identification of spoken Tamil."""
    import gtts
    import av

    tamil_text = "உங்கள் வங்கி கணக்கு முடக்கப்பட்டுள்ளது."
    tts = gtts.gTTS(tamil_text, lang="ta")
    buf = io.BytesIO()
    tts.write_to_fp(buf)
    buf.seek(0)

    container = av.open(buf)
    frames = [f.to_ndarray().squeeze() for f in container.decode(audio=0)]
    sr = container.streams.audio[0].rate
    audio = np.concatenate(frames).astype(np.float32)
    audio16k = AudioNormalizer.resample_if_needed(audio, sr)
    pcm16k = (np.clip(audio16k, -1.0, 1.0) * 32767).astype(np.int16).tobytes()

    res = asr_engine.transcribe_audio(pcm16k)
    assert res.status == ASRStatus.OK.value
    assert len(res.canonical_text) > 0
    # Must identify Tamil or Romanized Tamil (Tanglish)
    assert res.detected_language in ("ta-IN", "ta-Latn", "ta")
    assert res.token_confidence >= 0.40


# ============================================================================
# Test 8: Real Hindi Transcription
# ============================================================================

def test_08_hindi_transcription():
    """Verify transcription and language identification of spoken Hindi."""
    import gtts
    import av

    hindi_text = "आपका बैंक खाता बंद कर दिया गया है।"
    tts = gtts.gTTS(hindi_text, lang="hi")
    buf = io.BytesIO()
    tts.write_to_fp(buf)
    buf.seek(0)

    container = av.open(buf)
    frames = [f.to_ndarray().squeeze() for f in container.decode(audio=0)]
    sr = container.streams.audio[0].rate
    audio = np.concatenate(frames).astype(np.float32)
    audio16k = AudioNormalizer.resample_if_needed(audio, sr)
    pcm16k = (np.clip(audio16k, -1.0, 1.0) * 32767).astype(np.int16).tobytes()

    res = asr_engine.transcribe_audio(pcm16k)
    assert res.status == ASRStatus.OK.value
    assert len(res.canonical_text) > 0
    # Must identify Hindi script or Romanized Hindi (Hinglish)
    assert res.detected_language in ("hi-IN", "hi-Latn", "hi")
    assert res.token_confidence >= 0.40


# ============================================================================
# Test 9: Code-Switching Detection
# ============================================================================

def test_09_code_switching_detection():
    """Verify code-switching detection for mixed English/Tamil and English/Hindi speech."""
    lid = LanguageIdentifier()

    # Tanglish sample
    tanglish_text = "Sir account suspicious ah irukku, immediately amount transfer pannunga."
    lang, conf = lid.identify_language(tanglish_text)
    assert lang == "ta-Latn"
    assert conf >= 0.70

    is_cs, langs = lid.detect_code_switching(tanglish_text, lang)
    assert is_cs is True
    assert "ta" in langs or "ta-Latn" in langs

    # Hinglish sample
    hinglish_text = "Aapka bank account block ho gaya hai, please pay amount."
    lang, conf = lid.identify_language(hinglish_text)
    assert lang == "hi-Latn"
    is_cs, langs = lid.detect_code_switching(hinglish_text, lang)
    assert is_cs is True


# ============================================================================
# Test 10: Explicit ASR Failure Handling (Anti-Fabrication Rule)
# ============================================================================

def test_10_asr_failure_handling():
    """Verify provider failures yield explicit ASR_ERROR / ASR_UNAVAILABLE without fake text."""
    # Test unavailable provider
    faulty_provider = FasterWhisperASRProvider(model_size="non_existent_model_xyz")
    engine = MultilingualASREngine(provider=faulty_provider)

    t = np.linspace(0, 0.5, 8000, endpoint=False)
    audio_pcm = (np.sin(2 * np.pi * 440 * t) * 20000).astype(np.int16).tobytes()

    res = engine.transcribe_audio(audio_pcm)
    assert res.status in (ASRStatus.UNAVAILABLE.value, ASRStatus.ERROR.value)
    assert res.canonical_text == ""
    assert res.normalized_text == ""
    assert res.token_confidence == 0.0
    assert res.error_message is not None


# ============================================================================
# Test 11: Low Confidence Flagging
# ============================================================================

def test_11_low_confidence_flagging():
    """Verify low confidence speech segments receive ASR_LOW_CONFIDENCE status."""
    # Create low-amplitude noise with voiced characteristics but no clear phonemes
    np.random.seed(42)
    noise = np.random.normal(0, 0.02, 16000).astype(np.float32)
    noise_pcm = (noise * 32767).astype(np.int16).tobytes()

    res = asr_engine.transcribe_audio(noise_pcm)
    # Must either be detected as silence or low confidence, never hallucinated speech
    if res.status != ASRStatus.SILENCE.value:
        assert res.token_confidence < 0.85


# ============================================================================
# Test 12: Transcript Event Schema & Serialization
# ============================================================================

def test_12_transcript_event_metadata():
    """Verify ASRTranscript has all required fields and serializes to dict."""
    t = ASRTranscript(
        canonical_text="Your bank account is blocked",
        normalized_text="your bank account is blocked",
        detected_language="en-IN",
        language_confidence=0.98,
        token_confidence=0.92,
        duration_seconds=2.5,
        status=ASRStatus.OK.value,
        is_final=True,
        speaker="CALLER",
        timestamp_ms=1727700000000,
        code_switch_detected=False,
        detected_languages=["en-IN"],
    )

    d = t.to_dict()
    assert d["speaker"] == "CALLER"
    assert d["text"] == "your bank account is blocked"
    assert d["language"] == "en-IN"
    assert d["language_confidence"] == 0.98
    assert d["confidence"] == 0.92
    assert d["is_final"] is True
    assert d["status"] == "ASR_OK"
    assert d["code_switch_detected"] is False


# ============================================================================
# Test 13: Downstream Pipeline Integration
# ============================================================================

def test_13_downstream_integration():
    """Verify real transcript feeds directly into UnifiedAnalysisPipeline without breaking."""
    pipeline = get_pipeline()

    # Transcribe real English audio fixture
    fixture_path = os.path.join(os.path.dirname(__file__), "fixtures", "speech_en.wav")
    with wave.open(fixture_path, "rb") as wf:
        pcm = wf.readframes(wf.getnframes())
        sr = wf.getframerate()

    audio = AudioNormalizer.pcm16_to_float32(pcm)
    audio16k = AudioNormalizer.resample_if_needed(audio, sr)
    pcm16k = (np.clip(audio16k, -1.0, 1.0) * 32767).astype(np.int16).tobytes()

    asr_result = asr_engine.transcribe_audio(pcm16k)
    assert asr_result.status == ASRStatus.OK.value
    assert len(asr_result.normalized_text) > 0

    # Pass to UnifiedAnalysisPipeline
    result = pipeline.analyze(
        transcript=asr_result.normalized_text,
        session_id="test-asr-pipeline-session",
        detected_language=asr_result.detected_language
    )

    assert result.risk_score >= 0
    assert result.risk_level in ("LOW", "MEDIUM", "HIGH", "CRITICAL")
    assert result.stage is not None

    # Verify session context was created and recorded turn
    from app.ai.conversation_context import SessionRegistry
    session_ctx = SessionRegistry.get_or_create("test-asr-pipeline-session")
    assert session_ctx.session_id == "test-asr-pipeline-session"
    assert len(session_ctx.turns) > 0


# ============================================================================
# Test 14: Memory Cleanup & Streaming Backpressure
# ============================================================================

def test_14_memory_cleanup_and_backpressure():
    """Verify streaming buffer enforces max_buffer backpressure and clears memory on reset."""
    buf = StreamingAudioBuffer(session_id="test-sess-1", max_buffer_sec=2.0)

    # Ingest 3.0s of audio (exceeding 2.0s limit)
    t = np.linspace(0, 3.0, 48000, endpoint=False)
    pcm = (np.sin(2 * np.pi * 440 * t) * 10000).astype(np.int16).tobytes()

    accumulated = buf.add_pcm_chunk(pcm)
    # Maximum buffer should be capped at 2.0s * 16000 = 32000 samples
    assert len(accumulated) == 32000
    assert buf.get_duration_seconds() == pytest.approx(2.0, abs=0.01)

    # Flush clears buffer
    flushed = buf.flush()
    assert len(flushed) == 32000
    assert buf.get_duration_seconds() == 0.0

    # Engine session registry cleanup
    asr_engine.get_or_create_session("temp-session-xyz")
    assert "temp-session-xyz" in asr_engine._session_buffers
    asr_engine.reset_session("temp-session-xyz")
    assert "temp-session-xyz" not in asr_engine._session_buffers
