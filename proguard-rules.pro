# Giữ lại tên class Activity (không obfuscate)
-keep public class com.gameoptimizer.** { *; }
# Giữ lại các method được gọi qua reflection (GameManager)
-keepclassmembers class * {
    public void setGameMode(java.lang.String, int);
}
