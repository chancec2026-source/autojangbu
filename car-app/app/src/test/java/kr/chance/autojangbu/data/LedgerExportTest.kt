package kr.chance.autojangbu.data

import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LedgerExportTest {

    private fun at(day: Int, hour: Int) = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, day, hour, 0, 0) }.timeInMillis

    private val profile = TripStore.Profile(name = "대표", company = "예시상사", bizNo = "000-00-00000", carModel = "그랜저", plate = "12가3456")

    private val trips = listOf(
        Trip(1, at(22, 8), at(22, 9), 12340.0, 12358.0, Category.COMMUTE, Category.COMMUTE),
        Trip(2, at(22, 14), at(22, 15), 12358.0, 12400.0, Category.BUSINESS, null, memo = "○○아파트 현장"),
        Trip(3, at(25, 11), at(25, 12), 12400.0, 12440.0, Category.PERSONAL, Category.PERSONAL),
        Trip(4, at(26, 11), at(26, 12), 12440.0, 12450.0, Category.BUSINESS, Category.BUSINESS),
    )

    @Test
    fun `머리말에 차종과 자동차등록번호가 들어간다`() {
        val lines = LedgerExport.toCsv(trips, profile, "2026년 10월").removePrefix("﻿").lines()
        assertEquals("업무용승용차 운행기록부", lines[0])
        assertEquals("과세기간,2026년 10월", lines[1])
        assertEquals("상호(법인명),예시상사", lines[2])
        assertEquals("사업자등록번호,000-00-00000", lines[3])
        assertEquals("차종,그랜저", lines[4])
        assertEquals("자동차등록번호,12가3456", lines[5])
    }

    @Test
    fun `국세청 양식 칸 순서와 합계`() {
        val csv = LedgerExport.toCsv(trips, profile, "2026년 10월")
        val lines = csv.removePrefix("﻿").lines()
        val header = lines.indexOf("사용일자(요일),부서,성명,주행 전 계기판의 거리(km),주행 후 계기판의 거리(km),주행거리(km),출·퇴근용(km),일반 업무용(km),비고")
        assertTrue(header > 0)
        assertTrue(lines[header + 1].startsWith("2026-10-22(목),,대표,12340.0,12358.0,18.0,18.0,0.0,"))
        assertEquals("2026-10-22(목),,대표,12358.0,12400.0,42.0,0.0,42.0,○○아파트 현장 / 구분 확인 필요", lines[header + 2])
        assertTrue(lines[header + 3].contains(",40.0,0.0,0.0,"))
        assertTrue(csv.contains("과세기간 총주행거리(km),110.0"))
        assertTrue(csv.contains("과세기간 업무용 사용거리(km),70.0"))
        assertTrue(csv.contains("업무사용비율(%),63.6"))
    }

    @Test
    fun `번호판처럼 생긴 값만 차량번호로 받는다`() {
        assertTrue(TripStore.isPlausiblePlate("12가3456"))
        assertTrue(TripStore.isPlausiblePlate("123가4567"))
        assertTrue(TripStore.isPlausiblePlate("서울12가3456"))
        assertTrue(!TripStore.isPlausiblePlate("invalid"))
        assertTrue(!TripStore.isPlausiblePlate(""))
    }

    @Test
    fun `목적 없는 업무 운행은 비고에 표시한다`() {
        val csv = LedgerExport.toCsv(trips, profile, "2026년 10월")
        assertTrue(csv.lines().any { it.startsWith("2026-10-26(월)") && it.endsWith("방문 목적 미기재") })
        assertEquals(1, summarize(trips).missingPurposeCount)
    }
}
