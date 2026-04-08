package com.formulaknowledge.app.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.sp
import com.formulaknowledge.app.data.RaceResultResponse
import com.formulaknowledge.app.data.FormulaDatabase
import com.formulaknowledge.app.data.FormulaRepository

@Composable
fun RaceResultsScreen(
    roundNumber: Int, 
    gpName: String,
    onDriverClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val database = remember { FormulaDatabase.getDatabase(context) }
    val repository = remember { FormulaRepository(database) }

    val resultsEntities by repository.getRaceResults(roundNumber).collectAsState(initial = emptyList())
    val results = resultsEntities.map { RaceResultResponse(it.position, it.driver, it.team, it.points, it.time) }
    
    val isLoading = results.isEmpty()

    LaunchedEffect(roundNumber) {
        repository.refreshRaceResults(roundNumber)
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(modifier = Modifier.height(26.dp))

        Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.BottomStart) {
            val resourceId = remember {
                context.resources.getIdentifier("flag_chequered", "drawable", context.packageName)
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
                        contentDescription = "Chequered Flag",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().alpha(0.35f)
                    )
                }
            }

            Column {
                Text(
                    text = "RACE\nRESULTS",
                    color = Color.White,
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    letterSpacing = (-3).sp,
                    lineHeight = 44.sp
                )
                Text(
                    text = "${gpName.uppercase().replace(" GRAND PRIX", "")} GP",
                    color = Color(0xFF00FFCC),
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    letterSpacing = (-2).sp,
                    modifier = Modifier.offset(y = (-8).dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            Column {
                ShimmerPodiumHorizontalCard(height = 86.dp)
                Spacer(modifier = Modifier.height(8.dp))
                ShimmerPodiumHorizontalCard(height = 72.dp)
                Spacer(modifier = Modifier.height(8.dp))
                ShimmerPodiumHorizontalCard(height = 72.dp)
                Spacer(modifier = Modifier.height(12.dp))
                repeat(7) { ShimmerResultRow() }
            }
        } else {
            val podiumResults = results.take(3)
            val otherResults = results.drop(3)

            LazyColumn(contentPadding = PaddingValues(bottom = 120.dp)) {
                item {
                    PodiumList(podiumResults, onDriverClick)
                    Spacer(modifier = Modifier.height(12.dp))
                }

                items(otherResults) { result ->
                    ResultRow(result, onDriverClick)
                }
            }
        }
    }
}

@Composable
fun PodiumList(results: List<RaceResultResponse>, onDriverClick: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val p1 = results.find { it.position == 1 }
        val p2 = results.find { it.position == 2 }
        val p3 = results.find { it.position == 3 }

        p1?.let { PodiumHorizontalCard(it, Color(0xFFFFD700), onDriverClick) }
        p2?.let { PodiumHorizontalCard(it, Color(0xFFC0C0C0), onDriverClick) }
        p3?.let { PodiumHorizontalCard(it, Color(0xFFCD7F32), onDriverClick) }
    }
}

@Composable
fun PodiumHorizontalCard(result: RaceResultResponse, color: Color, onDriverClick: (String) -> Unit) {
    val driverNameParts = result.driver.split(" ")
    val lastName = driverNameParts.lastOrNull()?.uppercase() ?: ""
    val firstName = driverNameParts.dropLast(1).joinToString(" ").uppercase()
    val teamName = shortTeamName(result.team).uppercase()
    
    val isDnf = isDnfOrDns(result.time)
    val isDns = isDnf && (result.time.lowercase().contains("dns") || result.time.lowercase().contains("withdrawn"))
    val isDsq = isDnf && (result.time.lowercase().contains("dsq") || result.time.lowercase().contains("disqualified"))
    val pointsOrStatus = if (isDnf) {
        if (isDsq) "DSQ" else if (isDns) "DNS" else "DNF"
    } else "${result.points} PTS"
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (result.position == 1) 78.dp else 68.dp)
            .clickable { onDriverClick(result.driver) },
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.02f),
        border = BorderStroke(if (result.position == 1) 2.dp else 1.dp, color.copy(alpha = if (result.position == 1) 0.8f else 0.4f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(color.copy(alpha = 0.15f), Color.Transparent),
                        startX = 0f,
                        endX = size.width * 0.6f
                    )
                )
            }
            
            Text(
                text = "P${result.position}",
                color = color.copy(alpha = 0.05f),
                fontSize = if (result.position == 1) 110.sp else 90.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 20.dp, y = 5.dp)
            )

            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = color,
                    shape = CircleShape,
                    modifier = Modifier.size(if (result.position == 1) 42.dp else 34.dp),
                    shadowElevation = if (result.position == 1) 8.dp else 0.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = result.position.toString(),
                            color = Color.Black,
                            fontSize = if (result.position == 1) 24.sp else 18.sp,
                            fontWeight = FontWeight.Black,
                            fontStyle = FontStyle.Italic
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                    Text(
                        text = firstName,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = if (result.position == 1) 14.sp else 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        lineHeight = 16.sp
                    )
                    Text(
                        text = lastName,
                        color = Color.White,
                        fontSize = if (result.position == 1) 22.sp else 19.sp,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        lineHeight = 24.sp
                    )
                    Text(
                        text = teamName,
                        color = color,
                        fontSize = if (result.position == 1) 11.sp else 10.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 13.sp
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = pointsOrStatus,
                        color = if (isDnf) Color(0xFFFF0033) else Color.White,
                        fontSize = if (isDnf) (if (result.position == 1) 26.sp else 20.sp) else (if (result.position == 1) 24.sp else 18.sp),
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic
                    )
                    if (result.position == 1) {
                        Text(
                            text = "WINNER", 
                            color = color, 
                            fontSize = 10.sp, 
                            fontWeight = FontWeight.Black, 
                            modifier = Modifier.offset(y = (-4).dp),
                            letterSpacing = 1.sp
                        )
                    } else if (!isDnf) {
                        Text(
                            text = result.time,
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.offset(y = (-4).dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ShimmerPodiumHorizontalCard(height: androidx.compose.ui.unit.Dp) {
    val shimmerColors = listOf(Color.White.copy(alpha = 0.05f), Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.05f))
    val transition = rememberInfiniteTransition(label = "")
    val translateAnim = transition.animateFloat(initialValue = 0f, targetValue = 1000f, animationSpec = infiniteRepeatable(animation = tween(1200, easing = LinearEasing), repeatMode = RepeatMode.Restart), label = "")
    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )
    Box(modifier = Modifier.fillMaxWidth().height(height).background(brush, RoundedCornerShape(16.dp)))
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
    val isDns = isDnf && (result.time.lowercase().contains("dns") || result.time.lowercase().contains("withdrawn"))
    val isDsq = isDnf && (result.time.lowercase().contains("dsq") || result.time.lowercase().contains("disqualified"))
    val positionText = result.position.toString()
    val statusColor = Color.White.copy(alpha = 0.4f)
    val teamSubtitle = shortTeamName(result.team)
    val pointsOrStatusText = if (isDnf) {
        if (isDsq) "DSQ" else if (isDns) "DNS" else "DNF"
    } else "${result.points} PTS"
    val pointsColor = if (isDnf) Color(0xFFFF0033) else Color(0xFF00FFCC)

    Surface(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).clickable { onDriverClick(result.driver) }, shape = RoundedCornerShape(12.dp), color = Color.White.copy(alpha = 0.03f)) {
        Row(modifier = Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = positionText, color = statusColor, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(32.dp), lineHeight = 16.sp)
            Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                Text(text = result.driver.uppercase(), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 17.sp)
                Text(text = teamSubtitle.uppercase(), color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp, fontWeight = FontWeight.Medium, lineHeight = 12.sp, maxLines = 1)
            }
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(85.dp), verticalArrangement = Arrangement.Center) {
                Text(text = pointsOrStatusText, color = pointsColor, fontSize = if (isDnf) 16.sp else 14.sp, fontWeight = FontWeight.Black, lineHeight = 16.sp)
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
        lower.contains("racing bulls") || lower.contains("rb") || lower.contains("alphatauri") -> "Racing Bulls"
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
