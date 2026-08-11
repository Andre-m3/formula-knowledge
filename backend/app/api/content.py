from typing import List

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from .. import database, models
from ..services.calendar_service import CalendarService, CalendarUnavailableError
from ..services.fia_scraper import FiaScraperService
from .dependencies import get_api_key
from .schemas import NewsArticleResponseSchema, TeamUpdatesResponse, TeamUpdatesWrapperSchema


router = APIRouter()


@router.get("/api/v1/raceweek/updates", response_model=TeamUpdatesWrapperSchema, dependencies=[Depends(get_api_key)])
def get_latest_car_updates(db: Session = Depends(database.get_db)):
    try:
        calendar = CalendarService()
    except CalendarUnavailableError as exc:
        raise HTTPException(status_code=503, detail=str(exc))
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


@router.get("/api/v1/news", response_model=List[NewsArticleResponseSchema], dependencies=[Depends(get_api_key)])
def get_latest_news(db: Session = Depends(database.get_db)):
    articles = db.query(models.NewsArticle).order_by(models.NewsArticle.published_at.desc()).limit(20).all()
    return [
        {
            "id": a.id,
            "title": a.title,
            "source": a.source,
            "url": a.url,
            "image_url": a.image_url,
            "published_at": a.published_at
        } for a in articles
    ]
