import random

from fastapi import Depends, HTTPException, Security
from fastapi.security import OAuth2PasswordBearer
from fastapi.security.api_key import APIKeyHeader
from sqlalchemy.orm import Session

from firebase_admin import auth as firebase_auth

from .. import database, models
from ..core.config import settings


api_key_header = APIKeyHeader(name="X-API-Key", auto_error=False)


async def get_api_key(api_key_header: str = Security(api_key_header)):
    if api_key_header != settings.API_SECRET_KEY:
        raise HTTPException(status_code=403, detail="Non autorizzato: API Key mancante o non valida")
    return api_key_header


# OAuth2 scheme per estrarre automaticamente il token JWT dall'header "Authorization"
oauth2_scheme = OAuth2PasswordBearer(tokenUrl="token")


def generate_unique_f1_tag(db: Session) -> str:
    adjectives = ["Fast", "Flying", "Smooth", "Braking", "Apex", "Slipstream", "DRS", "ERS", "KERS", "Turbo", "Oversteer", "Understeer", "Chicane", "Podium", "Pitstop", "Quali", "Grid", "Lap", "Downforce", "G-Force", "Amazing", "Legendary", "Slick", "Blazing", "Rapid", "Furious", "Rocket", "Thunder"]
    nouns = ["Racer", "Driver", "Pilot", "Champion", "Rookie", "Legend", "Tifoso", "Max", "Schumacher", "Senna", "Prost", "Hamilton", "Vettel", "Rosberg", "Alonso", "Lauda", "Winner", "Predestinato", "Record", "Formula", "Monza", "Silverstone", "Lotus", "Ferrari", "McLaren"]

    while True:
        adj = random.choice(adjectives)
        noun = random.choice(nouns)
        num = random.randint(1, 99)
        tag = f"{adj}{noun}{num}"

        # Valida che il tag non sia troppo corto
        if len(tag) >= 9:
            if not db.query(models.User).filter(models.User.f1_tag == tag).first():
                return tag


async def get_current_user(token: str = Depends(oauth2_scheme), db: Session = Depends(database.get_db)):
    try:
        decoded_token = firebase_auth.verify_id_token(token)
        firebase_uid = decoded_token.get("uid")
    except Exception as e:
        raise HTTPException(status_code=401, detail=f"Token Firebase non valido: {str(e)}")

    user = db.query(models.User).filter(models.User.id == firebase_uid).first()
    if not user:
        new_tag = generate_unique_f1_tag(db)
        user = models.User(id=firebase_uid, f1_tag=new_tag, display_name=decoded_token.get("name"))
        db.add(user)
        db.commit()
        db.refresh(user)

    return user
