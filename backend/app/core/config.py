from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    PROJECT_NAME: str = "Formula Knowledge"
    # Per lo sviluppo iniziale usiamo SQLite locale, poi lo cambieremo con PostgreSQL
    DATABASE_URL: str = "sqlite:///./formula_knowledge.db"
    
    # Chiave segreta per autorizzare il traffico dall'App Android
    API_SECRET_KEY: str = "super_secret_formula_knowledge_key_2026!"

    class Config:
        env_file = ".env"

settings = Settings()