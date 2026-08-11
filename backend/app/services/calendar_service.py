from datetime import datetime, timezone
from .external_api_service import ExternalApiService
from ..core.config import settings

class CalendarUnavailableError(RuntimeError):
    """Raised when the authoritative external calendar cannot be loaded."""


class CalendarService:
    def __init__(self):
        # Il calendario deve provenire dalla fonte esterna autorevole.
        # Non manteniamo più un calendario hardcoded: potrebbe diventare
        # errato dopo una modifica ufficiale a date, round o cancellazioni.
        api_races = ExternalApiService.get_calendar(year=settings.F1_SEASON)

        if not api_races:
            raise CalendarUnavailableError(
                "Calendario F1 non disponibile: Jolpica non ha restituito dati validi."
            )

        self.races = api_races

    def get_current_or_next_race(self):
        # Usiamo l'ora UTC attuale per essere indipendenti dal fuso locale del server
        now_utc = datetime.now(timezone.utc).date()
        
        # Troviamo la prima gara che non è ancora "passata" e non è cancellata.
        # Una gara è considerata "passata" dal Lunedì successivo alla data della gara.
        for race in self.races:
            if now_utc > race["date"]:
                continue  # Questa gara è finita, passiamo alla prossima

            # Se siamo qui, la gara è oggi o nel futuro.
            # Se non è cancellata, è la nostra "current or next race".
            if not race.get("cancelled", False):
                return race
                
        # Se la stagione è finita, restituiamo l'ultimo GP disputato e non cancellato
        valid_races = [r for r in self.races if not r.get("cancelled", False)]
        return valid_races[-1] if valid_races else self.races[-1]

    def get_full_calendar(self):
        now_utc = datetime.now(timezone.utc).date()
        calendar_data = []
        current_race = self.get_current_or_next_race()
        for race in self.races:
            is_cancelled = race.get("cancelled", False)
            if not is_cancelled and race == current_race:
                status = "current"
            elif race["date"] < now_utc:
                status = "past"
            else:
                status = "future"
                
            calendar_data.append({
                **race,
                "status": status,
                "is_clickable": (status == "past" or status == "current") and not is_cancelled,
                "cancelled": is_cancelled
            })
        return calendar_data
