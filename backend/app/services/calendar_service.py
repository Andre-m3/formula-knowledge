from datetime import datetime, date, timedelta, timezone

class CalendarService:
    def __init__(self):
        # Calendario F1 2026 con nomi circuiti e stato cancellazione
        self.races = [
            {"name": "Australian Grand Prix", "country": "Australia", "city": "Melbourne", "circuit_name": "Albert Park Circuit", "lat": -37.8497, "lon": 144.968, "date": date(2026, 3, 1), "round": 1},
            {"name": "Chinese Grand Prix", "country": "China", "city": "Shanghai", "circuit_name": "Shanghai International Circuit", "lat": 31.3389, "lon": 121.22, "date": date(2026, 3, 22), "round": 2},
            {"name": "Japanese Grand Prix", "country": "Japan", "city": "Suzuka", "circuit_name": "Suzuka International Racing Course", "lat": 34.8431, "lon": 136.541, "date": date(2026, 3, 29), "round": 3},
            {"name": "Bahrain Grand Prix", "country": "Bahrain", "city": "Sakhir", "circuit_name": "Bahrain International Circuit", "lat": 26.0325, "lon": 50.5106, "date": date(2026, 4, 19), "round": 4, "cancelled": True},
            {"name": "Saudi Arabian Grand Prix", "country": "Saudi Arabia", "city": "Jeddah", "circuit_name": "Jeddah Corniche Circuit", "lat": 21.6319, "lon": 39.1044, "date": date(2026, 5, 3), "round": 5, "cancelled": True},
            {"name": "Miami Grand Prix", "country": "USA", "city": "Miami", "circuit_name": "Miami International Autodrome", "lat": 25.9581, "lon": -80.2389, "date": date(2026, 5, 17), "round": 6},
            {"name": "Imola Grand Prix", "country": "Italy", "city": "Imola", "circuit_name": "Autodromo Enzo e Dino Ferrari", "lat": 44.3439, "lon": 11.7167, "date": date(2026, 5, 31), "round": 7},
            {"name": "Monaco Grand Prix", "country": "Monaco", "city": "Monte Carlo", "circuit_name": "Circuit de Monaco", "lat": 43.7347, "lon": 7.4206, "date": date(2026, 6, 7), "round": 8},
            {"name": "Spanish Grand Prix", "country": "Spain", "city": "Barcelona", "circuit_name": "Circuit de Barcelona-Catalunya", "lat": 41.57, "lon": 2.2611, "date": date(2026, 6, 21), "round": 9},
            {"name": "Canadian Grand Prix", "country": "Canada", "city": "Montreal", "circuit_name": "Circuit Gilles Villeneuve", "lat": 45.5005, "lon": -73.5225, "date": date(2026, 7, 5), "round": 10},
            {"name": "Austrian Grand Prix", "country": "Austria", "city": "Spielberg", "circuit_name": "Red Bull Ring", "lat": 47.2197, "lon": 14.7647, "date": date(2026, 7, 19), "round": 11},
            {"name": "British Grand Prix", "country": "UK", "city": "Silverstone", "circuit_name": "Silverstone Circuit", "lat": 52.0786, "lon": -1.0169, "date": date(2026, 8, 2), "round": 12},
            {"name": "Belgian Grand Prix", "country": "Belgium", "city": "Spa", "circuit_name": "Circuit de Spa-Francorchamps", "lat": 50.4372, "lon": 5.9714, "date": date(2026, 8, 30), "round": 13},
            {"name": "Dutch Grand Prix", "country": "Netherlands", "city": "Zandvoort", "circuit_name": "Circuit Zandvoort", "lat": 52.3888, "lon": 4.5409, "date": date(2026, 9, 6), "round": 14},
            {"name": "Italian Grand Prix", "country": "Italy", "city": "Monza", "circuit_name": "Autodromo Nazionale Monza", "lat": 45.6156, "lon": 9.2811, "date": date(2026, 9, 20), "round": 15},
            {"name": "Azerbaijan Grand Prix", "country": "Azerbaijan", "city": "Baku", "circuit_name": "Baku City Circuit", "lat": 40.3725, "lon": 49.8533, "date": date(2026, 10, 4), "round": 16},
            {"name": "Singapore Grand Prix", "country": "Singapore", "city": "Marina Bay", "circuit_name": "Marina Bay Street Circuit", "lat": 1.2914, "lon": 103.864, "date": date(2026, 10, 18), "round": 17},
            {"name": "United States Grand Prix", "country": "USA", "city": "Austin", "circuit_name": "Circuit of the Americas", "lat": 30.1328, "lon": -97.6411, "date": date(2026, 11, 1), "round": 18},
            {"name": "Mexico City Grand Prix", "country": "Mexico", "city": "Mexico City", "circuit_name": "Autódromo Hermanos Rodríguez", "lat": 19.4042, "lon": -99.0907, "date": date(2026, 11, 8), "round": 19},
            {"name": "São Paulo Grand Prix", "country": "Brazil", "city": "Interlagos", "circuit_name": "Autódromo José Carlos Pace", "lat": -23.7036, "lon": -46.6997, "date": date(2026, 11, 22), "round": 20},
            {"name": "Las Vegas Grand Prix", "country": "USA", "city": "Las Vegas", "circuit_name": "Las Vegas Strip Circuit", "lat": 36.1147, "lon": -115.173, "date": date(2026, 12, 5), "round": 21},
            {"name": "Qatar Grand Prix", "country": "Qatar", "city": "Lusail", "circuit_name": "Lusail International Circuit", "lat": 25.49, "lon": 51.4542, "date": date(2026, 12, 13), "round": 22},
            {"name": "Abu Dhabi Grand Prix", "country": "UAE", "city": "Yas Marina", "circuit_name": "Yas Marina Circuit", "lat": 24.4672, "lon": 54.6031, "date": date(2026, 12, 20), "round": 23},
        ]

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
