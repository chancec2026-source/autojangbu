package kr.chance.autojangbu.data

import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 국세청 업무용승용차 운행기록부 칸 순서로 CSV를 만든다(엑셀에서 바로 열리도록 UTF-8 BOM).
 * 칸 구성은 세무사 감수 전 초안이다.
 */
object LedgerExport {
    private val day = SimpleDateFormat("yyyy-MM-dd(E)", Locale.KOREAN)

    fun toCsv(trips: List<Trip>, userName: String, department: String): String {
        val sb = StringBuilder("﻿")
        sb.appendLine("사용일자(요일),부서,성명,주행 전 계기판의 거리(km),주행 후 계기판의 거리(km),주행거리(km),출·퇴근용(km),일반 업무용(km),비고")
        trips.sortedBy { it.startMs }.forEach { t ->
            val c = t.effectiveCategory
            val commute = if (c == Category.COMMUTE) t.distanceKm else 0.0
            val business = if (c == Category.BUSINESS) t.distanceKm else 0.0
            val note = listOf(t.memo, if (!t.confirmed) "구분 확인 필요" else "").filter { it.isNotBlank() }.joinToString(" / ")
            sb.appendLine(
                listOf(day.format(t.startMs), department, userName, km(t.startOdoKm), km(t.endOdoKm),
                    km(t.distanceKm), km(commute), km(business), note).joinToString(",") { csv(it) }
            )
        }
        val s = summarize(trips)
        sb.appendLine()
        sb.appendLine("과세기간 총주행거리(km),${km(s.totalKm)}")
        sb.appendLine("업무용 사용거리(km),${km(s.businessUseKm)}")
        sb.appendLine("업무사용비율(%),${String.format(Locale.US, "%.1f", s.businessUseRatio * 100)}")
        return sb.toString()
    }

    fun write(dir: File, monthLabel: String, csv: String): File =
        File(dir, "운행기록부_$monthLabel.csv").apply { writeText(csv) }

    private fun km(v: Double) = String.format(Locale.US, "%.1f", v)
    private fun csv(v: String) = if (v.contains(',') || v.contains('"')) "\"" + v.replace("\"", "\"\"") + "\"" else v
}
