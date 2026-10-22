package kr.chance.autojangbu.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale
import kr.chance.autojangbu.data.Category
import kr.chance.autojangbu.data.Trip
import kr.chance.autojangbu.data.summarize

/** 이번 달 결산: 날짜별 막대(업무·출퇴근·개인)와 한 달 이야기. */
@Composable
fun MonthReport(month: YearMonth, trips: List<Trip>) {
    val s = summarize(trips)
    val zone = ZoneId.systemDefault()
    val byDay = trips.groupBy { Instant.ofEpochMilli(it.startMs).atZone(zone).dayOfMonth }
    val days = month.lengthOfMonth()

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("${month.monthValue}월 운행 결산", style = MaterialTheme.typography.headlineSmall)
        Surface(shape = RoundedCornerShape(14.dp), color = Palette.surface, border = BorderStroke(1.dp, Palette.line)) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Legend("일반 업무", Palette.sign)
                    Legend("출퇴근", Palette.lane)
                    Legend("개인", Palette.line)
                }
                DayBars(days, byDay)
                Row {
                    Text("1일", style = MaterialTheme.typography.bodyMedium, color = Palette.muted, modifier = Modifier.weight(1f))
                    Text("${days}일", style = MaterialTheme.typography.bodyMedium, color = Palette.muted)
                }
            }
        }
        val longest = trips.maxByOrNull { it.distanceKm }
        val places = trips.mapNotNull { it.memo.takeIf { m -> m.isNotBlank() } }
            .groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.take(3)
        val commuteDays = trips.filter { it.effectiveCategory == Category.COMMUTE }
            .map { Instant.ofEpochMilli(it.startMs).atZone(zone).dayOfMonth }.distinct().size
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Line("업무사용비율", String.format(Locale.KOREAN, "%.0f%%  (업무 %,.1f km / 전체 %,.1f km)", s.businessUseRatio * 100, s.businessUseKm, s.totalKm))
            Line("출근한 날", "${commuteDays}일")
            longest?.let { Line("가장 긴 운행", String.format(Locale.KOREAN, "%,.1f km", it.distanceKm) + (if (it.memo.isNotBlank()) " · ${it.memo}" else "")) }
            if (places.isNotEmpty()) Line("자주 간 곳", places.joinToString(", ") { "${it.key} ${it.value}회" })
            Line("확인 필요", if (s.unconfirmedCount == 0) "없음 — 세무사님께 바로 보낼 수 있어요" else "${s.unconfirmedCount}건")
        }
    }
}

@Composable
private fun DayBars(days: Int, byDay: Map<Int, List<Trip>>) {
    val maxKm = (1..days).maxOf { d -> byDay[d]?.sumOf { it.distanceKm } ?: 0.0 }.coerceAtLeast(1.0)
    Canvas(Modifier.fillMaxWidth().height(180.dp)) {
        val slot = size.width / days
        val barW = slot * 0.62f
        // 아래 기준선
        drawLine(Palette.line, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 2f)
        for (d in 1..days) {
            val trips = byDay[d] ?: continue
            var top = size.height
            val x = (d - 1) * slot + (slot - barW) / 2
            listOf(Category.BUSINESS to Palette.sign, Category.COMMUTE to Palette.lane, Category.PERSONAL to Palette.line)
                .forEach { (c, color) ->
                    val km = trips.filter { it.effectiveCategory == c }.sumOf { it.distanceKm }
                    if (km <= 0) return@forEach
                    val h = (km / maxKm * (size.height - 8f)).toFloat()
                    top -= h
                    drawRoundRect(color, Offset(x, top), Size(barW, h), CornerRadius(3f, 3f))
                }
        }
    }
}

@Composable
private fun Legend(label: String, color: androidx.compose.ui.graphics.Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(20.dp).background(color, RoundedCornerShape(4.dp)))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Palette.muted)
    }
}

@Composable
private fun Line(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = Palette.muted, modifier = Modifier.weight(0.32f))
        Text(value, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(0.68f))
    }
}
