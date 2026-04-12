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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
    val context = LocalContext.current
    val database = remember { FormulaDatabase.getDatabase(context) }
    val repository = remember { FormulaRepository(database) }

    val raceWeekEntity by repository.currentRaceWeek.collectAsState(initial = null)
    val raceWeek = raceWeekEntity?.let { entity ->
        val weather = entity.weather_json?.let { json ->
            try {
                com.google.gson.Gson().fromJson(json, WeatherForecast::class.java)
            } catch (e: Exception) { null }
        }
        val sessions = SessionTimes(entity.fp1_time, entity.fp2_time, entity.fp3_time, entity.sprint_shootout_time, entity.sprint_race_time, entity.quali_time, entity.race_time)
        RaceWeekResponse(
            entity.gp_name, entity.country, entity.city, entity.circuit_name,
            entity.round_number, entity.is_sprint, entity.status, entity.dates_joined.split(","), weather, sessions
        )
    }

    var updatesWrapper by remember { mutableStateOf<TeamUpdatesWrapper?>(null) }

    var isLoadingUpdates by remember { mutableStateOf(false) }
    var currentScreen by remember { mutableStateOf(AppScreen.HOME) }
    var selectedTeam by remember { mutableStateOf<TeamUpdatesResponse?>(null) }
    var selectedRound by remember { mutableIntStateOf(0) }
    var selectedGpName by remember { mutableStateOf("") }
    var selectedDriverName by remember { mutableStateOf("") }
    var selectedCircuitRound by remember { mutableIntStateOf(0) }

    var selectedSprintForSessions by remember { mutableStateOf(false) }
    var selectedGpForSessions by remember { mutableStateOf("") }
    var selectedCountryForSessions by remember { mutableStateOf("") }
    var selectedSessions by remember { mutableStateOf<SessionTimes?>(null) }
    var selectedGpStatusForSessions by remember { mutableStateOf("future") }
    var previousScreenForSessions by remember { mutableStateOf(AppScreen.HOME) }

    var showNotReadyDialog by remember { mutableStateOf(false) }

    var isBottomBarVisible by remember { mutableStateOf(true) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -15) { isBottomBarVisible = false }
                else if (available.y > 15) { isBottomBarVisible = true }
                return Offset.Zero
            }
            
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                // Se l'utente tenta di scorrere verso il basso ma ha raggiunto la fine della pagina (available.y negativo)
                if (available.y < -10) { isBottomBarVisible = true }
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
            AppScreen.CALENDAR, AppScreen.STANDINGS, AppScreen.PERSONAL, AppScreen.UPDATES_LIST, AppScreen.WEATHER_DETAIL -> currentScreen = AppScreen.HOME
            AppScreen.TEAM_DETAIL -> currentScreen = AppScreen.UPDATES_LIST
            AppScreen.DRIVER_DETAIL -> currentScreen = (if (selectedRound > 0) AppScreen.RESULTS else AppScreen.STANDINGS)
            AppScreen.CIRCUIT_DETAIL -> currentScreen = AppScreen.CALENDAR
            AppScreen.RESULTS -> currentScreen = AppScreen.CIRCUIT_DETAIL
            AppScreen.RACE_SESSIONS -> currentScreen = previousScreenForSessions
            else -> currentScreen = AppScreen.HOME
        }
    }

    LaunchedEffect(Unit) {
        launch { repository.refreshCurrentRaceWeek() }

        // Lanciamo il pre-fetch di TUTTO il calendario, sessioni e risultati storici in background
        // all'avvio dell'app. Così sarà tutto disponibile anche offline!
        launch { repository.refreshCalendar() }

        // Pre-fetch delle classifiche costruttori e piloti all'avvio!
        launch { repository.refreshStandings() }

        try {
            isLoadingUpdates = true
            Log.d("API_CALL", "Requesting Car Updates...")
            val wrapper = RetrofitClient.apiService.getLatestCarUpdates()
            updatesWrapper = wrapper
            Log.d("API_CALL", "Success: ${wrapper.gp} updates loaded")
        } catch (e: Exception) {
            Log.e("API_ERROR", "Error during initial data fetch: ${e.message}", e)
        } finally {
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
                    AppScreen.HOME -> HomeScreen(raceWeek, raceWeek == null, onNavigate = {
                        if (it == AppScreen.RACE_SESSIONS) {
                            selectedSprintForSessions = raceWeek?.is_sprint ?: false
                            selectedGpForSessions = raceWeek?.gp_name ?: ""
                            selectedCountryForSessions = raceWeek?.country ?: ""
                            selectedSessions = raceWeek?.sessions
                            selectedGpStatusForSessions = raceWeek?.status ?: "future"
                            previousScreenForSessions = AppScreen.HOME
                            currentScreen = it
                        } else if (it == AppScreen.UPDATES_LIST && updatesWrapper?.status == "not_ready") {
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
                    AppScreen.WEATHER_DETAIL -> WeatherDetailScreen(raceWeek, raceWeekEntity)
                    AppScreen.RESULTS -> RaceResultsScreen(selectedRound, selectedGpName, onDriverClick = { name ->
                        selectedDriverName = name
                        currentScreen = AppScreen.DRIVER_DETAIL
                    })
                    AppScreen.STANDINGS -> StandingsScreen(
                        onDriverClick = { name ->
                            selectedDriverName = name
                            currentScreen = AppScreen.DRIVER_DETAIL
                        }
                    )
                    AppScreen.DRIVER_DETAIL -> DriverDetailScreen(selectedDriverName)
                    AppScreen.RACE_SESSIONS -> RaceSessionsScreen(selectedSprintForSessions, selectedGpForSessions, selectedCountryForSessions, selectedSessions, selectedGpStatusForSessions)
                    AppScreen.CIRCUIT_DETAIL -> CircuitDetailScreen(
                        round = selectedCircuitRound,
                        onNavigateToResults = { round, name ->
                            selectedRound = round
                            selectedGpName = name
                            currentScreen = AppScreen.RESULTS
                        },
                        onNavigateToSessions = { isSprint, name, country, sessions, gpStatus ->
                            selectedSprintForSessions = isSprint
                            selectedGpForSessions = name
                            selectedCountryForSessions = country
                            selectedSessions = sessions
                            selectedGpStatusForSessions = gpStatus
                        previousScreenForSessions = AppScreen.CIRCUIT_DETAIL
                            currentScreen = AppScreen.RACE_SESSIONS
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
fun CircuitDetailScreen(round: Int, onNavigateToResults: (Int, String) -> Unit, onNavigateToSessions: (Boolean, String, String, SessionTimes, String) -> Unit) {
    val context = LocalContext.current
    val database = remember { FormulaDatabase.getDatabase(context) }
    val repository = remember { FormulaRepository(database) }

    val circuitEntity by repository.getCircuitDetail(round).collectAsState(initial = null)
    val circuitData = circuitEntity?.let {
        val sessions = SessionTimes(it.fp1_time, it.fp2_time, it.fp3_time, it.sprint_shootout_time, it.sprint_race_time, it.quali_time, it.race_time)
        CircuitDetailResponse(
            it.round, it.gp_name, it.circuit_name, it.location, it.country,
            it.length, it.corners, it.laps, it.record, it.is_sprint, it.dates_joined.split(","),
            it.status, it.previous_winner, it.most_driver_wins,
            it.most_constructor_wins, it.most_driver_podiums, it.most_poles,
            it.num_races_held, sessions
        )
    }
    
    val isLoading = circuitData == null

    LaunchedEffect(round) {
        repository.refreshCircuitDetail(round)
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
                    val countryFormat = data.country.lowercase().replace(" ", "_")
                    val resourceName = "flag_$countryFormat"
                    val resourceId = remember(resourceName) {
                        context.resources.getIdentifier(resourceName, "drawable", context.packageName)
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
                                contentDescription = "Country Flag",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().alpha(0.35f)
                            )
                        }
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = Color(0xFF00FFCC), shape = RoundedCornerShape(4.dp)) {
                                Text(text = "R${data.round}", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = "GRAND PRIX", color = Color.White.copy(alpha = 0.5f), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        val rawGpName = data.gp_name.uppercase().replace("GRAND PRIX", "").trim()
                        var displayGpName = "$rawGpName GP"
                        if (displayGpName.length > 11) { // "JAPANESE GP" è 11 caratteri
                            displayGpName = data.country.uppercase()
                        }

                        Text(
                            text = displayGpName, color = Color.White, fontSize = 58.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, letterSpacing = (-5).sp, maxLines = 1, softWrap = false
                        )

                        val dates = data.dates
                        val dateRangeStr = if (dates.size >= 3) {
                            val startDay = dates[0].substringBefore(" ").trimStart('0')
                            val endDay = dates[2].substringBefore(" ").trimStart('0')
                            val month = dates[2].substringAfter(" ").uppercase()
                            "$startDay-$endDay $month"
                        } else { "" }

                        val locationStr = data.location.uppercase().replace("MONTE CARLO", "MONTECARLO")
                        Text(
                            text = "$dateRangeStr \u2022 $locationStr", 
                            color = Color(0xFF00FFCC), 
                            fontSize = 18.sp, 
                            fontWeight = FontWeight.Black, 
                            fontStyle = FontStyle.Italic, 
                            letterSpacing = (-1).sp,
                            modifier = Modifier.offset(y = (-8).dp), 
                            maxLines = 1, 
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 120.dp)) {
                    item {
                        // CARD DEL TRACCIATO E DATI TECNICI
                        Surface(
                            modifier = Modifier.fillMaxWidth().height(180.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White.copy(alpha = 0.02f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                // Sfondo tecnico a griglia per profondità
                                Canvas(modifier = Modifier.fillMaxSize().alpha(0.06f)) {
                                    val gridSize = 16.dp.toPx()
                                    for (x in 0..size.width.toInt() step gridSize.toInt()) {
                                        drawLine(Color.White, Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height), strokeWidth = 1f)
                                    }
                                    for (y in 0..size.height.toInt() step gridSize.toInt()) {
                                        drawLine(Color.White, Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()), strokeWidth = 1f)
                                    }
                                }
                                
                                Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                                        // Layout Tracciato (Sinistra)
                                        Box(modifier = Modifier.weight(1.3f).fillMaxHeight().padding(12.dp), contentAlignment = Alignment.Center) {
                                            val resourceName = "track_r${data.round}"
                                            val resourceId = remember(resourceName) {
                                                context.resources.getIdentifier(resourceName, "drawable", context.packageName)
                                            }
            
                                            if (resourceId != 0) {
                                                Image(
                                                    painter = painterResource(id = resourceId),
                                                    contentDescription = "Layout del tracciato di ${data.circuit_name}",
                                                    modifier = Modifier.fillMaxSize(),
                                                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color(0xFF00FFCC).copy(alpha = 0.9f))
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.Map,
                                                    contentDescription = "Layout del tracciato non disponibile",
                                                    modifier = Modifier.size(60.dp),
                                                    tint = Color.White.copy(alpha = 0.3f)
                                                )
                                            }
                                        }
                                        
                                        // Dati (Destra)
                                        Column(
                                            modifier = Modifier.weight(0.7f).fillMaxHeight().background(Color.White.copy(alpha = 0.03f)).padding(16.dp),
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                        Text(text = "DISTANCE", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
                                        Text(text = "${data.laps} LAPS", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.offset(y = (-2).dp))
                                        Spacer(modifier = Modifier.height(4.dp))
                                            Text(text = "LENGTH", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
                                            Text(text = data.length, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.offset(y = (-2).dp))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = "CORNERS", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
                                        Text(text = "${data.corners}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.offset(y = (-2).dp))
                                        }
                                }
                            }
                        }
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        // Pulsante delle sessioni o dei risultati
                        if (data.status == "future" || data.status == "current") {
                            Button(
                                onClick = { onNavigateToSessions(data.is_sprint, data.gp_name, data.country, data.sessions, data.status) },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC).copy(alpha = 0.9f))
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Timer, null, tint = Color.Black)
                                    Spacer(Modifier.width(12.dp))
                                    Text("PROSSIME SESSIONI", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 16.sp)
                                }
                            }
                        } else if (data.status == "past") {
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
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    
                    item { HistoricalDataCard(data) }
                }
            }
        }
    }
}

@Composable
fun HistoricalDataCard(data: CircuitDetailResponse) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "HISTORICAL DATA", 
            color = Color(0xFF00FFCC), 
            fontSize = 28.sp, 
            fontWeight = FontWeight.Black, 
            fontStyle = FontStyle.Italic,
            letterSpacing = (-1.5).sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Formattazione dinamica per separare il tempo dal nome del pilota e dall'anno
        val recordParts = data.record.split(" (")
        if (recordParts.size == 2) {
            val time = recordParts[0]
            val driverAndYear = recordParts[1].removeSuffix(")")
            val subParts = driverAndYear.split(", ")
            val formattedDriverYear = if (subParts.size == 2) {
                "${subParts[0]} (${subParts[1]})"
            } else {
                driverAndYear
            }
            HistoricalStatItem("LAP RECORD: $time", formattedDriverYear, Icons.Default.Timer)
        } else {
            HistoricalStatItem("LAP RECORD", data.record, Icons.Default.Timer)
        }
        HistoricalStatItem("PREVIOUS WINNER", data.previous_winner, Icons.Default.EmojiEvents)
        HistoricalStatItem("MOST WINS (DRIVER)", data.most_driver_wins, Icons.Default.Person)
        HistoricalStatItem("MOST WINS (TEAM)", data.most_constructor_wins, Icons.Default.PrecisionManufacturing)
        HistoricalStatItem("MOST POLES", data.most_poles, Icons.Default.Flag)
    }
}

@Composable
fun HistoricalStatItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.03f),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF00FFCC).copy(alpha = 0.1f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = Color(0xFF00FFCC), modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
                Text(
                    text = value.uppercase(), 
                    color = Color.White, 
                    fontSize = 18.sp, 
                    fontWeight = FontWeight.Black, 
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.offset(y = (-5).dp)
                )
            }
        }
    }
}

@Composable
fun DriverDetailScreen(driverName: String) {
    val context = LocalContext.current
    val database = remember { FormulaDatabase.getDatabase(context) }
    val repository = remember { FormulaRepository(database) }

    val nameParts = driverName.split(" ")
    val firstName = nameParts.dropLast(1).joinToString(" ").uppercase()
    val lastName = nameParts.lastOrNull()?.uppercase() ?: ""

    // Map del driverId a partire dal nome, compatibile con il Backend
    val driverId = remember(driverName) {
        val lowerLast = lastName.lowercase()
        if (lowerLast.contains("sainz")) "sainz" 
        else if (lowerLast == "verstappen") "max_verstappen" 
        else if (lowerLast == "lindblad" || lowerLast == "limblad") "arvid_lindblad"
        else lowerLast.replace(" jr.", "")
    }

    val stats by repository.getDriverStats(driverId).collectAsState(initial = null)

    LaunchedEffect(driverId) {
        repository.refreshDriverStats(driverId)
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(modifier = Modifier.height(26.dp))
        
        Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.BottomStart) {
            val countryName = getDriverCountryForFlag(driverId)
            val resourceName = "flag_$countryName"
            val resourceId = remember(resourceName) {
                context.resources.getIdentifier(resourceName, "drawable", context.packageName)
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
                        contentDescription = "Driver Nationality Flag",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().alpha(0.35f)
                    )
                }
            }

            Column {
                Text(
                    text = "$firstName\n$lastName",
                    color = Color.White,
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    letterSpacing = (-3).sp,
                    lineHeight = 44.sp
                )
                Text(
                    text = "DRIVER STATS",
                    color = Color(0xFF00FFCC),
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    letterSpacing = (-2).sp,
                    modifier = Modifier.offset(y = (-10).dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        if (stats == null) {
            Box(modifier = Modifier.fillMaxSize().padding(top = 40.dp), contentAlignment = Alignment.TopCenter) {
                CircularProgressIndicator(color = Color(0xFF00FFCC))
            }
        } else {
            stats?.let { statData ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatGlassCard(title = "WORLD CHAMP.", value = statData.world_championships.toString(), isGold = true, modifier = Modifier.weight(1f).height(110.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatGlassCard(title = "WINS", value = statData.wins.toString(), modifier = Modifier.weight(1f).height(90.dp))
                    StatGlassCard(title = "PODIUMS", value = statData.podiums.toString(), modifier = Modifier.weight(1f).height(90.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatGlassCard(title = "POLES", value = statData.pole_positions.toString(), modifier = Modifier.weight(1f).height(90.dp))
                    StatGlassCard(title = "RACES", value = statData.total_races.toString(), modifier = Modifier.weight(1f).height(90.dp))
                }
            }
        }
    }
}

fun getDriverCountryForFlag(driverId: String): String {
    return when(driverId) {
        "russell", "hamilton", "norris", "arvid_lindblad", "bearman" -> "uk"
        "antonelli" -> "italy"
        "max_verstappen" -> "netherlands"
        "hadjar", "gasly", "ocon" -> "france"
        "leclerc" -> "monaco"
        "piastri" -> "australia"
        "alonso", "sainz" -> "spain"
        "stroll" -> "canada"
        "colapinto" -> "argentina"
        "albon" -> "thailand"
        "lawson" -> "new_zealand"
        "hulkenberg" -> "germany"
        "bortoleto" -> "brazil"
        "perez" -> "mexico"
        "bottas" -> "finland"
        else -> ""
    }
}

@Composable
fun StatGlassCard(title: String, value: String, isGold: Boolean = false, modifier: Modifier = Modifier) {
    val color = if (isGold) Color(0xFFFFD700) else Color(0xFF00FFCC)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.03f),
        border = BorderStroke(1.dp, color.copy(alpha = if (isGold) 0.6f else 0.2f))
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                color = Color.White,
                fontSize = if (isGold) 48.sp else 34.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic
            )
            Text(text = title, color = color.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
    }
}

@Composable
fun StandingsScreen(
    onDriverClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val database = remember { FormulaDatabase.getDatabase(context) }
    val repository = remember { FormulaRepository(database) }

    // Osserviamo il database in tempo reale (Flow -> State)
    val driverEntities by repository.driverStandings.collectAsState(initial = emptyList())
    val constructorEntities by repository.constructorStandings.collectAsState(initial = emptyList())

    // Mappiamo le Entities del DB ai modelli UI
    val drivers = driverEntities.map { DriverStanding(it.position, it.driver_name, it.constructor_name, it.points, it.wins) }
    val constructors = constructorEntities.map { ConstructorStanding(it.position, it.constructor_name, it.chassis_name, it.points, it.wins) }

    var selectedTab by remember { mutableStateOf("Drivers") }
    val isLoading = drivers.isEmpty() && constructors.isEmpty()
    
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
        // Chiediamo al repository di scaricare dati freschi in background.
        // Se non c'è internet, la UI continua felicemente a mostrare i dati del DB!
        repository.refreshStandings()
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp).then(dragModifier)) {
        Spacer(modifier = Modifier.height(46.dp)) 
        Text(text = "CLASSIFICHE", color = Color.White, fontSize = 54.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, letterSpacing = (-3).sp, lineHeight = 50.sp)
        
        Row(modifier = Modifier.fillMaxWidth().offset(y = (-10).dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = "2026", color = Color(0xFF00FFCC), fontSize = 38.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, letterSpacing = (-2).sp)
            Spacer(modifier = Modifier.width(16.dp)) // Distanza minima dallo slider
            Row(modifier = Modifier.wrapContentWidth().background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(12.dp)).padding(2.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                TabItemMinimalAnimated("Drivers", selectedTab == "Drivers") { selectedTab = "Drivers" }
                TabItemMinimalAnimated("Constructors", selectedTab == "Constructors") { selectedTab = "Constructors" }
            }
        }

        Spacer(modifier = Modifier.height(16.dp)) // Distanza aumentata tra header e classifiche

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
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(bottom = 120.dp), modifier = Modifier.graphicsLayer {
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
                                LeaderCard(name = leader.driver_name, subtitle = formatStandingsTeam(leader.constructor_name), points = leader.points.toString(), gap = if (leader.points - secondPlacePoints > 0) "+${leader.points - secondPlacePoints} PTS" else "LEADER", icon = "1", onClick = { onDriverClick(leader.driver_name) })
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                        items(drivers.drop(1)) { driver -> StandingRow(driver.position, driver.driver_name, driver.points.toString(), formatStandingsTeam(driver.constructor_name), height = 61.dp, onClick = { onDriverClick(driver.driver_name) }) }
                    } else {
                        val leader = constructors.firstOrNull()
                        val secondPlacePoints = constructors.getOrNull(1)?.points ?: 0
                        if (leader != null) {
                            item {
                                LeaderCard(
                                    name = formatStandingsTeam(leader.constructor_name),
                                    subtitle = leader.chassis_name ?: "CHASSIS",
                                    points = leader.points.toString(),
                                    gap = if (leader.points - secondPlacePoints > 0) "+${leader.points - secondPlacePoints} PTS" else "LEADER",
                                    icon = "\uD83C\uDFCE\uFE0F",
                                    teamNameForImage = leader.constructor_name
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                        items(constructors.drop(1)) { team -> StandingRow(team.position, formatStandingsTeam(team.constructor_name), team.points.toString(), subtitle = team.chassis_name, height = 61.dp) }
                    }
                }
            }
        }
    }
}

fun formatStandingsTeam(fullName: String): String {
    val upper = fullName.uppercase()
    return when {
        upper.contains("HAAS") -> "HAAS"
        upper.contains("ALPINE") -> "ALPINE"
        upper.contains("RB") || upper.contains("RACING BULLS") -> "RACING BULLS"
        upper.contains("CADILLAC") -> "CADILLAC"
        else -> upper
    }
}

// Helper per associare dinamicamente le vetture ai vari nomi dei team
fun getCarDrawableName(teamName: String): String {
    val lower = teamName.lowercase()
    return when {
        lower.contains("mercedes") -> "mercedes_model"
        lower.contains("ferrari") -> "ferrari_model"
        lower.contains("red bull") || lower.contains("redbull") -> "red_bull_model"
        lower.contains("mclaren") -> "mclaren_model"
        lower.contains("aston") -> "aston_model"
        lower.contains("alpine") -> "alpine_model"
        lower.contains("williams") -> "williams_model"
        lower.contains("racing bulls") || lower.contains("rb") || lower.contains("alphatauri") -> "rb_model"
        lower.contains("haas") -> "haas_model"
        lower.contains("audi") || lower.contains("sauber") -> "audi_model"
        lower.contains("cadillac") -> "cadillac_model"
        else -> "car_placeholder"
    }
}

@Composable
fun LeaderCard(name: String, subtitle: String, points: String, gap: String, icon: String, teamNameForImage: String? = null, onClick: () -> Unit = {}) {
    val context = LocalContext.current
    Surface(modifier = Modifier.fillMaxWidth().height(140.dp).clickable { onClick() }, shape = RoundedCornerShape(24.dp), color = Color(0xFF1E0A0A).copy(alpha = 0.4f), border = BorderStroke(1.5.dp, Color(0xFF00FFCC).copy(alpha = 0.6f))) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize().alpha(0.1f)) {
                drawCircle(color = Color(0xFF00FFCC), radius = size.width / 3f, center = Offset(size.width, 0f))
            }
            Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Surface(color = Color(0xFF00FFCC).copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                        Text(text = "CHAMPIONSHIP LEADER", color = Color(0xFF00FFCC), fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = name.uppercase(), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, lineHeight = 30.sp)
                    Text(text = subtitle.uppercase(), color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, modifier = Modifier.offset(y = (-4).dp))
                }
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.offset(y = (-1).dp)) {
                    Text(text = points, color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, lineHeight = 42.sp)
                    Text(text = gap, color = Color(0xFF00FFCC), fontSize = 12.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, modifier = Modifier.offset(y = (-8).dp))
                }
            }

            if (teamNameForImage != null) {
                val resourceName = getCarDrawableName(teamNameForImage) // Cerca esattamente mercedes_model!
                val resourceId = remember(resourceName) {
                    context.resources.getIdentifier(resourceName, "drawable", context.packageName)
                }

                if (resourceId != 0) {
                    Image(
                        painter = painterResource(id = resourceId),
                        contentDescription = "Car of $teamNameForImage",
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .fillMaxWidth(0.80f) // Dimensione della vettura aumentata
                            .offset(x = 25.dp, y = 5.dp) // Effetto sfondamento e pavimento
                            .alpha(0.45f), // Opacità da "sfondo"
                        contentScale = ContentScale.Fit
                    )
                } else {
                    // Fallback se l'SVG della vettura non è presente
                    Text(text = icon, color = Color.White.copy(alpha = 0.05f), fontSize = 120.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.BottomEnd).offset(x = 20.dp, y = 40.dp))
                }
            } else {
                // Per la card pilota restituiamo solo il suo numero (o eventuale fallback)
                Text(text = icon, color = Color.White.copy(alpha = 0.05f), fontSize = 120.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.BottomEnd).offset(x = 20.dp, y = 40.dp))
            }
        }
    }
}

@Composable
fun TabItemMinimalAnimated(label: String, isSelected: Boolean, onClick: () -> Unit) {
    val backgroundAlpha by animateFloatAsState(if (isSelected) 0.15f else 0f, label = "TabBg")
    val textColor by animateColorAsState(if (isSelected) Color(0xFF00FFCC) else Color.White.copy(alpha = 0.4f), label = "TabText")
    Box(modifier = Modifier.height(30.dp).background(Color(0xFF00FFCC).copy(alpha = backgroundAlpha), RoundedCornerShape(10.dp)).clickable { onClick() }.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
        Text(text = label.uppercase(), color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic)
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
    val barWidth = 252.dp
    val barHeight = 55.dp
    Surface(modifier = Modifier.height(barHeight).width(barWidth), shape = CircleShape, color = Color(0xFF1E0A0A).copy(alpha = 0.90f)) {
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
    val context = LocalContext.current
    val database = remember { FormulaDatabase.getDatabase(context) }
    val repository = remember { FormulaRepository(database) }
    val roundNumber = raceWeek?.round_number ?: 0
    val resultsEntities by repository.getRaceResults(roundNumber).collectAsState(initial = emptyList())
    val top5Results = resultsEntities.take(5).map { RaceResultResponse(it.position, it.driver, it.team, it.points, it.time) }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(modifier = Modifier.height(26.dp)) 
        Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.BottomStart) {
            // --- SFONDO CON BANDIERA NAZIONALE ---
            if (!isLoading && raceWeek != null) {
                val countryFormat = raceWeek.country.lowercase().replace(" ", "_")
                val resourceName = "flag_$countryFormat"
                val resourceId = remember(resourceName) {
                    context.resources.getIdentifier(resourceName, "drawable", context.packageName)
                }
                if (resourceId != 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.8f) // Occupa l'80% per una sfumatura più lunga
                            .align(Alignment.CenterEnd)
                            .graphicsLayer { 
                                alpha = 0.99f 
                                translationX = 20.dp.toPx() // Copre il padding orizzontale di destra
                                translationY = -26.dp.toPx() // Copre lo spacer in alto
                                scaleX = 1.26f // Ingrandisce per non lasciare bordi vuoti
                                scaleY = 1.26f
                            } 
                            .drawWithContent {
                                drawContent()
                                // Maschera per sfumare da sinistra a destra
                                drawRect(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(Color.Transparent, Color.Black),
                                        startX = 0f,
                                        endX = size.width * 0.6f
                                    ),
                                    blendMode = BlendMode.DstIn
                                )
                                // Maschera per sfumare dal basso verso l'alto
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
                            contentDescription = "Country Flag",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().alpha(0.35f) // Maggiore luminosità
                        )
                    }
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp).background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)))
            } else {
                Column {
                    val rawGpName = raceWeek?.gp_name?.uppercase()?.replace("GRAND PRIX", "")?.trim() ?: "JAPANESE"
                    var displayGpName = "$rawGpName GP"
                    if (displayGpName.length > 11) {
                        displayGpName = raceWeek?.country?.uppercase() ?: rawGpName
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = Color(0xFF00FFCC), shape = RoundedCornerShape(4.dp)) {
                            Text(text = "R${raceWeek?.round_number ?: "3"}", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                            Text(text = "NEXT EVENT", color = Color.White.copy(alpha = 0.5f), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(text = displayGpName, color = Color.White, fontSize = 58.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, letterSpacing = (-5).sp, maxLines = 1, softWrap = false)
                    
                    val dates = raceWeek?.dates
                    val dateRangeStr = if (!dates.isNullOrEmpty() && dates.size >= 3) {
                        val startDay = dates[0].substringBefore(" ").trimStart('0')
                        val endDay = dates[2].substringBefore(" ").trimStart('0')
                        val month = dates[2].substringAfter(" ").uppercase()
                        "$startDay-$endDay $month"
                    } else { "3-5 APRIL" }
                    val cityStr = raceWeek?.city?.uppercase() ?: "SUZUKA"
                    val countryStr = raceWeek?.country ?: "Japan"
                    val locationStr = "$cityStr ($countryStr)".replace("MONTE CARLO", "MONTECARLO")
                    Text(
                        text = "$dateRangeStr \u2022 $locationStr", 
                        color = Color(0xFF00FFCC), 
                        fontSize = 18.sp, 
                        fontWeight = FontWeight.Black, 
                        fontStyle = FontStyle.Italic, 
                        letterSpacing = (-1).sp,
                        modifier = Modifier.offset(y = (-8).dp), 
                        maxLines = 1, 
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(26.dp)) 
        
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { 
            FullWidthGlassCard(title = "RACE SESSIONS", content = "See all schedule...", accentColor = Color(0xFF00FFCC), isHighlighted = true, onClick = { onNavigate(AppScreen.RACE_SESSIONS) })
            
            // Ora mostriamo se sta caricando o se i dati non sono disponibili
            val weatherStatus = raceWeek?.weather_forecast?.status ?: if (isLoading) "Loading..." else "Not Available"
            val temp = raceWeek?.weather_forecast?.temp ?: if (isLoading) "--" else "N/A"
            val weatherIcon = when {
                weatherStatus.contains("Sunny") || weatherStatus.contains("Clear") -> "\u2600\ufe0f" // Ora il sereno mostra il sole!
                weatherStatus.contains("Cloudy") -> "\u26c5"
                weatherStatus.contains("Rain") -> "\ud83c\udf27\ufe0f"
                weatherStatus == "Loading..." -> "\u23F3" // Mostra una clessidra se carica
                else -> "\u2601\ufe0f"
            }
            FullWidthGlassCard(title = "WEATHER FORECAST", content = "$weatherIcon $weatherStatus \u2022 $temp", accentColor = Color(0xFF00FFCC), onClick = { onNavigate(AppScreen.WEATHER_DETAIL) })
            FullWidthGlassCard(title = "TECHNICAL UPDATES", content = "Check latest upgrades...", accentColor = Color(0xFF00FFCC), onClick = { onNavigate(AppScreen.UPDATES_LIST) })
            
            Row(modifier = Modifier.fillMaxWidth().height(180.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LastSessionCard(top5Results, Modifier.weight(0.53f).fillMaxHeight())
                FocusOnTrackCard(roundNumber, raceWeek?.city ?: "CITY", Modifier.weight(0.47f).fillMaxHeight())
            }
            
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun FullWidthGlassCard(title: String, content: String, accentColor: Color, isHighlighted: Boolean = false, onClick: () -> Unit) {
    val cardBackground = if (isHighlighted) Color(0xFF00FFCC).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f)
    val cardBorder = if (isHighlighted) Color(0xFF00FFCC) else Color.White.copy(alpha = 0.1f)
    val titleColor = if (isHighlighted) Color(0xFF00FFCC) else accentColor
    Surface(modifier = Modifier.fillMaxWidth().height(72.dp).clickable { onClick() }, shape = RoundedCornerShape(20.dp), color = cardBackground, border = BorderStroke(0.5.dp, cardBorder)) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                Text(text = title, color = titleColor, fontSize = 13.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, letterSpacing = 1.sp)
                Text(text = content, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun LastSessionCard(results: List<RaceResultResponse>, modifier: Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(20.dp), color = Color.White.copy(alpha = 0.03f), border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 6.dp)) {
                Text("LAST SESSION", color = Color(0xFF00FFCC), fontSize = 13.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, letterSpacing = 1.sp)
                Text("TOP 5", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.offset(y = (-4).dp))
            }

            if (results.isEmpty()) {
                Column(modifier = Modifier.fillMaxSize().offset(y = (-10).dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Timer, null, tint = Color.White.copy(alpha = 0.2f), modifier = Modifier.size(32.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Risultati ancora\nnon disponibili", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, textAlign = TextAlign.Center, lineHeight = 13.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    results.forEachIndexed { index, res ->
                        val lastName = res.driver.split(" ").lastOrNull()?.uppercase() ?: ""
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("P${res.position}", color = if (index == 0) Color(0xFF00FFCC) else Color.White.copy(alpha = 0.5f), fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(22.dp))
                                Text(lastName, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 65.dp))
                            }
                            
                            val isDnf = isDnfOrDns(res.time)
                            val isDns = isDnf && (res.time.lowercase().contains("dns") || res.time.lowercase().contains("withdrawn"))
                            val isDsq = isDnf && (res.time.lowercase().contains("dsq") || res.time.lowercase().contains("disqualified"))
                            val statusText = if (isDnf) { if (isDsq) "DSQ" else if (isDns) "DNS" else "DNF" } else res.time

                            Text(
                                text = statusText, 
                                color = if (index == 0) Color.White else Color.White.copy(alpha = 0.5f), 
                                fontSize = 10.sp, 
                                fontWeight = FontWeight.Black, 
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FocusOnTrackCard(round: Int, city: String, modifier: Modifier) {
    val context = LocalContext.current
    Surface(modifier = modifier, shape = RoundedCornerShape(20.dp), color = Color.White.copy(alpha = 0.02f), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize().alpha(0.06f)) {
                val gridSize = 16.dp.toPx()
                for (x in 0..size.width.toInt() step gridSize.toInt()) { drawLine(Color.White, Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height), strokeWidth = 1f) }
                for (y in 0..size.height.toInt() step gridSize.toInt()) { drawLine(Color.White, Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()), strokeWidth = 1f) }
            }
            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.03f))
                        .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 6.dp)
                ) {
                    Text("FOCUS ON", color = Color(0xFF00FFCC), fontSize = 13.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, letterSpacing = 1.sp)
                    Text("TRACK", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, fontStyle = FontStyle.Italic, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.offset(y = (-4).dp))
                }
                
                Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 8.dp), contentAlignment = Alignment.Center) {
                    val resourceName = "track_r$round"
                    val resourceId = remember(resourceName) {
                        context.resources.getIdentifier(resourceName, "drawable", context.packageName)
                    }
                    if (resourceId != 0) {
                        Image(painter = painterResource(id = resourceId), contentDescription = null, modifier = Modifier.fillMaxSize().padding(top = 2.dp, bottom = 2.dp), colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color(0xFF00FFCC).copy(alpha = 0.9f)))
                    } else {
                        Icon(Icons.Default.Map, null, modifier = Modifier.size(40.dp), tint = Color.White.copy(alpha = 0.3f))
                    }
                }
            }
        }
    }
}

@Composable
fun UpdatesListScreen(updates: List<TeamUpdatesResponse>, isLoading: Boolean, onTeamClick: (TeamUpdatesResponse) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(46.dp)) 
            Text(text = "UPDATES", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(start = 20.dp).padding(bottom = 24.dp))
        if (isLoading && updates.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFF00FFCC)) }
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
            Text(text = pos.toString(), color = Color(0xFF00FFCC), fontSize = 20.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(40.dp), lineHeight = 24.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, lineHeight = 20.sp)
                if (!subtitle.isNullOrBlank()) {
                    Text(text = subtitle.uppercase(), color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, fontWeight = FontWeight.Black, lineHeight = 13.sp)
                }
            }
            Text(text = points, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, lineHeight = 22.sp)
        }
    }
}
