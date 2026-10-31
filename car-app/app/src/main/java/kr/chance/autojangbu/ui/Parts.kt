package kr.chance.autojangbu.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kr.chance.autojangbu.data.Category

/** 어두운 판: 바탕색 + 가는 테두리. */
fun Modifier.panel(shape: Shape = RoundedCornerShape(28.dp), color: Color = Palette.panel, line: Color = Palette.line) =
    clip(shape).background(color, shape).border(1.dp, line, shape)

/** 아래쪽 가는 줄(목록 칸 나눔). */
fun Modifier.bottomLine(color: Color = Palette.line) = drawBehind {
    drawLine(color, Offset(0f, size.height - 0.5f), Offset(size.width, size.height - 0.5f), 1.dp.toPx())
}

/** 둘 중 하나 고르기(탭·기간). 바깥 높이 76dp 로 손가락에 맞춘다. */
@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.panel(RoundedCornerShape(22.dp)).padding(6.dp)) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            val shape = RoundedCornerShape(16.dp)
            Box(
                Modifier.height(62.dp).clip(shape)
                    .then(if (on) Modifier.background(Palette.raised, shape).border(1.dp, Palette.line2, shape) else Modifier)
                    .clickable { onSelect(i) }
                    .padding(horizontal = 30.dp),
                contentAlignment = Alignment.Center,
            ) { Text(label, style = kr(24, FontWeight.Bold, if (on) Palette.text else Palette.faint)) }
        }
    }
}

/** 밝은 주 버튼(대화상자의 저장·닫기). */
@Composable
fun PrimaryButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, height: Dp = 72.dp) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = height),
        enabled = enabled,
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Palette.text, contentColor = Palette.bg,
            disabledContainerColor = Palette.raised, disabledContentColor = Palette.faint,
        ),
    ) { Text(label, style = kr(24, FontWeight.Bold)) }
}

/** 테두리만 있는 버튼. */
@Composable
fun GhostButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    color: Color = Palette.sub,
    height: Dp = 72.dp,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = height),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Palette.line2),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
    ) {
        if (icon != null) Icon(icon, null, Modifier.padding(end = 10.dp).size(26.dp))
        Text(label, style = kr(23, FontWeight.Bold))
    }
}

@Composable
fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Palette.text,
    unfocusedTextColor = Palette.text,
    focusedContainerColor = Palette.panel,
    unfocusedContainerColor = Palette.panel,
    focusedBorderColor = Palette.biz,
    unfocusedBorderColor = Palette.line2,
    focusedLabelColor = Palette.biz,
    unfocusedLabelColor = Palette.sub,
    cursorColor = Palette.biz,
)

/** 구분 알약: 점(경고등) + 이름. 확인 전은 노란 점선으로 '업무?' */
@Composable
fun CategoryPill(category: Category, confirmed: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(30.dp)
    if (!confirmed) {
        Box(
            modifier.height(60.dp).clip(shape)
                .drawBehind {
                    val w = 2.dp.toPx()
                    drawRoundRect(
                        Palette.amber.copy(alpha = 0.75f), topLeft = Offset(w / 2, w / 2),
                        size = size.copy(width = size.width - w, height = size.height - w),
                        cornerRadius = CornerRadius(size.height / 2),
                        style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 5.dp.toPx()))),
                    )
                }
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) { Text("${category.label}?", style = kr(22, FontWeight.Bold, Palette.amber)) }
        return
    }
    val (bg, fg, dot) = when (category) {
        Category.BUSINESS -> Triple(Palette.biz.copy(alpha = 0.12f), Color(0xFFA5E2FF), Palette.biz)
        Category.COMMUTE -> Triple(Color(0xFF2F72D6).copy(alpha = 0.2f), Color(0xFFADC8F3), Color(0xFF4C8BEA))
        Category.PERSONAL -> Triple(Palette.per.copy(alpha = 0.24f), Color(0xFFAFBAC7), Color(0xFF6B7B90))
    }
    Row(
        modifier.height(60.dp).clip(shape).background(bg).clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(10.dp)
                .drawBehind {
                    if (category == Category.BUSINESS) drawCircle(dot.copy(alpha = 0.35f), radius = size.minDimension)
                    drawCircle(dot)
                }
        )
        Text(category.label, Modifier.padding(start = 10.dp), style = kr(22, FontWeight.Bold, fg))
    }
}

/** 화면 아래 잠깐 뜨는 알림(저장했어요 등). */
@Composable
fun Notice(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier.panel(RoundedCornerShape(22.dp), Palette.raised, Palette.line2).padding(horizontal = 28.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Glyph.check, null, Modifier.size(26.dp), tint = Palette.biz)
        Text(text, style = kr(22, FontWeight.SemiBold, Palette.text))
    }
}

/** 대화상자 공용 틀: 어두운 판, 제목, (넘치면 굴러가는) 내용, 아래 오른쪽 버튼. */
@Composable
fun DialogFrame(
    title: String,
    onDismiss: () -> Unit,
    width: Dp,
    buttons: @Composable RowScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        CompositionLocalProvider(LocalContentColor provides Palette.sub) {
            val shape = RoundedCornerShape(36.dp)
            Column(
                Modifier.widthIn(max = width).fillMaxWidth().heightIn(max = 1040.dp)
                    .background(Brush.verticalGradient(listOf(Color(0xFF132131), Color(0xFF0F1A26))), shape)
                    .border(1.dp, Palette.line2, shape)
                    .padding(start = 48.dp, end = 48.dp, top = 40.dp, bottom = 36.dp),
            ) {
                Text(title, style = kr(32, FontWeight.Bold, Palette.text))
                Column(
                    Modifier.weight(1f, fill = false).padding(top = 18.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    content = content,
                )
                Row(
                    Modifier.fillMaxWidth().padding(top = 28.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.End),
                    content = buttons,
                )
            }
        }
    }
}
