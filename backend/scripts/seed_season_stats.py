import requests
import time
from sqlalchemy.orm import Session
from datetime import datetime, timezone

from app.database import SessionLocal
from app.models import DriverSeasonStats, ConstructorSeasonStats
from app.core.config import settings

YEAR = settings.F1_SEASON

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

def seed_season():
    db: Session = SessionLocal()
    print(f"🌱 Avvio Seeding delle Statistiche Stagionali {YEAR}...")

    # Svuotiamo le tabelle stagionali attuali per evitare duplicati
    db.query(DriverSeasonStats).filter(DriverSeasonStats.year == YEAR).delete()
    db.query(ConstructorSeasonStats).filter(ConstructorSeasonStats.year == YEAR).delete()

    for round_number in range(1, 30):
        races = fetch_api(f"https://api.jolpi.ca/ergast/f1/{YEAR}/{round_number}/results.json").get("RaceTable", {}).get("Races", [])
        if not races:
            print(f"🏁 Nessun risultato trovato per il Round {round_number}. Il calendario attuale si ferma qui.")
            break
            
        print(f"\n🏎️  Elaborazione e calcolo metriche Round {round_number}...")

        quali_results = fetch_api(f"https://api.jolpi.ca/ergast/f1/{YEAR}/{round_number}/qualifying.json").get("RaceTable", {}).get("Races", [])
        sprint_results = fetch_api(f"https://api.jolpi.ca/ergast/f1/{YEAR}/{round_number}/sprint.json").get("RaceTable", {}).get("Races", [])

        r_res = races[0].get("Results", [])
        q_res = quali_results[0].get("QualifyingResults", []) if quali_results else []
        s_res = sprint_results[0].get("SprintResults", []) if sprint_results else []

        team_race_h2h = {}
        team_quali_h2h = {}
        team_sprint_h2h = {} # Per H2H Sprint
        team_dnfs = {}
        team_quali_sessions = {}

        # -- RACE --
        for res in r_res:
            d_id = res["Driver"]["driverId"]
            c_id = res["Constructor"]["constructorId"]
            pos = int(res["position"])
            points = float(res["points"])
            status = res.get("status", "").lower()
            pos_text = res.get("positionText", "").upper()
            
            is_fastest_lap = res.get("FastestLap", {}).get("rank") == "1"
            is_dnf = pos_text == "R"
            is_dns = pos_text == "W" or "withdrawn" in status or "did not start" in status
            is_dsq = pos_text == "D" or "disqualified" in status
            is_retirement = is_dnf or is_dns or is_dsq

            if c_id not in team_race_h2h: team_race_h2h[c_id] = []
            team_race_h2h[c_id].append({"id": d_id, "pos": pos if not is_retirement else 999, "points": points})

            if c_id not in team_dnfs: team_dnfs[c_id] = 0
            if is_retirement: team_dnfs[c_id] += 1

            d_season = db.query(DriverSeasonStats).filter_by(driver_id=d_id, year=YEAR).first()
            if not d_season:
                d_season = DriverSeasonStats(driver_id=d_id, year=YEAR)
                db.add(d_season)
                db.flush()

            c_season = db.query(ConstructorSeasonStats).filter_by(constructor_id=c_id, year=YEAR).first()
            if not c_season:
                c_season = ConstructorSeasonStats(constructor_id=c_id, year=YEAR)
                db.add(c_season)
                db.flush()

            d_season.total_races += 1
            if pos == 1:
                d_season.wins += 1
                c_season.wins += 1
            elif pos == 2:
                d_season.second_places += 1
            if pos <= 3:
                d_season.podiums += 1
                c_season.podiums += 1
            if is_fastest_lap:
                d_season.fastest_laps += 1
                c_season.fastest_laps += 1
            if is_retirement:
                d_season.retirements += 1
            if is_dsq:
                d_season.dsqs += 1
            if pos < (int(d_season.best_race_result) if d_season.best_race_result != "N/A" else 999):
                d_season.best_race_result = str(pos)
            if is_dnf or is_dns:
                c_season.retirements += 1
            if is_dsq:
                c_season.dsqs += 1

        # -- QUALI --
        for res in q_res:
            d_id = res["Driver"]["driverId"]
            c_id = res["Constructor"]["constructorId"]
            pos = int(res["position"])
            
            if c_id not in team_quali_sessions: team_quali_sessions[c_id] = {"Q3": 0, "Q2": 0, "Q1": 0}
            if res.get("Q3"): team_quali_sessions[c_id]["Q3"] += 1
            if res.get("Q2"): team_quali_sessions[c_id]["Q2"] += 1
            if res.get("Q1"): team_quali_sessions[c_id]["Q1"] += 1

            if c_id not in team_quali_h2h: team_quali_h2h[c_id] = []
            team_quali_h2h[c_id].append({"id": d_id, "pos": pos})

            d_season = db.query(DriverSeasonStats).filter_by(driver_id=d_id, year=YEAR).first()
            c_season = db.query(ConstructorSeasonStats).filter_by(constructor_id=c_id, year=YEAR).first()

            if d_season and c_season:
                if pos == 1:
                    d_season.pole_positions += 1
                if pos <= 2:
                    d_season.front_rows += 1
                if res.get("Q1"): d_season.q1_appearances += 1
                if res.get("Q2"): d_season.q2_appearances += 1
                if res.get("Q3"): d_season.q3_appearances += 1

        # -- SPRINT --
        if s_res:
            for res in s_res:
                d_id = res["Driver"]["driverId"]
                c_id = res["Constructor"]["constructorId"]
                pos = int(res["position"])
                grid = int(res.get("grid", 0))
                points = float(res.get("points", 0))

                if c_id not in team_sprint_h2h: team_sprint_h2h[c_id] = []
                team_sprint_h2h[c_id].append({"id": d_id, "pos": pos})

                d_season = db.query(DriverSeasonStats).filter_by(driver_id=d_id, year=YEAR).first()
                c_season = db.query(ConstructorSeasonStats).filter_by(constructor_id=c_id, year=YEAR).first()

                if d_season:
                    d_season.sprint_starts += 1
                    d_season.sprint_points += int(points)
                    if pos == 1: d_season.sprint_wins += 1
                    if pos <= 3: d_season.sprint_top_3 += 1
                    if points > 0: d_season.sprint_points_finishes += 1
                    if grid == 1: d_season.sprint_quali_poles += 1
                if c_season:
                    if pos == 1: c_season.sprint_wins += 1
                    if pos <= 3: c_season.sprint_podiums += 1
                    c_season.sprint_points += int(points)

        # -- H2H & TEAM STATS POST-ELABORAZIONE --
        def process_h2h(h2h_dict, stat_field):
            for t_id, drivers in h2h_dict.items():
                if len(drivers) >= 2:
                    drivers.sort(key=lambda x: x["pos"])
                    winner_id = drivers[0]["id"]
                    db_stat = db.query(DriverSeasonStats).filter_by(driver_id=winner_id, year=YEAR).first()
                    if db_stat: setattr(db_stat, stat_field, getattr(db_stat, stat_field) + 1)

        process_h2h(team_race_h2h, "beat_teammate_race")
        process_h2h(team_quali_h2h, "beat_teammate_quali")
        process_h2h(team_sprint_h2h, "beat_teammate_sprint")

        for team_id, drivers in team_race_h2h.items():
            c_season = db.query(ConstructorSeasonStats).filter_by(constructor_id=team_id, year=YEAR).first()
            if c_season:
                c_season.total_races += 1
                positions = sorted([d["pos"] for d in drivers])
                if len(positions) >= 2 and positions[0] == 1 and positions[1] == 2:
                    c_season.one_two_finishes += 1
                if team_dnfs.get(team_id, 0) >= 2:
                    c_season.double_dnfs += 1
                if sum(d.get("points", 0) for d in drivers) > 0:
                    c_season.races_in_points += 1

        for team_id, sessions in team_quali_sessions.items():
            c_season = db.query(ConstructorSeasonStats).filter_by(constructor_id=team_id, year=YEAR).first()
            if c_season:
                if sessions["Q3"] >= 2: c_season.double_q3 += 1
                if sessions["Q2"] >= 2: c_season.double_q2 += 1
                if sessions["Q1"] >= 2: c_season.double_q1 += 1
        
        # Correzione conteggio pole e front_rows per costruttori
        pole_team = None
        p1_team = None
        p2_team = None
        
        for res in q_res:
            pos = int(res['position'])
            team_id = res['Constructor']['constructorId']
            if pos == 1:
                pole_team = team_id
                p1_team = team_id
            elif pos == 2:
                p2_team = team_id
        
        if pole_team:
            c_season = db.query(ConstructorSeasonStats).filter_by(constructor_id=pole_team, year=YEAR).first()
            if c_season: c_season.pole_positions += 1

        if p1_team and p2_team and p1_team == p2_team:
            c_season = db.query(ConstructorSeasonStats).filter_by(constructor_id=p1_team, year=YEAR).first()
            if c_season: c_season.front_rows += 1

    db.commit()
    db.close()
    print(f"🎉 Seeding Stagione {YEAR} completato con successo!")

if __name__ == "__main__":
    seed_season()