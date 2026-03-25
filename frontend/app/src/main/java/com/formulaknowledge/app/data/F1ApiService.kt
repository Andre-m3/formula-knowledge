package com.formulaknowledge.app.data

import com.formulaknowledge.app.ui.CalendarResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface F1ApiService {
    @GET("/api/v1/raceweek/updates")
    suspend fun getLatestCarUpdates(): List<TeamUpdatesResponse>

    @GET("/api/v1/raceweek/current")
    suspend fun getCurrentRaceWeek(): RaceWeekResponse

    @GET("/api/v1/calendar")
    suspend fun getCalendar(): List<CalendarResponse>

    @GET("/api/v1/results/{round_number}")
    suspend fun getResults(@Path("round_number") roundNumber: Int): List<RaceResultResponse>
}
