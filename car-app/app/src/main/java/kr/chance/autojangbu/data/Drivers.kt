package kr.chance.autojangbu.data

import kotlin.math.abs
import org.json.JSONArray
import org.json.JSONObject

/** 운전석 시트 위치(앞뒤·등받이·높이, 차량 SDK 값). 사람마다 맞춰 앉는 자세가 달라 운전자를 구분하는 단서가 된다. */
data class SeatPose(val foreAft: Int, val backrest: Int, val height: Int) {
    fun distanceTo(o: SeatPose) = abs(foreAft - o.foreAft) + abs(backrest - o.backrest) + abs(height - o.height)

    /** 세 값이 모두 0이면 시트 위치를 못 읽은 것으로 본다(Pleos 에뮬레이터는 늘 0을 돌려준다). */
    val isKnown: Boolean get() = foreAft != 0 || backrest != 0 || height != 0
}

/** 한 차를 여러 사람이 쓸 때의 운전자. 국세청 양식 '사용자(부서·성명)' 칸에 들어간다. */
data class DriverEntry(val name: String, val department: String, val pose: SeatPose) {
    fun toJson(): JSONObject = JSONObject()
        .put("name", name).put("department", department)
        .put("foreAft", pose.foreAft).put("backrest", pose.backrest).put("height", pose.height)

    companion object {
        fun fromJson(o: JSONObject) = DriverEntry(
            o.getString("name"), o.optString("department", ""),
            SeatPose(o.getInt("foreAft"), o.getInt("backrest"), o.getInt("height")),
        )

        fun listToJson(list: List<DriverEntry>) = JSONArray().apply { list.forEach { put(it.toJson()) } }.toString()

        fun listFromJson(text: String?): List<DriverEntry> = runCatching {
            val arr = JSONArray(text ?: "[]")
            (0 until arr.length()).map { fromJson(arr.getJSONObject(it)) }
        }.getOrDefault(emptyList())
    }
}

/**
 * 지금 시트 위치와 가장 가까운 등록 운전자. 차이가 [maxDistance]보다 크면 아무도 아니라고 본다
 * (처음 타는 사람이거나 시트를 크게 바꾼 경우 → 기본 사용자로 기록).
 */
fun matchDriver(pose: SeatPose, drivers: List<DriverEntry>, maxDistance: Int = 12): DriverEntry? {
    if (!pose.isKnown) return null
    return drivers.minByOrNull { it.pose.distanceTo(pose) }?.takeIf { it.pose.distanceTo(pose) <= maxDistance }
}
