package com.formulaknowledge.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.formulaknowledge.app.data.RaceResultResponse
import com.formulaknowledge.app.data.RetrofitClient

@Composable
fun RaceResultsScreen(roundNumber: Int, gpName: String) {
    var results by remember { mutableStateOf<List<RaceResultResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(roundNumber) {
        try {
            isLoading = true
            val updatedResults = RetrofitClient.apiService.getResults(roundNumber)
            if (updatedResults.isNotEmpty()) {
                results = updatedResults
            }
        } catch (e: Exception) { } finally {
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(modifier = Modifier.height(46.dp)) 

        val gpNameUpper = gpName.uppercase()
        val parts = gpNameUpper.split(" GRAND PRIX")
        val countryName = parts[0]

        Text(
            text = buildAnnotatedString {
                withStyle(style = SpanStyle(fontSize = 56.sp)) {
                    append(countryName)
                }
                if (gpNameUpper.contains("GRAND PRIX")) {
                    append("\n")
                    withStyle(style = SpanStyle(fontSize = 32.sp)) {
                        append("GRAND PRIX")
                    }
                }
            },
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            letterSpacing = (-2).sp,
            lineHeight = 36.sp
        )

        Text(
            text = "RACE RESULTS • ROUND $roundNumber",
            color = Color(0xFF00FFCC),
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 2.dp)
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
                    Podium(podiumResults)
                    Spacer(modifier = Modifier.height(24.dp))
                }

                items(otherResults) { result ->
                    ResultRow(result)
                }
            }
        }
    }
}

@Composable
fun Podium(results: List<RaceResultResponse>) {
    val p1 = results.find { it.position == 1 }
    val p2 = results.find { it.position == 2 }
    val p3 = results.find { it.position == 3 }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom
    ) {
        // 5. Lowered podium by ~10% (140->125, 170->155, 120->110)
        if (p2 != null) PodiumStep(p2, 125.dp, Color(0xFFC0C0C0)) // Silver
        Spacer(modifier = Modifier.width(4.dp))
        if (p1 != null) PodiumStep(p1, 155.dp, Color(0xFFFFD700)) // Gold
        Spacer(modifier = Modifier.width(4.dp))
        if (p3 != null) PodiumStep(p3, 110.dp, Color(0xFFCD7F32)) // Bronze
    }
}

@Composable
fun RowScope.PodiumStep(result: RaceResultResponse, height: androidx.compose.ui.unit.Dp, color: Color) {
    val driverNameParts = result.driver.split(" ")
    val lastName = driverNameParts.lastOrNull()?.uppercase() ?: ""

    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = lastName,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            lineHeight = 18.sp
        )
        Text(
            text = result.team,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 10.sp,
            maxLines = 1,
            lineHeight = 12.sp
        )
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
                Text(
                    text = result.position.toString(),
                    color = color,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    lineHeight = 44.sp
                )
                Text(
                    text = "${result.points} PTS",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
fun ShimmerPodiumStep(height: androidx.compose.ui.unit.Dp) {
    val shimmerColors = listOf(
        Color.White.copy(alpha = 0.05f),
        Color.White.copy(alpha = 0.12f),
        Color.White.copy(alpha = 0.05f),
    )
    val transition = rememberInfiniteTransition(label = "")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = ""
    )
    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )
    Box(
        modifier = Modifier
            .width(100.dp)
            .height(height)
            .background(brush, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
    )
}

@Composable
fun ShimmerResultRow() {
    val shimmerColors = listOf(
        Color.White.copy(alpha = 0.05f),
        Color.White.copy(alpha = 0.12f),
        Color.White.copy(alpha = 0.05f),
    )
    val transition = rememberInfiniteTransition(label = "")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = ""
    )
    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .height(42.dp)
            .background(brush, RoundedCornerShape(8.dp))
    )
}

@Composable
fun ResultRow(result: RaceResultResponse) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.03f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = result.position.toString(),
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(45.dp),
                lineHeight = 16.sp
            )

            Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                Text(
                    text = result.driver.uppercase(),
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 17.sp
                )
                Text(
                    text = result.team,
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 12.sp
                )
            }

            Text(
                text = "${result.points} PTS",
                color = Color(0xFF00FFCC),
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.End,
                modifier = Modifier.width(80.dp),
                lineHeight = 16.sp
            )
        }
    }
}