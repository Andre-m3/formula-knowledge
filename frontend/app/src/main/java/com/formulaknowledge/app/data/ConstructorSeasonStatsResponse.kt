package com.formulaknowledge.app.data

data class ConstructorSeasonStatsResponse(
    val constructor_id: String,
    val year: Int,
    val total_races: Int,
    val wins: Int,
    val podiums: Int,
    val fastest_laps: Int,
    val pole_positions: Int,
    val front_rows: Int,
    val one_two_finishes: Int,
    val double_dnfs: Int,
    val last_updated: String
)