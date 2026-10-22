package kr.chance.autojangbu.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale
import kr.chance.autojangbu.data.Category
import kr.chance.autojangbu.data.LedgerExport
import kr.chance.autojangbu.data.Trip
import kr.chance.autojangbu.data.TripStore
import kr.chance.autojangbu.data.summarize
import kr.chance.autojangbu.share.ShareClient
import kr.chance.autojangbu.share.qrBitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val dayFmt = SimpleDateFormat("M/d(E)", Locale.KOREAN)
private val timeFmt = SimpleDateFormat("HH:mm", Locale.KOREAN)
private fun km(v: Double) = String.format(Locale.KOREAN, "%,.1f", v)
// 요약 칸은 좁아서 100km 넘으면 소수점을 뺀다.
private fun kmShort(v: Double) = if (v >= 100) String.format(Locale.KOREAN, "%,.0f", v) else km(v)
private fun odo(v: Double) = String.format(Locale.KOREAN, "%,.0f", v)
private fun Trip.month(): YearMonth = YearMonth.from(Instant.ofEpochMilli(startMs).atZone(ZoneId.systemDefault()))

@Composable
fun HomeScreen() {
    val trips by TripStore.trips.collectAsStateWithLifecycle()
    val activeStart by TripStore.activeStartOdoKm.collectAsStateWithLifecycle()
    val lastOdo by TripStore.lastOdoKm.collectAsStateWithLifecycle()
    val month = remember { YearMonth.now() }
    val monthTrips = trips.filter { it.month() == month }
    val pending = trips.filter { !it.confirmed }.maxByOrNull { it.endMs }

    Row(
        Modifier.fillMaxSize().background(Palette.paper).padding(horizontal = 40.dp, vertical = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(40.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(28.dp)) {
            Header(month, activeStart, lastOdo)
            Summary(monthTrips)
            if (pending != null) PendingCard(pending) else AllDone()
            ExportRow(month, monthTrips)
        }
        Column(Modifier.weight(1.15f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            var showReport by remember { mutableStateOf(false) }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Tab("운행 목록", selected = !showReport) { showReport = false }
                Tab("이번 달 결산", selected = showReport) { showReport = true }
            }
            if (showReport) {
                MonthReport(month, monthTrips)
                return@Column
            }
            if (trips.isEmpty()) {
                Text("아직 기록된 운행이 없어요. D로 출발했다가 P로 주차하고 내리면 여기에 한 줄이 생깁니다.",
                    style = MaterialTheme.typography.bodyLarge, color = Palette.muted)
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(trips.sortedByDescending { it.startMs }, key = { it.id }) { TripRow(it) }
            }
        }
    }
}

@Composable
private fun Tab(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick, modifier = Modifier.heightIn(min = 64.dp), shape = RoundedCornerShape(10.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = Modifier.heightIn(min = 64.dp), shape = RoundedCornerShape(10.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = Palette.ink)
        }
    }
}

@Composable
private fun Header(month: YearMonth, activeStart: Double?, lastOdo: Double?) {
    val driver by TripStore.driver.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("오토장부", style = MaterialTheme.typography.displaySmall, color = Palette.sign, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = { editing = true }, modifier = Modifier.heightIn(min = 64.dp), shape = RoundedCornerShape(10.dp)) {
                val who = listOf(driver.department, driver.name).filter { it.isNotBlank() }.joinToString(" ")
                Text("사용자: $who", style = MaterialTheme.typography.labelLarge, color = Palette.ink)
            }
        }
        if (editing) DriverDialog(driver, onDismiss = { editing = false })
        val status = when {
            activeStart != null -> "운행 기록 중 · 출발 계기판 ${odo(activeStart)}km"
            lastOdo != null -> "주차 중 · 계기판 ${odo(lastOdo)}km"
            else -> "차량 신호를 기다리는 중"
        }
        Text("${month.year}년 ${month.monthValue}월 업무용 승용차 운행기록부 · $status",
            style = MaterialTheme.typography.bodyMedium, color = Palette.muted)
    }
}

@Composable
private fun DriverDialog(driver: TripStore.Driver, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(driver.name) }
    var dept by remember { mutableStateOf(driver.department) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("운행기록부 사용자", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("국세청 양식의 부서·성명 칸에 들어갑니다.", style = MaterialTheme.typography.bodyMedium, color = Palette.muted)
                OutlinedTextField(name, { name = it }, label = { Text("성명") }, singleLine = true, textStyle = MaterialTheme.typography.bodyLarge)
                OutlinedTextField(dept, { dept = it }, label = { Text("부서(없으면 비워 두세요)") }, singleLine = true, textStyle = MaterialTheme.typography.bodyLarge)
            }
        },
        confirmButton = {
            Button(onClick = { TripStore.saveDriver(name.trim().ifEmpty { "대표" }, dept.trim()); onDismiss() }) {
                Text("저장", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("취소", style = MaterialTheme.typography.labelLarge) } },
    )
}

@Composable
private fun Summary(monthTrips: List<Trip>) {
    val s = summarize(monthTrips)
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Stat("운행", "${s.tripCount}건", Modifier.weight(1f))
        Stat("총 주행", "${kmShort(s.totalKm)}km", Modifier.weight(1f))
        Stat("업무 사용", "${kmShort(s.businessUseKm)}km", Modifier.weight(1f))
        Stat("업무사용비율", String.format(Locale.KOREAN, "%.0f%%", s.businessUseRatio * 100), Modifier.weight(1f), highlight = true)
    }
    if (s.unconfirmedCount > 0) {
        Text("추천으로 계산한 운행 ${s.unconfirmedCount}건은 확인이 필요해요.",
            style = MaterialTheme.typography.bodyMedium, color = Palette.lane)
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier, highlight: Boolean = false) {
    Surface(
        modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (highlight) Palette.sign else Palette.surface,
        border = if (highlight) null else BorderStroke(1.dp, Palette.line),
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = if (highlight) Palette.signSoft else Palette.muted)
            Text(value, style = MaterialTheme.typography.headlineSmall, maxLines = 1, softWrap = false,
                color = if (highlight) androidx.compose.ui.graphics.Color.White else Palette.ink)
        }
    }
}

@Composable
private fun PendingCard(trip: Trip) {
    Surface(shape = RoundedCornerShape(14.dp), color = Palette.surface, border = BorderStroke(2.dp, Palette.sign)) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("이번 운행, 어떻게 기록할까요?", style = MaterialTheme.typography.headlineSmall)
            Text("${dayFmt.format(trip.startMs)} ${timeFmt.format(trip.startMs)}–${timeFmt.format(trip.endMs)} · ${km(trip.distanceKm)}km",
                style = MaterialTheme.typography.bodyLarge)
            Text("계기판 ${odo(trip.startOdoKm)} → ${odo(trip.endOdoKm)}km", style = MaterialTheme.typography.bodyMedium, color = Palette.muted)
            Text("말로 해도 돼요: \"오토장부, 업무로 기록해. ○○현장 다녀왔어\"",
                style = MaterialTheme.typography.bodyMedium, color = Palette.sign)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val order = listOf(trip.suggested) + Category.entries.filter { it != trip.suggested }
                order.forEach { c ->
                    val suggested = c == trip.suggested
                    Button(
                        onClick = { TripStore.classify(trip.id, c) },
                        modifier = Modifier.weight(1f).heightIn(min = 76.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = if (suggested) ButtonDefaults.buttonColors()
                        else ButtonDefaults.buttonColors(containerColor = Palette.signSoft, contentColor = Palette.ink),
                    ) {
                        Text(if (suggested) "${c.label} (추천)" else c.label, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun AllDone() {
    Surface(shape = RoundedCornerShape(14.dp), color = Palette.signSoft) {
        Text("모든 운행을 분류했어요. 다음 운행은 주차하면 알림으로 물어볼게요.",
            Modifier.padding(24.dp), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ExportRow(month: YearMonth, monthTrips: List<Trip>) {
    val context = LocalContext.current
    var message by remember { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = {
                val label = "%d-%02d".format(month.year, month.monthValue)
                val driver = TripStore.driver.value
                val csv = LedgerExport.toCsv(monthTrips, userName = driver.name, department = driver.department)
                val dir = context.getExternalFilesDir(null) ?: context.filesDir
                message = "저장했어요: " + LedgerExport.write(dir, label, csv).absolutePath
            },
            modifier = Modifier.heightIn(min = 72.dp),
            shape = RoundedCornerShape(12.dp),
        ) { Text("이번 달 국세청 양식으로 내보내기(엑셀용 CSV)", style = MaterialTheme.typography.labelLarge) }
        message?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = Palette.muted) }
        ShareButton(month, monthTrips)
    }
}

/** 이번 달 장부를 올리고 QR 코드를 띄운다. 휴대폰으로 찍어 엑셀을 받거나 세무사에게 링크를 보낸다. */
@Composable
private fun ShareButton(month: YearMonth, monthTrips: List<Trip>) {
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<ShareState>(ShareState.Idle) }
    Button(
        onClick = {
            state = ShareState.Loading
            val label = "%d-%02d".format(month.year, month.monthValue)
            val body = ShareClient.payload(label, monthTrips, TripStore.driver.value)
            scope.launch {
                val result = withContext(Dispatchers.IO) { ShareClient.upload(body) }
                state = result.fold({ ShareState.Ready(it) }, { ShareState.Failed(it.message ?: "알 수 없는 오류") })
            }
        },
        enabled = state !is ShareState.Loading && monthTrips.isNotEmpty(),
        modifier = Modifier.heightIn(min = 72.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Text(if (state is ShareState.Loading) "보내는 중…" else "휴대폰으로 받기 · 세무사님께 공유", style = MaterialTheme.typography.labelLarge)
    }
    when (val s = state) {
        is ShareState.Failed -> Text("보내지 못했어요. 인터넷 연결을 확인하고 다시 눌러 주세요. (${s.reason})",
            style = MaterialTheme.typography.bodyMedium, color = Palette.lane)
        is ShareState.Ready -> {
            val qr = remember(s.url) { qrBitmap(s.url).asImageBitmap() }
            AlertDialog(
                onDismissRequest = { state = ShareState.Idle },
                title = { Text("휴대폰 카메라로 찍으세요", style = MaterialTheme.typography.headlineSmall) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(qr, contentDescription = "장부 링크 QR 코드", modifier = Modifier.size(360.dp))
                        Text("열린 화면에서 엑셀 파일을 받거나, 링크를 세무사님께 보내면 됩니다. 링크는 60일 동안 열립니다.",
                            style = MaterialTheme.typography.bodyMedium, color = Palette.muted)
                        Text(s.url, style = MaterialTheme.typography.bodyMedium, color = Palette.sign)
                    }
                },
                confirmButton = { Button(onClick = { state = ShareState.Idle }) { Text("닫기", style = MaterialTheme.typography.labelLarge) } },
            )
        }
        else -> Unit
    }
}

private sealed interface ShareState {
    data object Idle : ShareState
    data object Loading : ShareState
    data class Ready(val url: String) : ShareState
    data class Failed(val reason: String) : ShareState
}

@Composable
private fun TripRow(trip: Trip) {
    Surface(shape = RoundedCornerShape(10.dp), color = Palette.surface, border = BorderStroke(1.dp, Palette.line)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${dayFmt.format(trip.startMs)} ${timeFmt.format(trip.startMs)}–${timeFmt.format(trip.endMs)}",
                    style = MaterialTheme.typography.bodyLarge)
                Text("계기판 ${odo(trip.startOdoKm)} → ${odo(trip.endOdoKm)}km",
                    style = MaterialTheme.typography.bodyMedium, color = Palette.muted)
                if (trip.memo.isNotBlank()) {
                    Text("메모: ${trip.memo}", style = MaterialTheme.typography.bodyMedium, color = Palette.sign)
                }
            }
            Text("${km(trip.distanceKm)}km", style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 20.dp))
            CategoryChip(trip)
        }
    }
}

@Composable
private fun CategoryChip(trip: Trip) {
    var open by remember { mutableStateOf(false) }
    Box {
        val confirmed = trip.confirmed
        OutlinedButton(
            onClick = { open = true },
            modifier = Modifier.heightIn(min = 64.dp),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(if (confirmed) 1.dp else 2.dp, if (confirmed) Palette.line else Palette.lane),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = if (confirmed) Palette.signSoft else Palette.laneSoft),
        ) {
            Text(if (confirmed) trip.effectiveCategory.label else "${trip.suggested.label}?",
                style = MaterialTheme.typography.labelLarge, color = Palette.ink)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            Category.entries.forEach { c ->
                DropdownMenuItem(
                    text = { Text(c.label, style = MaterialTheme.typography.bodyLarge) },
                    onClick = { TripStore.classify(trip.id, c); open = false },
                )
            }
        }
    }
}
