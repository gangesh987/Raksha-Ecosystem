from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    database_url: str = "sqlite:///./rakshacall.db"
    jwt_secret: str = "change-me"
    jwt_algorithm: str = "HS256"
    access_token_minutes: int = 720
    cors_origins: str = "http://localhost:5173"
    model_mode: str = "gemini-live+groq+deterministic"
    gemini_live_model: str = "gemini-3.1-flash-live-preview"
    gemini_api_key: str = ""
    groq_api_key: str = ""
    groq_model: str = "qwen/qwen3.8-27b"
    twilio_account_sid: str = ""
    twilio_auth_token: str = ""
    twilio_from_number: str = ""

    # Real Multilingual ASR Pipeline Configuration
    asr_provider: str = "faster-whisper"
    asr_model: str = "base"
    asr_device: str = "cpu"
    asr_compute_type: str = "int8"
    asr_vad_enabled: bool = True
    asr_chunk_duration_sec: float = 1.0
    asr_max_buffer_sec: float = 10.0
    asr_language_mode: str = "auto"

    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

settings = Settings()
