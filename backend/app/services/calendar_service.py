from datetime import datetime, date, timedelta

class CalendarService:
    def __init__(self):
        # Calendario F1 2026
        self.races = [
            {"name": "Australian Grand Prix", "country": "Australia", "city": "Melbourne", "lat": -37.8497, "lon": 144.968, "date": date(2026, 3, 1), "round": 1},
            {"name": "Chinese Grand Prix", "country": "China", "city": "Shanghai", "lat": 31.3389, "lon": 121.22, "date": date(2026, 3, 22), "round": 2},
            {"name": "Japanese Grand Prix", "country": "Japan", "city": "Suzuka", "lat": 34.8431, "lon": 136.541, "date": date(2026, 4, 5), "round": 3},
            {"name": "Bahrain Grand Prix", "country": "Bahrain", "city": "Sakhir", "lat": 26.0325, "lon": 50.5106, "date": date(2026, 4, 19), "round": 4},
            {"name": "Saudi Arabian Grand Prix", "country": "Saudi Arabia", "city": "Jeddah", "lat": 21.6319, "lon": 39.1044, "date": date(2026, 5, 3), "round": 5},
            {"name": "Miami Grand Prix", "country": "USA", "city": "Miami", "lat": 25.9581, "lon": -80.2389, "date": date(2026, 5, 17), "round": 6},
            {"name": "Emilia Romagna Grand Prix", "country": "Italy", "city": "Imola", "lat": 44.3439, "lon": 11.7167, "date": date(2026, 5, 31), "round": 7},
            {"name": "Monaco Grand Prix", "country": "Monaco", "city": "Monte Carlo", "lat": 43.7347, "lon": 7.4206, "date": date(2026, 6, 7), "round": 8},
            {"name": "Spanish Grand Prix", "country": "Spain", "city": "Barcelona", "lat": 41.57, "lon": 2.2611, "date": date(2026, 6, 21), "round": 9},
            {"name": "Canadian Grand Prix", "country": "Canada", "city": "Montreal", "lat": 45.5005, "lon": -73.5225, "date": date(2026, 7, 5), "round": 10},
            {"name": "Austrian Grand Prix", "country": "Austria", "city": "Spielberg", "lat": 47.2197, "lon": 14.7647, "date": date(2026, 7, 19), "round": 11},
            {"name": "British Grand Prix", "country": "UK", "city": "Silverstone", "lat": 52.0786, "lon": -1.0169, "date": date(2026, 8, 2), "round": 12},
            {"name": "Belgian Grand Prix", "country": "Belgium", "city": "Spa", "lat": 50.4372, "lon": 5.9714, "date": date(2026, 8, 30), "round": 13},
            {"name": "Dutch Grand Prix", "country": "Netherlands", "city": "Zandvoort", "lat": 52.3888, "lon": 4.5409, "date": date(2026, 9, 6), "round": 14},
            {"name": "Italian Grand Prix", "country": "Italy", "city": "Monza", "lat": 45.6156, "lon": 9.2811, "date": date(2026, 9, 20), "round": 15},
            {"name": "Azerbaijan Grand Prix", "country": "Azerbaijan", "city": "Baku", "lat": 40.3725, "lon": 49.8533, "date": date(2026, 10, 4), "round": 16},
            {"name": "Singapore Grand Prix", "country": "Singapore", "city": "Marina Bay", "lat": 1.2914, "lon": 103.864, "date": date(2026, 10, 18), "round": 17},
            {"name": "United States Grand Prix", "country": "USA", "city": "Austin", "lat": 30.1328, "lon": -97.6411, "date": date(2026, 11, 1), "round": 18},
            {"name": "Mexico City Grand Prix", "country": "Mexico", "city": "Mexico City", "lat": 19.4042, "lon": -99.0907, "date": date(2026, 11, 8), "round": 19},
            {"name": "São Paulo Grand Prix", "country": "Brazil", "city": "Interlagos", "lat": -23.7036, "lon": -46.6997, "date": date(2026, 11, 22), "round": 20},
            {"name": "Las Vegas Grand Prix", "country": "USA", "city": "Las Vegas", "lat": 36.1147, "lon": -115.173, "date": date(2026, 12, 5), "round": 21},
            {"name": "Qatar Grand Prix", "country": "Qatar", "city": "Lusail", "lat": 25.49, "lon": 51.4542, "date": date(2026, 12, 13), "round": 22},
            {"name": "Abu Dhabi Grand Prix", "country": "UAE", "city": "Yas Marina", "lat": 24.4672, "lon": 54.6031, "date": date(2026, 12, 20), "round": 23},
        ]

    def get_current_or_next_race(self):
        today = date.today()
        # Logica: Se oggi è Lunedì (0), Martedì (1), etc...
        # Consideriamo "Race Week" a partire dal Lunedì dopo il GP precedente.
        # In pratica, cerchiamo la prima gara che NON è ancora passata di oltre 1 giorno (per gestire la domenica sera).
        for race in self.races:
            # Se la gara è oggi o nel futuro, oppure è finita da meno di un giorno (Domenica notte)
            if race["date"] >= today:
                return race
        return self.races[-1]

    def get_full_calendar(self):
        today = date.today()
        calendar_data = []
        current_race = self.get_current_or_next_race()
        for race in self.races:
            if race == current_race:
                status = "current"
            elif race["date"] < today:
                status = "past"
            else:
                status = "future"
                
            calendar_data.append({
                **race,
                "status": status,
                "is_clickable": status == "past" or status == "current"
            })
        return calendar_data
