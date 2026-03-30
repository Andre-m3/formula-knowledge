package com.formulaknowledge.app.data

data class TeamUpdatesResponse(
    val team_name: String,
    val team_color_hex: String,
    val updates: List<String>
)

data class TeamUpdatesWrapper(
    val status: String,
    val gp: String,
    val data: List<TeamUpdatesResponse> = emptyList()
)
