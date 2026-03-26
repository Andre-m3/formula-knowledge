package com.formulaknowledge.app.data

data class CalendarResponse(
    val name: String,
    val country: String,
    val city: String,
    val date: String,
    val round: Int,
    val status: String,
    val is_clickable: Boolean
)
