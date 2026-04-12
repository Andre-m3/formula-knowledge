package com.formulaknowledge.app.data

import kotlinx.coroutines.flow.Flow
import com.google.gson.Gson
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class FormulaRepository(private val database: FormulaDatabase) {

    private val dao = database.standingsDao()
    private val raceDao = database.raceDao()
    private val generalDao = database.generalDao()
    private val driverStatsDao = database.driverStatsDao()

    // I Flow sono dei "canali aperti" con il database:
    // appena il DB si aggiorna, la UI riceve i nuovi dati in automatico!
    val driverStandings: Flow<List<DriverStandingEntity>> = dao.getDriverStandings()
    val constructorStandings: Flow<List<ConstructorStandingEntity>> = dao.getConstructorStandings()

    suspend fun refreshStandings() {
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
                        val lastName = driver.driver_name.split(" ").lastOrNull()?.uppercase() ?: ""
                        val lowerLast = lastName.lowercase()
                            .replace("ü", "u")
                            .replace("é", "e")
                            .replace(" jr.", "")
                        
                        val driverId = when {
                            lowerLast.contains("sainz") -> "sainz"
                            lowerLast == "verstappen" -> "max_verstappen"
                            lowerLast == "lindblad" || lowerLast == "limblad" -> "arvid_lindblad"
                            else -> lowerLast
                        }

                        val existingStats = driverStatsDao.getStats(driverId).firstOrNull()
                        if (existingStats == null) {
                            refreshDriverStats(driverId)
                        }
                    }
                }
            }

        } catch (e: Exception) {
            // Se c'è un errore (es. nessuna connessione WiFi), lo ignoriamo!
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
        } catch (e: Exception) {}
    }

    suspend fun refreshRaceResults(round: Int) {
        try {
            val apiData = RetrofitClient.apiService.getResults(round)
            val entities = apiData.map { RaceResultEntity(0, round, it.position, it.driver, it.team, it.points, it.time) }
            raceDao.updateRaceResults(round, entities)
        } catch (e: Exception) {}
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
        } catch (e: Exception) {}
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
        } catch (e: Exception) { /* Ignoriamo l'errore, la UI gestirà i dati vecchi/assenti */ }
    }

    fun getDriverStats(driverId: String): Flow<DriverStatsEntity?> = driverStatsDao.getStats(driverId)

    suspend fun refreshDriverStats(driverId: String) {
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
        } catch (e: Exception) {}
    }
}
