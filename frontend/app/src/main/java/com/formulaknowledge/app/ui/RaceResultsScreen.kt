package com.formulaknowledge.app.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.formulaknowledge.app.data.RaceResultResponse
import com.formulaknowledge.app.data.RetrofitClient

@Composable
fun RaceResultsScreen(
    roundNumber: Int, 
    gpName: String,
    onDriverClick: (String) -> Unit = {}
) {
    var results by remember { mutableStateOf<List<RaceResultResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(roundNumber) {
        try {
            isLoading = true
            val updatedResults = RetrofitClient.apiService.getResults(roundNumber)
            results = updatedResults
        } catch (e: Exception) {
        } finally {
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(modifier = Modifier.height(46.dp))

        Text(
            text = "RACE RESULTS",
            color = Color.White,
            fontSize = 54.sp,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            letterSpacing = (-3).sp,
            lineHeight = 50.sp
        )
        Text(
            text = "${gpName.uppercase().replace(" GRAND PRIX", "")} GP",
            color = Color(0xFF00FFCC),
            fontSize = 38.sp,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            letterSpacing = (-2).sp,
            modifier = Modifier.offset(y = (-10).dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
                    ShimmerPodiumStep(height = 110.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    ShimmerPodiumStep(height = 135.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    ShimmerPodiumStep(height = 90.dp)
                }
                Spacer(modifier = Modifier.height(24.dp))
                repeat(7) { ShimmerResultRow() }
            }
        } else {
            val podiumResults = results.take(3)
            val otherResults = results.drop(3)

            LazyColumn(contentPadding = PaddingValues(bottom = 120.dp)) {
                item {
                    Podium(podiumResults, onDriverClick)
                    Spacer(modifier = Modifier.height(24.dp))
                }

                items(otherResults) { result ->
                    ResultRow(result, onDriverClick)
                }
            }
        }
    }
}

@Composable
fun Podium(results: List<RaceResultResponse>, onDriverClick: (String) -> Unit) {
    val p1 = results.find { it.position == 1 }
    val p2 = results.find { it.position == 2 }
    val p3 = results.find { it.position == 3 }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom
    ) {
        if (p2 != null) PodiumStep(p2, 125.dp, Color(0xFFC0C0C0), onDriverClick)
        Spacer(modifier = Modifier.width(4.dp))
        if (p1 != null) PodiumStep(p1, 155.dp, Color(0xFFFFD700), onDriverClick)
        Spacer(modifier = Modifier.width(4.dp))
        if (p3 != null) PodiumStep(p3, 110.dp, Color(0xFFCD7F32), onDriverClick)
    }
}

@Composable
fun RowScope.PodiumStep(result: RaceResultResponse, height: androidx.compose.ui.unit.Dp, color: Color, onDriverClick: (String) -> Unit) {
    val driverNameParts = result.driver.split(" ")
    val lastName = driverNameParts.lastOrNull()?.uppercase() ?: ""
    val teamName = shortTeamName(result.team)

    Column(
        modifier = Modifier.weight(1f).clickable { onDriverClick(result.driver) },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = lastName, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black, lineHeight = 18.sp)
        Text(text = teamName, color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp, maxLines = 1, lineHeight = 12.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            modifier = Modifier.fillMaxWidth().height(height),
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
            color = Color.White.copy(alpha = 0.05f),
            border = BorderStroke(1.dp, color.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(8.dp).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = result.position.toString(), color = color, fontSize = 42.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, lineHeight = 44.sp)
                Text(text = "${result.points} PTS", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, lineHeight = 14.sp)
                if (result.position > 1 && !isDnfOrDns(result.time)) {
                    Text(text = result.time, color = Color.White.copy(alpha = 0.5f), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
    }
}

@Composable
fun ShimmerPodiumStep(height: androidx.compose.ui.unit.Dp) {
    val shimmerColors = listOf(Color.White.copy(alpha = 0.05f), Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.05f))
    val transition = rememberInfiniteTransition(label = "")
    val translateAnim = transition.animateFloat(initialValue = 0f, targetValue = 1000f, animationSpec = infiniteRepeatable(animation = tween(1200, easing = LinearEasing), repeatMode = RepeatMode.Restart), label = "")
    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )
    Box(modifier = Modifier.width(100.dp).height(height).background(brush, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)))
}

@Composable
fun ShimmerResultRow() {
    val shimmerColors = listOf(Color.White.copy(alpha = 0.05f), Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.05f))
    val transition = rememberInfiniteTransition(label = "")
    val translateAnim = transition.animateFloat(initialValue = 0f, targetValue = 1000f, animationSpec = infiniteRepeatable(animation = tween(1200, easing = LinearEasing), repeatMode = RepeatMode.Restart), label = "")
    val brush = Brush.linearGradient(colors = shimmerColors, start = Offset.Zero, end = Offset(x = translateAnim.value, y = translateAnim.value))
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).height(42.dp).background(brush, RoundedCornerShape(8.dp)))
}

@Composable
fun ResultRow(result: RaceResultResponse, onDriverClick: (String) -> Unit) {
    val isDnf = isDnfOrDns(result.time)
    val statusText = if (isDnf) if (result.time.lowercase().contains("dns") || result.time.lowercase().contains("withdrawn")) "DNS" else "DNF" else result.position.toString()
    val statusColor = if (isDnf) Color(0xFFFF0033) else Color.White.copy(alpha = 0.4f)
    val teamSubtitle = if (isDnf) "${shortTeamName(result.team)} • ${result.time}" else shortTeamName(result.team)

    Surface(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onDriverClick(result.driver) }, shape = RoundedCornerShape(12.dp), color = Color.White.copy(alpha = 0.03f)) {
        Row(modifier = Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = statusText, color = statusColor, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(45.dp), lineHeight = 16.sp)
            Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                Text(text = result.driver.uppercase(), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 17.sp)
                Text(text = teamSubtitle.uppercase(), color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp, fontWeight = FontWeight.Medium, lineHeight = 12.sp, maxLines = 1)
            }
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(85.dp), verticalArrangement = Arrangement.Center) {
                Text(text = "${result.points} PTS", color = Color(0xFF00FFCC), fontSize = 14.sp, fontWeight = FontWeight.Black, lineHeight = 16.sp)
                if (!isDnf) {
                    Text(text = if (result.position == 1) "WINNER" else result.time, color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp, fontWeight = FontWeight.Bold, lineHeight = 12.sp, maxLines = 1)
                }
            }
        }
    }
}

fun shortTeamName(fullName: String): String {
    val lower = fullName.lowercase()
    return when {
        lower.contains("ferrari") -> "Ferrari"
        lower.contains("mercedes") -> "Mercedes"
        lower.contains("red bull") -> "Red Bull"
        lower.contains("mclaren") -> "McLaren"
        lower.contains("aston martin") -> "Aston Martin"
        lower.contains("alpine") -> "Alpine"
        lower.contains("williams") -> "Williams"
        lower.contains("racing bulls") || lower.contains("rb") || lower.contains("alphatauri") -> "RB"
        lower.contains("audi") || lower.contains("sauber") || lower.contains("alfa romeo") -> "Audi"
        lower.contains("haas") -> "Haas"
        lower.contains("cadillac") -> "Cadillac"
        else -> fullName
    }
}

fun isDnfOrDns(timeStr: String?): Boolean {
    if (timeStr.isNullOrBlank()) return false
    val lower = timeStr.lowercase()
    if (lower == "finished" || lower.contains("lap")) return false
    if (timeStr.contains(":") || timeStr.contains("+")) return false
    return true
}
