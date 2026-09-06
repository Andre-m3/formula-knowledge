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
│   ├── alembic/
│   │   ├── env.py
│   │   ├── script.py.mako
│   │   └── versions/
│   │       ├── 9f34e5026ddc_baseline_schema.py
│   │       └── e71272d84bd5_add_tsunoda_to_driver_roster.py
│   ├── alembic.ini                  # Configurazione migration schema
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
│   │   ├── backfill_practice_results.py # Manutenzione mirata FP1/FP2/FP3
│   │   ├── sync_session_results.py      # Riconcilia RaceResult da Jolpica
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
│   ├── data/
│   │   └── formula_knowledge.db
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
├── docs/
│   └── info/

├── todo.md
└── readme.md
```

## Backend

### Avvio e configurazione

Il backend utilizza attualmente SQLite con URL predefinito:

```text
sqlite:///./data/formula_knowledge.db
```

Il percorso è relativo alla directory di esecuzione. Per questo motivo i comandi backend devono essere eseguiti dalla cartella `backend`.
Il database runtime e i relativi backup locali sono esclusi dal versionamento tramite il `.gitignore` root.

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
- risultati finali di FP1, FP2, FP3 e Sprint Qualifying (SQ1/SQ2/SQ3) tramite il contratto Jolpica Alpha;
- statistiche storiche dei piloti.

Il calendario operativo non utilizza più una lista hardcoded locale quando Jolpica non è disponibile. In caso di risposta vuota viene restituito un errore controllato `503`, evitando di mostrare date potenzialmente obsolete.

`RaceResult` è la fonte canonica dei risultati serviti all'app. `sync_database` esegue per primo una riconciliazione idempotente di Race, Quali, Sprint, Sprint Qualifying e FP dei round conclusi e del weekend corrente: il round corrente viene individuato da `CalendarService`, quindi le sessioni di venerdì e sabato sono incluse anche prima della gara di domenica. Lo script confronta la fonte con il database e sostituisce soltanto una sessione completa e differente. La route risultati conserva un recupero lazy esclusivamente come fallback se una sessione non è ancora presente. FP1, FP2, FP3 e Sprint Qualifying usano Alpha (codice `SQ` per lo Shootout e campi SQ1/SQ2/SQ3); Race, Quali e Sprint usano le route Ergast compatibili. Una risposta vuota, non valida o rate-limited non cancella mai dati esistenti.

Jolpica Alpha è la fonte operativa corrente per FP e Sprint Qualifying; essendo un contratto Alpha, va monitorata durante i weekend. F1DB resta una possibile fonte di backfill storico, non un rimpiazzo pianificato delle API Jolpica.
### Trust TLS su Windows

Il backend carica truststore all’avvio prima di requests. In sviluppo Windows usa quindi CryptoAPI e il trust store di sistema, mantenendo la verifica TLS attiva anche in presenza di CA aziendali o antivirus attendibili nel sistema. truststore è una dipendenza backend pinata; non usare verify=False o bundle di certificati non verificati.

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
| GET | `/api/v1/results/{round_number}/{session_type}` | Risultati gara, sprint, qualifiche o prove libere (fp1, fp2, fp3) |
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
- `session_participants`;
- `races`;
- race_results: cache di risultati con la scuderia fotografata nella singola sessione; una riga può riferirsi a un driver titolare oppure a un session_participant.
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
I tester che partecipano soltanto alle prove libere vivono in session_participants, non in drivers: non hanno numero, statistiche o profilo pilota. La risposta risultati li marca come is_session_only e la UI li mostra nella classifica senza renderli apribili come dettaglio pilota. La scuderia è letta dal risultato Alpha della sessione, quindi sono rappresentati correttamente anche quando lo stesso partecipante guida per team diversi in weekend diversi.

### Migrazioni dello schema

La configurazione della connessione e letta da `app/core/config.py`. La revisione baseline `9f34e5026ddc` descrive lo schema SQLAlchemy canonico; il database runtime e attualmente registrato alla revisione `b84c1e8d4a72` tramite `alembic_version`.

Le migration modificano lo schema in modo versionato; non sostituiscono i seed dei dati. Prima di ogni modifica strutturale si esegue un backup e si revisiona manualmente il file generato con autogenerate.

## Frontend Android

Il frontend usa Kotlin, Jetpack Compose, Material 3, Retrofit, OkHttp, Room, DataStore, Coil e Firebase.

Room migra dalla versione 20 alla 21 aggiungendo il flag is_session_only alla cache risultati; la migrazione SQL aggiunge la colonna con default false e non cancella la cache esistente.
FirebaseAuth gestisce la sessione utente. La UI espone attualmente solo Google Sign-In; il codice email/password resta predisposto ma disabilitato dal flag `EMAIL_PASSWORD_AUTH_ENABLED`. `TokenManager` conserva solo lo stato onboarding e rimuove la chiave token legacy, senza persistire nuovi ID token.

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

Le soglie di refresh generali (30 minuti) sono al momento in memoria: nel medesimo processo evitano richieste duplicate, mentre a ogni riapertura completa vengono deliberatamente rieseguiti i refresh online per privilegiare la freschezza. Room resta il fallback offline e i risultati finali di sessione già presenti non vengono mai richiesti di nuovo. Un futuro refactor potrà rendere persistenti timestamp per risorsa e adottare stale-while-revalidate, senza cambiare il contratto dati.

### Offline-first

Al primo utilizzo l'app necessita di connessione per scaricare i dati iniziali. Prima di mostrare la Home, l'avvio attende il refresh della race week e del calendario; classifiche, statistiche e news partono poi in background.

Il refresh del calendario salva l'intera stagione in Room. Appena la Home è pronta, un prefetch in background completa dettagli circuito e risultati di tutte le sessioni previste dei round passati e del round corrente; i round futuri restano on-demand. Il prefetch usa al massimo tre richieste simultanee e non prolunga la splash.

Nei successivi accessi offline, Room può fornire i dati precedentemente scaricati. Una sessione con risultati già presenti in Room non richiama il backend e non viene mai sostituita da una risposta vuota; per una sessione corrente senza risultati il nuovo tentativo è limitato a uno ogni due minuti. Gli errori di rete vengono generalmente registrati e i dati locali vengono mantenuti.

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

### Migrazioni Alembic

Dalla directory backend, con il virtual environment attivo:

~~~powershell
python -m alembic current
python -m alembic check
python -m alembic upgrade head
~~~

current mostra la revisione applicata; check segnala se i modelli e l’ultima migration non sono allineati; upgrade head applica le revisioni mancanti. Per creare una nuova revisione:

~~~powershell
python -m alembic revision --autogenerate -m "descrizione"
~~~

Il file generato deve essere sempre revisionato e testato prima di applicarlo. downgrade è riservato a copie o procedure deliberate con backup. Non usare scripts.seed per aggiornare lo schema: il seed è distruttivo e gestisce i dati iniziali.
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

1. riconcilia i risultati canonici di tutte le sessioni disponibili;
2. ricalcola le statistiche stagionali;
3. ricalcola le statistiche carriera dei piloti;
4. corregge i mondiali;
5. ricalcola le statistiche carriera dei costruttori;
6. svuota la cache delle classifiche;
7. aggiorna le news RSS.

### Sincronizzazione canonica dei risultati

Dalla cartella `backend`, con il virtual environment attivo:

~~~powershell
# Anteprima completa: legge Jolpica, confronta ogni sessione e non scrive nulla.
python -m scripts.sync_session_results

# Scrittura delle sole sessioni complete e differenti.
python -m scripts.sync_session_results --apply

# Diagnosi o riconciliazione mirata di un weekend.
python -m scripts.sync_session_results --apply --round <ROUND>
~~~

Lo script include il GP corrente per recuperare sessioni già concluse: usa il calendario, non la sola data della gara, per includere anche venerdì e sabato prima della domenica. Attende almeno 1,1 secondi tra richieste Jolpica, usa retry esponenziale per `429 Too Many Requests` e distingue un rate limit da una sessione realmente non disponibile. `sync_database` esegue automaticamente la stessa sincronizzazione: questo comando separato serve per anteprima, diagnosi o interventi mirati.
### Backfill risultati prove libere

Il backfill popola la tabella backend RaceResult con FP1, FP2 e FP3 dei GP già conclusi. Non richiede Uvicorn: fermare il server e chiudere l’app prima di eseguirlo, così SQLite non deve gestire scritture concorrenti. Fare prima una copia di backend/data/formula_knowledge.db.

Dalla cartella backend, con il virtual environment attivo:

~~~powershell
# Anteprima: chiama Alpha e valida tutti i piloti, senza scrivere il database.
python -m scripts.backfill_practice_results

# Scrittura: salva le sessioni FP valide dei GP con data gara già trascorsa.
python -m scripts.backfill_practice_results --apply
~~~

Il comando è idempotente: una coppia round/sessione già presente viene saltata. Per includere esplicitamente il weekend corrente o tutti i round fino a un numero noto:

~~~powershell
python -m scripts.backfill_practice_results --apply --through-round <ROUND>
~~~

Per correggere solo una sessione già memorizzata, usare --force soltanto dopo aver verificato la fonte: la sostituzione è limitata alle coppie round/sessione richieste e avviene in una singola transazione.

~~~powershell
python -m scripts.backfill_practice_results --apply --round <ROUND> --force
~~~

Una risposta Alpha vuota non cancella dati esistenti e viene riportata come non disponibile. Questo script è utile solo per diagnosi/manutenzione FP mirata: la sincronizzazione ordinaria è affidata a `sync_database`.
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

- Fare un backup di `backend/data/formula_knowledge.db` prima di seed o modifiche allo schema.
- Non usare `scripts.seed` come comando di aggiornamento ordinario.
- Eseguire sempre Uvicorn dalla cartella `backend`, perché il percorso SQLite è relativo.
- Non committare credenziali Firebase Admin, `.env` o chiavi private.
- Il client Retrofit usa log ridotti e redige gli header sensibili; in release il logging HTTP è disattivato.
- Verificare il database corretto quando si usa un SQLite viewer.
- Non cancellare la cartella `backend/tests`: contiene fixture e test utili.

## Modifiche future previste

- mantenere Alembic come sistema versionato per le migrazioni dello schema;
- consolidare o archiviare definitivamente i vecchi modelli;
- migrare SQLite a PostgreSQL;
- aggiungere endpoint `/health`, logging strutturato e rate limiting prima del deployment pubblico;
- sostituire la navigazione manuale con Navigation Compose;
- migliorare gli stati di errore e sincronizzazione offline;
- aggiungere autorizzazioni backend per AI custom, notifiche, live timing e widget;
- localizzare l'intera app per inglese, italiano, francese, spagnolo e tedesco tramite risorse Android;
- preparare deployment pubblico con HTTPS, secret manager, backup e health check.


## Convenzioni operative dei seed

La stagione operativa è configurata in `backend/app/core/config.py` tramite `F1_SEASON` e può essere sovrascritta dall’ambiente (`F1_SEASON=2026`). In questa fase il valore predefinito è `2026`.

`backend/scripts/seed.py` ricrea il nucleo del database e usa il calendario Jolpica per le gare e le sessioni. I dati statici di squadre, piloti e statistiche storiche sono mantenuti nelle costanti del modulo; il seed non modifica più i dizionari globali durante l’esecuzione. In caso di errore il rollback viene eseguito e l’eccezione viene propagata, così il comando non può apparire riuscito quando il popolamento è incompleto.

`backend/scripts/sync_session_results.py` riconcilia i risultati di sessione con rate limit prudente, User-Agent identificativo e retry sui `429`; prima valida sempre l'intera sessione e non elimina dati se la fonte è vuota. `sync_database.py` lo esegue prima del ricalcolo statistiche, quindi coordina risultati, campionati, cache classifiche e feed RSS.

I dati mancanti dei circuiti non devono essere completati con valori inventati: prima si verifica la fonte, poi si aggiunge la voce storica al seed. `scripts.seed` resta un reset distruttivo e non sostituisce `scripts.sync_database` per gli aggiornamenti ordinari.

### Stato della manutenzione seed

La prima fase di refactoring è stata verificata con compilazione Python e suite sandbox: 6 test superati.

- I dati statici dei circuiti restanti sono presenti in `HISTORICAL_DATA` e sono stati verificati manualmente, inclusi i conteggi 2026 e la lunghezza di Sepang (`5.543 km`).
- `scripts.seed` valida i circuiti non cancellati prima del `drop_all()`. Gli alias espliciti servono solo a collegare denominazioni API e dati storici, senza alterare il nome salvato nel calendario.
- `seed_driver_stats.py` e `seed_constructor_stats.py` applicano gli aggiornamenti con un singolo commit, rollback su errore e chiusura garantita della sessione.
- La suite sandbox resta verde (`6/6`); i comportamenti transazionali dei due seed statistici sono stati verificati con sessioni simulate isolate dal database reale.

Restano eventualmente da aggiungere test permanenti dedicati ai seed; il refactor della directory base e la baseline Alembic sono già stati completati.
