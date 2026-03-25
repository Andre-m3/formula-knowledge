package com.formulaknowledge.app.data

data class RaceWeekResponse(
    val gp_name: String,
    val country: String,
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
    val uv: String
)
