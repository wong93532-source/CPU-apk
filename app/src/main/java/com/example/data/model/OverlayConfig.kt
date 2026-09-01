package com.example.data.model

enum class FloatingWindowStyle(val label: String, val description: String) {
    COMPACT_PILL("极简胶囊 (Pill)", "悬浮屏幕边缘，高频核心指标一览"),
    MINIMAL_BADGE("微型徽章 (Badge)", "超紧凑正方形，极低遮挡"),
    DETAILED_HUD("科技HUD (HUD)", "全指标科技仪表盘，带动态指示条"),
    FULL_CARD("完整卡片 (Card)", "全维度详细信息与波形图")
}

enum class TemperatureUnit(val symbol: String) {
    CELSIUS("°C"),
    FAHRENHEIT("°F")
}

enum class HistoryTimeWindow(val durationSeconds: Long, val label: String, val pointCount: Int) {
    ONE_MINUTE(60L, "1分钟", 60),
    FIVE_MINUTES(300L, "5分钟", 60),
    FIFTEEN_MINUTES(900L, "15分钟", 60)
}

enum class HistoryMetricType(val label: String) {
    ALL("综合 (ALL)"),
    GPU("GPU 占用"),
    CPU("CPU 占用"),
    RAM("内存 (RAM)"),
    TEMP("手机温度")
}

data class OverlayConfig(
    val isOverlayEnabled: Boolean = false,
    val transparency: Float = 0.85f,           // 0.20f to 1.0f
    val posX: Int = 50,                        // initial X in px
    val posY: Int = 100,                       // initial Y in px
    val style: FloatingWindowStyle = FloatingWindowStyle.DETAILED_HUD,
    val showGpu: Boolean = true,
    val showCpu: Boolean = true,
    val showRam: Boolean = true,
    val showTemp: Boolean = true,
    val showFps: Boolean = true,
    val showCoreTemps: Boolean = true,         // Show per-core & GPU individual temperatures
    val showHistoryCharts: Boolean = true,     // Show performance charts in dashboard
    val updateIntervalMs: Long = 1000L,
    val isLocked: Boolean = false,              // If true, dragging disabled to prevent mis-clicks
    val tempUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val isCollapsed: Boolean = false,           // Tap to collapse into mini dot/pill

    // Custom Alert Thresholds & Notification Settings
    val alertGpuEnabled: Boolean = true,
    val alertGpuThreshold: Float = 90f,        // 0-100%
    val alertCpuEnabled: Boolean = true,
    val alertCpuThreshold: Float = 90f,        // 0-100%
    val alertRamEnabled: Boolean = true,
    val alertRamThreshold: Float = 88f,        // 0-100%
    val alertTempEnabled: Boolean = true,
    val alertTempThreshold: Float = 45f,       // in Celsius (e.g. 45°C)
    val alertNotificationEnabled: Boolean = true,
    val alertVibrationEnabled: Boolean = true,
    val alertOverlayHighlight: Boolean = true
)
