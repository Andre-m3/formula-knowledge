package com.formulaknowledge.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.formulaknowledge.app.data.*

enum class AppScreen { HOME, CALENDAR, PERSONAL, UPDATES_LIST, TEAM_DETAIL, WEATHER_DETAIL, RESULTS, STANDINGS }

val AppBackgroundGradientColor = Color(0xFF0B0E14)

@Composable
fun UpdatesScreen() {
    val mockRaceWeek = RaceWeekResponse(
        gp_name = "Japanese Grand Prix",
        country = "Japan",
        city = "Suzuka",
        round_number = 3,
        is_sprint = false,
        dates = listOf("03 Apr", "04 Apr", "05 Apr"),
        weather_forecast = WeatherForecast("Partly Cloudy", "22°C", "82%", "22°C", "7km/h", "Low")
    )

    // CACHE LOCALE (Lifting State): i dati persistono finché l'app è aperta
    var updates by remember { mutableStateOf<List<TeamUpdatesResponse>>(emptyList()) }
    var raceWeek by remember { mutableStateOf<RaceWeekResponse?>(mockRaceWeek) }
    var cachedDrivers by remember { mutableStateOf<List<DriverStanding>>(emptyList()) }
    var cachedConstructors by remember { mutableStateOf<List<ConstructorStanding>>(emptyList()) }
    
    var isLoading by remember { mutableStateOf(false) }
    var currentScreen by remember { mutableStateOf(AppScreen.HOME) }
    var selectedTeam by remember { mutableStateOf<TeamUpdatesResponse?>(null) }
    var selectedRound by remember { mutableIntStateOf(0) }
    var selectedGpName by remember { mutableStateOf("") }

    BackHandler(enabled = currentScreen != AppScreen.HOME) {
        when (currentScreen) {
            AppScreen.TEAM_DETAIL -> currentScreen = AppScreen.UPDATES_LIST
            AppScreen.RESULTS, AppScreen.STANDINGS, AppScreen.WEATHER_DETAIL, AppScreen.UPDATES_LIST -> currentScreen = AppScreen.HOME
            else -> currentScreen = AppScreen.HOME
        }
    }

    LaunchedEffect(Unit) {
        try {
            val rw = RetrofitClient.apiService.getCurrentRaceWeek()
            raceWeek = rw
            updates = RetrofitClient.apiService.getLatestCarUpdates()
        } catch (e: Exception) { }
    }

    Box(modifier = Modifier.fillMaxSize().background(AppBackgroundGradientColor)) {
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
                    AppScreen.HOME -> HomeScreen(raceWeek, isLoading, onNavigate = { currentScreen = it })
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
                    AppScreen.STANDINGS -> StandingsScreen(
                        drivers = cachedDrivers,
                        constructors = cachedConstructors,
                        onDataLoaded = { d, c -> cachedDrivers = d; cachedConstructors = c },
                        onBack = { currentScreen = AppScreen.HOME }
                    )
                }
            }

            if (currentScreen in listOf(AppScreen.HOME, AppScreen.CALENDAR, AppScreen.PERSONAL, AppScreen.RESULTS)) {
                Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp)) {
                    FloatingBottomBar(currentScreen = currentScreen, onNavigate = { currentScreen = it })
                }
            }
        }
    }
}

@Composable
fun StandingsScreen(
    drivers: List<DriverStanding>,
    constructors: List<ConstructorStanding>,
    onDataLoaded: (List<DriverStanding>, List<ConstructorStanding>) -> Unit,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf("Drivers") }
    var isLoading by remember { mutableStateOf(drivers.isEmpty()) }

    LaunchedEffect(Unit) {
        if (drivers.isEmpty()) {
            try {
                isLoading = true
                val d = RetrofitClient.apiService.getDriverStandings()
                val c = RetrofitClient.apiService.getConstructorStandings()
                onDataLoaded(d, c)
            } catch (e: Exception) { } finally {
                isLoading = false
            }
        }
    }
    
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(modifier = Modifier.height(80.dp))
        Text(text = "STANDINGS", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Surface(
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(25.dp),
            color = Color.White.copy(alpha = 0.05f),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                TabItem("Drivers", selectedTab == "Drivers", Modifier.weight(1f)) { selectedTab = "Drivers" }
                TabItem("Constructors", selectedTab == "Constructors", Modifier.weight(1f)) { selectedTab = "Constructors" }
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 120.dp)) {
            if (isLoading) {
                items(10) { ShimmerStandingRow() }
            } else {
                if (selectedTab == "Drivers") {
                    items(drivers) { driver -> StandingRow(driver.position, driver.driver_name, driver.points.toString(), driver.constructor_name) }
                } else {
                    items(constructors) { team -> StandingRow(team.position, team.constructor_name, team.points.toString()) }
                }
            }
        }
    }
}

@Composable
fun ShimmerStandingRow() {
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
    Box(modifier = Modifier.fillMaxWidth().height(72.dp).background(brush, RoundedCornerShape(12.dp)))
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
                AppScreen.RESULTS -> AppScreen.CALENDAR
                else -> AppScreen.HOME
            } }
            
            val itemWidth = 220.dp / 3
            val indicatorOffset by animateDpAsState(
                targetValue = (selectedIndex * itemWidth.value).dp,
                animationSpec = tween(300),
                label = "Indicator"
            )

            Box(
                modifier = Modifier.offset(x = indicatorOffset).width(itemWidth).fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.size(38.dp).background(Color(0xFF00FFCC).copy(alpha = 0.25f), CircleShape))
            }

            Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                items.forEach { (icon, label, screen) ->
                    val isSelected = when {
                        currentScreen == screen -> true
                        screen == AppScreen.CALENDAR && currentScreen == AppScreen.RESULTS -> true
                        screen == AppScreen.HOME && currentScreen !in listOf(AppScreen.CALENDAR, AppScreen.PERSONAL, AppScreen.RESULTS) -> true
                        else -> false
                    }
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
fun HomeScreen(raceWeek: RaceWeekResponse?, isLoading: Boolean, onNavigate: (AppScreen) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(modifier = Modifier.height(40.dp))

        Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
            Column(modifier = Modifier.align(Alignment.BottomStart)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = Color(0xFF00FFCC), shape = RoundedCornerShape(4.dp)) {
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

                val displayGpName = raceWeek?.gp_name?.uppercase()?.replace("GRAND PRIX", "GP") ?: "JAPANESE GP"

                Text(
                    text = displayGpName,
                    color = Color.White,
                    fontSize = 58.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    letterSpacing = (-4).sp,
                    lineHeight = 64.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )

                val dates = raceWeek?.dates
                val dateRangeStr = if (!dates.isNullOrEmpty() && dates.size >= 3) {
                    val startDay = dates[0].substringBefore(" ").trimStart('0')
                    val endDay = dates[2].substringBefore(" ").trimStart('0')
                    val month = dates[2].substringAfter(" ").uppercase()
                    "$startDay-$endDay $month"
                } else {
                    "3-5 APRIL"
                }
                val locationStr = raceWeek?.city?.uppercase() ?: "SUZUKA"

                Text(
                    text = "$dateRangeStr \u2022 $locationStr",
                    color = Color(0xFF00FFCC),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.offset(y = (-4).dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            FullWidthGlassCard(title = "RACE SESSIONS", content = "FP1 starts in 3 days", accentColor = Color(0xFF00FFCC), isHighlighted = true, onClick = { })

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

            FullWidthGlassCard(title = "TECHNICAL UPDATES", content = "Check latest upgrades...", accentColor = Color(0xFF00FFCC), onClick = { onNavigate(AppScreen.UPDATES_LIST) })

            Row(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier.weight(0.55f).height(72.dp).clickable { onNavigate(AppScreen.STANDINGS) },
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.05f),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Row(modifier = Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Leaderboard, null, tint = Color(0xFF00FFCC), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("STANDINGS", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
                    }
                }
                Spacer(modifier = Modifier.weight(0.45f))
            }
        }
    }
}

@Composable
fun FullWidthGlassCard(title: String, content: String, accentColor: Color, isHighlighted: Boolean = false, onClick: () -> Unit) {
    val cardBackground = if (isHighlighted) Color(0xFF00FFCC).copy(alpha = 0.05f) else Color.White.copy(alpha = 0.05f)
    val cardBorder = if (isHighlighted) Color(0xFF00FFCC).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.1f)
    val titleColor = if (isHighlighted) Color(0xFF00FFCC) else accentColor

    Surface(
        modifier = Modifier.fillMaxWidth().height(92.dp).clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = cardBackground,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, cardBorder)
    ) {
        Column(modifier = Modifier.padding(horizontal = 22.dp), verticalArrangement = Arrangement.Center) {
            Text(text = title, color = titleColor, fontSize = 14.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
            Text(text = content, color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun UpdatesListScreen(updates: List<TeamUpdatesResponse>, isLoading: Boolean, onTeamClick: (TeamUpdatesResponse) -> Unit, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(text = "\u2190 BACK", color = Color.White.copy(alpha = 0.5f), modifier = Modifier.padding(top = 48.dp, start = 20.dp, bottom = 12.dp).clickable { onBack() }, fontWeight = FontWeight.Bold)
        Text(text = "UPDATES", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(start = 20.dp, bottom = 24.dp))
        if (isLoading && updates.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Color(0xFF00FFCC)) }
        } else {
            LazyColumn {
                items(updates) { update ->
                    TeamUpdateCard(update.team_name, update.team_color_hex, onClick = { onTeamClick(update) })
                }
            }
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
        LazyColumn {
            items(teamUpdate.updates) { update ->
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
}

@Composable
fun TeamUpdateCard(name: String, colorHex: String, onClick: () -> Unit) {
    val teamColor = try { Color(android.graphics.Color.parseColor(colorHex)) } catch (e: Exception) { Color.Gray }
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp).clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.05f),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, teamColor.copy(alpha = 0.4f))
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(4.dp, 24.dp).background(teamColor, RoundedCornerShape(2.dp)))
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PersonalScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "PERSONAL PROFILE", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TabItem(label: String, isSelected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier.fillMaxHeight().background(if (isSelected) Color(0xFF00FFCC).copy(alpha = 0.1f) else Color.Transparent).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = label.uppercase(), color = if (isSelected) Color(0xFF00FFCC) else Color.White.copy(alpha = 0.4f), fontSize = 13.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun StandingRow(pos: Int, name: String, points: String, subtitle: String? = null) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(72.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.03f)
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = pos.toString(), color = Color(0xFF00FFCC), fontSize = 20.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(40.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                if (subtitle != null) {
                    Text(text = subtitle.uppercase(), color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            }
            Text(text = points, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}
