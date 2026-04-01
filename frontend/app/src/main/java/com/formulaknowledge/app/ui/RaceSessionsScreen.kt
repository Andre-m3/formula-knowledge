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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.formulaknowledge.app.data.SessionTimes
import com.formulaknowledge.app.utils.TimeUtils

@Composable
fun RaceSessionsScreen(isSprint: Boolean, gpName: String, sessions: SessionTimes?) {
    val sessionsList = remember(isSprint, sessions) {
        val list = mutableListOf<SessionInfo>()
        if (sessions == null) return@remember emptyList<SessionInfo>()

        if (isSprint) {
            sessions.fp1?.let { list.add(SessionInfo("FREE PRACTICE 1", "FRIDAY", TimeUtils.formatUtcToLocalTime(it))) }
            sessions.sprint_shootout?.let { list.add(SessionInfo("SPRINT QUALIFYING", "FRIDAY", TimeUtils.formatUtcToLocalTime(it))) }
            sessions.sprint_race?.let { list.add(SessionInfo("SPRINT RACE", "SATURDAY", TimeUtils.formatUtcToLocalTime(it), isMajor = true)) }
            sessions.quali?.let { list.add(SessionInfo("QUALIFYING", "SATURDAY", TimeUtils.formatUtcToLocalTime(it), isMajor = true)) }
            sessions.race?.let { list.add(SessionInfo("GRAND PRIX", "SUNDAY", TimeUtils.formatUtcToLocalTime(it), isMajor = true)) }
        } else {
            sessions.fp1?.let { list.add(SessionInfo("FREE PRACTICE 1", "FRIDAY", TimeUtils.formatUtcToLocalTime(it))) }
            sessions.fp2?.let { list.add(SessionInfo("FREE PRACTICE 2", "FRIDAY", TimeUtils.formatUtcToLocalTime(it))) }
            sessions.fp3?.let { list.add(SessionInfo("FREE PRACTICE 3", "SATURDAY", TimeUtils.formatUtcToLocalTime(it))) }
            sessions.quali?.let { list.add(SessionInfo("QUALIFYING", "SATURDAY", TimeUtils.formatUtcToLocalTime(it), isMajor = true)) }
            sessions.race?.let { list.add(SessionInfo("GRAND PRIX", "SUNDAY", TimeUtils.formatUtcToLocalTime(it), isMajor = true)) }
        }
        list
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(modifier = Modifier.height(46.dp))
        Text(
            text = "RACE\nSESSIONS",
            color = Color.White,
            fontSize = 54.sp,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            letterSpacing = (-3).sp,
            lineHeight = 44.sp
        )
        Text(
            text = "${gpName.uppercase().replace(" GRAND PRIX", "")} \u2022 ${if (isSprint) "SPRINT WEEKEND" else "STANDARD WEEKEND"}",
            color = Color(0xFF00FFCC),
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            letterSpacing = (-1).sp,
            modifier = Modifier.offset(y = (-8).dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))

        if (sessionsList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Session times not available yet.", color = Color.White.copy(alpha = 0.5f))
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 120.dp)) {
                items(sessionsList) { session ->
                    SessionCard(session)
                }
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
