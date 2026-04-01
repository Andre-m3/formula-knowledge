package com.formulaknowledge.app.utils

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object TimeUtils {
    
    /**
     * Converte una stringa UTC "tagliata" dal backend (es. "15:00")
     * nell'orario locale del dispositivo (es. "16:00").
     */
    fun formatUtcToLocalTime(utcString: String?): String {
        if (utcString.isNullOrEmpty() || !utcString.contains(":")) return utcString ?: "TBD"

        return try {
            // Creiamo una data fittizia basata su OGGI per far capire ad Android 
            // se in questo momento siamo in ora solare (+1) o legale (+2)
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(java.util.Date())
            
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC") // Diciamo al parser che l'input è UTC
            
            // Eseguiamo il parsing unendo la data di oggi all'orario UTC
            val parsedDate = inputFormat.parse("$todayStr $utcString") ?: return "TBD"

            // 2. Creiamo un formatter per l'output che vedrà l'utente (solo ore e minuti)
            val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            // Impostiamo il fuso orario corrente del telefono!
            outputFormat.timeZone = TimeZone.getDefault()

            // Restituiamo l'orario convertito
            outputFormat.format(parsedDate)
            
        } catch (e: Exception) {
            utcString ?: "TBD"
        }
    }
}