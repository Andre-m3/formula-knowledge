import requests
import time
from sqlalchemy.orm import Session
from datetime import datetime, timezone
import argparse

from .database import SessionLocal
from .models import DriverCareerStats, DriverSeasonStats, ConstructorCareerStats, ConstructorSeasonStats

YEAR = 2026

def fetch_api(url):
    for _ in range(3):
        try:
            res = requests.get(url, timeout=10)
            if res.status_code == 200:
                return res.json().get("MRData", {})
            time.sleep(2)
        except Exception:
            time.sleep(2)
    return {}

def update_round(round_number: int):
    db: Session = SessionLocal()
    print(f"\n🏎️  INIZIO AGGIORNAMENTO DATI PER IL ROUND {round_number} ({YEAR})...")

    # 1. FETCH DATI (Race, Quali, Sprint)
    print("Scaricando i risultati della Gara...")
    race_data = fetch_api(f"https://api.jolpi.ca/ergast/f1/{YEAR}/{round_number}/results.json")
    race_results = race_data.get("RaceTable", {}).get("Races", [])
    
    print("Scaricando i risultati delle Qualifiche...")
    quali_data = fetch_api(f"https://api.jolpi.ca/ergast/f1/{YEAR}/{round_number}/qualifying.json")
    quali_results = quali_data.get("RaceTable", {}).get("Races", [])

    print("Scaricando i risultati della Sprint (se esiste)...")
    sprint_data = fetch_api(f"https://api.jolpi.ca/ergast/f1/{YEAR}/{round_number}/sprint.json")
    sprint_results = sprint_data.get("RaceTable", {}).get("Races", [])

    if not race_results:
        print("❌ Nessun risultato di gara trovato. Gara non ancora disputata?")
        db.close()
        return

    r_res = race_results[0].get("Results", [])
    q_res = quali_results[0].get("QualifyingResults", []) if quali_results else []
    s_res = sprint_results[0].get("SprintResults", []) if sprint_results else []

    # Strutture per calcolare Head 2 Head (H2H) e Statistiche Team
    team_race_h2h = {}
    team_quali_h2h = {}
    team_sprint_h2h = {}
    team_dnfs = {}

    # ==========================================
    # ELABORAZIONE GARA (RACE)
    # ==========================================
    print("\nElaborazione Risultati Gara...")
    for res in r_res:
        d_id = res["Driver"]["driverId"]
        c_id = res["Constructor"]["constructorId"]
        pos = int(res["position"])
        grid = int(res.get("grid", 0))
        points = float(res["points"])
        status = res.get("status", "").lower()
        pos_text = res.get("positionText", "").upper()
        
        is_fastest_lap = res.get("FastestLap", {}).get("rank") == "1"
        is_dnf = pos_text == "R"
        is_dns = pos_text == "W" or "withdrawn" in status or "did not start" in status
        is_dsq = pos_text == "D" or "disqualified" in status
        is_retirement = is_dnf or is_dns or is_dsq

        # H2H Setup
        if c_id not in team_race_h2h: team_race_h2h[c_id] = []
        team_race_h2h[c_id].append({"id": d_id, "pos": pos if not is_retirement else 999})

        if c_id not in team_dnfs: team_dnfs[c_id] = 0
        if is_retirement: team_dnfs[c_id] += 1

        # Update Driver Stats
        d_season = db.query(DriverSeasonStats).filter_by(driver_id=d_id, year=YEAR).first()
        if not d_season: d_season = DriverSeasonStats(driver_id=d_id, year=YEAR); db.add(d_season)
        
        d_career = db.query(DriverCareerStats).filter_by(driver_id=d_id).first()
        if not d_career: d_career = DriverCareerStats(driver_id=d_id); db.add(d_career)

        d_season.total_races += 1
        d_career.total_races += 1

        if pos == 1:
            d_season.wins += 1
            d_career.wins += 1
            if grid == 1: d_career.wins_from_pole += 1
            if grid == 1 and is_fastest_lap: d_career.hat_tricks += 1
        elif pos == 2:
            d_season.second_places += 1
        
        if pos <= 3:
            d_season.podiums += 1
            d_career.podiums += 1

        if is_fastest_lap:
            d_season.fastest_laps += 1
            d_career.fastest_laps += 1

        if is_retirement:
            d_season.retirements += 1
            if is_dnf: d_career.dnf_count += 1
            if is_dns: d_career.dns_count += 1
            if is_dsq: d_career.dsq_count += 1

        # Best Results
        if pos < (int(d_career.best_race_result) if d_career.best_race_result != "N/A" else 999):
            d_career.best_race_result = str(pos)

        # Update Constructor Stats
        c_season = db.query(ConstructorSeasonStats).filter_by(constructor_id=c_id, year=YEAR).first()
        if not c_season: c_season = ConstructorSeasonStats(constructor_id=c_id, year=YEAR); db.add(c_season)
        
        c_career = db.query(ConstructorCareerStats).filter_by(constructor_id=c_id).first()
        if not c_career: c_career = ConstructorCareerStats(constructor_id=c_id); db.add(c_career)

        c_career.total_points += points
        if pos == 1:
            c_season.wins += 1
            c_career.wins += 1
        if pos <= 3:
            c_season.podiums += 1
            c_career.podiums += 1
        if is_fastest_lap:
            c_season.fastest_laps += 1
            c_career.fastest_laps += 1

        if pos < (int(c_career.best_race_result) if c_career.best_race_result != "N/A" else 999):
            c_career.best_race_result = str(pos)

    # ==========================================
    # ELABORAZIONE QUALIFICHE (QUALI)
    # ==========================================
    print("Elaborazione Risultati Qualifiche...")
    for res in q_res:
        d_id = res["Driver"]["driverId"]
        c_id = res["Constructor"]["constructorId"]
        pos = int(res["position"])
        
        if c_id not in team_quali_h2h: team_quali_h2h[c_id] = []
        team_quali_h2h[c_id].append({"id": d_id, "pos": pos})

        d_season = db.query(DriverSeasonStats).filter_by(driver_id=d_id, year=YEAR).first()
        d_career = db.query(DriverCareerStats).filter_by(driver_id=d_id).first()
        c_season = db.query(ConstructorSeasonStats).filter_by(constructor_id=c_id, year=YEAR).first()
        c_career = db.query(ConstructorCareerStats).filter_by(constructor_id=c_id).first()

        if d_season and d_career and c_season and c_career:
            if pos == 1:
                d_season.pole_positions += 1
                d_career.pole_positions += 1
                c_season.pole_positions += 1
                c_career.pole_positions += 1
            if pos <= 2:
                d_season.front_rows += 1
                c_season.front_rows += 1
            
            if pos < (int(d_career.best_grid_position) if d_career.best_grid_position != "N/A" else 999):
                d_career.best_grid_position = str(pos)

            if res.get("Q1"): d_season.q1_appearances += 1
            if res.get("Q2"): d_season.q2_appearances += 1
            if res.get("Q3"): d_season.q3_appearances += 1

    # ==========================================
    # ELABORAZIONE SPRINT
    # ==========================================
    if s_res:
        print("Elaborazione Risultati Sprint...")
        for res in s_res:
            d_id = res["Driver"]["driverId"]
            c_id = res["Constructor"]["constructorId"]
            pos = int(res["position"])
            grid = int(res.get("grid", 0))
            points = float(res["points"])

            if c_id not in team_sprint_h2h: team_sprint_h2h[c_id] = []
            team_sprint_h2h[c_id].append({"id": d_id, "pos": pos})

            d_season = db.query(DriverSeasonStats).filter_by(driver_id=d_id, year=YEAR).first()
            d_career = db.query(DriverCareerStats).filter_by(driver_id=d_id).first()
            c_career = db.query(ConstructorCareerStats).filter_by(constructor_id=c_id).first()

            if d_season and d_career and c_career:
                d_season.sprint_starts += 1
                d_career.sprint_starts += 1
                d_season.sprint_points += int(points)
                c_career.total_points += points
                
                if pos == 1:
                    d_season.sprint_wins += 1
                    d_career.sprint_wins += 1
                if pos <= 3:
                    d_season.sprint_top_3 += 1
                    d_career.sprint_top_3 += 1
                if points > 0:
                    d_season.sprint_points_finishes += 1
                if grid == 1:
                    d_season.sprint_quali_poles += 1

    # ==========================================
    # CALCOLI INCROCIATI (H2H E DOPPIETTE)
    # ==========================================
    print("Calcolando Head-to-Head e Statistiche di Scuderia...")
    def process_h2h(h2h_dict, stat_field):
        for team_id, drivers in h2h_dict.items():
            if len(drivers) >= 2:
                drivers.sort(key=lambda x: x["pos"])
                winner_id = drivers[0]["id"] # Il pilota con la posizione minore vince
                db_stat = db.query(DriverSeasonStats).filter_by(driver_id=winner_id, year=YEAR).first()
                if db_stat: setattr(db_stat, stat_field, getattr(db_stat, stat_field) + 1)

    process_h2h(team_race_h2h, "beat_teammate_race")
    process_h2h(team_quali_h2h, "beat_teammate_quali")
    process_h2h(team_sprint_h2h, "beat_teammate_sprint")

    # Doppiette e Double DNFs
    for team_id, drivers in team_race_h2h.items():
        c_season = db.query(ConstructorSeasonStats).filter_by(constructor_id=team_id, year=YEAR).first()
        if c_season:
            positions = sorted([d["pos"] for d in drivers])
            if len(positions) >= 2 and positions[0] == 1 and positions[1] == 2:
                c_season.one_two_finishes += 1
            
            if team_dnfs.get(team_id, 0) >= 2:
                c_season.double_dnfs += 1

    db.commit()
    db.close()
    print("✅ AGGIORNAMENTO COMPLETATO CON SUCCESSO! IL DATABASE È AGGIORNATO.")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Aggiorna le statistiche post-gara")
    parser.add_argument("round", type=int, help="Il numero del round appena concluso (es. 1 per Melbourne)")
    args = parser.parse_args()
    update_round(args.round)