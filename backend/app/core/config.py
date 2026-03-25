from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    PROJECT_NAME: str = "Formula Knowledge"
    # Per lo sviluppo iniziale usiamo SQLite locale, poi lo cambieremo con PostgreSQL
    DATABASE_URL: str = "sqlite:///./formula_knowledge.db"

    class Config:
        env_file = ".env"

settings = Settings()