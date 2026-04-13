package com.formulaknowledge.app.data

data class ConstructorStatsResponse(
    val constructor_id: String,
    val total_races: Int,
    val wins: Int,
    val podiums: Int,
    val driver_championships: Int,
    val constructor_championships: Int,
    
    val first_gp_year: String,
    val first_win: String,
    val pole_positions: Int,
    val fastest_laps: Int,
    val total_points: Float,
    val seasons_entered: Int,
    val best_race_result: String,
    val best_championship_result: String,
    val power_unit: String,
    val team_principal: String,
    val base_location: String,
    val last_updated: String
)