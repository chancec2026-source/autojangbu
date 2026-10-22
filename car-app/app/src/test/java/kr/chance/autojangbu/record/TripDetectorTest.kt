package kr.chance.autojangbu.record

import kr.chance.autojangbu.data.Category
import kr.chance.autojangbu.data.Trip
import kr.chance.autojangbu.data.summarize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TripDetectorTest {

    @Test
    fun `D로 출발해 P로 주차하고 문을 열면 운행 1건`() {
        val d = TripDetector()
        d.onGear(isPark = true, odoKm = 12340.0, nowMs = 0)
        d.onGear(isPark = false, odoKm = 12340.0, nowMs = 1_000)
        d.onOdometer(12350.0)
        d.onGear(isPark = true, odoKm = 12358.0, nowMs = 60_000)
        val trip = d.onDriverDoorOpened(12358.0, 61_000)
        assertNotNull(trip)
        assertEquals(12340.0, trip!!.startOdoKm, 0.0)
        assertEquals(12358.0, trip.endOdoKm, 0.0)
        assertEquals(1_000L, trip.startMs)
        assertEquals(60_000L, trip.endMs)
    }

    @Test
    fun `P에 잠깐 섰다가 문 안 열고 다시 출발하면 같은 운행`() {
        val d = TripDetector()
        d.onGear(false, 100.0, 0)
        d.onGear(true, 105.0, 10_000)
        d.onGear(false, 105.0, 30_000)
        d.onGear(true, 110.0, 60_000)
        val trip = d.onDriverDoorOpened(110.0, 61_000)!!
        assertEquals(100.0, trip.startOdoKm, 0.0)
        assertEquals(110.0, trip.endOdoKm, 0.0)
    }

    @Test
    fun `주행 중 문 신호는 무시`() {
        val d = TripDetector()
        d.onGear(false, 100.0, 0)
        assertNull(d.onDriverDoorOpened(102.0, 5_000))
    }

    @Test
    fun `P로 3분 지나면 문을 안 열어도 끝`() {
        val d = TripDetector(parkTimeoutMs = 180_000)
        d.onGear(false, 100.0, 0)
        d.onGear(true, 120.0, 60_000)
        assertNull(d.onTick(120.0, 200_000))
        assertNotNull(d.onTick(120.0, 240_000))
    }

    @Test
    fun `기어만 바꿨다 돌아오면 운행 아님`() {
        val d = TripDetector()
        d.onGear(false, 100.0, 0)
        d.onGear(true, 100.0, 5_000)
        assertNull(d.onDriverDoorOpened(100.0, 6_000))
    }

    @Test
    fun `출발 때 계기판 값이 없으면 처음 들어온 값으로`() {
        val d = TripDetector()
        d.onGear(false, null, 0)
        d.onOdometer(500.0)
        d.onGear(true, 512.0, 10_000)
        assertEquals(500.0, d.onDriverDoorOpened(512.0, 11_000)!!.startOdoKm, 0.0)
    }

    @Test
    fun `업무사용비율은 출퇴근과 일반 업무를 합친다`() {
        fun t(id: Long, km: Double, c: Category) = Trip(id, 0, 0, 0.0, km, c, c)
        val s = summarize(listOf(t(1, 20.0, Category.COMMUTE), t(2, 30.0, Category.BUSINESS), t(3, 50.0, Category.PERSONAL)))
        assertEquals(100.0, s.totalKm, 0.0)
        assertEquals(50.0, s.businessUseKm, 0.0)
        assertEquals(0.5, s.businessUseRatio, 1e-9)
    }
}
