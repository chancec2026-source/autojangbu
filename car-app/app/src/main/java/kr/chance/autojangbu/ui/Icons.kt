package kr.chance.autojangbu.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** 선 아이콘(24 격자, 둥근 끝). 시안의 SVG 경로를 그대로 옮겼다. */
object Glyph {
    private fun stroke(name: String, vararg paths: String, width: Float = 24f, height: Float = 24f, line: Float = 2f): ImageVector =
        ImageVector.Builder(name, width.dp, height.dp, width, height).apply {
            paths.forEach {
                addPath(
                    pathData = addPathNodes(it),
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = line,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()

    val left = stroke("left", "M15 5l-7 7 7 7", line = 2.4f)
    val right = stroke("right", "M9 5l7 7-7 7", line = 2.4f)
    val mic = stroke("mic", "M9 6a3 3 0 0 1 6 0v5a3 3 0 0 1 -6 0z", "M5.5 11a6.5 6.5 0 0 0 13 0M12 17.5V21")
    val qr = stroke(
        "qr",
        "M5 4h4a1 1 0 0 1 1 1v4a1 1 0 0 1 -1 1h-4a1 1 0 0 1 -1 -1v-4a1 1 0 0 1 1 -1z",
        "M15 4h4a1 1 0 0 1 1 1v4a1 1 0 0 1 -1 1h-4a1 1 0 0 1 -1 -1v-4a1 1 0 0 1 1 -1z",
        "M5 14h4a1 1 0 0 1 1 1v4a1 1 0 0 1 -1 1h-4a1 1 0 0 1 -1 -1v-4a1 1 0 0 1 1 -1z",
        "M14 14h2v2h-2zM18 18h2v2h-2zM14 18h2M18 14h2",
    )
    val down = stroke("down", "M12 4v11M7 10.5l5 5 5-5M5 20h14")
    val pencil = stroke("pencil", "M4 20h4L19 9a2.1 2.1 0 0 0-3-3L5 17v3zM14 7l3 3", line = 2.2f)
    val warn = stroke("warn", "M12 3.8 2.6 19.6h18.8z", "M12 10v4.4M12 17.2v.3", line = 2.2f)
    val check = stroke("check", "M5 12.5l4.5 4.5L19 7.5", line = 2.4f)
    val link = stroke("link", "M10 14a4 4 0 0 0 5.7 0l3-3a4 4 0 0 0-5.7-5.7l-1 1M14 10a4 4 0 0 0-5.7 0l-3 3a4 4 0 0 0 5.7 5.7l1-1")

    /** 계기판 거리 사이의 가는 화살표(16×10). */
    val arrow = stroke("arrow", "M1 5h13M10 1.5 14 5l-4 3.5", width = 16f, height = 10f, line = 1.6f)
}
