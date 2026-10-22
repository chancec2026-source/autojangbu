package kr.chance.autojangbu.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.random.Random
import kr.chance.autojangbu.data.Category
import kr.chance.autojangbu.data.Trip
import kr.chance.autojangbu.data.TripStore
import kr.chance.autojangbu.data.suggestCategory

/**
 * 시연 영상용 예시 운행(개발용 빌드에만 들어간다).
 * adb shell am broadcast --user 10 -a kr.chance.autojangbu.DEMO_SEED -p kr.chance.autojangbu --ef odo 12388
 */
class DemoSeedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        TripStore.init(context)
        when (intent.action) {
            "kr.chance.autojangbu.DEMO_CLEAR" -> TripStore.replaceAll(emptyList())
            "kr.chance.autojangbu.DEMO_SEED" -> {
                // 마지막 예시 운행이 지금 차 계기판 값(--ef odo)에서 끝나도록 거리 전체를 옮긴다.
                val trips = sample(LocalDate.now())
                val endOdo = intent.getFloatExtra("odo", -1f).toDouble()
                val shift = if (endOdo > 0) endOdo - trips.last().endOdoKm else 0.0
                TripStore.replaceAll(trips.map { it.copy(startOdoKm = it.startOdoKm + shift, endOdoKm = it.endOdoKm + shift) })
            }
        }
    }

    private fun sample(today: LocalDate): List<Trip> {
        val zone = ZoneId.systemDefault()
        val rnd = Random(42)
        val places = listOf("강남 OO아파트 현장" to 14.0, "성수동 거래처 미팅" to 9.0, "판교 자재 업체" to 22.0,
            "세무사 사무실" to 6.0, "인천 현장 점검" to 38.0)
        var odo = 9_800.0
        val trips = mutableListOf<Trip>()
        fun add(date: LocalDate, at: LocalTime, km: Double, category: Category?, memo: String = "") {
            val start = date.atTime(at).atZone(zone).toInstant().toEpochMilli()
            val end = start + (km * 2.2 * 60_000).toLong()
            trips += Trip(end, start, end, odo, odo + km, suggestCategory(start), category, memo)
            odo += km
        }
        var date = today.withDayOfMonth(1)
        while (date.isBefore(today)) {
            val weekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
            if (weekend) {
                if (rnd.nextInt(3) > 0) add(date, LocalTime.of(11, 0), 20.0 + rnd.nextInt(40), Category.PERSONAL, "")
            } else {
                add(date, LocalTime.of(8, 10), 18.0, Category.COMMUTE)
                repeat(1 + rnd.nextInt(2)) { i ->
                    val (place, km) = places[rnd.nextInt(places.size)]
                    add(date, LocalTime.of(10 + i * 3, 30), km * 2, Category.BUSINESS, place)
                }
                add(date, LocalTime.of(18, 40), 18.0, Category.COMMUTE)
            }
            date = date.plusDays(1)
        }
        // 오늘 아침 운행은 분류 전 상태로 둔다.
        add(today, LocalTime.of(8, 10), 18.0, null)
        return trips
    }
}
