package com.formulaknowledge.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsMotorsports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.formulaknowledge.app.utils.F1Utils

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

@Composable
fun PreferencesOnboardingScreen(
    isLoading: Boolean,
    onSkip: () -> Unit,
    onSave: (String?, String?, String?) -> Unit
) {
    var selectedDriver1 by remember { mutableStateOf<String?>(null) }
    var selectedDriver2 by remember { mutableStateOf<String?>(null) }
    var selectedConstructor by remember { mutableStateOf<String?>(null) }

    // 0 = Scuderia, 1 = Pilota 1, 2 = Pilota 2
    var activeTab by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackgroundGradientColor)
            .systemBarsPadding()
    ) {
        // Tasto SKP (X)
        IconButton(
            onClick = { if (!isLoading) onSkip() },
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Salta", tint = Color.White.copy(alpha = 0.5f))
        }

        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(70.dp))
            
            // Header Testo
            Text(
                text = "FAI LE TUE SCELTE!", 
                color = Color.White, 
                fontSize = 34.sp, 
                fontWeight = FontWeight.Black, 
                fontStyle = FontStyle.Italic
            )
            
            Spacer(modifier = Modifier.height(20.dp))

            // 3 CARDS SUPERIORI
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SelectionCategoryCard(
                    title = "SCUDERIA",
                    selectedId = selectedConstructor,
                    isConstructor = true,
                    isActive = activeTab == 0,
                    onClick = { activeTab = 0 },
                    modifier = Modifier.weight(1f)
                )
                SelectionCategoryCard(
                    title = "1° PILOTA",
                    selectedId = selectedDriver1,
                    isConstructor = false,
                    isActive = activeTab == 1,
                    onClick = { activeTab = 1 },
                    modifier = Modifier.weight(1f)
                )
                SelectionCategoryCard(
                    title = "2° PILOTA",
                    selectedId = selectedDriver2,
                    isConstructor = false,
                    isActive = activeTab == 2,
                    onClick = { activeTab = 2 },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(26.dp))

            // GRIGLIA SELEZIONABILE CENTRALE
            Box(modifier = Modifier.weight(1f)) {
                when (activeTab) {
                    0 -> SelectionGrid(
                        items = availableConstructors,
                        selectedId = selectedConstructor,
                        isConstructor = true,
                        onSelect = { selectedConstructor = it; activeTab = 1 } // Salta al Pilota 1 in automatico
                    )
                    1 -> SelectionGrid(
                        items = availableDrivers,
                        selectedId = selectedDriver1,
                        isConstructor = false,
                        onSelect = { selectedDriver1 = it; if(selectedDriver2 == it) selectedDriver2 = null; activeTab = 2 } // Salta al Pilota 2 in automatico
                    )
                    2 -> SelectionGrid(
                        items = availableDrivers.filter { it.first != selectedDriver1 }, // Nascondi il pilota 1
                        selectedId = selectedDriver2,
                        isConstructor = false,
                        onSelect = { selectedDriver2 = it }
                    )
                }
            }

            // Footer e Controlli
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp)
            ) {
                Button(
                    onClick = { if (!isLoading) onSave(selectedDriver1, selectedDriver2, selectedConstructor) },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                    } else {
                        Text("SALVA PREFERENZE", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun SelectionCategoryCard(title: String, selectedId: String?, isConstructor: Boolean, isActive: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    // Animazione di Zoom (1.0f normale, 1.08f se attiva)
    val scale by animateFloatAsState(targetValue = if (isActive) 1.08f else 1.0f, label = "ScaleAnim")
    val borderColor = if (isActive) Color(0xFF00FFCC) else Color.White.copy(alpha = 0.1f)
    
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .scale(scale)
                .clickable { onClick() },
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1E0A0A).copy(alpha = if (isActive) 0.95f else 0.60f),
            border = BorderStroke(if (isActive) 2.dp else 1.dp, borderColor)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                // FIX 1: Background Radial perfetto che copre tutti gli angoli (usando drawRect al posto di drawCircle)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF00FFCC).copy(alpha = if (isActive) 0.35f else 0.05f), Color.Transparent),
                            center = Offset(size.width / 2, size.height / 2),
                            radius = size.width * 1.2f 
                        )
                    )
                }
                
                // FIX 3: Mostra il logo effettivo nella Top Card se è stato selezionato
                if (selectedId != null) {
                    val resourcePrefix = if (isConstructor) "team_" else "driver_"
                    val resourceName = "$resourcePrefix$selectedId"
                    val context = LocalContext.current
                    val resourceId = remember(resourceName) {
                        context.resources.getIdentifier(resourceName, "drawable", context.packageName)
                    }
                    
                    if (resourceId != 0) {
                        Image(
                            painter = painterResource(id = resourceId),
                            contentDescription = title,
                            modifier = Modifier.size(if (isConstructor) 54.dp else 48.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        val displayName = if (isConstructor) selectedId else selectedId.split("_").last()
                        Text(text = displayName.uppercase(), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(8.dp))
                    }
                } else {
                    Text(text = "-", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = title,
            color = Color.White.copy(alpha = if (isActive) 1f else 0.5f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun SelectionGrid(
    items: List<Pair<String, String>>,
    selectedId: String?,
    isConstructor: Boolean = false,
    onSelect: (String) -> Unit
) {
    val context = LocalContext.current
    LazyVerticalGrid(
        columns = GridCells.Fixed(if (isConstructor) 4 else 5), // Diminuzione drastica!
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(items) { (id, name) ->
            val isSelected = id == selectedId
            val teamColor = if (isConstructor) F1Utils.getTeamColor(name) else Color.Transparent
            
            val bgColor = if (isSelected) {
                if (isConstructor) Color(0xFF1E0A0A) else Color(0xFF00FFCC).copy(alpha = 0.2f)
            } else {
                if (isConstructor) Color(0xFF1E0A0A) else Color.White.copy(alpha = 0.05f)
            }
            
            val borderColor = if (isSelected) {
                if (isConstructor) teamColor else Color(0xFF00FFCC)
            } else {
                if (isConstructor) teamColor.copy(alpha = 0.3f) else Color.Transparent
            }

            // FIX 4: La griglia ora applica il RoundedCornerShape alla Surface, unendo colore e bordo!
            Surface(
                modifier = Modifier
                    .aspectRatio(1f) // Quadrate per tutti
                    .clickable { onSelect(id) },
                shape = RoundedCornerShape(12.dp),
                color = bgColor,
                border = BorderStroke(if (isSelected) 2.dp else (if (isConstructor) 0.5.dp else 0.dp), borderColor)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Effetto Luce Colorata Radiale Dietro al Logo del Team!
                    if (isConstructor) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(teamColor.copy(alpha = if(isSelected) 0.5f else 0.15f), Color.Transparent),
                                    center = Offset(size.width / 2, size.height / 2),
                                    radius = size.width * 0.8f
                                )
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxSize().padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // RICERCA DINAMICA ICONE
                        val resourcePrefix = if (isConstructor) "team_" else "driver_"
                        val resourceName = "$resourcePrefix$id"
                        val resourceId = remember(resourceName) {
                            context.resources.getIdentifier(resourceName, "drawable", context.packageName)
                        }

                        if (resourceId != 0) {
                            Image(
                                painter = painterResource(id = resourceId),
                                contentDescription = name,
                                modifier = Modifier.size(if (isConstructor) 42.dp else 24.dp), // Loghi molto più grandi!
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            // Fallback se l'icona non esiste ancora
                            if (isConstructor) {
                                Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = if (teamColor != Color.Transparent) teamColor else Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Icon(Icons.Default.SportsMotorsports, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            val displayName = if (isConstructor) name else name.split(" ").last()
                            Text(
                                text = displayName.uppercase(),
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                                fontSize = if (isConstructor) 9.sp else 8.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                lineHeight = 9.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selezionato",
                            tint = if (isConstructor) Color.White else Color(0xFF00FFCC),
                            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(12.dp)
                        )
                    }
                }
            }
        }
    }
}