package kr.chance.autojangbu.data

import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 국세청 업무용승용차 운행기록부 형식으로 CSV를 만든다(엑셀에서 바로 열리도록 UTF-8 BOM).
 * 머리말: 과세기간·상호·사업자등록번호·차종·자동차등록번호 / 표: 사용일자·사용자·운행내역·업무용 사용거리·비고 /
 * 끝: 총주행거리·업무용 사용거리·업무사용비율.
 */
object LedgerExport {
    private val day = SimpleDateFormat("yyyy-MM-dd(E)", Locale.KOREAN)

    /** 비고: 사용자 메모 + 확인이 필요한 사항. */
    fun note(t: Trip): String = listOf(
        t.memo,
        if (t.needsPurpose) "방문 목적 미기재" else "",
        if (!t.confirmed) "구분 확인 필요" else "",
    ).filter { it.isNotBlank() }.joinToString(" / ")

    /** 운행의 사용자(부서, 성명): 시트 위치로 알아본 운전자가 있으면 그 사람, 없으면 기본 사용자. */
    fun user(t: Trip, profile: TripStore.Profile): Pair<String, String> =
        if (t.driverName.isNotBlank()) t.driverDept to t.driverName else profile.department to profile.name

    fun toCsv(trips: List<Trip>, profile: TripStore.Profile, periodLabel: String): String {
        val sb = StringBuilder("﻿")
        sb.appendLine("업무용승용차 운행기록부")
        sb.appendLine("과세기간,${csv(periodLabel)}")
        sb.appendLine("상호(법인명),${csv(profile.company)}")
        sb.appendLine("사업자등록번호,${csv(profile.bizNo)}")
        sb.appendLine("차종,${csv(profile.carModel)}")
        sb.appendLine("자동차등록번호,${csv(profile.plate)}")
        sb.appendLine()
        sb.appendLine("사용일자(요일),부서,성명,주행 전 계기판의 거리(km),주행 후 계기판의 거리(km),주행거리(km),출·퇴근용(km),일반 업무용(km),비고")
        trips.sortedBy { it.startMs }.forEach { t ->
            val c = t.effectiveCategory
            val commute = if (c == Category.COMMUTE) t.distanceKm else 0.0
            val business = if (c == Category.BUSINESS) t.distanceKm else 0.0
            val (dept, name) = user(t, profile)
            sb.appendLine(
                listOf(day.format(t.startMs), dept, name, km(t.startOdoKm), km(t.endOdoKm),
                    km(t.distanceKm), km(commute), km(business), note(t)).joinToString(",") { csv(it) }
            )
        }
        val s = summarize(trips)
        sb.appendLine()
        sb.appendLine("과세기간 총주행거리(km),${km(s.totalKm)}")
        sb.appendLine("과세기간 업무용 사용거리(km),${km(s.businessUseKm)}")
        sb.appendLine("업무사용비율(%),${String.format(Locale.US, "%.1f", s.businessUseRatio * 100)}")
        return sb.toString()
    }

    fun write(dir: File, fileLabel: String, csv: String): File =
        File(dir, "운행기록부_$fileLabel.csv").apply { writeText(csv) }

    private fun km(v: Double) = String.format(Locale.US, "%.1f", v)
    private fun csv(v: String) = if (v.contains(',') || v.contains('"')) "\"" + v.replace("\"", "\"\"") + "\"" else v
}
