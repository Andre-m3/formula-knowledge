# Formula Knowledge — Roadmap tecnica

Questo documento raccoglie le decisioni architetturali e le attività consigliate prima di proseguire con nuove funzionalità.

## Stato architetturale attuale

### Backend

- `backend/app/main.py` contiene il bootstrap FastAPI, l'inizializzazione Firebase e l'inclusione del router.
- `backend/app/api/endpoints.py` è l'aggregatore dell'API: include i router per `raceweek`, `results`, `standings`, `content`, `stats` e `auth`.
- `backend/app/api/schemas.py` contiene gli schemi Pydantic condivisi; `dependencies.py` contiene API key, OAuth2 e dipendenze Firebase.
- SQLAlchemy utilizza SQLite tramite `backend/app/database.py`.
- Il modello attualmente utilizzato dal codice operativo è `backend/app/models.py`.
- La vecchia struttura `backend/app/models/` è stata rimossa dopo la verifica degli import; il codice operativo utilizza `backend/app/models.py` e `backend/app/database.py`.
- Le route espongono race week, calendario, circuiti, risultati, classifiche, statistiche, news, aggiornamenti tecnici e profilo utente.
- `ExternalApiService` recupera dati F1 da Jolpica/Ergast e applica cache in memoria.
- `WeatherService` recupera le previsioni da Open-Meteo.
- `FiaScraperService` cerca i documenti FIA, estrae i PDF, usa Gemini per strutturare/tradurre gli aggiornamenti tecnici e li salva nel database.
- `rss_scraper.py` importa news da feed RSS e mantiene gli articoli più recenti.
- Gli script di seed inizializzano anagrafiche, gare e statistiche storiche/stagionali.
- `update_post_race.py` applica i risultati di un round tramite delta e `RoundProcessingLog`, così lo stesso round può essere ricalcolato dopo penalità o modifiche ufficiali.
- `sync_session_results.py` riconcilia in modo idempotente la tabella canonica `RaceResult`; `sync_session_analysis.py` archivia i giri completi post-sessione; `sync_database.py` esegue entrambi senza alterare i dati già validi.

### Frontend

- L’app è Android nativa Kotlin con Jetpack Compose e Material 3.
- `MainActivity` avvia `UpdatesScreen`, che oggi funge da root container dell’app.
- La navigazione è manuale: enum `AppScreen`, variabili `selected*`, `AnimatedContent` e gestione personalizzata del back button.
- Retrofit/Gson comunica con FastAPI tramite `F1ApiService`.
- Un interceptor aggiunge l’header `X-API-Key` alle chiamate.
- `FormulaRepository` coordina API, cache e database locale Room.
- Room conserva classifiche, calendario, race week, risultati, Race/Quali Analysis, news e statistiche di piloti/costruttori.
- I dati vengono esposti alla UI tramite `Flow`.
- Il caricamento iniziale attende il refresh di race week e calendario prima di mostrare la Home, evitando il lampo di una race week Room obsoleta.
- `refreshCalendar()` aggiorna il calendario completo; dopo che la Home è pronta, `prefetchCompletedSessionResults()` completa in background dettagli circuito e tutte le sessioni dei round passati/correnti, mentre i round futuri restano on-demand.
- Il backup Android esclude il database Room/cache, DataStore e SharedPreferences per evitare il ripristino di dati vecchi sopra una nuova installazione.
- Identity gestita da Firebase; la UI corrente espone solo Google Sign-In, mentre email/password resta predisposto ma disabilitato dal flag `EMAIL_PASSWORD_AUTH_ENABLED`.
- FirebaseAuth gestisce la sessione; DataStore conserva soltanto lo stato di completamento onboarding.
- Gli ospiti possono consultare i dati generali; profilo, preferenze e future funzioni avanzate devono richiedere autenticazione.

## Decisioni consigliate

### 1. Consolidare i modelli SQLAlchemy

Il consolidamento dei modelli legacy è completato. Non bisogna comunque fondere alla cieca eventuali vecchi backup: rappresentavano un'architettura diversa, con `User`, tipi di ID e tabelle sovrapposte incompatibili.

La decisione applicata è:

1. considerare `backend/app/models.py` il modello canonico attuale;
2. verificare tutti gli import con `rg "from .*models|import .*models" backend`;
3. confrontare le tabelle dichiarate con lo schema reale di `backend/data/formula_knowledge.db`;
4. spostare eventualmente i modelli canonici in un package organizzato per dominio;
5. aggiornare gli import e i test;
6. eliminare la vecchia struttura solo dopo una verifica completa.

Una struttura futura, ancora non necessaria, potrebbe essere:

```text
backend/app/models/
├── __init__.py
├── core.py
├── racing.py
├── statistics.py
├── news.py
└── users.py
```

Il package dovrebbe esportare una sola `Base` e una sola definizione per ogni tabella.

### 2. Preparare la migrazione SQLite → PostgreSQL

La migrazione prevista tra circa tre mesi è il motivo per cui il baseline Alembic è stato introdotto subito; la migrazione verso PostgreSQL resta una fase successiva.

Il precedente bootstrap basato su `Base.metadata.create_all()` creava le tabelle mancanti, ma non gestiva in modo sicuro:

- aggiunta di colonne;
- rinomina di colonne;
- modifica dei tipi;
- vincoli e indici;
- migrazione dei dati esistenti.

Prima di PostgreSQL occorre quindi:

1. mantenere un solo modello canonico;
2. configurare `DATABASE_URL` tramite variabili d’ambiente;
3. ~~creare la prima migration Alembic;~~ Completato il 2026-09-03.
4. ~~verificare la migration su una copia del database SQLite;~~ Completato il 2026-09-03.
5. creare lo schema PostgreSQL;
6. trasferire i dati;
7. eseguire i test backend contro PostgreSQL;
8. usare Alembic per tutte le modifiche successive.

`psycopg2-binary` è già presente nelle dipendenze backend, ma il driver da usare in produzione andrà confermato in base alla configurazione del server.

### 3. Organizzare le route API — completato

Il refactor è stato completato con un aggregatore sottile e router per dominio. `main.py` è stato ridotto a bootstrap e composizione dell'app, mentre gli URL pubblici sono rimasti invariati.

Struttura applicata:

```text
backend/app/api/
├── endpoints.py    # aggregatore
├── schemas.py      # modelli Pydantic condivisi
├── dependencies.py # API key, OAuth2 e Firebase
├── raceweek.py     # race week, calendario e circuiti
├── results.py      # risultati e aggiornamenti di un GP
├── standings.py    # classifiche piloti e costruttori
├── content.py      # news e aggiornamenti tecnici
├── stats.py        # statistiche carriera e stagione
└── auth.py         # profilo e preferenze utente
```

Ogni modulo dovrebbe contenere un `APIRouter`. `main.py` dovrebbe occuparsi principalmente di:

- creare l’istanza FastAPI;
- inizializzare Firebase;
- configurare middleware e lifespan;
- includere i router;
- registrare eventuali health check.

Con circa quindici endpoint, questo numero limitato di moduli mantiene le responsabilità leggibili senza frammentare ogni singola route in file isolati. `endpoints.py` resta il punto unico di composizione per non cambiare gli import di `main.py`.

### 4. Gestire correttamente guest e utenti autenticati

La politica funzionale prevista è valida:

- guest: accesso a calendario, risultati, classifiche, news e contenuti F1 pubblici;
- utente autenticato: profilo, preferenze e personalizzazione;
- future funzioni protette: AI custom, notifiche push, sessioni live e widget.

Il controllo deve essere applicato anche nel backend. Nascondere una voce nella UI non è sufficiente.

Si consiglia di distinguere chiaramente:

- endpoint pubblici con API key applicativa;
- endpoint utente con token Firebase;
- endpoint avanzati con token Firebase e autorizzazioni/capability specifiche.

L’API key inserita nell’APK può essere estratta. Non deve quindi essere considerata un segreto o l’unico meccanismo di sicurezza.

Prima della pubblicazione, le credenziali Firebase Admin, le API key e le altre configurazioni sensibili devono essere spostate in variabili d’ambiente o in un secret manager.

### 5. Valutare Navigation Compose

La Navigation Component è la libreria Android che gestisce destinazioni, back stack, deep link, passaggio dei parametri, ripristino dello stato e ViewModel associati alla navigazione.

Poiché il frontend è interamente Compose, la variante adatta è Navigation Compose, con:

- `NavController` come coordinatore;
- `NavHost` come contenitore delle schermate;
- un navigation graph;
- route per schermate principali e dettagli.

La bottom bar attuale può essere mantenuta visivamente. Cambierebbe il meccanismo interno: le tab principali diventerebbero destinazioni top-level e i dettagli avrebbero una propria back stack.

La soluzione attuale funziona, ma la conservazione dello stato è parziale:

- `remember` conserva lo stato durante la composizione;
- Room conserva i dati persistenti;
- le variabili `selected*` conservano manualmente i parametri;
- il back stack e il ripristino dopo ricreazione/process death sono gestiti solo in parte.

La migrazione non è urgente per correggere l’app attuale, ma è consigliata prima di aggiungere notifiche, deep link, widget e molte nuove schermate.
### Strategia sorgenti dati — Jolpica e F1DB

~~~mermaid
flowchart LR
    J[Jolpica / Ergast stabile] --> J1[Calendario, orari, classifiche e risultati Race / Quali / Sprint]
    JA[Jolpica Alpha] --> JA1[Risultati finali FP1 / FP2 / FP3 / Sprint Qualifying]
    F[F1DB — release CC BY 4.0] --> F1[Backfill e verifica post-sessione: FP, risultati e dati storici ricchi]
    J1 --> DB[(Database Formula Knowledge)]
    JA1 --> DB
    F1 --> DB
    DB --> A[API FastAPI e cache Room]
~~~

| Fonte | Uso raccomandato | Stato |
|---|---|---|
| Jolpica Ergast-compatible | Calendario, orari sessioni, classifiche, risultati gara/qualifiche/sprint e flussi già integrati | Fonte primaria attuale |
| Jolpica Alpha | Risultati finali FP1, FP2, FP3 e Sprint Qualifying tramite endpoint dinamico per round/sessione | Da usare ora, monitorando il contratto Alpha nei weekend Sprint |
| F1DB | Import post-sessione/versionato, backfill, verifica dati e copertura storica ricca incluse prove libere | Non implementare ora |

Decisione: non sostituire Jolpica con F1DB e non sostituire il database interno con il database F1DB. F1DB dovrà essere importato nel modello dati canonico solo se offrirà un vantaggio concreto rispetto a Jolpica Alpha.

Condizione per valutare l’importer F1DB: verificare Jolpica Alpha per almeno tre weekend, includendo un weekend Sprint. Se i risultati FP arrivano completi e con tempi accettabili, F1DB resterà una fonte opzionale di backfill; se mancano risultati, campi o affidabilità, si progetterà un importer idempotente F1DB con attribuzione CC BY 4.0.

## Roadmap operativa dettagliata

### Fase 0 — Ripristino dell’ambiente — completata

- ~~Attivare il virtual environment Python corretto.~~ Verificato nel virtual environment del progetto.
- ~~Verificare python -m pip show sqlalchemy dalla cartella backend.~~ Verificato con esito positivo.
- ~~Verificare che il comando Uvicorn utilizzi lo stesso interprete.~~ Verificato durante i test del backend.
- ~~Non usare il terminale come amministratore per risolvere un problema di dipendenze.~~ La causa era l’interprete Python errato, non i privilegi.

### Fase 1 — Backup e fotografia dello schema — completata

- ~~Arrestare Uvicorn e qualsiasi processo che utilizzi il database.~~ Eseguito prima delle operazioni sullo schema.
- ~~Copiare backend/data/formula_knowledge.db in un file di backup datato.~~ Backup verificato e conservato.
- ~~Elencare tabelle e colonne presenti nel database.~~ Verificato durante la separazione del database runtime.
- ~~Verificare gli import dei due sistemi di modelli.~~ Completato; il modello canonico è app/models.py.
- ~~Eseguire i test backend esistenti.~~ Suite sandbox superata.

### Fase 2 — Consolidamento dei modelli — completata

- Confermare backend/app/models.py come fonte di verità.
- Portare nel modello canonico solo eventuali informazioni ancora utili del vecchio modulo.
- Evitare di riutilizzare automaticamente le vecchie definizioni di User e Team.
- Rimuovere il vecchio modulo solamente dopo aver aggiornato gli import — completato; i file sono stati salvati dall’utente come backup esterno.
- Aggiungere o aggiornare i test per tabelle, relazioni e serializzazione.

### Fase 3 — Migrazioni database — baseline completata il 2026-09-03

- ~~Installare e configurare Alembic nel virtual environment.~~ Completato; versione Alembic fissata nelle dipendenze backend.
- ~~Creare la configurazione collegata alla stessa DATABASE_URL dell’app.~~ Completato in backend/alembic/env.py.
- ~~Generare la migration iniziale basata sul modello canonico.~~ Completato nella revisione baseline.
- ~~Testare upgrade e downgrade su una copia del database.~~ Completato su database temporanei isolati.
- ~~Registrare con stamp la baseline sul database runtime già verificato.~~ Completato; il database è alla revisione head.
- ~~Evitare seed.py su un database contenente dati importanti: esegue drop_all() e ricrea le tabelle.~~ Documentato e mantenuto come regola operativa.
- ~~Rimuovere create_all() dal bootstrap applicativo dopo aver standardizzato il comando Alembic nella procedura di avvio/deployment.~~ Completato il 2026-09-03; lo schema deve essere aggiornato esplicitamente con Alembic prima dell’avvio.

### Fase 4 — Refactoring API — refactoring completato; hardening pendente

- Creare router separati per dominio — completato.
- Spostare gli schemi Pydantic e le dipendenze comuni in moduli dedicati — completato.
- Lasciare in main.py solo bootstrap e composizione — completato.
- Verificare che gli URL, i parametri, l’ordine delle route risultati e la sicurezza restino invariati — completato.
- Aggiungere endpoint /health e logging strutturato prima del deploy pubblico — pendente.
- Hardening logging Retrofit completato il 2026-09-03: BASIC solo in debug, NONE in release e redazione di Authorization/X-API-Key.

### Fase 5 — Autenticazione e autorizzazioni — base completata; sicurezza avanzata pendente

- ~~Formalizzare quali endpoint sono guest e quali autenticati.~~ Completato il 2026-09-03: dati generali richiedono API key; profilo e preferenze richiedono API key piu token Firebase.
- ~~Rimuovere il token Firebase duplicato dal DataStore.~~ Completato il 2026-09-03: FirebaseAuth gestisce la sessione e la chiave legacy viene eliminata durante avvio.
- ~~Eliminare la doppia verifica Firebase nel profilo utente.~~ Completato il 2026-09-03: una sola dependency verifica il token e passa i claims gia verificati alle route auth.
- Proteggere dal backend le feature avanzate.
- Introdurre ruoli o capability per AI, live timing, notifiche e widget.
- Spostare segreti e credenziali fuori da repository e APK.
- Mantenere temporaneamente `X-API-Key` per lo sviluppo locale; prima del deployment pubblico valutare la rimozione dalle route guest e la protezione con HTTPS, rate limiting, CORS e monitoraggio.

### Capability future — pianificata, non implementata

Questa sezione raccoglie le funzionalità avanzate che richiederanno autorizzazioni persistenti lato backend, senza introdurle nella fase corrente:

- capability separate per AI custom, notifiche push, sessioni live e widget;
- eventuali ruoli amministrativi o moderazione;
- modello dati e migration Alembic dedicati, da definire prima di modificare la tabella `users`;
- controllo backend obbligatorio: la UI potrà nascondere una funzione, ma non sarà mai l’unico controllo di accesso;
- audit, revoca e test di autorizzazione per ogni capability.


### Sicurezza API pubblica — obbligatoria prima del deployment

La `X-API-Key` distribuita nell’APK non è un segreto forte: può essere estratta da un client. Per lo sviluppo locale resta un filtro operativo utile, ma non deve rappresentare la protezione definitiva del servizio pubblico.

Prima della pubblicazione occorre valutare e testare in modo coordinato:

- rimozione della `X-API-Key` dalle sole route guest di sola lettura, oppure sostituzione con un meccanismo di attestazione/app integrity se realmente necessario;
- mantenimento del token Firebase sulle route personali e sulle future feature avanzate;
- HTTPS obbligatorio, rate limiting, CORS restrittivo, logging senza segreti e monitoraggio degli errori;
- aggiornamento simultaneo di backend, interceptor Retrofit, test HTTP e documentazione;
- piano di rollback e verifica su ambiente staging prima della modifica del server pubblico.

Questa attività è rinviata: non modifica il contratto locale corrente e non è necessaria per le feature offline-first attuali.

### Fase 6 — Navigation Compose

- Mantenere temporaneamente il design della bottom bar.
- Estrarre uno AppScaffold e un NavHost.
- Convertire prima le destinazioni top-level: Home, Calendario, Classifiche, Personal.
- Convertire poi le schermate dettaglio.
- Usare ViewModel e SavedStateHandle per parametri e stato di navigazione.
- Aggiungere deep link per notifiche e contenuti condivisibili.

### Fase 7 — PostgreSQL e deployment

- Preparare PostgreSQL su una macchina separata o sul server definitivo.
- Configurare backup automatici e logging.
- Eseguire le migration Alembic.
- Importare e validare i dati.
- Avviare Uvicorn con un processo supervisionato e configurazione da ambiente.
- Aggiornare API_BASE_URL dell’app a un endpoint HTTPS pubblico.
- Verificare CORS, TLS, rate limiting e health check.

### Fase 8 — Internazionalizzazione — pianificata, non iniziata

- Usare le risorse Android come fonte di verità per i testi UI, con values come fallback inglese.
- Aggiungere le traduzioni per italiano, francese, spagnolo e tedesco tramite values-it, values-fr, values-es e values-de.
- Sostituire i testi hardcoded con stringResource in Compose e con getString nei punti non composable.
- Usare stringhe parametrizzate e plurali Android per numeri, date, sessioni e messaggi variabili.
- Mappare gli errori applicativi tramite codici stabili o sealed result, traducendoli solo nel frontend.
- Non inserire traduzioni locali nel backend per le etichette UI; il backend deve restituire dati e codici strutturati.
- Audit completo dei testi hardcoded, inclusi contentDescription, dialog, onboarding, snackbar e messaggi di errore.
- Testare almeno inglese, italiano, francese, spagnolo e tedesco su emulatori/configurazioni locali.
- Definire in seguito la strategia per contenuti esterni come news, nomi ufficiali dei GP e documenti FIA, che non sono automaticamente traducibili senza alterarne il significato.
## Aggiornamento manuale del database

Il problema `No module named sqlalchemy` indica quasi certamente che il comando è stato eseguito con un Python globale invece del virtual environment del progetto. Nel repository il virtual environment esiste in:

```text
C:\CodeProjects\formula-knowledge\formula-knowledge\.venv
```

L’errore non dipende dal fatto che il terminale non sia stato aperto come amministratore.

### Procedura consigliata PowerShell

```powershell
cd C:\CodeProjects\formula-knowledge\formula-knowledge
\.venv\Scripts\Activate.ps1
cd .\backend
python -m pip show sqlalchemy
```

Se il comando `pip show` mostra SQLAlchemy, l’ambiente è corretto.

Se PowerShell blocca l’attivazione degli script, usare solo per quella sessione:

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
\.venv\Scripts\Activate.ps1
```

L’attivazione non è obbligatoria. È possibile usare direttamente l’interprete corretto:

```powershell
cd C:\CodeProjects\formula-knowledge\formula-knowledge\backend
..\.venv\Scripts\python.exe -m pip show sqlalchemy
```

### Aggiornamento di un singolo round

Prima fare un backup del database e fermare Uvicorn:

```powershell
cd C:\CodeProjects\formula-knowledge\formula-knowledge\backend
Copy-Item .\data\formula_knowledge.db .\data\formula_knowledge.db.backup-$(Get-Date -Format yyyyMMdd-HHmmss)
```

Poi eseguire il round corretto:

```powershell
python -m scripts.update_post_race <ROUND>
```

Esempio:

```powershell
python -m scripts.update_post_race 6
```

Lo script recupera gara, qualifiche e sprint da Jolpica/Ergast, applica le statistiche e aggiorna il log di rollback. Se lo stesso round era già stato processato, prima annulla i delta precedenti e poi applica i dati nuovi.

### Ricalcolo completo della stagione

Per penalità tardive, dati incoerenti o riallineamento completo:

```powershell
python -m scripts.sync_database
```

Questo riconcilia prima i risultati canonici delle sessioni, poi ricalcola statistiche stagionali e di carriera, corregge i mondiali, svuota la cache delle classifiche e aggiorna le news. È più lento del singolo round e richiede accesso alle API esterne.

### Reset completo iniziale

Usare questi comandi solo per setup iniziale o reset deliberati:

```powershell
python -m scripts.seed
python -m scripts.seed_driver_stats
python -m scripts.seed_constructor_stats
```

Attenzione: `python -m scripts.seed` esegue `drop_all()` e ricrea le tabelle. Non va usato per un normale aggiornamento post-gara e non va eseguito senza backup.

### Verifica finale

Per controllare che l’ambiente sia quello corretto:

```powershell
python -c "import sys; print(sys.executable)"
python -m pip show sqlalchemy
```

L’interprete stampato deve essere quello sotto `.venv\Scripts\python.exe`.

## Registro delle modifiche completate

### 2026-08-08 — Rimosso il fallback hardcoded del calendario

- Eliminata la lista statica di gare da `backend/app/services/calendar_service.py`.
- `CalendarService` ora utilizza esclusivamente il calendario restituito da Jolpica.
- Se Jolpica non restituisce dati validi, viene sollevato un errore controllato.
- Gli endpoint calendario, race week e aggiornamenti tecnici convertono l’errore in risposta HTTP `503`.
- La cache Room dell’app resta il livello offline per gli accessi successivi; il backend non inventa più date locali obsolete.
- Verificati AST, import, errore controllato del servizio, risposta endpoint `503` e suite backend.
- La suite backend su Windows richiede `PYTHONUTF8=1` per evitare errori di stampa Unicode nella console.

### 2026-08-08 — Aggiunto `readme.md`

- Documentata la struttura attuale del progetto.
- Documentati servizi esterni, endpoint, database, flusso dati Android, modalità offline-first e comandi operativi.
- Documentati i rischi di `seed`, il percorso relativo SQLite e le future attività di consolidamento/migrazione.

## Prossime modifiche autorizzabili separatamente

1. Eseguire il test manuale Android su installazione pulita e verificare che non venga ripristinata una Room obsoleta.
2. Eliminare i database sandbox generici dopo aver verificato che i test li ricreino correttamente.
3. Aggiungere uno stato UI distinto per errore race week e calendario, oltre alla splash iniziale.
4. ~~Preparare Alembic per la migrazione SQLite → PostgreSQL.~~ Baseline completata; resta la migrazione PostgreSQL.

### 2026-08-08 — Consolidato il manuale operativo

- Trasferiti nel `readme.md` gli scenari post-gara, le procedure di sync e le indicazioni di automazione.
- Eliminato `backend/REAME_OPERATIONS.md` perché duplicava la documentazione root.
- Verificato che i comandi operativi restino documentati nel README principale.
- Verificato l’import dell’app FastAPI dopo la rimozione.
- Suite backend eseguita nuovamente con 6 test superati.

### 2026-08-08 — Reso deterministico il test sandbox

- Aggiunto un calendario minimo locale per `simulate_and_test.py`.
- Intercettato anche il recupero dello schedule esterno durante il seeding sandbox.
- Aggiunta un’asserzione che verifica la creazione effettiva del round 1.
- Il test non dipende più da Jolpica e non maschera più un fallimento del seed.
- Suite backend eseguita con 6 test superati.

### Nota ambiente SSL Jolpica

- L’errore `SSLCertVerificationError` locale non dipende dalle fixture di test.
- Il certificato ricevuto da `api.jolpi.ca` è emesso da `Avast Web/Mail Shield Root`, segno di intercettazione HTTPS da parte dell’antivirus.
- `certifi` è installato e aggiornato, ma non contiene automaticamente la CA locale di Avast.
- Non usare `verify=False` nel codice applicativo; la soluzione corretta è configurare Avast/il trust store dell’ambiente di sviluppo oppure usare un ambiente/server senza intercettazione HTTPS.

### 2026-08-08 — Rimossa la vecchia directory `backend/app/models/`

- Eliminati `backend/app/models/database.py` e `backend/app/models/models.py`.
- Verificato che seed, sync, FastAPI e test importino `backend/app/models.py` e `backend/app/database.py`.
- I due file legacy sono stati salvati dall’utente fuori dal progetto come backup.
- Verificati gli import correnti e l’assenza della vecchia struttura package.
- Suite backend eseguita con 6 test superati.
- Il seed del sandbox segnala l’indisponibilità SSL di Jolpica, ma i test usano fixture locali e risultano comunque verdi.

### 2026-08-11 — Refactor API completato e verificato

- Separati gli schemi, i controlli di sicurezza, gli helper e le route in moduli per dominio sotto `backend/app/api/`.
- Mantenuto `backend/app/api/endpoints.py` come aggregatore unico dei router.
- Ridotto `backend/app/main.py` al bootstrap FastAPI, inizializzazione Firebase e inclusione del router.
- Mantenuti invariati metodi HTTP, URL, nomi e ordine dei parametri, modelli di risposta e dipendenze API key/Firebase.
- Corretto l’ordine della route specifica `/api/v1/results/{round_number}/updates`, che ora precede la route generica delle sessioni.
- Verificate con `TestClient` 13 endpoint dati con risposta `200`, il profilo senza token con `401` e una chiamata senza API key con `403`.
- Verificata la firma dei gestori parametrizzati, la dipendenza di sicurezza su tutte le route e la chiamata dei risultati con parametri dinamici.
- Suite backend eseguita: 6 test superati.
- Test manuale Android su dispositivo fisico ancora da eseguire dall’utente con Uvicorn attivo e app reinstallata.

### 2026-08-11 — Prefetch e protezione della cache Android

- La Home precarica race week e calendario prima di uscire dalla splash iniziale.
- Il calendario completo viene richiesto alla fonte Jolpica; per i round passati e corrente vengono precaricati dettagli circuito e risultati gara, mentre i round futuri restano on-demand.
- Rimossa la visualizzazione temporanea della race week Room precedente durante il refresh iniziale.
- Eliminati dalla Home i fallback fittizi `JAPANESE`, `R3`, `SUZUKA`, `Japan`, `3-5 APRIL` e `0 km`.
- Una data calendario malformata ora mostra `--` invece della data odierna.
- Configurati `backup_rules.xml` e `data_extraction_rules.xml` per escludere Room, DataStore e SharedPreferences dai backup Android.
- Verificati XML, ricerca dei fallback residui, `git diff --check` e assenza del fallback calendario backend.
- La compilazione Kotlin deve essere verificata in Android Studio: nell'ambiente dell'agente Gradle non ha potuto scaricare il toolchain richiesto.


## 2026-08-11 — Refactor seed scripts (fase 1)

- Centralizzata la stagione operativa in `backend/app/core/config.py` tramite `F1_SEASON`, attualmente `2026`.
- `seed.py` usa la stagione configurata per recuperare gli orari Jolpica.
- `seed_season_stats.py` usa la stessa configurazione e completa il ricalcolo con un unico commit finale.
- `sync_database.py` mostra nei log l’anno configurato.
- Corretto il seed principale: non modifica più in-place `DRIVERS_DATA`, quindi può essere rieseguito nello stesso processo senza perdere `team_name`.
- Gli errori del seed principale vengono rilanciati dopo il rollback, evitando un falso messaggio di successo.
- Nessun dato sportivo del calendario è stato aggiunto o modificato in questa fase.
- Verifica eseguita: compilazione Python e suite sandbox `6` test superati.

## 2026-08-11 — Dati circuiti validati e stagione centralizzata

- Confermate e presenti in `backend/scripts/seed.py` le voci storiche per Spa-Francorchamps, Hungaroring, Zandvoort, Monza, Madring, Baku, Sepang, Singapore, Austin, Mexico City, São Paulo, Las Vegas, Lusail e Yas Marina.
- Confermati i nomi dei circuiti, la lunghezza di Sepang (`5.543 km`), la convenzione dei pareggi e il conteggio `num_races_held` comprensivo delle gare 2026.
- `CalendarService` usa ora `settings.F1_SEASON` invece di un anno hardcoded, mantenendo invariati endpoint e formato delle risposte.
- Verifica eseguita dalla directory `backend` con il virtual environment del progetto: compilazione Python riuscita e suite sandbox `6/6` superata.

## 2026-08-11 — Validazione dati storici e alias circuiti

- Aggiunta una validazione preventiva nel seed: i circuiti non cancellati devono avere dati statici in `HISTORICAL_DATA` prima del reset delle tabelle.
- Aggiunto l'alias esplicito `Albert Park Circuit` → `Albert Park Grand Prix Circuit` per gestire la differenza di denominazione tra fixture/API e dati storici.
- Il nome originale ricevuto dall'API resta invariato nel record `Race`; l'alias viene usato solo per il lookup dei dati statici.
- Verifica eseguita dalla directory `backend`: compilazione Python riuscita e suite sandbox `6/6` superata.

## 2026-08-11 — Centralizzazione stagione nel servizio API

- Sostituiti i cinque default `year=2026` di `ExternalApiService` con `settings.F1_SEASON`.
- Mantenuti invariati endpoint, firme dei metodi, parametri espliciti e chiavi di cache.
- Verifica eseguita: compilazione Python, controllo delle firme e suite sandbox `6/6` superata.

## 2026-08-11 — Seed costruttori transazionale

- seed_constructor_stats.py ora applica tutti gli aggiornamenti in un'unica transazione.
- In caso di eccezione esegue `rollback()` e rilancia l'errore; `close()` è garantito nel blocco `finally`.
- Verificati sintassi, commit singolo, rollback su errore e chiusura della sessione con una sessione finta isolata dal database reale.

## 2026-08-11 — Seed piloti transazionale

- `seed_driver_stats.py` ora prepara tutti gli aggiornamenti nella stessa transazione e committa una sola volta al termine.
- In caso di errore durante il recupero o il salvataggio esegue `rollback()`, rilancia l'eccezione e chiude sempre la sessione.
- Verificati sintassi, commit singolo e rollback su errore di recupero con sessione e risposta API simulate, senza rete o database reale.

## 2026-08-11 — Documentazione seed riallineata

- Aggiornata la sezione finale di `readme.md` per riflettere la validazione dei circuiti, la protezione pre-reset del seed e la transazionalità dei seed statistici.
- Documentati i test sandbox `6/6` e i test isolati di commit/rollback eseguiti senza usare il database reale.


## 2026-08-11 — Refactor struttura operativa e gitignore

- Spostati i sette script operativi in `backend/scripts/`: seed, sincronizzazione, aggiornamento post-gara e correzione campionati.
- Aggiornati gli import applicativi, la suite sandbox e tutti i comandi documentati da `python -m app...` a `python -m scripts...`.
- Trasferito `frontend/.gitignore` nella root e aggiunte esclusioni per segreti, ambienti locali, cache, build e database runtime.
- Verificati import di `app.main` e dei sette script, compilazione Python e suite sandbox `6/6` superata.

## Roadmap aperta

### Prossimi step operativi

1. ~~Validare manualmente i dati dei circuiti restanti e inserirli in `HISTORICAL_DATA` solo dopo conferma.~~ Completato il 2026-08-11.
2. ~~Aggiungere una validazione esplicita che segnali i circuiti senza dati storici, senza introdurre fallback sportivi inventati.~~ Completato il 2026-08-11.
3. `Rendere più robusti e transazionali `seed_driver_stats.py` e `seed_constructor_stats.py`.` Completato il 2026-08-11; resta eventualmente l'aggiunta di test permanenti dedicati fuori da `backend/tests`.
4. Ricontrollare i fallback residui e verificare che i dati del calendario aggiornato non vengano sovrascritti da seed distruttivi.
5. Proseguire il refactor della directory base e preparare la migrazione SQLite/PostgreSQL; la baseline Alembic è stata completata il 2026-09-03.

### Refactor futuro della struttura root

La root non è completamente disordinata, ma contiene elementi di natura diversa mescolati insieme:

- codice backend e frontend;
- database runtime e database di test;
- virtual environment locale;
- documenti di riferimento;
- snapshot di dipendenze;
- file di configurazione e possibili segreti;
- artefatti generati come `__pycache__`.

Non è necessario spostare tutto subito. Il refactor dovrà essere graduale, con test e controllo dei percorsi dopo ogni spostamento.

### Esempio di struttura target

```text
formula-knowledge/
├── README.md
├── TODO.md
├── .gitignore
├── .env.example
├── backend/
│   ├── app/
│   │   ├── api/
│   │   │   ├── endpoints.py
│   │   │   ├── schemas.py
│   │   │   ├── dependencies.py
│   │   │   ├── raceweek.py
│   │   │   ├── results.py
│   │   │   ├── standings.py
│   │   │   ├── content.py
│   │   │   ├── stats.py
│   │   │   └── auth.py
│   │   ├── core/
│   │   ├── services/
│   │   ├── database.py
│   │   ├── models.py
│   │   └── main.py
│   ├── scripts/
│   │   ├── __init__.py
│   │   ├── seed.py
│   │   ├── seed_driver_stats.py
│   │   ├── seed_constructor_stats.py
│   │   ├── seed_season_stats.py
│   │   ├── sync_database.py
│   │   ├── sync_session_results.py
│   │   ├── update_champs.py
│   │   └── update_post_race.py
│   ├── tests/
│   │   └── data/
│   ├── data/
│   │   └── formula_knowledge.db
│   ├── requirements.txt
│   ├── Dockerfile
│   └── README.md
├── frontend/
│   ├── app/
│   ├── gradle/
│   ├── build.gradle.kts
│   └── settings.gradle.kts
├── docs/
│   └── info/
└── .venv/                         # locale, ignorato da Git
```

### Regole del refactor

1. ~~Non spostare contemporaneamente backend, frontend e database.~~ Rispettato durante il refactor del 2026-09-03.
2. ~~Prima definire il `.gitignore` per `.venv`, `__pycache__`, database runtime, `.env` e credenziali.~~ Completato il 2026-08-11.
3. ~~Separare i dati runtime dai sorgenti, spostando il database in una directory `data/` solo dopo aver aggiornato `DATABASE_URL`.~~ Completato il 2026-09-03.
4. ~~Spostare gli script operativi in `backend/scripts/` solo dopo aver verificato gli import relativi e i comandi documentati.~~ Completato il 2026-08-11.
5. ~~Conservare le fixture in `backend/tests/data/` e non mischiarle con il database runtime.~~ Verificato il 2026-09-03.
6. ~~Valutare se il file root `requirements.txt` sia necessario oppure se mantenere solo le dipendenze backend dichiarate in `backend/requirements.txt`.~~ Completato il 2026-09-03.
7. ~~Spostare `info/` in `docs/info/` dopo aver verificato eventuali riferimenti esterni.~~ Completato il 2026-09-03.
8. ~~Dopo ogni spostamento eseguire import, test backend, avvio Uvicorn e verifica endpoint principale.~~ Verifica finale completata il 2026-09-03.

### Ordine futuro consigliato

1. ~~Pulizia degli artefatti generati e controllo `.gitignore`.~~ Completato il 2026-08-11.
2. ~~Consolidamento dei file README/TODO, convenzioni di naming e requirements.~~ Completato il 2026-09-03; backend/requirements.txt è il file dipendenze canonico.
3. ~~Separazione del database runtime dai sorgenti.~~ Completato il 2026-09-03.
4. ~~Separazione degli script operativi dal package `app`.~~ Completato il 2026-08-11.
5. ~~Spostamento della documentazione in `docs/`.~~ Completato il 2026-09-03.
6. ~~Verifica finale dei comandi di sviluppo, test e deployment.~~ Completata il 2026-09-03.

## 2026-09-03 — Separazione database runtime

- Spostato il database runtime da `backend/formula_knowledge.db` a `backend/data/formula_knowledge.db`.
- Aggiornato `DATABASE_URL` e verificati hash SHA-256, `PRAGMA integrity_check` e risoluzione dell'engine SQLAlchemy.
- Conservato il vecchio percorso come backup locale reversibile `formula_knowledge.db.migrated-backup-20260903`; aggiunto `backend/data/.gitkeep` per mantenere la directory nei cloni puliti.
- Aggiornati README, TODO e istruzioni di backup; la suite sandbox resta verde con `6/6` test superati.

## 2026-09-03 — Documentazione spostata in docs/info

- Spostati i PDF di riferimento da `info/` a `docs/info/`.
- Verificata l'assenza di riferimenti applicativi al vecchio percorso e conservate intatte entrambe le dimensioni dei file.
- Aggiornati il tree del README e la regola corrispondente nel TODO.

## 2026-09-03 — Consolidamento requirements

- Verificato che il requirements.txt root e backend/requirements.txt avessero lo stesso contenuto semantico.
- Rimosso il duplicato root non tracciato; backend/requirements.txt resta il file dipendenze canonico.
- Aggiornato il tree del README e completato il relativo punto della roadmap.

## 2026-09-03 — Chiusura regole del refactor

- Completate le regole 1, 3, 5, 6 e 8: spostamenti sequenziali, database in `backend/data/`, fixture separate, requirements consolidati e verifica tecnica finale.
- Completati i punti 5 e 6 dell'ordine futuro: documentazione in `docs/info/` e verifica finale dei comandi/servizi.
- Test eseguiti: compilazione Python, import app/script, suite sandbox `6/6`, Uvicorn su localhost, endpoint `/api/v1/circuit/1` con risposte `200` e `403` attese.

## 2026-09-03 — Tsunoda e denominazione Bahrain

- Aggiunto Yuki Tsunoda (#22, nazionalità Japanese) al roster Racing Bulls in backend/scripts/seed.py.
- Aggiunto l'ID Jolpica/Ergast tsunoda e i dati manuali di carriera in backend/scripts/seed_driver_stats.py, così il seed può creare la riga necessaria a GET /api/v1/drivers/tsunoda/stats.
- Mantenuto Liam Lawson nel roster Racing Bulls: la presenza occasionale in un'altra vettura non viene modellata come cambio anagrafico stabile.
- Mantenuto nel backend/database il nome canonico del Bahrain; CalendarScreen abbrevia solo la visualizzazione in BAHRAIN (MALAYSIA) GP.
- La città e il paese restano forniti dai dati calendario, quindi non viene duplicata o hardcodata la località nella UI.
- Verifiche completate: compilazione Python, controllo roster/ID e numeri univoci, suite sandbox 6/6, test HTTP isolato dell'endpoint Tsunoda (200) su SQLite in memoria e :app:compileDebugKotlin --offline (BUILD SUCCESSFUL).
## 2026-09-03 — Fase 3 e internazionalizzazione

- Configurato Alembic in backend/alembic/ con env.py collegato a app.core.config.settings e alla Base SQLAlchemy canonica.
- Verifica finale: pip check senza conflitti, compilazione Python/Alembic, current/check alla revisione head, suite sandbox 6/6, ciclo upgrade/downgrade isolato e compilazione Android offline riuscita.
- Generata la revisione iniziale 9f34e5026ddc_baseline_schema.py confrontando i modelli con un database SQLite vuoto in memoria.
- Verificati stamp/check sulla copia del database runtime e upgrade/downgrade su database temporanei; il database runtime è stato poi registrato alla revisione head senza ricreare tabelle o dati.
- Aggiunti alembic==1.13.2 e Mako==1.4.1 a backend/requirements.txt.
- Pianificata la localizzazione completa Android per inglese, italiano, francese, spagnolo e tedesco tramite risorse separate per lingua; implementazione rimandata.
- Diagnosi iniziale bandiera Tsunoda: flag_japan.xml era presente e funzionava nel calendario, ma mancavano i casi tsunoda in getDriverCountryForFlag() e getDriverCarNumber(); entrambi sono stati corretti nella migration UI successiva.
## 2026-09-03 — Integrazione completa Tsunoda

- Aggiunti i mapping UI tsunoda → japan e tsunoda → 22 in UpdatesScreen.kt.
- Applicata la revisione Alembic e71272d84bd5_add_tsunoda_to_driver_roster.py al database runtime: Tsunoda è nella tabella drivers con id 23, team Racing Bulls e numero 22.
- La migration è idempotente, non modifica statistiche o risultati e protegge da conflitti sul numero 22; il downgrade resta non distruttivo.
- Verificati Alembic current/check, compilazione Python, endpoint reale GET /api/v1/drivers/tsunoda/stats con 200, suite backend 6/6 e compilazione Android offline riuscita.
## 2026-09-03 — Bootstrap schema affidato ad Alembic

- Rimosso `Base.metadata.create_all()` da `backend/app/main.py`: l’avvio dell’API non modifica più silenziosamente lo schema.
- Confermato che `backend/scripts/seed.py` resta l’unico comando distruttivo per il ripristino completo dei dati iniziali.
- Confermato il ciclo operativo: `alembic current`/`check` per diagnosi, `alembic upgrade head` per applicare migrazioni, `scripts.sync_database` per sincronizzare i dati.
- Verifiche completate: compilazione Python, suite backend `6/6`, `alembic current/check`, import applicativo e test HTTP isolato su `/api/v1/circuit/1` con `200` e `403` attesi.
## 2026-09-03 — Hardening logging client

- Ridotto il logging OkHttp/Retrofit a `BASIC` in debug e disattivato in release.
- Redatti esplicitamente gli header `Authorization` e `X-API-Key` per evitare la presenza di token o chiavi nei log.
- Verificata la compilazione Android offline con `:app:compileDebugKotlin` (`BUILD SUCCESSFUL`).
- Il logging strutturato backend e l’endpoint `/health` restano attività separate e non ancora completate.
## 2026-09-03 — Sessione Firebase e login Google-only

- Rimosso il salvataggio persistente duplicato del Firebase ID token: il token viene richiesto da FirebaseAuth e usato solo per la richiesta corrente.
- Aggiunta la pulizia della chiave legacy `jwt_token` dal DataStore all’avvio dell’AuthViewModel.
- Disabilitati nella UI Facebook placeholder ed email/password tramite `EMAIL_PASSWORD_AUTH_ENABLED = false`; il metodo Firebase resta predisposto per una futura riattivazione.
- Mantenuta la distinzione guest/autenticato senza aggiungere capability o modifiche allo schema utenti.
- Verificata la compilazione Android offline con `:app:compileDebugKotlin` (`BUILD SUCCESSFUL`).
## 2026-09-03 — Hardening verifica Firebase

- Sostituito il dettaglio dinamico delle eccezioni Firebase con una risposta generica `401 Token Firebase non valido`.
- Aggiunto il controllo esplicito della presenza dell’UID nel token verificato.
- Confermato il contratto guest/autenticato: endpoint dati `200` con API key, `/auth/me` senza token `401`, token non valido `401` generico.
- Verificati compilazione Python, suite sandbox `6/6`, test HTTP guest/auth e `alembic check` senza modifiche pendenti.
## 2026-09-03 — Claims Firebase verificati una sola volta

- Introdotto `AuthenticatedUser` con entità utente e claims Firebase già verificati.
- Mantenuta `get_current_user` come dependency compatibile per eventuali route future.
- Aggiornate `/api/v1/auth/me` e `/api/v1/auth/preferences` per usare il contesto autenticato senza rieseguire `verify_id_token`.
- Mantenuti invariati URL, metodi HTTP, payload e risposte delle route auth.
- Verificati compilazione Python, suite sandbox `6/6`, profilo auth isolato `200`, guest `200`, token non valido `401`, `alembic check` e `git diff --check`.
## 2026-09-03 — Strategia futura X-API-Key

- Confermato il mantenimento temporaneo della `X-API-Key` per lo sviluppo locale e per non alterare il contratto corrente.
- Registrato che la chiave distribuita nell’APK non è un segreto forte e non sarà considerata una protezione definitiva in produzione.
- Pianificata una fase dedicata prima del deployment pubblico: valutazione della rimozione dalle route guest, HTTPS, rate limiting, CORS, logging sicuro, monitoraggio e staging con rollback.
- Nessuna modifica applicativa o di schema eseguita in questo passaggio.
## 2026-09-05 — Risultati Prove Libere tramite Jolpica Alpha

- Decisione operativa: Jolpica Alpha è utilizzabile subito per tutte le FP con risultati già pubblicati, senza una soglia arbitraria di tre weekend. La disponibilità va monitorata a ogni weekend; F1DB sarà valutato solo se emergono lacune concrete o indisponibilità prolungate.

- Verificato il contratto alpha su Monza 2026: FP1 e FP2 hanno restituito HTTP 200 con 22 risultati completi; FP3 durante lo svolgimento ha restituito HTTP 200 con lista vuota, comportamento gestito senza cache negativa.
- ExternalApiService usa Jolpica Alpha esclusivamente per fp1, fp2 e fp3; recupera dinamicamente l’URL dei risultati dalla schedule stagionale, senza hardcodare gli ID opachi dei round.
- Le risposte FP non vuote vengono salvate nella cache esistente; una risposta vuota non viene memorizzata, così l’app può ritentare appena il provider pubblica la classifica finale.
- Mantenuti invariati database, migration Alembic, endpoint pubblico dei risultati, Retrofit e flussi Race/Quali/Sprint esistenti.
- Le card FP concluse aprono ora RaceResultsScreen; rimossi il popup che dichiarava le prove libere non disponibili e la visualizzazione errata di 0 PTS.
- La schermata risultati FP usa tempi e gap, identifica il più veloce con FASTEST e mostra uno stato esplicito se il provider non ha ancora pubblicato risultati.
- Aggiunti tre test isolati: mapping URL alpha e dati FP, assenza di cache per lista FP vuota, persistenza/cache della route con SQLite in memoria.
- Verifiche completate: 3/3 test FP, suite sandbox esistente 6/6, compilazione Python dei moduli coinvolti e compilazione Kotlin offline riuscita.
- Risolto il trust TLS Python su Windows: truststore usa CryptoAPI prima di requests, il test reale Alpha restituisce 200 e non e stato introdotto verify=False.
## 2026-09-05 — Tester FP, team snapshot e trust TLS

- Aggiunta e applicata la migration Alembic b84c1e8d4a72: crea session_participants e aggiunge a race_results session_participant_id e team_name. Il database runtime è a head; Alembic check non rileva upgrade pendenti.
- Inseriti nove partecipanti solo sessione: Jak Crawford, Leonardo Fornaroli, Paul Aron, Dino Beganovic, Ayumu Iwasa, Frederik Vesti, Colton Herta, Luke Browning e Ryo Hirakawa. Il seed li ricrea senza inserirli nel roster drivers o nelle statistiche.
- RaceResult conserva la scuderia fornita da Alpha per quella sessione; risolto il caso di Iwasa e Aron, presenti con team diversi in round differenti.
- Il payload risultati espone is_session_only. Anche la prima risposta dopo un cache miss viene riletta dalla cache appena persistita, quindi include subito il flag e la scuderia snapshot; Room 20→21 conserva la cache con migration SQL; RaceResultsScreen mantiene visibili i tester ma disabilita il click verso statistiche/profilo.
- Aggiunto truststore 0.10.4 alle dipendenze backend e inizializzato prima di requests. La verifica HTTPS rimane attiva e il test Python reale verso Jolpica Alpha restituisce HTTP 200.
- Creato backup timestampato prima della migration. Eseguito backfill forzato FP fino al round 13: 29 sessioni e 634 righe salvate, 23 presenze tester complete, nessuna sessione non valida. Dieci sessioni senza risultati sono FP2/FP3 non previste nei weekend Sprint.
- Verifiche completate: compilazione Python, 7/7 test isolati cache/backfill (inclusa la prima risposta tester), compilazione Kotlin offline, Alembic current/check, import FastAPI e controllo diretto della risposta Barcelona FP1.
## 2026-09-06 — Sprint Qualifying, cache risultati e navigazione Quali

- Risolta la causa delle Sprint Qualifying vuote: la route rispondeva `200` ma `ExternalApiService` restituiva intenzionalmente una lista vuota per `sprint_shootout`. Jolpica Alpha ora viene risolta dalla schedule con codice `SQ` e mappa i componenti `SQ1`, `SQ2` e `SQ3` nei campi già usati dalla UI.
- Verificato il provider reale: round 2 ha restituito 21 risultati; la scrittura controllata ha popolato 21 righe `sprint_shootout` nel database backend con tempi SQ1/SQ2/SQ3.
- Il prefetch parte dopo race week e calendario, senza bloccare la splash. Considera i round `past` e `current`, tutte le sessioni previste, mantiene al massimo tre richieste concorrenti e lascia i round futuri on-demand.
- Risultati finali già presenti in Room sono considerati immutabili: non generano una nuova richiesta e non possono essere cancellati da una risposta API vuota. Per una sessione corrente ancora non pubblicata, il retry è limitato a due minuti.
- In RaceResultsScreen Quali e Sprint Quali mantengono i tab e supportano anche swipe orizzontale: sinistra verso la fase successiva, destra verso quella precedente. Header e tab restano fissi; solo la classifica usa la transizione direzionale con fade di Standings. Lo stato iniziale è Q3/SQ3 e viene correttamente reinizializzato cambiando tipo sessione.
- Nessuna migration Room o Alembic richiesta. Verifiche completate: compilazione Python, suite backend 8/8, risposta Alpha SQ reale, persistenza SQ DB e compilazione Android offline (`BUILD SUCCESSFUL`).
- Test manuale da eseguire su dispositivo: riavviare Uvicorn, avviare l'app con rete disponibile e verificare che una Sprint Quali storica mostri dati e swipe; dopo il prefetch, riaprire una FP/Quali/Race già caricata e confermare assenza di nuova richiesta risultati nel log Uvicorn.
## 2026-09-07 — UX podio Race Results e backlog Race Analysis

- Restyling delle card podio Race/Quali: rimosso il badge numerico circolare; nome, cognome e team iniziano ora a sinistra della card; tipografia più compatta; sigla P1/P2/P3 di sfondo alzata e ridotta di circa il 5%.
- Rifinita la gerarchia di `RaceResults`: in Quali e Sprint Quali i tab Q1/Q2/Q3 (o SQ1/SQ2/SQ3) sono a sinistra e l'azione compatta `QUALI ANALYSIS` a destra sulla stessa riga, con stessa altezza del selettore, freccia e spaziatura minima garantita; eliminato il prefisso "VIEW". L'azione Race mantiene la versione estesa.
- Nelle card evidenziate, il blocco tempo + gap/stato è ora centrato verticalmente e più compatto; ripristinate le dimensioni tipografiche originali, la palette oro/argento/bronzo dei primi tre e una spaziatura verticale proporzionata di 6dp. Compilazione Kotlin offline verificata con `:app:compileDebugKotlin` (`BUILD SUCCESSFUL`).
- Calendario: aggiunto un watermark vettoriale della bandiera nazionale nella parte destra di ogni card, dietro alla data. È ritagliato entro la card, sfuma verso sinistra e usa opacità differenziata per stato, così date e testi restano prioritari. Aggiunte le risorse mancanti per Azerbaijan, Singapore, Messico e Brasile; il mapping copre tutti i 23 paesi del calendario runtime. Compilazione Kotlin offline riuscita.
- ~~Race Analysis post-sessione~~ completata: i dati Alpha vengono memorizzati nel backend, l'app verifica prima la disponibilità e scarica il payload una sola volta in Room; il telefono non interroga mai Jolpica direttamente.
- Pit-stop timeline rinviata: l'attuale durata rappresenta il tempo complessivo in pit lane e non il solo cambio gomme. Rivalutare soltanto con una fonte che esponga compound/stint e durata fermo vettura affidabile.
## 2026-09-06 — Risultati canonici e sincronizzazione post-sessione

- Aggiunto `backend/scripts/sync_session_results.py`: in anteprima per default, confronta tutte le sessioni previste dei round conclusi e del weekend corrente con `RaceResult`. Il weekend corrente è determinato da `CalendarService`, così le sessioni di venerdì/sabato vengono incluse anche prima della gara di domenica. Lo script valida l'intero payload e in `--apply` sostituisce in una transazione solo le coppie round/sessione differenti. Race, Quali, Sprint, Sprint Qualifying e FP sono quindi dati canonici del database backend; il recupero lazy dell'API rimane soltanto un fallback.
- Integrato lo script come primo passaggio di `scripts.sync_database`. Dopo un GP, il comando ordinario da usare è quindi `python -m scripts.sync_database`; il comando separato serve per anteprima o riconciliazione mirata con `--round <ROUND>`.
- Introdotti User-Agent identificativo, intervallo minimo di 1,1 secondi e retry esponenziale sui `429` Jolpica. Un rate limit è conteggiato separatamente dai risultati non disponibili e non può causare cancellazioni. Il primo apply ha incontrato un 429 dopo 50 sessioni; le restanti sono state riprese con il nuovo limite senza perdita dati.
- Corretto il replace ORM di RaceResult: delete ORM e flush esplicito evitano il riuso ambiguo di primary key SQLite e i warning SQLAlchemy del bulk delete. Test di regressione dedicato incluso.
- Sincronizzazione reale fino al round 13 completata in due run controllati: 64 sessioni e 1.398 righe aggiornate. Restano non disponibili FP2/FP3 nei weekend Sprint in cui non esistono e la Race del round 13, non ancora pubblicata; nessun payload non valido.
- Audit Room: i risultati finali già in cache fanno return prima della chiamata Retrofit; le risposte vuote fanno return prima di `updateRaceResults`; il clear/insert è `@Transaction` ed è raggiungibile solo con una lista non vuota. Nessuna migration Room o Alembic richiesta. Database backend: 0 duplicati per round/sessione/pilota o partecipante.
- Verifiche completate: compilazione Python, suite backend 12/12 (idempotenza, variazione post-sessione, risposta vuota, rate limit, replace ORM e selezione del weekend corrente prima della domenica), query integrità SQLite e compilazione Android offline riuscita.
- Corretto il 404 `season_stats` di Tsunoda: `seed_season_stats` inizializza a zero una riga stagionale per ogni `DRIVER_IDS` gestito prima di leggere i risultati. Applicato il fix al database runtime: `/api/v1/drivers/tsunoda/season_stats` restituisce 200, `total_races=0`, `best_race_result=N/A`; il test SQLite in memoria conferma che il helper è idempotente.
- Audit cache al riavvio: le scadenze generali di 30 minuti sono mappe in memoria, quindi una chiusura completa azzera i timestamp e rinnova richieste online. Non è una perdita Room: i `Flow` mantengono i dati locali/offline e i risultati finali delle sessioni non richiamano il backend. Prima di adottare timestamp persistenti per risorsa e stale-while-revalidate, discutere esplicitamente le differenze rispetto alla policy network-first attuale: freschezza percepita, ordine UI/cache/rete, TTL per ciascuna risorsa, refresh manuale, gestione errori e invalidazione post-GP.
## 2026-09-07 — Race / Quali Analysis persistita

- Aggiunte le tabelle backend `session_analyses` e `session_laps`, con migration Alembic `d6a4e12f9b3c`; creata una copia preventiva `formula_knowledge.before-session-analysis-20260907.db` prima dell'upgrade. La migration è additiva e non modifica `RaceResult` o dati preesistenti.
- `sync_session_analysis.py` elabora soltanto Race, Quali e Sprint Quali che possiedono già risultati canonici; convalida l'intero payload Alpha prima di un replace atomico, salta gli archivi esistenti e non cancella nulla in caso di risposta vuota, non valida o rate-limited. `--refresh` è limitato a una correzione esplicita della fonte.
- Aggiunte API protette da API key: disponibilità leggera e snapshot completo. Sono local-only: nessuna route Analysis esegue un fallback Jolpica.
- Applicato il backfill fino al round 13: 31 snapshot completi e 20.271 giri (Race, Quali e cinque Sprint Quali). Verificati endpoint reali: round 13 Race `available=true`, 1.052 giri, fase `R`; Quali `available=true`, 275 giri, fasi `Q1/Q2/Q3`.
- Room 21→22 aggiunge la cache Analysis non distruttiva. RaceResults mostra l'azione disabilitata finché l'archivio non è disponibile; la nuova schermata gestisce fasi Q/SQ, pilota, grafico della posizione in gara o del passo/giro, e mantiene i dati offline dopo il primo download.
- Verifiche: 18 test backend verdi (persistenza, idempotenza, payload assente, API HTTP e regressioni esistenti), `alembic current/check` a head e compilazione Android offline riuscita.
- Pit-stop timeline resta rinviata: i dati Alpha disponibili indicano il tempo complessivo in pit lane, non la sola durata del cambio gomme.
- Backlog Analysis v2, da affrontare dopo il refactor Navigation Compose: (1) Race Analysis con vista classifica lap-by-lap separata dal focus del singolo pilota; (2) dal pilota in Race Results, destinazione contestuale con passo gara, grafici, giro veloce e durata complessiva in pit lane; (3) revisione UX/struttura di Quali Analysis. Il tap non deve più portare alle statistiche stagionali quando il contesto è un risultato di sessione.