package com.formulaknowledge.app.ui

import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.formulaknowledge.app.data.SessionTimes
import com.formulaknowledge.app.utils.TimeUtils
import java.text.SimpleDateFormat
import java.util.Locale

enum class SessionStatus { FUTURE, ONGOING, CONCLUDED }

data class SessionInfo(val name: String, val day: String, val time: String, val dateStr: String, val isMajor: Boolean = false, val status: SessionStatus = SessionStatus.FUTURE)

fun getSessionStatus(sessionDay: String, sessionTimeLocal: String, gpStatus: String): SessionStatus {
    if (gpStatus == "past") return SessionStatus.CONCLUDED
    if (gpStatus == "future") return SessionStatus.FUTURE
    
    val currentDay = java.time.LocalDate.now().dayOfWeek.name
    val days = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")
    val currentDayIdx = days.indexOf(currentDay)
    val sessionDayIdx = days.indexOf(sessionDay.uppercase())
    
    if (sessionDayIdx < currentDayIdx) return SessionStatus.CONCLUDED
    if (sessionDayIdx > currentDayIdx) return SessionStatus.FUTURE
    
    try {
        val formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
        val sTime = java.time.LocalTime.parse(sessionTimeLocal, formatter)
        val nowTime = java.time.LocalTime.now()
        
        if (nowTime.isBefore(sTime)) return SessionStatus.FUTURE
        if (nowTime.isAfter(sTime) && nowTime.isBefore(sTime.plusHours(2))) return SessionStatus.ONGOING
        return SessionStatus.CONCLUDED
    } catch (e: Exception) {
        return SessionStatus.FUTURE
    }
}

@Composable
fun RaceSessionsScreen(isSprint: Boolean, gpName: String, country: String, sessions: SessionTimes?, gpStatus: String, dates: List<String>) {
    val context = LocalContext.current
    var showOngoingDialog by remember { mutableStateOf(false) }
    var showConcludedDialog by remember { mutableStateOf(false) }

    val sessionsList = remember(isSprint, sessions) {
        val list = mutableListOf<SessionInfo>()
        if (sessions == null) return@remember emptyList<SessionInfo>()
        
        val friDate = dates.getOrNull(0) ?: ""
        val satDate = dates.getOrNull(1) ?: ""
        val sunDate = dates.getOrNull(2) ?: ""

        if (isSprint) {
            sessions.fp1?.let { val t = TimeUtils.formatUtcToLocalTime(it); list.add(SessionInfo("FREE PRACTICE 1", "FRIDAY", t, friDate, false, getSessionStatus("FRIDAY", t, gpStatus))) }
            sessions.sprint_shootout?.let { val t = TimeUtils.formatUtcToLocalTime(it); list.add(SessionInfo("SPRINT QUALI", "FRIDAY", t, friDate, false, getSessionStatus("FRIDAY", t, gpStatus))) }
            sessions.sprint_race?.let { val t = TimeUtils.formatUtcToLocalTime(it); list.add(SessionInfo("SPRINT RACE", "SATURDAY", t, satDate, true, getSessionStatus("SATURDAY", t, gpStatus))) }
            sessions.quali?.let { val t = TimeUtils.formatUtcToLocalTime(it); list.add(SessionInfo("QUALIFYING", "SATURDAY", t, satDate, true, getSessionStatus("SATURDAY", t, gpStatus))) }
            sessions.race?.let { val t = TimeUtils.formatUtcToLocalTime(it); list.add(SessionInfo("GRAND PRIX", "SUNDAY", t, sunDate, true, getSessionStatus("SUNDAY", t, gpStatus))) }
        } else {
            sessions.fp1?.let { val t = TimeUtils.formatUtcToLocalTime(it); list.add(SessionInfo("FREE PRACTICE 1", "FRIDAY", t, friDate, false, getSessionStatus("FRIDAY", t, gpStatus))) }
            sessions.fp2?.let { val t = TimeUtils.formatUtcToLocalTime(it); list.add(SessionInfo("FREE PRACTICE 2", "FRIDAY", t, friDate, false, getSessionStatus("FRIDAY", t, gpStatus))) }
            sessions.fp3?.let { val t = TimeUtils.formatUtcToLocalTime(it); list.add(SessionInfo("FREE PRACTICE 3", "SATURDAY", t, satDate, false, getSessionStatus("SATURDAY", t, gpStatus))) }
            sessions.quali?.let { val t = TimeUtils.formatUtcToLocalTime(it); list.add(SessionInfo("QUALIFYING", "SATURDAY", t, satDate, true, getSessionStatus("SATURDAY", t, gpStatus))) }
            sessions.race?.let { val t = TimeUtils.formatUtcToLocalTime(it); list.add(SessionInfo("GRAND PRIX", "SUNDAY", t, sunDate, true, getSessionStatus("SUNDAY", t, gpStatus))) }
        }
        list
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(modifier = Modifier.height(26.dp))

        Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.BottomStart) {
            val countryFormat = country.lowercase().replace(" ", "_")
            val resourceName = "flag_$countryFormat"
            val resourceId = remember(resourceName) {
                context.resources.getIdentifier(resourceName, "drawable", context.packageName)
            }

            if (resourceId != 0) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.8f)
                        .align(Alignment.CenterEnd)
                        .graphicsLayer {
                            alpha = 0.99f
                            translationX = 20.dp.toPx()
                            translationY = -26.dp.toPx()
                            scaleX = 1.26f
                            scaleY = 1.26f
                        }
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color.Transparent, Color.Black),
                                    startX = 0f,
                                    endX = size.width * 0.6f
                                ),
                                blendMode = BlendMode.DstIn
                            )
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color.Black, Color.Transparent),
                                    startY = size.height * 0.35f,
                                    endY = size.height
                                ),
                                blendMode = BlendMode.DstIn
                            )
                        }
                ) {
                    Image(
                        painter = painterResource(id = resourceId),
                        contentDescription = "Country Flag",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().alpha(0.35f)
                    )
                }
            }

            Column {
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
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        if (sessionsList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Session times not available yet.", color = Color.White.copy(alpha = 0.5f))
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 120.dp)) {
                items(sessionsList) { session ->
                    SessionCard(session) {
                        when(session.status) {
                            SessionStatus.FUTURE -> {
                                val intent = Intent(Intent.ACTION_INSERT).apply {
                                    data = CalendarContract.Events.CONTENT_URI
                                    putExtra(CalendarContract.Events.TITLE, "$gpName - ${session.name}")
                                    // putExtra(CalendarContract.Events.EVENT_LOCATION, country)
                                }
                                context.startActivity(intent)
                            }
                            SessionStatus.ONGOING -> { showOngoingDialog = true }
                            SessionStatus.CONCLUDED -> { showConcludedDialog = true }
                        }
                    }
                }
            }
        }
        
        if (showOngoingDialog) {
            AlertDialog(
                onDismissRequest = { showOngoingDialog = false },
                containerColor = Color(0xFF1E0A0A).copy(alpha = 0.95f),
                title = { Text("SESSIONE IN CORSO", color = Color(0xFFFF8000), fontWeight = FontWeight.Black) },
                text = { Text("Attendi la conclusione della sessione per visualizzare la classifica e i risultati completi.", color = Color.White) },
                confirmButton = { TextButton(onClick = { showOngoingDialog = false }) { Text("OK", color = Color(0xFFFF8000)) } }
            )
        }

        if (showConcludedDialog) {
            AlertDialog(
                onDismissRequest = { showConcludedDialog = false },
                containerColor = Color(0xFF1E0A0A).copy(alpha = 0.95f),
                title = { Text("RISULTATI SESSIONE", color = Color(0xFF00FFCC), fontWeight = FontWeight.Black) },
                text = { Text("I risultati in tempo reale per questa specifica sessione saranno disponibili con le prossime integrazioni API.", color = Color.White) },
                confirmButton = { TextButton(onClick = { showConcludedDialog = false }) { Text("OK", color = Color(0xFF00FFCC)) } }
            )
        }
    }
}

@Composable
fun SessionCard(session: SessionInfo, onClick: () -> Unit) {
    val borderColor = if (session.isMajor) Color(0xFF00FFCC).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.05f)
    val backgroundColor = if (session.isMajor) Color(0xFF00FFCC).copy(alpha = 0.05f) else Color.White.copy(alpha = 0.01f)
    
    val statusText = when (session.status) {
        SessionStatus.FUTURE -> "Aggiungi al calendario"
        SessionStatus.ONGOING -> "Attendi la conclusione"
        SessionStatus.CONCLUDED -> "Vedi risultati"
    }
    val statusColor = when (session.status) {
        SessionStatus.FUTURE -> Color.White.copy(alpha = 0.4f)
        SessionStatus.ONGOING -> Color(0xFFFF8000)
        SessionStatus.CONCLUDED -> Color(0xFF00FFCC)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(74.dp)
            .background(backgroundColor, RoundedCornerShape(18.dp))
            .border(0.5.dp, borderColor, RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
            Text(
                text = session.day,
                color = if (session.isMajor) Color(0xFF00FFCC) else Color.White.copy(alpha = 0.4f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 13.sp
            )
            Text(
                text = session.name.uppercase(),
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                fontStyle = if (session.isMajor) FontStyle.Italic else FontStyle.Normal,
                lineHeight = 22.sp
            )
            Text(
                text = statusText.uppercase(),
                color = statusColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 14.sp
            )
        }
        
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxHeight(), verticalArrangement = Arrangement.Center) {
            Text(
                text = session.time,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 24.sp
            )
            Text(
                text = "LOCAL TIME",
                color = if (session.isMajor) Color(0xFF00FFCC) else Color.White.copy(alpha = 0.2f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 12.sp
            )
        }
    }
}
