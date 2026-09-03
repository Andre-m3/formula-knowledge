from typing import Optional

from fastapi import APIRouter, Depends
from sqlalchemy import func
from sqlalchemy.orm import Session

from .. import database, models
from .dependencies import AuthenticatedUser, get_api_key, get_current_user_context
from .schemas import UpdatePreferencesSchema, UserResponseSchema


router = APIRouter()


def map_team_string_to_id(db: Session, team_str: str) -> Optional[int]:
    if not team_str:
        return None
    search = team_str.lower().replace("_", " ")
    if search == "rb":
        search = "racing bulls"
    if search == "audi":
        search = "audi"
    team = db.query(models.Team).filter(func.lower(models.Team.name).contains(search)).first()
    return team.id if team else None


def map_driver_string_to_id(db: Session, driver_str: str) -> Optional[int]:
    if not driver_str:
        return None
    search = driver_str.split("_")[-1].lower()
    driver = db.query(models.Driver).filter(func.lower(models.Driver.last_name).contains(search)).first()
    return driver.id if driver else None


def map_team_id_to_string(db: Session, team_id: int) -> Optional[str]:
    if not team_id:
        return None
    team = db.query(models.Team).filter(models.Team.id == team_id).first()
    if not team:
        return None
    name = team.name.lower()
    if "ferrari" in name:
        return "ferrari"
    if "mclaren" in name:
        return "mclaren"
    if "mercedes" in name:
        return "mercedes"
    if "red bull" in name:
        return "red_bull"
    if "aston" in name:
        return "aston_martin"
    if "alpine" in name:
        return "alpine"
    if "williams" in name:
        return "williams"
    if "bulls" in name:
        return "rb"
    if "audi" in name:
        return "audi"
    if "haas" in name:
        return "haas"
    if "cadillac" in name:
        return "cadillac"
    return "unknown"


def map_driver_id_to_string(db: Session, driver_id: int) -> Optional[str]:
    if not driver_id:
        return None
    driver = db.query(models.Driver).filter(models.Driver.id == driver_id).first()
    if not driver:
        return None
    name = driver.last_name.lower().replace("ü", "u").replace("é", "e").replace(" jr.", "")
    if "sainz" in name:
        return "sainz"
    if "verstappen" in name:
        return "max_verstappen"
    if "lindblad" in name:
        return "arvid_lindblad"
    return name


def build_user_profile(user: models.User, claims: dict, db: Session):
    return {
        "id": user.id,
        "email": claims.get("email", "N/A"),
        "full_name": user.display_name or claims.get("name", "Tifoso"),
        "f1_tag": user.f1_tag,
        "profile_image_url": claims.get("picture"),
        "favorite_constructor_id": map_team_id_to_string(db, user.favorite_team_id),
        "favorite_driver1_id": map_driver_id_to_string(db, user.favorite_driver1_id),
        "favorite_driver2_id": map_driver_id_to_string(db, user.favorite_driver2_id),
        "preferences_set": user.preferences_set,
        "auth_provider": claims.get("firebase", {}).get("sign_in_provider", "unknown"),
    }


@router.get("/api/v1/auth/me", response_model=UserResponseSchema, dependencies=[Depends(get_api_key)])
def get_my_profile(
    auth_context: AuthenticatedUser = Depends(get_current_user_context),
    db: Session = Depends(database.get_db),
):
    return build_user_profile(auth_context.user, auth_context.claims, db)


@router.put("/api/v1/auth/preferences", response_model=UserResponseSchema, dependencies=[Depends(get_api_key)])
def update_my_preferences(
    req: UpdatePreferencesSchema,
    auth_context: AuthenticatedUser = Depends(get_current_user_context),
    db: Session = Depends(database.get_db),
):
    user = auth_context.user
    user.favorite_team_id = map_team_string_to_id(db, req.favorite_team_id)
    user.favorite_driver1_id = map_driver_string_to_id(db, req.favorite_driver1_id)
    user.favorite_driver2_id = map_driver_string_to_id(db, req.favorite_driver2_id)
    user.preferences_set = req.preferences_set

    db.commit()
    db.refresh(user)
    return build_user_profile(user, auth_context.claims, db)