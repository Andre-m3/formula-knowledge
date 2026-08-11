from typing import Optional

from fastapi import APIRouter, Depends
from sqlalchemy import func
from sqlalchemy.orm import Session

from firebase_admin import auth as firebase_auth

from .. import database, models
from .dependencies import get_api_key, get_current_user, oauth2_scheme
from .schemas import UpdatePreferencesSchema, UserResponseSchema


router = APIRouter()


def map_team_string_to_id(db: Session, team_str: str) -> Optional[int]:
    if not team_str: return None
    search = team_str.lower().replace("_", " ")
    if search == "rb": search = "racing bulls"
    if search == "audi": search = "audi"
    t = db.query(models.Team).filter(func.lower(models.Team.name).contains(search)).first()
    return t.id if t else None


def map_driver_string_to_id(db: Session, driver_str: str) -> Optional[int]:
    if not driver_str: return None
    search = driver_str.split("_")[-1].lower()
    d = db.query(models.Driver).filter(func.lower(models.Driver.last_name).contains(search)).first()
    return d.id if d else None


def map_team_id_to_string(db: Session, team_id: int) -> Optional[str]:
    if not team_id: return None
    t = db.query(models.Team).filter(models.Team.id == team_id).first()
    if not t: return None
    n = t.name.lower()
    if "ferrari" in n: return "ferrari"
    if "mclaren" in n: return "mclaren"
    if "mercedes" in n: return "mercedes"
    if "red bull" in n: return "red_bull"
    if "aston" in n: return "aston_martin"
    if "alpine" in n: return "alpine"
    if "williams" in n: return "williams"
    if "bulls" in n: return "rb"
    if "audi" in n: return "audi"
    if "haas" in n: return "haas"
    if "cadillac" in n: return "cadillac"
    return "unknown"


def map_driver_id_to_string(db: Session, driver_id: int) -> Optional[str]:
    if not driver_id: return None
    d = db.query(models.Driver).filter(models.Driver.id == driver_id).first()
    if not d: return None
    n = d.last_name.lower().replace("ü", "u").replace("é", "e").replace(" jr.", "")
    if "sainz" in n: return "sainz"
    if "verstappen" in n: return "max_verstappen"
    if "lindblad" in n: return "arvid_lindblad"
    return n


def build_user_profile(user: models.User, token: str, db: Session):
    try:
        decoded_token = firebase_auth.verify_id_token(token)
    except:
        decoded_token = {}
    return {
        "id": user.id,
        "email": decoded_token.get("email", "N/A"),
        "full_name": user.display_name or decoded_token.get("name", "Tifoso"),
        "f1_tag": user.f1_tag,
        "profile_image_url": decoded_token.get("picture"),
        "favorite_constructor_id": map_team_id_to_string(db, user.favorite_team_id),
        "favorite_driver1_id": map_driver_id_to_string(db, user.favorite_driver1_id),
        "favorite_driver2_id": map_driver_id_to_string(db, user.favorite_driver2_id),
        "preferences_set": user.preferences_set,
        "auth_provider": decoded_token.get("firebase", {}).get("sign_in_provider", "unknown")
    }


@router.get("/api/v1/auth/me", response_model=UserResponseSchema, dependencies=[Depends(get_api_key)])
def get_my_profile(current_user: models.User = Depends(get_current_user), token: str = Depends(oauth2_scheme), db: Session = Depends(database.get_db)):
    return build_user_profile(current_user, token, db)


@router.put("/api/v1/auth/preferences", response_model=UserResponseSchema, dependencies=[Depends(get_api_key)])
def update_my_preferences(req: UpdatePreferencesSchema, current_user: models.User = Depends(get_current_user), token: str = Depends(oauth2_scheme), db: Session = Depends(database.get_db)):
    current_user.favorite_team_id = map_team_string_to_id(db, req.favorite_team_id)
    current_user.favorite_driver1_id = map_driver_string_to_id(db, req.favorite_driver1_id)
    current_user.favorite_driver2_id = map_driver_string_to_id(db, req.favorite_driver2_id)
    current_user.preferences_set = req.preferences_set

    db.commit()
    db.refresh(current_user)
    return build_user_profile(current_user, token, db)
