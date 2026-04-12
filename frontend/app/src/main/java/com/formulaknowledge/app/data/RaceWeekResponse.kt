package com.formulaknowledge.app.data

data class RaceWeekResponse(
    val gp_name: String,
    val country: String,
    val city: String,
    val circuit_name: String? = null,
    val round_number: Int,
    val is_sprint: Boolean,
    val status: String,
    val dates: List<String>,
    val weather_forecast: WeatherForecast?,
    val sessions: SessionTimes
)

data class SessionTimes(
    val fp1: String?,
    val fp2: String?,
    val fp3: String?,
    val sprint_shootout: String?,
    val sprint_race: String?,
    val quali: String?,
    val race: String?
)

data class WeatherForecast(
    val status: String,
    val temp: String,
    val humidity: String,
    val feels_like: String,
    val wind: String,
    val uv: String,
    val rain_probability: String,
    val daily: List<DailyForecast>
)

data class DailyForecast(
    val day: String,
    val status: String,
    val temp_max: String,
    val temp_min: String,
    val wind: String,
    val rain_probability: String
)

data class CircuitDetailResponse(
    val round: Int,
    val gp_name: String,
    val circuit_name: String,
    val location: String,
    val country: String,
    val length: String,
    val corners: Int,
    val laps: Int,
    val record: String,
    val is_sprint: Boolean,
    val dates: List<String>, // AGGIUNTO
    val status: String,      // AGGIUNTO
    val previous_winner: String,
    val most_driver_wins: String,
    val most_constructor_wins: String,
    val most_driver_podiums: String,
    val most_poles: String,
    val num_races_held: Int,
    val sessions: SessionTimes
)
