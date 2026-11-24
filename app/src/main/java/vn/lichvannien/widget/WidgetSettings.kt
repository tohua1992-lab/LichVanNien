package vn.lichvannien.widget

import android.content.Context
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** Tông màu nền kính. */
enum class BgTone(val label: String, val color: Int, val dark: Boolean) {
    WHITE("Trắng", 0xFFFFFFFF.toInt(), false),
    SMOKE("Khói tối", 0xFF2B2F36.toInt(), true),
    ICE("Xanh băng", 0xFFDDEFF7.toInt(), false),
    CREAM("Kem ấm", 0xFFF6F2E7.toInt(), false),
}

/** Màu chữ hiển thị. */
enum class TextTone(val label: String, val color: Int) {
    CHARCOAL("Đen than", 0xFF1C1F26.toInt()),
    WHITE("Trắng sáng", 0xFFFFFFFF.toInt()),
    NAVY("Xanh Navy", 0xFF1E3A8A.toInt()),
}

data class City(val name: String, val lat: Double, val lon: Double)

val CITIES = listOf(
    City("Hà Nội", 21.0285, 105.8542),
    City("TP.HCM", 10.7769, 106.7009),
    City("Đà Nẵng", 16.0544, 108.2022),
    City("Hải Phòng", 20.8449, 106.6881),
    City("Cần Thơ", 10.0452, 105.7469),
    City("Huế", 16.4637, 107.5909),
    City("Nha Trang", 12.2388, 109.1967),
    City("Đà Lạt", 11.9404, 108.4583),
    City("Vũng Tàu", 10.3460, 107.0843),
    City("Biên Hòa", 10.9574, 106.8429),
    City("Thủ Dầu Một", 10.9804, 106.6519),
    City("Vinh", 18.6796, 105.6813),
    City("Quy Nhơn", 13.7820, 109.2196),
    City("Buôn Ma Thuột", 12.6667, 108.0500),
    City("Hạ Long", 20.9599, 107.0425),
    City("Thái Nguyên", 21.5942, 105.8482),
)

data class WidgetSettings(
    val bgTone: BgTone = BgTone.CREAM,
    val opacity: Int = 100,        // 0..100 %
    val cornerRadius: Int = 32,    // dp
    val gloss: Int = 0,            // 0..100 % độ bóng & viền kính
    val clockSize: Int = 54,       // sp
    val textTone: TextTone = TextTone.CHARCOAL,
    val cityIndex: Int = 0,
    val showWeek: Boolean = true,
    val showForecast: Boolean = true,
) {
    val city: City get() = CITIES.getOrElse(cityIndex) { CITIES[0] }

    companion object {
        private const val PREFS = "widget_settings"

        fun load(ctx: Context): WidgetSettings {
            val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val d = WidgetSettings()
            return WidgetSettings(
                bgTone = runCatching { BgTone.valueOf(p.getString("bgTone", d.bgTone.name)!!) }.getOrDefault(d.bgTone),
                opacity = p.getInt("opacity", d.opacity),
                cornerRadius = p.getInt("cornerRadius", d.cornerRadius),
                gloss = p.getInt("gloss", d.gloss),
                clockSize = p.getInt("clockSize", d.clockSize),
                textTone = runCatching { TextTone.valueOf(p.getString("textTone", d.textTone.name)!!) }.getOrDefault(d.textTone),
                cityIndex = p.getInt("cityIndex", d.cityIndex),
                showWeek = p.getBoolean("showWeek", d.showWeek),
                showForecast = p.getBoolean("showForecast", d.showForecast),
            )
        }

        fun save(ctx: Context, s: WidgetSettings) {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("bgTone", s.bgTone.name)
                .putInt("opacity", s.opacity)
                .putInt("cornerRadius", s.cornerRadius)
                .putInt("gloss", s.gloss)
                .putInt("clockSize", s.clockSize)
                .putString("textTone", s.textTone.name)
                .putInt("cityIndex", s.cityIndex)
                .putBoolean("showWeek", s.showWeek)
                .putBoolean("showForecast", s.showForecast)
                .apply()
        }
    }
}

/** Các chuỗi hiển thị dùng chung cho widget và màn hình xem trước. */
object VnText {
    fun greeting(t: LocalTime = LocalTime.now()): String = when (t.hour) {
        in 5..10 -> "Chào buổi sáng"
        in 11..12 -> "Chào buổi trưa"
        in 13..17 -> "Chào buổi chiều"
        in 18..21 -> "Chào buổi tối"
        else -> "Chúc ngủ ngon"
    }

    fun weekdayLong(d: DayOfWeek): String = when (d) {
        DayOfWeek.MONDAY -> "Thứ Hai"
        DayOfWeek.TUESDAY -> "Thứ Ba"
        DayOfWeek.WEDNESDAY -> "Thứ Tư"
        DayOfWeek.THURSDAY -> "Thứ Năm"
        DayOfWeek.FRIDAY -> "Thứ Sáu"
        DayOfWeek.SATURDAY -> "Thứ Bảy"
        DayOfWeek.SUNDAY -> "Chủ Nhật"
    }

    fun weekdayShort(d: DayOfWeek): String =
        if (d == DayOfWeek.SUNDAY) "CN" else "T${d.value + 1}"

    fun forecastLabel(date: LocalDate, today: LocalDate): String = when {
        date == today -> "H.NAY"
        date.dayOfWeek == DayOfWeek.SUNDAY -> "CN"
        else -> "THỨ ${date.dayOfWeek.value + 1}"
    }

    fun solarLine(d: LocalDate): String =
        "${weekdayLong(d.dayOfWeek)}, ${d.dayOfMonth} Thg ${d.monthValue}"

    fun lunarLine(d: LocalDate): String {
        val l = LunarCalendar.fromSolar(d)
        val leap = if (l.leap) "N" else ""
        return "${l.day}/${l.month}$leap ÂL • ${LunarCalendar.yearCanChi(l.year)}"
    }

    /** Ngày âm hiển thị dưới mỗi ô tuần: mùng 1 thì ghi kèm tháng, ví dụ "1/11". */
    fun lunarCell(d: LocalDate): String {
        val l = LunarCalendar.fromSolar(d)
        return if (l.day == 1) "1/${l.month}${if (l.leap) "N" else ""}" else l.day.toString()
    }

    /** Ngày thứ Hai của tuần chứa [d]. */
    fun weekStart(d: LocalDate): LocalDate = d.minusDays((d.dayOfWeek.value - 1).toLong())
}

/** Màu phụ (chữ mờ) = màu chính với độ trong suốt. */
fun Int.withAlpha(alpha: Int): Int = (this and 0x00FFFFFF) or (alpha.coerceIn(0, 255) shl 24)

val RED = 0xFFEF4444.toInt()
val SUNDAY_RED = 0xFFE53935.toInt()
val BATTERY_GREEN = 0xFF16A34A.toInt()
