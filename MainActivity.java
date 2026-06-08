package com.gameoptimizer;

import android.app.*;
import android.content.*;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.io.*;

public class MainActivity extends Activity {

    private TextView tvStatus, tvCpu, tvRam, tvTemp;
    private Switch swGameMode, swKillApps, swDnd, swThermal;
    private Button btnApply, btnRestore;

    // FIX #1: Handler phải dùng Looper rõ ràng (tránh memory leak)
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable statsRunnable;
    private boolean optimized = false;

    // FIX #2: Lưu WifiLock để release đúng cách
    private WifiManager.WifiLock wifiLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        bindViews();
        startStatsLoop();
    }

    private void bindViews() {
        tvStatus   = findViewById(R.id.tvStatus);
        tvCpu      = findViewById(R.id.tvCpu);
        tvRam      = findViewById(R.id.tvRam);
        tvTemp     = findViewById(R.id.tvTemp);
        swGameMode = findViewById(R.id.swGameMode);
        swKillApps = findViewById(R.id.swKillApps);
        swDnd      = findViewById(R.id.swDnd);
        swThermal  = findViewById(R.id.swThermal);
        btnApply   = findViewById(R.id.btnApply);
        btnRestore = findViewById(R.id.btnRestore);

        swGameMode.setChecked(true);
        swKillApps.setChecked(true);
        swDnd.setChecked(true);
        swThermal.setChecked(true);

        btnApply.setOnClickListener(v -> applyOptimizations());
        btnRestore.setOnClickListener(v -> restoreDefaults());
    }

    // ─── TỐI ƯU HÓA CHÍNH ───────────────────────────────────────────────────

    private void applyOptimizations() {
        tvStatus.setText("⏳ Đang tối ưu hóa...");
        optimized = true;

        if (swKillApps.isChecked())  killBackgroundApps();
        if (swGameMode.isChecked())  enableGameMode();
        if (swDnd.isChecked())       enableDoNotDisturb();
        if (swThermal.isChecked())   applyThermalProtection();

        reduceAnimations();
        optimizeWifi();

        tvStatus.setText("✅ Đã tối ưu! FPS ngon hơn rồi 🎮");
        btnApply.setText("✅ Đã áp dụng");
        btnApply.setEnabled(false);
        btnRestore.setEnabled(true);
    }

    /**
     * Kill background apps — dùng KILL_BACKGROUND_PROCESSES (không cần root)
     */
    private void killBackgroundApps() {
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        if (am == null) return;
        for (ActivityManager.RunningAppProcessInfo proc : am.getRunningAppProcesses()) {
            if (proc.importance >= ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED) {
                am.killBackgroundProcesses(proc.processName);
            }
        }
    }

    /**
     * Game Mode: Android 12+ dùng GameManager, Android cũ dùng AudioManager fallback.
     * FIX #3: Bọc GameManager trong try-catch đúng cách, tránh crash trên máy cũ.
     */
    private void enableGameMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                // Chỉ gọi nếu API tồn tại, không crash nếu OEM tắt
                Object gm = getSystemService("game");
                if (gm != null) {
                    // GameManager.setGameMode(packageName, GAME_MODE_PERFORMANCE)
                    gm.getClass()
                      .getMethod("setGameMode", String.class, int.class)
                      .invoke(gm, getPackageName(), 2); // 2 = GAME_MODE_PERFORMANCE
                }
            } catch (Exception ignored) {
                // Không crash — máy không hỗ trợ thì bỏ qua
            }
        }
        // Fallback cho tất cả Android: giảm audio interrupt
        AudioManager audio = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (audio != null) {
            audio.setMode(AudioManager.MODE_IN_COMMUNICATION);
        }
    }

    /**
     * Do Not Disturb — chặn thông báo khi chơi game.
     */
    private void enableDoNotDisturb() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm == null) return;
        if (nm.isNotificationPolicyAccessGranted()) {
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE);
        } else {
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            startActivity(intent);
            Toast.makeText(this, "Hãy bật quyền Do Not Disturb cho app", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Bảo vệ nhiệt: đọc nhiệt độ pin qua BatteryManager (không cần BATTERY_STATS).
     * FIX #4: Bỏ BATTERY_STATS (bị restricted), dùng ACTION_BATTERY_CHANGED thay thế.
     */
    private void applyThermalProtection() {
        // Đọc nhiệt độ tức thời
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent status = registerReceiver(null, ifilter); // null = sticky broadcast, không cần unregister
        if (status != null) {
            int temp = status.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10;
            if (temp > 42) {
                Toast.makeText(this,
                    "⚠️ Nhiệt độ pin: " + temp + "°C — Hãy để máy nghỉ 5 phút!",
                    Toast.LENGTH_LONG).show();
            }
        }
        // Theo dõi liên tục
        registerReceiver(thermalReceiver, ifilter);
    }

    /**
     * Giảm animation scale — giao diện phản hồi nhanh hơn.
     * FIX #5: canWrite() kiểm tra đúng Settings.System (không phải Global).
     * Settings.Global.WINDOW_ANIMATION_SCALE vẫn dùng được nếu có WRITE_SETTINGS.
     */
    private void reduceAnimations() {
        if (Settings.System.canWrite(this)) {
            try {
                Settings.Global.putFloat(getContentResolver(),
                    Settings.Global.WINDOW_ANIMATION_SCALE, 0.5f);
                Settings.Global.putFloat(getContentResolver(),
                    Settings.Global.TRANSITION_ANIMATION_SCALE, 0.5f);
                Settings.Global.putFloat(getContentResolver(),
                    Settings.Global.ANIMATOR_DURATION_SCALE, 0.5f);
            } catch (SecurityException e) {
                // Android 12+ có thể từ chối — bắt lỗi thay vì crash
                Toast.makeText(this, "Không thể giảm animation (cần cấp thêm quyền)", Toast.LENGTH_SHORT).show();
            }
        } else {
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(android.net.Uri.parse("package:" + getPackageName()));
            startActivity(intent);
            Toast.makeText(this, "Cấp quyền 'Thay đổi cài đặt hệ thống' để tối ưu animation", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * WiFi Lock — giữ kết nối ổn định trong game online.
     * FIX #6: Lưu lock vào field để release đúng lúc, tránh hao pin.
     */
    private void optimizeWifi() {
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        if (wm != null && wifiLock == null) {
            wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "GameOptimizerLock");
            wifiLock.acquire();
        }
    }

    // ─── KHÔI PHỤC MẶC ĐỊNH ──────────────────────────────────────────────────

    private void restoreDefaults() {
        // Khôi phục animation
        if (Settings.System.canWrite(this)) {
            try {
                Settings.Global.putFloat(getContentResolver(), Settings.Global.WINDOW_ANIMATION_SCALE, 1.0f);
                Settings.Global.putFloat(getContentResolver(), Settings.Global.TRANSITION_ANIMATION_SCALE, 1.0f);
                Settings.Global.putFloat(getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f);
            } catch (SecurityException ignored) {}
        }
        // Tắt DND
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null && nm.isNotificationPolicyAccessGranted()) {
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
        }
        // Khôi phục audio
        AudioManager audio = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (audio != null) audio.setMode(AudioManager.MODE_NORMAL);

        // FIX #6: Release WiFi lock khi restore
        if (wifiLock != null && wifiLock.isHeld()) {
            wifiLock.release();
            wifiLock = null;
        }

        optimized = false;
        tvStatus.setText("🔄 Đã khôi phục về mặc định");
        btnApply.setText("⚡ TỐI ƯU NGAY");
        btnApply.setEnabled(true);
        btnRestore.setEnabled(false);
    }

    // ─── THEO DÕI NHIỆT ĐỘ ──────────────────────────────────────────────────

    private final BroadcastReceiver thermalReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            int temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10;
            if (tvTemp == null) return;
            tvTemp.setText(temp + "°C");
            if (temp > 45) {
                tvTemp.setTextColor(0xFFFF6D00);
                if (optimized) Toast.makeText(ctx,
                    "🔥 Máy quá nóng (" + temp + "°C)! Nghỉ game 5 phút!", Toast.LENGTH_LONG).show();
            } else if (temp > 38) {
                tvTemp.setTextColor(0xFFFFAB00);
            } else {
                tvTemp.setTextColor(0xFF76FF03);
            }
        }
    };

    // ─── CPU & RAM STATS (chạy trên background thread) ──────────────────────

    /**
     * FIX #1 CHÍNH: updateCpuUsage() KHÔNG được chạy trên Main Thread vì có Thread.sleep().
     * Giải pháp: chạy trong Thread riêng, post kết quả lên UI thread bằng handler.
     */
    private void startStatsLoop() {
        statsRunnable = new Runnable() {
            @Override
            public void run() {
                // Đọc CPU trên background thread để tránh ANR
                new Thread(() -> {
                    final String cpuText = readCpuUsage();
                    handler.post(() -> {
                        if (tvCpu != null) tvCpu.setText(cpuText);
                    });
                }).start();

                // RAM đọc nhanh, OK trên main thread
                updateRamUsage();

                handler.postDelayed(this, 2000);
            }
        };
        handler.post(statsRunnable);
    }

    private String readCpuUsage() {
        try (RandomAccessFile reader = new RandomAccessFile("/proc/stat", "r")) {
            String[] t1 = reader.readLine().split("\\s+");
            long idle1 = Long.parseLong(t1[4]);
            long total1 = 0;
            for (int i = 1; i < t1.length; i++) total1 += Long.parseLong(t1[i]);

            Thread.sleep(300); // Safe: đang trên background thread

            reader.seek(0);
            String[] t2 = reader.readLine().split("\\s+");
            long idle2 = Long.parseLong(t2[4]);
            long total2 = 0;
            for (int i = 1; i < t2.length; i++) total2 += Long.parseLong(t2[i]);

            long totalDiff = total2 - total1;
            long idleDiff  = idle2  - idle1;
            if (totalDiff == 0) return "--";
            int cpu = (int)(100L * (totalDiff - idleDiff) / totalDiff);
            return cpu + "%";
        } catch (Exception e) {
            return "--";
        }
    }

    private void updateRamUsage() {
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        if (am == null || tvRam == null) return;
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        long freeMB = mi.availMem / (1024 * 1024);
        tvRam.setText(freeMB + " MB");
    }

    // ─── LIFECYCLE ──────────────────────────────────────────────────────────

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop stats loop
        handler.removeCallbacks(statsRunnable);
        // Unregister thermal receiver an toàn
        try { unregisterReceiver(thermalReceiver); } catch (IllegalArgumentException ignored) {}
        // Khôi phục nếu đang optimize
        if (optimized) restoreDefaults();
    }
}
