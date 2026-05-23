package com.formulaknowledge.app.utils

import androidx.compose.ui.graphics.Color

object F1Utils {
    fun getDriverIdFromName(fullName: String): String {
        val lastName = fullName.split(" ").lastOrNull()?.uppercase() ?: ""
        val lowerLast = lastName.lowercase()
            .replace("ü", "u")
            .replace("é", "e")
            .replace(" jr.", "")
        
        return when {
            lowerLast.contains("sainz") -> "sainz"
            lowerLast == "verstappen" -> "max_verstappen"
            lowerLast == "lindblad" || lowerLast == "limblad" -> "arvid_lindblad"
            else -> lowerLast
        }
    }

    fun getConstructorIdForStats(fullName: String): String {
        val lower = fullName.lowercase()
        return when {
            lower.contains("mercedes") -> "mercedes"
            lower.contains("ferrari") -> "ferrari"
            lower.contains("red bull") || lower.contains("redbull") -> "red_bull"
            lower.contains("mclaren") -> "mclaren"
            lower.contains("aston") -> "aston_martin"
            lower.contains("alpine") -> "alpine"
            lower.contains("williams") -> "williams"
            lower.contains("racing bulls") || lower.contains("rb") || lower.contains("alphatauri") -> "rb"
            lower.contains("haas") -> "haas"
            lower.contains("audi") || lower.contains("sauber") || lower.contains("alfa") -> "audi"
            lower.contains("cadillac") -> "cadillac"
            else -> lower.replace(" ", "_")
        }
    }

    fun getTeamColor(teamName: String?): Color {
        val lower = teamName?.lowercase() ?: return Color.Gray
        return when {
            lower.contains("ferrari") -> Color(0xFFE8002D)
            lower.contains("mercedes") -> Color(0xFF27F4D2)
            lower.contains("red bull") || lower.contains("redbull") -> Color(0xFF3671C6)
            lower.contains("mclaren") -> Color(0xFFFF8000)
            lower.contains("aston martin") -> Color(0xFF229971)
            lower.contains("alpine") -> Color(0xFF00A1E8)
            lower.contains("williams") -> Color(0xFF1868DB)
            lower.contains("racing bulls") || lower.contains("rb") || lower.contains("alphatauri") -> Color(0xFF6692FF)
            lower.contains("audi") || lower.contains("sauber") || lower.contains("alfa romeo") -> Color(0xFFFF2D00)
            lower.contains("haas") -> Color(0xFFDEE1E2)
            lower.contains("cadillac") -> Color(0xFFAAAAAD)
            else -> Color.Gray
        }
    }
}