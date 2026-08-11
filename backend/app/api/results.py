from typing import List

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from .. import database, models
from ..services.external_api_service import ExternalApiService
from .dependencies import get_api_key
from .schemas import RaceResultResponseSchema, TeamUpdatesResponse


router = APIRouter()


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
    # 1. Cerchiamo i risultati nel DB locale
    db_results = db.query(models.RaceResult).join(models.Race).filter(models.Race.round_number == round_number, models.RaceResult.session_type == session_type).order_by(models.RaceResult.position).all()
    if db_results:
        return [
            {
                "position": r.position,
                "driver": f"{r.driver.first_name} {r.driver.last_name}",
                "team": r.driver.team.name,
                "points": int(r.points),
                "time": r.time_str or "",
                "q1": r.q1,
                "q2": r.q2,
                "q3": r.q3
            } for r in db_results
        ]

    # 2. Se non ci sono nel DB (sessione mai caricata prima), chiamiamo Jolpica
    external_data = ExternalApiService.get_session_results(round_number, session_type, year=2026)

    # 3. Salviamo nel DB per fare da cache persistente
    race = db.query(models.Race).filter(models.Race.round_number == round_number).first()
    if race and external_data:
        db_drivers = db.query(models.Driver).all()
        for data in external_data:
            # Trova il pilota tramite cognome, gestendo anche suffissi come "Jr." e il typo "Limblad/Lindblad"
            db_driver = None
            api_driver_lower = data["driver"].lower()
            for d in db_drivers:
                db_last_lower = d.last_name.lower()
                if (db_last_lower in api_driver_lower or
                    db_last_lower.replace(" jr.", "") in api_driver_lower):
                    db_driver = d
                    break

            if db_driver:
                new_result = models.RaceResult(
                    race_id=race.id,
                    driver_id=db_driver.id,
                    position=data["position"],
                    points=data["points"],
                    time_str=data["time"],
                    q1=data.get("q1"), q2=data.get("q2"), q3=data.get("q3"),
                    session_type=session_type
                )
                db.add(new_result)
        db.commit()

    return [RaceResultResponseSchema(**data) for data in external_data]
