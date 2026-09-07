import requests
import time
from datetime import datetime
from ..core.config import settings

class ExternalApiRateLimitError(RuntimeError):
    """Raised internally when Jolpica throttles a provider request."""

class ExternalApiService:
    _cache = {}
    CACHE_TTL = 3600  # 1 ora di cache per i dati storici/classifiche
    REQUEST_HEADERS = {
        "User-Agent": "FormulaKnowledge/0.1 (private educational Android application)",
    }

    @classmethod
    def _request(cls, url: str):
        response = requests.get(url, timeout=5, headers=cls.REQUEST_HEADERS)
        if getattr(response, "status_code", None) == 429:
            raise ExternalApiRateLimitError(f"Jolpica rate limit reached for {url}")
        return response

    @classmethod
    def _get_cached(cls, key):
        if key in cls._cache:
            data, timestamp = cls._cache[key]
            if time.time() - timestamp < cls.CACHE_TTL:
                return data
        return None

    @classmethod
    def _set_cache(cls, key, data):
        cls._cache[key] = (data, time.time())

    @classmethod
    def _standardize_team_name(cls, name: str) -> str:
        lower_name = name.lower()
        if "racing bulls" in lower_name or "rb" in lower_name or "alphatauri" in lower_name:
            return "Racing Bulls"
        return name

    @classmethod
    def get_calendar(cls, year: int = settings.F1_SEASON):
        cache_key = f"calendar_{year}"
        cached = cls._get_cached(cache_key)
        if cached: return cached
        
        url = f"https://api.jolpi.ca/ergast/f1/{year}/races.json?limit=100"
        try:
            response = cls._request(url)
            response.raise_for_status()
            races = response.json().get("MRData", {}).get("RaceTable", {}).get("Races", [])
            
            calendar = []
            for race in races:
                calendar.append({
                    "round": int(race["round"]),
                    "name": race["raceName"],
                    "country": race.get("Circuit", {}).get("Location", {}).get("country", ""),
                    "city": race.get("Circuit", {}).get("Location", {}).get("locality", ""),
                    "circuit_name": race.get("Circuit", {}).get("circuitName", ""),
                    "lat": float(race.get("Circuit", {}).get("Location", {}).get("lat", 0.0)),
                    "lon": float(race.get("Circuit", {}).get("Location", {}).get("long", 0.0)),
                    "date": datetime.strptime(race["date"], "%Y-%m-%d").date(),
                    "cancelled": False
                })
            
            if calendar:
                cls._set_cache(cache_key, calendar)
            return calendar
        except Exception as e:
            print(f"Errore API Calendar Jolpica: {e}")
            return []

    @classmethod
    def get_schedule(cls, year: int = settings.F1_SEASON):
        cache_key = f"schedule_{year}"
        cached = cls._get_cached(cache_key)
        if cached: return cached
        
        url = f"https://api.jolpi.ca/ergast/f1/{year}/races.json?limit=100"
        try:
            response = cls._request(url)
            response.raise_for_status()
            data = response.json()
            races = data.get("MRData", {}).get("RaceTable", {}).get("Races", [])
            
            schedule = {}
            for race in races:
                round_num = int(race["round"])
                
                def extract_time(session_data):
                    if not session_data: return None
                    t = session_data.get("time", "")
                    return t[:5] if t else None

                is_sprint_api = "Sprint" in race or "SprintQualifying" in race
                
                sessions = {
                    "fp1": extract_time(race.get("FirstPractice")),
                    "fp2": extract_time(race.get("SecondPractice")),
                    "fp3": extract_time(race.get("ThirdPractice")),
                    "sprint_shootout": extract_time(race.get("SprintQualifying")),
                    "sprint_race": extract_time(race.get("Sprint")),
                    "quali": extract_time(race.get("Qualifying")),
                    "race": extract_time(race),
                    "is_sprint_jolpica": is_sprint_api
                }
                schedule[round_num] = sessions
            cls._set_cache(cache_key, schedule)
            return schedule
        except Exception as e:
            print(f"Errore API Schedule Jolpica: {e}")
            return {}

    @classmethod
    def get_driver_standings(cls, year: int = settings.F1_SEASON):
        cache_key = f"driver_standings_{year}"
        cached = cls._get_cached(cache_key)
        if cached:
            return cached

        # Utilizziamo Jolpica-F1, il successore moderno e open-source di Ergast
        url = f"https://api.jolpi.ca/ergast/f1/{year}/driverStandings.json"
        try:
            response = cls._request(url)
            response.raise_for_status()
            data = response.json()

            standings_list = data.get("MRData", {}).get("StandingsTable", {}).get("StandingsLists", [])
            if not standings_list:
                return []

            driver_standings = standings_list[0].get("DriverStandings", [])

            results = []
            for item in driver_standings:
                # Pulizia nome Kimi Antonelli
                driver_name = f"{item['Driver']['givenName']} {item['Driver']['familyName']}"
                if driver_name == "Andrea Kimi Antonelli":
                    driver_name = "Kimi Antonelli"

                results.append({ # type: ignore
                    "position": int(item["position"]),
                    "driver_name": driver_name,
                    "constructor_name": item["Constructors"][0]["name"],
                    "points": int(float(item["points"])),
                    "wins": int(item["wins"])
                })

            cls._set_cache(cache_key, results)
            return results
        except Exception as e:
            print(f"Errore durante il recupero API esterna piloti: {e}")
            return []

    @classmethod
    def get_constructor_standings(cls, year: int = settings.F1_SEASON):
        cache_key = f"constructor_standings_{year}"
        cached = cls._get_cached(cache_key)
        if cached:
            return cached

        url = f"https://api.jolpi.ca/ergast/f1/{year}/constructorStandings.json"
        try:
            response = cls._request(url)
            response.raise_for_status()
            data = response.json()

            standings_list = data.get("MRData", {}).get("StandingsTable", {}).get("StandingsLists", [])
            if not standings_list:
                return []

            constructor_standings = standings_list[0].get("ConstructorStandings", [])

            results = []
            for item in constructor_standings: # type: ignore
                results.append({
                    "position": int(item["position"]),
                    "constructor_name": item["Constructor"]["name"],
                    "points": int(float(item["points"])),
                    "wins": int(item["wins"])
                })

            cls._set_cache(cache_key, results)
            return results
        except Exception as e:
            print(f"Errore durante il recupero API esterna costruttori: {e}")
            return []

    @classmethod
    def _get_alpha_timing_results(
        cls,
        round_number: int,
        session_type: str,
        year: int,
    ):
        """Return FP or Sprint Qualifying results from Jolpica Alpha.

        Alpha identifies rounds through opaque IDs, so the seasonal schedule is
        the source of truth for each session result URL. This also keeps the
        implementation resilient to provider-side round ID changes.
        """
        schedule_cache_key = f"alpha_schedule_{year}"
        schedule_data = cls._get_cached(schedule_cache_key)

        try:
            if not schedule_data:
                schedule_response = cls._request(
                    f"https://api.jolpi.ca/f1/alpha/schedules/{year}/"
                )
                schedule_response.raise_for_status()
                schedule_data = schedule_response.json()
                cls._set_cache(schedule_cache_key, schedule_data)

            events = schedule_data.get("data", {}).get("events", [])
            event = next(
                (
                    item
                    for item in events
                    if item.get("round", {}).get("number") == round_number
                ),
                None,
            )
            if not event:
                return []

            session_code = {"sprint_shootout": "SQ"}.get(
                session_type,
                session_type.upper(),
            )
            schedule_entry = next(
                (
                    item
                    for item in event.get("schedule", [])
                    if item.get("code") == session_code
                ),
                None,
            )
            results_url = schedule_entry.get("results_url") if schedule_entry else None
            if not results_url:
                return []

            response = cls._request(results_url)
            response.raise_for_status()
            raw_results = response.json().get("data", {}).get("results", [])

            results = []
            for item in raw_results:
                driver = item.get("driver", {})
                team = item.get("team", {})
                position = item.get("position")

                if not isinstance(position, int) or not driver:
                    continue

                driver_name = " ".join(
                    filter(
                        None,
                        [driver.get("given_name"), driver.get("family_name")],
                    )
                )
                if driver_name == "Andrea Kimi Antonelli":
                    driver_name = "Kimi Antonelli"

                components = item.get("components", {})
                if not isinstance(components, dict):
                    components = {}

                def component_time(code: str):
                    component = components.get(code, {})
                    return component.get("time") if isinstance(component, dict) else None

                is_sprint_shootout = session_type == "sprint_shootout"
                results.append(
                    {
                        "position": position,
                        "driver": driver_name,
                        "team": team.get("name", ""),
                        "points": 0,
                        "time": item.get("time") or item.get("position_text") or "",
                        "q1": component_time("SQ1") if is_sprint_shootout else None,
                        "q2": component_time("SQ2") if is_sprint_shootout else None,
                        "q3": component_time("SQ3") if is_sprint_shootout else None,
                    }
                )

            # Never cache an empty result set: Alpha can publish it shortly
            # after a session ends, and callers must be able to retry.
            if results:
                cls._set_cache(
                    f"session_results_{year}_{round_number}_{session_type}",
                    results,
                )
            return results
        except ExternalApiRateLimitError:
            raise
        except Exception as e:
            print(f"Errore API Alpha risultati {session_type}: {e}")
            return []
    @classmethod
    def get_alpha_session_laps(
        cls,
        round_number: int,
        session_type: str,
        year: int = settings.F1_SEASON,
        *,
        force_refresh: bool = False,
    ) -> dict | None:
        """Return the raw Alpha lap payload for one completed session.

        The Alpha API uses opaque round identifiers. The seasonal schedule is
        therefore resolved first and its laps_url is treated as authoritative.
        Callers persist the returned payload; this method deliberately performs
        no database writes and returns None on unavailable/invalid responses.
        """
        cache_key = f"alpha_laps_{year}_{round_number}_{session_type}"
        cached = None if force_refresh else cls._get_cached(cache_key)
        if cached is not None:
            return cached

        schedule_cache_key = f"alpha_schedule_{year}"
        schedule_data = None if force_refresh else cls._get_cached(schedule_cache_key)

        try:
            if not schedule_data:
                schedule_response = cls._request(
                    f"https://api.jolpi.ca/f1/alpha/schedules/{year}/"
                )
                schedule_response.raise_for_status()
                schedule_data = schedule_response.json()
                cls._set_cache(schedule_cache_key, schedule_data)

            events = schedule_data.get("data", {}).get("events", [])
            event = next(
                (
                    item
                    for item in events
                    if item.get("round", {}).get("number") == round_number
                ),
                None,
            )
            if not event:
                return None

            session_code = {
                "race": "R",
                "quali": "Q",
                "sprint_shootout": "SQ",
            }.get(session_type)
            if not session_code:
                return None

            schedule_entry = next(
                (
                    item
                    for item in event.get("schedule", [])
                    if item.get("code") == session_code
                ),
                None,
            )
            laps_url = schedule_entry.get("laps_url") if schedule_entry else None
            if not laps_url:
                return None

            response = cls._request(laps_url)
            response.raise_for_status()
            payload = response.json().get("data")
            if not isinstance(payload, dict) or not isinstance(payload.get("laps"), list):
                return None
            # Do not cache incomplete/empty timings: Alpha can expose the
            # endpoint before FIA-derived lap data has been published.
            if not payload["laps"]:
                return None

            normalized_payload = {
                "session_code": session_code,
                "laps": payload["laps"],
                "drivers_by_id": payload.get("drivers_by_id", {}),
                "teams_by_id": payload.get("teams_by_id", {}),
                "sessions_by_id": payload.get("sessions_by_id", {}),
            }
            cls._set_cache(cache_key, normalized_payload)
            return normalized_payload
        except ExternalApiRateLimitError:
            raise
        except Exception as exc:
            print(f"Errore API Alpha giri {session_type}: {exc}")
            return None
    @classmethod
    def get_session_results(
        cls,
        round_number: int,
        session_type: str,
        year: int = settings.F1_SEASON,
        *,
        force_refresh: bool = False,
    ):
        cache_key = f"session_results_{year}_{round_number}_{session_type}"
        cached = None if force_refresh else cls._get_cached(cache_key)
        if cached:
            return cached

        if session_type in {"fp1", "fp2", "fp3", "sprint_shootout"}:
            return cls._get_alpha_timing_results(round_number, session_type, year)

        if session_type == "race":
            url = f"https://api.jolpi.ca/ergast/f1/{year}/{round_number}/results.json"
            result_key = "Results"
        elif session_type == "sprint":
            url = f"https://api.jolpi.ca/ergast/f1/{year}/{round_number}/sprint.json"
            result_key = "SprintResults"
        elif session_type == "quali":
            url = f"https://api.jolpi.ca/ergast/f1/{year}/{round_number}/qualifying.json"
            result_key = "QualifyingResults"
        else:
            return []

        try:
            response = cls._request(url)
            response.raise_for_status()
            data = response.json()

            race_table = data.get("MRData", {}).get("RaceTable", {}).get("Races", [])
            if not race_table:
                return []

            results_data = race_table[0].get(result_key, [])

            results = []
            for item in results_data:
                # Pulizia nome Kimi Antonelli
                driver_name = f"{item['Driver']['givenName']} {item['Driver']['familyName']}"
                if driver_name == "Andrea Kimi Antonelli":
                    driver_name = "Kimi Antonelli"

                if session_type in ["race", "sprint"]:
                    time_obj = item.get("Time", {})
                    time_str = time_obj.get("time", item.get("status", ""))
                    points = int(float(item.get("points", 0)))
                    q1 = q2 = q3 = None
                else:
                    time_str = item.get("Q3", item.get("Q2", item.get("Q1", "")))
                    q1 = item.get("Q1")
                    q2 = item.get("Q2")
                    q3 = item.get("Q3")
                    points = 0

                results.append({
                    "position": int(item["position"]),
                    "driver": driver_name,
                    "team": item["Constructor"]["name"],
                    "points": points,
                    "time": time_str,
                    "q1": q1,
                    "q2": q2,
                    "q3": q3,
                })

            cls._set_cache(cache_key, results)
            return results
        except ExternalApiRateLimitError:
            raise
        except Exception as e:
            print(f"Errore API esterna risultati: {e}")
            return []