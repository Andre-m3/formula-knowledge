from fastapi import FastAPI, Depends, HTTPException
from sqlalchemy.orm import Session
from pydantic import BaseModel
from typing import Optional, List
from datetime import datetime, timedelta

# Importiamo il necessario per creare il DB
from app.models import database, models
from app.services.fia_scraper import FiaScraperService
from app.services.weather_service import WeatherService
from app.services.calendar_service import CalendarService
from app.services.results_service import ResultsService

# Setup fittizio di FastAPI
app = FastAPI(title="Formula Knowledge API")

# Questa riga dice ad SQLAlchemy di creare il file .db e tutte le tabelle se non esistono!
models.Base.metadata.create_all(bind=database.engine)

# --- SCHEMI PYDANTIC ---

class RaceWeekResponse(BaseModel):
    gp_name: str
    country: str
    round_number: int
    is_sprint: bool
    dates: List[str]
    weather_forecast: Optional[dict] = None

class CalendarEntryResponse(BaseModel):
    name: str
    country: str
    city: str
    date: str
    round: int
    status: str
    is_clickable: bool

class RaceResultEntry(BaseModel):
    position: int
    driver: str
    team: str
    points: int
    time: str

class TeamUpdatesResponse(BaseModel):
    team_name: str
    team_color_hex: str
    updates: List[str]

# --- ENDPOINTS ---

@app.get("/api/v1/raceweek/current", response_model=RaceWeekResponse)
async def get_current_raceweek(db: Session = Depends(database.get_db)):
    calendar = CalendarService()
    weather = WeatherService()
    
    race = calendar.get_current_or_next_race()
    forecast = weather.get_forecast(race["lat"], race["lon"])
    
    race_date = race["date"]
    dates_list = [
        f"{(race_date - timedelta(days=2)).day} {race_date.strftime('%b')}",
        f"{(race_date - timedelta(days=1)).day} {race_date.strftime('%b')}",
        f"{race_date.day} {race_date.strftime('%b')}"
    ]
    
    return {
        "gp_name": race["name"],
        "country": race["country"],
        "round_number": race["round"],
        "is_sprint": False,
        "dates": dates_list,
        "weather_forecast": forecast
    }

@app.get("/api/v1/calendar", response_model=List[CalendarEntryResponse])
def get_calendar():
    calendar = CalendarService()
    full_calendar = calendar.get_full_calendar()
    
    response = []
    for race in full_calendar:
        race_copy = race.copy()
        race_copy["date"] = race["date"].isoformat()
        response.append(race_copy)
    return response

@app.get("/api/v1/results/{round_number}", response_model=List[RaceResultEntry])
def get_results(round_number: int):
    results_service = ResultsService()
    results = results_service.get_race_results(round_number)
    if not results:
        raise HTTPException(status_code=404, detail="Risultati non trovati per questo round")
    return results

@app.get("/api/v1/raceweek/updates", response_model=List[TeamUpdatesResponse])
def get_latest_car_updates(db: Session = Depends(database.get_db)):
    scraper = FiaScraperService()
    parsed_data = scraper.process_latest_car_presentation()
    
    response_list = []
    for item in parsed_data:
        db_team = db.query(models.Team).filter(models.Team.name == item["team_name"]).first()
        team_color = db_team.color_hex if db_team else "#808080"
        
        response_list.append(TeamUpdatesResponse(
            team_name=item["team_name"],
            team_color_hex=team_color,
            updates=item["updates"]
        ))
        
    return response_list
