package kr.chance.autojangbu.ui

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {
    @Test
    fun `적산계 숫자판은 반올림해서 자리를 올린다`() {
        assertEquals("012492" to 0, odometerDrums(12491.96))
        assertEquals("012468" to 4, odometerDrums(12468.4))
        assertEquals("000000" to 0, odometerDrums(0.0))
        assertEquals("999999" to 9, odometerDrums(999999.94))
    }

    @Test
    fun `번호판은 뒤 네 자리 앞을 띄운다`() {
        assertEquals("12가 3456", platePretty("12가3456"))
        assertEquals("123가 4567", platePretty("123가 4567"))
        assertEquals("서울12가 3456", platePretty("서울12가3456"))
        assertEquals("invalid", platePretty("invalid"))
    }

    @Test
    fun `조사 로와 으로를 받침에 맞춘다`() {
        assertEquals("업무로", withRo("업무"))
        assertEquals("출퇴근으로", withRo("출퇴근"))
        assertEquals("개인으로", withRo("개인"))
        assertEquals("서울로", withRo("서울"))
    }

    @Test
    fun `날짜 머리는 월점일과 요일`() {
        assertEquals("10.30" to "금", dayHead(LocalDate.of(2026, 10, 30)))
    }
}
