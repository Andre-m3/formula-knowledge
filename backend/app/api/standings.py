from datetime import date
from typing import List

from fastapi import APIRouter, Depends
from sqlalchemy import func
from sqlalchemy.orm import Session

from .. import database, models
from ..services.external_api_service import ExternalApiService
from .dependencies import get_api_key
from .schemas import ConstructorStandingResponse, DriverStandingResponse


router = APIRouter()


@router.get("/api/v1/standings/drivers", response_model=List[DriverStandingResponse], dependencies=[Depends(get_api_key)])
def get_driver_standings(db: Session = Depends(database.get_db)):
    cached = db.query(models.DriverStandingCache).all()
    # Controlla se la cache non è vuota e se i dati sono di oggi
    if cached and all(c.last_updated == date.today() for c in cached):
        return [DriverStandingResponse(position=c.position, driver_name=c.driver_name, constructor_name=c.constructor_name, points=c.points, wins=c.wins) for c in cached]

    # Se la cache è vecchia o vuota, la puliamo e procediamo a ricaricare i dati
    db.query(models.DriverStandingCache).delete()
    db.commit()

    external_data = ExternalApiService.get_driver_standings(year=2026)
    for item in external_data:
        new_cache = models.DriverStandingCache(position=item["position"], driver_name=item["driver_name"], constructor_name=item["constructor_name"], points=item["points"], wins=item["wins"], last_updated=date.today())
        db.add(new_cache)
    db.commit()
    return [DriverStandingResponse(**data) for data in external_data]


@router.get("/api/v1/standings/constructors", response_model=List[ConstructorStandingResponse], dependencies=[Depends(get_api_key)])
def get_constructor_standings(db: Session = Depends(database.get_db)):
    cached = db.query(models.ConstructorStandingCache).all()
    # Controlla se la cache non è vuota e se i dati sono di oggi
    if cached and all(c.last_updated == date.today() for c in cached):
        return [ConstructorStandingResponse(position=c.position, constructor_name=c.constructor_name, chassis_name=c.chassis_name, points=c.points, wins=c.wins) for c in cached]

    # Se la cache è vecchia o vuota, la puliamo e procediamo a ricaricare i dati
    db.query(models.ConstructorStandingCache).delete()
    db.commit()

    external_data = ExternalApiService.get_constructor_standings(year=2026)
    enriched_results = []
    for item in external_data:
        api_name = item["constructor_name"].lower()

        search_name = api_name
        if api_name == "rb" or "rb " in api_name or "racing bulls" in api_name or "alphatauri" in api_name:
            search_name = "racing bulls"
        elif "haas" in api_name:
            search_name = "haas"
        elif "alpine" in api_name:
            search_name = "alpine"
        elif "aston" in api_name:
            search_name = "aston"

        db_team = db.query(models.Team).filter(func.lower(models.Team.name).contains(search_name)).first()
        if not db_team:
            parts = search_name.split()
            if parts:
                db_team = db.query(models.Team).filter(func.lower(models.Team.name).contains(parts[0])).first()
        chassis = db_team.chassis_name if db_team else "N/A"
        new_cache = models.ConstructorStandingCache(position=item["position"], constructor_name=item["constructor_name"], chassis_name=chassis, points=item["points"], wins=item["wins"], last_updated=date.today())
        db.add(new_cache)
        enriched_results.append({"position": item["position"], "constructor_name": item["constructor_name"], "chassis_name": chassis, "points": item["points"], "wins": item["wins"]})
    db.commit()
    return enriched_results
