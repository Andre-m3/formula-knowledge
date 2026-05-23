package com.formulaknowledge.app.data

data class RaceResultResponse(
    val position: Int,
    val driver: String,
    val team: String,
    val points: Int,
    val time: String,
    val q1: String? = null,
    val q2: String? = null,
    val q3: String? = null
)
