package kr.chance.autojangbu.voice

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale
import kr.chance.autojangbu.data.Category
import kr.chance.autojangbu.data.Trip
import kr.chance.autojangbu.data.TripStore
import kr.chance.autojangbu.data.summarize
import kr.chance.autojangbu.record.Notifications
import org.json.JSONArray
import org.json.JSONObject

/**
 * Gleo 음성 함수 3개를 받는다(assets/schema.json). 결과는 Gleo에 콜백으로 돌려주고 Gleo가 말로 답한다.
 * 콜백 message는 Pleos 지침대로 영어로 쓰고, 메모 같은 사용자 문장은 그대로 둔다.
 */
class GleoActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        TripStore.init(context)
        val action = intent.action ?: return
        val params = parseParams(intent.getStringExtra(EXTRA_PARAM))
        Log.i(TAG, "Gleo 호출 $action $params")
        val result = when (action) {
            CLASSIFY -> classify(context, params)
            ADD_NOTE -> addNote(params)
            SUMMARY -> summary()
            else -> return
        }
        respond(context, action, result)
    }

    private fun classify(context: Context, p: JSONObject): Result {
        val trip = lastTrip() ?: return Result("fail", "There is no finished trip to record yet.")
        val category = parseCategory(p.optString("category"))
            ?: return Result("asking", "Should the ${km(trip)} km trip be business, commute, or personal?")
        val note = p.optString("note").trim()
        TripStore.classify(trip.id, category)
        if (note.isNotEmpty()) TripStore.setMemo(trip.id, note)
        Notifications.cancelTrip(context, trip.id)
        val noteText = if (note.isNotEmpty()) " with the note \"$note\"" else ""
        // 일반 업무인데 방문 목적이 없으면 세무 증빙을 위해 한마디 더 청한다.
        val ask = if (category == Category.BUSINESS && note.isEmpty())
            " Tell me where you went or why, and I will add it as the purpose for the tax record." else ""
        return Result("success", "Recorded the ${km(trip)} km trip as ${english(category)} use$noteText.$ask", trip)
    }

    private fun addNote(p: JSONObject): Result {
        val trip = lastTrip() ?: return Result("fail", "There is no finished trip to add a note to.")
        val note = p.optString("note").trim()
        if (note.isEmpty()) return Result("asking", "What note should be added to the ${km(trip)} km trip?")
        TripStore.setMemo(trip.id, note)
        return Result("success", "Added the note \"$note\" to the ${km(trip)} km trip.", trip)
    }

    private fun summary(): Result {
        val month = YearMonth.now()
        val trips = TripStore.trips.value.filter {
            YearMonth.from(Instant.ofEpochMilli(it.startMs).atZone(ZoneId.systemDefault())) == month
        }
        val s = summarize(trips)
        val pending = when (s.unconfirmedCount) {
            0 -> " Every trip has a purpose."
            1 -> " 1 trip still needs a purpose."
            else -> " ${s.unconfirmedCount} trips still need a purpose."
        }
        val tripCount = if (s.tripCount == 1) "1 trip" else "${s.tripCount} trips"
        return Result(
            "success",
            String.format(Locale.US, "This month: %s, %.1f km in total, %.1f km for business use, a business-use ratio of %.0f percent.",
                tripCount, s.totalKm, s.businessUseKm, s.businessUseRatio * 100) + pending,
        )
    }

    private fun lastTrip(): Trip? = TripStore.trips.value.maxByOrNull { it.endMs }

    private fun respond(context: Context, functionName: String, r: Result) {
        val payload = JSONObject()
            .put("functionName", functionName)
            .put("status", r.status)
            .put("message", r.message)
            .put("mode", "voice")
        r.trip?.let {
            payload.put("data", JSONArray().put(JSONObject()
                .put("distanceKm", it.distanceKm)
                .put("startOdometerKm", it.startOdoKm)
                .put("endOdometerKm", it.endOdoKm)).toString())
        }
        context.sendBroadcast(Intent(RESPONSE_ACTION)
            .putExtra("result", r.status)
            .putExtra("packageName", context.packageName)
            .putExtra("response", payload.toString()))
        Log.i(TAG, "Gleo 응답 $payload")
    }

    private data class Result(val status: String, val message: String, val trip: Trip? = null)

    companion object {
        private const val TAG = "AutoJangbu"
        const val CLASSIFY = "CLASSIFY_LAST_TRIP"
        const val ADD_NOTE = "ADD_TRIP_NOTE"
        const val SUMMARY = "GET_MONTHLY_LOGBOOK_SUMMARY"
        private const val EXTRA_PARAM = "ACTION_PARAM"
        private const val RESPONSE_ACTION = "ai.pleos.playground.intent.action.APP_ACTION_RESPONSE"

        /** 문서 예제는 {"actionParameters": {...}} 전체를 넘기지만, 파라미터만 오는 경우도 받아 준다. */
        fun parseParams(raw: String?): JSONObject {
            val o = runCatching { JSONObject(raw?.trim()?.removeSurrounding("\"") ?: "{}") }.getOrDefault(JSONObject())
            return o.optJSONObject("actionParameters") ?: o
        }

        /** Gleo가 영어로 넘기든 사용자 말 그대로 넘기든 알아듣는다. */
        fun parseCategory(text: String): Category? {
            val t = text.trim().lowercase(Locale.ROOT)
            return when {
                t.isEmpty() -> null
                listOf("commut", "출퇴근", "출근", "퇴근").any { it in t } -> Category.COMMUTE
                listOf("personal", "private", "개인", "사적").any { it in t } -> Category.PERSONAL
                listOf("business", "work", "업무", "일", "거래처", "현장", "미팅", "출장").any { it in t } -> Category.BUSINESS
                else -> null
            }
        }

        private fun km(t: Trip) = String.format(Locale.US, "%.1f", t.distanceKm)
        private fun english(c: Category) = when (c) {
            Category.BUSINESS -> "business"
            Category.COMMUTE -> "commute"
            Category.PERSONAL -> "personal"
        }
    }
}
