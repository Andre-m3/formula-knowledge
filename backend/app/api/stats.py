from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from .. import database, models
from .dependencies import get_api_key
from .schemas import (
    ConstructorSeasonStatsResponseSchema,
    ConstructorStatsResponseSchema,
    DriverSeasonStatsResponseSchema,
    DriverStatsResponseSchema,
)


router = APIRouter()


@router.get("/api/v1/drivers/{driver_id}/stats", response_model=DriverStatsResponseSchema, dependencies=[Depends(get_api_key)])
def get_driver_stats(driver_id: str, db: Session = Depends(database.get_db)):
    # Nessuna chiamata esterna! Risposta fulminea grazie all'Internal Aggregator.
    stats = db.query(models.DriverCareerStats).filter(models.DriverCareerStats.driver_id == driver_id).first()
    if not stats:
        raise HTTPException(status_code=404, detail="Driver stats not found")
    return stats


@router.get("/api/v1/constructors/{constructor_id}/stats", response_model=ConstructorStatsResponseSchema, dependencies=[Depends(get_api_key)])
def get_constructor_stats(constructor_id: str, db: Session = Depends(database.get_db)):
    stats = db.query(models.ConstructorCareerStats).filter(models.ConstructorCareerStats.constructor_id == constructor_id).first()
    if not stats:
        raise HTTPException(status_code=404, detail="Constructor stats not found")
    return stats


@router.get("/api/v1/drivers/{driver_id}/season_stats", response_model=DriverSeasonStatsResponseSchema, dependencies=[Depends(get_api_key)])
def get_driver_season_stats(driver_id: str, db: Session = Depends(database.get_db)):
    stats = db.query(models.DriverSeasonStats).filter(models.DriverSeasonStats.driver_id == driver_id, models.DriverSeasonStats.year == 2026).first()
    if not stats:
        raise HTTPException(status_code=404, detail="Driver season stats not found")
    return stats


@router.get("/api/v1/constructors/{constructor_id}/season_stats", response_model=ConstructorSeasonStatsResponseSchema, dependencies=[Depends(get_api_key)])
def get_constructor_season_stats(constructor_id: str, db: Session = Depends(database.get_db)):
    stats = db.query(models.ConstructorSeasonStats).filter(models.ConstructorSeasonStats.constructor_id == constructor_id, models.ConstructorSeasonStats.year == 2026).first()
    if not stats:
        raise HTTPException(status_code=404, detail="Constructor season stats not found")
    return stats
