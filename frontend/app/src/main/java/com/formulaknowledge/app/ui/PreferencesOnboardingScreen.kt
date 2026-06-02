package com.formulaknowledge.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.formulaknowledge.app.utils.F1Utils
import kotlinx.coroutines.launch

// Liste fisse per la UI di Onboarding
private val availableDrivers = listOf(
    "max_verstappen" to "Max Verstappen", "hadjar" to "Isack Hadjar",
    "leclerc" to "Charles Leclerc", "hamilton" to "Lewis Hamilton",
    "norris" to "Lando Norris", "piastri" to "Oscar Piastri",
    "russell" to "George Russell", "antonelli" to "Kimi Antonelli",
    "alonso" to "Fernando Alonso", "stroll" to "Lance Stroll",
    "sainz" to "Carlos Sainz", "albon" to "Alexander Albon",
    "gasly" to "Pierre Gasly", "colapinto" to "Franco Colapinto",
    "arvid_lindblad" to "Arvid Lindblad", "lawson" to "Liam Lawson",
    "hulkenberg" to "Nico Hülkenberg", "bortoleto" to "Gabriel Bortoleto",
    "ocon" to "Esteban Ocon", "bearman" to "Oliver Bearman",
    "perez" to "Sergio Pérez", "bottas" to "Valtteri Bottas"
)

private val availableConstructors = listOf(
    "ferrari" to "Ferrari", "mclaren" to "McLaren",
    "mercedes" to "Mercedes", "red_bull" to "Red Bull",
    "aston_martin" to "Aston Martin", "alpine" to "Alpine",
    "williams" to "Williams", "rb" to "Racing Bulls",
    "audi" to "Audi", "haas" to "Haas", "cadillac" to "Cadillac"
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PreferencesOnboardingScreen(
    onSkip: () -> Unit,
    onSave: (String?, String?, String?) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()

    var selectedDriver1 by remember { mutableStateOf<String?>(null) }
    var selectedDriver2 by remember { mutableStateOf<String?>(null) }
    var selectedConstructor by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackgroundGradientColor)
            .systemBarsPadding()
    ) {
        // Tasto SKP (X)
        IconButton(
            onClick = { onSkip() },
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Salta", tint = Color.White.copy(alpha = 0.5f))
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(40.dp))
            
            // Header Testo
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                val title = when (pagerState.currentPage) {
                    0 -> "CHI TI FA BATTERE\nIL CUORE?"
                    1 -> "E SE IL PRIMO\nSI RITIRA?"
                    else -> "PER QUALE BANDIERA\nFAI IL TIFO?"
                }
                val subtitle = when (pagerState.currentPage) {
                    0 -> "Scegli il tuo 1° Pilota preferito."
                    1 -> "Scegli il tuo 2° Pilota preferito."
                    else -> "Seleziona la tua Scuderia del cuore."
                }

                Text(text = title, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, lineHeight = 34.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = subtitle, color = Color(0xFF00FFCC), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Pager Scorrevole
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                when (page) {
                    0 -> SelectionGrid(
                        items = availableDrivers,
                        selectedId = selectedDriver1,
                        onSelect = { selectedDriver1 = it; if(selectedDriver2 == it) selectedDriver2 = null }
                    )
                    1 -> SelectionGrid(
                        items = availableDrivers.filter { it.first != selectedDriver1 }, // Esclude il pilota 1
                        selectedId = selectedDriver2,
                        onSelect = { selectedDriver2 = it }
                    )
                    2 -> SelectionGrid(
                        items = availableConstructors,
                        selectedId = selectedConstructor,
                        isConstructor = true,
                        onSelect = { selectedConstructor = it }
                    )
                }
            }

            // Footer e Controlli
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Indicatori di pagina (Puntini)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(3) { index ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 10.dp else 8.dp)
                                .background(if (isSelected) Color(0xFF00FFCC) else Color.White.copy(alpha = 0.2f), CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Pulsante AVANTI / SALVA
                val isLastPage = pagerState.currentPage == 2
                val buttonText = if (isLastPage) "SALVA PREFERENZE" else "AVANTI"
                
                Button(
                    onClick = {
                        if (isLastPage) {
                            onSave(selectedDriver1, selectedDriver2, selectedConstructor)
                        } else {
                            coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC))
                ) {
                    Text(buttonText, color = Color.Black, fontWeight = FontWeight.Black, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun SelectionGrid(
    items: List<Pair<String, String>>,
    selectedId: String?,
    isConstructor: Boolean = false,
    onSelect: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(if (isConstructor) 2 else 3),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(items) { (id, name) ->
            val isSelected = id == selectedId
            val teamColor = if (isConstructor) F1Utils.getTeamColor(name) else Color.Transparent
            
            val bgColor = if (isSelected) {
                if (isConstructor) teamColor.copy(alpha = 0.3f) else Color(0xFF00FFCC).copy(alpha = 0.2f)
            } else {
                Color.White.copy(alpha = 0.05f)
            }
            
            val borderColor = if (isSelected) {
                if (isConstructor) teamColor else Color(0xFF00FFCC)
            } else {
                Color.White.copy(alpha = 0.1f)
            }

            Surface(
                modifier = Modifier
                    .aspectRatio(if (isConstructor) 1.5f else 1f)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onSelect(id) },
                color = bgColor,
                border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (isConstructor) {
                            // Pallino colore per la scuderia
                            Box(modifier = Modifier.size(12.dp).background(teamColor, CircleShape))
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        
                        Text(
                            text = name.uppercase().replace(" ", "\n"),
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                            fontSize = if (isConstructor) 16.sp else 12.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            lineHeight = 14.sp
                        )
                    }

                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selezionato",
                            tint = if (isConstructor) Color.White else Color(0xFF00FFCC),
                            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(16.dp)
                        )
                    }
                }
            }
        }
    }
}