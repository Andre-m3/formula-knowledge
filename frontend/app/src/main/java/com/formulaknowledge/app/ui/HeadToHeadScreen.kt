package com.formulaknowledge.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.formulaknowledge.app.data.FormulaDatabase
import com.formulaknowledge.app.data.FormulaRepository
import kotlinx.coroutines.flow.flowOf
import com.formulaknowledge.app.utils.F1Utils
import kotlin.math.pow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeadToHeadScreen(
    driver1Id: String,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val database = remember { FormulaDatabase.getDatabase(context) }
    val repository = remember { FormulaRepository(database) }

    var currentDriver1Id by remember { mutableStateOf(driver1Id) }
    var currentDriver2Id by remember { mutableStateOf<String?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectingDriver by remember { mutableStateOf(2) }

    // --- 1. GESTIONE REATTIVA DEI DATI (FLOWS) ---
    val standings by repository.driverStandings.collectAsState(initial = emptyList())
    
    val d1Career by remember(currentDriver1Id) { repository.getDriverStats(currentDriver1Id) }.collectAsState(initial = null)
    val d1Season by remember(currentDriver1Id) { repository.getDriverSeasonStats(currentDriver1Id) }.collectAsState(initial = null)
    
    // Usiamo `remember` sui flow per non spezzare l'albero di composizione quando l'ID è nullo
    val d2CareerFlow = remember(currentDriver2Id) { currentDriver2Id?.let { repository.getDriverStats(it) } ?: flowOf(null) }
    val d2Career by d2CareerFlow.collectAsState(initial = null)

    val d2SeasonFlow = remember(currentDriver2Id) { currentDriver2Id?.let { repository.getDriverSeasonStats(it) } ?: flowOf(null) }
    val d2Season by d2SeasonFlow.collectAsState(initial = null)

    // Filtriamo dalla classifica generale i due piloti per estrapolare nomi completi e team
    val driver1Standing = standings.find { F1Utils.getDriverIdFromName(it.driver_name) == currentDriver1Id }
    val driver2Standing = currentDriver2Id?.let { id -> standings.find { F1Utils.getDriverIdFromName(it.driver_name) == id } }

    val color1 = F1Utils.getTeamColor(driver1Standing?.constructor_name)
    val color2 = currentDriver2Id?.let { F1Utils.getTeamColor(driver2Standing?.constructor_name) } ?: Color.DarkGray

    val name1 = driver1Standing?.driver_name?.uppercase() ?: currentDriver1Id.uppercase()
    val name2 = driver2Standing?.driver_name?.uppercase() ?: "SELEZIONA"

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("STAGIONE 2026", "CARRIERA")

    // Fetch da internet in background
    LaunchedEffect(currentDriver1Id) {
        repository.refreshDriverStats(currentDriver1Id)
        repository.refreshDriverSeasonStats(currentDriver1Id)
    }
    LaunchedEffect(currentDriver2Id) {
        currentDriver2Id?.let { 
            repository.refreshDriverStats(it) 
            repository.refreshDriverSeasonStats(it)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0F0F0F))) {
        // --- 2. HEADER SPLIT SCREEN STILE "FIGHTING GAME" ---
        Box(modifier = Modifier.fillMaxWidth().height(235.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val hOffset = h * 0.25f // Rende la linea leggermente meno inclinata

                val path1 = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(w, 0f)
                    lineTo(w, hOffset)
                    lineTo(0f, h - hOffset)
                    close()
                }
                val path2 = Path().apply {
                    moveTo(w, hOffset)
                    lineTo(w, h)
                    lineTo(0f, h)
                    lineTo(0f, h - hOffset)
                    close()
                }
                drawPath(path1, color1.copy(alpha = 0.6f))
                drawPath(path2, color2.copy(alpha = 0.6f))

                // Linea netta di separazione stile Fighting Game
                drawLine(
                    color = Color.White.copy(alpha = 0.2f),
                    start = androidx.compose.ui.geometry.Offset(w, hOffset),
                    end = androidx.compose.ui.geometry.Offset(0f, h - hOffset),
                    strokeWidth = 3f
                )
            }

            // Overlay scuro per migliorare la leggibilità
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)))

            // Layout Duellanti Ruotato
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val w = constraints.maxWidth.toFloat()
                val h = constraints.maxHeight.toFloat()
                val hOffset = h * 0.25f

                // Ricalcoliamo l'angolo di rotazione basandoci sulla nuova pendenza della linea
                val angle = (kotlin.math.atan2((2 * hOffset - h).toDouble(), w.toDouble()) * (180f / Math.PI)).toFloat()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationZ = angle }
                ) {
                    // Pilota 1 (Top Left)
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = (-34).dp), // L'offset Y ci allontana perpendicolarmente dalla linea!
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(driver1Standing?.constructor_name?.uppercase() ?: "", color = color1, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic, modifier = Modifier.offset(y = 8.dp))
                        Text(name1.split(" ").last(), color = Color.White, fontSize = 46.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic)
                    }

                    // Pilota 2 (Bottom Right)
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = 34.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (currentDriver2Id == null) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.2f),
                                // Ruotiamo il + in senso inverso per mantenerlo dritto
                                modifier = Modifier.size(54.dp).graphicsLayer { rotationZ = -angle }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.padding(12.dp))
                            }
                        } else {
                            Text(name2.split(" ").last(), color = Color.White, fontSize = 46.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic)
                            Text(driver2Standing?.constructor_name?.uppercase() ?: "", color = color2, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic, modifier = Modifier.offset(y = (-8).dp))
                        }
                    }
                }
            }

            // Overlay cliccabili trasparenti per selezione Pilota (divisi a metà)
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth().clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = { selectingDriver = 1; showBottomSheet = true }
                ))
                Box(modifier = Modifier.weight(1f).fillMaxWidth().clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = { selectingDriver = 2; showBottomSheet = true }
                ))
            }

            // Pulsante Indietro (posizionato alla fine per stare sopra agli overlay cliccabili)
            Box(modifier = Modifier.align(Alignment.TopStart).padding(top = 16.dp, start = 8.dp)) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Indietro",
                        tint = Color.White
                    )
                }
            }
        }

        // --- 3. TABS ---
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = Color.White,
            divider = { HorizontalDivider(color = Color.White.copy(alpha = 0.1f)) },
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(tabPositions[selectedTab]), color = Color(0xFF00FFCC))
                }
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null,
                            onClick = { selectedTab = index }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(title, fontWeight = FontWeight.Bold, color = if (selectedTab == index) Color.White else Color.White.copy(alpha = 0.6f))
                }
            }
        }

        // --- 4. ROWS DI CONFRONTO ---
        Box(modifier = Modifier.weight(1f).padding(horizontal = 20.dp)) {
            if (currentDriver2Id == null) {
                Text("Seleziona un pilota per visualizzare il confronto.", color = Color.White.copy(alpha = 0.4f), modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(contentPadding = PaddingValues(vertical = 16.dp)) {
                    if (selectedTab == 1 && d1Career != null && d2Career != null) {
                        item { StatComparisonRow("GARE DISPUTATE", d1Career!!.total_races.toString(), d2Career!!.total_races.toString(), true, color1, color2) }
                        item { StatComparisonRow("VITTORIE", d1Career!!.wins.toString(), d2Career!!.wins.toString(), true, color1, color2) }
                        item { StatComparisonRow("PODI", d1Career!!.podiums.toString(), d2Career!!.podiums.toString(), true, color1, color2) }
                        item { StatComparisonRow("POLE POSITIONS", d1Career!!.pole_positions.toString(), d2Career!!.pole_positions.toString(), true, color1, color2) }
                        item { StatComparisonRow("MONDIALI", d1Career!!.world_championships.toString(), d2Career!!.world_championships.toString(), true, color1, color2) }
                        item { StatComparisonRow("HAT TRICKS", d1Career!!.hat_tricks.toString(), d2Career!!.hat_tricks.toString(), true, color1, color2) }
                        item { StatComparisonRow("MIGLIOR GARA", d1Career!!.best_race_result, d2Career!!.best_race_result, false, color1, color2) }
                        item { StatComparisonRow("RITIRI", d1Career!!.dnf_count.toString(), d2Career!!.dnf_count.toString(), false, color1, color2) }
                    } else if (selectedTab == 0 && d1Season != null && d2Season != null) {
                        item { StatComparisonRow("PUNTI", driver1Standing?.points?.toString() ?: "0", driver2Standing?.points?.toString() ?: "0", true, color1, color2) }
                        item { StatComparisonRow("VITTORIE", d1Season!!.wins.toString(), d2Season!!.wins.toString(), true, color1, color2) }
                        item { StatComparisonRow("PODI", d1Season!!.podiums.toString(), d2Season!!.podiums.toString(), true, color1, color2) }
                        item { StatComparisonRow("POLE", d1Season!!.pole_positions.toString(), d2Season!!.pole_positions.toString(), true, color1, color2) }
                        item { StatComparisonRow("TESTA A TESTA GARA", d1Season!!.beat_teammate_race.toString(), d2Season!!.beat_teammate_race.toString(), true, color1, color2) }
                        item { StatComparisonRow("Q3", d1Season!!.q3_appearances.toString(), d2Season!!.q3_appearances.toString(), true, color1, color2) }
                        item { StatComparisonRow("GIRI AL COMANDO", d1Season!!.laps_led.toString(), d2Season!!.laps_led.toString(), true, color1, color2) }
                        item { StatComparisonRow("RITIRI", d1Season!!.retirements.toString(), d2Season!!.retirements.toString(), false, color1, color2) }
                    }
                }
            }
        }
    }

    // --- 5. BOTTOM SHEET (SELETTORE SFIDANTE) ---
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            containerColor = Color(0xFF1E1E1E)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(if (selectingDriver == 1) "Seleziona Pilota 1" else "Seleziona Pilota 2", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                LazyColumn {
                    val otherDriverId = if (selectingDriver == 1) currentDriver2Id else currentDriver1Id
                    items(standings.filter { F1Utils.getDriverIdFromName(it.driver_name) != otherDriverId }) { standing ->
                        val teamCol = F1Utils.getTeamColor(standing.constructor_name)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    if (selectingDriver == 1) {
                                        currentDriver1Id = F1Utils.getDriverIdFromName(standing.driver_name)
                                    } else {
                                        currentDriver2Id = F1Utils.getDriverIdFromName(standing.driver_name)
                                    }
                                    showBottomSheet = false 
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(color = teamCol, shape = CircleShape, modifier = Modifier.size(12.dp)) {}
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(standing.driver_name.uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                            Text(standing.constructor_name, color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeadToHeadConstructorScreen(
    constructor1Id: String,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val database = remember { FormulaDatabase.getDatabase(context) }
    val repository = remember { FormulaRepository(database) }

    var currentConstructor1Id by remember { mutableStateOf(constructor1Id) }
    var currentConstructor2Id by remember { mutableStateOf<String?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectingConstructor by remember { mutableStateOf(2) }

    // --- 1. GESTIONE REATTIVA DEI DATI (FLOWS) ---
    val standings by repository.constructorStandings.collectAsState(initial = emptyList())
    
    val c1Career by remember(currentConstructor1Id) { repository.getConstructorStats(currentConstructor1Id) }.collectAsState(initial = null)
    val c1Season by remember(currentConstructor1Id) { repository.getConstructorSeasonStats(currentConstructor1Id) }.collectAsState(initial = null)
    
    val c2CareerFlow = remember(currentConstructor2Id) { currentConstructor2Id?.let { repository.getConstructorStats(it) } ?: flowOf(null) }
    val c2Career by c2CareerFlow.collectAsState(initial = null)

    val c2SeasonFlow = remember(currentConstructor2Id) { currentConstructor2Id?.let { repository.getConstructorSeasonStats(it) } ?: flowOf(null) }
    val c2Season by c2SeasonFlow.collectAsState(initial = null)

    val constructor1Standing = standings.find { getConstructorIdForStats(it.constructor_name) == currentConstructor1Id }
    val constructor2Standing = currentConstructor2Id?.let { id -> standings.find { getConstructorIdForStats(it.constructor_name) == id } }

    val color1 = F1Utils.getTeamColor(constructor1Standing?.constructor_name)
    val color2 = currentConstructor2Id?.let { F1Utils.getTeamColor(constructor2Standing?.constructor_name) } ?: Color.DarkGray

    val name1 = getConstructorDisplayName(currentConstructor1Id)
    val name2 = currentConstructor2Id?.let { getConstructorDisplayName(it) } ?: "SELEZIONA"

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("STAGIONE 2026", "ALL-TIME")

    LaunchedEffect(currentConstructor1Id) {
        repository.refreshConstructorStats(currentConstructor1Id)
        repository.refreshConstructorSeasonStats(currentConstructor1Id)
    }
    LaunchedEffect(currentConstructor2Id) {
        currentConstructor2Id?.let { 
            repository.refreshConstructorStats(it) 
            repository.refreshConstructorSeasonStats(it)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0F0F0F))) {
        // --- 2. HEADER SPLIT SCREEN STILE "FIGHTING GAME" ---
        Box(modifier = Modifier.fillMaxWidth().height(235.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val hOffset = h * 0.25f

                val path1 = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(w, 0f)
                    lineTo(w, hOffset)
                    lineTo(0f, h - hOffset)
                    close()
                }
                val path2 = Path().apply {
                    moveTo(w, hOffset)
                    lineTo(w, h)
                    lineTo(0f, h)
                    lineTo(0f, h - hOffset)
                    close()
                }
                drawPath(path1, color1.copy(alpha = 0.6f))
                drawPath(path2, color2.copy(alpha = 0.6f))

                drawLine(
                    color = Color.White.copy(alpha = 0.2f),
                    start = androidx.compose.ui.geometry.Offset(w, hOffset),
                    end = androidx.compose.ui.geometry.Offset(0f, h - hOffset),
                    strokeWidth = 3f
                )
            }

            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)))

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val w = constraints.maxWidth.toFloat()
                val h = constraints.maxHeight.toFloat()
                val hOffset = h * 0.25f

                val angle = (kotlin.math.atan2((2 * hOffset - h).toDouble(), w.toDouble()) * (180f / Math.PI)).toFloat()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationZ = angle }
                ) {
                    // Scuderia 1 (Top Left)
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = (-34).dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(constructor1Standing?.chassis_name?.uppercase() ?: "", color = color1, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic, modifier = Modifier.offset(y = 8.dp))
                        Text(name1, color = Color.White, fontSize = 46.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic)
                    }

                    // Scuderia 2 (Bottom Right)
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = 34.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (currentConstructor2Id == null) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(54.dp).graphicsLayer { rotationZ = -angle }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.padding(12.dp))
                            }
                        } else {
                            Text(name2, color = Color.White, fontSize = 46.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic)
                            Text(constructor2Standing?.chassis_name?.uppercase() ?: "", color = color2, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic, modifier = Modifier.offset(y = (-8).dp))
                        }
                    }
                }
            }

            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth().clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = { selectingConstructor = 1; showBottomSheet = true }
                ))
                Box(modifier = Modifier.weight(1f).fillMaxWidth().clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = { selectingConstructor = 2; showBottomSheet = true }
                ))
            }

            Box(modifier = Modifier.align(Alignment.TopStart).padding(top = 16.dp, start = 8.dp)) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Indietro",
                        tint = Color.White
                    )
                }
            }
        }

        // --- 3. TABS ---
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = Color.White,
            divider = { HorizontalDivider(color = Color.White.copy(alpha = 0.1f)) },
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(tabPositions[selectedTab]), color = Color(0xFF00FFCC))
                }
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null,
                            onClick = { selectedTab = index }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(title, fontWeight = FontWeight.Bold, color = if (selectedTab == index) Color.White else Color.White.copy(alpha = 0.6f))
                }
            }
        }

        // --- 4. ROWS DI CONFRONTO ---
        Box(modifier = Modifier.weight(1f).padding(horizontal = 20.dp)) {
            if (currentConstructor2Id == null) {
                Text("Seleziona una scuderia per visualizzare il confronto.", color = Color.White.copy(alpha = 0.4f), modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(contentPadding = PaddingValues(vertical = 16.dp)) {
                    if (selectedTab == 1 && c1Career != null && c2Career != null) {
                        item { StatComparisonRow("STAGIONI", c1Career!!.seasons_entered.toString(), c2Career!!.seasons_entered.toString(), true, color1, color2) }
                        item { StatComparisonRow("GARE DISPUTATE", c1Career!!.total_races.toString(), c2Career!!.total_races.toString(), true, color1, color2) }
                        item { StatComparisonRow("VITTORIE", c1Career!!.wins.toString(), c2Career!!.wins.toString(), true, color1, color2) }
                        item { StatComparisonRow("PODI", c1Career!!.podiums.toString(), c2Career!!.podiums.toString(), true, color1, color2) }
                        item { StatComparisonRow("POLE POSITIONS", c1Career!!.pole_positions.toString(), c2Career!!.pole_positions.toString(), true, color1, color2) }
                        item { StatComparisonRow("MONDIALI PILOTI", c1Career!!.driver_championships.toString(), c2Career!!.driver_championships.toString(), true, color1, color2) }
                        item { StatComparisonRow("MONDIALI COSTRUTTORI", c1Career!!.constructor_championships.toString(), c2Career!!.constructor_championships.toString(), true, color1, color2) }
                        item { StatComparisonRow("MIGLIOR GARA", c1Career!!.best_race_result, c2Career!!.best_race_result, false, color1, color2) }
                    } else if (selectedTab == 0 && c1Season != null && c2Season != null) {
                        item { StatComparisonRow("PUNTI", constructor1Standing?.points?.toString() ?: "0", constructor2Standing?.points?.toString() ?: "0", true, color1, color2) }
                        item { StatComparisonRow("VITTORIE", c1Season!!.wins.toString(), c2Season!!.wins.toString(), true, color1, color2) }
                        item { StatComparisonRow("PODI", c1Season!!.podiums.toString(), c2Season!!.podiums.toString(), true, color1, color2) }
                        item { StatComparisonRow("POLE POSITIONS", c1Season!!.pole_positions.toString(), c2Season!!.pole_positions.toString(), true, color1, color2) }
                        item { StatComparisonRow("PRIME FILE", c1Season!!.front_rows.toString(), c2Season!!.front_rows.toString(), true, color1, color2) }
                        item { StatComparisonRow("DOPPIETTE (1-2)", c1Season!!.one_two_finishes.toString(), c2Season!!.one_two_finishes.toString(), true, color1, color2) }
                        item { StatComparisonRow("DOPPIO Q3", c1Season!!.double_q3.toString(), c2Season!!.double_q3.toString(), true, color1, color2) }
                        item { StatComparisonRow("RITIRI", c1Season!!.retirements.toString(), c2Season!!.retirements.toString(), false, color1, color2) }
                    }
                }
            }
        }
    }

    // --- 5. BOTTOM SHEET (SELETTORE SFIDANTE) ---
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            containerColor = Color(0xFF1E1E1E)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(if (selectingConstructor == 1) "Seleziona Scuderia 1" else "Seleziona Scuderia 2", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                LazyColumn {
                    val otherConstructorId = if (selectingConstructor == 1) currentConstructor2Id else currentConstructor1Id
                    items(standings.filter { getConstructorIdForStats(it.constructor_name) != otherConstructorId }) { standing ->
                        val teamCol = F1Utils.getTeamColor(standing.constructor_name)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    if (selectingConstructor == 1) {
                                        currentConstructor1Id = getConstructorIdForStats(standing.constructor_name)
                                    } else {
                                        currentConstructor2Id = getConstructorIdForStats(standing.constructor_name)
                                    }
                                    showBottomSheet = false 
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(color = teamCol, shape = CircleShape, modifier = Modifier.size(12.dp)) {}
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(getConstructorDisplayName(getConstructorIdForStats(standing.constructor_name)), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                            Text(standing.chassis_name ?: "", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatComparisonRow(label: String, val1: String, val2: String, isHigherBetter: Boolean, color1: Color, color2: Color) {
    val v1 = val1.toFloatOrNull()
    val v2 = val2.toFloatOrNull()
    var c1 = Color.White
    var c2 = Color.White
    
    if (v1 != null && v2 != null) {
        if (v1 > v2) {
            c1 = if (isHigherBetter) color1 else Color.Gray
            c2 = if (isHigherBetter) Color.Gray else color2
        } else if (v2 > v1) {
            c1 = if (isHigherBetter) Color.Gray else color1
            c2 = if (isHigherBetter) color2 else Color.Gray
        } else {
            // Pareggio: coloriamo entrambi i numeri col colore della loro scuderia!
            c1 = color1
            c2 = color2
        }
    }

    // Padding asimmetrico: molto spazio sopra per distaccarsi dalla riga precedente, pochissimo sotto per restare incollati alla propria barra
    Column(modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 2.dp)) {
        Text(
            text = label, 
            color = Color.White.copy(alpha = 0.5f), 
            fontSize = 14.sp, 
            fontWeight = FontWeight.ExtraBold, 
            modifier = Modifier.fillMaxWidth(), 
            textAlign = TextAlign.Center
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text = val1, color = c1, fontSize = 22.sp, fontWeight = FontWeight.Black, modifier = Modifier.widthIn(min = 40.dp), textAlign = TextAlign.Start)
            
            if (v1 != null && v2 != null) {
                val realW1: Float
                val realW2: Float
                if (isHigherBetter) {
                    val maxVal = maxOf(v1, v2)
                    realW1 = if (maxVal > 0) v1 / maxVal else 0f
                    realW2 = if (maxVal > 0) v2 / maxVal else 0f
                } else {
                    // Logica Inversa: se il numero minore è meglio (es. posizione in gara), la barra più lunga va al migliore.
                    if (v1 == 0f && v2 == 0f) { realW1 = 0f; realW2 = 0f }
                    else if (v1 == 0f) { realW1 = 0f; realW2 = 1f }
                    else if (v2 == 0f) { realW1 = 1f; realW2 = 0f }
                    else { realW1 = if (v1 <= v2) 1f else v2 / v1; realW2 = if (v2 <= v1) 1f else v1 / v2 }
                }
                
                // Applicazione logica visuale: pallino minimo del 6% per lo "0" e boost proporzionale per valori piccoli
                val w1 = if (realW1 > 0f) maxOf(realW1.toDouble().pow(0.65).toFloat(), 0.12f) else 0.06f
                val w2 = if (realW2 > 0f) maxOf(realW2.toDouble().pow(0.65).toFloat(), 0.12f) else 0.06f

                Row(modifier = Modifier.weight(1f).padding(horizontal = 16.dp).height(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Barra divergente a SINISTRA
                    Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.CenterEnd) {
                        Box(modifier = Modifier.fillMaxWidth(w1).fillMaxHeight().background(color1, CircleShape))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    // Barra divergente a DESTRA
                    Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.CenterStart) {
                        Box(modifier = Modifier.fillMaxWidth(w2).fillMaxHeight().background(color2, CircleShape))
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
            
            Text(text = val2, color = c2, fontSize = 22.sp, fontWeight = FontWeight.Black, modifier = Modifier.widthIn(min = 40.dp), textAlign = TextAlign.End)
        }
    }
}