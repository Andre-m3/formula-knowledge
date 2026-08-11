import os

import firebase_admin
from fastapi import FastAPI
from firebase_admin import credentials

from . import database, models
from .api.endpoints import router as api_router


app = FastAPI(title="Formula Knowledge API")

models.Base.metadata.create_all(bind=database.engine)

# --- FIREBASE SETUP ---
firebase_cred_path = os.path.join(os.path.dirname(__file__), "firebase-adminsdk.json")
try:
    if not firebase_admin._apps:
        cred = credentials.Certificate(firebase_cred_path)
        firebase_admin.initialize_app(cred)
except Exception as e:
    print(f"⚠️ ERRORE FIREBASE: Impossibile caricare {firebase_cred_path}. {e}")


# Mantiene invariati i percorsi pubblici: le route sono registrate senza prefix.
app.include_router(api_router)
