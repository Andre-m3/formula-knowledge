import os
import json
import unittest
from unittest.mock import patch
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

# Calcoliamo dinamicamente la cartella "tests" in cui si trova questo script
BASE_DIR = os.path.dirname(os.path.abspath(__file__))

# Modifichiamo il path per permettere l'import da 'app'
import sys
sys.path.insert(0, os.path.abspath(os.path.join(BASE_DIR, '..')))

from app.database import Base
from app.models import DriverSeasonStats, ConstructorSeasonStats, RoundProcessingLog
from app.seed import seed_database
from app.update_post_race import update_round

# Usiamo un file locale semplice per evitare i bug dei path assoluti di Windows con SQLite
TEST_DB_FILE = "test_sandbox.db"


def mock_fetch_api(url):
    """
    Questa funzione sostituisce la chiamata API reale.
    Legge i dati da file JSON locali a seconda dell'URL richiesto.
    """
    print(f"  [MOCK] Intercettata richiesta API per: {url.split('/')[-1]}")
    if "results.json" in url:
        file_path = os.path.join(BASE_DIR, "data", "fake_race_1.json")
    elif "qualifying.json" in url:
        file_path = os.path.join(BASE_DIR, "data", "fake_quali_1.json")
    elif "sprint.json" in url:
        file_path = os.path.join(BASE_DIR, "data", "fake_sprint_1.json")
    else:
        return {}
    
    with open(file_path, 'r') as f:
        return json.load(f).get("MRData", {})

class TestPostRaceUpdate(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        """ Eseguito una sola volta prima di tutti i test """
        if os.path.exists(TEST_DB_FILE):
            os.remove(TEST_DB_FILE)

        # Creiamo un engine e una sessione SPECIFICI per il test puntando al DB Sandbox
        cls.engine = create_engine(f"sqlite:///{TEST_DB_FILE}", connect_args={"check_same_thread": False})
        cls.TestSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=cls.engine)

        # IL TRUCCO: Patchiamo SessionLocal ESATTAMENTE nei file che lo importano!
        with patch('app.seed.SessionLocal', cls.TestSessionLocal), \
             patch('app.seed.engine', cls.engine), \
             patch('app.update_post_race.SessionLocal', cls.TestSessionLocal), \
             patch('app.update_post_race.fetch_api', side_effect=mock_fetch_api):
             
            print("\n--- 1. Creazione e Seeding del Database di Test ---")
            seed_database()

            print("\n--- 2. Esecuzione Script di Aggiornamento (Round 1) ---")
            update_round(1)

    def setUp(self):
        """ Eseguito prima di OGNI test """
        self.db = self.TestSessionLocal()

    def tearDown(self):
        """ Eseguito dopo OGNI test """
        self.db.close()

    def test_01_antonelli_stats_updated(self):
        print("\n--- 3. Verifica Statistiche Pilota (Antonelli) ---")
        ant_stats = self.db.query(DriverSeasonStats).filter_by(driver_id="antonelli").first()
        self.assertIsNotNone(ant_stats)
        self.assertEqual(ant_stats.wins, 1)
        self.assertEqual(ant_stats.pole_positions, 1)
        self.assertEqual(ant_stats.fastest_laps, 1)
        self.assertEqual(ant_stats.sprint_top_3, 1)
        print("✅  Vittorie, Pole e Fastest Lap di Antonelli corretti (Senza punti extra!).")

    def test_02_haas_team_stats_updated(self):
        print("\n--- 4. Verifica Statistiche Scuderia (Haas) ---")
        haas_stats = self.db.query(ConstructorSeasonStats).filter_by(constructor_id="haas").first()
        self.assertIsNotNone(haas_stats)
        self.assertEqual(haas_stats.wins, 0)
        self.assertEqual(haas_stats.podiums, 1) # Solo Ocon a podio in gara (P2)
        self.assertEqual(haas_stats.sprint_wins, 1) # Ocon vince la sprint
        print("✅  Podi e Sprint Win Haas calcolati correttamente.")

    def test_03_head_to_head_haas(self):
        print("\n--- 5. Verifica Head to Head (Ocon vs Bearman) ---")
        ocon_stats = self.db.query(DriverSeasonStats).filter_by(driver_id="ocon").first()
        self.assertIsNotNone(ocon_stats)
        self.assertEqual(ocon_stats.beat_teammate_race, 1)
        self.assertEqual(ocon_stats.beat_teammate_quali, 1)
        self.assertEqual(ocon_stats.beat_teammate_sprint, 1)
        print("✅  Ocon batte Bearman in tutte le sessioni nel Testa a Testa interno.")

    def test_04_rollback_log_created(self):
        print("\n--- 6. Verifica Creazione Log di Rollback ---")
        log = self.db.query(RoundProcessingLog).filter_by(round_number=1).first()
        self.assertIsNotNone(log)
        deltas = json.loads(log.constructor_season_deltas)
        self.assertEqual(deltas["mercedes"]["wins"], 1)
        print("✅  Log di supporto per il Round 1 creato correttamente con i nuovi dati.")

    def test_05_leclerc_ferrari_stats_updated(self):
        print("\n--- 7. Verifica Statistiche (Leclerc e Ferrari) ---")
        lec_stats = self.db.query(DriverSeasonStats).filter_by(driver_id="leclerc").first()
        self.assertIsNotNone(lec_stats)
        self.assertEqual(lec_stats.podiums, 1) # P3 in gara
        self.assertEqual(lec_stats.sprint_top_3, 1) # P2 in sprint
        self.assertEqual(lec_stats.front_rows, 1) # P2 in qualifica

        fer_stats = self.db.query(ConstructorSeasonStats).filter_by(constructor_id="ferrari").first()
        self.assertIsNotNone(fer_stats)
        self.assertEqual(fer_stats.podiums, 1)
        self.assertEqual(fer_stats.front_rows, 0) # La Ferrari non ha fatto un "Front Row Lockout" (Doppietta sin qualifica)
        print("✅  Podi, Sprint e Prima Fila di Leclerc registrati perfettamente.")

    def test_06_midfield_stats_updated(self):
        print("\n--- 8. Verifica Statistiche Midfield (Colapinto/Alpine, Stroll/Aston) ---")
        col_stats = self.db.query(DriverSeasonStats).filter_by(driver_id="colapinto").first()
        self.assertIsNotNone(col_stats)
        self.assertEqual(col_stats.q2_appearances, 1) # P13 in qualifica
        
        alpine_stats = self.db.query(ConstructorSeasonStats).filter_by(constructor_id="alpine").first()
        self.assertIsNotNone(alpine_stats)
        self.assertEqual(alpine_stats.races_in_points, 1) # Colapinto P9 (2 punti)
        
        stroll_stats = self.db.query(DriverSeasonStats).filter_by(driver_id="stroll").first()
        self.assertIsNotNone(stroll_stats)
        self.assertEqual(stroll_stats.q1_appearances, 1) # P21 in qualifica
        self.assertEqual(stroll_stats.q2_appearances, 0)
        print("✅  Piazzamenti a punti, Q1 e Q2 per il centrogruppo calcolati con esattezza.")

if __name__ == '__main__':
    print("======================================================")
    print("🚀 AVVIO TEST DI SIMULAZIONE POST-GARA 🚀")
    print("======================================================")
    unittest.main(verbosity=0)