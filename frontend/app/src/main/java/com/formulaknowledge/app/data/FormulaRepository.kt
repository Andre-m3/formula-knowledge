package com.formulaknowledge.app.data

import kotlinx.coroutines.flow.Flow
import com.google.gson.Gson
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class FormulaRepository(private val database: FormulaDatabase) {

    private val dao = database.standingsDao()
    private val raceDao = database.raceDao()
    private val generalDao = database.generalDao()

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
            val entity = CircuitDetailEntity(
                apiData.round, apiData.gp_name, apiData.circuit_name, apiData.location,
                apiData.length, apiData.laps, apiData.record, apiData.is_sprint,
                apiData.dates.joinToString(","), apiData.status,
                apiData.previous_winner, apiData.most_wins, apiData.most_poles
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

            // Pre-fetch silente dei dettagli e dei risultati per le gare passate/correnti
            coroutineScope {
                entities.filter { it.is_clickable }.forEach { race ->
                    launch {
                        refreshCircuitDetail(race.round)
                        if (race.status == "past") refreshRaceResults(race.round)
                    }
                }
            }
        } catch (e: Exception) {}
    }

    suspend fun refreshCurrentRaceWeek() {
        try {
            val apiData = RetrofitClient.apiService.getCurrentRaceWeek()
            val weatherJson = Gson().toJson(apiData.weather_forecast)
            val entity = RaceWeekEntity(
                1, apiData.gp_name, apiData.country, apiData.city, apiData.circuit_name,
                apiData.round_number, apiData.is_sprint, apiData.dates.joinToString(","), weatherJson
            )
            generalDao.insertRaceWeek(entity)
        } catch (e: Exception) {}
    }
}
