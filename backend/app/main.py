from fastapi import FastAPI, Depends, HTTPException
from sqlalchemy.orm import Session
from pydantic import BaseModel
from typing import Optional, List
from datetime import date, timedelta

from . import database, models
from .services.fia_scraper import FiaScraperService
from .services.weather_service import WeatherService
from .services.calendar_service import CalendarService
from .services.results_service import ResultsService
from .services.external_api_service import ExternalApiService

app = FastAPI(title="Formula Knowledge API")

# Questo comando crea fisicamente le tabelle nel DB SQLite basandosi sui modelli creati
models.Base.metadata.create_all(bind=database.engine)

# --- SCHEMI ---

class WeatherForecastSchema(BaseModel):
    status: str
    temp: str
    humidity: str
    feels_like: str
    wind: str
    uv: str

class RaceWeekResponse(BaseModel):
    gp_name: str
    country: str
    city: str
    round_number: int
    is_sprint: bool
    dates: List[str]
    weather_forecast: Optional[WeatherForecastSchema] = None

class CalendarEntryResponse(BaseModel):
    name: str
    country: str
    city: str
    date: date
    round: int
    status: str
    is_clickable: bool

class DriverStandingResponse(BaseModel):
    position: int
    driver_name: str
    constructor_name: str
    points: int
    wins: int

class ConstructorStandingResponse(BaseModel):
    position: int
    constructor_name: str
    points: int
    wins: int

class TeamUpdatesResponse(BaseModel):
    team_name: str
    team_color_hex: str
    updates: List[str]

class RaceResultResponseSchema(BaseModel):
    position: int
    driver: str
    team: str
    points: int
    time: str

# --- ENDPOINTS ---

@app.get("/api/v1/raceweek/current", response_model=RaceWeekResponse)
async def get_current_raceweek():
    calendar = CalendarService()
    weather = WeatherService()
    race = calendar.get_current_or_next_race()
    forecast = weather.get_forecast(race["lat"], race["lon"])
    
    race_date = race["date"]
    dates_list = [
        (race_date - timedelta(days=2)).strftime('%d %b'),
        (race_date - timedelta(days=1)).strftime('%d %b'),
        race_date.strftime('%d %b')
    ]
    return {
        "gp_name": race["name"],
        "country": race["country"],
        "city": race["city"],
        "round_number": race["round"],
        "is_sprint": False,
        "dates": dates_list,
        "weather_forecast": forecast
    }

@app.get("/api/v1/standings/drivers", response_model=List[DriverStandingResponse])
def get_driver_standings():
    # Nessun database: chiamata diretta alle API esterne!
    external_data = ExternalApiService.get_driver_standings(year=2026)
    return [DriverStandingResponse(**data) for data in external_data]

@app.get("/api/v1/standings/constructors", response_model=List[ConstructorStandingResponse])
def get_constructor_standings():
    # Nessun database: chiamata diretta alle API esterne!
    external_data = ExternalApiService.get_constructor_standings(year=2026)
    return [ConstructorStandingResponse(**data) for data in external_data]

@app.get("/api/v1/calendar", response_model=List[CalendarEntryResponse])
def get_calendar():
    calendar = CalendarService()
    return calendar.get_full_calendar()

@app.get("/api/v1/results/{round_number}", response_model=List[RaceResultResponseSchema])
def get_race_results(round_number: int):
    # Nessun database: chiamata diretta alle API esterne (Jolpica)!
    external_data = ExternalApiService.get_race_results(round_number, year=2026)
    return [RaceResultResponseSchema(**data) for data in external_data]

@app.get("/api/v1/raceweek/updates", response_model=List[TeamUpdatesResponse])
def get_latest_car_updates(db: Session = Depends(database.get_db)):
    scraper = FiaScraperService()
    parsed_data = scraper.process_latest_car_presentation()
    response = []
    for item in parsed_data:
        db_team = db.query(models.Team).filter(models.Team.name == item["team_name"]).first()
        response.append(TeamUpdatesResponse(team_name=item["team_name"], team_color_hex=db_team.color_hex if db_team else "#808080", updates=item["updates"]))
    return response
