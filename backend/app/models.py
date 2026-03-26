from sqlalchemy import Column, Integer, String, Float, Date, ForeignKey
from sqlalchemy.orm import relationship
from .database import Base

class Team(Base):
    __tablename__ = "teams"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, unique=True, index=True, nullable=False)
    color_hex = Column(String, nullable=False) # Es. "#E32219"
    power_unit = Column(String, nullable=True)

    # Relazioni (Cosa possiede un team?)
    drivers = relationship("Driver", back_populates="team")
    updates = relationship("TechnicalUpdate", back_populates="team")

class Driver(Base):
    __tablename__ = "drivers"

    id = Column(Integer, primary_key=True, index=True)
    first_name = Column(String, nullable=False)
    last_name = Column(String, nullable=False)
    number = Column(Integer, unique=True, nullable=False) # Il numero dell'auto
    nationality = Column(String, nullable=True)
    
    team_id = Column(Integer, ForeignKey("teams.id"))

    # Relazioni
    team = relationship("Team", back_populates="drivers")
    race_results = relationship("RaceResult", back_populates="driver")

class Race(Base):
    __tablename__ = "races"

    id = Column(Integer, primary_key=True, index=True)
    round_number = Column(Integer, unique=True, nullable=False)
    name = Column(String, nullable=False) # Es. "Italian Grand Prix"
    date = Column(Date, nullable=False)
    country = Column(String, nullable=False)
    city = Column(String, nullable=False)
    
    # Relazioni
    results = relationship("RaceResult", back_populates="race")
    updates = relationship("TechnicalUpdate", back_populates="race")

class RaceResult(Base):
    __tablename__ = "race_results"

    id = Column(Integer, primary_key=True, index=True)
    position = Column(Integer, nullable=False)
    points = Column(Float, nullable=False) # Float perché storicamente ci sono state gare a metà punteggio (es. Spa 2021)
    time_str = Column(String, nullable=True) # Es. "1:32:41.432" o "+2.431s"
    
    race_id = Column(Integer, ForeignKey("races.id"))
    driver_id = Column(Integer, ForeignKey("drivers.id"))

    # Relazioni
    race = relationship("Race", back_populates="results")
    driver = relationship("Driver", back_populates="race_results")

class TechnicalUpdate(Base):
    __tablename__ = "technical_updates"

    id = Column(Integer, primary_key=True, index=True)
    component = Column(String, nullable=False) # Es. "Front Wing", "Floor"
    description = Column(String, nullable=False) # Spiegazione dell'AI
    
    race_id = Column(Integer, ForeignKey("races.id"))
    team_id = Column(Integer, ForeignKey("teams.id"))

    # Relazioni
    race = relationship("Race", back_populates="updates")
    team = relationship("Team", back_populates="updates")