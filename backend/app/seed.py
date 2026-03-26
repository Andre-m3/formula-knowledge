from sqlalchemy.orm import Session
from .database import SessionLocal, engine
from .models import Team, Driver, Race, RaceResult, Base
from .services.calendar_service import CalendarService

# Dati per il seeding (Stagione 2026 - Proiezione)
TEAMS_DATA = [
    {"name": "Mercedes-AMG PETRONAS F1 Team", "color_hex": "#00D2BE", "power_unit": "Mercedes"},
    {"name": "Oracle Red Bull Racing", "color_hex": "#1E41FF", "power_unit": "RedBull-Ford Powertrains"},
    {"name": "Scuderia Ferrari HP", "color_hex": "#E32219", "power_unit": "Ferrari"},
    {"name": "McLaren Mastercard F1 Team", "color_hex": "#FF8000", "power_unit": "Mercedes"},
    {"name": "Aston Martin Aramco F1 Team", "color_hex": "#006F62", "power_unit": "Honda"},
    {"name": "BWT Alpine F1 Team", "color_hex": "#0090FF", "power_unit": "Mercedes"},
    {"name": "Atlassian Williams F1 Team", "color_hex": "#005AFF", "power_unit": "Mercedes"},
    {"name": "Visa Cash App Racing Bulls", "color_hex": "#00359F", "power_unit": "RedBull-Ford Powertrains"},
    {"name": "Audi Revolut F1 Team", "color_hex": "#A9A9A9", "power_unit": "Audi"},
    {"name": "TGR Haas F1 Team", "color_hex": "#B6B6B6", "power_unit": "Ferrari"},
    {"name": "Cadillac F1 Team", "color_hex": "#00008B", "power_unit": "Ferrari"},
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

def seed_database():
    # Assicuriamoci che le tabelle vengano create se il database non esiste
    Base.metadata.create_all(bind=engine)
    
    db: Session = SessionLocal()
    try:
        if db.query(Team).count() > 0:
            print("Database già popolato. Salto il seeding.")
            return

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
        for race_data in CalendarService().races:
            race = Race(round_number=race_data["round"], name=race_data["name"], date=race_data["date"], country=race_data["country"], city=race_data["city"])
            db.add(race)

        db.flush() # Salva temporaneamente per avere gli ID generati
        
        print("Popolamento tabella RaceResults (Round 1 & 2)...")
        def get_driver_id(last_name):
            return db.query(Driver).filter(Driver.last_name == last_name).first().id
            
        race1 = db.query(Race).filter(Race.round_number == 1).first()
        race2 = db.query(Race).filter(Race.round_number == 2).first()
        
        if race1 and race2:
            mock_race_results = [
                # ROUND 1 - Australia
                {"race_id": race1.id, "driver_id": get_driver_id("Leclerc"), "position": 1, "points": 25, "time_str": "1:32:41.432"},
                {"race_id": race1.id, "driver_id": get_driver_id("Hamilton"), "position": 2, "points": 18, "time_str": "+2.431s"},
                {"race_id": race1.id, "driver_id": get_driver_id("Norris"), "position": 3, "points": 15, "time_str": "+5.672s"},
                {"race_id": race1.id, "driver_id": get_driver_id("Verstappen"), "position": 4, "points": 12, "time_str": "+10.123s"},
                {"race_id": race1.id, "driver_id": get_driver_id("Russell"), "position": 5, "points": 10, "time_str": "+15.432s"},
                # ROUND 2 - Cina
                {"race_id": race2.id, "driver_id": get_driver_id("Verstappen"), "position": 1, "points": 25, "time_str": "1:29:11.111"},
                {"race_id": race2.id, "driver_id": get_driver_id("Russell"), "position": 2, "points": 18, "time_str": "+11.222s"},
                {"race_id": race2.id, "driver_id": get_driver_id("Leclerc"), "position": 3, "points": 15, "time_str": "+15.333s"},
                {"race_id": race2.id, "driver_id": get_driver_id("Norris"), "position": 4, "points": 12, "time_str": "+16.444s"},
                {"race_id": race2.id, "driver_id": get_driver_id("Hamilton"), "position": 5, "points": 10, "time_str": "+22.555s"},
            ]
            for res_data in mock_race_results:
                db.add(RaceResult(**res_data))

        db.commit()
        print("Database popolato con successo!")

    except Exception as e:
        print(f"Errore durante il seeding: {e}")
        db.rollback()
    finally:
        db.close()

if __name__ == "__main__":
    print("Avvio del processo di seeding del database...")
    seed_database()