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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.window.DialogProperties
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
import kr.chance.autojangbu.data.DriverEntry
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
            ExportRow(month, monthTrips, trips.filter { it.month().year == month.year })
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
    val profile by TripStore.profile.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("오토장부", style = MaterialTheme.typography.displaySmall, color = Palette.sign, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = { editing = true }, modifier = Modifier.heightIn(min = 64.dp), shape = RoundedCornerShape(10.dp)) {
                val who = listOf(profile.plate, profile.name).filter { it.isNotBlank() }.joinToString(" · ")
                Text("장부 정보: $who", style = MaterialTheme.typography.labelLarge, color = Palette.ink)
            }
        }
        if (editing) ProfileDialog(profile, onDismiss = { editing = false })
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
private fun ProfileDialog(profile: TripStore.Profile, onDismiss: () -> Unit) {
    var p by remember { mutableStateOf(profile) }
    @Composable
    fun field(label: String, value: String, onChange: (String) -> Unit) =
        OutlinedTextField(value, onChange, label = { Text(label) }, singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge, modifier = Modifier.fillMaxWidth())
    AlertDialog(
        onDismissRequest = onDismiss,
        // 차 화면은 넓으므로 기본 폭(좁음) 대신 넓게 쓴다.
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.widthIn(max = 1000.dp).fillMaxWidth(0.62f),
        title = { Text("운행기록부 정보", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("국세청 양식의 머리말과 사용자 칸에 들어갑니다. 자동차등록번호는 차에서 읽어 와요.",
                    style = MaterialTheme.typography.bodyMedium, color = Palette.muted)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.weight(1f)) { field("상호(법인명)", p.company) { p = p.copy(company = it) } }
                    Box(Modifier.weight(1f)) { field("사업자등록번호", p.bizNo) { p = p.copy(bizNo = it) } }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.weight(1f)) { field("차종(예: 그랜저)", p.carModel) { p = p.copy(carModel = it) } }
                    Box(Modifier.weight(1f)) { field("자동차등록번호", p.plate) { p = p.copy(plate = it) } }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.weight(1f)) { field("기본 사용자 성명", p.name) { p = p.copy(name = it) } }
                    Box(Modifier.weight(1f)) { field("부서(선택)", p.department) { p = p.copy(department = it) } }
                }
                DriversSection()
            }
        },
        confirmButton = {
            Button(onClick = {
                TripStore.saveProfile(p.copy(
                    name = p.name.trim().ifEmpty { "대표" }, department = p.department.trim(), company = p.company.trim(),
                    bizNo = p.bizNo.trim(), carModel = p.carModel.trim(), plate = p.plate.trim(),
                ))
                onDismiss()
            }) {
                Text("저장", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("취소", style = MaterialTheme.typography.labelLarge) } },
    )
}

/** 한 차를 여러 사람이 쓸 때: 각자 맞춰 둔 시트 위치를 기억해 출발할 때 운전자를 알아본다. */
@Composable
private fun DriversSection() {
    val drivers by TripStore.drivers.collectAsStateWithLifecycle()
    val pose by TripStore.currentPose.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf("") }
    var dept by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("운전자 여러 명(시트 위치로 자동 구분)", style = MaterialTheme.typography.titleLarge)
        Text("운전자가 자기 자세로 시트를 맞춘 뒤 등록하면, 출발할 때 시트 위치로 누가 운전하는지 알아보고 사용자 칸을 채웁니다.",
            style = MaterialTheme.typography.bodyMedium, color = Palette.muted)
        drivers.forEach { d ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${listOf(d.department, d.name).filter { it.isNotBlank() }.joinToString(" ")}  ·  시트 ${d.pose.foreAft}/${d.pose.backrest}/${d.pose.height}",
                    style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                OutlinedButton(onClick = { TripStore.saveDrivers(drivers - d) }) { Text("지우기") }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(name, { name = it }, label = { Text("운전자 성명") }, singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            OutlinedTextField(dept, { dept = it }, label = { Text("부서") }, singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        }
        val p = pose
        Button(
            onClick = { p?.let { TripStore.saveDrivers(drivers + DriverEntry(name.trim(), dept.trim(), it)); name = ""; dept = "" } },
            enabled = p != null && p.isKnown && name.isNotBlank(),
            modifier = Modifier.heightIn(min = 64.dp),
        ) {
            Text(when {
                p == null -> "시트 위치를 읽는 중…"
                !p.isKnown -> "이 차에서는 시트 위치를 읽을 수 없어요"
                else -> "지금 시트 위치로 등록 (앞뒤 ${p.foreAft} · 등받이 ${p.backrest} · 높이 ${p.height})"
            }, style = MaterialTheme.typography.labelLarge)
        }
    }
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
    if (s.missingPurposeCount > 0) {
        Text("방문 목적이 비어 있는 업무 운행 ${s.missingPurposeCount}건 — 목적을 적어 두면 세무 증빙에 유리해요.",
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
private fun ExportRow(month: YearMonth, monthTrips: List<Trip>, yearTrips: List<Trip>) {
    val context = LocalContext.current
    var message by remember { mutableStateOf<String?>(null) }
    // 업무사용비율은 사업연도 단위로 따지므로 '올해 전체'도 내보낼 수 있게 한다.
    var wholeYear by remember { mutableStateOf(false) }
    val label = if (wholeYear) "${month.year}" else "%d-%02d".format(month.year, month.monthValue)
    val trips = if (wholeYear) yearTrips else monthTrips
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("내보낼 기간", style = MaterialTheme.typography.bodyLarge, color = Palette.muted)
            Tab("이번 달", selected = !wholeYear) { wholeYear = false }
            Tab("올해 전체(사업연도)", selected = wholeYear) { wholeYear = true }
        }
        OutlinedButton(
            onClick = {
                val csv = LedgerExport.toCsv(trips, TripStore.profile.value, ShareClient.periodText(label))
                val dir = context.getExternalFilesDir(null) ?: context.filesDir
                message = "저장했어요: " + LedgerExport.write(dir, label, csv).absolutePath
            },
            modifier = Modifier.heightIn(min = 72.dp),
            shape = RoundedCornerShape(12.dp),
        ) { Text("국세청 양식으로 내보내기(엑셀용 CSV)", style = MaterialTheme.typography.labelLarge) }
        message?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = Palette.muted) }
        ShareButton(label, trips)
    }
}

/** 장부를 올리고 QR 코드를 띄운다. 휴대폰으로 찍어 엑셀을 받거나 세무사에게 링크를 보낸다. */
@Composable
private fun ShareButton(label: String, trips: List<Trip>) {
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<ShareState>(ShareState.Idle) }
    Button(
        onClick = {
            state = ShareState.Loading
            val body = ShareClient.payload(label, trips, TripStore.profile.value)
            scope.launch {
                val result = withContext(Dispatchers.IO) { ShareClient.upload(body) }
                state = result.fold({ ShareState.Ready(it) }, { ShareState.Failed(it.message ?: "알 수 없는 오류") })
            }
        },
        enabled = state !is ShareState.Loading && trips.isNotEmpty(),
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
                val driver = if (trip.driverName.isNotBlank()) " · 운전 ${trip.driverName}" else ""
                Text("계기판 ${odo(trip.startOdoKm)} → ${odo(trip.endOdoKm)}km$driver",
                    style = MaterialTheme.typography.bodyMedium, color = Palette.muted)
                if (trip.memo.isNotBlank()) {
                    Text("메모: ${trip.memo}", style = MaterialTheme.typography.bodyMedium, color = Palette.sign)
                } else if (trip.needsPurpose) {
                    Text("방문 목적을 적어 두세요", style = MaterialTheme.typography.bodyMedium, color = Palette.lane)
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
        var editingMemo by remember { mutableStateOf(false) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            Category.entries.forEach { c ->
                DropdownMenuItem(
                    text = { Text(c.label, style = MaterialTheme.typography.bodyLarge) },
                    onClick = { TripStore.classify(trip.id, c); open = false },
                )
            }
            DropdownMenuItem(
                text = { Text("메모(방문 목적) 쓰기", style = MaterialTheme.typography.bodyLarge, color = Palette.sign) },
                onClick = { open = false; editingMemo = true },
            )
        }
        if (editingMemo) MemoDialog(trip, onDismiss = { editingMemo = false })
    }
}

@Composable
private fun MemoDialog(trip: Trip, onDismiss: () -> Unit) {
    var memo by remember { mutableStateOf(trip.memo) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("방문처·목적", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${dayFmt.format(trip.startMs)} ${timeFmt.format(trip.startMs)} · ${km(trip.distanceKm)}km 운행의 비고 칸에 들어갑니다.",
                    style = MaterialTheme.typography.bodyMedium, color = Palette.muted)
                OutlinedTextField(memo, { memo = it }, label = { Text("예: 강남 OO아파트 현장 점검") }, singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = { TripStore.setMemo(trip.id, memo.trim()); onDismiss() }) { Text("저장", style = MaterialTheme.typography.labelLarge) }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("취소", style = MaterialTheme.typography.labelLarge) } },
    )
}
