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
import com.formulaknowledge.app.data.CalendarResponse
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun CalendarScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToResults: (Int, String) -> Unit
) {
    val mockCalendar = listOf(
        CalendarResponse("Australian Grand Prix", "Australia", "Melbourne", "2026-03-01", 1, "past", true),
        CalendarResponse("Chinese Grand Prix", "China", "Shanghai", "2026-03-22", 2, "past", true),
        CalendarResponse("Japanese Grand Prix", "Japan", "Suzuka", "2026-04-05", 3, "current", true),
        CalendarResponse("Bahrain Grand Prix", "Bahrain", "Sakhir", "2026-04-19", 4, "future", false),
        CalendarResponse("Saudi Arabian Grand Prix", "Saudi Arabia", "Jeddah", "2026-05-03", 5, "future", false),
        CalendarResponse("Miami Grand Prix", "USA", "Miami", "2026-05-17", 6, "future", false),
        CalendarResponse("Emilia Romagna Grand Prix", "Italy", "Imola", "2026-05-31", 7, "future", false),
        CalendarResponse("Monaco Grand Prix", "Monaco", "Monte Carlo", "2026-06-07", 8, "future", false),
        CalendarResponse("Spanish Grand Prix", "Spain", "Barcelona", "2026-06-21", 9, "future", false),
        CalendarResponse("Canadian Grand Prix", "Canada", "Montreal", "2026-07-05", 10, "future", false)
    )

    var calendarItems by remember { mutableStateOf<List<CalendarResponse>>(mockCalendar) }

    LaunchedEffect(Unit) {
        try {
            val updatedCalendar = RetrofitClient.apiService.getCalendar()
            if (updatedCalendar.isNotEmpty()) {
                calendarItems = updatedCalendar
            }
        } catch (e: Exception) { }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(modifier = Modifier.height(40.dp))
        
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

        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(calendarItems) { race ->
                    CalendarRaceCard(race, onClick = {
                        if (race.status == "past") {
                            onNavigateToResults(race.round, race.name)
                        } else if (race.status == "current" || race.is_clickable) {
                            onNavigateToHome()
                        }
                    })
                }
            }
        }
    }
}

@Composable
fun CalendarRaceCard(race: CalendarResponse, onClick: () -> Unit) {
    val isPast = race.status == "past"
    val isCurrent = race.status == "current"
    val isFuture = race.status == "future"
    
    val borderColor = when {
        isCurrent -> Color(0xFF00FFCC).copy(alpha = 0.8f)
        isPast -> Color.White.copy(alpha = 0.15f)
        else -> Color.White.copy(alpha = 0.05f)
    }

    val backgroundColor = when {
        isCurrent -> Color(0xFF00FFCC).copy(alpha = 0.05f)
        isPast -> Color.White.copy(alpha = 0.03f)
        else -> Color.White.copy(alpha = 0.01f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(18.dp))
            .border(0.5.dp, borderColor, RoundedCornerShape(18.dp))
            .clickable(enabled = race.is_clickable || isPast || isCurrent) { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "ROUND ${race.round}",
                color = if (isCurrent) Color(0xFF00FFCC) else Color.White.copy(alpha = 0.4f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black
            )
            // 1. Nome GP leggermente più grande
            Text(
                text = race.name.uppercase(),
                color = if (isPast || isCurrent) Color.White else Color.White.copy(alpha = 0.3f),
                fontSize = 19.sp, 
                fontWeight = FontWeight.ExtraBold,
                fontStyle = if (isCurrent) FontStyle.Italic else FontStyle.Normal,
                lineHeight = 20.sp
            )
            // 1. Ridotto distanziamento tra nome e luogo
            Text(
                text = "${race.city.uppercase()}, ${race.country}",
                color = if (isFuture) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.offset(y = (-4).dp)
            )
        }
        
        Column(horizontalAlignment = Alignment.End) {
            val dateObj = try { LocalDate.parse(race.date) } catch(e: Exception) { LocalDate.now() }
            Text(
                text = dateObj.dayOfMonth.toString(),
                color = if (isPast || isCurrent) Color.White else Color.White.copy(alpha = 0.2f),
                fontSize = 24.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = dateObj.format(DateTimeFormatter.ofPattern("MMM")).uppercase(),
                color = if (isPast || isCurrent) Color(0xFF00FFCC) else Color.White.copy(alpha = 0.2f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}
