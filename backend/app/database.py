from sqlalchemy import create_engine
from sqlalchemy.orm import declarative_base
from sqlalchemy.orm import sessionmaker

# Qui è dove decidiamo a cosa connetterci.
# Per ora creiamo un file "formula_knowledge.db" direttamente nella cartella del progetto.
SQLALCHEMY_DATABASE_URL = "sqlite:///./formula_knowledge.db"

# Creiamo il "motore" del database. (check_same_thread è una configurazione specifica richiesta da SQLite)
engine = create_engine(
    SQLALCHEMY_DATABASE_URL, connect_args={"check_same_thread": False}
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