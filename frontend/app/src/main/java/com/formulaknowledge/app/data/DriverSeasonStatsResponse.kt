package com.formulaknowledge.app.data

data class DriverSeasonStatsResponse(
    val driver_id: String,
    val year: Int,
    val total_races: Int,
    val wins: Int,
    val second_places: Int,
    val podiums: Int,
    val laps_led: Int,
    val fastest_laps: Int,
    val beat_teammate_race: Int,
    val beat_teammate_quali: Int,
    val pole_positions: Int,
    val front_rows: Int,
    val retirements: Int,
    val q3_appearances: Int,
    val q2_appearances: Int,
    val q1_appearances: Int,
    val sprint_starts: Int,
    val sprint_wins: Int,
    val sprint_top_3: Int,
    val sprint_points_finishes: Int,
    val sprint_points: Int,
    val beat_teammate_sprint: Int,
    val sprint_quali_poles: Int,
    val last_updated: String
)