package com.formulaknowledge.app.data

import retrofit2.http.GET
import retrofit2.http.Path

interface F1ApiService {
    @GET("api/v1/raceweek/current")
    suspend fun getCurrentRaceWeek(): RaceWeekResponse

    @GET("api/v1/raceweek/updates")
    suspend fun getLatestCarUpdates(): TeamUpdatesWrapper

    @GET("api/v1/calendar")
    suspend fun getCalendar(): List<CalendarResponse>

    @GET("api/v1/results/{round_number}")
    suspend fun getResults(@Path("round_number") round: Int): List<RaceResultResponse>

    @GET("api/v1/results/{round_number}/updates")
    suspend fun getPastGpUpdates(@Path("round_number") round: Int): List<TeamUpdatesResponse>

    @GET("api/v1/standings/drivers")
    suspend fun getDriverStandings(): List<DriverStanding>

    @GET("api/v1/standings/constructors")
    suspend fun getConstructorStandings(): List<ConstructorStanding>

    @GET("api/v1/circuit/{round_number}")
    suspend fun getCircuitDetails(@Path("round_number") round: Int): CircuitDetailResponse

    @GET("api/v1/drivers/{driver_id}/stats")
    suspend fun getDriverStats(@Path("driver_id") driverId: String): DriverStatsResponse
}
