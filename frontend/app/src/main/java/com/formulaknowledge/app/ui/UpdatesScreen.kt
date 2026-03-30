package com.formulaknowledge.app.ui

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.formulaknowledge.app.data.*
import kotlinx.coroutines.launch

enum class AppScreen { HOME, CALENDAR, PERSONAL, UPDATES_LIST, TEAM_DETAIL, WEATHER_DETAIL, RESULTS, STANDINGS, DRIVER_DETAIL, RACE_SESSIONS, CIRCUIT_DETAIL }

val AppBackgroundGradientColor = Color(0xFF0B0E14)

@Composable
fun UpdatesScreen() {
    val mockRaceWeek = RaceWeekResponse(
        gp_name = "Japanese",
        country = "Japan",
        city = "Suzuka",
        circuit_name = "Suzuka International Racing Course",
        round_number = 3,
        is_sprint = false,
        dates = listOf("03 Apr", "04 Apr", "05 Apr"),
        weather_forecast = WeatherForecast("Partly Cloudy", "22°C", "82%", "22°C", "7km/h", "Low", "10%", emptyList())
    )

    var updatesWrapper by remember { mutableStateOf<TeamUpdatesWrapper?>(null) }
    var raceWeek by remember { mutableStateOf<RaceWeekResponse?>(mockRaceWeek) }
    var cachedDrivers by remember { mutableStateOf<List<DriverStanding>>(emptyList()) }
    var cachedConstructors by remember { mutableStateOf<List<ConstructorStanding>>(emptyList()) }

    var isLoadingUpdates by remember { mutableStateOf(false) }
    var currentScreen by remember { mutableStateOf(AppScreen.HOME) }
    var selectedTeam by remember { mutableStateOf<TeamUpdatesResponse?>(null) }
    var selectedRound by remember { mutableIntStateOf(0) }
    var selectedGpName by remember { mutableStateOf("") }
    var selectedDriverName by remember { mutableStateOf("") }
    var selectedCircuitRound by remember { mutableIntStateOf(0) }
    
    var showNotReadyDialog by remember { mutableStateOf(false) }

    var isBottomBarVisible by remember { mutableStateOf(true) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -15) { isBottomBarVisible = false } 
                else if (available.y > 15) { isBottomBarVisible = true }
                return Offset.Zero
            }
        }
    }
    
    LaunchedEffect(currentScreen) {
        isBottomBarVisible = true
    }

    val bottomBarOffset by animateDpAsState(
        targetValue = if (isBottomBarVisible) 0.dp else 120.dp,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "BottomBarOffset"
    )

    BackHandler(enabled = currentScreen != AppScreen.HOME) {
        when (currentScreen) {
            AppScreen.TEAM_DETAIL -> currentScreen = AppScreen.UPDATES_LIST
            AppScreen.DRIVER_DETAIL -> currentScreen = (if (selectedRound > 0) AppScreen.RESULTS else AppScreen.STANDINGS)
            AppScreen.CIRCUIT_DETAIL -> currentScreen = AppScreen.CALENDAR
            AppScreen.RESULTS, AppScreen.STANDINGS, AppScreen.WEATHER_DETAIL, AppScreen.UPDATES_LIST, AppScreen.RACE_SESSIONS -> currentScreen = AppScreen.HOME
            else -> currentScreen = AppScreen.HOME
        }
    }

    LaunchedEffect(Unit) {
        try {
            Log.d("API_CALL", "Requesting Current Race Week...")
            val rw = RetrofitClient.apiService.getCurrentRaceWeek()
            raceWeek = rw
            Log.d("API_CALL", "Success: ${rw.gp_name}")
            
            isLoadingUpdates = true
            Log.d("API_CALL", "Requesting Car Updates...")
            val wrapper = RetrofitClient.apiService.getLatestCarUpdates()
            updatesWrapper = wrapper
            isLoadingUpdates = false
            Log.d("API_CALL", "Success: ${wrapper.gp} updates loaded")
        } catch (e: Exception) { 
            Log.e("API_ERROR", "Error during initial data fetch: ${e.message}", e)
            isLoadingUpdates = false
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(AppBackgroundGradientColor)
        .nestedScroll(nestedScrollConnection)
    ) {
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
                    AppScreen.HOME -> HomeScreen(raceWeek, false, onNavigate = { 
                        if (it == AppScreen.UPDATES_LIST && updatesWrapper?.status == "not_ready") {
                            showNotReadyDialog = true
                        } else {
                            currentScreen = it 
                        }
                    })
                    AppScreen.CALENDAR -> CalendarScreen(
                        onNavigateToHome = { currentScreen = AppScreen.HOME },
                        onNavigateToResults = { round, name ->
                            selectedRound = round
                            selectedGpName = name
                            currentScreen = AppScreen.RESULTS
                        },
                        onNavigateToCircuit = { round ->
                            selectedCircuitRound = round
                            currentScreen = AppScreen.CIRCUIT_DETAIL
                        }
                    )
                    AppScreen.PERSONAL -> PersonalScreen()
                    AppScreen.UPDATES_LIST -> UpdatesListScreen(updatesWrapper?.data ?: emptyList(), isLoadingUpdates, onTeamClick = { selectedTeam = it; currentScreen = AppScreen.TEAM_DETAIL })
                    AppScreen.TEAM_DETAIL -> TeamUpdateDetailScreen(selectedTeam!!)
                    AppScreen.WEATHER_DETAIL -> WeatherDetailScreen(raceWeek)
                    AppScreen.RESULTS -> RaceResultsScreen(selectedRound, selectedGpName, onDriverClick = { name ->
                        selectedDriverName = name
                        currentScreen = AppScreen.DRIVER_DETAIL
                    })
                    AppScreen.STANDINGS -> StandingsScreen(
                        drivers = cachedDrivers,
                        constructors = cachedConstructors,
                        onDataLoaded = { d, c -> cachedDrivers = d; cachedConstructors = c },
                        onDriverClick = { name ->
                            selectedDriverName = name
                            currentScreen = AppScreen.DRIVER_DETAIL
                        }
                    )
                    AppScreen.DRIVER_DETAIL -> DriverDetailScreen(selectedDriverName)
                    AppScreen.RACE_SESSIONS -> RaceSessionsScreen(raceWeek)
                    AppScreen.CIRCUIT_DETAIL -> CircuitDetailScreen(selectedCircuitRound, onNavigateToResults = { round, name ->
                        selectedRound = round
                        selectedGpName = name
                        currentScreen = AppScreen.RESULTS
                    })
                }
            }

            if (currentScreen in listOf(AppScreen.HOME, AppScreen.CALENDAR, AppScreen.PERSONAL, AppScreen.RESULTS, AppScreen.STANDINGS, AppScreen.WEATHER_DETAIL, AppScreen.DRIVER_DETAIL, AppScreen.RACE_SESSIONS, AppScreen.CIRCUIT_DETAIL)) {
                Box(
                    modifier = Modifier
                        .offset(y = bottomBarOffset)
                        .align(Alignment.BottomCenter)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, AppBackgroundGradientColor.copy(alpha = 0.85f))
                                )
                            )
                    )

                    Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp)) {
                        FloatingBottomBar(currentScreen = currentScreen, onNavigate = { 
                            if (it == AppScreen.UPDATES_LIST && updatesWrapper?.status == "not_ready") {
                                showNotReadyDialog = true
                            } else {
                                currentScreen = it 
                            }
                        })
                    }
                }
            }
        }

        if (showNotReadyDialog) {
            AlertDialog(
                onDismissRequest = { showNotReadyDialog = false },
                shape = RoundedCornerShape(28.dp),
                containerColor = Color(0xFF1E0A0A).copy(alpha = 0.95f),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, null, tint = Color(0xFF00FFCC), modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("NON ANCORA PRONTO", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    }
                },
                text = {
                    Text(
                        "La FIA non ha ancora pubblicato il documento degli aggiornamenti tecnici per il ${updatesWrapper?.gp ?: "GP"}. Torna Venerdì mattina!",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showNotReadyDialog = false }) {
                        Text("OK", color = Color(0xFF00FFCC), fontWeight = FontWeight.Black)
                    }
                }
            )
        }
    }
}

@Composable
fun CircuitDetailScreen(round: Int, onNavigateToResults: (Int, String) -> Unit) {
    var circuitData by remember { mutableStateOf<CircuitDetailResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(round) {
        try {
            isLoading = true
            circuitData = RetrofitClient.apiService.getCircuitDetails(round)
        } catch (e: Exception) { } finally {
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(modifier = Modifier.height(26.dp)) 
        
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF00FFCC))
            }
        } else {
            circuitData?.let { data ->
                Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.BottomStart) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = Color(0xFF00FFCC), shape = RoundedCornerShape(4.dp)) {
                                Text(text = "R${data.round}", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = "GRAND PRIX", color = Color.White.copy(alpha = 0.5f), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        val rawGpName = data.gp_name.uppercase().replace("GRAND PRIX", "").trim()
                        Text(text = "$rawGpName GP", color = Color.White, fontSize = 58.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, letterSpacing = (-5).sp, lineHeight = 64.sp)
                        
                        val dates = data.dates
                        val dateRangeStr = if (dates.size >= 3) {
                            val startDay = dates[0].substringBefore(" ").trimStart('0')
                            val endDay = dates[2].substringBefore(" ").trimStart('0')
                            val month = dates[2].substringAfter(" ").uppercase()
                            "$startDay-$endDay $month"
                        } else { "" }
                        
                        Text(text = "$dateRangeStr \u2022 ${data.location.uppercase()}", color = Color(0xFF00FFCC), fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.offset(y = (-4).dp))
                    }
                }

                Spacer(modifier = Modifier.height(26.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(bottom = 120.dp)) {
                    item { CircuitTechnicalCard("CIRCUIT NAME", data.circuit_name.uppercase()) }
                    item { CircuitTechnicalCard("TRACK LENGTH", data.length) }
                    item { CircuitTechnicalCard("RACE DISTANCE", "${data.laps} LAPS") }
                    item { CircuitTechnicalCard("LAP RECORD", data.record) }
                    
                    if (data.status == "past") {
                        item {
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { onNavigateToResults(data.round, data.gp_name) },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0033).copy(alpha = 0.9f))
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Leaderboard, null, tint = Color.White)
                                    Spacer(Modifier.width(12.dp))
                                    Text("VIEW RACE RESULTS", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CircuitTechnicalCard(label: String, value: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.05f),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(text = label, color = Color(0xFF00FFCC), fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
            Text(text = value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun DriverDetailScreen(driverName: String) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(modifier = Modifier.height(46.dp))
        Text(text = driverName.uppercase(), color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic)
        Text(text = "DRIVER STATISTICS", color = Color(0xFF00FFCC), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "Pagina statistiche in arrivo...", color = Color.White.copy(alpha = 0.3f), fontSize = 16.sp)
        }
    }
}

@Composable
fun StandingsScreen(
    drivers: List<DriverStanding>,
    constructors: List<ConstructorStanding>,
    onDataLoaded: (List<DriverStanding>, List<ConstructorStanding>) -> Unit,
    onDriverClick: (String) -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf("Drivers") }
    var isLoading by remember { mutableStateOf(drivers.isEmpty()) }
    
    var swipeOffset by remember { mutableFloatStateOf(0f) }
    val dragModifier = Modifier.draggable(
        orientation = Orientation.Horizontal,
        state = rememberDraggableState { delta ->
            swipeOffset += delta
        },
        onDragStopped = {
            if (swipeOffset < -150 && selectedTab == "Drivers") {
                selectedTab = "Constructors"
            } else if (swipeOffset > 150 && selectedTab == "Constructors") {
                selectedTab = "Drivers"
            }
            swipeOffset = 0f
        }
    )

    LaunchedEffect(Unit) {
        if (drivers.isEmpty()) {
            try {
                isLoading = true
                Log.d("API_CALL", "Requesting Standings...")
                val d = RetrofitClient.apiService.getDriverStandings()
                val c = RetrofitClient.apiService.getConstructorStandings()
                onDataLoaded(d, c)
                Log.d("API_CALL", "Standings loaded successfully")
            } catch (e: Exception) { 
                Log.e("API_ERROR", "Error loading standings: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp).then(dragModifier)) {
        Spacer(modifier = Modifier.height(46.dp)) 
        Text(text = "CLASSIFICHE", color = Color.White, fontSize = 54.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, letterSpacing = (-3).sp, lineHeight = 50.sp)
        Text(text = "2026", color = Color(0xFF00FFCC), fontSize = 38.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, letterSpacing = (-2).sp, modifier = Modifier.offset(y = (-10).dp))

        Spacer(modifier = Modifier.height(14.dp))

        Box(modifier = Modifier.fillMaxWidth().height(44.dp), contentAlignment = Alignment.CenterStart) {
            Row(modifier = Modifier.wrapContentWidth().background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(22.dp)).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TabItemMinimalAnimated("Drivers", selectedTab == "Drivers") { selectedTab = "Drivers" }
                TabItemMinimalAnimated("Constructors", selectedTab == "Constructors") { selectedTab = "Constructors" }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                if (targetState == "Constructors") {
                    (slideInHorizontally { width -> width } + fadeIn()).togetherWith(slideOutHorizontally { width -> -width } + fadeOut())
                } else {
                    (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(slideOutHorizontally { width -> width } + fadeOut())
                }.using(SizeTransform(clip = false))
            },
            label = "StandingsListTransition"
        ) { targetTab ->
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 120.dp), modifier = Modifier.graphicsLayer {
                val progress = Math.min(1f, Math.abs(swipeOffset) / 500f)
                alpha = 1f - (progress * 0.5f)
                scaleX = 1f - (progress * 0.05f)
            }) {
                if (isLoading) {
                    items(10) { ShimmerStandingRow() }
                } else {
                    if (targetTab == "Drivers") {
                        val leader = drivers.firstOrNull()
                        val secondPlacePoints = drivers.getOrNull(1)?.points ?: 0
                        if (leader != null) {
                            item {
                                LeaderCard(name = leader.driver_name, subtitle = leader.constructor_name, points = leader.points.toString(), gap = if (leader.points - secondPlacePoints > 0) "+${leader.points - secondPlacePoints} PTS" else "LEADER", icon = "1", onClick = { onDriverClick(leader.driver_name) })
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                        items(drivers.drop(1)) { driver -> StandingRow(driver.position, driver.driver_name, driver.points.toString(), driver.constructor_name, height = 61.dp, onClick = { onDriverClick(driver.driver_name) }) }
                    } else {
                        val leader = constructors.firstOrNull()
                        val secondPlacePoints = constructors.getOrNull(1)?.points ?: 0
                        if (leader != null) {
                            item {
                                LeaderCard(name = leader.constructor_name, subtitle = "LEADER • ${leader.chassis_name ?: "CHASSIS"}", points = leader.points.toString(), gap = if (leader.points - secondPlacePoints > 0) "+${leader.points - secondPlacePoints} PTS" else "LEADER", icon = "\uD83C\uDFCE\uFE0F")
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                        items(constructors.drop(1)) { team -> StandingRow(team.position, team.constructor_name, team.points.toString(), subtitle = team.chassis_name, height = 61.dp) }
                    }
                }
            }
        }
    }
}

@Composable
fun LeaderCard(name: String, subtitle: String, points: String, gap: String, icon: String, onClick: () -> Unit = {}) {
    Surface(modifier = Modifier.fillMaxWidth().height(140.dp).clickable { onClick() }, shape = RoundedCornerShape(24.dp), color = Color(0xFF1E0A0A).copy(alpha = 0.4f), border = BorderStroke(1.5.dp, Color(0xFF00FFCC).copy(alpha = 0.6f))) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize().alpha(0.1f)) {
                drawCircle(color = Color(0xFF00FFCC), radius = size.width / 3f, center = Offset(size.width, 0f))
            }
            Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Surface(color = Color(0xFF00FFCC).copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                        Text(text = "CHAMPIONSHIP LEADER", color = Color(0xFF00FFCC), fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = name.uppercase(), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, lineHeight = 30.sp)
                    Text(text = subtitle.uppercase(), color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = points, color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, lineHeight = 44.sp)
                    Text(text = gap, color = Color(0xFF00FFCC), fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
            }
            Text(text = icon, color = Color.White.copy(alpha = 0.05f), fontSize = 120.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.BottomEnd).offset(x = 20.dp, y = 40.dp))
        }
    }
}

@Composable
fun TabItemMinimalAnimated(label: String, isSelected: Boolean, onClick: () -> Unit) {
    val backgroundAlpha by animateFloatAsState(if (isSelected) 0.15f else 0f, label = "TabBg")
    val textColor by animateColorAsState(if (isSelected) Color(0xFF00FFCC) else Color.White.copy(alpha = 0.4f), label = "TabText")
    Box(modifier = Modifier.height(36.dp).background(Color(0xFF00FFCC).copy(alpha = backgroundAlpha), RoundedCornerShape(18.dp)).clickable { onClick() }.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
        Text(text = label.uppercase(), color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun ShimmerStandingRow() {
    val shimmerColors = listOf(Color.White.copy(alpha = 0.05f), Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.05f))
    val transition = rememberInfiniteTransition(label = "")
    val translateAnim = transition.animateFloat(initialValue = 0f, targetValue = 1000f, animationSpec = infiniteRepeatable(animation = tween(1200, easing = LinearEasing), repeatMode = RepeatMode.Restart), label = "")
    val brush = Brush.linearGradient(colors = shimmerColors, start = Offset.Zero, end = Offset(x = translateAnim.value, y = translateAnim.value))
    Box(modifier = Modifier.fillMaxWidth().height(61.dp).background(brush, RoundedCornerShape(12.dp)))
}

@Composable
fun FloatingBottomBar(currentScreen: AppScreen, onNavigate: (AppScreen) -> Unit) {
    val items = listOf(
        Triple(Icons.Default.DateRange, "Calendar", AppScreen.CALENDAR),
        Triple(Icons.Default.Home, "Home", AppScreen.HOME),
        Triple(Icons.Default.Leaderboard, "Classifiche", AppScreen.STANDINGS),
        Triple(Icons.Default.Person, "Personal", AppScreen.PERSONAL)
    )
    val barWidth = 240.dp
    val barHeight = 52.dp
    Surface(modifier = Modifier.height(barHeight).width(barWidth), shape = RoundedCornerShape(26.dp), color = Color(0xFF1E0A0A).copy(alpha = 0.82f), border = BorderStroke(1.2.dp, Color(0xFFFF0033).copy(alpha = 0.9f))) {
        Box(modifier = Modifier.fillMaxSize()) {
            val selectedIndex = items.indexOfFirst { it.third == when(currentScreen) {
                AppScreen.CALENDAR -> AppScreen.CALENDAR
                AppScreen.CIRCUIT_DETAIL -> AppScreen.CALENDAR
                AppScreen.STANDINGS -> AppScreen.STANDINGS
                AppScreen.DRIVER_DETAIL -> AppScreen.STANDINGS
                AppScreen.PERSONAL -> AppScreen.PERSONAL
                AppScreen.RESULTS -> AppScreen.CALENDAR
                else -> AppScreen.HOME
            } }
            val itemWidth = barWidth / items.size
            val indicatorOffset by animateDpAsState(targetValue = (selectedIndex * itemWidth.value).dp, animationSpec = tween(300), label = "Indicator")
            Box(modifier = Modifier.offset(x = indicatorOffset).width(itemWidth).fillMaxHeight(), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(38.dp).background(Color(0xFF00FFCC).copy(alpha = 0.25f), CircleShape))
            }
            Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                items.forEach { (icon, label, screen) ->
                    val isSelected = when {
                        currentScreen == screen -> true
                        screen == AppScreen.STANDINGS && (currentScreen == AppScreen.STANDINGS || currentScreen == AppScreen.DRIVER_DETAIL) -> true
                        screen == AppScreen.CALENDAR && (currentScreen == AppScreen.RESULTS || currentScreen == AppScreen.CIRCUIT_DETAIL) -> true
                        screen == AppScreen.HOME && currentScreen !in listOf(AppScreen.CALENDAR, AppScreen.PERSONAL, AppScreen.RESULTS, AppScreen.STANDINGS, AppScreen.WEATHER_DETAIL, AppScreen.DRIVER_DETAIL, AppScreen.RACE_SESSIONS, AppScreen.CIRCUIT_DETAIL) -> true
                        else -> false
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        IconButton(onClick = { onNavigate(screen) }) {
                            Icon(imageVector = icon, contentDescription = label, tint = if (isSelected) Color(0xFF00FFCC) else Color.White.copy(alpha = 0.4f), modifier = Modifier.size(24.dp))
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
        Spacer(modifier = Modifier.height(26.dp)) 
        Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.BottomStart) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp).background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)))
            } else {
                Column {
                    val rawGpName = raceWeek?.gp_name?.uppercase()?.replace("GRAND PRIX", "")?.trim() ?: "JAPANESE"
                    val displayGpName = "$rawGpName GP"
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = Color(0xFF00FFCC), shape = RoundedCornerShape(4.dp)) {
                            Text(text = "R${raceWeek?.round_number ?: "3"}", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = "NEXT EVENT", color = Color.White.copy(alpha = 0.5f), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(text = displayGpName, color = Color.White, fontSize = 58.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, letterSpacing = (-5).sp, lineHeight = 64.sp, maxLines = 1, overflow = TextOverflow.Clip)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val dates = raceWeek?.dates
                    val dateRangeStr = if (!dates.isNullOrEmpty() && dates.size >= 3) {
                        val startDay = dates[0].substringBefore(" ").trimStart('0')
                        val endDay = dates[2].substringBefore(" ").trimStart('0')
                        val month = dates[2].substringAfter(" ").uppercase()
                        "$startDay-$endDay $month"
                    } else { "3-5 APRIL" }
                    val cityStr = raceWeek?.city?.uppercase() ?: "SUZUKA"
                    val countryStr = raceWeek?.country ?: "Japan"
                    val locationStr = "$cityStr ($countryStr)"
                    Text(text = "$dateRangeStr \u2022 $locationStr", color = Color(0xFF00FFCC), fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.offset(y = (-4).dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Spacer(modifier = Modifier.height(26.dp)) 
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) { 
            FullWidthGlassCard(title = "RACE SESSIONS", content = "See all schedule...", accentColor = Color(0xFF00FFCC), isHighlighted = true, onClick = { onNavigate(AppScreen.RACE_SESSIONS) })
            val weatherStatus = raceWeek?.weather_forecast?.status ?: "Partly Cloudy"
            val weatherIcon = when {
                weatherStatus.contains("Sunny") -> "\u2600\ufe0f"
                weatherStatus.contains("Cloudy") -> "\u26c5"
                weatherStatus.contains("Rain") -> "\ud83c\udf27\ufe0f"
                else -> "\u2601\ufe0f"
            }
            FullWidthGlassCard(title = "WEATHER FORECAST", content = "$weatherIcon $weatherStatus \u2022 ${raceWeek?.weather_forecast?.temp ?: "22\u00b0C"}", accentColor = Color(0xFF00FFCC), onClick = { onNavigate(AppScreen.WEATHER_DETAIL) })
            FullWidthGlassCard(title = "TECHNICAL UPDATES", content = "Check latest upgrades...", accentColor = Color(0xFF00FFCC), onClick = { onNavigate(AppScreen.UPDATES_LIST) })
        }
    }
}

@Composable
fun FullWidthGlassCard(title: String, content: String, accentColor: Color, isHighlighted: Boolean = false, onClick: () -> Unit) {
    val cardBackground = if (isHighlighted) Color(0xFF00FFCC).copy(alpha = 0.05f) else Color.White.copy(alpha = 0.05f)
    val cardBorder = if (isHighlighted) Color(0xFF00FFCC).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.1f)
    val titleColor = if (isHighlighted) Color(0xFF00FFCC) else accentColor
    Surface(modifier = Modifier.fillMaxWidth().height(76.dp).clickable { onClick() }, shape = RoundedCornerShape(20.dp), color = cardBackground, border = BorderStroke(0.5.dp, cardBorder)) {
        Column(modifier = Modifier.padding(horizontal = 16.dp).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
            Text(text = title, color = titleColor, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
            Text(text = content, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun UpdatesListScreen(updates: List<TeamUpdatesResponse>, isLoading: Boolean, onTeamClick: (TeamUpdatesResponse) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(46.dp)) 
        Text(text = "UPDATES", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(start = 20.dp, bottom = 24.dp))
        if (isLoading && updates.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Color(0xFF00FFCC)) }
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 120.dp)) {
                items(updates) { update ->
                    TeamUpdateCard(update.team_name, update.team_color_hex, onClick = { onTeamClick(update) })
                }
            }
        }
    }
}

@Composable
fun TeamUpdateDetailScreen(teamUpdate: TeamUpdatesResponse) {
    val teamColor = try { Color(android.graphics.Color.parseColor(teamUpdate.team_color_hex)) } catch (e: Exception) { Color.Gray }
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(modifier = Modifier.height(46.dp))
        Text(text = teamUpdate.team_name.uppercase(), color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic)
        Spacer(modifier = Modifier.height(24.dp))
        LazyColumn(contentPadding = PaddingValues(bottom = 120.dp)) {
            items(teamUpdate.updates) { update ->
                Surface(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), shape = RoundedCornerShape(16.dp), color = Color.White.copy(alpha = 0.05f), border = BorderStroke(0.5.dp, teamColor.copy(alpha = 0.3f))) {
                    Text(text = update, color = Color.White.copy(alpha = 0.8f), modifier = Modifier.padding(20.dp), lineHeight = 24.sp)
                }
            }
        }
    }
}

@Composable
fun TeamUpdateCard(name: String, colorHex: String, onClick: () -> Unit) {
    val teamColor = try { Color(android.graphics.Color.parseColor(colorHex)) } catch (e: Exception) { Color.Gray }
    Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp).clickable { onClick() }, shape = RoundedCornerShape(16.dp), color = Color.White.copy(alpha = 0.05f), border = BorderStroke(0.5.dp, teamColor.copy(alpha = 0.4f))) {
        Row(modifier = Modifier.padding(20.dp).fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(4.dp, 24.dp).background(teamColor, RoundedCornerShape(2.dp)))
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PersonalScreen() {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(modifier = Modifier.height(46.dp))
        Text(text = "PERSONAL", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic)
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "PROFILE DETAILS", color = Color.White.copy(alpha = 0.5f), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun StandingRow(pos: Int, name: String, points: String, subtitle: String? = null, height: androidx.compose.ui.unit.Dp = 72.dp, onClick: () -> Unit = {}) {
    Surface(modifier = Modifier.fillMaxWidth().height(height).clickable { onClick() }, shape = RoundedCornerShape(12.dp), color = Color.White.copy(alpha = 0.03f)) {
        Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = pos.toString(), color = Color(0xFF00FFCC), fontSize = 18.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(40.dp), lineHeight = 22.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, lineHeight = 18.sp)
                if (!subtitle.isNullOrBlank()) {
                    Text(text = subtitle.uppercase(), color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp, fontWeight = FontWeight.Black, lineHeight = 12.sp)
                }
            }
            Text(text = points, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, lineHeight = 22.sp)
        }
    }
}
