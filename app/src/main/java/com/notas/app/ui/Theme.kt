package com.notas.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Color(0xFF5B4BDB),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE6E2FF),
    onPrimaryContainer = Color(0xFF1B0F6B),
    secondary = Color(0xFFF5A623),
    onSecondary = Color(0xFF3B2600),
    secondaryContainer = Color(0xFFFFE8C2),
    onSecondaryContainer = Color(0xFF3B2600),
    background = Color(0xFFFAF7F2),
    onBackground = Color(0xFF1C1B22),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1B22),
    surfaceVariant = Color(0xFFF0ECE4),
    onSurfaceVariant = Color(0xFF6B6878),
    outline = Color(0xFFD9D4C9),
    error = Color(0xFFD64545)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB3A9FF),
    onPrimary = Color(0xFF1B0F6B),
    primaryContainer = Color(0xFF3A2FA8),
    onPrimaryContainer = Color(0xFFE6E2FF),
    secondary = Color(0xFFFFC565),
    onSecondary = Color(0xFF3B2600),
    secondaryContainer = Color(0xFF5A3F0A),
    onSecondaryContainer = Color(0xFFFFE8C2),
    background = Color(0xFF121218),
    onBackground = Color(0xFFEDEBF5),
    surface = Color(0xFF1C1C25),
    onSurface = Color(0xFFEDEBF5),
    surfaceVariant = Color(0xFF272733),
    onSurfaceVariant = Color(0xFFA9A6B8),
    outline = Color(0xFF3A3A48),
    error = Color(0xFFFF8A8A)
)

private val AnotaTypography = Typography(
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp, letterSpacing = (-0.5).sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.3.sp)
)

private val AnotaShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

private val CategoryPalette = listOf(
    Color(0xFF7C6CF0), // violeta
    Color(0xFFF2994A), // laranja
    Color(0xFF27AE60), // verde
    Color(0xFFEB5757), // vermelho
    Color(0xFF2D9CDB), // azul
    Color(0xFFBB6BD9), // lilás
    Color(0xFFE0B100), // amarelo
    Color(0xFF00A896)  // turquesa
)

fun categoryColor(index: Int): Color = CategoryPalette[index.mod(CategoryPalette.size)]

@Composable
fun AnotaTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AnotaTypography,
        shapes = AnotaShapes,
        content = content
    )
}
