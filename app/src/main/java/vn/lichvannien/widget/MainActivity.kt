package vn.lichvannien.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val Accent = Color(0xFF14C8B4)
private val Title = Color(0xFF4338CA)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Accent)) { SettingsScreen() }
        }
    }
}

@Composable
private fun SettingsScreen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var s by remember { mutableStateOf(WidgetSettings.load(ctx)) }
    var weather by remember { mutableStateOf(Weather.cached(ctx)) }
    var wallpaper by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(false) }

    fun refreshWeather() {
        loading = true
        scope.launch {
            weather = runCatching { Weather.fetch(ctx, s.city) }.getOrElse {
                Toast.makeText(ctx, "Không tải được thời tiết, kiểm tra kết nối mạng", Toast.LENGTH_SHORT).show()
                weather
            }
            loading = false
            CalendarWidgetProvider.updateAll(ctx)
        }
    }

    LaunchedEffect(s.cityIndex) {
        if (weather?.cityName != s.city.name) refreshWeather()
    }

    val wallpapers = listOf(
        "Nền Kem" to Brush.verticalGradient(listOf(Color(0xFFEDEAE3), Color(0xFFDCD7CC))),
        "Nền Xanh" to Brush.verticalGradient(listOf(Color(0xFF7DD3FC), Color(0xFF2563EB))),
        "Nền Tối" to Brush.verticalGradient(listOf(Color(0xFF1F2937), Color(0xFF0B0F19))),
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F4F6))
            .verticalScroll(rememberScrollState())
    ) {
        // ===== Xem trước trực tiếp =====
        Column(
            Modifier
                .fillMaxWidth()
                .background(wallpapers[wallpaper].second)
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "XEM TRƯỚC TRỰC TIẾP",
                    fontSize = 12.sp,
                    color = if (wallpaper == 2) Color.White else Color(0xFF374151),
                    modifier = Modifier.weight(1f)
                )
                wallpapers.forEachIndexed { i, (name, _) ->
                    val sel = i == wallpaper
                    Text(
                        name, fontSize = 12.sp,
                        color = if (sel) Color.White else Color(0xFF1F2937),
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (sel) Title else Color.White.copy(alpha = 0.7f))
                            .clickable { wallpaper = i }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            WidgetPreview(s, weather)
            Spacer(Modifier.height(6.dp))
        }

        // ===== 1. Kính lỏng & khung viền =====
        Section("1. KÍNH LỎNG & KHUNG VIỀN") {
            Label("Tông màu nền kính:")
            ChoiceRow(BgTone.entries, s.bgTone, { it.label }) { s = s.copy(bgTone = it) }
            SliderRow("Độ đục nền: ${s.opacity}%", s.opacity, 0..100) { s = s.copy(opacity = it) }
            SliderRow("Bo góc đồng tâm: ${s.cornerRadius}dp", s.cornerRadius, 0..40) { s = s.copy(cornerRadius = it) }
            SliderRow("Độ bóng & viền kính: ${s.gloss}%", s.gloss, 0..100) { s = s.copy(gloss = it) }
        }

        // ===== 2. Đồng hồ & màu sắc =====
        Section("2. ĐỒNG HỒ & MÀU SẮC") {
            SliderRow("Cỡ chữ Đồng hồ: ${s.clockSize}sp", s.clockSize, 32..64) { s = s.copy(clockSize = it) }
            Label("Màu chữ hiển thị:")
            ChoiceRow(TextTone.entries, s.textTone, { it.label }) { s = s.copy(textTone = it) }
        }

        // ===== 3. Thời tiết & hiển thị =====
        Section("3. THỜI TIẾT & HIỂN THỊ") {
            Label("Thành phố:")
            CityPicker(s.cityIndex) { s = s.copy(cityIndex = it) }
            Spacer(Modifier.height(8.dp))
            SwitchRow("Hiện dải lịch tuần", s.showWeek) { s = s.copy(showWeek = it) }
            SwitchRow("Hiện dự báo 5 ngày", s.showForecast) { s = s.copy(showForecast = it) }
            Spacer(Modifier.height(6.dp))
            OutlinedButton(onClick = { refreshWeather() }, enabled = !loading) {
                Text(if (loading) "Đang tải…" else "Làm mới thời tiết")
            }
            weather?.let {
                Text(
                    "Cập nhật lúc " + java.time.Instant.ofEpochMilli(it.updatedAt)
                        .atZone(java.time.ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm dd/MM")) +
                        " • Nguồn: Open-Meteo",
                    fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // ===== Nút lưu =====
        Column(Modifier.padding(16.dp)) {
            Button(
                onClick = {
                    WidgetSettings.save(ctx, s)
                    CalendarWidgetProvider.updateAll(ctx)
                    Weather.refreshNow(ctx)
                    Toast.makeText(ctx, "Đã lưu và cập nhật widget", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) { Text("Lưu & cập nhật widget", fontSize = 16.sp) }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    WidgetSettings.save(ctx, s)
                    val mgr = AppWidgetManager.getInstance(ctx)
                    if (mgr.isRequestPinAppWidgetSupported) {
                        mgr.requestPinAppWidget(ComponentName(ctx, CalendarWidgetProvider::class.java), null, null)
                    } else {
                        Toast.makeText(ctx, "Nhấn giữ màn hình chính → Tiện ích → Lịch Vạn Niên", Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) { Text("Thêm widget vào màn hình chính") }
            Spacer(Modifier.navigationBarsPadding().height(12.dp))
        }
    }
}

// ---------------- Bản xem trước widget (vẽ bằng Compose, giống widget thật) ----------------

@Composable
private fun WidgetPreview(s: WidgetSettings, w: WeatherData?) {
    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) { while (true) { now = LocalTime.now(); delay(1000) } }
    val today = LocalDate.now()

    val primary = Color(s.textTone.color)
    val secondary = primary.copy(alpha = 0.7f)
    val darkText = s.textTone != TextTone.WHITE
    val shape = RoundedCornerShape(s.cornerRadius.dp)
    val g = s.gloss / 100f
    val borderColor = if (s.bgTone.dark) Color.White.copy(alpha = g * 0.43f) else Color.Black.copy(alpha = g * 0.16f)

    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(s.bgTone.color).copy(alpha = s.opacity / 100f))
            .then(
                if (g > 0) Modifier
                    .background(Brush.verticalGradient(0f to Color.White.copy(alpha = g * 0.35f), 0.6f to Color.Transparent))
                    .border(1.5.dp, borderColor, shape)
                else Modifier
            )
            .padding(16.dp)
    ) {
        Column {
            Row {
                Column(Modifier.weight(1f)) {
                    Text(VnText.greeting(now), fontSize = 13.sp, color = secondary)
                    Text(
                        now.format(DateTimeFormatter.ofPattern("HH:mm")),
                        fontSize = s.clockSize.sp, lineHeight = s.clockSize.sp,
                        fontWeight = FontWeight.Medium, color = primary
                    )
                    Text(VnText.solarLine(today), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = primary)
                    Text(
                        VnText.lunarLine(today), fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .padding(top = 5.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (darkText) Color(0xE6374151) else Color.White.copy(alpha = 0.25f))
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    val (desc, icon) = w?.let { Weather.describe(it.code, it.isDay) } ?: ("Đang tải…" to "🌡️")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(icon, fontSize = 26.sp)
                        Text(
                            w?.let { "${it.temp}°" } ?: "--°", fontSize = 34.sp, color = primary,
                            fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                    Text(desc, fontSize = 13.sp, color = primary, fontWeight = FontWeight.Medium)
                    val td = w?.days?.firstOrNull { it.date == today }
                    Text(
                        "📍 ${w?.cityName ?: s.city.name}" + (td?.let { " • ↑${it.max}° ↓${it.min}°" } ?: ""),
                        fontSize = 11.sp, color = secondary
                    )
                    Battery.read(LocalContext.current)?.let { b ->
                        Text(
                            Battery.label(b), fontSize = 11.sp, fontWeight = FontWeight.Medium,
                            color = if (darkText) Color(BATTERY_GREEN) else Color(0xFF4ADE80),
                            modifier = Modifier
                                .padding(top = 5.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (darkText) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.12f))
                                .padding(horizontal = 9.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            if (s.showWeek) {
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth()) {
                    val start = VnText.weekStart(today)
                    for (i in 0 until 7) {
                        val d = start.plusDays(i.toLong())
                        val isToday = d == today
                        Column(
                            Modifier
                                .weight(1f)
                                .padding(horizontal = 2.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isToday) Color(RED) else Color.Transparent)
                                .padding(vertical = 5.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                VnText.weekdayShort(d.dayOfWeek), fontSize = 11.sp,
                                color = if (isToday) Color.White else if (i == 6) Color(SUNDAY_RED) else secondary
                            )
                            Text(
                                d.dayOfMonth.toString(), fontSize = 17.sp, fontWeight = FontWeight.Bold,
                                color = if (isToday) Color.White else primary
                            )
                            Text(
                                VnText.lunarCell(d), fontSize = 10.sp,
                                color = if (isToday) Color.White.copy(alpha = 0.9f) else secondary
                            )
                        }
                    }
                }
            }

            val days = w?.days?.filter { !it.date.isBefore(today) }?.take(5).orEmpty()
            if (s.showForecast && days.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth()) {
                    days.forEach { d ->
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(VnText.forecastLabel(d.date, today), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = secondary)
                            Text(Weather.describe(d.code).second, fontSize = 18.sp)
                            Text("${d.min}°/${d.max}°", fontSize = 11.sp, color = primary, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}

// ---------------- Các thành phần giao diện cài đặt ----------------

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Text(title, color = Title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun Label(t: String) = Text(t, fontSize = 15.sp, color = Color(0xFF374151), modifier = Modifier.padding(top = 6.dp))

@Composable
private fun <T> ChoiceRow(items: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        items.forEach { item ->
            Row(
                Modifier.weight(1f).clickable { onSelect(item) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = item == selected, onClick = { onSelect(item) })
                Text(label(item), fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun SliderRow(label: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Text(label, fontSize = 15.sp, color = Color(0xFF374151), modifier = Modifier.padding(top = 8.dp))
    Slider(
        value = value.toFloat(),
        onValueChange = { onChange(it.toInt()) },
        valueRange = range.first.toFloat()..range.last.toFloat(),
    )
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CityPicker(index: Int, onSelect: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }) {
        OutlinedTextField(
            value = CITIES[index].name, onValueChange = {}, readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(open) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            CITIES.forEachIndexed { i, c ->
                DropdownMenuItem(text = { Text(c.name) }, onClick = { onSelect(i); open = false })
            }
        }
    }
}
