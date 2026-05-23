import requests
import time
from datetime import datetime

class ExternalApiService:
    _cache = {}
    CACHE_TTL = 3600  # 1 ora di cache per i dati storici/classifiche

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
    def get_calendar(cls, year: int = 2026):
        cache_key = f"calendar_{year}"
        cached = cls._get_cached(cache_key)
        if cached: return cached
        
        url = f"https://api.jolpi.ca/ergast/f1/{year}/races.json?limit=100"
        try:
            response = requests.get(url, timeout=5)
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
    def get_schedule(cls, year: int = 2026):
        cache_key = f"schedule_{year}"
        cached = cls._get_cached(cache_key)
        if cached: return cached
        
        url = f"https://api.jolpi.ca/ergast/f1/{year}/races.json?limit=100"
        try:
            response = requests.get(url, timeout=5)
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
    def get_driver_standings(cls, year: int = 2026):
        cache_key = f"driver_standings_{year}"
        cached = cls._get_cached(cache_key)
        if cached:
            return cached

        # Utilizziamo Jolpica-F1, il successore moderno e open-source di Ergast
        url = f"https://api.jolpi.ca/ergast/f1/{year}/driverStandings.json"
        try:
            response = requests.get(url, timeout=5)
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
    def get_constructor_standings(cls, year: int = 2026):
        cache_key = f"constructor_standings_{year}"
        cached = cls._get_cached(cache_key)
        if cached:
            return cached

        url = f"https://api.jolpi.ca/ergast/f1/{year}/constructorStandings.json"
        try:
            response = requests.get(url, timeout=5)
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
    def get_session_results(cls, round_number: int, session_type: str, year: int = 2026):
        cache_key = f"session_results_{year}_{round_number}_{session_type}"
        cached = cls._get_cached(cache_key)
        if cached:
            return cached

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
            # sprint_shootout non e' sempre supportato in tutte le stagioni, gestiamo il placeholder
            return []

        try:
            response = requests.get(url, timeout=5)
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

                results.append({ # type: ignore
                    "position": int(item["position"]),
                    "driver": driver_name,
                    "team": item["Constructor"]["name"],
                    "points": points,
                    "time": time_str,
                    "q1": q1, "q2": q2, "q3": q3
                })

            cls._set_cache(cache_key, results)
            return results
        except Exception as e:
            print(f"Errore API esterna risultati: {e}")
            return []
