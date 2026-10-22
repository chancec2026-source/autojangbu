package kr.chance.autojangbu.share

import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import kr.chance.autojangbu.BuildConfig
import kr.chance.autojangbu.data.Category
import kr.chance.autojangbu.data.LedgerExport
import kr.chance.autojangbu.data.Trip
import kr.chance.autojangbu.data.TripStore
import kr.chance.autojangbu.data.summarize
import org.json.JSONArray
import org.json.JSONObject

/** 이번 달 장부를 공유 서버에 올리고 휴대폰·세무사용 링크를 받는다. 위치 정보는 보내지 않는다. */
object ShareClient {
    private val day = SimpleDateFormat("yyyy-MM-dd(E)", Locale.KOREAN)

    fun payload(monthLabel: String, trips: List<Trip>, driver: TripStore.Driver): JSONObject {
        val sorted = trips.sortedBy { it.startMs }
        val s = summarize(sorted)
        val rows = JSONArray()
        sorted.forEach { t ->
            val c = t.effectiveCategory
            rows.put(JSONObject()
                .put("date", day.format(t.startMs))
                .put("startKm", t.startOdoKm)
                .put("endKm", t.endOdoKm)
                .put("km", t.distanceKm)
                .put("commuteKm", if (c == Category.COMMUTE) t.distanceKm else 0.0)
                .put("businessKm", if (c == Category.BUSINESS) t.distanceKm else 0.0)
                .put("note", listOf(t.memo, if (!t.confirmed) "구분 확인 필요" else "").filter { it.isNotBlank() }.joinToString(" / ")))
        }
        return JSONObject()
            .put("month", monthLabel)
            .put("driver", JSONObject().put("name", driver.name).put("department", driver.department))
            .put("summary", JSONObject()
                .put("tripCount", s.tripCount)
                .put("totalKm", s.totalKm)
                .put("businessUseKm", s.businessUseKm)
                .put("businessUseRatio", s.businessUseRatio)
                .put("unconfirmedCount", s.unconfirmedCount))
            .put("rows", rows)
            .put("csv", LedgerExport.toCsv(sorted, driver.name, driver.department))
    }

    /** 네트워크 작업이므로 메인 스레드 밖에서 부른다. 성공하면 공유 링크를 돌려준다. */
    fun upload(body: JSONObject): Result<String> = runCatching {
        val conn = URL("${BuildConfig.SHARE_URL}/api/ledgers").openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            conn.doOutput = true
            conn.setRequestProperty("content-type", "application/json; charset=utf-8")
            conn.setRequestProperty("X-App-Key", BuildConfig.SHARE_KEY)
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream).bufferedReader().readText()
            check(code in 200..299) { "서버 응답 $code" }
            JSONObject(text).getString("url")
        } finally {
            conn.disconnect()
        }
    }
}
