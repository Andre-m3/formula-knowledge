import argparse
from sqlalchemy.orm import Session

from .database import SessionLocal
from .models import DriverStandingCache, ConstructorStandingCache

# Importiamo le funzioni dei nostri script di seeding
from .seed_season_stats import seed_season
from .seed_driver_stats import seed_driver_stats
from .seed_constructor_stats import seed_constructor_stats
from .update_champs import fix_world_championships

def run_master_sync():
    print("\n=======================================================")
    print("🚀 AVVIO SINCRONIZZAZIONE MASTER (IDEMPOTENTE)")
    print("=======================================================\n")
    print("Questo script è progettato per essere eseguito post-gara.")
    print("Ricalcola tutto da zero, gestendo in automatico qualsiasi")
    print("squalifica o penalità FIA avvenuta ore o giorni dopo.\n")

    # 1. Ricalcolo Statistiche Stagione (Cancella e ricrea l'anno in corso)
    print("--> 1. RICALCOLO STAGIONE 2026...")
    seed_season()

    # 2. Ricalcolo Carriera Piloti (Sostituisce i dati esistenti)
    print("\n--> 2. RICALCOLO CARRIERA PILOTI...")
    seed_driver_stats()
    # Fissiamo i mondiali (per evitare il conteggio della stagione corrente non finita)
    fix_world_championships()

    # 3. Ricalcolo Carriera Costruttori
    print("\n--> 3. RICALCOLO STORICO COSTRUTTORI...")
    seed_constructor_stats()

    # 4. Svuotamento Cache Classifiche (Per forzare l'app a prendere i Punti freschi)
    print("\n--> 4. RESET CACHE PUNTI E CLASSIFICHE...")
    db: Session = SessionLocal()
    db.query(DriverStandingCache).delete()
    db.query(ConstructorStandingCache).delete()
    db.commit()
    db.close()

    print("\n=======================================================")
    print("✅ SINCRONIZZAZIONE MASTER COMPLETATA CON SUCCESSO!")
    print("Il database ora riflette i risultati ufficiali e definitivi.")
    print("=======================================================\n")

if __name__ == "__main__":
    run_master_sync()