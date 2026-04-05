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
import com.formulaknowledge.app.data.SessionTimes
import com.formulaknowledge.app.utils.TimeUtils

@Composable
fun RaceSessionsScreen(isSprint: Boolean, gpName: String, country: String, sessions: SessionTimes?) {
    val context = LocalContext.current
    val sessionsList = remember(isSprint, sessions) {
        val list = mutableListOf<SessionInfo>()
        if (sessions == null) return@remember emptyList<SessionInfo>()

        if (isSprint) {
            sessions.fp1?.let { list.add(SessionInfo("FREE PRACTICE 1", "FRIDAY", TimeUtils.formatUtcToLocalTime(it))) }
            sessions.sprint_shootout?.let { list.add(SessionInfo("SPRINT QUALI", "FRIDAY", TimeUtils.formatUtcToLocalTime(it))) }
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
                            scaleX = 1.2f
                            scaleY = 1.2f
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
    val borderColor = if (session.isMajor) Color(0xFF00FFCC).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.05f)
    val backgroundColor = if (session.isMajor) Color(0xFF00FFCC).copy(alpha = 0.05f) else Color.White.copy(alpha = 0.01f)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(74.dp)
            .background(backgroundColor, RoundedCornerShape(18.dp))
            .border(0.5.dp, borderColor, RoundedCornerShape(18.dp))
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
