package com.formulaknowledge.app.data

data class TeamUpdatesResponse(
    val team_name: String,
    val team_color_hex: String,
    val updates: List<String>
)