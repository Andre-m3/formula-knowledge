import requests

class ResultsService:
    def __init__(self):
        # In futuro useremo OpenF1 o Ergast. Per ora simuliamo i dati.
        self.base_url = "https://api.openf1.org/v1"

    def get_race_results(self, round_number: int):
        if round_number == 1:
            return [
                {"position": 1, "driver": "Charles Leclerc", "team": "Ferrari", "points": 25, "time": "1:32:41.432"},
                {"position": 2, "driver": "Lewis Hamilton", "team": "Ferrari", "points": 18, "time": "+2.431s"},
                {"position": 3, "driver": "Lando Norris", "team": "McLaren", "points": 15, "time": "+5.672s"},
                {"position": 4, "driver": "Max Verstappen", "team": "Red Bull", "points": 12, "time": "+10.123s"},
                {"position": 5, "driver": "George Russell", "team": "Mercedes", "points": 10, "time": "+15.432s"},
            ]
        return []

    def get_driver_standings(self):
        return [
            {"position": 1, "driver_name": "Charles Leclerc", "constructor_name": "Ferrari", "points": 40, "wins": 1},
            {"position": 2, "driver_name": "Max Verstappen", "constructor_name": "Red Bull", "points": 37, "wins": 1},
            {"position": 3, "driver_name": "Lewis Hamilton", "constructor_name": "Ferrari", "points": 33, "wins": 0},
            {"position": 4, "driver_name": "Lando Norris", "constructor_name": "McLaren", "points": 27, "wins": 0},
            {"position": 5, "driver_name": "George Russell", "constructor_name": "Mercedes", "points": 20, "wins": 0},
        ]

    def get_constructor_standings(self):
        return [
            {"position": 1, "constructor_name": "Ferrari", "points": 73, "wins": 1},
            {"position": 2, "constructor_name": "Red Bull", "points": 45, "wins": 1},
            {"position": 3, "constructor_name": "McLaren", "points": 38, "wins": 0},
            {"position": 4, "constructor_name": "Mercedes", "points": 30, "wins": 0},
            {"position": 5, "constructor_name": "Aston Martin", "points": 12, "wins": 0},
        ]
