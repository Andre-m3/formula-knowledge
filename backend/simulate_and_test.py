import os
import json
import unittest
from unittest.mock import patch, MagicMock
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

# Modifichiamo il path per permettere l'import da 'app'
import sys
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

from app.database import Base
from app.models import DriverSeasonStats, ConstructorSeasonStats, RoundProcessingLog
from app.seed import seed_database
from app.update_post_race import update_round

TEST_DB_FILE = "test_sandbox.db"

def mock_fetch_api(url):
    """
    Questa funzione sostituisce la chiamata API reale.
    Legge i dati da file JSON locali a seconda dell'URL richiesto.
    """
    print(f"  [MOCK] Intercettata richiesta API per: {url.split('/')[-1]}")
    if "results.json" in url:
        file_path = "tests/data/fake_race_1.json"
    elif "qualifying.json" in url:
        file_path = "tests/data/fake_quali_1.json"
    elif "sprint.json" in url:
        file_path = "tests/data/fake_sprint_1.json"
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

        # Creiamo un engine e una sessione SPECIFICI per il test
        cls.engine = create_engine(f"sqlite:///./{TEST_DB_FILE}", connect_args={"check_same_thread": False})
        cls.TestSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=cls.engine)

        # Patchiamo il SessionLocal usato dagli script per puntare al nostro DB di test
        with patch('app.database.SessionLocal', cls.TestSessionLocal):
            with patch('app.seed.engine', cls.engine): # Anche l'engine per il drop/create
                print("\n--- 1. Creazione e Seeding del Database di Test ---")
                seed_database()

            # Usiamo il mock per intercettare le chiamate API
            with patch('app.update_post_race.fetch_api', side_effect=mock_fetch_api):
                print("\n--- 2. Esecuzione Script di Aggiornamento (Round 1) ---")
                update_round(1)

    def setUp(self):
        """ Eseguito prima di OGNI test """
        self.db = self.TestSessionLocal()

    def tearDown(self):
        """ Eseguito dopo OGNI test """
        self.db.close()

    def test_01_haas_stats_updated(self):
        print("\n--- 3. Verifica Statistiche Scuderia (Haas) ---")
        haas_stats = self.db.query(ConstructorSeasonStats).filter_by(constructor_id="haas").first()
        self.assertIsNotNone(haas_stats)
        self.assertEqual(haas_stats.wins, 1)
        self.assertEqual(haas_stats.podiums, 2)
        self.assertEqual(haas_stats.one_two_finishes, 1)
        self.assertEqual(haas_stats.pole_positions, 1)
        print("✅  Vittorie, Podi, Doppietta e Pole Haas corrette.")

    def test_02_bearman_stats_updated(self):
        print("\n--- 4. Verifica Statistiche Pilota (Bearman) ---")
        bearman_stats = self.db.query(DriverSeasonStats).filter_by(driver_id="bearman").first()
        self.assertIsNotNone(bearman_stats)
        self.assertEqual(bearman_stats.wins, 1)
        self.assertEqual(bearman_stats.podiums, 1)
        self.assertEqual(bearman_stats.pole_positions, 1)
        self.assertEqual(bearman_stats.fastest_laps, 1)
        self.assertEqual(bearman_stats.beat_teammate_race, 1)
        self.assertEqual(bearman_stats.beat_teammate_quali, 1)
        print("✅  Vittorie, Podi, Pole e H2H di Bearman corretti.")

    def test_03_rollback_log_created(self):
        print("\n--- 5. Verifica Creazione Log di Rollback ---")
        log = self.db.query(RoundProcessingLog).filter_by(round_number=1).first()
        self.assertIsNotNone(log)
        deltas = json.loads(log.constructor_season_deltas)
        self.assertEqual(deltas["haas"]["wins"], 1)
        print("✅  Log di supporto per il Round 1 creato correttamente.")

if __name__ == '__main__':
    print("======================================================")
    print("🚀 AVVIO TEST DI SIMULAZIONE POST-GARA 🚀")
    print("======================================================")
    unittest.main(verbosity=0)