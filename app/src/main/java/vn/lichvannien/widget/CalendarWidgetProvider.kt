package vn.lichvannien.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class CalendarWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { render(ctx, mgr, it) }
        scheduleMidnight(ctx)
        Weather.schedulePeriodic(ctx)
        if (Weather.cached(ctx) == null) Weather.refreshNow(ctx)
    }

    override fun onAppWidgetOptionsChanged(ctx: Context, mgr: AppWidgetManager, id: Int, newOptions: Bundle) {
        render(ctx, mgr, id)
    }

    override fun onEnabled(ctx: Context) {
        Weather.schedulePeriodic(ctx)
        Weather.refreshNow(ctx)
    }

    override fun onDisabled(ctx: Context) {
        Weather.cancelAll(ctx)
        ctx.getSystemService(AlarmManager::class.java).cancel(midnightIntent(ctx))
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        super.onReceive(ctx, intent)
        when (intent.action) {
            ACTION_REFRESH, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_LOCALE_CHANGED, Intent.ACTION_BOOT_COMPLETED -> {
                updateAll(ctx)
                scheduleMidnight(ctx)
            }
        }
    }

    companion object {
        const val ACTION_REFRESH = "vn.lichvannien.widget.REFRESH"

        private val WEEK_CELL = intArrayOf(R.id.wk_cell_0, R.id.wk_cell_1, R.id.wk_cell_2, R.id.wk_cell_3, R.id.wk_cell_4, R.id.wk_cell_5, R.id.wk_cell_6)
        private val WEEK_LABEL = intArrayOf(R.id.wk_label_0, R.id.wk_label_1, R.id.wk_label_2, R.id.wk_label_3, R.id.wk_label_4, R.id.wk_label_5, R.id.wk_label_6)
        private val WEEK_DAY = intArrayOf(R.id.wk_day_0, R.id.wk_day_1, R.id.wk_day_2, R.id.wk_day_3, R.id.wk_day_4, R.id.wk_day_5, R.id.wk_day_6)
        private val WEEK_LUNAR = intArrayOf(R.id.wk_lunar_0, R.id.wk_lunar_1, R.id.wk_lunar_2, R.id.wk_lunar_3, R.id.wk_lunar_4, R.id.wk_lunar_5, R.id.wk_lunar_6)
        private val FC_LABEL = intArrayOf(R.id.fc_label_0, R.id.fc_label_1, R.id.fc_label_2, R.id.fc_label_3, R.id.fc_label_4)
        private val FC_ICON = intArrayOf(R.id.fc_icon_0, R.id.fc_icon_1, R.id.fc_icon_2, R.id.fc_icon_3, R.id.fc_icon_4)
        private val FC_TEMP = intArrayOf(R.id.fc_temp_0, R.id.fc_temp_1, R.id.fc_temp_2, R.id.fc_temp_3, R.id.fc_temp_4)

        fun updateAll(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx)
            mgr.getAppWidgetIds(ComponentName(ctx, CalendarWidgetProvider::class.java)).forEach { render(ctx, mgr, it) }
        }

        fun render(ctx: Context, mgr: AppWidgetManager, id: Int) {
            val s = WidgetSettings.load(ctx)
            val v = RemoteViews(ctx.packageName, R.layout.widget_calendar)
            val today = LocalDate.now()
            val primary = s.textTone.color
            val secondary = primary.withAlpha(0xB3)
            val darkText = s.textTone != TextTone.WHITE

            // Nền kính
            val opts = mgr.getAppWidgetOptions(id)
            val wDp = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH).takeIf { it > 0 } ?: 340
            val hDp = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT).takeIf { it > 0 } ?: 300
            v.setImageViewBitmap(R.id.bg, BackgroundRenderer.render(ctx, s, wDp, hDp))

            // Khối đồng hồ & ngày
            v.setTextViewText(R.id.greeting, VnText.greeting())
            v.setTextColor(R.id.greeting, secondary)
            v.setTextColor(R.id.clock, primary)
            v.setTextViewTextSize(R.id.clock, TypedValue.COMPLEX_UNIT_SP, s.clockSize.toFloat())
            v.setTextViewText(R.id.solar_date, VnText.solarLine(today))
            v.setTextColor(R.id.solar_date, primary)
            v.setTextViewText(R.id.lunar_pill, VnText.lunarLine(today))
            v.setInt(R.id.lunar_pill, "setBackgroundResource", if (darkText) R.drawable.pill_dark else R.drawable.pill_light)

            // Thời tiết hiện tại
            val w = Weather.cached(ctx)?.takeIf { it.cityName == s.city.name }
            if (w != null) {
                val (desc, icon) = Weather.describe(w.code, w.isDay)
                v.setTextViewText(R.id.weather_icon, icon)
                v.setTextViewText(R.id.weather_temp, "${w.temp}°")
                v.setTextViewText(R.id.weather_desc, desc)
                val td = w.days.firstOrNull { it.date == today }
                v.setTextViewText(
                    R.id.weather_loc,
                    "📍 ${w.cityName}" + (td?.let { " • ↑${it.max}° ↓${it.min}°" } ?: "")
                )
            } else {
                v.setTextViewText(R.id.weather_icon, "🌡️")
                v.setTextViewText(R.id.weather_temp, "--°")
                v.setTextViewText(R.id.weather_desc, "Đang tải thời tiết…")
                v.setTextViewText(R.id.weather_loc, "📍 ${s.city.name}")
            }
            v.setTextColor(R.id.weather_temp, primary)
            v.setTextColor(R.id.weather_desc, primary)
            v.setTextColor(R.id.weather_loc, secondary)

            // Pin
            val b = Battery.read(ctx)
            if (b != null) {
                v.setViewVisibility(R.id.battery_chip, View.VISIBLE)
                v.setTextViewText(R.id.battery_chip, Battery.label(b))
                v.setTextColor(R.id.battery_chip, if (darkText) BATTERY_GREEN else 0xFF4ADE80.toInt())
                v.setInt(R.id.battery_chip, "setBackgroundResource", if (darkText) R.drawable.chip_light_bg else R.drawable.chip_dark_bg)
            } else {
                v.setViewVisibility(R.id.battery_chip, View.GONE)
            }

            // Dải tuần
            v.setViewVisibility(R.id.week_row, if (s.showWeek) View.VISIBLE else View.GONE)
            if (s.showWeek) {
                val start = VnText.weekStart(today)
                for (i in 0 until 7) {
                    val d = start.plusDays(i.toLong())
                    val isToday = d == today
                    val isSunday = i == 6
                    v.setTextViewText(WEEK_LABEL[i], VnText.weekdayShort(d.dayOfWeek))
                    v.setTextViewText(WEEK_DAY[i], d.dayOfMonth.toString())
                    v.setTextViewText(WEEK_LUNAR[i], VnText.lunarCell(d))
                    v.setInt(WEEK_CELL[i], "setBackgroundResource", if (isToday) R.drawable.today_bg else 0)
                    v.setTextColor(WEEK_LABEL[i], if (isToday) 0xFFFFFFFF.toInt() else if (isSunday) SUNDAY_RED else secondary)
                    v.setTextColor(WEEK_DAY[i], if (isToday) 0xFFFFFFFF.toInt() else primary)
                    v.setTextColor(WEEK_LUNAR[i], if (isToday) 0xE6FFFFFF.toInt() else secondary)
                }
            }

            // Dự báo 5 ngày
            val showFc = s.showForecast && w != null && w.days.isNotEmpty()
            v.setViewVisibility(R.id.forecast_row, if (showFc) View.VISIBLE else View.GONE)
            if (showFc) {
                val days = w!!.days.filter { !it.date.isBefore(today) }
                for (i in 0 until 5) {
                    val d = days.getOrNull(i)
                    if (d == null) {
                        v.setTextViewText(FC_LABEL[i], ""); v.setTextViewText(FC_ICON[i], ""); v.setTextViewText(FC_TEMP[i], "")
                        continue
                    }
                    v.setTextViewText(FC_LABEL[i], VnText.forecastLabel(d.date, today))
                    v.setTextViewText(FC_ICON[i], Weather.describe(d.code).second)
                    v.setTextViewText(FC_TEMP[i], "${d.min}°/${d.max}°")
                    v.setTextColor(FC_LABEL[i], secondary)
                    v.setTextColor(FC_TEMP[i], primary)
                }
            }

            // Chạm vào widget -> mở màn hình cài đặt
            val open = PendingIntent.getActivity(
                ctx, 0, Intent(ctx, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            v.setOnClickPendingIntent(R.id.widget_root, open)

            mgr.updateAppWidget(id, v)
        }

        private fun midnightIntent(ctx: Context): PendingIntent = PendingIntent.getBroadcast(
            ctx, 1,
            Intent(ctx, CalendarWidgetProvider::class.java).setAction(ACTION_REFRESH),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        /** Hẹn giờ cập nhật lúc qua ngày mới (đổi ngày dương & âm lịch). */
        fun scheduleMidnight(ctx: Context) {
            val next = LocalDateTime.of(LocalDate.now().plusDays(1), java.time.LocalTime.of(0, 0, 5))
            val at = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            ctx.getSystemService(AlarmManager::class.java)
                .setWindow(AlarmManager.RTC, at, 5 * 60 * 1000L, midnightIntent(ctx))
        }

        fun requestRefresh(ctx: Context) {
            ctx.sendBroadcast(Intent(ctx, CalendarWidgetProvider::class.java).setAction(ACTION_REFRESH))
        }
    }
}
