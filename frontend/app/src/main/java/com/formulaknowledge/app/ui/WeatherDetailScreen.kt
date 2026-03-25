package com.formulaknowledge.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
fun WeatherDetailScreen(raceWeek: RaceWeekResponse?, onBack: () -> Unit) {
    val weather = raceWeek?.weather_forecast
    
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
            text = "WEATHER",
            color = Color.White,
            fontSize = 42.sp,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            letterSpacing = (-2).sp
        )
        Text(
            text = "${raceWeek?.gp_name?.uppercase() ?: "CIRCUIT"} FORECAST",
            color = Color(0xFF00FFCC),
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        // MAIN TEMPERATURE CARD
        Surface(
            modifier = Modifier.fillMaxWidth().height(180.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color.White.copy(alpha = 0.05f),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = weather?.temp ?: "--°C",
                    color = Color.White,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = weather?.status ?: "Loading...",
                    color = Color(0xFF00FFCC),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // GRID OF DETAILS
        Row(modifier = Modifier.fillMaxWidth()) {
            WeatherInfoSmallCard("FEELS LIKE", weather?.feels_like ?: "--", Modifier.weight(1f))
            Spacer(modifier = Modifier.width(12.dp))
            WeatherInfoSmallCard("HUMIDITY", weather?.humidity ?: "--", Modifier.weight(1f))
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(modifier = Modifier.fillMaxWidth()) {
            WeatherInfoSmallCard("WIND SPEED", weather?.wind ?: "--", Modifier.weight(1f))
            Spacer(modifier = Modifier.width(12.dp))
            WeatherInfoSmallCard("UV INDEX", weather?.uv ?: "--", Modifier.weight(1f))
        }
    }
}

@Composable
fun WeatherInfoSmallCard(label: String, value: String, modifier: Modifier) {
    Surface(
        modifier = modifier.height(90.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.03f),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = label, color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            Text(text = value, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}
