import requests
import time
from sqlalchemy.orm import Session
from datetime import datetime, timezone
import argparse
import json
from collections import defaultdict

from .database import SessionLocal
from .models import DriverCareerStats, DriverSeasonStats, ConstructorCareerStats, ConstructorSeasonStats, DriverStandingCache, ConstructorStandingCache, RoundProcessingLog

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

def add_stat(deltas, item_id, stat, amount=1):
    deltas[item_id][stat] += amount

def update_best(deltas, item_id, stat_name, current_val_str, new_val_int):
    if current_val_str is None:
        current_val_str = "N/A"
    current_val = int(current_val_str) if current_val_str != "N/A" else 999
    if new_val_int < current_val:
        if f"{stat_name}_prev" not in deltas[item_id]:
            deltas[item_id][f"{stat_name}_prev"] = current_val_str
        deltas[item_id][stat_name] = str(new_val_int)

def apply_revert(db, log: RoundProcessingLog):
    print(f"⚠️ TROVATO LOG PER IL ROUND {log.round_number}. ESECUZIONE ROLLBACK IN CORSO...")
    
    def revert_model(model_class, deltas_json, id_field, is_season=False):
        deltas = json.loads(deltas_json)
        for item_id, changes in deltas.items():
            query = db.query(model_class).filter(getattr(model_class, id_field) == item_id)
            if is_season:
                query = query.filter(model_class.year == log.year)
            obj = query.first()
            
            if obj:
                for k, v in changes.items():
                    if k.endswith("_prev"):
                        original_field = k.replace("_prev", "")
                        setattr(obj, original_field, v)
                    elif not isinstance(v, str): # E' una addizione matematica, quindi sottraiamo
                        current = getattr(obj, k)
                        setattr(obj, k, max(0, (current if current is not None else 0) - v)) # max 0 per sicurezza

    revert_model(DriverSeasonStats, log.driver_season_deltas, "driver_id", True)
    revert_model(DriverCareerStats, log.driver_career_deltas, "driver_id", False)
    revert_model(ConstructorSeasonStats, log.constructor_season_deltas, "constructor_id", True)
    revert_model(ConstructorCareerStats, log.constructor_career_deltas, "constructor_id", False)

    db.delete(log)
    db.commit()
    print("✅ ROLLBACK COMPLETATO. PRONTO PER L'APPLICAZIONE DEI NUOVI DATI.")

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

    # Controllo IDEMPOTENZA tramite Tabella di Supporto (Log)
    existing_log = db.query(RoundProcessingLog).filter_by(round_number=round_number, year=YEAR).first()
    if existing_log:
        apply_revert(db, existing_log)

    r_res = race_results[0].get("Results", [])
    q_res = quali_results[0].get("QualifyingResults", []) if quali_results else []
    s_res = sprint_results[0].get("SprintResults", []) if sprint_results else []

    team_race_h2h = {}
    team_quali_h2h = {}
    team_sprint_h2h = {}
    team_dnfs = {}

    ds_deltas = defaultdict(lambda: defaultdict(int))
    dc_deltas = defaultdict(lambda: defaultdict(int))
    cs_deltas = defaultdict(lambda: defaultdict(int))
    cc_deltas = defaultdict(lambda: defaultdict(int))

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

        if c_id not in team_race_h2h: team_race_h2h[c_id] = []
        team_race_h2h[c_id].append({"id": d_id, "pos": pos if not is_retirement else 999, "points": points})

        if c_id not in team_dnfs: team_dnfs[c_id] = 0
        if is_retirement: team_dnfs[c_id] += 1

        d_season = db.query(DriverSeasonStats).filter_by(driver_id=d_id, year=YEAR).first() or DriverSeasonStats(driver_id=d_id, year=YEAR)
        d_career = db.query(DriverCareerStats).filter_by(driver_id=d_id).first() or DriverCareerStats(driver_id=d_id)
        c_season = db.query(ConstructorSeasonStats).filter_by(constructor_id=c_id, year=YEAR).first() or ConstructorSeasonStats(constructor_id=c_id, year=YEAR)
        c_career = db.query(ConstructorCareerStats).filter_by(constructor_id=c_id).first() or ConstructorCareerStats(constructor_id=c_id)

        add_stat(ds_deltas, d_id, "total_races")
        add_stat(dc_deltas, d_id, "total_races")
        add_stat(cs_deltas, c_id, "total_races")
        add_stat(cc_deltas, c_id, "total_races")
        add_stat(cc_deltas, c_id, "total_points", points)

        if pos == 1:
            add_stat(ds_deltas, d_id, "wins")
            add_stat(dc_deltas, d_id, "wins")
            add_stat(cs_deltas, c_id, "wins")
            add_stat(cc_deltas, c_id, "wins")
            if grid == 1: add_stat(dc_deltas, d_id, "wins_from_pole")
            if grid == 1 and is_fastest_lap: add_stat(dc_deltas, d_id, "hat_tricks")
        elif pos == 2:
            add_stat(ds_deltas, d_id, "second_places")
        
        if pos <= 3:
            add_stat(ds_deltas, d_id, "podiums")
            add_stat(dc_deltas, d_id, "podiums")
            add_stat(cs_deltas, c_id, "podiums")
            add_stat(cc_deltas, c_id, "podiums")

        if is_fastest_lap:
            add_stat(ds_deltas, d_id, "fastest_laps")
            add_stat(dc_deltas, d_id, "fastest_laps")
            add_stat(cs_deltas, c_id, "fastest_laps")
            add_stat(cc_deltas, c_id, "fastest_laps")

        if is_retirement:
            add_stat(ds_deltas, d_id, "retirements")
            if is_dnf: add_stat(dc_deltas, d_id, "dnf_count")
            if is_dns: add_stat(dc_deltas, d_id, "dns_count")
            if is_dsq: 
                add_stat(dc_deltas, d_id, "dsq_count")
                add_stat(ds_deltas, d_id, "dsqs")
        
        if is_dnf or is_dns:
            add_stat(cs_deltas, c_id, "retirements")
        if is_dsq:
            add_stat(cs_deltas, c_id, "dsqs")

        update_best(ds_deltas, d_id, "best_race_result", d_season.best_race_result, pos)
        update_best(dc_deltas, d_id, "best_race_result", d_career.best_race_result, pos)
        update_best(cc_deltas, c_id, "best_race_result", c_career.best_race_result, pos)

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

        d_career = db.query(DriverCareerStats).filter_by(driver_id=d_id).first() or DriverCareerStats(driver_id=d_id)

        if pos == 1:
            add_stat(ds_deltas, d_id, "pole_positions")
            add_stat(dc_deltas, d_id, "pole_positions")
        if pos <= 2:
            add_stat(ds_deltas, d_id, "front_rows")
        
        update_best(dc_deltas, d_id, "best_grid_position", d_career.best_grid_position, pos)

        if res.get("Q1"): add_stat(ds_deltas, d_id, "q1_appearances")
        if res.get("Q2"): add_stat(ds_deltas, d_id, "q2_appearances")
        if res.get("Q3"): add_stat(ds_deltas, d_id, "q3_appearances")

        if res.get("Q3"): add_stat(cs_deltas, c_id, "q3_for_team")
        if res.get("Q2"): add_stat(cs_deltas, c_id, "q2_for_team")
        if res.get("Q1"): add_stat(cs_deltas, c_id, "q1_for_team")

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

            d_career = db.query(DriverCareerStats).filter_by(driver_id=d_id).first() or DriverCareerStats(driver_id=d_id)

            add_stat(ds_deltas, d_id, "sprint_starts")
            add_stat(dc_deltas, d_id, "sprint_starts")
            add_stat(ds_deltas, d_id, "sprint_points", int(points))
            add_stat(cc_deltas, c_id, "total_points", points)
            add_stat(cs_deltas, c_id, "sprint_points", int(points))

            if pos == 1:
                add_stat(cs_deltas, c_id, "sprint_wins")
                add_stat(ds_deltas, d_id, "sprint_wins")
                add_stat(dc_deltas, d_id, "sprint_wins")
            if pos <= 3:
                add_stat(cs_deltas, c_id, "sprint_podiums")
                add_stat(ds_deltas, d_id, "sprint_top_3")
                add_stat(dc_deltas, d_id, "sprint_top_3")
            if points > 0:
                add_stat(ds_deltas, d_id, "sprint_points_finishes")
            if grid == 1:
                add_stat(ds_deltas, d_id, "sprint_quali_poles")

            update_best(dc_deltas, d_id, "best_sprint_result", d_career.best_sprint_result, pos)
            if grid > 0:
                update_best(dc_deltas, d_id, "best_sprint_grid_position", d_career.best_sprint_grid_position, grid)

    # ==========================================
    # CALCOLI INCROCIATI (H2H E DOPPIETTE)
    # ==========================================
    print("Calcolando Head-to-Head e Statistiche di Scuderia...")
    def process_h2h(h2h_dict, stat_field):
        for team_id, drivers in h2h_dict.items():
            if len(drivers) >= 2:
                drivers.sort(key=lambda x: x["pos"])
                winner_id = drivers[0]["id"]
                add_stat(ds_deltas, winner_id, stat_field)

    process_h2h(team_race_h2h, "beat_teammate_race")
    process_h2h(team_quali_h2h, "beat_teammate_quali")
    process_h2h(team_sprint_h2h, "beat_teammate_sprint")

    for team_id, drivers in team_race_h2h.items():
        positions = sorted([d["pos"] for d in drivers])
        if len(positions) >= 2 and positions[0] == 1 and positions[1] == 2:
            add_stat(cs_deltas, team_id, "one_two_finishes")
        
        if team_dnfs.get(team_id, 0) >= 2:
            add_stat(cs_deltas, team_id, "double_dnfs")
            
        total_team_points = sum(d.get("points", 0) for d in drivers)
        if total_team_points > 0:
            add_stat(cs_deltas, team_id, "races_in_points")

    for team_id, changes in cs_deltas.items():
        if changes.get("q3_for_team", 0) >= 2: add_stat(cs_deltas, team_id, "double_q3")
        if changes.get("q2_for_team", 0) >= 2: add_stat(cs_deltas, team_id, "double_q2")
        if changes.get("q1_for_team", 0) >= 2: add_stat(cs_deltas, team_id, "double_q1")
        changes.pop("q3_for_team", None); changes.pop("q2_for_team", None); changes.pop("q1_for_team", None)
    
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
        add_stat(cs_deltas, pole_team, "pole_positions")
        add_stat(cc_deltas, pole_team, "pole_positions")

    if p1_team and p2_team and p1_team == p2_team:
        add_stat(cs_deltas, p1_team, "front_rows")
        
    # ==========================================
    # APPLICAZIONE MODIFICHE E SALVATAGGIO LOG
    # ==========================================
    print("Salvataggio Dati nel Database e Creazione Log di Supporto...")
    def apply_and_save(model_class, deltas, id_field, is_season=False):
        for item_id, changes in deltas.items():
            query = db.query(model_class).filter(getattr(model_class, id_field) == item_id)
            if is_season: query = query.filter(model_class.year == YEAR)
            obj = query.first()
            if not obj:
                obj = model_class(**{id_field: item_id})
                if is_season: obj.year = YEAR
                db.add(obj)
            
            for k, v in changes.items():
                if k.endswith("_prev"): continue
                if isinstance(v, str): # field testo "best_"
                    setattr(obj, k, v)
                else:
                    current = getattr(obj, k)
                    setattr(obj, k, (current if current is not None else 0) + v)

    apply_and_save(DriverSeasonStats, ds_deltas, "driver_id", True)
    apply_and_save(DriverCareerStats, dc_deltas, "driver_id", False)
    apply_and_save(ConstructorSeasonStats, cs_deltas, "constructor_id", True)
    apply_and_save(ConstructorCareerStats, cc_deltas, "constructor_id", False)

    # Creazione del nuovo LOG per eventuali futuri rollback!
    new_log = RoundProcessingLog(
        round_number=round_number,
        year=YEAR,
        driver_season_deltas=json.dumps(ds_deltas),
        driver_career_deltas=json.dumps(dc_deltas),
        constructor_season_deltas=json.dumps(cs_deltas),
        constructor_career_deltas=json.dumps(cc_deltas)
    )
    db.add(new_log)

    # ==========================================
    # PULIZIA CACHE CLASSIFICHE
    # ==========================================
    print("Svuotamento cache delle classifiche per forzare l'aggiornamento al prossimo avvio dell'app...")
    db.query(DriverStandingCache).delete()
    db.query(ConstructorStandingCache).delete()

    db.commit()
    db.close()
    print("✅ AGGIORNAMENTO COMPLETATO CON SUCCESSO! IL DATABASE È AGGIORNATO.")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Aggiorna le statistiche post-gara")
    parser.add_argument("round", type=int, help="Il numero del round appena concluso (es. 1 per Melbourne)")
    args = parser.parse_args()
    update_round(args.round)