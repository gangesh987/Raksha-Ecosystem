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

    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

settings = Settings()
