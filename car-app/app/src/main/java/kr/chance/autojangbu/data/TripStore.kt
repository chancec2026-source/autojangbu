package kr.chance.autojangbu.data

import android.content.Context
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray

/** 운행 기록 저장소. 차 안 파일에 먼저 저장한다(서버 동기화는 다음 단계). 위치 정보는 저장하지 않는다. */
object TripStore {
    private lateinit var file: File
    private lateinit var prefs: android.content.SharedPreferences
    private val _trips = MutableStateFlow<List<Trip>>(emptyList())
    val trips: StateFlow<List<Trip>> = _trips

    private val _lastOdoKm = MutableStateFlow<Double?>(null)
    val lastOdoKm: StateFlow<Double?> = _lastOdoKm

    @Synchronized
    fun init(context: Context) {
        if (::file.isInitialized) return
        file = File(context.filesDir, "trips.json")
        prefs = context.getSharedPreferences("recorder", Context.MODE_PRIVATE)
        _trips.value = load()
        loadDriver()
    }

    private fun load(): List<Trip> {
        if (!file.exists()) return emptyList()
        return runCatching {
            val arr = JSONArray(file.readText())
            (0 until arr.length()).map { Trip.fromJson(arr.getJSONObject(it)) }
        }.getOrDefault(emptyList())
    }

    private fun save(list: List<Trip>) {
        val arr = JSONArray().apply { list.forEach { put(it.toJson()) } }
        val tmp = File(file.parentFile, "trips.json.tmp")
        tmp.writeText(arr.toString())
        tmp.renameTo(file)
        _trips.value = list
    }

    @Synchronized
    fun add(trip: Trip) = save(_trips.value + trip)

    @Synchronized
    fun classify(id: Long, category: Category) =
        save(_trips.value.map { if (it.id == id) it.copy(category = category) else it })

    @Synchronized
    fun replaceAll(list: List<Trip>) = save(list)

    @Synchronized
    fun setMemo(id: Long, memo: String) =
        save(_trips.value.map { if (it.id == id) it.copy(memo = memo) else it })

    fun find(id: Long): Trip? = _trips.value.firstOrNull { it.id == id }

    fun setLastOdometer(km: Double) { _lastOdoKm.value = km }

    // 진행 중인 운행: 서비스가 다시 켜져도 출발 거리를 잃지 않도록 따로 저장한다.
    fun saveActive(startOdoKm: Double?, startMs: Long) {
        prefs.edit().apply {
            if (startOdoKm == null) remove("activeStartOdo").remove("activeStartMs")
            else putString("activeStartOdo", startOdoKm.toString()).putLong("activeStartMs", startMs)
        }.apply()
        _activeStart.value = startOdoKm
    }

    fun loadActive(): Pair<Double, Long>? {
        val odo = prefs.getString("activeStartOdo", null)?.toDoubleOrNull() ?: return null
        return odo to prefs.getLong("activeStartMs", System.currentTimeMillis())
    }

    private val _activeStart = MutableStateFlow<Double?>(null)
    val activeStartOdoKm: StateFlow<Double?> = _activeStart

    // 운행기록부의 '사용자' 칸(부서·성명)
    data class Driver(val name: String, val department: String)

    private val _driver = MutableStateFlow(Driver("대표", ""))
    val driver: StateFlow<Driver> = _driver

    fun loadDriver() {
        _driver.value = Driver(prefs.getString("driverName", "대표") ?: "대표", prefs.getString("driverDept", "") ?: "")
    }

    fun saveDriver(name: String, department: String) {
        prefs.edit().putString("driverName", name).putString("driverDept", department).apply()
        _driver.value = Driver(name, department)
    }
}
