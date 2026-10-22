package kr.chance.autojangbu.record

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.DoorArea
import ai.pleos.playground.vehicle.constant.area.DoorValue
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.listener.DistanceDrivenListener
import ai.pleos.playground.vehicle.listener.DoorPositionListener
import ai.pleos.playground.vehicle.listener.GearSelectionListener
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.constant.area.SeatValue
import kr.chance.autojangbu.data.SeatPose
import kr.chance.autojangbu.data.Trip
import kr.chance.autojangbu.data.TripStore
import kr.chance.autojangbu.data.matchDriver
import kr.chance.autojangbu.data.suggestCategory

/**
 * 화면이 꺼져 있어도 운행을 기록하는 상주 서비스.
 * 기어·계기판·운전석 문 신호를 [TripDetector]에 넘기고, 운행이 끝나면 저장하고 분류 알림을 띄운다.
 */
class RecorderService : Service() {

    private val main = Handler(Looper.getMainLooper())
    private val detector = TripDetector()
    private var vehicle: Vehicle? = null
    private var lastOdoKm: Double? = null
    private val driverDoor = DoorArea(DoorValue.ROW_1_LEFT)

    // 차량 신호는 다른 스레드로 들어오므로 모두 메인 스레드로 옮겨 순서대로 처리한다.
    private val gearListener = object : GearSelectionListener {
        override fun onSelected(gear: VehicleGear?) { main.post { onGear(gear) } }
        override fun onFailed(e: Exception) { log("기어 신호 실패", e) }
    }
    private val odoListener = object : DistanceDrivenListener {
        override fun onDistanceDrivenUpdated(km: Float?) { main.post { km?.let { onOdometer(it.toDouble()) } } }
        override fun onFailed(e: Exception) { log("계기판 신호 실패", e) }
    }
    private val doorListener = object : DoorPositionListener {
        override fun onPositionUpdated(position: Int?) { main.post { if ((position ?: 0) > 0) onDriverDoorOpened() } }
        override fun onFailed(e: Exception) { log("문 신호 실패", e) }
    }

    private val tick = object : Runnable {
        override fun run() {
            detector.onTick(lastOdoKm, System.currentTimeMillis())?.let(::save)
            readPose { TripStore.setCurrentPose(it) }
            main.postDelayed(this, TICK_MS)
        }
    }

    private val driverSeat = SeatArea(SeatValue.ROW_1_LEFT)

    /** 운전석 시트 위치 세 값(앞뒤·등받이·높이)을 모두 읽으면 한 번 알려 준다. */
    private fun readPose(onPose: (SeatPose) -> Unit) {
        val seat = vehicle?.getSeat() ?: return
        val values = arrayOfNulls<Int>(3)
        fun done(i: Int, v: Int?) = main.post {
            values[i] = v
            if (values.all { it != null }) onPose(SeatPose(values[0]!!, values[1]!!, values[2]!!))
        }
        runCatching {
            seat.getCurrentSeatForeAftPosition(driverSeat, { done(0, it) }, { log("시트 앞뒤 읽기 실패", it) })
            seat.getCurrentSeatBackrestAnglePosition(driverSeat, { done(1, it) }, { log("등받이 읽기 실패", it) })
            seat.getCurrentSeatHeightPosition(driverSeat, { done(2, it) }, { log("시트 높이 읽기 실패", it) })
        }
    }

    override fun onCreate() {
        super.onCreate()
        TripStore.init(this)
        Notifications.ensureChannels(this)
        val notification = Notifications.recording(this)
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(Notifications.ID_RECORDING, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(Notifications.ID_RECORDING, notification)
        }
        TripStore.loadActive()?.let { (odo, ms) -> detector.restore(odo, ms); TripStore.saveActive(odo, ms) }
        connect(attempt = 1)
        main.postDelayed(tick, TICK_MS)
    }

    /** 차량 서비스가 늦게 뜨는 경우가 있어 몇 번 다시 시도한다. */
    private fun connect(attempt: Int) {
        try {
            val v = vehicle ?: Vehicle(applicationContext).also { vehicle = it }
            v.initialize()
            v.getGear().registerGearSelection(gearListener)
            v.getOdometer().registerDistanceDriven(odoListener)
            v.getDoor().registerDoorPosition(driverDoor, doorListener)
            // 등록 직후 현재 값도 한 번 읽어 둔다.
            v.getOdometer().getCurrentDistanceDriven({ km -> main.post { km?.let { onOdometer(it.toDouble()) } } }, { log("계기판 읽기 실패", it) })
            v.getGear().getCurrentGearSelectionState({ g -> main.post { onGear(g) } }, { log("기어 읽기 실패", it) })
            // 운행기록부 머리말의 자동차등록번호를 차에서 채운다.
            v.getCarInfo().getCRNInfo({ crn -> main.post { crn?.let(TripStore::fillPlateFromCar); Log.i(TAG, "자동차등록번호: $crn") } },
                { log("자동차등록번호 읽기 실패", it) })
            Log.i(TAG, "차량 연결 완료")
        } catch (e: Exception) {
            log("차량 연결 실패($attempt)", e)
            if (attempt < 10) main.postDelayed({ connect(attempt + 1) }, 3_000L)
        }
    }

    private fun onOdometer(km: Double) {
        lastOdoKm = km
        TripStore.setLastOdometer(km)
        val hadStart = detector.startOdoKm != null
        detector.onOdometer(km)
        if (!hadStart && detector.startOdoKm != null) TripStore.saveActive(detector.startOdoKm, detector.startMs)
    }

    private fun onGear(gear: VehicleGear?) {
        if (gear == null || gear == VehicleGear.GEAR_UNKNOWN) return
        val wasInTrip = detector.inTrip
        detector.onGear(gear == VehicleGear.GEAR_PARK, lastOdoKm, System.currentTimeMillis())
        if (!wasInTrip && detector.inTrip) {
            Log.i(TAG, "운행 시작 ${detector.startOdoKm}km")
            TripStore.saveActive(detector.startOdoKm, detector.startMs)
            // 출발할 때 시트 위치로 운전자를 알아본다(등록된 운전자가 있을 때만).
            TripStore.saveActiveDriver(null)
            if (TripStore.drivers.value.isNotEmpty()) readPose { pose ->
                val d = matchDriver(pose, TripStore.drivers.value)
                TripStore.saveActiveDriver(d)
                Log.i(TAG, "운전자: ${d?.name ?: "기본 사용자"} (시트 $pose)")
            }
        }
    }

    private fun onDriverDoorOpened() {
        detector.onDriverDoorOpened(lastOdoKm, System.currentTimeMillis())?.let(::save)
    }

    private fun save(d: TripDetector.Draft) {
        val (driverName, driverDept) = TripStore.loadActiveDriver()
        val trip = Trip(
            id = d.endMs,
            startMs = d.startMs,
            endMs = d.endMs,
            startOdoKm = d.startOdoKm,
            endOdoKm = d.endOdoKm,
            suggested = suggestCategory(d.startMs),
            driverName = driverName,
            driverDept = driverDept,
        )
        TripStore.add(trip)
        TripStore.saveActive(null, 0)
        Log.i(TAG, "운행 저장 ${trip.startOdoKm}→${trip.endOdoKm}km (${trip.distanceKm}km)")
        Notifications.tripFinished(this, trip)
    }

    private fun log(msg: String, e: Exception) { Log.w(TAG, "$msg: ${e.message}") }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        main.removeCallbacksAndMessages(null)
        runCatching {
            vehicle?.getGear()?.unregisterGearSelection(gearListener)
            vehicle?.getOdometer()?.unregisterDistanceDriven(odoListener)
            vehicle?.getDoor()?.unregisterDoorPosition(driverDoor, doorListener)
            vehicle?.release()
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "AutoJangbu"
        private const val TICK_MS = 10_000L

        fun start(context: Context) {
            context.startForegroundService(Intent(context, RecorderService::class.java))
        }
    }
}
