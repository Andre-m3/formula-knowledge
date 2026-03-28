package com.formulaknowledge.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.formulaknowledge.app.data.DailyForecast
import com.formulaknowledge.app.data.RaceWeekResponse

@Composable
fun WeatherDetailScreen(raceWeek: RaceWeekResponse?) {
    val weather = raceWeek?.weather_forecast

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(modifier = Modifier.height(32.dp)) 

        Text(
            text = "WEATHER",
            color = Color.White,
            fontSize = 54.sp,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            letterSpacing = (-2).sp,
            lineHeight = 50.sp
        )
        Text(
            text = "${raceWeek?.gp_name?.uppercase() ?: "CIRCUIT"} FORECAST",
            color = Color(0xFF00FFCC),
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        MainWeatherCard(weather)

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "5-DAY FORECAST", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = Color.White.copy(alpha = 0.03f),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                weather?.daily?.forEachIndexed { index, dailyForecast ->
                    DailyForecastRow(dailyForecast)
                    if (index < weather.daily.size - 1) {
                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun MainWeatherCard(weather: com.formulaknowledge.app.data.WeatherForecast?) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = Color(0xFF1E0A0A).copy(alpha = 0.6f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00FFCC).copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getWeatherIcon(weather?.status),
                contentDescription = weather?.status,
                tint = getWeatherColor(weather?.status),
                modifier = Modifier.size(70.dp)
            )
            Spacer(modifier = Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = weather?.temp?.replace("°C", "") ?: "--", 
                        color = Color.White, 
                        fontSize = 52.sp, 
                        fontWeight = FontWeight.Black,
                        lineHeight = 52.sp
                    )
                    Text(
                        text = "°C", 
                        color = Color(0xFF00FFCC), 
                        fontSize = 20.sp, 
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(bottom = 12.dp, start = 2.dp)
                    )
                }
                Text(text = "Feels like ${weather?.feels_like ?: "--"}", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                // 1. REORDERED: Wind -> Rain -> Humidity
                WeatherInfoChip(Icons.Default.Air, weather?.wind ?: "--", Color.White.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.height(6.dp))
                WeatherInfoChip(Icons.Default.Water, weather?.rain_probability ?: "--", Color(0xFFA9A9A9)) // Rain icon same as cloud color
                Spacer(modifier = Modifier.height(6.dp))
                WeatherInfoChip(Icons.Default.WaterDrop, weather?.humidity ?: "--", Color(0xFF00D2BE))
            }
        }
    }
}

@Composable
fun WeatherInfoChip(icon: ImageVector, text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = text, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DailyForecastRow(forecast: DailyForecast) {
    Row(
        modifier = Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = forecast.day.uppercase().take(3), color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp, modifier = Modifier.width(45.dp))
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Icon(
            imageVector = getWeatherIcon(forecast.status), 
            contentDescription = null, 
            tint = getWeatherColor(forecast.status), 
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        // Vertical Divider
        Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.White.copy(alpha = 0.1f)))
        
        Spacer(modifier = Modifier.width(16.dp))

        // TEMP MAX
        Text(
            text = forecast.temp_max, 
            color = Color.White, 
            fontWeight = FontWeight.ExtraBold, 
            fontSize = 17.sp,
            modifier = Modifier.width(40.dp),
            textAlign = TextAlign.End
        )

        Spacer(modifier = Modifier.width(16.dp))

        // RAIN PROB - Moved closer to Temp and re-styled
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.width(55.dp), horizontalArrangement = Arrangement.End) {
            Icon(Icons.Default.Water, null, tint = Color(0xFFA9A9A9), modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = forecast.rain_probability, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp, fontWeight = FontWeight.Black)
        }
    }
}

fun getWeatherIcon(status: String?): ImageVector {
    return when {
        status == null -> Icons.Default.DeviceThermostat
        status.contains("Clear") || status.contains("Sunny") -> Icons.Default.WbSunny
        status.contains("Partly Cloudy") -> Icons.Default.CloudQueue
        status.contains("Cloudy") || status.contains("Overcast") -> Icons.Default.Cloud
        status.contains("Fog") -> Icons.Default.BlurOn
        status.contains("Drizzle") || status.contains("Rain") -> Icons.Default.Water
        status.contains("Snow") -> Icons.Default.AcUnit
        status.contains("Thunderstorm") -> Icons.Default.Thunderstorm
        else -> Icons.Default.Cloud
    }
}

fun getWeatherColor(status: String?): Color {
    return when {
        status == null -> Color.White
        status.contains("Clear") || status.contains("Sunny") -> Color(0xFFFFD700) // Gold
        status.contains("Partly Cloudy") -> Color(0xFF00FFCC) // Neon
        status.contains("Cloudy") || status.contains("Overcast") -> Color(0xFFA9A9A9) // Gray
        status.contains("Rain") || status.contains("Drizzle") -> Color(0xFFA9A9A9) // Gray as requested
        status.contains("Thunderstorm") -> Color(0xFFE32219) // F1 Red for danger
        else -> Color.White
    }
}
