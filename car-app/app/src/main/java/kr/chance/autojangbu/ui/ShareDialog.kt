package kr.chance.autojangbu.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kr.chance.autojangbu.data.Trip
import kr.chance.autojangbu.data.TripStore
import kr.chance.autojangbu.share.ShareClient
import kr.chance.autojangbu.share.qrBitmap

private sealed interface ShareState {
    data object Loading : ShareState
    data object Empty : ShareState
    data class Ready(val url: String) : ShareState
    data class Failed(val reason: String) : ShareState
}

/**
 * 장부를 올리고 QR 코드를 띄운다. 휴대폰으로 찍어 엑셀을 받거나 세무사에게 링크를 보낸다.
 * 기간(이번 달 · 올해 전체)을 바꾸면 그 기간으로 다시 올린다.
 */
@Composable
fun ShareDialog(month: YearMonth, allTrips: List<Trip>, profile: TripStore.Profile, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var wholeYear by remember { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }
    var state by remember { mutableStateOf<ShareState>(ShareState.Loading) }
    var saved by remember { mutableStateOf<String?>(null) }
    // 업무사용비율은 사업연도 단위로 따지므로 '올해 전체'도 보낼 수 있게 한다.
    val label = if (wholeYear) "${month.year}" else monthLabel(month)
    val trips = remember(allTrips, label) { allTrips.filter { if (wholeYear) it.month().year == month.year else it.month() == month } }
    LaunchedEffect(label, attempt) {
        if (trips.isEmpty()) { state = ShareState.Empty; return@LaunchedEffect }
        state = ShareState.Loading
        val body = ShareClient.payload(label, trips, profile)
        val result = withContext(Dispatchers.IO) { ShareClient.upload(body) }
        state = result.fold({ ShareState.Ready(it) }, { ShareState.Failed(it.message ?: "알 수 없는 오류") })
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        CompositionLocalProvider(LocalContentColor provides Palette.text) {
            val shape = RoundedCornerShape(36.dp)
            Column(
                Modifier.width(820.dp)
                    .background(Brush.verticalGradient(listOf(Color(0xFF132131), Color(0xFF0F1A26))), shape)
                    .border(1.dp, Palette.line2, shape)
                    .padding(start = 52.dp, end = 52.dp, top = 44.dp, bottom = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("휴대폰 카메라로 찍으세요", style = kr(36, FontWeight.Bold))
                Text(
                    "열린 화면에서 엑셀 파일을 받거나, 링크를 세무사님께 보내면 됩니다.",
                    Modifier.padding(top = 10.dp),
                    style = kr(22, color = Palette.sub),
                    textAlign = TextAlign.Center,
                )
                Segmented(
                    listOf("${month.monthValue}월 장부", "${month.year}년 전체"),
                    if (wholeYear) 1 else 0,
                    { wholeYear = it == 1 },
                    Modifier.padding(top = 26.dp),
                )
                QrBox(state, onRetry = { attempt++ }, Modifier.padding(top = 30.dp))
                val ready = state as? ShareState.Ready
                Row(Modifier.padding(top = 28.dp).alpha(if (ready != null) 1f else 0f), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Glyph.link, null, Modifier.size(22.dp), tint = Palette.sub)
                    Text(
                        ready?.url?.removePrefix("https://")?.let { if (it.length > 50) it.take(46) + "…" else it } ?: " ",
                        Modifier.padding(start = 8.dp),
                        style = num(20, FontWeight.Medium, Palette.sub),
                        maxLines = 1,
                    )
                }
                Text(
                    "${LocalDate.now().plusDays(60).let { "${it.monthValue}월 ${it.dayOfMonth}일" }}까지 열려요 (60일)",
                    Modifier.padding(top = 6.dp).alpha(if (ready != null) 1f else 0f),
                    style = kr(20, FontWeight.SemiBold, Palette.faint),
                )
                Row(Modifier.fillMaxWidth().padding(top = 30.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    GhostButton("엑셀 파일 저장", { saved = saveCsv(context, label, trips, profile) }, Modifier.weight(1f), Glyph.down, height = 84.dp)
                    PrimaryButton("닫기", onDismiss, Modifier.weight(1f), height = 84.dp)
                }
                // 창 뒤 화면은 어둡게 가려지므로 저장 확인은 창 안에 띄운다.
                saved?.let {
                    Row(Modifier.padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Glyph.check, null, Modifier.size(24.dp), tint = Palette.biz)
                        Text(it, Modifier.padding(start = 10.dp), style = kr(21, FontWeight.SemiBold, Palette.sub))
                    }
                }
            }
        }
    }
}

/** 흰 QR 칸과 네 귀퉁이의 하늘색 조준선. */
@Composable
private fun QrBox(state: ShareState, onRetry: () -> Unit, modifier: Modifier) {
    Box(modifier.size(456.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val l = 46.dp.toPx()
            val r = 18.dp.toPx()
            val w = 4.dp.toPx()
            val corner = Path().apply {
                moveTo(w / 2, l)
                lineTo(w / 2, r)
                quadraticBezierTo(w / 2, w / 2, r, w / 2)
                lineTo(l, w / 2)
            }
            listOf(1f to 1f, -1f to 1f, 1f to -1f, -1f to -1f).forEach { (sx, sy) ->
                scale(sx, sy, pivot = center) {
                    drawPath(corner, Palette.biz.copy(alpha = 0.35f), style = Stroke(w * 3, cap = StrokeCap.Round))
                    drawPath(corner, Palette.biz, style = Stroke(w, cap = StrokeCap.Round))
                }
            }
        }
        Box(
            Modifier.size(420.dp).background(Color.White, RoundedCornerShape(26.dp)).padding(26.dp),
            contentAlignment = Alignment.Center,
        ) {
            val ink = Color(0xFF0A1119)
            when (state) {
                is ShareState.Ready -> {
                    val qr = remember(state.url) { qrBitmap(state.url).asImageBitmap() }
                    Image(qr, "장부 링크 QR 코드", Modifier.fillMaxSize())
                }
                ShareState.Loading -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(Modifier.size(64.dp), color = ink, strokeWidth = 5.dp)
                    Text("링크를 만드는 중…", Modifier.padding(top = 20.dp), style = kr(22, FontWeight.SemiBold, Palette.faint))
                }
                ShareState.Empty -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("이 기간에는 운행이 없어요", style = kr(25, FontWeight.Bold, ink), textAlign = TextAlign.Center)
                    Text("다른 기간을 고르거나 운행을 기록한 뒤 보내 주세요.", Modifier.padding(top = 10.dp), style = kr(20, color = Palette.faint), textAlign = TextAlign.Center)
                }
                is ShareState.Failed -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Glyph.warn, null, Modifier.size(48.dp), tint = Color(0xFFC98A12))
                    Text("보내지 못했어요", Modifier.padding(top = 12.dp), style = kr(26, FontWeight.Bold, ink))
                    Text(
                        "인터넷 연결을 확인하고 다시 보내 주세요.\n(${state.reason})",
                        Modifier.padding(top = 8.dp),
                        style = kr(19, color = Palette.faint),
                        textAlign = TextAlign.Center,
                    )
                    Box(
                        Modifier.padding(top = 18.dp).clip(RoundedCornerShape(18.dp)).background(ink)
                            .clickable(onClick = onRetry).padding(horizontal = 30.dp, vertical = 16.dp)
                    ) { Text("다시 보내기", style = kr(22, FontWeight.Bold, Color.White)) }
                }
            }
        }
    }
}
