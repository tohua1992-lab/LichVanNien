package vn.lichvannien.widget

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.os.BatteryManager

/**
 * Vẽ nền "kính" của widget thành bitmap: màu nền + độ đục + bo góc + viền/bóng kính.
 * RemoteViews không cho đặt bo góc tuỳ ý nên phải vẽ sẵn.
 */
object BackgroundRenderer {
    fun render(ctx: Context, s: WidgetSettings, widthDp: Int, heightDp: Int): Bitmap {
        val density = ctx.resources.displayMetrics.density
        // Giới hạn kích thước để không vượt quá bộ nhớ cho phép của RemoteViews
        val scale = minOf(density, 1200f / widthDp.coerceAtLeast(1), 1200f / heightDp.coerceAtLeast(1))
        val w = (widthDp * scale).toInt().coerceAtLeast(1)
        val h = (heightDp * scale).toInt().coerceAtLeast(1)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val r = s.cornerRadius * scale
        val rect = RectF(0f, 0f, w.toFloat(), h.toFloat())

        val alpha = (s.opacity / 100f * 255).toInt()
        c.drawRoundRect(rect, r, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = s.bgTone.color.withAlpha(alpha) })

        if (s.gloss > 0) {
            val g = s.gloss / 100f
            // Ánh bóng phía trên
            c.drawRoundRect(rect, r, r, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, 0f, 0f, h * 0.6f,
                    0xFFFFFFFF.toInt().withAlpha((g * 90).toInt()), 0x00FFFFFF, Shader.TileMode.CLAMP
                )
            })
            // Viền kính
            val stroke = 1.5f * scale
            val inset = RectF(rect).apply { inset(stroke / 2, stroke / 2) }
            c.drawRoundRect(inset, r, r, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = stroke
                color = (if (s.bgTone.dark) 0xFFFFFFFF.toInt() else 0xFF000000.toInt())
                    .withAlpha((g * if (s.bgTone.dark) 110 else 40).toInt())
            })
        }
        return bmp
    }
}

data class BatteryInfo(val percent: Int, val tempC: Double, val charging: Boolean)

object Battery {
    fun read(ctx: Context): BatteryInfo? {
        val i: Intent = ctx.applicationContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return null
        val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val temp = i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10.0
        val status = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        if (level < 0) return null
        return BatteryInfo(
            percent = level * 100 / scale.coerceAtLeast(1),
            tempC = temp,
            charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL,
        )
    }

    fun label(b: BatteryInfo): String =
        "${if (b.charging) "⚡" else "🔋"} ${b.percent}% • ${String.format(java.util.Locale.US, "%.1f", b.tempC)}°C"
}
