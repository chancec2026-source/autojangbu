package kr.chance.autojangbu.ui

import android.graphics.BlurMaskFilter
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import java.time.YearMonth
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kr.chance.autojangbu.data.MonthSummary

/**
 * 이번 달 업무사용비율 계기판. 240° 눈금 위에 일반 업무(하늘색)와 출퇴근(파랑)을 잇고,
 * 바늘은 업무 사용이 끝나는 곳(=비율)을 가리킨다. 남은 회색 칸이 개인 운행이다.
 * 시안(700×452)의 비율을 그대로 키우고 줄인다.
 */
@Composable
fun RatioGauge(month: YearMonth, s: MonthSummary, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(28.dp)
    val measurer = rememberTextMeasurer()
    val b = if (s.totalKm > 0) (s.businessKm / s.totalKm).toFloat().coerceIn(0f, 1f) else 0f
    val u = s.businessUseRatio.toFloat().coerceIn(0f, 1f)
    val personalKm = (s.totalKm - s.businessUseKm).coerceAtLeast(0.0)
    BoxWithConstraints(
        modifier.panel(shape, Color.Transparent)
            .background(Brush.verticalGradient(listOf(Palette.panelTop, Palette.panelBottom)), shape)
    ) {
        // k = 시안 1칸이 몇 dp 인가. 글자도 같은 비율로(글꼴 배율과 상관없이) 맞춘다.
        val k = min(maxWidth.value / 700f, maxHeight.value / 452f)
        val top = (maxHeight.value - 452f * k) / 2f
        val density = LocalDensity.current
        fun sz(v: Float): TextUnit = with(density) { (v * k).dp.toSp() }

        Canvas(Modifier.fillMaxSize()) {
            val p = k * this.density
            val cx = size.width / 2f
            val cy = top * this.density + 232f * p
            val r = 166f * p
            val w = 18f * p
            fun at(v: Float, rr: Float): Offset {
                val a = Math.toRadians(150.0 + 240.0 * v)
                return Offset(cx + rr * cos(a).toFloat(), cy + rr * sin(a).toFloat())
            }
            // 눈금: 바늘까지는 밝게
            for (i in 0..50) {
                val v = i / 50f
                val major = i % 5 == 0
                val lit = v <= u + 1e-4f
                val color = when {
                    lit && major -> Color(0xFFB9CADB)
                    lit -> Color(0xFF5E7489)
                    major -> Color(0xFF3B4A5C)
                    else -> Color(0xFF253242)
                }
                drawLine(color, at(v, r + 18f * p), at(v, r + (if (major) 36f else 27f) * p), (if (major) 3f else 2f) * p, StrokeCap.Round)
            }
            for (n in 0..100 step 20) {
                val v = n / 100f
                val layout = measurer.measure(n.toString(), num(sz(20f), FontWeight.Medium, if (v <= u) Color(0xFF7F91A5) else Color(0xFF46556A)))
                val c = at(v, r + 60f * p)
                drawText(layout, topLeft = Offset(c.x - layout.size.width / 2f, c.y - layout.size.height / 2f))
            }
            val tl = Offset(cx - r, cy - r)
            val box = Size(2 * r, 2 * r)
            drawArc(Color(0xFF18232F), 150f, 240f, false, tl, box, style = Stroke(w, cap = StrokeCap.Round))
            if (u < 1f) drawArc(Color(0xFF33445A), 150f + 240f * u, 240f * (1 - u), false, tl, box, style = Stroke(w))
            // 번지는 빛
            drawIntoCanvas { canvas ->
                val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = w
                    maskFilter = BlurMaskFilter(8f * p, BlurMaskFilter.Blur.NORMAL)
                }
                val rect = RectF(cx - r, cy - r, cx + r, cy + r)
                glow.color = Palette.biz.copy(alpha = 0.55f).toArgb()
                if (b > 0f) canvas.nativeCanvas.drawArc(rect, 150f, 240f * b, false, glow)
                glow.color = Palette.com.copy(alpha = 0.55f).toArgb()
                if (u > b) canvas.nativeCanvas.drawArc(rect, 150f + 240f * b, 240f * (u - b), false, glow)
                val n0 = at(u, r - 16f * p)
                val n1 = at(u, r + 16f * p)
                glow.color = Color.White.copy(alpha = 0.7f).toArgb()
                glow.strokeWidth = 8f * p
                glow.strokeCap = Paint.Cap.ROUND
                canvas.nativeCanvas.drawLine(n0.x, n0.y, n1.x, n1.y, glow)
            }
            if (b > 0f) drawArc(Palette.biz, 150f, 240f * b, false, tl, box, style = Stroke(w, cap = StrokeCap.Round))
            if (u > b) drawArc(Palette.com, 150f + 240f * b, 240f * (u - b), false, tl, box, style = Stroke(w))
            drawLine(Color.White, at(u, r - 16f * p), at(u, r + 16f * p), 5f * p, StrokeCap.Round)
        }

        Column(Modifier.fillMaxWidth().offset(y = (top + 106f * k).dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${month.monthValue}월 업무사용비율", style = kr(sz(22f), FontWeight.Bold, Palette.sub))
            Row {
                Text(
                    "%.0f".format(u * 100),
                    Modifier.alignByBaseline(),
                    style = num(sz(150f), FontWeight.SemiBold, Palette.text)
                        .copy(lineHeight = sz(150f) * 0.95f, shadow = Shadow(Palette.biz.copy(alpha = 0.22f), blurRadius = 40f)),
                )
                Text("%", Modifier.alignByBaseline().padding(start = (4f * k).dp), style = num(sz(56f), FontWeight.Medium, Palette.sub))
            }
        }
        Text(
            buildAnnotatedString {
                append("업무 ")
                withStyle(SpanStyle(color = Palette.text, fontWeight = FontWeight.SemiBold)) { append(km1(s.businessUseKm)) }
                append(" / 전체 ${km1(s.totalKm)} km")
            },
            Modifier.fillMaxWidth().offset(y = (top + 336f * k).dp),
            style = num(sz(24f), FontWeight.Medium, Palette.sub),
            textAlign = TextAlign.Center,
        )
        Row(
            Modifier.align(Alignment.BottomCenter).offset(y = (-(top + 26f * k)).dp),
            horizontalArrangement = Arrangement.spacedBy((30f * k).dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Legend("일반 업무", s.businessKm, Palette.biz, k, ::sz)
            Legend("출퇴근", s.commuteKm, Palette.com, k, ::sz)
            Legend("개인", personalKm, Color(0xFF33445A), k, ::sz)
        }
    }
}

@Composable
private fun Legend(label: String, km: Double, color: Color, k: Float, sz: (Float) -> TextUnit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size((20f * k).dp, (7f * k).dp).background(color, RoundedCornerShape(4.dp)))
        Text(label, Modifier.padding(start = (9f * k).dp), style = kr(sz(20f), FontWeight.SemiBold, Palette.sub))
        Text(km1(km), Modifier.padding(start = (8f * k).dp), style = num(sz(22f), FontWeight.SemiBold, Palette.text))
    }
}
