from sqlalchemy import Column, Integer, String, Float, Date, ForeignKey, Boolean
from sqlalchemy.orm import relationship
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
    lap_record = Column(String, nullable=True)
    is_sprint = Column(Boolean, default=False)
    cancelled = Column(Boolean, default=False)
    
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
