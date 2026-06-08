# ⚡ Android Game Optimizer

Ứng dụng tối ưu hiệu năng game cho **mọi dòng Android** (5.0+), **không cần root**, an toàn cho linh kiện.

---

## 📦 Cách build APK (5 bước)

### Yêu cầu
- Android Studio Hedgehog (2023.1.1) trở lên
- JDK 17+

### Bước 1 — Mở project
```
File → Open → chọn thư mục GameOptimizer
```

### Bước 2 — Sync Gradle
Android Studio sẽ tự hỏi → nhấn **Sync Now**

### Bước 3 — Build APK
```
Build → Build Bundle(s) / APK(s) → Build APK(s)
```

### Bước 4 — Tìm file APK
```
app/build/outputs/apk/debug/app-debug.apk
```

### Bước 5 — Cài lên điện thoại
- Bật **Cài từ nguồn không rõ** trong Settings
- Copy file APK sang điện thoại và cài

---

## 🔧 Tính năng

| Tính năng | Cần root? | Mô tả |
|-----------|-----------|-------|
| Kill RAM | ❌ | Dọn app chạy ngầm |
| Game Mode | ❌ | Ưu tiên CPU cho game (Android 12+) |
| Do Not Disturb | ❌ | Chặn thông báo |
| Giám sát nhiệt | ❌ | Cảnh báo khi > 42°C |
| Giảm animation | ❌* | Giao diện mượt hơn |
| WiFi Lock | ❌ | Không cho WiFi sleep |

*Cần cấp quyền WRITE_SETTINGS lần đầu

---

## 🛡 An toàn linh kiện

- **KHÔNG** ép xung CPU (overclock)
- **KHÔNG** tắt thermal throttling
- Cảnh báo nhiệt ở 42°C (ngưỡng an toàn)
- Khôi phục toàn bộ cài đặt khi thoát app
- Chỉ dùng API chính thức Android

---

## 📁 Cấu trúc project

```
GameOptimizer/
├── app/src/main/
│   ├── java/com/gameoptimizer/
│   │   └── MainActivity.java    ← Logic chính
│   ├── res/layout/
│   │   └── activity_main.xml   ← Giao diện
│   ├── res/values/
│   │   └── styles.xml          ← Theme tối
│   └── AndroidManifest.xml     ← Quyền hạn
├── app/build.gradle
├── build.gradle
└── settings.gradle
```
