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
import androidx.compose.ui.platform.LocalContext
import com.formulaknowledge.app.data.FormulaDatabase
import com.formulaknowledge.app.data.FormulaRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun CalendarScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToResults: (Int, String) -> Unit,
    onNavigateToCircuit: (Int) -> Unit
) {
    val context = LocalContext.current
    val database = remember { FormulaDatabase.getDatabase(context) }
    val repository = remember { FormulaRepository(database) }

    val calendarEntities by repository.calendar.collectAsState(initial = emptyList())
    val calendarItems = calendarEntities.map {
        CalendarResponse(
            it.name, it.country, it.city, it.circuit_name,
            it.date, it.round, it.status, it.is_clickable, it.cancelled
        )
    }

    LaunchedEffect(Unit) {
        repository.refreshCalendar()
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
                verticalArrangement = Arrangement.spacedBy(8.dp),
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
