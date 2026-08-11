package com.formulaknowledge.app.data

import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Body
import retrofit2.http.Header

interface F1ApiService {
    @GET("api/v1/raceweek/current")
    suspend fun getCurrentRaceWeek(): RaceWeekResponse

    @GET("api/v1/raceweek/updates")
    suspend fun getLatestCarUpdates(): TeamUpdatesWrapper

    @GET("api/v1/calendar")
    suspend fun getCalendar(): List<CalendarResponse>

    @GET("api/v1/results/{round_number}/{session_type}")
    suspend fun getSessionResults(@Path("round_number") round: Int, @Path("session_type") sessionType: String): List<RaceResultResponse>

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

    @GET("api/v1/constructors/{constructor_id}/stats")
    suspend fun getConstructorStats(@Path("constructor_id") constructorId: String): ConstructorStatsResponse

    @GET("api/v1/drivers/{driver_id}/season_stats")
    suspend fun getDriverSeasonStats(@Path("driver_id") driverId: String): DriverSeasonStatsResponse

    @GET("api/v1/constructors/{constructor_id}/season_stats")
    suspend fun getConstructorSeasonStats(@Path("constructor_id") constructorId: String): ConstructorSeasonStatsResponse
    
    @GET("api/v1/news")
    suspend fun getLatestNews(): List<NewsArticleResponse>

    @GET("api/v1/auth/me")
    suspend fun getMyProfile(@Header("Authorization") authHeader: String): UserProfileResponse

    @PUT("api/v1/auth/preferences")
    suspend fun updatePreferences(@Header("Authorization") authHeader: String, @Body request: UpdatePreferencesRequest): UserProfileResponse
}

data class UpdatePreferencesRequest(
    val favorite_team_id: String?,
    val favorite_driver1_id: String?,
    val favorite_driver2_id: String?,
    val preferences_set: Boolean
)

data class UserProfileResponse(
    val id: String,
    val email: String,
    val full_name: String?,
    val f1_tag: String?,
    val profile_image_url: String?,
    val favorite_constructor_id: String?,
    val favorite_driver1_id: String?,
    val favorite_driver2_id: String?,
    val preferences_set: Boolean,
    val auth_provider: String
)
