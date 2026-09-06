from typing import List

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from .. import database, models
from ..services.external_api_service import ExternalApiRateLimitError, ExternalApiService
from ..services.result_cache_service import (
    ResultPersistenceError,
    persist_session_results,
)
from .dependencies import get_api_key
from .schemas import RaceResultResponseSchema, TeamUpdatesResponse


router = APIRouter()


def _get_cached_session_results(
    db: Session,
    round_number: int,
    session_type: str,
) -> list[models.RaceResult]:
    return (
        db.query(models.RaceResult)
        .join(models.Race)
        .filter(
            models.Race.round_number == round_number,
            models.RaceResult.session_type == session_type,
        )
        .order_by(models.RaceResult.position)
        .all()
    )


def _serialize_result(result: models.RaceResult) -> dict:
    if result.driver is not None:
        driver_name = f"{result.driver.first_name} {result.driver.last_name}"
        fallback_team_name = result.driver.team.name if result.driver.team else ""
    elif result.session_participant is not None:
        driver_name = (
            f"{result.session_participant.first_name} "
            f"{result.session_participant.last_name}"
        )
        fallback_team_name = ""
    else:
        driver_name = "Unknown driver"
        fallback_team_name = ""

    return {
        "position": result.position,
        "driver": driver_name,
        "team": result.team_name or fallback_team_name,
        "points": int(result.points),
        "time": result.time_str or "",
        "q1": result.q1,
        "q2": result.q2,
        "q3": result.q3,
        "is_session_only": result.session_participant is not None,
    }


@router.get("/api/v1/results/{round_number}/updates", response_model=List[TeamUpdatesResponse], dependencies=[Depends(get_api_key)])
def get_past_gp_updates(round_number: int, db: Session = Depends(database.get_db)):
    updates = db.query(models.TechnicalUpdate).filter(models.TechnicalUpdate.race_id == round_number).all()
    if not updates:
        return []
    teams_dict = {}
    for up in updates:
        team_name = up.team.name
        if team_name not in teams_dict:
            teams_dict[team_name] = {"color": up.team.color_hex, "updates": []}
        teams_dict[team_name]["updates"].append(up.description)
    return [TeamUpdatesResponse(team_name=k, team_color_hex=v["color"], updates=v["updates"]) for k, v in teams_dict.items()]


@router.get("/api/v1/results/{round_number}/{session_type}", response_model=List[RaceResultResponseSchema], dependencies=[Depends(get_api_key)])
def get_session_results(round_number: int, session_type: str, db: Session = Depends(database.get_db)):
    db_results = _get_cached_session_results(db, round_number, session_type)
    if db_results:
        return [_serialize_result(result) for result in db_results]

    try:
        external_data = ExternalApiService.get_session_results(
            round_number,
            session_type,
            year=2026,
        )
    except ExternalApiRateLimitError:
        # The local cache remains authoritative. A client without cached data
        # receives the existing empty-result response and can retry later.
        print(f"Jolpica rate limited {round_number}/{session_type}; no cache update.")
        external_data = []

    if external_data:
        try:
            persist_session_results(
                db,
                round_number,
                session_type,
                external_data,
            )
        except ResultPersistenceError as exc:
            print(f"Risultati {round_number}/{session_type} non messi in cache: {exc}")
        else:
            # Keep the response identical on a cache miss: after persistence,
            # include tester metadata and the provider team snapshot immediately.
            cached_results = _get_cached_session_results(db, round_number, session_type)
            if cached_results:
                return [_serialize_result(result) for result in cached_results]

    return [RaceResultResponseSchema(**data) for data in external_data]