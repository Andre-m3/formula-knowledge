package com.formulaknowledge.app.data

import kotlinx.coroutines.flow.Flow

class FormulaRepository(private val database: FormulaDatabase) {

    private val dao = database.standingsDao()

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
}
