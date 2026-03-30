package com.formulaknowledge.app.data

data class DriverStanding(
    val position: Int,
    val driver_name: String,
    val constructor_name: String,
    val points: Int,
    val wins: Int
)

data class ConstructorStanding(
    val position: Int,
    val constructor_name: String,
    val chassis_name: String? = null,
    val points: Int,
    val wins: Int
)
