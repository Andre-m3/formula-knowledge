import requests

class ResultsService:
    def __init__(self):
        # In futuro useremo OpenF1 o Ergast. Per ora simuliamo i dati.
        self.base_url = "https://api.openf1.org/v1" # Esempio

    def get_race_results(self, round_number: int):
        # MOCK DATA per i risultati di un GP
        # Nella realtà qui faremmo una richiesta API o leggeremmo dal DB
        if round_number == 1: # Bahrain (simulato per 2026)
            return [
                {"position": 1, "driver": "Charles Leclerc", "team": "Ferrari", "points": 25, "time": "1:32:41.432"},
                {"position": 2, "driver": "Lewis Hamilton", "team": "Ferrari", "points": 18, "time": "+2.431s"},
                {"position": 3, "driver": "Lando Norris", "team": "McLaren", "points": 15, "time": "+5.672s"},
                {"position": 4, "driver": "Max Verstappen", "team": "Red Bull", "points": 12, "time": "+10.123s"},
                {"position": 5, "driver": "George Russell", "team": "Mercedes", "points": 10, "time": "+15.432s"},
            ]
        elif round_number == 2: # Saudi Arabia
            return [
                {"position": 1, "driver": "Max Verstappen", "team": "Red Bull", "points": 25, "time": "1:25:32.123"},
                {"position": 2, "driver": "Sergio Perez", "team": "Red Bull", "points": 18, "time": "+5.123s"},
                {"position": 3, "driver": "Charles Leclerc", "team": "Ferrari", "points": 15, "time": "+7.890s"},
            ]
        return []
