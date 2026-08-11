from datetime import datetime, timedelta, timezone
from typing import List

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from .. import database, models
from ..services.calendar_service import CalendarService, CalendarUnavailableError
from ..services.weather_service import WeatherService
from .dependencies import get_api_key
from .schemas import CalendarEntryResponse, CircuitDetailResponse, RaceWeekResponse


router = APIRouter()


@router.get("/api/v1/raceweek/current", response_model=RaceWeekResponse, dependencies=[Depends(get_api_key)])
async def get_current_raceweek(db: Session = Depends(database.get_db)):
    try:
        calendar = CalendarService()
    except CalendarUnavailableError as exc:
        raise HTTPException(status_code=503, detail=str(exc))
    weather = WeatherService()
    race_info = calendar.get_current_or_next_race()
    db_race = db.query(models.Race).filter(models.Race.round_number == race_info["round"]).first()
    is_sprint = db_race.is_sprint if db_race else False

    # FIX Coordinate: Jolpica a volte ritorna 0.0 per i circuiti cittadini storici
    lat = race_info.get("lat", 0.0)
    lon = race_info.get("lon", 0.0)
    if (lat == 0.0 or lon == 0.0) and "monaco" in race_info.get("name", "").lower():
        lat, lon = 43.7347, 7.4206

    forecast = weather.get_forecast(lat, lon)
    race_date = race_info["date"]
    dates_list = [
        (race_date - timedelta(days=2)).strftime('%d %b'),
        (race_date - timedelta(days=1)).strftime('%d %b'),
        race_date.strftime('%d %b')
    ]

    sessions = {
        "fp1": db_race.fp1_time, "fp2": db_race.fp2_time,
        "fp3": db_race.fp3_time, "sprint_shootout": db_race.sprint_shootout_time,
        "sprint_race": db_race.sprint_race_time, "quali": db_race.quali_time,
        "race": db_race.race_time
    } if db_race else {}

    # Calcoliamo se la gara è "questa settimana" (entro 6 giorni da oggi) o se è lontana nel futuro
    now_utc = datetime.now(timezone.utc).date()
    if race_date < now_utc:
        status = "past"
    elif race_date <= now_utc + timedelta(days=6):
        status = "current"
    else:
        status = "future"

    return {
        "gp_name": race_info["name"],
        "country": race_info["country"],
        "city": race_info["city"],
        "circuit_name": race_info.get("circuit_name"),
        "round_number": race_info["round"],
        "is_sprint": is_sprint,
        "status": status,
        "dates": dates_list,
        "weather_forecast": forecast,
        "sessions": sessions,
        "circuit_length": db_race.circuit_length if db_race else "N/A",
        "laps": db_race.laps if db_race else 0,
        "corners": db_race.corners if db_race else 0
    }


@router.get("/api/v1/circuit/{round_number}", response_model=CircuitDetailResponse, dependencies=[Depends(get_api_key)])
def get_circuit_details(round_number: int, db: Session = Depends(database.get_db)):
    race = db.query(models.Race).filter(models.Race.round_number == round_number).first()
    if not race:
        raise HTTPException(status_code=404, detail="Circuit not found")

    race_date = race.date
    dates_list = [
        (race_date - timedelta(days=2)).strftime('%d %b'),
        (race_date - timedelta(days=1)).strftime('%d %b'),
        race_date.strftime('%d %b')
    ]

    now_utc = datetime.now(timezone.utc).date()
    if race.date < now_utc:
        status = "past"
    elif race.date == now_utc:
        status = "current"
    else:
        status = "future"

    display_gp_name = race.name
    if "Emilia Romagna" in race.name:
        display_gp_name = "Imola Grand Prix"

    return {
        "round": race.round_number,
        "gp_name": display_gp_name,
        "country": race.country,
        "circuit_name": race.circuit_name or race.name,
        "location": f"{race.city.upper()} ({race.country})",
        "length": race.circuit_length or "N/A",
        "corners": race.corners or 0,
        "laps": race.laps,
        "record": race.lap_record or "N/A",
        "is_sprint": race.is_sprint,
        "dates": dates_list,
        "status": status,
        "previous_winner": race.previous_winner or "N/A",
        "most_driver_wins": race.most_driver_wins or "N/A",
        "most_constructor_wins": race.most_constructor_wins or "N/A",
        "most_driver_podiums": race.most_driver_podiums or "N/A",
        "most_poles": race.most_poles or "N/A",
        "num_races_held": race.num_races_held or 0,
        "sessions": {
            "fp1": race.fp1_time,
            "fp2": race.fp2_time,
            "fp3": race.fp3_time,
            "sprint_shootout": race.sprint_shootout_time,
            "sprint_race": race.sprint_race_time,
            "quali": race.quali_time,
            "race": race.race_time
        }
    }


@router.get("/api/v1/calendar", response_model=List[CalendarEntryResponse], dependencies=[Depends(get_api_key)])
def get_calendar():
    try:
        calendar = CalendarService()
    except CalendarUnavailableError as exc:
        raise HTTPException(status_code=503, detail=str(exc))
    return calendar.get_full_calendar()
