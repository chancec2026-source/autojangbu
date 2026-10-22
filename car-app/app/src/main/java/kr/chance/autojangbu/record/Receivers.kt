package kr.chance.autojangbu.record

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kr.chance.autojangbu.data.Category
import kr.chance.autojangbu.data.TripStore

/** 알림의 업무/출퇴근/개인 버튼. */
class ClassifyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_ID, -1)
        val category = intent.getStringExtra(EXTRA_CATEGORY)?.let { Category.valueOf(it) } ?: return
        TripStore.init(context)
        TripStore.classify(id, category)
        Notifications.cancelTrip(context, id)
    }

    companion object {
        private const val EXTRA_ID = "id"
        private const val EXTRA_CATEGORY = "category"

        fun intent(context: Context, tripId: Long, category: Category): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                (tripId % 100_000).toInt() * 10 + category.ordinal,
                Intent(context, ClassifyReceiver::class.java)
                    .putExtra(EXTRA_ID, tripId)
                    .putExtra(EXTRA_CATEGORY, category.name),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
    }
}

/** 시동이 걸려 차가 켜지면 기록을 시작한다. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        RecorderService.start(context)
    }
}
