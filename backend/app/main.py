from fastapi import FastAPI, Depends, HTTPException
from sqlalchemy.orm import Session
from sqlalchemy import func
from pydantic import BaseModel
from typing import Optional, List
from datetime import date, timedelta, datetime, timezone

from . import database, models
from .services.fia_scraper import FiaScraperService
from .services.weather_service import WeatherService
from .services.calendar_service import CalendarService
from .services.results_service import ResultsService
from .services.external_api_service import ExternalApiService

app = FastAPI(title="Formula Knowledge API")

models.Base.metadata.create_all(bind=database.engine)

# --- SCHEMI ---

class DailyForecastSchema(BaseModel):
    day: str
    status: str
    temp_max: str
    temp_min: str
    wind: str
    rain_probability: str

class WeatherForecastSchema(BaseModel):
    status: str
    temp: str
    humidity: str
    feels_like: str
    wind: str
    uv: str
    rain_probability: str
    daily: List[DailyForecastSchema]

class RaceWeekResponse(BaseModel):
    gp_name: str
    country: str
    city: str
    circuit_name: Optional[str] = None
    round_number: int
    is_sprint: bool
    dates: List[str]
    weather_forecast: Optional[WeatherForecastSchema] = None

class CalendarEntryResponse(BaseModel):
    name: str
    country: str
    city: str
    circuit_name: Optional[str] = None
    date: date
    round: int
    status: str
    is_clickable: bool
    cancelled: Optional[bool] = False

class DriverStandingResponse(BaseModel):
    position: int
    driver_name: str
    constructor_name: str
    points: int
    wins: int

class ConstructorStandingResponse(BaseModel):
    position: int
    constructor_name: str
    chassis_name: Optional[str] = None
    points: int
    wins: int

class TeamUpdatesResponse(BaseModel):
    team_name: str
    team_color_hex: str
    updates: List[str]

class TeamUpdatesWrapperSchema(BaseModel):
    status: str
    gp: str
    data: List[TeamUpdatesResponse] = []

class RaceResultResponseSchema(BaseModel):
    position: int
    driver: str
    team: str
    points: int
    time: str

class CircuitDetailResponse(BaseModel):
    round: int
    gp_name: str
    circuit_name: str
    location: str
    length: str
    laps: int
    record: str
    is_sprint: bool
    dates: List[str]
    status: str

# --- ENDPOINTS ---

@app.get("/api/v1/raceweek/current", response_model=RaceWeekResponse)
async def get_current_raceweek(db: Session = Depends(database.get_db)):
    calendar = CalendarService()
    weather = WeatherService()
    race_info = calendar.get_current_or_next_race()
    db_race = db.query(models.Race).filter(models.Race.round_number == race_info["round"]).first()
    is_sprint = db_race.is_sprint if db_race else False
    forecast = weather.get_forecast(race_info["lat"], race_info["lon"])
    race_date = race_info["date"]
    dates_list = [
        (race_date - timedelta(days=2)).strftime('%d %b'),
        (race_date - timedelta(days=1)).strftime('%d %b'),
        race_date.strftime('%d %b')
    ]
    return {
        "gp_name": race_info["name"],
        "country": race_info["country"],
        "city": race_info["city"],
        "circuit_name": race_info.get("circuit_name"),
        "round_number": race_info["round"],
        "is_sprint": is_sprint,
        "dates": dates_list,
        "weather_forecast": forecast
    }

@app.get("/api/v1/circuit/{round_number}", response_model=CircuitDetailResponse)
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

    return {
        "round": race.round_number,
        "gp_name": race.name,
        "circuit_name": race.circuit_name or race.name,
        "location": f"{race.city.upper()} ({race.country})",
        "length": race.circuit_length or "N/A",
        "laps": race.laps,
        "record": race.lap_record or "N/A",
        "is_sprint": race.is_sprint,
        "dates": dates_list,
        "status": status
    }

@app.get("/api/v1/results/{round_number}", response_model=List[RaceResultResponseSchema])
def get_race_results(round_number: int, db: Session = Depends(database.get_db)):
    # 1. Cerchiamo i risultati nel DB locale (ordinati per posizione)
    db_results = db.query(models.RaceResult).join(models.Race).filter(models.Race.round_number == round_number).order_by(models.RaceResult.position).all()
    if db_results:
        return [
            {
                "position": r.position,
                "driver": f"{r.driver.first_name} {r.driver.last_name}",
                "team": r.driver.team.name,
                "points": int(r.points),
                "time": r.time_str or ""
            } for r in db_results
        ]
    
    # 2. Se non ci sono nel DB (gara mai caricata prima), chiamiamo Jolpica
    external_data = ExternalApiService.get_race_results(round_number, year=2026)
    
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
                    db_last_lower.replace(" jr.", "") in api_driver_lower or
                    ("limblad" in db_last_lower and "lindblad" in api_driver_lower)):
                    db_driver = d
                    break
                    
            if db_driver:
                new_result = models.RaceResult(race_id=race.id, driver_id=db_driver.id, position=data["position"], points=data["points"], time_str=data["time"])
                db.add(new_result)
        db.commit()
        
    return [RaceResultResponseSchema(**data) for data in external_data]

@app.get("/api/v1/standings/drivers", response_model=List[DriverStandingResponse])
def get_driver_standings(db: Session = Depends(database.get_db)):
    cached = db.query(models.DriverStandingCache).all()
    if cached:
        return [DriverStandingResponse(position=c.position, driver_name=c.driver_name, constructor_name=c.constructor_name, points=c.points, wins=c.wins) for c in cached]
    external_data = ExternalApiService.get_driver_standings(year=2026)
    for item in external_data:
        new_cache = models.DriverStandingCache(position=item["position"], driver_name=item["driver_name"], constructor_name=item["constructor_name"], points=item["points"], wins=item["wins"], last_updated=date.today())
        db.add(new_cache)
    db.commit()
    return [DriverStandingResponse(**data) for data in external_data]

@app.get("/api/v1/standings/constructors", response_model=List[ConstructorStandingResponse])
def get_constructor_standings(db: Session = Depends(database.get_db)):
    cached = db.query(models.ConstructorStandingCache).all()
    if cached:
        return [ConstructorStandingResponse(position=c.position, constructor_name=c.constructor_name, chassis_name=c.chassis_name, points=c.points, wins=c.wins) for c in cached]
    external_data = ExternalApiService.get_constructor_standings(year=2026)
    enriched_results = []
    for item in external_data:
        api_name = item["constructor_name"].lower()
        db_team = db.query(models.Team).filter(func.lower(models.Team.name).contains(api_name)).first()
        if not db_team:
            parts = api_name.split()
            if parts:
                db_team = db.query(models.Team).filter(func.lower(models.Team.name).contains(parts[0])).first()
        chassis = db_team.chassis_name if db_team else "N/A"
        new_cache = models.ConstructorStandingCache(position=item["position"], constructor_name=item["constructor_name"], chassis_name=chassis, points=item["points"], wins=item["wins"], last_updated=date.today())
        db.add(new_cache)
        enriched_results.append({"position": item["position"], "constructor_name": item["constructor_name"], "chassis_name": chassis, "points": item["points"], "wins": item["wins"]})
    db.commit()
    return enriched_results

@app.get("/api/v1/calendar", response_model=List[CalendarEntryResponse])
def get_calendar():
    calendar = CalendarService()
    return calendar.get_full_calendar()

@app.get("/api/v1/raceweek/updates", response_model=TeamUpdatesWrapperSchema)
def get_latest_car_updates(db: Session = Depends(database.get_db)):
    calendar = CalendarService()
    current_race = calendar.get_current_or_next_race()
    round_num = current_race["round"]
    existing_updates = db.query(models.TechnicalUpdate).filter(models.TechnicalUpdate.race_id == round_num).all()
    if existing_updates:
        teams_dict = {}
        for up in existing_updates:
            team_name = up.team.name
            if team_name not in teams_dict:
                teams_dict[team_name] = {"color": up.team.color_hex, "updates": []}
            teams_dict[team_name]["updates"].append(up.description)
        data = [TeamUpdatesResponse(team_name=k, team_color_hex=v["color"], updates=v["updates"]) for k, v in teams_dict.items()]
        return TeamUpdatesWrapperSchema(status="ready", gp=current_race["name"], data=data)
    scraper = FiaScraperService()
    result = scraper.process_latest_car_presentation()
    if result["status"] == "not_ready":
        return TeamUpdatesWrapperSchema(status="not_ready", gp=result["gp"], data=[])
    final_data = []
    for item in result["data"]:
        db_team = db.query(models.Team).filter(models.Team.name == item["team_name"]).first()
        if db_team:
            for update_desc in item["updates"]:
                new_update = models.TechnicalUpdate(race_id=round_num, team_id=db_team.id, component="General", description=update_desc)
                db.add(new_update)
            final_data.append(TeamUpdatesResponse(team_name=item["team_name"], team_color_hex=db_team.color_hex, updates=item["updates"]))
    db.commit()
    return TeamUpdatesWrapperSchema(status="ready", gp=result["gp"], data=final_data)

@app.get("/api/v1/results/{round_number}/updates", response_model=List[TeamUpdatesResponse])
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
