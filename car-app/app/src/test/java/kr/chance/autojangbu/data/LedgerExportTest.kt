package kr.chance.autojangbu.data

import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LedgerExportTest {

    private fun at(day: Int, hour: Int) = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, day, hour, 0, 0) }.timeInMillis

    @Test
    fun `국세청 양식 칸 순서와 합계`() {
        val trips = listOf(
            Trip(1, at(22, 8), at(22, 9), 12340.0, 12358.0, Category.COMMUTE, Category.COMMUTE),
            Trip(2, at(22, 14), at(22, 15), 12358.0, 12400.0, Category.BUSINESS, null, memo = "○○아파트 현장"),
            Trip(3, at(25, 11), at(25, 12), 12400.0, 12440.0, Category.PERSONAL, Category.PERSONAL),
        )
        val csv = LedgerExport.toCsv(trips, userName = "대표", department = "")
        val lines = csv.removePrefix("﻿").lines()
        println(csv)
        assertEquals("사용일자(요일),부서,성명,주행 전 계기판의 거리(km),주행 후 계기판의 거리(km),주행거리(km),출·퇴근용(km),일반 업무용(km),비고", lines[0])
        assertTrue(lines[1].startsWith("2026-10-22(목),,대표,12340.0,12358.0,18.0,18.0,0.0,"))
        assertEquals("2026-10-22(목),,대표,12358.0,12400.0,42.0,0.0,42.0,○○아파트 현장 / 구분 확인 필요", lines[2])
        assertTrue(lines[3].contains(",40.0,0.0,0.0,"))
        assertTrue(csv.contains("과세기간 총주행거리(km),100.0"))
        assertTrue(csv.contains("업무용 사용거리(km),60.0"))
        assertTrue(csv.contains("업무사용비율(%),60.0"))
    }
}
