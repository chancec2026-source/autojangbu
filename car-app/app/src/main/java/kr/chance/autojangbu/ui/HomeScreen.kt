package kr.chance.autojangbu.ui

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale
import kotlinx.coroutines.delay
import kr.chance.autojangbu.BuildConfig
import kr.chance.autojangbu.R
import kr.chance.autojangbu.data.Category
import kr.chance.autojangbu.data.DriverEntry
import kr.chance.autojangbu.data.LedgerExport
import kr.chance.autojangbu.data.MonthSummary
import kr.chance.autojangbu.data.Trip
import kr.chance.autojangbu.data.TripStore
import kr.chance.autojangbu.data.summarize
import kr.chance.autojangbu.share.ShareClient

private val timeFmt = SimpleDateFormat("HH:mm", Locale.KOREAN)
private val dayFmt = SimpleDateFormat("M월 d일 E", Locale.KOREAN)
private val shortDayFmt = SimpleDateFormat("M/d", Locale.KOREAN)
internal fun Trip.date(): LocalDate = Instant.ofEpochMilli(startMs).atZone(ZoneId.systemDefault()).toLocalDate()
internal fun Trip.month(): YearMonth = YearMonth.from(date())
internal fun monthLabel(m: YearMonth) = "%d-%02d".format(m.year, m.monthValue)

/** 목록 제목: 메모가 있으면 메모, 없으면 출근·퇴근·개인 운행. */
private fun Trip.title(): String = memo.ifBlank {
    when (effectiveCategory) {
        Category.COMMUTE -> if (hourOf(startMs) < 12) "출근" else "퇴근"
        Category.PERSONAL -> "개인 운행"
        Category.BUSINESS -> "업무 운행"
    }
}

private fun hourOf(ms: Long) = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).hour

@Composable
fun HomeScreen() {
    val trips by TripStore.trips.collectAsStateWithLifecycle()
    val activeStart by TripStore.activeStartOdoKm.collectAsStateWithLifecycle()
    val lastOdo by TripStore.lastOdoKm.collectAsStateWithLifecycle()
    val profile by TripStore.profile.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val thisMonth = remember { YearMonth.now() }
    var month by remember { mutableStateOf(thisMonth) }
    val monthTrips = remember(trips, month) { trips.filter { it.month() == month } }
    val summary = remember(monthTrips) { summarize(monthTrips) }
    val unconfirmed = trips.filter { !it.confirmed }
    val pending = unconfirmed.maxByOrNull { it.endMs }
    var showReport by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf(false) }
    var sharing by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(notice) { if (notice != null) { delay(4_000); notice = null } }

    CompositionLocalProvider(LocalContentColor provides Palette.text) {
        Box(
            Modifier.fillMaxSize().background(Palette.bg).drawBehind {
                drawRect(Brush.radialGradient(listOf(Palette.glow, Color.Transparent), Offset(size.width * 0.16f, -size.height * 0.1f), 1100.dp.toPx()))
                drawRect(Brush.radialGradient(listOf(Color(0xFF0F1B29), Color.Transparent), Offset(size.width * 1.05f, size.height * 1.15f), 900.dp.toPx()))
            }
        ) {
            Column(Modifier.fillMaxSize()) {
                TopBar(month, thisMonth, { month = it }, activeStart, lastOdo, profile) { editingProfile = true }
                Row(
                    Modifier.fillMaxSize().padding(start = 40.dp, end = 40.dp, top = 8.dp, bottom = 40.dp),
                    horizontalArrangement = Arrangement.spacedBy(40.dp),
                ) {
                    Column(Modifier.weight(700f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        RatioGauge(month, summary, Modifier.fillMaxWidth().weight(1f))
                        Tiles(summary)
                        if (pending != null) PendingCard(pending, unconfirmed.size) else AllDoneCard(month, summary)
                        Actions(
                            onSend = { sharing = true },
                            onSave = { notice = saveCsv(context, monthLabel(month), monthTrips, profile) },
                        )
                    }
                    Column(Modifier.weight(884f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(Modifier.fillMaxWidth().height(76.dp), verticalAlignment = Alignment.CenterVertically) {
                            Segmented(listOf("운행 장부", "${month.monthValue}월 결산"), if (showReport) 1 else 0, { showReport = it == 1 })
                            Spacer(Modifier.weight(1f))
                            Text(
                                buildAnnotatedString {
                                    withStyle(SpanStyle(color = Palette.text)) { append("${summary.tripCount}") }
                                    append("건 · ")
                                    withStyle(SpanStyle(color = Palette.text)) { append(km1(summary.totalKm)) }
                                    append(" km")
                                },
                                style = num(23, FontWeight.Medium, Palette.sub),
                            )
                        }
                        if (showReport) MonthReport(month, monthTrips, profile, Modifier.weight(1f))
                        else TripList(monthTrips, profile, month == thisMonth, Modifier.weight(1f))
                    }
                }
            }
            notice?.let { Notice(it, Modifier.align(Alignment.BottomCenter).padding(bottom = 56.dp)) }
        }
        if (editingProfile) ProfileDialog(profile, onDismiss = { editingProfile = false })
        if (sharing) ShareDialog(month, trips, profile, onDismiss = { sharing = false })
    }
}

/** 국세청 양식 CSV 를 차 저장공간에 쓰고 알림 문구를 돌려준다. */
internal fun saveCsv(context: Context, label: String, trips: List<Trip>, profile: TripStore.Profile): String {
    val csv = LedgerExport.toCsv(trips, profile, ShareClient.periodText(label))
    val dir = context.getExternalFilesDir(null) ?: context.filesDir
    return "엑셀 파일을 저장했어요 · " + LedgerExport.write(dir, label, csv).name
}

@Composable
private fun TopBar(
    month: YearMonth,
    thisMonth: YearMonth,
    onMonth: (YearMonth) -> Unit,
    activeStart: Double?,
    lastOdo: Double?,
    profile: TripStore.Profile,
    onProfile: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().height(112.dp).padding(horizontal = 40.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(26.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Image(painterResource(R.drawable.ic_brand), null, Modifier.size(56.dp))
            Text("오토장부", style = kr(30, FontWeight.ExtraBold).copy(letterSpacing = (-0.02).em))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.padding(end = 18.dp).width(1.dp).height(48.dp).background(Palette.line2))
            MonthArrow(Glyph.left, "이전 달", true) { onMonth(month.minusMonths(1)) }
            Text("${month.year}. %02d".format(month.monthValue), Modifier.widthIn(min = 112.dp), style = num(30, FontWeight.SemiBold), textAlign = TextAlign.Center)
            MonthArrow(Glyph.right, "다음 달", month < thisMonth) { onMonth(month.plusMonths(1)) }
        }
        Spacer(Modifier.weight(1f))
        val (gear, status) = when {
            activeStart != null -> "D" to "기록 중"
            lastOdo != null -> "P" to "주차 중"
            else -> "–" to "차량 신호를 기다리는 중"
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(40.dp).background(if (gear == "D") Palette.biz else Palette.text, RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center,
            ) { Text(gear, style = num(26, FontWeight.Bold, Palette.bg)) }
            Text(status, style = kr(22, FontWeight.SemiBold, Palette.sub))
        }
        Odometer(lastOdo ?: activeStart)
        PlateChip(profile, onProfile)
    }
}

@Composable
private fun MonthArrow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(56.dp, 64.dp).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label, Modifier.size(30.dp), tint = if (enabled) Palette.sub else Palette.line2) }
}

/** 기계식 적산계: 정수 여섯 칸은 검은 북, 소수 칸은 흰 북. */
@Composable
private fun Odometer(km: Double?) {
    val (digits, tenth) = km?.let { odometerDrums(it) } ?: ("------" to null)
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier.background(Color(0xFF060A10), shape).border(1.dp, Palette.line2, shape).padding(horizontal = 9.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        digits.forEach { Drum(it.toString(), dark = true) }
        Spacer(Modifier.width(3.dp))
        Drum(tenth?.toString() ?: "-", dark = false)
        Text("km", Modifier.padding(start = 8.dp, end = 6.dp), style = num(22, FontWeight.Medium, Palette.sub))
    }
}

private val drumDark = Brush.verticalGradient(
    0f to Color(0xFF03060A), 0.24f to Color(0xFF18222E), 0.5f to Color(0xFF1F2B39), 0.76f to Color(0xFF18222E), 1f to Color(0xFF03060A),
)
private val drumLight = Brush.verticalGradient(
    0f to Color(0xFF9AA6B3), 0.26f to Color(0xFFEEF2F6), 0.5f to Color.White, 0.74f to Color(0xFFEEF2F6), 1f to Color(0xFF9AA6B3),
)

@Composable
private fun Drum(d: String, dark: Boolean) {
    Box(
        Modifier.size(33.dp, 50.dp).background(if (dark) drumDark else drumLight, RoundedCornerShape(5.dp)),
        contentAlignment = Alignment.Center,
    ) { Text(d, style = num(34, FontWeight.SemiBold, if (dark) Color(0xFFF1F5F9) else Palette.bg)) }
}

/** 번호판 + 사용자. 누르면 장부 정보. */
@Composable
private fun PlateChip(profile: TripStore.Profile, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier.height(64.dp).panel(shape, Palette.panel, Palette.line2).clickable(onClick = onClick).padding(start = 8.dp, end = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (profile.plate.isNotBlank()) {
            val plateShape = RoundedCornerShape(9.dp)
            Box(
                Modifier.height(46.dp).background(Palette.plate, plateShape).border(2.dp, Palette.plateInk, plateShape).padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) { Text(platePretty(profile.plate), style = kr(23, FontWeight.ExtraBold, Palette.plateInk).copy(letterSpacing = 0.03.em)) }
            Text(profile.name, style = kr(22, FontWeight.SemiBold, Palette.sub))
        } else {
            Icon(Glyph.pencil, null, Modifier.padding(start = 12.dp).size(24.dp), tint = Palette.biz)
            Text("장부 정보 넣기", style = kr(22, FontWeight.SemiBold))
        }
    }
}

/** 경고등 칸: 확인할 것이 있으면 노랗게 켜진다. */
@Composable
private fun Tiles(s: MonthSummary) {
    Row(Modifier.fillMaxWidth().height(100.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Tile("운행", s.tripCount, false, Modifier.weight(1f))
        Tile("확인 필요", s.unconfirmedCount, s.unconfirmedCount > 0, Modifier.weight(1f))
        Tile("목적 빈칸", s.missingPurposeCount, s.missingPurposeCount > 0, Modifier.weight(1f))
    }
}

@Composable
private fun Tile(label: String, count: Int, lit: Boolean, modifier: Modifier) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier.fillMaxHeight()
            .then(
                if (lit) Modifier.panel(shape, Color.Transparent, Palette.amber.copy(alpha = 0.28f))
                    .background(Brush.verticalGradient(listOf(Palette.amber.copy(alpha = 0.07f), Palette.amber.copy(alpha = 0.02f))), shape)
                else Modifier.panel(shape)
            )
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (lit) Icon(Glyph.warn, null, Modifier.padding(end = 9.dp).size(24.dp), tint = Palette.amber)
        Text(label, Modifier.weight(1f), style = kr(20, FontWeight.Bold, if (lit) Palette.amber else Palette.sub), maxLines = 1)
        Row {
            Text("$count", Modifier.alignByBaseline(), style = num(42, FontWeight.SemiBold))
            Text("건", Modifier.alignByBaseline().padding(start = 3.dp), style = kr(20, FontWeight.Bold, Palette.sub))
        }
    }
}

/** 방금 주차한(또는 아직 고르지 않은) 운행을 업무·출퇴근·개인 중에서 고른다. 추천을 맨 앞에. */
@Composable
private fun PendingCard(trip: Trip, unconfirmedCount: Int) {
    val shape = RoundedCornerShape(28.dp)
    val recent = System.currentTimeMillis() - trip.endMs < 3 * 60 * 60 * 1000L
    val sameDay = trip.date() == LocalDate.now()
    Column(
        Modifier.fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color(0xFF17283A), Color(0xFF112031))), shape)
            .drawWithContent {
                drawContent()
                // 위 테두리에 하늘색 빛 한 줄
                drawLine(
                    Brush.horizontalGradient(listOf(Color.Transparent, Palette.biz, Color.Transparent), 40.dp.toPx(), size.width - 40.dp.toPx()),
                    Offset(40.dp.toPx(), 0f), Offset(size.width - 40.dp.toPx(), 0f), 2.dp.toPx(),
                )
            }
            .border(1.dp, Color(0xFF2A4157), shape)
            .padding(start = 30.dp, end = 30.dp, top = 28.dp, bottom = 26.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(10.dp).drawBehind {
                    drawCircle(Palette.biz.copy(alpha = 0.35f), radius = size.minDimension)
                    drawCircle(Palette.biz)
                }
            )
            val eyebrow = (if (recent) "방금 주차했어요" else "분류를 기다리는 운행") + if (unconfirmedCount > 1) " · ${unconfirmedCount}건 남음" else ""
            Text(eyebrow, Modifier.padding(start = 10.dp).weight(1f), style = kr(20, FontWeight.Bold, Palette.biz))
            Text(
                (if (sameDay) "" else shortDayFmt.format(trip.endMs) + " ") + timeFmt.format(trip.endMs) + " 주차",
                style = num(21, FontWeight.Medium, Palette.sub),
            )
        }
        Text("이번 운행은 어떤 운행이었나요?", Modifier.padding(top = 12.dp), style = kr(34, FontWeight.Bold).copy(letterSpacing = (-0.015).em))
        Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(dayFmt.format(trip.startMs), Modifier.alignByBaseline(), style = kr(21, FontWeight.SemiBold, Palette.sub))
            Text("${timeFmt.format(trip.startMs)} – ${timeFmt.format(trip.endMs)}", Modifier.alignByBaseline(), style = num(24, FontWeight.SemiBold))
            Text("${km1(trip.distanceKm)} km", Modifier.alignByBaseline(), style = num(30, FontWeight.SemiBold, Palette.biz))
        }
        Row(Modifier.padding(top = 22.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            val order = listOf(trip.suggested) + Category.entries.filter { it != trip.suggested }
            order.forEachIndexed { i, c -> Choice(c, i == 0, Modifier.weight(if (i == 0) 1.3f else 1f)) { TripStore.classify(trip.id, c) } }
        }
        Row(Modifier.padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Glyph.mic, null, Modifier.size(24.dp), tint = Palette.sub)
            Text(
                buildAnnotatedString {
                    append("말로 해도 돼요 — ")
                    withStyle(SpanStyle(color = Palette.text, fontWeight = FontWeight.SemiBold)) { append("“오토장부, ${withRo(trip.suggested.label)} 기록해”") }
                },
                Modifier.padding(start = 10.dp),
                style = kr(20, FontWeight.Medium, Palette.sub),
            )
        }
    }
}

@Composable
private fun Choice(c: Category, recommended: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier.height(92.dp)
            .then(
                if (recommended) Modifier.background(Brush.verticalGradient(listOf(Color(0xFF7AD6FF), Color(0xFF4FC2F7))), shape)
                else Modifier.panel(shape, Palette.raised, Palette.line2)
            )
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(c.label, style = kr(28, FontWeight.Bold, if (recommended) Palette.onBiz else Palette.text))
        if (recommended) {
            Text(
                "추천",
                Modifier.padding(start = 12.dp).background(Palette.onBiz.copy(alpha = 0.14f), RoundedCornerShape(50)).padding(horizontal = 11.dp, vertical = 4.dp),
                style = kr(17, FontWeight.ExtraBold, Palette.onBiz),
            )
        }
    }
}

/** 고를 운행이 없을 때. */
@Composable
private fun AllDoneCard(month: YearMonth, s: MonthSummary) {
    val shape = RoundedCornerShape(28.dp)
    Column(Modifier.fillMaxWidth().panel(shape, Palette.panel, Palette.line2).padding(horizontal = 30.dp, vertical = 30.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Glyph.check, null, Modifier.size(26.dp), tint = Palette.biz)
            Text("모두 분류했어요", Modifier.padding(start = 10.dp), style = kr(20, FontWeight.Bold, Palette.biz))
        }
        Text("다음 운행은 주차하면 알림으로 물어볼게요", Modifier.padding(top = 12.dp), style = kr(32, FontWeight.Bold).copy(letterSpacing = (-0.015).em))
        Text(
            when {
                s.tripCount == 0 -> "${month.monthValue}월에는 기록된 운행이 없어요."
                s.missingPurposeCount > 0 -> "방문 목적이 빈 업무 운행이 ${s.missingPurposeCount}건 있어요. 목록에서 눌러 적어 두면 세무 증빙에 유리해요."
                else -> "${month.monthValue}월 장부는 세무사님께 바로 보낼 수 있어요."
            },
            Modifier.padding(top = 12.dp),
            style = kr(21, FontWeight.Medium, if (s.missingPurposeCount > 0) Palette.amber else Palette.sub),
        )
    }
}

@Composable
private fun Actions(onSend: () -> Unit, onSave: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(92.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        val shape = RoundedCornerShape(22.dp)
        Row(
            Modifier.weight(1.55f).fillMaxHeight().clip(shape).background(Palette.biz.copy(alpha = 0.07f), shape)
                .border(1.5.dp, Palette.biz.copy(alpha = 0.6f), shape).clickable(onClick = onSend),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Glyph.qr, null, Modifier.size(30.dp), tint = Palette.biz)
            Text("장부 보내기 · 휴대폰/세무사", Modifier.padding(start = 12.dp), style = kr(24, FontWeight.Bold))
        }
        Row(
            Modifier.weight(1f).fillMaxHeight().clip(shape).border(1.dp, Palette.line2, shape).clickable(onClick = onSave),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Glyph.down, null, Modifier.size(28.dp), tint = Palette.sub)
            Text("엑셀 파일 저장", Modifier.padding(start = 12.dp), style = kr(24, FontWeight.Bold, Palette.sub))
        }
    }
}

/** 날짜별로 묶은 운행 목록. 날짜 머리는 위에 붙어 있다. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TripList(trips: List<Trip>, profile: TripStore.Profile, isThisMonth: Boolean, modifier: Modifier) {
    val groups = remember(trips) { trips.sortedByDescending { it.startMs }.groupBy { it.date() } }
    val today = LocalDate.now()
    val listState = rememberLazyListState()
    // 최신 운행이 맨 위다. 새 운행이 들어오면 지난 위치를 붙잡지 말고 맨 위를 보여 준다.
    val newest = trips.maxOfOrNull { it.startMs }
    LaunchedEffect(newest) { listState.scrollToItem(0) }
    Box(modifier.fillMaxWidth().panel()) {
        if (trips.isEmpty()) {
            Text(
                if (isThisMonth) "아직 기록된 운행이 없어요. D로 출발했다가 P로 주차하고 내리면 여기에 한 줄이 생깁니다."
                else "이 달에는 기록된 운행이 없어요.",
                Modifier.align(Alignment.Center).padding(48.dp),
                style = kr(24, color = Palette.sub),
                textAlign = TextAlign.Center,
            )
        }
        LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = 72.dp)) {
            groups.forEach { (day, list) ->
                stickyHeader(key = "d$day") { DayHeader(day, list, today) }
                items(list, key = { it.id }) { TripRow(it, profile) }
            }
        }
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(90.dp)
                .background(Brush.verticalGradient(listOf(Palette.panel.copy(alpha = 0f), Palette.panel)))
        )
    }
}

@Composable
private fun DayHeader(day: LocalDate, list: List<Trip>, today: LocalDate) {
    val (md, wd) = dayHead(day)
    Row(
        Modifier.fillMaxWidth().height(56.dp).background(Palette.dayHead).bottomLine().padding(horizontal = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row {
            Text(md, Modifier.alignByBaseline(), style = num(24, FontWeight.SemiBold))
            Text(wd, Modifier.alignByBaseline().padding(start = 6.dp), style = kr(21, FontWeight.Bold))
        }
        val tag = when (day) {
            today -> "오늘"
            today.minusDays(1) -> "어제"
            else -> null
        }
        if (tag != null) {
            val on = tag == "오늘"
            Text(
                tag,
                Modifier.padding(start = 12.dp)
                    .background(if (on) Palette.biz.copy(alpha = 0.14f) else Palette.sub.copy(alpha = 0.14f), RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 3.dp),
                style = kr(16, FontWeight.Bold, if (on) Palette.biz else Palette.sub),
            )
        }
        Spacer(Modifier.weight(1f))
        Text("${km1(list.sumOf { it.distanceKm })} km", style = num(21, FontWeight.Medium, Palette.sub))
    }
}

@Composable
private fun TripRow(trip: Trip, profile: TripStore.Profile) {
    var menuOpen by remember { mutableStateOf(false) }
    var editingMemo by remember { mutableStateOf(false) }
    val pending = !trip.confirmed
    Row(
        Modifier.fillMaxWidth().height(104.dp)
            .then(
                if (pending) Modifier.background(Brush.horizontalGradient(0f to Palette.amber.copy(alpha = 0.08f), 0.7f to Palette.amber.copy(alpha = 0f)))
                else Modifier
            )
            .bottomLine()
            .padding(start = 28.dp, end = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.width(104.dp)) {
            Text(timeFmt.format(trip.startMs), style = num(32, FontWeight.SemiBold))
            Text("– ${timeFmt.format(trip.endMs)}", Modifier.padding(top = 2.dp), style = num(20, FontWeight.Medium, Palette.faint))
        }
        Column(Modifier.weight(1f)) {
            when {
                pending -> Alert(Glyph.warn, "분류를 기다려요") { menuOpen = true }
                trip.needsPurpose -> Alert(Glyph.pencil, "방문 목적을 적어 주세요") { editingMemo = true }
                else -> Text(trip.title(), style = kr(26, FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(km1(trip.startOdoKm), style = num(20, FontWeight.Medium, Palette.sub))
                Icon(Glyph.arrow, null, Modifier.padding(horizontal = 6.dp).size(14.dp, 9.dp), tint = Palette.sub)
                Text("${km1(trip.endOdoKm)} km · ", style = num(20, FontWeight.Medium, Palette.sub))
                Text(LedgerExport.user(trip, profile).second, style = kr(20, FontWeight.SemiBold, Palette.sub), maxLines = 1)
            }
        }
        Row(Modifier.width(116.dp), horizontalArrangement = Arrangement.End) {
            Text(km1(trip.distanceKm), Modifier.alignByBaseline(), style = num(36, FontWeight.SemiBold))
            Text("km", Modifier.alignByBaseline().padding(start = 3.dp), style = num(20, FontWeight.Medium, Palette.sub))
        }
        Box(Modifier.width(146.dp)) {
            CategoryPill(trip.effectiveCategory, trip.confirmed, { menuOpen = true }, Modifier.fillMaxWidth())
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                Category.entries.forEach { c ->
                    DropdownMenuItem(
                        text = { Text(c.label, style = kr(24, FontWeight.SemiBold)) },
                        onClick = { TripStore.classify(trip.id, c); menuOpen = false },
                        modifier = Modifier.heightIn(min = 64.dp),
                    )
                }
                DropdownMenuItem(
                    text = { Text("메모(방문 목적) 쓰기", style = kr(24, FontWeight.SemiBold, Palette.biz)) },
                    onClick = { menuOpen = false; editingMemo = true },
                    modifier = Modifier.heightIn(min = 64.dp),
                )
            }
        }
    }
    if (editingMemo) MemoDialog(trip, onDismiss = { editingMemo = false })
}

@Composable
private fun Alert(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: () -> Unit) {
    Row(Modifier.clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(24.dp), tint = Palette.amber)
        Text(text, Modifier.padding(start = 8.dp), style = kr(26, FontWeight.SemiBold, Palette.amber), maxLines = 1)
    }
}

@Composable
private fun MemoDialog(trip: Trip, onDismiss: () -> Unit) {
    var memo by remember { mutableStateOf(trip.memo) }
    DialogFrame(
        "방문처 · 목적",
        onDismiss,
        width = 820.dp,
        buttons = {
            GhostButton("취소", onDismiss, Modifier.width(200.dp))
            PrimaryButton("저장", { TripStore.setMemo(trip.id, memo.trim()); onDismiss() }, Modifier.width(200.dp))
        },
    ) {
        Text("${dayFmt.format(trip.startMs)} ${timeFmt.format(trip.startMs)} · ${km1(trip.distanceKm)} km 운행의 비고 칸에 들어갑니다.", style = kr(21))
        OutlinedTextField(
            memo, { memo = it }, label = { Text("예: 강남 OO아파트 현장 점검", style = kr(20)) }, singleLine = true,
            textStyle = kr(24, color = Palette.text), colors = fieldColors(), modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ProfileDialog(profile: TripStore.Profile, onDismiss: () -> Unit) {
    var p by remember { mutableStateOf(profile) }
    @Composable
    fun field(label: String, value: String, modifier: Modifier, onChange: (String) -> Unit) =
        OutlinedTextField(
            value, onChange, label = { Text(label, style = kr(20)) }, singleLine = true,
            textStyle = kr(24, color = Palette.text), colors = fieldColors(), modifier = modifier,
        )
    // 차 화면은 넓으므로 기본 대화상자(최대 560dp) 대신 넓은 틀을 쓴다.
    DialogFrame(
        "운행기록부 정보",
        onDismiss,
        width = 1000.dp,
        buttons = {
            GhostButton("취소", onDismiss, Modifier.width(200.dp))
            PrimaryButton("저장", {
                TripStore.saveProfile(p.copy(
                    name = p.name.trim().ifEmpty { "대표" }, department = p.department.trim(), company = p.company.trim(),
                    bizNo = p.bizNo.trim(), carModel = p.carModel.trim(), plate = p.plate.trim(),
                ))
                onDismiss()
            }, Modifier.width(200.dp))
        },
    ) {
        Text("국세청 양식의 머리말과 사용자 칸에 들어갑니다. 자동차등록번호는 차에서 읽어 와요.", style = kr(21))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            field("상호(법인명)", p.company, Modifier.weight(1f)) { p = p.copy(company = it) }
            field("사업자등록번호", p.bizNo, Modifier.weight(1f)) { p = p.copy(bizNo = it) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            field("차종(예: 그랜저)", p.carModel, Modifier.weight(1f)) { p = p.copy(carModel = it) }
            field("자동차등록번호", p.plate, Modifier.weight(1f)) { p = p.copy(plate = it) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            field("기본 사용자 성명", p.name, Modifier.weight(1f)) { p = p.copy(name = it) }
            field("부서(선택)", p.department, Modifier.weight(1f)) { p = p.copy(department = it) }
        }
        DriversSection()
        if (BuildConfig.DEBUG) DemoSeedButton(onDone = onDismiss)
    }
}

/** 체험판(개발용 빌드)에만: 한 달 치 예시 운행을 넣는다. 받는 쪽은 src/debug 의 DemoSeedReceiver. */
@Composable
private fun DemoSeedButton(onDone: () -> Unit) {
    val context = LocalContext.current
    val lastOdo by TripStore.lastOdoKm.collectAsStateWithLifecycle()
    GhostButton(
        "체험용 예시 데이터 넣기(지금 장부를 예시로 바꿉니다)",
        {
            context.sendBroadcast(android.content.Intent("kr.chance.autojangbu.DEMO_SEED")
                .setPackage(context.packageName)
                .putExtra("odo", (lastOdo ?: 12_000.0).toFloat()))
            onDone()
        },
        color = Palette.amber,
    )
}

/** 한 차를 여러 사람이 쓸 때: 각자 맞춰 둔 시트 위치를 기억해 출발할 때 운전자를 알아본다. */
@Composable
private fun DriversSection() {
    val drivers by TripStore.drivers.collectAsStateWithLifecycle()
    val pose by TripStore.currentPose.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf("") }
    var dept by remember { mutableStateOf("") }
    Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("운전자 여러 명 (시트 위치로 자동 구분)", style = kr(26, FontWeight.Bold, Palette.text))
        Text("운전자가 자기 자세로 시트를 맞춘 뒤 등록하면, 출발할 때 시트 위치로 누가 운전하는지 알아보고 사용자 칸을 채웁니다.", style = kr(21))
        drivers.forEach { d ->
            Row(
                Modifier.fillMaxWidth().panel(RoundedCornerShape(18.dp), Palette.panel).padding(start = 22.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(listOf(d.department, d.name).filter { it.isNotBlank() }.joinToString(" "), Modifier.weight(1f), style = kr(24, FontWeight.SemiBold, Palette.text))
                Text("시트 ${d.pose.foreAft}/${d.pose.backrest}/${d.pose.height}", Modifier.padding(end = 16.dp), style = num(21, FontWeight.Medium, Palette.sub))
                GhostButton("지우기", { TripStore.saveDrivers(drivers - d) }, height = 60.dp)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                name, { name = it }, label = { Text("운전자 성명", style = kr(20)) }, singleLine = true,
                textStyle = kr(24, color = Palette.text), colors = fieldColors(), modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                dept, { dept = it }, label = { Text("부서", style = kr(20)) }, singleLine = true,
                textStyle = kr(24, color = Palette.text), colors = fieldColors(), modifier = Modifier.weight(1f),
            )
        }
        val p = pose
        PrimaryButton(
            when {
                p == null -> "시트 위치를 읽는 중…"
                !p.isKnown -> "이 차에서는 시트 위치를 읽을 수 없어요"
                else -> "지금 시트 위치로 등록 (앞뒤 ${p.foreAft} · 등받이 ${p.backrest} · 높이 ${p.height})"
            },
            { p?.let { TripStore.saveDrivers(drivers + DriverEntry(name.trim(), dept.trim(), it)); name = ""; dept = "" } },
            enabled = p != null && p.isKnown && name.isNotBlank(),
        )
    }
}
