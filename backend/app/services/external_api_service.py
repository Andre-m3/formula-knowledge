import requests
import time

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
                    
                results.append({
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
            for item in constructor_standings:
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
    def get_race_results(cls, round_number: int, year: int = 2026):
        cache_key = f"race_results_{year}_{round_number}"
        cached = cls._get_cached(cache_key)
        if cached:
            return cached

        url = f"https://api.jolpi.ca/ergast/f1/{year}/{round_number}/results.json"
        try:
            response = requests.get(url, timeout=5)
            response.raise_for_status()
            data = response.json()
            
            race_table = data.get("MRData", {}).get("RaceTable", {}).get("Races", [])
            if not race_table:
                return []
                
            results_data = race_table[0].get("Results", [])
            
            results = []
            for item in results_data:
                # Gestione sicura del tempo (chi si ritira non ha un tempo)
                time_obj = item.get("Time", {})
                time_str = time_obj.get("time", item.get("status", ""))
                
                # Pulizia nome Kimi Antonelli
                driver_name = f"{item['Driver']['givenName']} {item['Driver']['familyName']}"
                if driver_name == "Andrea Kimi Antonelli":
                    driver_name = "Kimi Antonelli"
                
                results.append({
                    "position": int(item["position"]),
                    "driver": driver_name,
                    "team": item["Constructor"]["name"],
                    "points": int(float(item["points"])),
                    "time": time_str
                })
            
            cls._set_cache(cache_key, results)
            return results
        except Exception as e:
            print(f"Errore API esterna risultati: {e}")
            return []
