package kr.chance.autojangbu.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs
import kotlin.math.ceil
import kr.chance.autojangbu.data.Category
import kr.chance.autojangbu.data.LedgerExport
import kr.chance.autojangbu.data.Trip
import kr.chance.autojangbu.data.TripStore
import kr.chance.autojangbu.data.summarize

/** 이번 달 결산: 날짜별 막대(일반 업무·출퇴근·개인)와 한 달 이야기. */
@Composable
fun MonthReport(month: YearMonth, trips: List<Trip>, profile: TripStore.Profile, modifier: Modifier = Modifier) {
    val s = summarize(trips)
    val today = LocalDate.now()
    val isThisMonth = YearMonth.from(today) == month
    val upTo = if (isThisMonth) today.dayOfMonth else month.lengthOfMonth()
    val byDay = trips.groupBy { it.date().dayOfMonth }
    Column(
        modifier.fillMaxWidth().panel().verticalScroll(rememberScrollState()).padding(horizontal = 32.dp, vertical = 30.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("${month.monthValue}월 운행 결산", style = kr(32, FontWeight.Bold))
            Spacer(Modifier.weight(1f))
            Text("1일 – ${upTo}일 · 하루 평균 ${km1(s.totalKm / upTo)} km", style = kr(22, FontWeight.Medium, Palette.sub))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(26.dp)) {
            LegendChip("일반 업무", Palette.biz)
            LegendChip("출퇴근", Palette.com)
            LegendChip("개인", Palette.per)
            Spacer(Modifier.weight(1f))
            Text("흐린 칸은 주말", style = kr(19, FontWeight.SemiBold, Palette.faint))
        }
        DayBars(month, byDay, if (isThisMonth) today.dayOfMonth else null, Modifier.fillMaxWidth().height(340.dp))

        val commutes = trips.filter { it.effectiveCategory == Category.COMMUTE }
        val longest = trips.maxByOrNull { it.distanceKm }
        val places = trips.mapNotNull { it.memo.takeIf { m -> m.isNotBlank() } }
            .groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.take(3)
        val drivers = trips.groupingBy { LedgerExport.user(it, profile).let { (dept, name) -> if (dept.isBlank()) name else "$name ($dept)" } }
            .eachCount().entries.sortedByDescending { it.value }.take(3)
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Fact("출근한 날", Modifier.weight(1f)) {
                Big("${commutes.map { it.date() }.distinct().size}", "일")
                Small("출퇴근 ${commutes.size}회 · ${km1(commutes.sumOf { it.distanceKm })} km")
            }
            Fact("가장 긴 운행", Modifier.weight(1f)) {
                if (longest == null) Small("아직 없어요")
                else {
                    Big(km1(longest.distanceKm), "km")
                    val (md, wd) = dayHead(longest.date())
                    Small("${longest.memo.ifBlank { longest.effectiveCategory.label + " 운행" }} · $md $wd")
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Fact("자주 간 곳", Modifier.weight(1f)) {
                if (places.isEmpty()) Small("메모를 남기면 여기에 모여요")
                places.forEach { Ranked(it.key, "${it.value}회") }
            }
            Fact("운전한 사람", Modifier.weight(1f)) {
                if (drivers.isEmpty()) Small("아직 없어요")
                drivers.forEach { Ranked(it.key, "${it.value}건") }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            val waiting = s.unconfirmedCount > 0
            Icon(if (waiting) Glyph.warn else Glyph.check, null, Modifier.size(24.dp), tint = if (waiting) Palette.amber else Palette.biz)
            Text(
                if (waiting) "확인 필요 ${s.unconfirmedCount}건 — 정하면 바로 세무사님께 보낼 수 있어요"
                else "확인할 운행이 없어요 — 세무사님께 바로 보낼 수 있어요",
                Modifier.padding(start = 10.dp),
                style = kr(21, FontWeight.SemiBold, if (waiting) Palette.amber else Palette.sub),
            )
        }
    }
}

@Composable
private fun LegendChip(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(18.dp, 7.dp).background(color, RoundedCornerShape(4.dp)))
        Text(label, Modifier.padding(start = 8.dp), style = kr(19, FontWeight.SemiBold, Palette.sub))
    }
}

@Composable
private fun Fact(title: String, modifier: Modifier, content: @Composable () -> Unit) {
    Column(modifier.heightIn(min = 150.dp).panel(RoundedCornerShape(20.dp), Palette.raised).padding(horizontal = 24.dp, vertical = 20.dp)) {
        Text(title, style = kr(19, FontWeight.Bold, Palette.sub))
        content()
    }
}

@Composable
private fun Big(value: String, unit: String) {
    Row(Modifier.padding(top = 6.dp)) {
        Text(value, Modifier.alignByBaseline(), style = num(44, FontWeight.SemiBold))
        Text(unit, Modifier.alignByBaseline().padding(start = 4.dp), style = kr(21, FontWeight.Bold, Palette.sub))
    }
}

@Composable
private fun Small(text: String) = Text(text, Modifier.padding(top = 4.dp), style = kr(20, FontWeight.Medium, Palette.sub), maxLines = 2, overflow = TextOverflow.Ellipsis)

@Composable
private fun Ranked(name: String, count: String) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(name, Modifier.weight(1f), style = kr(21, FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(count, style = num(22, FontWeight.SemiBold, Palette.biz))
    }
}

/** 날짜별 쌓은 막대. 아래부터 일반 업무 · 출퇴근 · 개인. 주말 칸은 흐리게, 오늘은 점선. */
@Composable
private fun DayBars(month: YearMonth, byDay: Map<Int, List<Trip>>, today: Int?, modifier: Modifier) {
    val days = month.lengthOfMonth()
    val measurer = rememberTextMeasurer()
    val maxKm = (1..days).maxOf { d -> byDay[d]?.sumOf { it.distanceKm } ?: 0.0 }
    // 눈금은 4~5줄만: 20 · 40 · 50 · 100km 같은 보기 좋은 간격을 고른다.
    val step = listOf(10.0, 20.0, 25.0, 40.0, 50.0, 100.0, 200.0, 250.0, 500.0).firstOrNull { ceil(maxKm / it) <= 5 } ?: 1000.0
    val top = (ceil(maxKm / step) * step).coerceAtLeast(step)
    Canvas(modifier) {
        val padL = 44.dp.toPx()
        val padB = 40.dp.toPx()
        val padT = 12.dp.toPx()
        val plotW = size.width - padL
        val plotH = size.height - padB - padT
        val slot = plotW / days
        val barW = minOf(14.dp.toPx(), slot * 0.6f)
        fun y(km: Double) = (padT + plotH - (km / top) * plotH).toFloat()
        for (d in 1..days) {
            val dow = month.atDay(d).dayOfWeek
            if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY)
                drawRect(Color.White.copy(alpha = 0.025f), Offset(padL + (d - 1) * slot, padT), Size(slot, plotH))
        }
        var v = 0.0
        while (v <= top + 1e-6) {
            val yy = y(v)
            if (v == 0.0) drawLine(Color(0xFF34455A), Offset(padL, yy), Offset(size.width, yy), 2f)
            else {
                drawLine(Palette.line, Offset(padL, yy), Offset(size.width, yy), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 6.dp.toPx())))
                val t = measurer.measure("${v.toInt()}", num(18, FontWeight.Medium, Palette.faint))
                drawText(t, topLeft = Offset(padL - 12.dp.toPx() - t.size.width, yy - t.size.height / 2f))
            }
            v += step
        }
        for (d in 1..days) {
            val list = byDay[d] ?: emptyList()
            val x = padL + (d - 1) * slot + (slot - barW) / 2
            var base = padT + plotH
            listOf(Category.BUSINESS to Palette.biz, Category.COMMUTE to Palette.com, Category.PERSONAL to Palette.per).forEach { (c, color) ->
                val km = list.filter { it.effectiveCategory == c }.sumOf { it.distanceKm }
                if (km <= 0) return@forEach
                val h = (km / top * plotH).toFloat()
                base -= h
                drawRoundRect(color, Offset(x, base), Size(barW, (h - 2f).coerceAtLeast(1f)), CornerRadius(3.dp.toPx()))
            }
            // 날짜 숫자: 1일·5일 간격·오늘. 오늘 바로 옆 숫자는 겹치므로 뺀다.
            if (d == today || ((d == 1 || d % 5 == 0) && (today == null || abs(d - today) > 1))) {
                val on = d == today
                val t = measurer.measure("$d", num(19, if (on) FontWeight.SemiBold else FontWeight.Medium, if (on) Palette.biz else Palette.faint))
                drawText(t, topLeft = Offset(x + barW / 2 - t.size.width / 2f, size.height - t.size.height - 4.dp.toPx()))
            }
        }
        if (today != null) {
            val tx = padL + (today - 1) * slot + slot / 2
            drawLine(
                Palette.biz.copy(alpha = 0.6f), Offset(tx, padT), Offset(tx, padT + plotH), 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 5.dp.toPx())),
            )
        }
    }
}
