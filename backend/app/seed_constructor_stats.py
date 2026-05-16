from sqlalchemy.orm import Session
from datetime import datetime, timezone
from .database import SessionLocal
from .models import ConstructorCareerStats

# Dizionario storico accurato per i Costruttori 2026
MANUAL_CONSTRUCTOR_DATA = {
    "ferrari": {
        "total_races": 1125, "wins": 248, "podiums": 839, "driver_championships": 15, "constructor_championships": 16,
        "first_gp_year": "1950", "first_win": "1951 (British GP)", "pole_positions": 254, "fastest_laps": 265,
        "total_points": 10812.0, "seasons_entered": 77, "best_race_result": "1", "best_championship_result": "1st (16 times)",
        "power_unit": "Ferrari", "team_principal": "Fred Vasseur", "base_location": "Maranello, Italy"
    },
    "mclaren": {
        "total_races": 996, "wins": 203, "podiums": 559, "driver_championships": 13, "constructor_championships": 10,
        "first_gp_year": "1966", "first_win": "1968 (Belgian GP)", "pole_positions": 177, "fastest_laps": 184,
        "total_points": 7836.5, "seasons_entered": 61, "best_race_result": "1", "best_championship_result": "1st (10 times)",
        "power_unit": "Mercedes", "team_principal": "Andrea Stella", "base_location": "Woking, United Kingdom"
    },
    "mercedes": {
        "total_races": 344, "wins": 134, "podiums": 315, "driver_championships": 9, "constructor_championships": 8,
        "first_gp_year": "1954", "first_win": "1954 (French GP)", "pole_positions": 146, "fastest_laps": 117,
        "total_points": 8294.5, "seasons_entered": 19, "best_race_result": "1", "best_championship_result": "1st (8 times)",
        "power_unit": "Mercedes", "team_principal": "Toto Wolff", "base_location": "Brackley, United Kingdom"
    },
    "red_bull": {
        "total_races": 420, "wins": 130, "podiums": 297, "driver_championships": 8, "constructor_championships": 6,
        "first_gp_year": "2005", "first_win": "2009 (Chinese GP)", "pole_positions": 111, "fastest_laps": 103,
        "total_points": 8304.0, "seasons_entered": 22, "best_race_result": "1", "best_championship_result": "1st (6 times)",
        "power_unit": "RBPT", "team_principal": "Christian Horner", "base_location": "Milton Keynes, United Kingdom"
    },
    "williams": {
        "total_races": 878, "wins": 114, "podiums": 315, "driver_championships": 7, "constructor_championships": 9,
        "first_gp_year": "1977", "first_win": "1979 (British GP)", "pole_positions": 128, "fastest_laps": 134,
        "total_points": 3776.0, "seasons_entered": 51, "best_race_result": "1", "best_championship_result": "1st (9 times)",
        "power_unit": "Mercedes", "team_principal": "James Vowles", "base_location": "Grove, United Kingdom"
    },
    
    # I dati di base per le altre scuderie: potrai modificarli facilmente a mano e ri-eseguire lo script
    "aston_martin": {
        "total_races": 73,
        "wins": 0,
        "podiums": 9,
        "driver_championships": 0,
        "constructor_championships": 0,
        "first_gp_year": "1959",
        "first_win": "N/A",
        "pole_positions": 0,
        "fastest_laps": 1,
        "total_points": 400.0,
        "seasons_entered": 5,
        "best_race_result": "2",
        "best_championship_result": "5th (2023)",
        "power_unit": "Honda",
        "team_principal": "Mike Krack",
        "base_location": "Silverstone, United Kingdom"
    },
    "alpine": {
        "total_races": 66,
        "wins": 1,
        "podiums": 4,
        "driver_championships": 0,
        "constructor_championships": 0,
        "first_gp_year": "2021",
        "first_win": "2021 (Hungarian GP)",
        "pole_positions": 0,
        "fastest_laps": 0,
        "total_points": 450.0,
        "seasons_entered": 4,
        "best_race_result": "1",
        "best_championship_result": "4th (2022)",
        "power_unit": "Mercedes",
        "team_principal": "Oliver Oakes",
        "base_location": "Enstone, UK / Viry, France"
    },
    "rb": {
        "total_races": 0,
        "wins": 0,
        "podiums": 0,
        "driver_championships": 0,
        "constructor_championships": 0,
        "first_gp_year": "2024",
        "first_win": "N/A",
        "pole_positions": 0,
        "fastest_laps": 0,
        "total_points": 0.0,
        "seasons_entered": 1,
        "best_race_result": "7",
        "best_championship_result": "8th (2024)",
        "power_unit": "RBPT",
        "team_principal": "Laurent Mekies",
        "base_location": "Faenza, Italy"
    },
    "haas": {
        "total_races": 166,
        "wins": 0,
        "podiums": 0,
        "driver_championships": 0,
        "constructor_championships": 0,
        "first_gp_year": "2016",
        "first_win": "N/A",
        "pole_positions": 1,
        "fastest_laps": 2,
        "total_points": 249.0,
        "seasons_entered": 9,
        "best_race_result": "4",
        "best_championship_result": "5th (2018)",
        "power_unit": "Ferrari",
        "team_principal": "Ayao Komatsu",
        "base_location": "Kannapolis, United States"
    },
    "sauber": {
        "total_races": 465,
        "wins": 1,
        "podiums": 26,
        "driver_championships": 0,
        "constructor_championships": 0,
        "first_gp_year": "1993",
        "first_win": "2008 (Canadian GP)",
        "pole_positions": 1,
        "fastest_laps": 5,
        "total_points": 865.0,
        "seasons_entered": 30,
        "best_race_result": "1",
        "best_championship_result": "2nd (2008)",
        "power_unit": "Audi",
        "team_principal": "Mattia Binotto",
        "base_location": "Hinwil, Switzerland"
    },
    "cadillac": {
        "total_races": 0,
        "wins": 0,
        "podiums": 0,
        "driver_championships": 0,
        "constructor_championships": 0,
        "first_gp_year": "2026",
        "first_win": "N/A",
        "pole_positions": 0,
        "fastest_laps": 0,
        "total_points": 0.0,
        "seasons_entered": 0,
        "best_race_result": "N/A",
        "best_championship_result": "N/A",
        "power_unit": "Ferrari",
        "team_principal": "TBD",
        "base_location": "United States"
    }
}

def seed_constructor_stats():
    db: Session = SessionLocal()
    
    for cid, data in MANUAL_CONSTRUCTOR_DATA.items():
        stat_obj = db.query(ConstructorCareerStats).filter(ConstructorCareerStats.constructor_id == cid).first()
        if not stat_obj:
            stat_obj = ConstructorCareerStats(constructor_id=cid)
            db.add(stat_obj)
            
        for key, value in data.items():
            setattr(stat_obj, key, value)
            
        stat_obj.last_updated = datetime.now(timezone.utc)
        db.commit()
        
        print(f"-> Dati All-Time di '{cid.upper()}' salvati in locale!")
        
    db.close()
    print("✅ Initial Seeding Constructor Stats completato!")

if __name__ == "__main__":
    seed_constructor_stats()