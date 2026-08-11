from datetime import date, datetime
from typing import List, Optional

from pydantic import BaseModel


class ConstructorStatsResponseSchema(BaseModel):
    constructor_id: str
    total_races: int
    wins: int
    podiums: int
    driver_championships: int
    constructor_championships: int
    first_gp_year: str
    first_win: str
    pole_positions: int
    fastest_laps: int
    total_points: float
    seasons_entered: int
    best_race_result: str
    best_championship_result: str
    power_unit: str
    team_principal: str
    base_location: str
    last_updated: datetime


class DriverSeasonStatsResponseSchema(BaseModel):
    driver_id: str
    year: int
    total_races: int
    wins: int
    second_places: int
    podiums: int
    laps_led: int
    fastest_laps: int
    beat_teammate_race: int
    beat_teammate_quali: int
    pole_positions: int
    front_rows: int
    retirements: int
    q3_appearances: int
    q2_appearances: int
    q1_appearances: int
    dsqs: int
    best_race_result: str
    sprint_starts: int
    sprint_wins: int
    sprint_top_3: int
    sprint_points_finishes: int
    sprint_points: int
    beat_teammate_sprint: int
    sprint_quali_poles: int
    last_updated: datetime


class ConstructorSeasonStatsResponseSchema(BaseModel):
    constructor_id: str
    year: int
    total_races: int
    wins: int
    podiums: int
    fastest_laps: int
    pole_positions: int
    front_rows: int
    one_two_finishes: int
    double_dnfs: int
    retirements: int
    dsqs: int
    races_in_points: int
    double_q3: int
    double_q2: int
    double_q1: int
    sprint_wins: int
    sprint_podiums: int
    sprint_points: int
    last_updated: datetime


# --- AUTH SCHEMAS ---
class UserResponseSchema(BaseModel):
    id: str
    email: str
    full_name: Optional[str] = None
    f1_tag: Optional[str] = None
    profile_image_url: Optional[str] = None
    favorite_constructor_id: Optional[str] = None
    favorite_driver1_id: Optional[str] = None
    favorite_driver2_id: Optional[str] = None
    preferences_set: bool
    auth_provider: str


class UpdatePreferencesSchema(BaseModel):
    favorite_team_id: Optional[str] = None
    favorite_driver1_id: Optional[str] = None
    favorite_driver2_id: Optional[str] = None
    preferences_set: bool


# --- RACE WEEK SCHEMAS ---
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


class SessionTimesSchema(BaseModel):
    fp1: Optional[str] = None
    fp2: Optional[str] = None
    fp3: Optional[str] = None
    sprint_shootout: Optional[str] = None
    sprint_race: Optional[str] = None
    quali: Optional[str] = None
    race: Optional[str] = None


class RaceWeekResponse(BaseModel):
    gp_name: str
    country: str
    city: str
    circuit_name: Optional[str] = None
    round_number: int
    is_sprint: bool
    status: str
    dates: List[str]
    weather_forecast: Optional[WeatherForecastSchema] = None
    sessions: SessionTimesSchema
    circuit_length: Optional[str] = None
    laps: Optional[int] = None
    corners: Optional[int] = None


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


class CircuitDetailResponse(BaseModel):
    round: int
    gp_name: str
    circuit_name: str
    location: str
    country: str
    length: str
    corners: int
    laps: int
    record: str
    is_sprint: bool
    status: str
    dates: List[str]
    previous_winner: str
    most_driver_wins: str
    most_constructor_wins: str
    most_driver_podiums: str
    most_poles: str
    num_races_held: int
    sessions: SessionTimesSchema


# --- RESULTS, STANDINGS AND CONTENT SCHEMAS ---
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
    q1: Optional[str] = None
    q2: Optional[str] = None
    q3: Optional[str] = None


class NewsArticleResponseSchema(BaseModel):
    id: int
    title: str
    source: str
    url: str
    image_url: Optional[str] = None
    published_at: datetime


class DriverStatsResponseSchema(BaseModel):
    driver_id: str
    total_races: int
    wins: int
    podiums: int
    pole_positions: int
    wins_from_pole: int
    world_championships: int

    best_race_result: str
    best_championship_result: str
    best_grid_position: str
    fastest_laps: int
    dns_count: int
    dnf_count: int
    dsq_count: int

    sprint_starts: int
    sprint_wins: int
    sprint_top_3: int
    best_sprint_result: str
    best_sprint_grid_position: str

    place_of_birth: str
    date_of_birth: str
    first_gp: str
    first_win: str
    hat_tricks: int
    grand_slams: int

    last_updated: datetime
