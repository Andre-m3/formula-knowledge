import argparse
from sqlalchemy.orm import Session

from app.database import SessionLocal
from app.models import DriverStandingCache, ConstructorStandingCache

# Importiamo le funzioni dei nostri script di seeding
from .seed_season_stats import seed_season
from .seed_driver_stats import seed_driver_stats
from .seed_constructor_stats import seed_constructor_stats
from .update_champs import fix_world_championships
from .sync_session_results import sync_session_results
from app.rss_scraper import run_scraper
from app.core.config import settings

def run_master_sync():
    print("\n=======================================================")
    print("🚀 AVVIO SINCRONIZZAZIONE MASTER (IDEMPOTENTE)")
    print("=======================================================\n")
    print("Questo script è progettato per essere eseguito post-gara.")
    print("Ricalcola tutto da zero, gestendo in automatico qualsiasi")
    print("squalifica o penalità FIA avvenuta ore o giorni dopo.\n")

    # 1. Risultati canonici: le sessioni complete vengono riconciliate
    # prima delle statistiche. Risposte mancanti/non valide non cancellano nulla.
    print(f"--> 1. SINCRONIZZAZIONE RISULTATI SESSIONE {settings.F1_SEASON}...")
    db: Session = SessionLocal()
    try:
        session_summary = sync_session_results(db, apply=True)
    finally:
        db.close()
    print(
        "    Risultati: "
        f"{session_summary.sessions_written} sessioni scritte, "
        f"{session_summary.sessions_unchanged} invariate, "
        f"{session_summary.sessions_unavailable} non disponibili, "
        f"{session_summary.sessions_rate_limited} rate limited."
    )

    # 2. Ricalcolo Statistiche Stagione (Cancella e ricrea l'anno in corso)
    print(f"\n--> 2. RICALCOLO STAGIONE {settings.F1_SEASON}...")
    seed_season()
    # 3. Ricalcolo Carriera Piloti (Sostituisce i dati esistenti)
    print("\n--> 3. RICALCOLO CARRIERA PILOTI...")
    seed_driver_stats()
    # Fissiamo i mondiali (per evitare il conteggio della stagione corrente non finita)
    fix_world_championships()

    # 4. Ricalcolo Carriera Costruttori
    print("\n--> 4. RICALCOLO STORICO COSTRUTTORI...")
    seed_constructor_stats()

    # 5. Svuotamento Cache Classifiche (Per forzare l'app a prendere i Punti freschi)
    print("\n--> 5. RESET CACHE PUNTI E CLASSIFICHE...")
    db: Session = SessionLocal()
    db.query(DriverStandingCache).delete()
    db.query(ConstructorStandingCache).delete()
    db.commit()
    db.close()
    
    # 6. Sincronizzazione Feed RSS (Per aggiornare le notizie post-gara)
    print("\n--> 6. SINCRONIZZAZIONE FEED RSS...")
    run_scraper()

    print("\n=======================================================")
    print("✅ SINCRONIZZAZIONE MASTER COMPLETATA CON SUCCESSO!")
    print("Il database ora riflette i risultati ufficiali e definitivi.")
    print("=======================================================\n")

if __name__ == "__main__":
    run_master_sync()