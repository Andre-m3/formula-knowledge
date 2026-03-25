package com.formulaknowledge.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 1. Definizione della Palette Dark Morbida
val AppDarkBackground = Color(0xFF1C1C1E)
val AppDarkSurface = Color(0xFF2C2C2E)
val TextPrimary = Color(0xFFE0E0E0)

// Sfondo a gradiente per un look più moderno e profondo
val AppBackgroundGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF1A1A1D), Color(0xFF000000))
)

// 2. Colori Dinamici delle Scuderie
enum class FavoriteTeam(val accentColor: Color) {
    FERRARI(Color(0xFFE32219)),
    MCLAREN(Color(0xFFFF8000)),
    MERCEDES(Color(0xFF00D2BE)),
    REDBULL(Color(0xFF1B2B5A))
}

@Composable
fun DynamicF1Theme(
    favoriteTeam: FavoriteTeam,
    content: @Composable () -> Unit
) {
    // Il colore primario dell'app diventa il colore del team
    val colorScheme = darkColorScheme(
        primary = favoriteTeam.accentColor,
        background = AppDarkBackground,
        surface = AppDarkSurface,
        onPrimary = Color.White,
        onBackground = TextPrimary,
        onSurface = TextPrimary
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}

// 3. UI Component "Family-Friendly" per la RaceWeek
@Composable
fun RaceWeekCard(team: FavoriteTeam) {
    DynamicF1Theme(favoriteTeam = team) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp), // Bordi molto arrotondati
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Prossima Gara",
                    color = MaterialTheme.colorScheme.primary, // Colore dinamico!
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Gran Premio d'Italia",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp
                )
                Text(
                    text = "Round 16 • Monza",
                    color = Color.Gray,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { /* Naviga ai dettagli */ },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Vedi Orari e Meteo")
                }
            }
        }
    }
}

// 5. UI Component: Card Dashboard generica (Meteo, Sessioni, ecc.)
@Composable
fun GlassDashboardCard(
    title: String,
    subtitle: String,
    accentColor: Color = Color.White,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .padding(8.dp)
            .height(130.dp) // Altezza fissa più grande
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)), // Vetro super trasparente (stile iOS)
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f)) // Bordo sottilissimo e riflettente
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp), // Padding ridotto per sfruttare meglio lo spazio
            verticalArrangement = Arrangement.SpaceBetween // Ancora titolo in alto e sottotitolo in basso
        ) {
            Text(
                text = title,
                color = accentColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = Color.White,
                fontSize = 22.sp, // Testo più grande
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 22.sp
            )
        }
    }
}

// 4. UI Component: Card stile NEON/GLASS per gli Aggiornamenti
@Composable
fun TeamUpdateCard(teamName: String, teamColorHex: String, onClick: () -> Unit) {
    val teamColor = try {
        Color(android.graphics.Color.parseColor(teamColorHex))
    } catch (e: Exception) {
        Color.Gray // Fallback in caso di errore
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp), // Angoli molto arrotondati
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)), // Vetro super trasparente
        border = androidx.compose.foundation.BorderStroke(0.5.dp, teamColor.copy(alpha = 0.4f)) // Bordo sottile Neon
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(
                text = teamName,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp
            )
            Text(
                text = "Scopri →",
                color = teamColor, // Freccia Neon
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}