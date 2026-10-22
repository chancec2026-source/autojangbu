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
        loadProfile()
        loadDrivers()
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

    /**
     * 운행기록부 머리말과 '사용자' 칸. 국세청 고시 기재사항(차종·자동차등록번호·사용자)에
     * 세무사가 바로 쓰도록 상호·사업자등록번호를 더했다.
     */
    data class Profile(
        val name: String = "대표",
        val department: String = "",
        val company: String = "",
        val bizNo: String = "",
        val carModel: String = "",
        val plate: String = "",
    )

    private val _profile = MutableStateFlow(Profile())
    val profile: StateFlow<Profile> = _profile

    fun loadProfile() {
        fun s(key: String, def: String = "") = prefs.getString(key, def) ?: def
        // 이전 버전이 차에서 받은 "invalid"를 저장했을 수 있어 걸러 낸다.
        val plate = s("plate").let { if (it.equals("invalid", ignoreCase = true) || it == "\"invalid\"") "" else it }
        _profile.value = Profile(s("driverName", "대표"), s("driverDept"), s("company"), s("bizNo"), s("carModel"), plate)
    }

    fun saveProfile(p: Profile) {
        prefs.edit()
            .putString("driverName", p.name).putString("driverDept", p.department)
            .putString("company", p.company).putString("bizNo", p.bizNo)
            .putString("carModel", p.carModel).putString("plate", p.plate)
            .apply()
        _profile.value = p
    }

    // 운전자 여러 명(시트 위치로 구분)
    private val _drivers = MutableStateFlow<List<DriverEntry>>(emptyList())
    val drivers: StateFlow<List<DriverEntry>> = _drivers

    fun loadDrivers() { _drivers.value = DriverEntry.listFromJson(prefs.getString("drivers", "[]")) }

    fun saveDrivers(list: List<DriverEntry>) {
        prefs.edit().putString("drivers", DriverEntry.listToJson(list)).apply()
        _drivers.value = list
    }

    // 상주 서비스가 주기적으로 읽는 지금 운전석 시트 위치(운전자 등록 화면에서 쓴다)
    private val _currentPose = MutableStateFlow<SeatPose?>(null)
    val currentPose: StateFlow<SeatPose?> = _currentPose
    fun setCurrentPose(p: SeatPose) { _currentPose.value = p }

    // 진행 중 운행의 운전자(출발 때 시트 위치로 정함)
    fun saveActiveDriver(d: DriverEntry?) {
        prefs.edit().putString("activeDriverName", d?.name ?: "").putString("activeDriverDept", d?.department ?: "").apply()
    }
    fun loadActiveDriver(): Pair<String, String> =
        (prefs.getString("activeDriverName", "") ?: "") to (prefs.getString("activeDriverDept", "") ?: "")

    /** 차에서 읽은 자동차등록번호는 번호판처럼 생겼고 사용자가 비워 둔 경우에만 채운다. */
    fun fillPlateFromCar(plate: String) {
        val p = plate.trim().removeSurrounding("\"")
        if (isPlausiblePlate(p) && _profile.value.plate.isBlank()) saveProfile(_profile.value.copy(plate = p))
    }

    /** 예: 12가3456, 123가4567, 서울12가3456. 에뮬레이터는 "invalid" 같은 값을 돌려준다. */
    fun isPlausiblePlate(p: String): Boolean = Regex("""^[가-힣]{0,2}\s?\d{2,3}\s?[가-힣]\s?\d{4}$""").matches(p)
}
