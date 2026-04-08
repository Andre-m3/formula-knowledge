from sqlalchemy.orm import Session
from .database import SessionLocal, engine
from .models import Team, Driver, Race, RaceResult, Base
from .services.calendar_service import CalendarService
from .services.external_api_service import ExternalApiService
from datetime import date

# Dati per il seeding (Stagione 2026 - Proiezione)
TEAMS_DATA = [
    {"name": "Mercedes-AMG PETRONAS F1 Team", "color_hex": "#00D2BE", "power_unit": "Mercedes", "chassis_name": "W17"},
    {"name": "Oracle Red Bull Racing", "color_hex": "#1E41FF", "power_unit": "RedBull-Ford Powertrains", "chassis_name": "RB22"},
    {"name": "Scuderia Ferrari HP", "color_hex": "#E32219", "power_unit": "Ferrari", "chassis_name": "SF-26"},
    {"name": "McLaren Mastercard F1 Team", "color_hex": "#FF8000", "power_unit": "Mercedes", "chassis_name": "MCL40"},
    {"name": "Aston Martin Aramco F1 Team", "color_hex": "#006F62", "power_unit": "Honda", "chassis_name": "AMR26"},
    {"name": "BWT Alpine F1 Team", "color_hex": "#0090FF", "power_unit": "Mercedes", "chassis_name": "A526"},
    {"name": "Atlassian Williams F1 Team", "color_hex": "#005AFF", "power_unit": "Mercedes", "chassis_name": "FW48"},
    {"name": "Visa Cash App Racing Bulls", "color_hex": "#00359F", "power_unit": "RedBull-Ford Powertrains", "chassis_name": "VCARB 03"},
    {"name": "Audi Revolut F1 Team", "color_hex": "#A9A9A9", "power_unit": "Audi", "chassis_name": "R26"},
    {"name": "TGR Haas F1 Team", "color_hex": "#B6B6B6", "power_unit": "Ferrari", "chassis_name": "VF-26"},
    {"name": "Cadillac F1 Team", "color_hex": "#00008B", "power_unit": "Ferrari", "chassis_name": "MAC-26"},
]

DRIVERS_DATA = [
    # Mercedes
    {"first_name": "George", "last_name": "Russell", "number": 63, "nationality": "British", "team_name": "Mercedes-AMG PETRONAS F1 Team"},
    {"first_name": "Kimi", "last_name": "Antonelli", "number": 12, "nationality": "Italian", "team_name": "Mercedes-AMG PETRONAS F1 Team"},
    # Red Bull
    {"first_name": "Max", "last_name": "Verstappen", "number": 3, "nationality": "Dutch", "team_name": "Oracle Red Bull Racing"},
    {"first_name": "Isack", "last_name": "Hadjar", "number": 6, "nationality": "French", "team_name": "Oracle Red Bull Racing"},
    # Ferrari
    {"first_name": "Charles", "last_name": "Leclerc", "number": 16, "nationality": "Monegasque", "team_name": "Scuderia Ferrari HP"},
    {"first_name": "Lewis", "last_name": "Hamilton", "number": 44, "nationality": "British", "team_name": "Scuderia Ferrari HP"},
    # McLaren
    {"first_name": "Lando", "last_name": "Norris", "number": 1, "nationality": "British", "team_name": "McLaren Mastercard F1 Team"},
    {"first_name": "Oscar", "last_name": "Piastri", "number": 81, "nationality": "Australian", "team_name": "McLaren Mastercard F1 Team"},
    # Aston Martin
    {"first_name": "Fernando", "last_name": "Alonso", "number": 14, "nationality": "Spanish", "team_name": "Aston Martin Aramco F1 Team"},
    {"first_name": "Lance", "last_name": "Stroll", "number": 18, "nationality": "Canadian", "team_name": "Aston Martin Aramco F1 Team"},
    # Alpine
    {"first_name": "Pierre", "last_name": "Gasly", "number": 10, "nationality": "French", "team_name": "BWT Alpine F1 Team"},
    {"first_name": "Franco", "last_name": "Colapinto", "number": 43, "nationality": "Argentinian", "team_name": "BWT Alpine F1 Team"},
    # Williams
    {"first_name": "Alexander", "last_name": "Albon", "number": 23, "nationality": "Thai", "team_name": "Atlassian Williams F1 Team"},
    {"first_name": "Carlos", "last_name": "Sainz Jr.", "number": 55, "nationality": "Spanish", "team_name": "Atlassian Williams F1 Team"},
    # RB
    {"first_name": "Arvid", "last_name": "Limblad", "number": 41, "nationality": "British", "team_name": "Visa Cash App Racing Bulls"},
    {"first_name": "Liam", "last_name": "Lawson", "number": 30, "nationality": "New Zealander", "team_name": "Visa Cash App Racing Bulls"},
    # Audi
    {"first_name": "Nico", "last_name": "Hülkenberg", "number": 27, "nationality": "German", "team_name": "Audi Revolut F1 Team"},
    {"first_name": "Gabriel", "last_name": "Bortoleto", "number": 5, "nationality": "Brazilian", "team_name": "Audi Revolut F1 Team"},
    # Haas
    {"first_name": "Esteban", "last_name": "Ocon", "number": 31, "nationality": "French", "team_name": "TGR Haas F1 Team"},
    {"first_name": "Oliver", "last_name": "Bearman", "number": 87, "nationality": "British", "team_name": "TGR Haas F1 Team"},
    # Cadillac
    {"first_name": "Sergio", "last_name": "Pérez", "number": 11, "nationality": "Mexican", "team_name": "Cadillac F1 Team"},
    {"first_name": "Valtteri", "last_name": "Bottas", "number": 77, "nationality": "Finnish", "team_name": "Cadillac F1 Team"},
]

HISTORICAL_DATA = {
    "Albert Park Grand Prix Circuit": {
        "laps": 58,
        "circuit_length": "5.278 km",
        "corners": 14,
        "lap_record": "1:20.235 (Sergio Pérez, 2025)",
        "previous_winner": "Carlos Sainz (2024)",
        "most_driver_wins": "Michael Schumacher (4)",
        "most_constructor_wins": "Ferrari (13)",
        "most_driver_podiums": "Lewis Hamilton (10)",
        "most_poles": "Lewis Hamilton (8)",
        "num_races_held": 27,
    },
    
    "Shanghai International Circuit": {
        "laps": 56,
        "circuit_length": "5.451 km",
        "corners": 16,
        "lap_record": "1:30.641 (Oscar Piastri, 2025)",
        "previous_winner": "Kimi Antonelli (2026)",
        "most_driver_wins": "Lewis Hamilton (6)",
        "most_constructor_wins": "Mercedes (6)",
        "most_driver_podiums": "Lewis Hamilton (10)",
        "most_poles": "Lewis Hamilton (6)",
        "num_races_held": 19,
    },
    
    "Suzuka Circuit": {
        "laps": 53,
        "circuit_length": "5.807 km",
        "corners": 18,
        "lap_record": "1:26.983 (Max Verstappen, 2025)",
        "previous_winner": "Kimi Antonelli (2026)",
        "most_driver_wins": "Michael Schumacher (6)",
        "most_constructor_wins": "McLaren (9)",
        "most_driver_podiums": "Michael Schumacher (9)",
        "most_poles": "Michael Schumacher (8)",
        "num_races_held": 36,
    },
    
    "Miami International Autodrome": {
        "laps": 57,
        "circuit_length": "5.412 km",
        "corners": 19,
        "lap_record": "1:26.983 (Max Verstappen, 2025)",
        "previous_winner": "Oscar Piastri (2025)",
        "most_driver_wins": "Max Verstappen (2)",
        "most_constructor_wins": "Red Bull (2)",
        "most_driver_podiums": "Max Verstappen (3)",
        "most_poles": "Max Verstappen (2)",
        "num_races_held": 4,
    },
    
    "Circuit Gilles Villeneuve": {
        "laps": 70,
        "circuit_length": "5.361 km",
        "corners": 14,
        "lap_record": "1:10.240 (Sebastian Vettel, 2019)",
        "previous_winner": "George Russell (2025)",
        "most_driver_wins": "Schumacher/Hamilton (7)",
        "most_constructor_wins": "Ferrari (11)",
        "most_driver_podiums": "Michael Schumacher (12)",
        "most_poles": "Schumacher/Hamilton (6)",
        "num_races_held": 44,
    },
    
    "Circuit de Monaco": {
        "laps": 78,
        "circuit_length": "3.337 km",
        "corners": 19,
        "lap_record": "1:09.954 (Lando Norris, 2025)",
        "previous_winner": "Lando Norris (2025)",
        "most_driver_wins": "Ayrton Senna (6)",
        "most_constructor_wins": "McLaren (16)",
        "most_driver_podiums": "Ayrton Senna (8)",
        "most_poles": "Ayrton Senna (5)",
        "num_races_held": 71,
    },
    
    "Circuit de Barcelona-Catalunya": {
        "laps": 66,
        "circuit_length": "4.657 km",
        "corners": 14,
        "lap_record": "1:11.383 (Lando Norris, 2024)",
        "previous_winner": "Oscar Piastri (2025)",
        "most_driver_wins": "Schumacher/Hamilton (6)",
        "most_constructor_wins": "Mercedes (6)",
        "most_driver_podiums": "Schumacher/Hamilton (19)",
        "most_poles": "Michael Schumacher (7)",
        "num_races_held": 35,
    },
    
    "Red Bull Ring": {
        "laps": 71,
        "circuit_length": "4.326 km",
        "corners": 10,
        "lap_record": "1:02.939 (Valtteri Bottas, 2020)",
        "previous_winner": "Lando Norris (2025)",
        "most_driver_wins": "Max Verstappen (5)",
        "most_constructor_wins": "Mercedes (7)",
        "most_driver_podiums": "Max Verstappen (8)",
        "most_poles": "Max Verstappen (5)",
        "num_races_held": 21,
    },
    
    "Silverstone Circuit": {
        "laps": 52,
        "circuit_length": "5.891 km",
        "corners": 18,
        "lap_record": "1:24.303 (Lewis Hamilton, 2020)",
        "previous_winner": "Lando Norris (2025)",
        "most_driver_wins": "Lewis Hamilton (9)",
        "most_constructor_wins": "Ferrari (15)",
        "most_driver_podiums": "Lewis Hamilton (15)",
        "most_poles": "Lewis Hamilton (7)",
        "num_races_held": 60,
    },
}

def seed_database():
    # Elimina tabelle esistenti per rifare il seeding con i nuovi campi
    Base.metadata.drop_all(bind=engine)
    Base.metadata.create_all(bind=engine)
    
    db: Session = SessionLocal()
    try:
        print("Popolamento tabella Teams...")
        team_map = {}
        for team_data in TEAMS_DATA:
            team = Team(**team_data)
            db.add(team)
            db.flush()
            team_map[team.name] = team.id
        
        print("Popolamento tabella Drivers...")
        for driver_data in DRIVERS_DATA:
            team_name = driver_data.pop("team_name")
            driver = Driver(**driver_data, team_id=team_map[team_name])
            db.add(driver)

        print("Popolamento tabella Races...")
        calendar_service = CalendarService()
        schedule_data = ExternalApiService.get_schedule(year=2026)
        for race_data in calendar_service.races:
            circuit_name = race_data.get("circuit_name")
            hist_data = HISTORICAL_DATA.get(circuit_name, {}) # Prende i dati storici tramite nome circuito
            round_num = race_data["round"]
            sessions = schedule_data.get(round_num, {})
            
            # Manteniamo la nostra lista manuale come fonte di verità assoluta
            is_sprint_final = (race_data["round"] in [2, 4, 5, 9, 12, 16]) or sessions.get("is_sprint_jolpica", False)

            race = Race(
                # dati round #
                round_number=race_data["round"],
                name=race_data["name"],
                date=race_data["date"],
                country=race_data["country"],
                city=race_data["city"],
                circuit_name=circuit_name,
                laps=hist_data.get("laps", 57),
                circuit_length=hist_data.get("circuit_length", "N/A"),
                corners=hist_data.get("corners", 0),
                lap_record=hist_data.get("lap_record", "N/A"),
                
                # dati storici #
                previous_winner=hist_data.get("previous_winner", "N/A"),
                most_driver_wins=hist_data.get("most_driver_wins", "N/A"),
                most_constructor_wins=hist_data.get("most_constructor_wins", "N/A"),
                most_driver_podiums=hist_data.get("most_driver_podiums", "N/A"),
                most_poles=hist_data.get("most_poles", "N/A"),
                num_races_held=hist_data.get("num_races_held", 0),
                is_sprint=is_sprint_final,
                cancelled=race_data.get("cancelled", False),
                
                # sessioni di gara #
                fp1_time=sessions.get("fp1"),
                fp2_time=sessions.get("fp2"),
                fp3_time=sessions.get("fp3"),
                sprint_shootout_time=sessions.get("sprint_shootout"),
                sprint_race_time=sessions.get("sprint_race"),
                quali_time=sessions.get("quali"),
                race_time=sessions.get("race")
            )
            db.add(race)

        db.flush()
        
        db.commit()
        print("Database popolato con successo!")

    except Exception as e:
        print(f"Errore durante il seeding: {e}")
        db.rollback()
    finally:
        db.close()

if __name__ == "__main__":
    seed_database()
