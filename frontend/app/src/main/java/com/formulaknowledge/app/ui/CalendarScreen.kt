package com.formulaknowledge.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.formulaknowledge.app.data.RetrofitClient
import com.formulaknowledge.app.data.CalendarResponse
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun CalendarScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToResults: (Int, String) -> Unit,
    onNavigateToCircuit: (Int) -> Unit
) {
    // Lista completa 2026 come fallback locale
    val full2026Calendar = listOf(
        CalendarResponse("Australian Grand Prix", "Australia", "Melbourne", "Albert Park Circuit", "2026-03-01", 1, "past", true),
        CalendarResponse("Chinese Grand Prix", "China", "Shanghai", "Shanghai International Circuit", "2026-03-22", 2, "past", true),
        CalendarResponse("Japanese Grand Prix", "Japan", "Suzuka", "Suzuka International Racing Course", "2026-04-05", 3, "current", true),
        CalendarResponse("Bahrain Grand Prix", "Bahrain", "Sakhir", "Bahrain International Circuit", "2026-04-19", 4, "future", false, true),
        CalendarResponse("Saudi Arabian Grand Prix", "Saudi Arabia", "Jeddah", "Jeddah Corniche Circuit", "2026-05-03", 5, "future", false, true),
        CalendarResponse("Miami Grand Prix", "USA", "Miami", "Miami International Autodrome", "2026-05-17", 6, "future", false),
        CalendarResponse("Emilia Romagna Grand Prix", "Italy", "Imola", "Autodromo Enzo e Dino Ferrari", "2026-05-31", 7, "future", false),
        CalendarResponse("Monaco Grand Prix", "Monaco", "Monte Carlo", "Circuit de Monaco", "2026-06-07", 8, "future", false),
        CalendarResponse("Spanish Grand Prix", "Spain", "Barcelona", "Circuit de Barcelona-Catalunya", "2026-06-21", 9, "future", false),
        CalendarResponse("Canadian Grand Prix", "Canada", "Montreal", "Circuit Gilles Villeneuve", "2026-07-05", 10, "future", false),
        CalendarResponse("Austrian Grand Prix", "Austria", "Spielberg", "Red Bull Ring", "2026-07-19", 11, "future", false),
        CalendarResponse("British Grand Prix", "UK", "Silverstone", "Silverstone Circuit", "2026-08-02", 12, "future", false),
        CalendarResponse("Belgian Grand Prix", "Belgium", "Spa", "Circuit de Spa-Francorchamps", "2026-08-30", 13, "future", false),
        CalendarResponse("Dutch Grand Prix", "Netherlands", "Zandvoort", "Circuit Zandvoort", "2026-09-06", 14, "future", false),
        CalendarResponse("Italian Grand Prix", "Italy", "Monza", "Autodromo Nazionale Monza", "2026-09-20", 15, "future", false),
        CalendarResponse("Azerbaijan Grand Prix", "Azerbaijan", "Baku", "Baku City Circuit", "2026-10-04", 16, "future", false),
        CalendarResponse("Singapore Grand Prix", "Singapore", "Marina Bay", "Marina Bay Street Circuit", "2026-10-18", 17, "future", false),
        CalendarResponse("United States Grand Prix", "USA", "Austin", "Circuit of the Americas", "2026-11-01", 18, "future", false),
        CalendarResponse("Mexico City Grand Prix", "Mexico", "Mexico City", "Autódromo Hermanos Rodríguez", "2026-11-08", 19, "future", false),
        CalendarResponse("São Paulo Grand Prix", "Brazil", "Interlagos", "Autódromo José Carlos Pace", "2026-11-22", 20, "future", false),
        CalendarResponse("Las Vegas Grand Prix", "USA", "Las Vegas", "Las Vegas Strip Circuit", "2026-12-05", 21, "future", false),
        CalendarResponse("Qatar Grand Prix", "Qatar", "Lusail", "Lusail International Circuit", "2026-12-13", 22, "future", false),
        CalendarResponse("Abu Dhabi Grand Prix", "UAE", "Yas Marina", "Yas Marina Circuit", "2026-12-20", 23, "future", false)
    )

    var calendarItems by remember { mutableStateOf<List<CalendarResponse>>(full2026Calendar) }

    LaunchedEffect(Unit) {
        try {
            val updatedCalendar = RetrofitClient.apiService.getCalendar()
            if (updatedCalendar.isNotEmpty()) {
                calendarItems = updatedCalendar
            }
        } catch (e: Exception) { }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(modifier = Modifier.height(46.dp)) 
        
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
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                items(calendarItems) { race ->
                    CalendarRaceCard(race, onClick = {
                        onNavigateToCircuit(race.round)
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
    val isCancelled = race.cancelled == true
    
    val borderColor = when {
        isCancelled -> Color.Red.copy(alpha = 0.3f)
        isCurrent -> Color(0xFF00FFCC).copy(alpha = 0.8f)
        isPast -> Color.White.copy(alpha = 0.15f)
        else -> Color.White.copy(alpha = 0.05f)
    }

    val backgroundColor = when {
        isCancelled -> Color.Red.copy(alpha = 0.02f)
        isCurrent -> Color(0xFF00FFCC).copy(alpha = 0.05f)
        isPast -> Color.White.copy(alpha = 0.03f)
        else -> Color.White.copy(alpha = 0.01f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp) // Fixed height for alignment
            .background(backgroundColor, RoundedCornerShape(18.dp))
            .border(0.5.dp, borderColor, RoundedCornerShape(18.dp))
            .clickable(enabled = !isCancelled) { onClick() }
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
            Text(
                text = if (isCancelled) "CANCELLED" else "ROUND ${race.round}",
                color = when {
                    isCancelled -> Color.Red.copy(alpha = 0.6f)
                    isCurrent -> Color(0xFF00FFCC)
                    else -> Color.White.copy(alpha = 0.4f)
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 13.sp
            )
            Text(
                text = race.name.uppercase(),
                color = if (isCancelled) Color.White.copy(alpha = 0.2f) else if (isPast || isCurrent) Color.White else Color.White.copy(alpha = 0.3f),
                fontSize = 19.sp, 
                fontWeight = FontWeight.ExtraBold,
                fontStyle = if (isCurrent) FontStyle.Italic else FontStyle.Normal,
                lineHeight = 22.sp,
                style = if (isCancelled) TextStyle(textDecoration = TextDecoration.LineThrough) else TextStyle.Default
            )
            Text(
                text = "${race.city.uppercase()}, ${race.country}",
                color = if (isCancelled) Color.White.copy(alpha = 0.1f) else if (isFuture) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 14.sp
            )
        }
        
        if (!isCancelled) {
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                val dateObj = try { LocalDate.parse(race.date) } catch(e: Exception) { LocalDate.now() }
                Text(
                    text = dateObj.dayOfMonth.toString(),
                    color = if (isPast || isCurrent) Color.White else Color.White.copy(alpha = 0.2f),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 26.sp
                )
                Text(
                    text = dateObj.format(DateTimeFormatter.ofPattern("MMM")).uppercase(),
                    color = if (isPast || isCurrent) Color(0xFF00FFCC) else Color.White.copy(alpha = 0.2f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 13.sp
                )
            }
        }
    }
}
