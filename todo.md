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
- `sync_database.py` esegue il ricalcolo completo delle statistiche, pulisce le cache e aggiorna le news.

### Frontend

- L’app è Android nativa Kotlin con Jetpack Compose e Material 3.
- `MainActivity` avvia `UpdatesScreen`, che oggi funge da root container dell’app.
- La navigazione è manuale: enum `AppScreen`, variabili `selected*`, `AnimatedContent` e gestione personalizzata del back button.
- Retrofit/Gson comunica con FastAPI tramite `F1ApiService`.
- Un interceptor aggiunge l’header `X-API-Key` alle chiamate.
- `FormulaRepository` coordina API, cache e database locale Room.
- Room conserva classifiche, calendario, race week, risultati, news e statistiche di piloti/costruttori.
- I dati vengono esposti alla UI tramite `Flow`.
- Il caricamento iniziale attende il refresh di race week e calendario prima di mostrare la Home, evitando il lampo di una race week Room obsoleta.
- `refreshCalendar()` aggiorna il calendario completo e precarica dettagli circuito e risultati gara per round passati e round corrente; i round futuri restano on-demand.
- Il backup Android esclude il database Room/cache, DataStore e SharedPreferences per evitare il ripristino di dati vecchi sopra una nuova installazione.
- Firebase gestisce autenticazione email/password e Google Sign-In.
- DataStore conserva token e stato di completamento dell’onboarding.
- Gli ospiti possono consultare i dati generali; profilo, preferenze e future funzioni avanzate devono richiedere autenticazione.

## Decisioni consigliate

### 1. Consolidare i modelli SQLAlchemy

Il consolidamento dei modelli legacy è completato. Non bisogna comunque fondere alla cieca eventuali vecchi backup: rappresentavano un'architettura diversa, con `User`, tipi di ID e tabelle sovrapposte incompatibili.

La decisione applicata è:

1. considerare `backend/app/models.py` il modello canonico attuale;
2. verificare tutti gli import con `rg "from .*models|import .*models" backend`;
3. confrontare le tabelle dichiarate con lo schema reale di `backend/formula_knowledge.db`;
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

La migrazione prevista tra circa tre mesi è un buon motivo per introdurre subito Alembic.

Attualmente `Base.metadata.create_all()` crea le tabelle mancanti, ma non gestisce in modo sicuro:

- aggiunta di colonne;
- rinomina di colonne;
- modifica dei tipi;
- vincoli e indici;
- migrazione dei dati esistenti.

Prima di PostgreSQL occorre quindi:

1. mantenere un solo modello canonico;
2. configurare `DATABASE_URL` tramite variabili d’ambiente;
3. creare la prima migration Alembic;
4. verificare la migration su una copia del database SQLite;
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

## Roadmap operativa dettagliata

### Fase 0 — Ripristino dell’ambiente

- Attivare il virtual environment Python corretto.
- Verificare `python -m pip show sqlalchemy` dalla cartella `backend`.
- Verificare che `python -m app.main` o il comando Uvicorn utilizzino lo stesso interprete.
- Non usare il terminale come amministratore per risolvere un problema di dipendenze: il problema più probabile è l’interprete Python sbagliato.

### Fase 1 — Backup e fotografia dello schema

- Arrestare Uvicorn e qualsiasi processo che utilizzi il database.
- Copiare `backend/formula_knowledge.db` in un file di backup datato.
- Elencare tabelle e colonne presenti nel database.
- Verificare gli import dei due sistemi di modelli.
- Eseguire i test backend esistenti.

### Fase 2 — Consolidamento dei modelli — completata

- Confermare `backend/app/models.py` come fonte di verità.
- Portare nel modello canonico solo eventuali informazioni ancora utili del vecchio modulo.
- Evitare di riutilizzare automaticamente le vecchie definizioni di `User` e `Team`.
- Rimuovere il vecchio modulo solamente dopo aver aggiornato gli import — completato; i file sono stati salvati dall'utente come backup esterno.
- Aggiungere o aggiornare i test per tabelle, relazioni e serializzazione.

### Fase 3 — Migrazioni database

- Installare e configurare Alembic nel virtual environment.
- Creare la configurazione collegata alla stessa `DATABASE_URL` dell’app.
- Generare la migration iniziale basata sul modello canonico.
- Testare upgrade e downgrade su una copia del database.
- Evitare `seed.py` su un database contenente dati importanti: esegue `drop_all()` e ricrea le tabelle.

### Fase 4 — Refactoring API — completata

- Creare router separati per dominio — completato.
- Spostare gli schemi Pydantic e le dipendenze comuni in moduli dedicati — completato.
- Lasciare in `main.py` solo bootstrap e composizione — completato.
- Verificare che gli URL, i parametri, l'ordine delle route risultati e la sicurezza restino invariati — completato.
- Aggiungere endpoint `/health` e logging strutturato prima del deploy pubblico — pendente.

### Fase 5 — Autenticazione e autorizzazioni

- Formalizzare quali endpoint sono guest e quali autenticati.
- Proteggere dal backend le feature avanzate.
- Introdurre ruoli o capability per AI, live timing, notifiche e widget.
- Spostare segreti e credenziali fuori dal repository e dall’APK.

### Fase 6 — Navigation Compose

- Mantenere temporaneamente il design della bottom bar.
- Estrarre uno `AppScaffold` e un `NavHost`.
- Convertire prima le destinazioni top-level: Home, Calendario, Classifiche, Personal.
- Convertire poi le schermate dettaglio.
- Usare ViewModel e `SavedStateHandle` per parametri e stato di navigazione.
- Aggiungere deep link per notifiche e contenuti condivisibili.

### Fase 7 — PostgreSQL e deployment

- Preparare PostgreSQL su una macchina separata o sul server definitivo.
- Configurare backup automatici e logging.
- Eseguire le migration Alembic.
- Importare e validare i dati.
- Avviare Uvicorn con un processo supervisionato e configurazione da ambiente.
- Aggiornare `API_BASE_URL` dell’app a un endpoint HTTPS pubblico.
- Verificare CORS, TLS, rate limiting e health check.

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
Copy-Item .\formula_knowledge.db .\formula_knowledge.db.backup-$(Get-Date -Format yyyyMMdd-HHmmss)
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

Questo ricalcola statistiche stagionali e di carriera, corregge i mondiali, svuota la cache delle classifiche e aggiorna le news. È più lento del singolo round e richiede accesso alle API esterne.

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
4. Preparare Alembic per la migrazione SQLite → PostgreSQL.

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
3. `Rendere più robusti e transazionali anche `seed_driver_stats.py` e `seed_constructor_stats.py`.` Parte transazionale completata il 2026-08-11; resta eventualmente l'aggiunta di test permanenti dedicati fuori da `backend/tests`.
4. Ricontrollare i fallback residui e verificare che i dati del calendario aggiornato non vengano sovrascritti da seed distruttivi.
5. Proseguire il refactor della directory base con la separazione del database runtime e la valutazione dei requirements, poi preparare Alembic e la migrazione SQLite/PostgreSQL.

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

1. Non spostare contemporaneamente backend, frontend e database.
2. ~~Prima definire il `.gitignore` per `.venv`, `__pycache__`, database runtime, `.env` e credenziali.~~ Completato il 2026-08-11.
3. Separare i dati runtime dai sorgenti, spostando il database in una directory `data/` solo dopo aver aggiornato `DATABASE_URL`.
4. ~~Spostare gli script operativi in `backend/scripts/` solo dopo aver verificato gli import relativi e i comandi documentati.~~ Completato il 2026-08-11.
5. Conservare le fixture in `backend/tests/data/` e non mischiarle con il database runtime.
6. Valutare se il file root `requirements.txt` sia necessario oppure se mantenere solo le dipendenze backend dichiarate in `backend/requirements.txt`.
7. Spostare `info/` in `docs/info/` soltanto dopo aver verificato eventuali riferimenti esterni.
8. Dopo ogni spostamento eseguire import, test backend, avvio Uvicorn e verifica dell’endpoint principale.

### Ordine futuro consigliato

1. Pulizia degli artefatti generati e controllo `.gitignore`.
2. Consolidamento dei file README/TODO e convenzioni di naming.
3. Separazione del database runtime dai sorgenti.
4. Separazione degli script operativi dal package `app`.
5. Eventuale spostamento della documentazione in `docs/`.
6. Verifica finale dei comandi di sviluppo, test e deployment.
