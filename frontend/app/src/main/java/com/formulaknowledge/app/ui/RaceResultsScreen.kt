package com.formulaknowledge.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.formulaknowledge.app.data.RaceResultResponse
import com.formulaknowledge.app.data.RetrofitClient

@Composable
fun RaceResultsScreen(roundNumber: Int, gpName: String, onBack: () -> Unit) {
    var results by remember { mutableStateOf<List<RaceResultResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(roundNumber) {
        try {
            results = RetrofitClient.apiService.getResults(roundNumber)
            isLoading = false
        } catch (e: Exception) {
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(modifier = Modifier.height(60.dp))
        
        Text(
            text = "← BACK",
            color = Color.White.copy(alpha = 0.5f),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable { onBack() }
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = gpName.uppercase(),
            color = Color.White,
            fontSize = 42.sp,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            letterSpacing = (-2).sp,
            lineHeight = 40.sp
        )
        Text(
            text = "RACE RESULTS • ROUND $roundNumber",
            color = Color(0xFF00FFCC),
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF00FFCC))
            }
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth().weight(1f),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = Color.White.copy(alpha = 0.03f),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
            ) {
                LazyColumn(
                    modifier = Modifier.padding(16.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(results) { result ->
                        ResultRow(result)
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            thickness = 0.5.dp,
                            color = Color.White.copy(alpha = 0.05f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ResultRow(result: RaceResultResponse) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Posizione
        Text(
            text = result.position.toString(),
            color = if (result.position <= 3) Color(0xFF00FFCC) else Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.width(40.dp)
        )
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = result.driver.uppercase(),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = result.team,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = result.time,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "+${result.points} PTS",
                color = Color(0xFF00FFCC).copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}
