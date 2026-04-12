import uuid
from datetime import datetime, timezone
from sqlalchemy import Column, String, Boolean, ForeignKey, DateTime
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import relationship
from app.models.database import Base

# Usiamo stringhe per gli ID in SQLite (che simuleranno gli UUID)
def generate_uuid():
    return str(uuid.uuid4())

class Team(Base):
    __tablename__ = "teams"

    id = Column(String, primary_key=True, default=generate_uuid, index=True)
    name = Column(String, unique=True, index=True, nullable=False)
    color_hex = Column(String, nullable=False)

    # Relazioni
    users = relationship("User", back_populates="favorite_team")

class User(Base):
    __tablename__ = "users"

    id = Column(String, primary_key=True, default=generate_uuid, index=True)
    device_token = Column(String, unique=True, index=True, nullable=False) # Per notifiche FCM
    favorite_team_id = Column(String, ForeignKey("teams.id"), nullable=True)
    notify_fp1 = Column(Boolean, default=False)
    notify_race = Column(Boolean, default=True)

    # Relazioni
    favorite_team = relationship("Team", back_populates="users")

class FiaReport(Base):
    __tablename__ = "fia_reports"

    id = Column(String, primary_key=True, default=generate_uuid, index=True)
    race_id = Column(String, index=True, nullable=False) # Es. "monza_2024"
    team_id = Column(String, ForeignKey("teams.id"), nullable=False)
    translated_text = Column(String, nullable=False)
    created_at = Column(DateTime, default=lambda: datetime.now(timezone.utc))