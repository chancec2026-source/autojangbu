package kr.chance.autojangbu.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import kr.chance.autojangbu.R

// 시안 A '나이트 클러스터': 밤의 계기판처럼 어두운 바탕. 일반 업무는 하늘색, 출퇴근은 파랑, 확인할 것은 경고등 노랑.
object Palette {
    val bg = Color(0xFF0A1119)
    val glow = Color(0xFF152233)
    val panel = Color(0xFF101A25)
    val panelTop = Color(0xFF111C28)
    val panelBottom = Color(0xFF0C151F)
    val raised = Color(0xFF16222F)
    val dayHead = Color(0xFF0D1620)
    val line = Color(0xFF1C2835)
    val line2 = Color(0xFF283748)
    val text = Color(0xFFEAF0F6)
    val sub = Color(0xFF93A1B2)
    val faint = Color(0xFF5C6A7B)
    val biz = Color(0xFF61CDFF)
    val com = Color(0xFF3A80E4)
    val per = Color(0xFF4A5A6E)
    val amber = Color(0xFFF2B54A)
    val onBiz = Color(0xFF03131F)
    val plate = Color(0xFFF4F6F8)
    val plateInk = Color(0xFF0B1118)
}

/** 한글: SUIT. 앱에 넣어 둔 글꼴(OFL, assets/licenses). */
val Suit = FontFamily(
    Font(R.font.suit_regular, FontWeight.Normal),
    Font(R.font.suit_medium, FontWeight.Medium),
    Font(R.font.suit_semibold, FontWeight.SemiBold),
    Font(R.font.suit_bold, FontWeight.Bold),
    Font(R.font.suit_extrabold, FontWeight.ExtraBold),
)

/** 숫자: Barlow Semi Condensed. 도로 표지·번호판에서 온 글꼴이라 계기판 숫자와 잘 맞는다. */
val Barlow = FontFamily(
    Font(R.font.barlow_sc_medium, FontWeight.Medium),
    Font(R.font.barlow_sc_semibold, FontWeight.SemiBold),
    Font(R.font.barlow_sc_bold, FontWeight.Bold),
)

// 한글이 어절 중간에서 끊기지 않게 한다.
private val korean = LineBreak(LineBreak.Strategy.Balanced, LineBreak.Strictness.Normal, LineBreak.WordBreak.Phrase)

/** 한글 글자 모양. size 는 sp. 색을 비우면 둘레의 글자색(LocalContentColor)을 따른다. */
fun kr(size: Int, weight: FontWeight = FontWeight.Normal, color: Color = Color.Unspecified) = kr(size.sp, weight, color)

fun kr(size: TextUnit, weight: FontWeight = FontWeight.Normal, color: Color = Color.Unspecified) =
    TextStyle(fontFamily = Suit, fontSize = size, lineHeight = size * 1.35f, fontWeight = weight, color = color, lineBreak = korean)

/** 숫자 글자 모양. 자릿수가 흔들리지 않게 고정폭 숫자(tnum)를 쓴다. */
fun num(size: Int, weight: FontWeight = FontWeight.SemiBold, color: Color = Color.Unspecified) = num(size.sp, weight, color)

fun num(size: TextUnit, weight: FontWeight = FontWeight.SemiBold, color: Color = Color.Unspecified) =
    TextStyle(fontFamily = Barlow, fontSize = size, lineHeight = size * 1.15f, fontWeight = weight, color = color, fontFeatureSettings = "tnum")

@Composable
fun AutoJangbuTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Palette.biz,
            onPrimary = Palette.onBiz,
            background = Palette.bg,
            onBackground = Palette.text,
            surface = Palette.raised,
            onSurface = Palette.text,
            surfaceVariant = Palette.raised,
            onSurfaceVariant = Palette.sub,
            surfaceContainer = Palette.raised,
            surfaceContainerHigh = Palette.raised,
            surfaceContainerHighest = Palette.raised,
            outline = Palette.line2,
            outlineVariant = Palette.line,
        ),
        typography = Typography(
            displaySmall = kr(44, FontWeight.Bold),
            headlineSmall = kr(30, FontWeight.Bold),
            titleLarge = kr(26, FontWeight.Bold),
            bodyLarge = kr(24),
            bodyMedium = kr(21),
            bodySmall = kr(19),
            labelLarge = kr(22, FontWeight.Bold),
        ),
        content = content,
    )
}
