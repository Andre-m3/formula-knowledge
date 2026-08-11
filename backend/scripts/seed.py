from sqlalchemy.orm import Session
from app.database import SessionLocal, engine
from app.models import Team, Driver, Race, RaceResult, Base
from app.core.config import settings
from app.services.calendar_service import CalendarService
from app.services.external_api_service import ExternalApiService

# Dati per il seeding della stagione configurata.
TEAMS_DATA = [
    {"name": "Mercedes-AMG PETRONAS F1 Team", "color_hex": "#27F4D2", "power_unit": "Mercedes", "chassis_name": "W17"},
    {"name": "Oracle Red Bull Racing", "color_hex": "#3671C6", "power_unit": "RedBull-Ford Powertrains", "chassis_name": "RB22"},
    {"name": "Scuderia Ferrari HP", "color_hex": "#E8002D", "power_unit": "Ferrari", "chassis_name": "SF-26"},
    {"name": "McLaren Mastercard F1 Team", "color_hex": "#FF8000", "power_unit": "Mercedes", "chassis_name": "MCL40"},
    {"name": "Aston Martin Aramco F1 Team", "color_hex": "#229971", "power_unit": "Honda", "chassis_name": "AMR26"},
    {"name": "BWT Alpine F1 Team", "color_hex": "#00A1E8", "power_unit": "Mercedes", "chassis_name": "A526"},
    {"name": "Atlassian Williams F1 Team", "color_hex": "#1868DB", "power_unit": "Mercedes", "chassis_name": "FW48"},
    {"name": "Visa Cash App Racing Bulls", "color_hex": "#6692FF", "power_unit": "RedBull-Ford Powertrains", "chassis_name": "VCARB 03"},
    {"name": "Audi Revolut F1 Team", "color_hex": "#FF2D00", "power_unit": "Audi", "chassis_name": "R26"},
    {"name": "TGR Haas F1 Team", "color_hex": "#DEE1E2", "power_unit": "Ferrari", "chassis_name": "VF-26"},
    {"name": "Cadillac F1 Team", "color_hex": "#AAAAAD", "power_unit": "Ferrari", "chassis_name": "MAC-26"},
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
    {"first_name": "Arvid", "last_name": "Lindblad", "number": 41, "nationality": "British", "team_name": "Visa Cash App Racing Bulls"},
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
        "previous_winner": "Kimi Antonelli (2026)",
        "most_driver_wins": "Ayrton Senna (6)",
        "most_constructor_wins": "McLaren (16)",
        "most_driver_podiums": "Ayrton Senna (8)",
        "most_poles": "Ayrton Senna (5)",
        "num_races_held": 72,
    },
    
    "Circuit de Barcelona-Catalunya": {
        "laps": 66,
        "circuit_length": "4.657 km",
        "corners": 14,
        "lap_record": "1:11.383 (Lando Norris, 2024)",
        "previous_winner": "Lewis Hamilton (2026)",
        "most_driver_wins": "Lewis Hamilton (7)",
        "most_constructor_wins": "Mercedes (6)",
        "most_driver_podiums": "Lewis Hamilton (20)",
        "most_poles": "Michael Schumacher (7)",
        "num_races_held": 36,
    },
    
    "Red Bull Ring": {
        "laps": 71,
        "circuit_length": "4.326 km",
        "corners": 10,
        "lap_record": "1:02.939 (Valtteri Bottas, 2020)",
        "previous_winner": "George Russell (2026)",
        "most_driver_wins": "Max Verstappen (5)",
        "most_constructor_wins": "Mercedes (8)",
        "most_driver_podiums": "Max Verstappen (8)",
        "most_poles": "Max Verstappen (5)",
        "num_races_held": 22,
    },
    
    "Silverstone Circuit": {
        "laps": 52,
        "circuit_length": "5.891 km",
        "corners": 18,
        "lap_record": "1:24.303 (Lewis Hamilton, 2020)",
        "previous_winner": "Charles Leclerc (2026)",
        "most_driver_wins": "Lewis Hamilton (9)",
        "most_constructor_wins": "Ferrari (16)",
        "most_driver_podiums": "Lewis Hamilton (16)",
        "most_poles": "Lewis Hamilton (7)",
        "num_races_held": 61,
    },
    
    "Circuit de Spa-Francorchamps": {
        "laps": 44,
        "circuit_length": "7.004 km",
        "corners": 19,
        "lap_record": "1:44.701 (Sergio Pérez, 2024)",
        "previous_winner": "Kimi Antonelli (2026)",
        "most_driver_wins": "Michael Schumacher (6)",
        "most_constructor_wins": "Ferrari (14)",
        "most_driver_podiums": "Lewis Hamilton (11)",
        "most_poles": "Lewis Hamilton (6)",
        "num_races_held": 59,
    },

    "Hungaroring": {
        "laps": 70,
        "circuit_length": "4.381 km",
        "corners": 14,
        "lap_record": "1:16.627 (Lewis Hamilton, 2020)",
        "previous_winner": "Lando Norris (2026)",
        "most_driver_wins": "Lewis Hamilton (8)",
        "most_constructor_wins": "McLaren (15)",
        "most_driver_podiums": "Lewis Hamilton (12)",
        "most_poles": "Lewis Hamilton (9)",
        "num_races_held": 41,
    },

    "Circuit Park Zandvoort": {
        "laps": 72,
        "circuit_length": "4.259 km",
        "corners": 14,
        "lap_record": "1:11.097 (Lewis Hamilton, 2021)",
        "previous_winner": "Oscar Piastri (2025)",
        "most_driver_wins": "Jim Clark (4)",
        "most_constructor_wins": "Ferrari (8)",
        "most_driver_podiums": "Clark/Lauda (6)",
        "most_poles": "Arnoux/Verstappen (3)",
        "num_races_held": 35,
    },

    "Autodromo Nazionale Monza": {
        "laps": 53,
        "circuit_length": "5.793 km",
        "corners": 11,
        "lap_record": "1:20.901 (Lando Norris, 2025)",
        "previous_winner": "Max Verstappen (2025)",
        "most_driver_wins": "Schumacher/Hamilton (5)",
        "most_constructor_wins": "Ferrari (20)",
        "most_driver_podiums": "Schumacher/Hamilton (8)",
        "most_poles": "Lewis Hamilton (7)",
        "num_races_held": 75,
    },

    "Madring": {
        "laps": 57,
        "circuit_length": "5.416 km",
        "corners": 22,
        "lap_record": "N/A",
        "previous_winner": "N/A",
        "most_driver_wins": "N/A",
        "most_constructor_wins": "N/A",
        "most_driver_podiums": "N/A",
        "most_poles": "N/A",
        "num_races_held": 0,
    },
    
    "Baku City Circuit": {
        "laps": 51,
        "circuit_length": "6.003 km",
        "corners": 20,
        "lap_record": "1:43.009 (Charles Leclerc, 2019)",
        "previous_winner": "Max Verstappen (2025)",
        "most_driver_wins": "Pérez/Verstappen (2)",
        "most_constructor_wins": "Red Bull (5)",
        "most_driver_podiums": "Sergio Pérez (5)",
        "most_poles": "Charles Leclerc (4)",
        "num_races_held": 9,
    },

    "Sepang International Circuit": {
        "laps": 56,
        "circuit_length": "5.543 km",
        "corners": 15,
        "lap_record": "1:34.080 (Sebastian Vettel, 2017)",
        "previous_winner": "Max Verstappen (2017)",
        "most_driver_wins": "Sebastian Vettel (4)",
        "most_constructor_wins": "Ferrari (7)",
        "most_driver_podiums": "Lewis Hamilton (6)",
        "most_poles": "Schumacher/Hamilton (5)",
        "num_races_held": 19,
    },

    "Marina Bay Street Circuit": {
        "laps": 62,
        "circuit_length": "4.927 km",
        "corners": 19,
        "lap_record": "1:33.808 (Lewis Hamilton, 2025)",
        "previous_winner": "George Russell (2025)",
        "most_driver_wins": "Sebastian Vettel (5)",
        "most_constructor_wins": "Mercedes (5)",
        "most_driver_podiums": "Sebastian Vettel (8)",
        "most_poles": "Vettel/Hamilton (4)",
        "num_races_held": 16,
    },

    "Circuit of the Americas": {
        "laps": 56,
        "circuit_length": "5.513 km",
        "corners": 20,
        "lap_record": "1:36.169 (Charles Leclerc, 2019)",
        "previous_winner": "Max Verstappen (2025)",
        "most_driver_wins": "Lewis Hamilton (5)",
        "most_constructor_wins": "Mercedes/Red Bull (5)",
        "most_driver_podiums": "Lewis Hamilton (9)",
        "most_poles": "Lewis Hamilton (3)",
        "num_races_held": 13,
    },

    "Autódromo Hermanos Rodríguez": {
        "laps": 71,
        "circuit_length": "4.304 km",
        "corners": 17,
        "lap_record": "1:17.774 (Valtteri Bottas, 2021)",
        "previous_winner": "Lando Norris (2025)",
        "most_driver_wins": "Max Verstappen (5)",
        "most_constructor_wins": "Red Bull (5)",
        "most_driver_podiums": "Hamilton/Verstappen (6)",
        "most_poles": "Jim Clark (4)",
        "num_races_held": 25,
    },
    
    "Autódromo José Carlos Pace": {
        "laps": 71,
        "circuit_length": "4.309 km",
        "corners": 15,
        "lap_record": "1:10.540 (Valtteri Bottas, 2018)",
        "previous_winner": "Lando Norris (2025)",
        "most_driver_wins": "Michael Schumacher (4)",
        "most_constructor_wins": "Ferrari/McLaren (9)",
        "most_driver_podiums": "Michael Schumacher (10)",
        "most_poles": "Senna/Häkkinen /Massa/Barrichello /Hamilton (3)",
        "num_races_held": 42,
    },

    "Las Vegas Strip Circuit": {
        "laps": 50,
        "circuit_length": "6.201 km",
        "corners": 17,
        "lap_record": "1:33.365 (Max Verstappen, 2025)",
        "previous_winner": "Max Verstappen (2025)",
        "most_driver_wins": "Max Verstappen (2)",
        "most_constructor_wins": "Red Bull (2)",
        "most_driver_podiums": "Max Verstappen/George Russell (2)",
        "most_poles": "Leclerc/Russell /Norris (1)",
        "num_races_held": 3,
    },

    "Lusail International Circuit": {
        "laps": 57,
        "circuit_length": "5.419 km",
        "corners": 16,
        "lap_record": "1:22.384 (Lando Norris, 2024)",
        "previous_winner": "Max Verstappen (2025)",
        "most_driver_wins": "Max Verstappen (3)",
        "most_constructor_wins": "Red Bull (3)",
        "most_driver_podiums": "Max Verstappen (4)",
        "most_poles": "Hamilton/Verstappen /Russell/Piastri (1)",
        "num_races_held": 4,
    },

    "Yas Marina Circuit": {
        "laps": 55,
        "circuit_length": "5.554 km",
        "corners": 16,
        "lap_record": "1:39.283 (Lewis Hamilton, 2019)",
        "previous_winner": "Max Verstappen (2025)",
        "most_driver_wins": "Hamilton/Verstappen (5)",
        "most_constructor_wins": "Red Bull (8)",
        "most_driver_podiums": "Lewis Hamilton (10)",
        "most_poles": "Hamilton/Verstappen (5)",
        "num_races_held": 17,
    },
}

# Jolpica e le fixture storiche possono usare denominazioni diverse per lo
# stesso circuito. Manteniamo il nome ricevuto dall'API nel database e usiamo
# questo alias solo per la ricerca dei dati statici.
CIRCUIT_NAME_ALIASES = {
    "Albert Park Circuit": "Albert Park Grand Prix Circuit",
}

def _historical_circuit_name(circuit_name):
    return CIRCUIT_NAME_ALIASES.get(circuit_name, circuit_name)

def _validate_historical_data(calendar_races):
    """Fail before any destructive reset if a live circuit lacks static data."""
    missing = []
    for race in calendar_races:
        if race.get("cancelled", False):
            continue

        circuit_name = race.get("circuit_name")
        if not circuit_name:
            missing.append(f"round {race.get('round')}: circuit_name assente")
        elif _historical_circuit_name(circuit_name) not in HISTORICAL_DATA:
            missing.append(circuit_name)

    if missing:
        missing_display = ", ".join(sorted(set(missing)))
        raise ValueError(
            "Dati storici mancanti per circuiti non cancellati: "
            f"{missing_display}"
        )
def seed_database():
    calendar_service = CalendarService()
    _validate_historical_data(calendar_service.races)

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
            # Non modificare DRIVERS_DATA in-place: il seed deve poter essere
            # eseguito più volte nello stesso processo, soprattutto nei test.
            team_name = driver_data["team_name"]
            driver_fields = {
                key: value for key, value in driver_data.items() if key != "team_name"
            }
            driver = Driver(**driver_fields, team_id=team_map[team_name])
            db.add(driver)

        print("Popolamento tabella Races...")
        # TODO: In futuro, quando passeremo a PostgreSQL, inseriremo l'intero calendario
        # in modo hardcodato (comprese le date di FP1, FP2, ecc.) per rimuovere la dipendenza da Jolpica.
        # CalendarService è già stato validato prima del reset del database.
        schedule_data = ExternalApiService.get_schedule(year=settings.F1_SEASON)
        for race_data in calendar_service.races:
            circuit_name = race_data.get("circuit_name")
            historical_circuit_name = _historical_circuit_name(circuit_name)
            hist_data = HISTORICAL_DATA.get(historical_circuit_name, {}) # Prende i dati storici tramite nome circuito
            sessions = schedule_data.get(race_data["round"], {})
            
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
        raise
    finally:
        db.close()

if __name__ == "__main__":
    seed_database()
