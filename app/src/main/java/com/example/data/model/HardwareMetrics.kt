package com.example.data.model

data class HistorySample(
    val timestamp: Long,
    val gpuUsage: Float,
    val cpuUsage: Float,
    val ramUsagePercent: Float,
    val temperature: Float
)

data class HardwareMetrics(
    val gpuUsage: Float = 0f,              // 0% - 100%
    val gpuFreqMhz: Int = 0,               // e.g. 587 MHz
    val gpuModel: String = "Adreno / Mali GPU",
    val gpuTemperature: Float = 0f,        // GPU core temp in Celsius
    val cpuUsage: Float = 0f,              // 0% - 100%
    val cpuFreqMhz: Int = 0,               // Max/Current avg freq
    val cpuCoreCount: Int = 8,
    val coreUsages: List<CoreMetric> = emptyList(),
    val ramUsedBytes: Long = 0L,
    val ramTotalBytes: Long = 0L,
    val ramUsagePercent: Float = 0f,       // 0% - 100%
    val ramAvailableBytes: Long = 0L,
    val batteryTemperature: Float = 0f,    // in Celsius (e.g. 32.5°C)
    val socTemperature: Float = 0f,        // in Celsius
    val batteryLevel: Int = 0,             // 0% - 100%
    val isCharging: Boolean = false,
    val fps: Int = 60,
    val timestamp: Long = System.currentTimeMillis(),
    val gpuLoadHistory: List<Float> = emptyList(),
    val cpuLoadHistory: List<Float> = emptyList(),
    val ramLoadHistory: List<Float> = emptyList(),
    val tempHistory: List<Float> = emptyList(),
    val fullHistorySamples: List<HistorySample> = emptyList() // Timestamped samples for multi-timeframe analysis
) {
    val ramUsedGbFormatted: String
        get() = String.format("%.1f GB", ramUsedBytes / (1024.0 * 1024.0 * 1024.0))

    val ramTotalGbFormatted: String
        get() = String.format("%.1f GB", ramTotalBytes / (1024.0 * 1024.0 * 1024.0))

    val effectiveTemp: Float
        get() = if (socTemperature > 0f) socTemperature else batteryTemperature

    val effectiveGpuTemp: Float
        get() = if (gpuTemperature > 0f) gpuTemperature else (effectiveTemp + (gpuUsage / 100f * 4.5f))

    val effectiveCpuTemp: Float
        get() {
            val measured = coreUsages.filter { it.temperatureCelsius > 0f }
            return if (measured.isNotEmpty()) measured.map { it.temperatureCelsius }.average().toFloat() else (effectiveTemp + (cpuUsage / 100f * 3.5f))
        }
}

data class CoreMetric(
    val coreIndex: Int,
    val usagePercent: Float,
    val curFreqMhz: Int,
    val maxFreqMhz: Int,
    val temperatureCelsius: Float = 0f
)
