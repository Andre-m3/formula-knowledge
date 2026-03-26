package com.formulaknowledge.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.formulaknowledge.app.data.RaceResultResponse
import com.formulaknowledge.app.data.RetrofitClient

@Composable
fun RaceResultsScreen(roundNumber: Int, gpName: String, onBack: () -> Unit) {
    var results by remember { mutableStateOf<List<RaceResultResponse>>(emptyList()) }
    var showAll by remember { mutableStateOf(false) }
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
        Spacer(modifier = Modifier.height(40.dp))
        
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

        Surface(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = Color.White.copy(alpha = 0.03f),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
        ) {
            val displayedResults = if (showAll) results else results.take(9)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                if (isLoading && results.isEmpty()) {
                    items(10) { ShimmerResultRow() }
                } else {
                    items(displayedResults) { result ->
                        ResultRow(result)
                    }

                    if (!showAll && results.size > 9) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(64.dp)
                                    .clickable { showAll = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "SHOW MORE...",
                                    color = Color(0xFF00FFCC),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
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
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .height(42.dp)
            .background(brush, RoundedCornerShape(8.dp))
    )
}

@Composable
fun ResultRow(result: RaceResultResponse) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = result.position.toString(),
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.width(45.dp)
            )
            
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(
                    text = result.driver.uppercase(),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = result.team,
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Center, modifier = Modifier.width(90.dp)) {
                Text(
                    text = "${result.points} PTS",
                    color = Color(0xFF00FFCC),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = result.time,
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 20.dp),
            thickness = 0.5.dp,
            color = Color.White.copy(alpha = 0.05f)
        )
    }
}
