# Formula Knowledge

Applicazione Android nativa per lo studio e l'analisi della Formula 1.

Il progetto è composto da:

- un backend Python/FastAPI;
- un database SQLite locale durante lo sviluppo;
- una futura migrazione a PostgreSQL;
- un'app Android Kotlin/Jetpack Compose;
- servizi esterni per calendario, risultati, meteo, news e documenti tecnici FIA.

## Struttura attuale

```text
formula-knowledge/
├── backend/
│   ├── app/
│   │   ├── api/
│   │   │   ├── endpoints.py             # aggregatore dei router
│   │   │   ├── schemas.py               # schemi Pydantic condivisi
│   │   │   ├── dependencies.py          # API key, OAuth2 e Firebase
│   │   │   ├── raceweek.py              # race week, calendario e circuiti
│   │   │   ├── results.py               # risultati e aggiornamenti GP
│   │   │   ├── standings.py             # classifiche
│   │   │   ├── content.py               # news e aggiornamenti tecnici
│   │   │   ├── stats.py                 # statistiche carriera/stagione
│   │   │   └── auth.py                  # profilo e preferenze
│   │   ├── core/
│   │   │   └── config.py                # Configurazione Pydantic Settings
│   │   ├── services/
│   │   │   ├── calendar_service.py
│   │   │   ├── external_api_service.py
│   │   │   ├── fia_scraper.py
│   │   │   └── weather_service.py
│   │   ├── database.py                   # Engine, SessionLocal e Base attuali
│   │   ├── models.py                     # Modelli SQLAlchemy attuali
│   │   ├── main.py                       # Bootstrap FastAPI, Firebase e router
│   │   ├── rss_scraper.py
│   ├── scripts/
│   │   ├── __init__.py
│   │   ├── seed.py
│   │   ├── seed_constructor_stats.py
│   │   ├── seed_driver_stats.py
│   │   ├── seed_season_stats.py
│   │   ├── sync_database.py
│   │   ├── update_champs.py
│   │   └── update_post_race.py
│   ├── tests/
│   │   ├── data/                         # Fixture JSON dei test
│   │   └── simulate_and_test.py
│   ├── formula_knowledge.db
│   ├── requirements.txt
│   └── Dockerfile
├── frontend/
│   ├── app/
│   │   ├── src/main/java/com/formulaknowledge/app/
│   │   │   ├── data/
│   │   │   │   ├── F1ApiService.kt
│   │   │   │   ├── RetrofitClient.kt
│   │   │   │   ├── FormulaRepository.kt
│   │   │   │   ├── FormulaDatabase.kt
│   │   │   │   ├── TokenManager.kt
│   │   │   │   └── response/data classes
│   │   │   ├── ui/
│   │   │   │   ├── UpdatesScreen.kt
│   │   │   │   ├── AuthViewModel.kt
│   │   │   │   ├── CalendarScreen.kt
│   │   │   │   ├── RaceResultsScreen.kt
│   │   │   │   ├── RaceSessionsScreen.kt
│   │   │   │   ├── HeadToHeadScreen.kt
│   │   │   │   ├── PreferencesOnboardingScreen.kt
│   │   │   │   └── WeatherDetailScreen.kt
│   │   │   ├── MainActivity.kt
│   │   │   └── F1Utils.kt
│   │   ├── src/main/res/                  # Icone, flag, circuiti e immagini team/piloti
│   │   ├── build.gradle.kts
│   │   └── google-services.json
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   └── gradlew / gradlew.bat
├── .gitignore
├── .venv/
├── info/
├── requirements.txt                       # Snapshot locale dell'ambiente Python
├── todo.md
└── readme.md
```

## Backend

### Avvio e configurazione

Il backend utilizza attualmente SQLite con URL predefinito:

```text
sqlite:///./formula_knowledge.db
```

Il percorso è relativo alla directory di esecuzione. Per questo motivo i comandi backend devono essere eseguiti dalla cartella `backend`.

Il virtual environment del progetto si trova nella root:

```text
.venv\Scripts\python.exe
```

Attivazione PowerShell:

```powershell
cd C:\CodeProjects\formula-knowledge\formula-knowledge
.\.venv\Scripts\Activate.ps1
cd .\backend
```

Verifica dell'ambiente:

```powershell
python -c "import sys; print(sys.executable)"
python -m pip show sqlalchemy
```

### Avvio Uvicorn

Dalla directory `backend`:

```powershell
python -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

L'opzione `0.0.0.0` consente ai dispositivi della rete locale, incluso il telefono Android, di raggiungere il server.

### Sicurezza delle richieste

Le route API richiedono l'header applicativo:

```text
X-API-Key
```

Le route di autenticazione richiedono inoltre un token Firebase nell'header:

```text
Authorization: Bearer <firebase-token>
```

La chiave applicativa non deve essere considerata un segreto forte perché è distribuita nell'APK Android. Le credenziali Firebase Admin e le chiavi di servizi esterni devono rimanere fuori dal repository e venire caricate tramite ambiente o secret manager in produzione.

## API esterne

### Jolpica / Ergast API

Jolpica è la fonte open-source principale per i dati sportivi F1. Il backend la usa tramite `ExternalApiService` per:

- calendario e località dei Gran Premi;
- orari delle sessioni;
- classifiche piloti e costruttori;
- risultati gara;
- risultati sprint;
- qualifiche;
- statistiche storiche dei piloti.

Il calendario operativo non utilizza più una lista hardcoded locale quando Jolpica non è disponibile. In caso di risposta vuota viene restituito un errore controllato `503`, evitando di mostrare date potenzialmente obsolete.

### Open-Meteo

`WeatherService` usa Open-Meteo per:

- temperatura;
- umidità;
- temperatura percepita;
- vento;
- indice UV;
- probabilità di pioggia;
- previsione giornaliera a cinque giorni.

Il servizio mantiene una cache in memoria di circa trenta minuti.

### FIA e Gemini

`FiaScraperService`:

1. cerca la pagina FIA della stagione;
2. individua il PDF ufficiale delle presentazioni tecniche;
3. scarica ed estrae il testo con PyMuPDF;
4. invia il testo a Gemini;
5. salva gli aggiornamenti tecnici associandoli a gara e team.

Se il documento FIA non è ancora disponibile, il servizio restituisce lo stato `not_ready`.

### Feed RSS

`rss_scraper.py` aggrega feed di Motorsport.com, Sky Sports F1 e Autosport. Filtra i contenuti pertinenti, salva titolo, fonte, URL, immagine e data, e mantiene gli articoli più recenti.

## Endpoint attuali

Le route sono divise per dominio nei moduli sotto `backend/app/api/` e raccolte da `endpoints.py` tramite un unico `APIRouter`, incluso da `backend/app/main.py` senza alcun prefix aggiuntivo. Gli URL pubblici restano invariati e `main.py` si limita al bootstrap dell'applicazione, all'inizializzazione Firebase e alla composizione del router.

| Metodo | Endpoint | Funzione |
|---|---|---|
| GET | `/api/v1/raceweek/current` | Race week corrente o prossima, sessioni e meteo |
| GET | `/api/v1/circuit/{round_number}` | Dettagli del circuito |
| GET | `/api/v1/results/{round_number}/{session_type}` | Risultati gara, sprint o qualifiche |
| GET | `/api/v1/standings/drivers` | Classifica piloti |
| GET | `/api/v1/standings/constructors` | Classifica costruttori |
| GET | `/api/v1/calendar` | Calendario completo |
| GET | `/api/v1/raceweek/updates` | Aggiornamenti tecnici del prossimo GP |
| GET | `/api/v1/results/{round_number}/updates` | Aggiornamenti tecnici di un GP passato |
| GET | `/api/v1/drivers/{driver_id}/stats` | Statistiche carriera pilota |
| GET | `/api/v1/constructors/{constructor_id}/stats` | Statistiche carriera costruttore |
| GET | `/api/v1/drivers/{driver_id}/season_stats` | Statistiche pilota stagione 2026 |
| GET | `/api/v1/constructors/{constructor_id}/season_stats` | Statistiche costruttore stagione 2026 |
| GET | `/api/v1/news` | News recenti |
| GET | `/api/v1/auth/me` | Profilo autenticato |
| PUT | `/api/v1/auth/preferences` | Aggiornamento preferenze utente |

Tutte le route richiedono `X-API-Key`; le route di autenticazione richiedono inoltre il token Firebase. La route specifica `/api/v1/results/{round_number}/updates` viene registrata prima della route generica `/api/v1/results/{round_number}/{session_type}`, così il valore letterale `updates` non viene interpretato erroneamente come tipo di sessione.

## Database e modelli

Il codice operativo usa:

- `backend/app/database.py` per engine, sessioni e `Base`;
- `backend/app/models.py` per i modelli SQLAlchemy.

Le tabelle principali sono:

- `teams`;
- `drivers`;
- `races`;
- `race_results`;
- `technical_updates`;
- `driver_standings_cache`;
- `constructor_standings_cache`;
- `driver_career_stats`;
- `constructor_career_stats`;
- `driver_season_stats`;
- `constructor_season_stats`;
- `round_processing_logs`;
- `news_articles`;
- `users`.

La vecchia cartella `backend/app/models/` è stata rimossa dopo la verifica degli import. Il codice operativo utilizza esclusivamente `backend/app/models.py` e `backend/app/database.py`.

## Frontend Android

Il frontend usa Kotlin, Jetpack Compose, Material 3, Retrofit, OkHttp, Room, DataStore, Coil e Firebase.

### Flusso dati

```text
FastAPI/Jolpica/Open-Meteo/RSS
            ↓
       RetrofitClient
            ↓
       F1ApiService
            ↓
      FormulaRepository
            ↓
       Room Database
            ↓
        Compose UI
```

Il repository aggiorna Room in background e la UI osserva i dati tramite `Flow`. L'app conserva quindi in locale race week, calendario, risultati, classifiche, news e statistiche già scaricate.

### Offline-first

Al primo utilizzo l'app necessita di connessione per scaricare i dati iniziali. Prima di mostrare la Home, l'avvio attende il refresh della race week e del calendario; classifiche, statistiche e news partono poi in background.

Il refresh del calendario salva l'intera stagione in Room. Per ridurre le richieste ripetitive, precarica dettagli circuito e risultati gara dei round passati e del round corrente; i round futuri vengono caricati quando l'utente li apre.

Nei successivi accessi offline, Room può fornire i dati precedentemente scaricati. Gli errori di rete vengono generalmente registrati e i dati locali vengono mantenuti.

La race week è una condizione importante per visualizzare la Home. Se il refresh fallisce, un dato Room già presente può ancora essere usato offline; se non esiste alcuna cache, resta mostrata la splash fino alla disponibilità del dato essenziale.

Il manifest Android esclude il database Room/cache, DataStore e SharedPreferences dai backup cloud e device-transfer. Questo evita che una nuova installazione ripristini una cache obsoleta e faccia apparire per un istante una gara non più corrente.

### URL backend Android

Per emulatore Android Studio, se il backend gira sullo stesso PC:

```text
http://10.0.2.2:8000/
```

Per telefono fisico sulla stessa rete Wi-Fi:

```text
http://<IP_DEL_PC>:8000/
```

Il backend deve essere avviato con `--host 0.0.0.0` e il firewall deve consentire la porta 8000.

## Comandi database

Tutti i comandi si eseguono da `backend`, con il virtual environment attivo.

### Aggiornamento di un singolo round

```powershell
python -m scripts.update_post_race <ROUND>
```

Lo script scarica gara, qualifica e sprint, applica i delta alle statistiche e salva un `RoundProcessingLog` per permettere il rollback/ricalcolo dello stesso round.

### Master sync

```powershell
python -m scripts.sync_database
```

Il comando:

1. ricalcola le statistiche stagionali;
2. ricalcola le statistiche carriera dei piloti;
3. corregge i mondiali;
4. ricalcola le statistiche carriera dei costruttori;
5. svuota la cache delle classifiche;
6. aggiorna le news RSS.

### Seed iniziale o reset completo

```powershell
python -m scripts.seed
python -m scripts.seed_driver_stats
python -m scripts.seed_constructor_stats
```

`scripts.seed` è distruttivo: esegue `drop_all()` e ricrea le tabelle. Non deve essere usato per un normale aggiornamento post-gara senza backup.

### Test backend

Dalla cartella `backend`:

```powershell
python -m unittest discover -s tests -p "simulate_and_test.py"
```

I test usano dati JSON locali e un database sandbox separato.

## Comandi Android

Dalla cartella `frontend`:

```powershell
\.\gradlew.bat test
\.\gradlew.bat assembleDebug
```

L'app può poi essere avviata da Android Studio su dispositivo fisico o emulatore. Dopo ogni modifica a `API_BASE_URL` è necessario ricompilare e reinstallare l'APK.

## Playbook operativo post-gara

### Gara appena conclusa

Dopo che Jolpica ha pubblicato i risultati ufficiali o provvisori, aggiornare il round interessato:

```powershell
python -m scripts.update_post_race <ROUND>
```

Il comando scarica gara, qualifiche e sprint, applica le statistiche e svuota le cache delle classifiche.

### Controllo penalità e squalifiche

Durante le ore successive è possibile ripetere lo stesso comando. Il `RoundProcessingLog` esegue il rollback dei delta precedenti prima di applicare i dati nuovi.

### Penalità tardive

Per una correzione relativa a un singolo round è possibile ripetere `update_post_race`. Per un riallineamento più prudente dell'intera stagione usare:

```powershell
python -m scripts.sync_database
```

### Sprint race

Lo stesso aggiornamento del round può essere eseguito dopo la sprint e nuovamente dopo la gara domenicale. I dati sprint e gara vengono ricalcolati nel pacchetto del round.

### Nuova stagione o reset completo

Usare `scripts.seed` solo per un setup iniziale o un reset deliberato, seguito dai seed delle statistiche. Il comando ricrea le tabelle e non deve essere utilizzato come aggiornamento ordinario.

### Automazione futura

Su un server Linux sarà possibile configurare:

- `update_post_race <ROUND>` a intervalli ravvicinati durante il weekend di gara;
- `sync_database` periodicamente, ad esempio dopo la finestra delle penalità tardive;
- backup del database prima delle procedure distruttive o di riallineamento.

## Avvertenze operative

- Fare un backup di `backend/formula_knowledge.db` prima di seed o modifiche allo schema.
- Non usare `scripts.seed` come comando di aggiornamento ordinario.
- Eseguire sempre Uvicorn dalla cartella `backend`, perché il percorso SQLite è relativo.
- Non committare credenziali Firebase Admin, `.env` o chiavi private.
- Verificare il database corretto quando si usa un SQLite viewer.
- Non cancellare la cartella `backend/tests`: contiene fixture e test utili.

## Modifiche future previste

- introdurre Alembic per le migrazioni;
- consolidare o archiviare definitivamente i vecchi modelli;
- migrare SQLite a PostgreSQL;
- aggiungere endpoint `/health`, logging strutturato e rate limiting prima del deployment pubblico;
- sostituire la navigazione manuale con Navigation Compose;
- migliorare gli stati di errore e sincronizzazione offline;
- aggiungere autorizzazioni backend per AI custom, notifiche, live timing e widget;
- preparare deployment pubblico con HTTPS, secret manager, backup e health check.


## Convenzioni operative dei seed

La stagione operativa è configurata in `backend/app/core/config.py` tramite `F1_SEASON` e può essere sovrascritta dall’ambiente (`F1_SEASON=2026`). In questa fase il valore predefinito è `2026`.

`backend/scripts/seed.py` ricrea il nucleo del database e usa il calendario Jolpica per le gare e le sessioni. I dati statici di squadre, piloti e statistiche storiche sono mantenuti nelle costanti del modulo; il seed non modifica più i dizionari globali durante l’esecuzione. In caso di errore il rollback viene eseguito e l’eccezione viene propagata, così il comando non può apparire riuscito quando il popolamento è incompleto.

`backend/scripts/seed_season_stats.py` ricostruisce le statistiche stagionali dell’anno configurato e salva il risultato al termine dell’elaborazione. `seed_driver_stats.py` e `seed_constructor_stats.py` aggiornano rispettivamente i dati carriera dei piloti e dei costruttori. `sync_database.py` coordina questi script, corregge i campionati, svuota la cache classifiche e aggiorna i feed RSS.

I dati mancanti dei circuiti non devono essere completati con valori inventati: prima si verifica la fonte, poi si aggiunge la voce storica al seed. `scripts.seed` resta un reset distruttivo e non sostituisce `scripts.sync_database` per gli aggiornamenti ordinari.

### Stato della manutenzione seed

La prima fase di refactoring è stata verificata con compilazione Python e suite sandbox: 6 test superati.

- I dati statici dei circuiti restanti sono presenti in `HISTORICAL_DATA` e sono stati verificati manualmente, inclusi i conteggi 2026 e la lunghezza di Sepang (`5.543 km`).
- `scripts.seed` valida i circuiti non cancellati prima del `drop_all()`. Gli alias espliciti servono solo a collegare denominazioni API e dati storici, senza alterare il nome salvato nel calendario.
- `seed_driver_stats.py` e `seed_constructor_stats.py` applicano gli aggiornamenti con un singolo commit, rollback su errore e chiusura garantita della sessione.
- La suite sandbox resta verde (`6/6`); i comportamenti transazionali dei due seed statistici sono stati verificati con sessioni simulate isolate dal database reale.

Restano da affrontare la centralizzazione degli hardcode di stagione ancora presenti in alcuni endpoint/UI, l'eventuale aggiunta di test permanenti dedicati ai seed e il refactor della directory base prima della preparazione Alembic/PostgreSQL.
