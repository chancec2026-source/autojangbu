package kr.chance.autojangbu.data

import java.util.Calendar
import org.json.JSONObject

/** 국세청 운행기록부 기준 구분. 출퇴근과 일반 업무는 모두 '업무용 사용거리'에 들어간다. */
enum class Category(val label: String, val isBusinessUse: Boolean) {
    BUSINESS("업무", true),
    COMMUTE("출퇴근", true),
    PERSONAL("개인", false),
}

/** 운행 1건 = 출발(D)부터 주차 후 내릴 때까지. 거리는 계기판 값 그대로다. */
data class Trip(
    val id: Long,
    val startMs: Long,
    val endMs: Long,
    val startOdoKm: Double,
    val endOdoKm: Double,
    val suggested: Category,
    val category: Category? = null,
    val memo: String = "",
) {
    val distanceKm: Double get() = (endOdoKm - startOdoKm).coerceAtLeast(0.0)
    val confirmed: Boolean get() = category != null

    /** 사람이 고르기 전에는 추천값으로 계산하고, 화면에 '확인 필요'로 표시한다. */
    val effectiveCategory: Category get() = category ?: suggested

    /** 일반 업무 운행인데 방문처·목적 메모가 없으면 세무조사 때 사적 사용으로 다퉈질 수 있다. */
    val needsPurpose: Boolean get() = effectiveCategory == Category.BUSINESS && memo.isBlank()

    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("startMs", startMs)
        .put("endMs", endMs)
        .put("startOdoKm", startOdoKm)
        .put("endOdoKm", endOdoKm)
        .put("suggested", suggested.name)
        .put("category", category?.name ?: JSONObject.NULL)
        .put("memo", memo)

    companion object {
        fun fromJson(o: JSONObject) = Trip(
            id = o.getLong("id"),
            startMs = o.getLong("startMs"),
            endMs = o.getLong("endMs"),
            startOdoKm = o.getDouble("startOdoKm"),
            endOdoKm = o.getDouble("endOdoKm"),
            suggested = Category.valueOf(o.getString("suggested")),
            category = if (o.isNull("category")) null else Category.valueOf(o.getString("category")),
            memo = o.optString("memo", ""),
        )
    }
}

/** 시간대로 구분을 추천한다. 평일 출근·퇴근 시간대는 출퇴근, 평일 낮은 업무, 주말은 개인. */
fun suggestCategory(startMs: Long): Category {
    val cal = Calendar.getInstance().apply { timeInMillis = startMs }
    val weekend = cal.get(Calendar.DAY_OF_WEEK) in setOf(Calendar.SATURDAY, Calendar.SUNDAY)
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    return when {
        weekend -> Category.PERSONAL
        hour in 6..9 || hour in 17..20 -> Category.COMMUTE
        hour in 10..16 -> Category.BUSINESS
        else -> Category.PERSONAL
    }
}

data class MonthSummary(
    val tripCount: Int,
    val totalKm: Double,
    val commuteKm: Double,
    val businessKm: Double,
    val unconfirmedCount: Int,
    val missingPurposeCount: Int = 0,
) {
    val businessUseKm: Double get() = commuteKm + businessKm
    val businessUseRatio: Double get() = if (totalKm > 0) businessUseKm / totalKm else 0.0
}

fun summarize(trips: List<Trip>): MonthSummary {
    fun km(c: Category) = trips.filter { it.effectiveCategory == c }.sumOf { it.distanceKm }
    return MonthSummary(
        tripCount = trips.size,
        totalKm = trips.sumOf { it.distanceKm },
        commuteKm = km(Category.COMMUTE),
        businessKm = km(Category.BUSINESS),
        unconfirmedCount = trips.count { !it.confirmed },
        missingPurposeCount = trips.count { it.needsPurpose },
    )
}
