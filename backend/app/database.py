from sqlalchemy import create_engine
from sqlalchemy.orm import declarative_base
from sqlalchemy.orm import sessionmaker
from .core.config import settings

# Qui è dove decidiamo a cosa connetterci.
# L'URL viene letto centralmente dal file di configurazione.
SQLALCHEMY_DATABASE_URL = settings.DATABASE_URL

# Creiamo il "motore" del database. (check_same_thread è una configurazione specifica richiesta da SQLite)
engine = create_engine(
    SQLALCHEMY_DATABASE_URL, connect_args={"check_same_thread": False} if "sqlite" in SQLALCHEMY_DATABASE_URL else {}
)

# SessionLocal sarà la nostra "finestra" temporanea sul DB per ogni singola richiesta
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

Base = declarative_base()

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()