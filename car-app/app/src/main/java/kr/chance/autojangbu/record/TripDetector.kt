package kr.chance.autojangbu.record

/**
 * 차량 신호로 운행 1건의 시작과 끝을 판단한다. 차량 SDK와 떨어져 있어 단위 테스트로 검증한다.
 *
 * - 시작: 기어가 P가 아닌 값(D·R·N)으로 바뀐 순간의 계기판 거리
 * - 끝: P에 둔 상태에서 운전석 문이 열리거나, P로 [parkTimeoutMs] 이상 머문 순간
 * - P에 잠깐 섰다가(드라이브스루 등) 문을 열지 않고 다시 출발하면 같은 운행으로 이어 간다
 */
class TripDetector(private val parkTimeoutMs: Long = 3 * 60_000L) {

    data class Draft(val startOdoKm: Double, val endOdoKm: Double, val startMs: Long, val endMs: Long)

    var startOdoKm: Double? = null
        private set
    var startMs: Long = 0
        private set
    private var moving = false
    private var parkedSinceMs: Long? = null

    val inTrip: Boolean get() = moving || startOdoKm != null

    /** 서비스가 다시 켜졌을 때 저장해 둔 진행 중 운행을 이어 받는다. */
    fun restore(startOdoKm: Double, startMs: Long) {
        this.startOdoKm = startOdoKm
        this.startMs = startMs
        moving = true
    }

    fun onGear(isPark: Boolean, odoKm: Double?, nowMs: Long) {
        if (!isPark) {
            if (!moving && startOdoKm == null) startMs = nowMs
            moving = true
            parkedSinceMs = null
            if (startOdoKm == null && odoKm != null) startOdoKm = odoKm
        } else if (moving) {
            moving = false
            parkedSinceMs = nowMs
        }
    }

    /** 출발 순간에 계기판 값이 아직 없었으면 처음 들어온 값을 출발 거리로 쓴다. */
    fun onOdometer(odoKm: Double) {
        if (moving && startOdoKm == null) startOdoKm = odoKm
    }

    fun onDriverDoorOpened(odoKm: Double?, nowMs: Long): Draft? =
        if (parkedSinceMs != null) finish(odoKm) else null

    fun onTick(odoKm: Double?, nowMs: Long): Draft? {
        val parked = parkedSinceMs ?: return null
        return if (nowMs - parked >= parkTimeoutMs) finish(odoKm) else null
    }

    private fun finish(odoKm: Double?): Draft? {
        val start = startOdoKm
        val end = odoKm ?: start
        val draft = if (start != null && end != null) Draft(start, end, startMs, parkedSinceMs ?: startMs) else null
        startOdoKm = null
        parkedSinceMs = null
        moving = false
        // 기어만 바꿨다 돌아온 경우처럼 0.1km 미만은 운행으로 치지 않는다.
        return draft?.takeIf { it.endOdoKm - it.startOdoKm >= 0.1 }
    }
}
