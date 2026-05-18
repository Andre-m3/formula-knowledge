# 📖 MANUALE OPERATIVO BACKEND - Formula Knowledge

Questo documento descrive le procedure standard e di emergenza per mantenere il database di Formula Knowledge sempre sincronizzato con i risultati reali del campionato mondiale di F1, sfruttando l'architettura a **Rollback Log** e **Master Sync**.

## 🛠️ Glossario dei Comandi

Tutti i comandi vanno eseguiti dalla cartella root del backend (`c:\CodeProjects\formula-knowledge\formula-knowledge\backend\`), avendo l'ambiente virtuale Python attivato.

*   `python -m app.update_post_race <ROUND>`: È il tuo "cavallo di battaglia". Scarica i risultati del round specificato, calcola le statistiche (deltas) e le applica al DB. Se il round era già stato calcolato, annulla automaticamente le modifiche precedenti (Rollback) prima di applicare le nuove.
*   `python -m app.sync_database`: Il "Bottone Rosso". Cancella la cache delle classifiche, azzera la stagione in corso, e ricalcola tutto partendo dai dati API certificati. Completamente *Idempotente*.
*   `python -m app.seed`: Ripristina il database partendo da zero, caricando i team, i piloti e i dati storici salvati a mano. Da usare solo in fase di setup iniziale o per reset drastici.

---

## 🏎️ Playbook: Scenari Post-Gara

Ecco esattamente cosa fare in ogni situazione durante il weekend di gara.

### Scenario A: La Gara è appena finita (Normale amministrazione)
La gara si conclude (es. alle 16:30). La FIA pubblica i risultati provvisori e Jolpica li rende disponibili tramite API.

*   **Quando:** Circa 30-60 minuti dopo la bandiera a scacchi (es. 17:30).
*   **Azione (Manuale o Cron Job):**
    ```bash
    python -m app.update_post_race 6
    ```
    *(Sostituisci `6` con il round corrente).*
*   **Cosa succede:** Lo script calcola vittorie, podi e testa a testa per la gara 6, aggiorna il database e svuota la cache delle classifiche. Appena gli utenti apriranno l'app, vedranno le nuove statistiche e la nuova classifica fresca di giornata.

### Scenario B: Finestra di Controllo Penalità (0 - 8 ore post-gara)
Nelle ore successive alla gara, i commissari pesano le vetture e analizzano le telemetrie. Potrebbero fioccare penalità in tempo (es. 5 secondi) o squalifiche (DSQ).

*   **Quando:** Ogni ora, per le 8 ore successive alla gara.
*   **Azione (Cron Job Automatizzato raccomandato):**
    ```bash
    python -m app.update_post_race 6
    ```
*   **Cosa succede:** Lo script si accorge di aver già processato il round 6. Legge il `RoundProcessingLog` dal database, **sottrae** esattamente i punti e i podi che aveva assegnato alle 17:30, scarica la nuova classifica da Jolpica aggiornata con le penalità, e applica i nuovi dati. Tutto avviene in millisecondi in modo del tutto trasparente.

### Scenario C: Penalità o Squalifiche Tardive (> 24 ore dopo)
Caso rarissimo ma possibile (es. ricorso accettato giorni dopo, o squalifica per irregolarità tecnica scoperta il Martedì).

*   **Quando:** Appena la notizia diventa ufficiale e Jolpica aggiorna le proprie API.
*   **Azione:** Non farti prendere dal panico. Hai due opzioni, entrambe valide:
    1.  **Opzione Rapida:** Esegui di nuovo `python -m app.update_post_race <ROUND>`. L'intelligenza del Rollback Log sistemerà tutto.
    2.  **Opzione Sicura (Consigliata):**
        ```bash
        python -m app.sync_database
        ```
*   **Cosa succede:** Usando `sync_database`, il sistema si prende 20-30 secondi per rileggere da zero tutto il campionato 2026. Qualsiasi squisitezza regolamentare viene piallata e ricalcolata alla perfezione. Gli utenti vedranno i dati corretti all'istante.

### Scenario D: Gestione delle Sprint Race (Sabato)
Il Sabato si corre la Sprint Race. I risultati danno punti mondiali e valgono per le statistiche Sprint.

*   **Quando:** 1 ora dopo la fine della Sprint Race.
*   **Azione:**
    ```bash
    python -m app.update_post_race 6
    ```
*   **Cosa succede:** Lo script è intelligente. Leggerà i risultati della Sprint, aggiornerà i punti e le statistiche Sprint, ma lascerà intatte le statistiche di Gara (poiché la gara non c'è ancora stata). La Domenica, eseguendo lo stesso comando, lo script annullerà le vecchie modifiche e applicherà un pacchetto unificato (Sprint + Gara) salvandolo nel Log.

### Scenario E: Inizio di una Nuova Stagione / Setup DB
*   **Quando:** All'inizio del progetto, o in caso di migrazione server.
*   **Azione:**
    ```bash
    python -m app.seed
    python -m app.seed_driver_stats
    python -m app.seed_constructor_stats
    ```
*   **Cosa succede:** Il database viene ricostruito e vengono inseriti i dati pre-calcolati (storico dal 1950 al 2025).

---

## 🤖 Suggerimenti per l'Automazione (Cron Jobs)

Se ospiti il backend su un server Linux (es. Ubuntu, AWS, DigitalOcean), non dovrai lanciare questi comandi a mano. Ecco la configurazione ottimale del `crontab`:

1.  **Aggiornamento Gara/Sprint Frequente (Domenica e Sabato):**
    Imposta un cron che esegua `python -m app.update_post_race <SCRIPT_CHE_CALCOLA_IL_ROUND>` ogni ora, dalle 16:00 alle 23:59 del Sabato e della Domenica. Questo copre perfettamente lo "Scenario A" e lo "Scenario B".
2.  **Master Sync Settimanale (Martedì Notte):**
    Imposta un cron che esegua `python -m app.sync_database` ogni Martedì alle 03:00 del mattino. Questo fa da "spazzino", assicurandosi che eventuali penalità postume del Lunedì vengano recepite e consolidando in modo inattaccabile il database.