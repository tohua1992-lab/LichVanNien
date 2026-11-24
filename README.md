# Lịch Vạn Niên – Widget Android

Widget màn hình chính gồm: đồng hồ, lời chào theo buổi, ngày dương + âm lịch (can chi), dải lịch tuần có ngày âm, thời tiết hiện tại + dự báo 5 ngày (Open-Meteo, miễn phí, không cần API key), % pin và nhiệt độ pin. Mở app để chỉnh giao diện có **xem trước trực tiếp**.

## Tính năng chỉnh trong app
1. **Kính lỏng & khung viền**: tông nền (Trắng / Khói tối / Xanh băng / Kem ấm), độ đục, bo góc, độ bóng & viền kính
2. **Đồng hồ & màu sắc**: cỡ chữ đồng hồ (32–64sp), màu chữ (Đen than / Trắng sáng / Xanh Navy)
3. **Thời tiết & hiển thị**: chọn thành phố (16 tỉnh/thành), bật/tắt dải tuần và dự báo
- Nút **Thêm widget vào màn hình chính** (ghim widget trực tiếp, Android 8+)

## Cách 1 – Build bằng GitHub (không cần cài gì)
1. Tạo repo mới trên GitHub, upload toàn bộ thư mục này (giữ nguyên cả thư mục `.github`).
2. Vào tab **Actions** → workflow **Build APK** tự chạy (khoảng 5–8 phút).
3. Mở lần chạy đã xong → mục **Artifacts** → tải `LichVanNien-apk` → giải nén ra file `.apk`.
4. Chép APK vào điện thoại, mở để cài (cho phép "Cài ứng dụng không rõ nguồn gốc").

## Cách 2 – Android Studio
1. Cài Android Studio (bản mới), chọn **Open** → thư mục `LichVanNien`.
2. Chờ Gradle sync xong, cắm điện thoại (bật USB debugging) → bấm ▶ Run.
3. Hoặc **Build → Build App Bundle(s)/APK(s) → Build APK(s)**.

## Sau khi cài
- Nhấn giữ màn hình chính → **Tiện ích (Widgets)** → **Lịch Vạn Niên**, kéo ra (khuyên dùng 4×3).
- Chạm vào widget để mở màn hình cài đặt, chỉnh xong bấm **Lưu & cập nhật widget**.
- Một số máy (Xiaomi, Oppo, Vivo, Samsung) hay chặn chạy nền: nên vào Cài đặt pin → cho app "Không giới hạn" để thời tiết tự cập nhật mỗi giờ.

## Cấu trúc mã nguồn
| File | Vai trò |
|---|---|
| `LunarCalendar.kt` | Đổi dương → âm lịch Việt Nam (thuật toán Hồ Ngọc Đức, múi giờ +7), tên năm can chi |
| `CalendarWidgetProvider.kt` | Vẽ widget (RemoteViews), cập nhật 30 phút/lần + lúc nửa đêm |
| `BackgroundRenderer.kt` | Vẽ nền kính (màu, độ đục, bo góc, viền bóng); đọc pin |
| `Weather.kt` | Gọi Open-Meteo, lưu cache, cập nhật mỗi giờ (WorkManager) |
| `WidgetSettings.kt` | Lưu cài đặt, danh sách thành phố, chuỗi tiếng Việt |
| `MainActivity.kt` | Màn hình cài đặt (Jetpack Compose) + xem trước trực tiếp |
| `res/layout/widget_calendar.xml` | Bố cục widget |

Thêm thành phố: sửa danh sách `CITIES` trong `WidgetSettings.kt` (tên, vĩ độ, kinh độ).
