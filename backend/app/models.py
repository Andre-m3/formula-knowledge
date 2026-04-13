from sqlalchemy import Column, Integer, String, Float, Date, ForeignKey, Boolean, DateTime
from sqlalchemy.orm import Mapped, relationship
from typing import Optional
from datetime import datetime, timezone
from .database import Base

class Team(Base):
    __tablename__ = "teams"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, unique=True, index=True, nullable=False)
    color_hex = Column(String, nullable=False)
    power_unit = Column(String, nullable=True)
    chassis_name = Column(String, nullable=True)

    drivers = relationship("Driver", back_populates="team")
    updates = relationship("TechnicalUpdate", back_populates="team")

class Driver(Base):
    __tablename__ = "drivers"

    id = Column(Integer, primary_key=True, index=True)
    first_name = Column(String, nullable=False)
    last_name = Column(String, nullable=False)
    number = Column(Integer, unique=True, nullable=False)
    nationality = Column(String, nullable=True)
    
    team_id = Column(Integer, ForeignKey("teams.id"))
    team = relationship("Team", back_populates="drivers")
    race_results = relationship("RaceResult", back_populates="driver")

class Race(Base):
    __tablename__ = "races"

    id = Column(Integer, primary_key=True, index=True)
    round_number = Column(Integer, unique=True, nullable=False)
    name = Column(String, nullable=False)
    date = Column(Date, nullable=False)
    country = Column(String, nullable=False)
    city = Column(String, nullable=False)
    circuit_name = Column(String, nullable=True)
    
    laps = Column(Integer, default=57)
    circuit_length = Column(String, nullable=True)
    corners = Column(Integer, nullable=True)
    lap_record = Column(String, nullable=True)
    is_sprint = Column(Boolean, default=False)
    cancelled = Column(Boolean, default=False)
    
    fp1_time: Mapped[Optional[str]]
    fp2_time: Mapped[Optional[str]]
    fp3_time: Mapped[Optional[str]]
    sprint_shootout_time: Mapped[Optional[str]]
    sprint_race_time: Mapped[Optional[str]]
    quali_time: Mapped[Optional[str]]
    race_time: Mapped[Optional[str]]
    
    previous_winner: Mapped[Optional[str]]
    most_driver_wins: Mapped[Optional[str]]
    most_constructor_wins: Mapped[Optional[str]]
    most_driver_podiums: Mapped[Optional[str]]
    most_poles: Mapped[Optional[str]]
    num_races_held: Mapped[int] = 0
    
    results = relationship("RaceResult", back_populates="race")
    updates = relationship("TechnicalUpdate", back_populates="race")

# NUOVE TABELLE PER IL CACHING DELLE CLASSIFICHE (Punto 1 brainstorming)
class DriverStandingCache(Base):
    __tablename__ = "driver_standings_cache"
    id = Column(Integer, primary_key=True, index=True)
    position = Column(Integer)
    driver_name = Column(String)
    constructor_name = Column(String)
    points = Column(Integer)
    wins = Column(Integer)
    last_updated = Column(Date)

class ConstructorStandingCache(Base):
    __tablename__ = "constructor_standings_cache"
    id = Column(Integer, primary_key=True, index=True)
    position = Column(Integer)
    constructor_name = Column(String)
    chassis_name = Column(String, nullable=True)
    points = Column(Integer)
    wins = Column(Integer)
    last_updated = Column(Date)

class RaceResult(Base):
    __tablename__ = "race_results"
    id = Column(Integer, primary_key=True, index=True)
    position = Column(Integer, nullable=False)
    points = Column(Float, nullable=False)
    time_str = Column(String, nullable=True)
    race_id = Column(Integer, ForeignKey("races.id"))
    driver_id = Column(Integer, ForeignKey("drivers.id"))
    race = relationship("Race", back_populates="results")
    driver = relationship("Driver", back_populates="race_results")

class TechnicalUpdate(Base):
    __tablename__ = "technical_updates"
    id = Column(Integer, primary_key=True, index=True)
    component = Column(String, nullable=False)
    description = Column(String, nullable=False)
    race_id = Column(Integer, ForeignKey("races.id"))
    team_id = Column(Integer, ForeignKey("teams.id"))
    race = relationship("Race", back_populates="updates")
    team = relationship("Team", back_populates="updates")

class DriverCareerStats(Base):
    __tablename__ = "driver_career_stats"

    driver_id = Column(String, primary_key=True, index=True)
    total_races = Column(Integer, default=0)
    wins = Column(Integer, default=0)
    podiums = Column(Integer, default=0)
    pole_positions = Column(Integer, default=0)
    wins_from_pole = Column(Integer, default=0)
    world_championships = Column(Integer, default=0)

    # Nuove Statistiche (Race)
    best_race_result = Column(String, default="N/A")
    best_championship_result = Column(String, default="N/A")
    best_grid_position = Column(String, default="N/A")
    fastest_laps = Column(Integer, default=0)
    dns_count = Column(Integer, default=0)
    dnf_count = Column(Integer, default=0)
    dsq_count = Column(Integer, default=0)

    # Nuove Statistiche (Sprint)
    sprint_starts = Column(Integer, default=0)
    sprint_wins = Column(Integer, default=0)
    sprint_top_3 = Column(Integer, default=0)
    best_sprint_result = Column(String, default="N/A")
    best_sprint_grid_position = Column(String, default="N/A")

    # Dettagli Personali
    place_of_birth = Column(String, default="N/A")
    date_of_birth = Column(String, default="N/A")
    first_gp = Column(String, default="N/A")
    first_win = Column(String, default="N/A")
    hat_tricks = Column(Integer, default=0)
    grand_slams = Column(Integer, default=0)

    last_updated = Column(DateTime, default=lambda: datetime.now(timezone.utc))

class ConstructorCareerStats(Base):
    __tablename__ = "constructor_career_stats"

    constructor_id = Column(String, primary_key=True, index=True) # es. "ferrari", "mercedes"
    total_races = Column(Integer, default=0)
    wins = Column(Integer, default=0)
    podiums = Column(Integer, default=0)
    driver_championships = Column(Integer, default=0)
    constructor_championships = Column(Integer, default=0)
    
    first_gp_year = Column(String, default="N/A")
    first_win = Column(String, default="N/A")
    
    pole_positions = Column(Integer, default=0)
    fastest_laps = Column(Integer, default=0)
    total_points = Column(Float, default=0.0)
    seasons_entered = Column(Integer, default=0)
    
    best_race_result = Column(String, default="N/A")
    best_championship_result = Column(String, default="N/A")
    
    # Dati BIO Scuderia
    power_unit = Column(String, default="N/A")
    team_principal = Column(String, default="N/A")
    base_location = Column(String, default="N/A")

    last_updated = Column(DateTime, default=lambda: datetime.now(timezone.utc))

class DriverSeasonStats(Base):
    __tablename__ = "driver_season_stats"
    
    driver_id = Column(String, primary_key=True, index=True)
    year = Column(Integer, primary_key=True, default=2026)
    
    total_races = Column(Integer, default=0)
    wins = Column(Integer, default=0)
    second_places = Column(Integer, default=0)
    podiums = Column(Integer, default=0)
    laps_led = Column(Integer, default=0)
    fastest_laps = Column(Integer, default=0)
    beat_teammate_race = Column(Integer, default=0)
    beat_teammate_quali = Column(Integer, default=0)
    pole_positions = Column(Integer, default=0)
    front_rows = Column(Integer, default=0)
    retirements = Column(Integer, default=0)
    q3_appearances = Column(Integer, default=0)
    q2_appearances = Column(Integer, default=0)
    q1_appearances = Column(Integer, default=0)
    
    sprint_starts = Column(Integer, default=0)
    sprint_wins = Column(Integer, default=0)
    sprint_top_3 = Column(Integer, default=0)
    sprint_points_finishes = Column(Integer, default=0)
    sprint_points = Column(Integer, default=0)
    beat_teammate_sprint = Column(Integer, default=0)
    sprint_quali_poles = Column(Integer, default=0)
    
    last_updated = Column(DateTime, default=lambda: datetime.now(timezone.utc))

class ConstructorSeasonStats(Base):
    __tablename__ = "constructor_season_stats"
    
    constructor_id = Column(String, primary_key=True, index=True)
    year = Column(Integer, primary_key=True, default=2026)
    
    total_races = Column(Integer, default=0)
    wins = Column(Integer, default=0)
    podiums = Column(Integer, default=0)
    fastest_laps = Column(Integer, default=0)
    pole_positions = Column(Integer, default=0)
    front_rows = Column(Integer, default=0)
    one_two_finishes = Column(Integer, default=0)
    double_dnfs = Column(Integer, default=0)
    
    last_updated = Column(DateTime, default=lambda: datetime.now(timezone.utc))
