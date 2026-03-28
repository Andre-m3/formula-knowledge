package com.formulaknowledge.app.data

data class RaceWeekResponse(
    val gp_name: String,
    val country: String,
    val city: String,
    val circuit_name: String? = null,
    val round_number: Int,
    val is_sprint: Boolean,
    val dates: List<String>,
    val weather_forecast: WeatherForecast?
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
