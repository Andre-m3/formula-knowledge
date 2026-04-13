from sqlalchemy.orm import Session
from .database import SessionLocal
from .models import DriverCareerStats

def fix_world_championships():
    db: Session = SessionLocal()
    
    # I campioni in attività e i loro titoli
    champions = {
        "hamilton": 7,       # (2008, 2014, 2015, 2017, 2018, 2019, 2020)
        "max_verstappen": 4, # (2021, 2022, 2023, 2024)
        "alonso": 2,         # (2005, 2006)
        "norris": 1          # (2025)
    }
    
    for driver_id, titles in champions.items():
        stat_obj = db.query(DriverCareerStats).filter(DriverCareerStats.driver_id == driver_id).first()
        if stat_obj:
            stat_obj.world_championships = titles
            print(f"✅ Aggiornato {driver_id}: {titles} Campionati Mondiali")
            
    db.commit()
    db.close()

if __name__ == "__main__":
    fix_world_championships()