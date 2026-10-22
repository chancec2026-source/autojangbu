package kr.chance.autojangbu.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.sp

// 보고서 화면과 같은 '도로 표지판' 색: 표지판 초록 + 차선 노랑 + 아스팔트 회색
object Palette {
    val paper = Color(0xFFF1F3EF)
    val surface = Color(0xFFFBFCFA)
    val ink = Color(0xFF16201A)
    val muted = Color(0xFF56635B)
    val line = Color(0xFFD3D9D3)
    val sign = Color(0xFF0B6A45)
    val signSoft = Color(0xFFE2EEE7)
    val lane = Color(0xFFC98E00)
    val laneSoft = Color(0xFFF6EBCB)
}

// 한글이 어절 중간에서 끊기지 않게 한다.
private val korean = LineBreak(LineBreak.Strategy.Balanced, LineBreak.Strictness.Normal, LineBreak.WordBreak.Phrase)

private fun style(size: Int, weight: androidx.compose.ui.text.font.FontWeight) =
    TextStyle(fontSize = size.sp, lineHeight = (size * 1.35).sp, fontWeight = weight, lineBreak = korean)

@Composable
fun AutoJangbuTheme(content: @Composable () -> Unit) {
    val bold = androidx.compose.ui.text.font.FontWeight.Bold
    val normal = androidx.compose.ui.text.font.FontWeight.Normal
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Palette.sign,
            onPrimary = Color.White,
            background = Palette.paper,
            surface = Palette.surface,
            onSurface = Palette.ink,
            onBackground = Palette.ink,
        ),
        typography = Typography(
            displaySmall = style(44, bold),
            headlineSmall = style(30, bold),
            titleLarge = style(26, bold),
            bodyLarge = style(24, normal),
            bodyMedium = style(21, normal),
            labelLarge = style(22, bold),
        ),
        content = content,
    )
}
