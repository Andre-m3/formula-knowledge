package com.formulaknowledge.app.data

import android.util.Log
import kotlinx.coroutines.flow.Flow
import com.google.gson.Gson
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.firstOrNull
import com.formulaknowledge.app.utils.F1Utils
import kotlinx.coroutines.launch

class FormulaRepository(private val database: FormulaDatabase) {

    private val dao = database.standingsDao()
    private val raceDao = database.raceDao()
    private val generalDao = database.generalDao()
    private val driverStatsDao = database.driverStatsDao()
    private val constructorStatsDao = database.constructorStatsDao()
    private val driverSeasonStatsDao = database.driverSeasonStatsDao()
    private val constructorSeasonStatsDao = database.constructorSeasonStatsDao()

    // I Flow sono dei "canali aperti" con il database:
    // appena il DB si aggiorna, la UI riceve i nuovi dati in automatico!
    val driverStandings: Flow<List<DriverStandingEntity>> = dao.getDriverStandings()
    val constructorStandings: Flow<List<ConstructorStandingEntity>> = dao.getConstructorStandings()

    companion object {
        // Variabili in memoria CONDIVISE per evitare di spammare chiamate di rete tra le schermate
        private var lastStandingsFetch = 0L
        private val fetchedDriverStats = mutableMapOf<String, Long>()
        private val fetchedDriverSeasonStats = mutableMapOf<String, Long>()
        private val fetchedConstructorStats = mutableMapOf<String, Long>()
        private val fetchedConstructorSeasonStats = mutableMapOf<String, Long>()
        private const val CACHE_EXPIRY = 30 * 60 * 1000L // 30 minuti
    }

    suspend fun refreshStandings() {
        val now = System.currentTimeMillis()
        val localDrivers = dao.getDriverStandings().firstOrNull()
        
        // Se abbiamo già dei dati locali e l'ultima chiamata è stata fatta da meno di 30 minuti, saltiamo!
        if (!localDrivers.isNullOrEmpty() && (now - lastStandingsFetch < CACHE_EXPIRY)) {
            return
        }

        try {
            // 1. Scarica i dati freschi da Internet
            val apiDrivers = RetrofitClient.apiService.getDriverStandings()
            val apiConstructors = RetrofitClient.apiService.getConstructorStandings()

            // 2. Li converte nel formato per il Database
            val driverEntities = apiDrivers.map {
                DriverStandingEntity(it.position, it.driver_name, it.constructor_name, it.points, it.wins)
            }
            val constructorEntities = apiConstructors.map {
                ConstructorStandingEntity(it.position, it.constructor_name, it.chassis_name, it.points, it.wins)
            }

            // 3. Sovrascrive le vecchie classifiche nel DB
            dao.updateDriverStandings(driverEntities)
            dao.updateConstructorStandings(constructorEntities)

            // Pre-fetch silente: scarichiamo in background le statistiche dei piloti
            coroutineScope {
                apiDrivers.forEach { driver ->
                    launch {
                        // Il controllo TTL è ora DENTRO refreshDriverStats, quindi possiamo chiamarlo in sicurezza.
                        val driverId = F1Utils.getDriverIdFromName(driver.driver_name)
                        refreshDriverStats(driverId)
                    }
                }
            }

            lastStandingsFetch = System.currentTimeMillis()

        } catch (e: Exception) {
            // Se c'è un errore (es. nessuna connessione WiFi), lo ignoriamo!
            Log.e("FormulaRepository", "refreshStandings failed", e)
            // L'utente continuerà felicemente a visualizzare i dati salvati in locale nel DB.
        }
    }

    fun getCircuitDetail(round: Int): Flow<CircuitDetailEntity?> = raceDao.getCircuitDetail(round)
    
    fun getRaceResults(round: Int): Flow<List<RaceResultEntity>> = raceDao.getRaceResults(round)

    suspend fun refreshCircuitDetail(round: Int) {
        try {
            val apiData = RetrofitClient.apiService.getCircuitDetails(round)
            val existingCircuit = raceDao.getCircuitDetail(round).firstOrNull()
            
            // Se abbiamo già dati locali, usiamoli come fallback per le sessioni
            // nel caso in cui l'API (magari a causa di pre-fetch in massa) fallisca nel portarli
            val fp1 = apiData.sessions.fp1 ?: existingCircuit?.fp1_time
            val fp2 = apiData.sessions.fp2 ?: existingCircuit?.fp2_time
            val fp3 = apiData.sessions.fp3 ?: existingCircuit?.fp3_time
            val sprintShootout = apiData.sessions.sprint_shootout ?: existingCircuit?.sprint_shootout_time
            val sprintRace = apiData.sessions.sprint_race ?: existingCircuit?.sprint_race_time
            val quali = apiData.sessions.quali ?: existingCircuit?.quali_time
            val race = apiData.sessions.race ?: existingCircuit?.race_time
            
            val entity = CircuitDetailEntity(
                apiData.round, apiData.gp_name, apiData.circuit_name, apiData.location, apiData.country,
                apiData.length, apiData.corners, apiData.laps, apiData.record, apiData.is_sprint,
                apiData.dates.joinToString(","), apiData.status, apiData.previous_winner,
                apiData.most_driver_wins, apiData.most_constructor_wins, apiData.most_driver_podiums,
                apiData.most_poles, apiData.num_races_held,
                fp1, fp2, fp3, sprintShootout, sprintRace, quali, race
            )
            raceDao.insertCircuitDetail(entity)
        } catch (e: Exception) {
            Log.e("FormulaRepository", "refreshCircuitDetail for round $round failed", e)
        }
    }

    suspend fun refreshRaceResults(round: Int) {
        try {
            val apiData = RetrofitClient.apiService.getResults(round)
            val entities = apiData.map { RaceResultEntity(0, round, it.position, it.driver, it.team, it.points, it.time) }
            raceDao.updateRaceResults(round, entities)
        } catch (e: Exception) {
            Log.e("FormulaRepository", "refreshRaceResults for round $round failed", e)
        }
    }

    val calendar: Flow<List<CalendarEntity>> = generalDao.getCalendar()
    val currentRaceWeek: Flow<RaceWeekEntity?> = generalDao.getCurrentRaceWeek()

    suspend fun refreshCalendar() {
        try {
            val apiData = RetrofitClient.apiService.getCalendar()
            val entities = apiData.map {
                CalendarEntity(
                    it.round, it.name, it.country, it.city, it.circuit_name,
                    it.date, it.status, it.is_clickable, it.cancelled ?: false
                )
            }
            generalDao.updateCalendar(entities)

            // Pre-fetch silente: scarichiamo in background solo ciò che manca nel DB
            coroutineScope {
                entities.filter { !it.cancelled }.forEach { race ->
                    launch {
                        // Pre-carica i dettagli del circuito per tutte le gare non cancellate (se mancano)
                        val existingCircuit = raceDao.getCircuitDetail(race.round).firstOrNull()
                        if (existingCircuit == null) {
                            refreshCircuitDetail(race.round)
                        }
                        
                        // Pre-carica i risultati solo per le gare passate (se mancano)
                        if (race.status == "past") {
                            val existingResults = raceDao.getRaceResults(race.round).firstOrNull()
                            if (existingResults.isNullOrEmpty()) {
                                refreshRaceResults(race.round)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("FormulaRepository", "refreshCalendar failed", e)
        }
    }

    suspend fun refreshCurrentRaceWeek() {
        try {
            val existingData = currentRaceWeek.firstOrNull()
            val now = System.currentTimeMillis()

            // Richiediamo sempre dati freschi all'API all'avvio.
            // Non c'è rischio di spam perché il Backend Python usa una cache di 30 minuti!
            val apiData = RetrofitClient.apiService.getCurrentRaceWeek()
            val weatherJson = Gson().toJson(apiData.weather_forecast)
            val entity = RaceWeekEntity(
                1, apiData.gp_name, apiData.country, apiData.city, apiData.circuit_name,
                apiData.round_number, apiData.is_sprint, apiData.dates.joinToString(","), apiData.status, weatherJson, now,
                apiData.sessions.fp1, apiData.sessions.fp2, apiData.sessions.fp3,
                apiData.sessions.sprint_shootout, apiData.sessions.sprint_race, apiData.sessions.quali, apiData.sessions.race
            )
            generalDao.insertRaceWeek(entity)
        } catch (e: Exception) {
            Log.e("FormulaRepository", "refreshCurrentRaceWeek failed", e)
            /* Ignoriamo l'errore, la UI gestirà i dati vecchi/assenti */ }
    }

    fun getDriverStats(driverId: String): Flow<DriverStatsEntity?> = driverStatsDao.getStats(driverId)

    suspend fun refreshDriverStats(driverId: String) {
        val now = System.currentTimeMillis()
        val lastFetch = fetchedDriverStats[driverId] ?: 0L
        val localData = driverStatsDao.getStats(driverId).firstOrNull()
        
        if (localData != null && (now - lastFetch < CACHE_EXPIRY)) {
            return
        }

        try {
            val apiData = RetrofitClient.apiService.getDriverStats(driverId)
            val entity = DriverStatsEntity(
                apiData.driver_id,
                apiData.total_races,
                apiData.wins,
                apiData.podiums,
                apiData.pole_positions,
                apiData.wins_from_pole,
                apiData.world_championships,
                
                apiData.best_race_result,
                apiData.best_championship_result,
                apiData.best_grid_position,
                apiData.fastest_laps,
                apiData.dns_count,
                apiData.dnf_count,
                apiData.dsq_count,
                
                apiData.sprint_starts,
                apiData.sprint_wins,
                apiData.sprint_top_3,
                apiData.best_sprint_result,
                apiData.best_sprint_grid_position,
                
                apiData.place_of_birth,
                apiData.date_of_birth,
                apiData.first_gp,
                apiData.first_win,
                apiData.hat_tricks,
                apiData.grand_slams,
                
                apiData.last_updated
            )
            driverStatsDao.insertStats(entity)
            fetchedDriverStats[driverId] = System.currentTimeMillis()
        } catch (e: Exception) {
            Log.e("FormulaRepository", "refreshDriverStats for $driverId failed", e)
        }
    }

    fun getConstructorStats(constructorId: String): Flow<ConstructorStatsEntity?> = constructorStatsDao.getStats(constructorId)

    suspend fun refreshConstructorStats(constructorId: String) {
        val now = System.currentTimeMillis()
        val lastFetch = fetchedConstructorStats[constructorId] ?: 0L
        val localData = constructorStatsDao.getStats(constructorId).firstOrNull()
        
        if (localData != null && (now - lastFetch < CACHE_EXPIRY)) {
            return
        }

        try {
            val apiData = RetrofitClient.apiService.getConstructorStats(constructorId)
            val entity = ConstructorStatsEntity(
                apiData.constructor_id,
                apiData.total_races,
                apiData.wins,
                apiData.podiums,
                apiData.driver_championships,
                apiData.constructor_championships,
                apiData.first_gp_year,
                apiData.first_win,
                apiData.pole_positions,
                apiData.fastest_laps,
                apiData.total_points,
                apiData.seasons_entered,
                apiData.best_race_result,
                apiData.best_championship_result,
                apiData.power_unit,
                apiData.team_principal,
                apiData.base_location,
                apiData.last_updated
            )
            constructorStatsDao.insertStats(entity)
            fetchedConstructorStats[constructorId] = System.currentTimeMillis()
        } catch (e: Exception) {
            Log.e("FormulaRepository", "refreshConstructorStats for $constructorId failed", e)
        }
    }

    fun getDriverSeasonStats(driverId: String): Flow<DriverSeasonStatsEntity?> = driverSeasonStatsDao.getStats(driverId)

    suspend fun refreshDriverSeasonStats(driverId: String) {
        val now = System.currentTimeMillis()
        val lastFetch = fetchedDriverSeasonStats[driverId] ?: 0L
        val localData = driverSeasonStatsDao.getStats(driverId).firstOrNull()
        
        if (localData != null && (now - lastFetch < CACHE_EXPIRY)) {
            return
        }

        try {
            val apiData = RetrofitClient.apiService.getDriverSeasonStats(driverId)
            val entity = DriverSeasonStatsEntity(
                apiData.driver_id,
                apiData.year,
                apiData.total_races,
                apiData.wins,
                apiData.second_places,
                apiData.podiums,
                apiData.laps_led,
                apiData.fastest_laps,
                apiData.beat_teammate_race,
                apiData.beat_teammate_quali,
                apiData.pole_positions,
                apiData.front_rows,
                apiData.retirements,
                apiData.q3_appearances,
                apiData.q2_appearances,
                apiData.q1_appearances,
                apiData.dsqs,
                apiData.best_race_result,
                apiData.sprint_starts,
                apiData.sprint_wins,
                apiData.sprint_top_3,
                apiData.sprint_points_finishes,
                apiData.sprint_points,
                apiData.beat_teammate_sprint,
                apiData.sprint_quali_poles,
                apiData.last_updated
            )
            driverSeasonStatsDao.insertStats(entity)
            fetchedDriverSeasonStats[driverId] = System.currentTimeMillis()
        } catch (e: Exception) {
            Log.e("FormulaRepository", "refreshDriverSeasonStats for $driverId failed", e)
        }
    }

    fun getConstructorSeasonStats(constructorId: String): Flow<ConstructorSeasonStatsEntity?> = constructorSeasonStatsDao.getStats(constructorId)

    suspend fun refreshConstructorSeasonStats(constructorId: String) {
        val now = System.currentTimeMillis()
        val lastFetch = fetchedConstructorSeasonStats[constructorId] ?: 0L
        val localData = constructorSeasonStatsDao.getStats(constructorId).firstOrNull()
        
        if (localData != null && (now - lastFetch < CACHE_EXPIRY)) {
            return
        }

        try {
            val apiData = RetrofitClient.apiService.getConstructorSeasonStats(constructorId)
            val entity = ConstructorSeasonStatsEntity(
                apiData.constructor_id,
                apiData.year,
                apiData.total_races,
                apiData.wins,
                apiData.podiums,
                apiData.fastest_laps,
                apiData.pole_positions,
                apiData.front_rows,
                apiData.one_two_finishes,
                apiData.double_dnfs,
                apiData.retirements,
                apiData.dsqs,
                apiData.races_in_points,
                apiData.double_q3,
                apiData.double_q2,
                apiData.double_q1,
                apiData.sprint_wins,
                apiData.sprint_podiums,
                apiData.sprint_points,
                apiData.last_updated
            )
            constructorSeasonStatsDao.insertStats(entity)
            fetchedConstructorSeasonStats[constructorId] = System.currentTimeMillis()
        } catch (e: Exception) {
            Log.e("FormulaRepository", "refreshConstructorSeasonStats for $constructorId failed", e)
        }
    }
}
