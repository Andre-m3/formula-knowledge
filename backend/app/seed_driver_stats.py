import requests
import time
from sqlalchemy.orm import Session
from datetime import datetime, timezone

from .database import SessionLocal
from .models import DriverCareerStats

# Gli ID standard (Ergast/Jolpica) dei 20 piloti attuali (stagione 2026/2025)
DRIVER_IDS = [
    "russell", "antonelli", "max_verstappen", "hadjar", "leclerc",
    "hamilton", "norris", "piastri", "alonso", "stroll", "gasly",
    "colapinto", "albon", "sainz", "arvid_lindblad", "lawson", "hulkenberg",
    "bortoleto", "ocon", "bearman", "perez", "bottas"
]

def fetch_with_retry(url):
    for _ in range(3):
        try:
            res = requests.get(url, timeout=10)
            if res.status_code == 200:
                return res
            elif res.status_code == 429:
                print("Rate limit API raggiunto. Attendo 6 secondi e riprovo...")
                time.sleep(6)
            else:
                break
        except Exception as e:
            print(f"Eccezione HTTP ({e}). Attendo 6 secondi e riprovo...")
            time.sleep(6)
    return None

def get_driver_stats(driver_id: str):
    print(f"Calcolando storico carriera per '{driver_id}'...")
    stats = {
        "total_races": 0, "wins": 0, "podiums": 0, "pole_positions": 0, "wins_from_pole": 0,
        "world_championships": 0, "best_race_result": 999, "best_grid_position": 999,
        "best_championship_result": 999, "fastest_laps": 0,
        "dns_count": 0, "dnf_count": 0, "dsq_count": 0,
        "sprint_starts": 0, "sprint_wins": 0, 
        "best_sprint_result": 999, "best_sprint_grid_position": 999,
        "place_of_birth": "N/A", "date_of_birth": "N/A",
        "first_gp": "N/A", "first_win": "N/A",
        "hat_tricks": 0, "grand_slams": 0
    }

    # 1. Risultati Gara (Races, Wins, Podiums)
    offset = 0
    while True:
        url = f"https://api.jolpi.ca/ergast/f1/drivers/{driver_id}/results.json?limit=100&offset={offset}"
        res = fetch_with_retry(url)
        if res and res.status_code == 200:
            data = res.json().get("MRData", {})
            races = data.get("RaceTable", {}).get("Races", [])
            if not races: break
            
            stats["total_races"] += len(races)
            for r in races:
                if stats["date_of_birth"] == "N/A":
                    stats["date_of_birth"] = r["Results"][0]["Driver"].get("dateOfBirth", "N/A")
                
                if stats["first_gp"] == "N/A":
                    stats["first_gp"] = f"{r['season']} {r['raceName']}"

                pos = int(r["Results"][0]["position"])
                if pos == 1: stats["wins"] += 1
                if pos <= 3: stats["podiums"] += 1
                if pos < stats["best_race_result"]: stats["best_race_result"] = pos
                if pos == 1 and stats["first_win"] == "N/A": stats["first_win"] = f"{r['season']} {r['raceName']}"

                # DNF / DNS / DSQ
                res_status = r["Results"][0].get("status", "").lower()
                pos_text = r["Results"][0].get("positionText", "").upper()
                if pos_text == "W" or "withdrawn" in res_status or "did not start" in res_status: stats["dns_count"] += 1
                elif pos_text == "D" or "disqualified" in res_status: stats["dsq_count"] += 1
                elif pos_text == "R": stats["dnf_count"] += 1
            
            total = int(data.get("total", 0))
            offset += 100
            if offset >= total: break
            time.sleep(0.1) # Cortesia API
        else:
            break

    # 2. Qualifiche (Poles)
    offset = 0
    while True:
        url = f"https://api.jolpi.ca/ergast/f1/drivers/{driver_id}/qualifying.json?limit=100&offset={offset}"
        q_res = fetch_with_retry(url)
        if q_res and q_res.status_code == 200:
            data = q_res.json().get("MRData", {})
            q_races = data.get("RaceTable", {}).get("Races", [])
            if not q_races: break
            
            for q in q_races:
                if int(q["QualifyingResults"][0]["position"]) == 1:
                    stats["pole_positions"] += 1
                q_pos = int(q["QualifyingResults"][0]["position"])
                if q_pos < stats["best_grid_position"]:
                    stats["best_grid_position"] = q_pos
            
            total = int(data.get("total", 0))
            offset += 100
            if offset >= total: break
            time.sleep(0.1)
        else:
            break

    # 3. Fastest Laps (Chiamata singola con risultato total)
    f_url = f"https://api.jolpi.ca/ergast/f1/drivers/{driver_id}/fastest/1/results.json?limit=1"
    f_res = fetch_with_retry(f_url)
    if f_res and f_res.status_code == 200:
        stats["fastest_laps"] = int(f_res.json().get("MRData", {}).get("total", 0))
    time.sleep(0.1)

    # 4. Sprints
    s_url = f"https://api.jolpi.ca/ergast/f1/drivers/{driver_id}/sprint.json?limit=100"
    s_res = fetch_with_retry(s_url)
    if s_res and s_res.status_code == 200:
        sprints = s_res.json().get("MRData", {}).get("RaceTable", {}).get("Races", [])
        stats["sprint_starts"] = len(sprints)
        for s in sprints:
            s_pos = int(s["SprintResults"][0]["position"])
            s_grid = int(s["SprintResults"][0]["grid"])
            if s_pos == 1: stats["sprint_wins"] += 1
            if s_pos < stats["best_sprint_result"]: stats["best_sprint_result"] = s_pos
            if s_grid > 0 and s_grid < stats["best_sprint_grid_position"]: stats["best_sprint_grid_position"] = s_grid
    time.sleep(0.1)

    # Normalizzazione Valori "Migliori"
    stats["best_race_result"] = str(stats["best_race_result"]) if stats["best_race_result"] != 999 else "N/A"
    stats["best_grid_position"] = str(stats["best_grid_position"]) if stats["best_grid_position"] != 999 else "N/A"
    stats["best_sprint_result"] = str(stats["best_sprint_result"]) if stats["best_sprint_result"] != 999 else "N/A"
    stats["best_sprint_grid_position"] = str(stats["best_sprint_grid_position"]) if stats["best_sprint_grid_position"] != 999 else "N/A"

    # 3. Campionati Mondiali
    url_champ = f"https://api.jolpi.ca/ergast/f1/drivers/{driver_id}/driverStandings/1/seasons.json?limit=100"
    c_res = fetch_with_retry(url_champ)
    if c_res and c_res.status_code == 200:
        seasons = c_res.json().get("MRData", {}).get("SeasonTable", {}).get("Seasons", [])
        current_year = str(datetime.now(timezone.utc).year)
        # Assicuriamoci che l'anno provvisorio (se è attualmente 1°) non venga contato fino a fine stagione
        valid_seasons = [s for s in seasons if s.get("season") != current_year]
        stats["world_championships"] = len(valid_seasons)

    # Restituiamo N/A per il momento su best championship per risparmiare query pesanti. 
    stats["best_championship_result"] = "N/A" 

    time.sleep(0.2)

    return stats

def seed_driver_stats():
    db: Session = SessionLocal()
    for did in DRIVER_IDS:
        data = get_driver_stats(did)
        stat_obj = db.query(DriverCareerStats).filter(DriverCareerStats.driver_id == did).first()
        if not stat_obj:
            stat_obj = DriverCareerStats(driver_id=did)
            db.add(stat_obj)
        
        stat_obj.total_races = data["total_races"]
        stat_obj.wins = data["wins"]
        stat_obj.podiums = data["podiums"]
        stat_obj.pole_positions = data["pole_positions"]
        stat_obj.wins_from_pole = data["wins_from_pole"]
        stat_obj.world_championships = data["world_championships"]
        
        stat_obj.best_race_result = data["best_race_result"]
        stat_obj.best_championship_result = data["best_championship_result"]
        stat_obj.best_grid_position = data["best_grid_position"]
        stat_obj.fastest_laps = data["fastest_laps"]
        stat_obj.dns_count = data["dns_count"]
        stat_obj.dnf_count = data["dnf_count"]
        stat_obj.dsq_count = data["dsq_count"]
        
        stat_obj.sprint_starts = data["sprint_starts"]
        stat_obj.sprint_wins = data["sprint_wins"]
        stat_obj.best_sprint_result = data["best_sprint_result"]
        stat_obj.best_sprint_grid_position = data["best_sprint_grid_position"]
        
        stat_obj.place_of_birth = data["place_of_birth"]
        stat_obj.date_of_birth = data["date_of_birth"]
        stat_obj.first_gp = data["first_gp"]
        stat_obj.first_win = data["first_win"]
        stat_obj.hat_tricks = data["hat_tricks"]
        stat_obj.grand_slams = data["grand_slams"]

        stat_obj.last_updated = datetime.now(timezone.utc)
        
        db.commit()
        print(f"-> Dati di '{did}' salvati in locale nel DB SQL!")
        
    db.close()
    print("✅ Initial Seeding Stats completato!")

if __name__ == "__main__":
    seed_driver_stats()