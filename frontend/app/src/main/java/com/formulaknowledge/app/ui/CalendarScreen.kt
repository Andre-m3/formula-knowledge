package com.formulaknowledge.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.formulaknowledge.app.data.RetrofitClient
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// In Kotlin usiamo classi per i dati dell'API
data class CalendarResponse(
    val name: String,
    val country: String,
    val city: String,
    val date: String,
    val round: Int,
    val status: String,
    val is_clickable: Boolean
)

@Composable
fun CalendarScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToResults: (Int, String) -> Unit
) {
    var calendarItems by remember { mutableStateOf<List<CalendarResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            calendarItems = RetrofitClient.apiService.getCalendar()
            isLoading = false
        } catch (e: Exception) {
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(modifier = Modifier.height(40.dp)) // Spazio alzato
        
        // TITOLO MOLTO GRANDE
        Text(
            text = "CALENDARIO",
            color = Color.White,
            fontSize = 54.sp,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            letterSpacing = (-3).sp,
            lineHeight = 50.sp
        )
        Text(
            text = "2026",
            color = Color(0xFF00FFCC),
            fontSize = 38.sp,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            letterSpacing = (-2).sp,
            modifier = Modifier.offset(y = (-10).dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF00FFCC))
            }
        } else {
            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(calendarItems) { race ->
                        CalendarRaceCard(race, onClick = {
                            if (race.status == "past") {
                                onNavigateToResults(race.round, race.name)
                            } else if (race.is_clickable) {
                                onNavigateToHome()
                            }
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarRaceCard(race: CalendarResponse, onClick: () -> Unit) {
    val isPast = race.status == "past"
    val isCurrent = race.is_clickable && !isPast
    
    val borderColor = when {
        isCurrent -> Color(0xFF00FFCC).copy(alpha = 0.8f)
        isPast -> Color.White.copy(alpha = 0.15f)
        else -> Color.Transparent
    }

    val backgroundColor = if (isCurrent) Color(0xFF00FFCC).copy(alpha = 0.05f) else Color.White.copy(alpha = 0.03f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(18.dp))
            .border(0.5.dp, borderColor, RoundedCornerShape(18.dp))
            .clickable(enabled = race.is_clickable || isPast) { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp), // Altezza ridotta (vertical da 20 a 14)
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "ROUND ${race.round}",
                color = if (isCurrent) Color(0xFF00FFCC) else Color.White.copy(alpha = 0.4f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = race.name.uppercase(),
                color = if (isPast || isCurrent) Color.White else Color.White.copy(alpha = 0.3f),
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                fontStyle = if (isCurrent) FontStyle.Italic else FontStyle.Normal
            )
            // CITTÀ E NAZIONE PIÙ VICINE
            Text(
                text = "${race.city.uppercase()}, ${race.country}",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.offset(y = (-2).dp)
            )
        }
        
        Column(horizontalAlignment = Alignment.End) {
            val dateObj = LocalDate.parse(race.date)
            Text(
                text = dateObj.dayOfMonth.toString(),
                color = if (isPast || isCurrent) Color.White else Color.White.copy(alpha = 0.3f),
                fontSize = 24.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = dateObj.format(DateTimeFormatter.ofPattern("MMM")).uppercase(),
                color = if (isPast || isCurrent) Color(0xFF00FFCC) else Color.White.copy(alpha = 0.3f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}
