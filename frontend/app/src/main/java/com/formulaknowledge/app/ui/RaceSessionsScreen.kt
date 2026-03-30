package com.formulaknowledge.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.formulaknowledge.app.data.RaceWeekResponse

@Composable
fun RaceSessionsScreen(raceWeek: RaceWeekResponse?) {
    val isSprint = raceWeek?.is_sprint ?: false
    
    val sessions = if (isSprint) {
        listOf(
            SessionInfo("FREE PRACTICE 1", "FRIDAY", "11:30"),
            SessionInfo("SPRINT QUALIFYING", "FRIDAY", "15:30"),
            SessionInfo("SPRINT RACE", "SATURDAY", "11:00"),
            SessionInfo("QUALIFYING", "SATURDAY", "15:00", isMajor = true),
            SessionInfo("GRAND PRIX", "SUNDAY", "14:00", isMajor = true)
        )
    } else {
        listOf(
            SessionInfo("FREE PRACTICE 1", "FRIDAY", "12:30"),
            SessionInfo("FREE PRACTICE 2", "FRIDAY", "16:00"),
            SessionInfo("FREE PRACTICE 3", "SATURDAY", "11:30"),
            SessionInfo("QUALIFYING", "SATURDAY", "15:00", isMajor = true),
            SessionInfo("GRAND PRIX", "SUNDAY", "14:00", isMajor = true)
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(modifier = Modifier.height(46.dp))
        Text(text = "RACE SESSIONS", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic)
        Text(text = if (isSprint) "SPRINT WEEKEND" else "STANDARD WEEKEND", color = Color(0xFF00FFCC), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        
        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 120.dp)) {
            items(sessions) { session ->
                SessionCard(session)
            }
        }
    }
}

data class SessionInfo(val name: String, val day: String, val time: String, val isMajor: Boolean = false)

@Composable
fun SessionCard(session: SessionInfo) {
    val borderColor = if (session.isMajor) Color(0xFF00FFCC).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.1f)
    val backgroundAlpha = if (session.isMajor) 0.08f else 0.04f
    
    Surface(
        modifier = Modifier.fillMaxWidth().height(68.dp), // Altezza ridotta
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = backgroundAlpha),
        border = BorderStroke(if (session.isMajor) 1.2.dp else 0.5.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = session.day, color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp, fontWeight = FontWeight.Black)
                Text(text = session.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timer, null, tint = if (session.isMajor) Color(0xFF00FFCC) else Color.White.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(text = session.time, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}
