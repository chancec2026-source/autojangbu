package kr.chance.autojangbu.ui

import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToLong

// 화면 글자 만들기(안드로이드에 기대지 않아 단위 테스트로 확인한다).

fun km1(v: Double): String = String.format(Locale.KOREAN, "%,.1f", v)

/** 기계식 적산계 숫자판: 정수 6자리 + 소수 첫째 자리. 12,491.96 → ("012492", 0) */
fun odometerDrums(km: Double): Pair<String, Int> {
    val tenths = (km * 10).roundToLong().coerceAtLeast(0)
    return String.format(Locale.US, "%06d", (tenths / 10) % 1_000_000) to (tenths % 10).toInt()
}

/** 번호판처럼 띄운다. "12가3456" → "12가 3456". 모양이 다르면 그대로 둔다. */
fun platePretty(plate: String): String = Regex("""^(.*[가-힣])\s?(\d{4})$""").replace(plate.trim(), "$1 $2")

/** 받침이 있으면 '으로', 없거나 ㄹ 받침이면 '로'. 업무로 · 출퇴근으로 · 개인으로 */
fun withRo(word: String): String {
    val c = word.lastOrNull() ?: return word
    if (c !in '가'..'힣') return word + "로"
    val jong = (c - '가') % 28
    return word + if (jong == 0 || jong == 8) "로" else "으로"
}

/** 목록의 날짜 머리: (10.30, 금) */
fun dayHead(d: LocalDate): Pair<String, String> =
    "${d.monthValue}.${d.dayOfMonth}" to d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)
