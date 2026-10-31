package kr.chance.autojangbu.record

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.text.SimpleDateFormat
import java.util.Locale
import kr.chance.autojangbu.MainActivity
import kr.chance.autojangbu.R
import kr.chance.autojangbu.data.Category
import kr.chance.autojangbu.data.Trip

object Notifications {
    const val ID_RECORDING = 1
    private const val CH_RECORDING = "recording"
    private const val CH_TRIP = "trip"
    // 앱의 '일반 업무' 하늘색(ui/Theme.kt Palette.biz)
    private const val ACCENT = 0xFF61CDFF.toInt()
    private val time = SimpleDateFormat("HH:mm", Locale.KOREAN)

    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH_RECORDING, "운행 기록 중", NotificationManager.IMPORTANCE_MIN))
        nm.createNotificationChannel(NotificationChannel(CH_TRIP, "운행 분류", NotificationManager.IMPORTANCE_HIGH))
    }

    fun recording(context: Context): Notification =
        Notification.Builder(context, CH_RECORDING)
            .setSmallIcon(R.drawable.ic_stat_gauge)
            .setColor(ACCENT)
            .setContentTitle("오토장부")
            .setContentText("운행을 자동으로 기록하고 있어요")
            .setOngoing(true)
            .setContentIntent(openApp(context))
            .build()

    /** 주차하면 버튼 세 개로 바로 분류한다. 추천 구분은 맨 앞에 둔다. */
    fun tripFinished(context: Context, trip: Trip) {
        val order = listOf(trip.suggested) + Category.entries.filter { it != trip.suggested }
        val builder = Notification.Builder(context, CH_TRIP)
            .setSmallIcon(R.drawable.ic_stat_gauge)
            .setColor(ACCENT)
            .setContentTitle("운행 %.1fkm 기록했어요".format(trip.distanceKm))
            .setContentText("${time.format(trip.startMs)}–${time.format(trip.endMs)} · 계기판 %,.0f → %,.0fkm · 추천: ${trip.suggested.label}"
                .format(trip.startOdoKm, trip.endOdoKm))
            .setCategory(Notification.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(openApp(context))
        order.forEach { c ->
            builder.addAction(Notification.Action.Builder(null, c.label, ClassifyReceiver.intent(context, trip.id, c)).build())
        }
        context.getSystemService(NotificationManager::class.java).notify(tripNotificationId(trip.id), builder.build())
    }

    fun cancelTrip(context: Context, tripId: Long) =
        context.getSystemService(NotificationManager::class.java).cancel(tripNotificationId(tripId))

    private fun tripNotificationId(tripId: Long) = (tripId % 100_000).toInt() + 100

    private fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
    )
}
