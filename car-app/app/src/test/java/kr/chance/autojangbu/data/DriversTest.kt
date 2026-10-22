package kr.chance.autojangbu.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DriversTest {
    private val boss = DriverEntry("대표", "", SeatPose(60, 40, 50))
    private val kim = DriverEntry("김과장", "영업팀", SeatPose(30, 45, 70))

    @Test
    fun `시트 위치가 가장 가까운 운전자를 고른다`() {
        assertEquals(kim, matchDriver(SeatPose(32, 44, 68), listOf(boss, kim)))
        assertEquals(boss, matchDriver(SeatPose(58, 40, 51), listOf(boss, kim)))
    }

    @Test
    fun `많이 다르면 아무도 아니다`() {
        assertNull(matchDriver(SeatPose(90, 10, 10), listOf(boss, kim)))
        assertNull(matchDriver(SeatPose(60, 40, 50), emptyList()))
    }

    @Test
    fun `시트 값을 못 읽으면(모두 0) 아무도 고르지 않는다`() {
        val zero = DriverEntry("영점", "", SeatPose(0, 0, 0))
        assertNull(matchDriver(SeatPose(0, 0, 0), listOf(zero, boss)))
    }

    @Test
    fun `저장 형식을 오간다`() {
        assertEquals(listOf(boss, kim), DriverEntry.listFromJson(DriverEntry.listToJson(listOf(boss, kim))))
        assertEquals(emptyList<DriverEntry>(), DriverEntry.listFromJson("깨진 값"))
    }

    @Test
    fun `사용자 칸은 알아본 운전자 우선`() {
        val profile = TripStore.Profile(name = "대표", department = "")
        val t = Trip(1, 0, 0, 0.0, 10.0, Category.BUSINESS, Category.BUSINESS, driverName = "김과장", driverDept = "영업팀")
        assertEquals("영업팀" to "김과장", LedgerExport.user(t, profile))
        assertEquals("" to "대표", LedgerExport.user(t.copy(driverName = "", driverDept = ""), profile))
    }
}
