package com.formulaknowledge.app.data

data class DriverStatsResponse(
    val driver_id: String,
    val total_races: Int,
    val wins: Int,
    val podiums: Int,
    val pole_positions: Int,
    val wins_from_pole: Int,
    val world_championships: Int,

    val best_race_result: String,
    val best_championship_result: String,
    val best_grid_position: String,
    val fastest_laps: Int,
    val dns_count: Int,
    val dnf_count: Int,
    val dsq_count: Int,

    val sprint_starts: Int,
    val sprint_wins: Int,
    val sprint_top_3: Int,
    val best_sprint_result: String,
    val best_sprint_grid_position: String,

    val place_of_birth: String,
    val date_of_birth: String,
    val first_gp: String,
    val first_win: String,
    val hat_tricks: Int,
    val grand_slams: Int,

    val last_updated: String
)