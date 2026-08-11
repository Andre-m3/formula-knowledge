"""Aggregate all public API routers."""

from fastapi import APIRouter

from .auth import router as auth_router
from .content import router as content_router
from .raceweek import router as raceweek_router
from .results import router as results_router
from .standings import router as standings_router
from .stats import router as stats_router


router = APIRouter()

router.include_router(raceweek_router)
router.include_router(results_router)
router.include_router(standings_router)
router.include_router(content_router)
router.include_router(stats_router)
router.include_router(auth_router)
