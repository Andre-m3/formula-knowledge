package com.formulaknowledge.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.formulaknowledge.app.data.RaceWeekResponse
import com.formulaknowledge.app.data.WeatherForecast
import com.formulaknowledge.app.data.RetrofitClient
import com.formulaknowledge.app.data.TeamUpdatesResponse

enum class AppScreen { HOME, CALENDAR, PERSONAL, UPDATES_LIST, TEAM_DETAIL, WEATHER_DETAIL, RESULTS, STANDINGS }

@Composable
fun UpdatesScreen() {
    var updates by remember { mutableStateOf<List<TeamUpdatesResponse>>(emptyList()) }
    var raceWeek by remember { mutableStateOf<RaceWeekResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var currentScreen by remember { mutableStateOf(AppScreen.HOME) }
    var selectedTeam by remember { mutableStateOf<TeamUpdatesResponse?>(null) }
    
    var selectedRound by remember { mutableIntStateOf(0) }
    var selectedGpName by remember { mutableStateOf("") }

    BackHandler(enabled = currentScreen != AppScreen.HOME) {
        when (currentScreen) {
            AppScreen.TEAM_DETAIL -> currentScreen = AppScreen.UPDATES_LIST
            AppScreen.RESULTS, AppScreen.STANDINGS -> currentScreen = AppScreen.HOME
            else -> currentScreen = AppScreen.HOME
        }
    }

    LaunchedEffect(Unit) {
        try {
            val rw = RetrofitClient.apiService.getCurrentRaceWeek()
            raceWeek = rw
            updates = RetrofitClient.apiService.getLatestCarUpdates()
        } catch (e: Exception) {
            // Fallback robusto in caso di errore server
            raceWeek = RaceWeekResponse(
                gp_name = "Japanese Grand Prix",
                country = "Japan",
                round_number = 3,
                is_sprint = false,
                dates = listOf("03 Apr", "04 Apr", "05 Apr"),
                weather_forecast = WeatherForecast(
                    status = "Partly Cloudy",
                    temp = "22°C",
                    humidity = "82%",
                    feels_like = "22°C",
                    wind = "7km/h",
                    uv = "Low"
                )
            )
        } finally {
            isLoading = false
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(AppBackgroundGradient)) {
        Canvas(modifier = Modifier.fillMaxSize().blur(220.dp).alpha(0.15f)) {
            drawCircle(color = Color(0xFFE32219), radius = size.width / 2f, center = center.copy(y = size.height * 0.2f, x = size.width * 0.8f))
            drawCircle(color = Color(0xFF00D2BE), radius = size.width / 2.5f, center = center.copy(y = size.height * 0.7f, x = size.width * 0.2f))
        }

        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) },
                label = "ScreenTransition"
            ) { targetScreen ->
                when (targetScreen) {
                    AppScreen.HOME -> HomeScreen(raceWeek, onNavigate = { currentScreen = it })
                    AppScreen.CALENDAR -> CalendarScreen(
                        onNavigateToHome = { currentScreen = AppScreen.HOME },
                        onNavigateToResults = { round, name ->
                            selectedRound = round
                            selectedGpName = name
                            currentScreen = AppScreen.RESULTS
                        }
                    )
                    AppScreen.PERSONAL -> PersonalScreen()
                    AppScreen.UPDATES_LIST -> UpdatesListScreen(updates, isLoading, onTeamClick = { selectedTeam = it; currentScreen = AppScreen.TEAM_DETAIL }, onBack = { currentScreen = AppScreen.HOME })
                    AppScreen.TEAM_DETAIL -> TeamUpdateDetailScreen(selectedTeam!!, onBack = { currentScreen = AppScreen.UPDATES_LIST })
                    AppScreen.WEATHER_DETAIL -> WeatherDetailScreen(raceWeek, onBack = { currentScreen = AppScreen.HOME })
                    AppScreen.RESULTS -> RaceResultsScreen(selectedRound, selectedGpName, onBack = { currentScreen = AppScreen.CALENDAR })
                    AppScreen.STANDINGS -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text("STANDINGS (Coming Soon)", color = Color.White) }
                }
            }

            if (currentScreen in listOf(AppScreen.HOME, AppScreen.CALENDAR, AppScreen.PERSONAL)) {
                Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp)) {
                    FloatingBottomBar(currentScreen = currentScreen, onNavigate = { currentScreen = it })
                }
            }
        }
    }
}

@Composable
fun FloatingBottomBar(currentScreen: AppScreen, onNavigate: (AppScreen) -> Unit) {
    val items = listOf(
        Triple(Icons.Default.DateRange, "Calendar", AppScreen.CALENDAR),
        Triple(Icons.Default.Home, "Home", AppScreen.HOME),
        Triple(Icons.Default.Person, "Personal", AppScreen.PERSONAL)
    )

    val barWidth = 220.dp
    val barHeight = 52.dp

    Surface(
        modifier = Modifier.height(barHeight).width(barWidth),
        shape = RoundedCornerShape(26.dp),
        color = Color.White.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val selectedIndex = items.indexOfFirst { it.third == when(currentScreen) {
                AppScreen.CALENDAR -> AppScreen.CALENDAR
                AppScreen.PERSONAL -> AppScreen.PERSONAL
                else -> AppScreen.HOME
            } }
            
            val itemWidth = 220.dp / 3
            val indicatorOffset by animateDpAsState(
                targetValue = (selectedIndex * itemWidth.value).dp,
                animationSpec = tween(300),
                label = "Indicator"
            )

            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(itemWidth)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.size(38.dp).background(Color(0xFF00FFCC).copy(alpha = 0.25f), CircleShape))
            }

            Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                items.forEach { (icon, label, screen) ->
                    val isSelected = currentScreen == screen || (screen == AppScreen.HOME && currentScreen !in listOf(AppScreen.CALENDAR, AppScreen.PERSONAL))
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        IconButton(onClick = { onNavigate(screen) }) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (isSelected) Color(0xFF00FFCC) else Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(raceWeek: RaceWeekResponse?, onNavigate: (AppScreen) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(modifier = Modifier.height(35.dp))
        
        Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
            Column(modifier = Modifier.align(Alignment.BottomStart)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color(0xFF00FFCC),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "R${raceWeek?.round_number ?: "3"}",
                            color = Color.Black,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = "NEXT EVENT", color = Color.White.copy(alpha = 0.5f), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = (raceWeek?.gp_name?.split(" ")?.firstOrNull() ?: "JAPAN").uppercase(),
                    color = Color.White,
                    fontSize = 78.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    letterSpacing = (-4).sp,
                    lineHeight = 70.sp
                )
                Text(
                    text = "3-5 APRIL \u2022 Suzuka International Circuit",
                    color = Color(0xFF00FFCC),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.offset(y = (-4).dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            FullWidthGlassCard(
                title = "RACE SESSIONS",
                content = "FP1 starts in 3 days",
                accentColor = Color(0xFF00FFCC),
                onClick = { }
            )
            
            val weatherStatus = raceWeek?.weather_forecast?.status ?: "Partly Cloudy"
            val weatherIcon = when {
                weatherStatus.contains("Sunny") -> "\u2600\ufe0f"
                weatherStatus.contains("Cloudy") -> "\u26c5"
                weatherStatus.contains("Rain") -> "\ud83c\udf27\ufe0f"
                else -> "\u2601\ufe0f"
            }

            FullWidthGlassCard(
                title = "WEATHER FORECAST",
                content = "$weatherIcon $weatherStatus \u2022 ${raceWeek?.weather_forecast?.temp ?: "22\u00b0C"}",
                accentColor = Color(0xFF00FFCC),
                onClick = { onNavigate(AppScreen.WEATHER_DETAIL) }
            )
            
            FullWidthGlassCard(
                title = "TECHNICAL UPDATES",
                content = "Check latest upgrades...",
                accentColor = Color(0xFF00FFCC),
                onClick = { onNavigate(AppScreen.UPDATES_LIST) }
            )

            // PULSANTE STANDINGS A METÀ LARGHEZZA
            Row(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier
                        .weight(0.5f)
                        .height(70.dp)
                        .clickable { onNavigate(AppScreen.STANDINGS) },
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White.copy(alpha = 0.05f),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Leaderboard, null, tint = Color(0xFF00FFCC), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("STANDINGS", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    }
                }
                Spacer(modifier = Modifier.weight(0.5f)) // Per tenerlo a sinistra
            }
        }
    }
}

@Composable
fun FullWidthGlassCard(title: String, content: String, accentColor: Color, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(92.dp).clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.05f),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 22.dp), verticalArrangement = Arrangement.Center) {
            Text(
                text = title, 
                color = accentColor, 
                fontSize = 14.sp,
                fontWeight = FontWeight.Black, 
                letterSpacing = 1.5.sp
            )
            Text(text = content, color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun UpdatesListScreen(updates: List<TeamUpdatesResponse>, isLoading: Boolean, onTeamClick: (TeamUpdatesResponse) -> Unit, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(text = "\u2190 BACK", color = Color.White.copy(alpha = 0.5f), modifier = Modifier.padding(top = 48.dp, start = 20.dp, bottom = 12.dp).clickable { onBack() }, fontWeight = FontWeight.Bold)
        Text(text = "UPDATES", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(start = 20.dp, bottom = 24.dp))
        if (isLoading) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Color(0xFF00FFCC)) }
        } else {
            updates.forEach { TeamUpdateCard(it.team_name, it.team_color_hex, onClick = { onTeamClick(it) }) }
        }
    }
}

@Composable
fun TeamUpdateDetailScreen(teamUpdate: TeamUpdatesResponse, onBack: () -> Unit) {
    val teamColor = try { Color(android.graphics.Color.parseColor(teamUpdate.team_color_hex)) } catch (e: Exception) { Color.Gray }
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text(text = "\u2190 BACK", color = teamColor, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 28.dp).clickable { onBack() })
        Text(text = teamUpdate.team_name.uppercase(), color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic)
        Spacer(modifier = Modifier.height(24.dp))
        teamUpdate.updates.forEach { update ->
            Surface(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.05f),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, teamColor.copy(alpha = 0.3f))
            ) {
                Text(text = update, color = Color.White.copy(alpha = 0.8f), modifier = Modifier.padding(20.dp), lineHeight = 24.sp)
            }
        }
    }
}

@Composable
fun PersonalScreen() {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Text("PERSONAL AREA", color = Color.White.copy(alpha = 0.3f), fontWeight = FontWeight.Black)
    }
}
