package vn.lichvannien.widget

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.util.concurrent.TimeUnit

data class DayForecast(val date: LocalDate, val code: Int, val min: Int, val max: Int)

data class WeatherData(
    val cityName: String,
    val temp: Int,
    val code: Int,
    val isDay: Boolean,
    val days: List<DayForecast>,
    val updatedAt: Long,
) {
    fun toJson(): String = JSONObject().apply {
        put("city", cityName); put("temp", temp); put("code", code); put("isDay", isDay)
        put("updatedAt", updatedAt)
        put("days", JSONArray().apply {
            days.forEach { d ->
                put(JSONObject().put("date", d.date.toString()).put("code", d.code).put("min", d.min).put("max", d.max))
            }
        })
    }.toString()

    companion object {
        fun fromJson(s: String): WeatherData? = runCatching {
            val o = JSONObject(s)
            val arr = o.getJSONArray("days")
            WeatherData(
                cityName = o.getString("city"),
                temp = o.getInt("temp"),
                code = o.getInt("code"),
                isDay = o.optBoolean("isDay", true),
                days = (0 until arr.length()).map {
                    val d = arr.getJSONObject(it)
                    DayForecast(LocalDate.parse(d.getString("date")), d.getInt("code"), d.getInt("min"), d.getInt("max"))
                },
                updatedAt = o.getLong("updatedAt"),
            )
        }.getOrNull()
    }
}

object Weather {
    private const val PREFS = "weather_cache"

    /** Mã thời tiết WMO -> (mô tả tiếng Việt, biểu tượng). */
    fun describe(code: Int, isDay: Boolean = true): Pair<String, String> = when (code) {
        0 -> (if (isDay) "Trời nắng" else "Trời quang") to (if (isDay) "☀️" else "🌙")
        1 -> "Ít mây" to (if (isDay) "🌤️" else "🌙")
        2 -> "Có mây rải rác" to "⛅"
        3 -> "Nhiều mây" to "☁️"
        45, 48 -> "Sương mù" to "🌫️"
        51, 53, 55, 56, 57 -> "Mưa phùn" to "🌦️"
        61, 63, 66 -> "Mưa" to "🌧️"
        65, 67 -> "Mưa to" to "🌧️"
        71, 73, 75, 77 -> "Có tuyết" to "🌨️"
        80, 81 -> "Mưa rào" to "🌦️"
        82 -> "Mưa rào mạnh" to "⛈️"
        85, 86 -> "Mưa tuyết" to "🌨️"
        95, 96, 99 -> "Dông" to "⛈️"
        else -> "—" to "🌡️"
    }

    fun cached(ctx: Context): WeatherData? =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("data", null)?.let { WeatherData.fromJson(it) }

    private fun save(ctx: Context, w: WeatherData) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("data", w.toJson()).apply()
    }

    /** Lấy dữ liệu từ Open-Meteo (miễn phí, không cần API key). */
    suspend fun fetch(ctx: Context, city: City): WeatherData = withContext(Dispatchers.IO) {
        val url = URL(
            "https://api.open-meteo.com/v1/forecast?latitude=${city.lat}&longitude=${city.lon}" +
                "&current=temperature_2m,weather_code,is_day" +
                "&daily=weather_code,temperature_2m_max,temperature_2m_min" +
                "&timezone=Asia%2FBangkok&forecast_days=5"
        )
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000; readTimeout = 15000
        }
        try {
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val o = JSONObject(body)
            val cur = o.getJSONObject("current")
            val daily = o.getJSONObject("daily")
            val dates = daily.getJSONArray("time")
            val codes = daily.getJSONArray("weather_code")
            val maxs = daily.getJSONArray("temperature_2m_max")
            val mins = daily.getJSONArray("temperature_2m_min")
            val w = WeatherData(
                cityName = city.name,
                temp = Math.round(cur.getDouble("temperature_2m")).toInt(),
                code = cur.getInt("weather_code"),
                isDay = cur.optInt("is_day", 1) == 1,
                days = (0 until dates.length()).map {
                    DayForecast(
                        LocalDate.parse(dates.getString(it)),
                        codes.getInt(it),
                        Math.round(mins.getDouble(it)).toInt(),
                        Math.round(maxs.getDouble(it)).toInt(),
                    )
                },
                updatedAt = System.currentTimeMillis(),
            )
            save(ctx, w)
            w
        } finally {
            conn.disconnect()
        }
    }

    private val netConstraint = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    /** Cập nhật thời tiết mỗi giờ khi có mạng. */
    fun schedulePeriodic(ctx: Context) {
        val req = PeriodicWorkRequestBuilder<WeatherWorker>(1, TimeUnit.HOURS)
            .setConstraints(netConstraint).build()
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork("weather_hourly", ExistingPeriodicWorkPolicy.KEEP, req)
    }

    /** Cập nhật ngay (khi đổi thành phố hoặc bấm nút làm mới). */
    fun refreshNow(ctx: Context) {
        val req = OneTimeWorkRequestBuilder<WeatherWorker>().setConstraints(netConstraint).build()
        WorkManager.getInstance(ctx).enqueueUniqueWork("weather_now", ExistingWorkPolicy.REPLACE, req)
    }

    fun cancelAll(ctx: Context) {
        WorkManager.getInstance(ctx).cancelUniqueWork("weather_hourly")
    }
}

class WeatherWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result = try {
        Weather.fetch(applicationContext, WidgetSettings.load(applicationContext).city)
        CalendarWidgetProvider.updateAll(applicationContext)
        Result.success()
    } catch (e: Exception) {
        if (runAttemptCount < 3) Result.retry() else Result.failure()
    }
}
