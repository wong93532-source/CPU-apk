package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.collector.ActiveAlertState
import com.example.data.collector.HardwareAlertManager
import com.example.data.collector.HardwareCollector
import com.example.data.model.FloatingWindowStyle
import com.example.data.model.HardwareMetrics
import com.example.data.model.HistoryMetricType
import com.example.data.model.HistorySample
import com.example.data.model.HistoryTimeWindow
import com.example.data.model.OverlayConfig
import com.example.data.model.TemperatureUnit
import com.example.data.preferences.PreferencesManager
import com.example.service.FloatingMonitorService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class PositionPreset(val label: String) {
    TOP_CENTER("顶部居中"),
    TOP_LEFT("左上角"),
    TOP_RIGHT("右上角"),
    BOTTOM_CENTER("底部居中"),
    MIDDLE_RIGHT("右侧居中")
}

data class TimeframeDataSeries(
    val samples: List<HistorySample> = emptyList(),
    val minGpu: Float = 0f,
    val maxGpu: Float = 0f,
    val avgGpu: Float = 0f,
    val minCpu: Float = 0f,
    val maxCpu: Float = 0f,
    val avgCpu: Float = 0f,
    val minRam: Float = 0f,
    val maxRam: Float = 0f,
    val avgRam: Float = 0f,
    val minTemp: Float = 0f,
    val maxTemp: Float = 0f,
    val avgTemp: Float = 0f
)

class MonitorViewModel(application: Application) : AndroidViewModel(application) {

    private val collector = HardwareCollector(application)
    private val prefsManager = PreferencesManager(application)
    private val alertManager = HardwareAlertManager(application)

    private val _metrics = MutableStateFlow(HardwareMetrics())
    val metrics: StateFlow<HardwareMetrics> = _metrics.asStateFlow()

    val config: StateFlow<OverlayConfig> = prefsManager.configFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, prefsManager.loadConfig())

    val activeAlertState: StateFlow<ActiveAlertState> = alertManager.alertState

    private val _selectedTimeWindow = MutableStateFlow(HistoryTimeWindow.ONE_MINUTE)
    val selectedTimeWindow: StateFlow<HistoryTimeWindow> = _selectedTimeWindow.asStateFlow()

    private val _selectedHistoryMetric = MutableStateFlow(HistoryMetricType.ALL)
    val selectedHistoryMetric: StateFlow<HistoryMetricType> = _selectedHistoryMetric.asStateFlow()
    val selectedMetricType: StateFlow<HistoryMetricType> = _selectedHistoryMetric.asStateFlow()

    private val _hasOverlayPermission = MutableStateFlow(checkPermission(application))
    val hasOverlayPermission: StateFlow<Boolean> = _hasOverlayPermission.asStateFlow()

    private var samplingJob: Job? = null

    init {
        startSampling()
    }

    private fun checkPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    fun refreshPermissionStatus(context: Context) {
        _hasOverlayPermission.value = checkPermission(context)
    }

    fun requestOverlayPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(intent)
            } catch (_: Exception) {
                val fallback = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallback)
            }
        }
    }

    private fun startSampling() {
        samplingJob?.cancel()
        samplingJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                val data = collector.sampleMetrics()
                _metrics.value = data

                // Evaluate threshold alerts
                alertManager.evaluateMetrics(data, config.value)

                val interval = config.value.updateIntervalMs.coerceIn(500L, 5000L)
                delay(interval)
            }
        }
    }

    fun setHistoryTimeWindow(window: HistoryTimeWindow) {
        _selectedTimeWindow.value = window
    }

    fun setTimeWindow(window: HistoryTimeWindow) {
        _selectedTimeWindow.value = window
    }

    fun setHistoryMetric(metric: HistoryMetricType) {
        _selectedHistoryMetric.value = metric
    }

    fun setMetricType(metric: HistoryMetricType) {
        _selectedHistoryMetric.value = metric
    }

    fun getTimeframeDataSeries(timeWindow: HistoryTimeWindow): TimeframeDataSeries {
        return getTimeframeDataSeries(timeWindow, _metrics.value.fullHistorySamples)
    }

    /**
     * Extracts and computes statistics for the selected time window (1m, 5m, 15m)
     */
    fun getTimeframeDataSeries(timeWindow: HistoryTimeWindow, allSamples: List<HistorySample>): TimeframeDataSeries {
        if (allSamples.isEmpty()) return TimeframeDataSeries()

        val now = System.currentTimeMillis()
        val durationMs = timeWindow.durationSeconds * 1000L
        val cutoff = now - durationMs

        val windowSamples = allSamples.filter { it.timestamp >= cutoff }
        val targetSamples = if (windowSamples.isNotEmpty()) windowSamples else allSamples.takeLast(timeWindow.durationSeconds.toInt())

        // Downsample to a clean 60 points if window is large (e.g. 5m or 15m)
        val sampledList = if (targetSamples.size > 60) {
            val step = targetSamples.size / 60.0
            (0 until 60).map { i ->
                val index = (i * step).toInt().coerceIn(0, targetSamples.size - 1)
                targetSamples[index]
            }
        } else {
            targetSamples
        }

        if (sampledList.isEmpty()) return TimeframeDataSeries()

        val gpuVals = sampledList.map { it.gpuUsage }
        val cpuVals = sampledList.map { it.cpuUsage }
        val ramVals = sampledList.map { it.ramUsagePercent }
        val tempVals = sampledList.map { it.temperature }

        return TimeframeDataSeries(
            samples = sampledList,
            minGpu = gpuVals.minOrNull() ?: 0f,
            maxGpu = gpuVals.maxOrNull() ?: 0f,
            avgGpu = (gpuVals.average()).toFloat(),
            minCpu = cpuVals.minOrNull() ?: 0f,
            maxCpu = cpuVals.maxOrNull() ?: 0f,
            avgCpu = (cpuVals.average()).toFloat(),
            minRam = ramVals.minOrNull() ?: 0f,
            maxRam = ramVals.maxOrNull() ?: 0f,
            avgRam = (ramVals.average()).toFloat(),
            minTemp = tempVals.minOrNull() ?: 0f,
            maxTemp = tempVals.maxOrNull() ?: 0f,
            avgTemp = (tempVals.average()).toFloat()
        )
    }

    fun toggleOverlayService(context: Context) {
        val current = config.value
        if (!current.isOverlayEnabled) {
            if (!checkPermission(context)) {
                requestOverlayPermission(context)
                return
            }
            val updated = current.copy(isOverlayEnabled = true)
            prefsManager.updateConfig(updated)
            FloatingMonitorService.startService(context)
        } else {
            val updated = current.copy(isOverlayEnabled = false)
            prefsManager.updateConfig(updated)
            FloatingMonitorService.stopService(context)
        }
    }

    fun setTransparency(alpha: Float) {
        prefsManager.updateTransparency(alpha)
    }

    fun setStyle(style: FloatingWindowStyle) {
        val current = config.value
        prefsManager.updateConfig(current.copy(style = style))
    }

    fun setPosition(x: Int, y: Int) {
        prefsManager.updatePosition(x, y)
    }

    fun applyPositionPreset(preset: PositionPreset, screenWidth: Int, screenHeight: Int) {
        val (x, y) = when (preset) {
            PositionPreset.TOP_CENTER -> Pair((screenWidth / 2) - 130, 80)
            PositionPreset.TOP_LEFT -> Pair(20, 80)
            PositionPreset.TOP_RIGHT -> Pair(screenWidth - 260, 80)
            PositionPreset.BOTTOM_CENTER -> Pair((screenWidth / 2) - 130, screenHeight - 200)
            PositionPreset.MIDDLE_RIGHT -> Pair(screenWidth - 260, screenHeight / 2)
        }
        prefsManager.updatePosition(x.coerceAtLeast(10), y.coerceAtLeast(40))
    }

    fun toggleMetric(
        gpu: Boolean? = null,
        cpu: Boolean? = null,
        ram: Boolean? = null,
        temp: Boolean? = null,
        fps: Boolean? = null,
        coreTemps: Boolean? = null
    ) {
        val current = config.value
        val updated = current.copy(
            showGpu = gpu ?: current.showGpu,
            showCpu = cpu ?: current.showCpu,
            showRam = ram ?: current.showRam,
            showTemp = temp ?: current.showTemp,
            showFps = fps ?: current.showFps,
            showCoreTemps = coreTemps ?: current.showCoreTemps
        )
        prefsManager.updateConfig(updated)
    }

    // Alert Configuration Methods
    fun setGpuThreshold(threshold: Float) = updateAlertThreshold(gpu = threshold)
    fun toggleGpuAlert(enabled: Boolean) = toggleAlertEnabled(gpu = enabled)
    fun setCpuThreshold(threshold: Float) = updateAlertThreshold(cpu = threshold)
    fun toggleCpuAlert(enabled: Boolean) = toggleAlertEnabled(cpu = enabled)
    fun setRamThreshold(threshold: Float) = updateAlertThreshold(ram = threshold)
    fun toggleRamAlert(enabled: Boolean) = toggleAlertEnabled(ram = enabled)
    fun setTempThreshold(threshold: Float) = updateAlertThreshold(temp = threshold)
    fun toggleTempAlert(enabled: Boolean) = toggleAlertEnabled(temp = enabled)

    fun toggleCoreTemps() {
        val current = config.value
        prefsManager.updateConfig(current.copy(showCoreTemps = !current.showCoreTemps))
    }

    fun toggleHistoryCharts() {
        val current = config.value
        prefsManager.updateConfig(current.copy(showHistoryCharts = !current.showHistoryCharts))
    }

    fun updateAlertThreshold(gpu: Float? = null, cpu: Float? = null, ram: Float? = null, temp: Float? = null) {
        val current = config.value
        val updated = current.copy(
            alertGpuThreshold = gpu ?: current.alertGpuThreshold,
            alertCpuThreshold = cpu ?: current.alertCpuThreshold,
            alertRamThreshold = ram ?: current.alertRamThreshold,
            alertTempThreshold = temp ?: current.alertTempThreshold
        )
        prefsManager.updateConfig(updated)
    }

    fun toggleAlertEnabled(gpu: Boolean? = null, cpu: Boolean? = null, ram: Boolean? = null, temp: Boolean? = null) {
        val current = config.value
        val updated = current.copy(
            alertGpuEnabled = gpu ?: current.alertGpuEnabled,
            alertCpuEnabled = cpu ?: current.alertCpuEnabled,
            alertRamEnabled = ram ?: current.alertRamEnabled,
            alertTempEnabled = temp ?: current.alertTempEnabled
        )
        prefsManager.updateConfig(updated)
    }

    fun toggleAlertNotification(enabled: Boolean) {
        val current = config.value
        prefsManager.updateConfig(current.copy(alertNotificationEnabled = enabled))
    }

    fun toggleAlertVibration(enabled: Boolean) {
        val current = config.value
        prefsManager.updateConfig(current.copy(alertVibrationEnabled = enabled))
    }

    fun toggleAlertOverlayHighlight(enabled: Boolean) {
        val current = config.value
        prefsManager.updateConfig(current.copy(alertOverlayHighlight = enabled))
    }

    fun triggerTestAlert() {
        alertManager.triggerTestNotification(config.value)
    }

    fun setUpdateInterval(intervalMs: Long) {
        val current = config.value
        prefsManager.updateConfig(current.copy(updateIntervalMs = intervalMs))
    }

    fun toggleLock() {
        val current = config.value
        prefsManager.updateConfig(current.copy(isLocked = !current.isLocked))
    }

    fun toggleTempUnit() {
        val current = config.value
        val nextUnit = if (current.tempUnit == TemperatureUnit.CELSIUS) TemperatureUnit.FAHRENHEIT else TemperatureUnit.CELSIUS
        prefsManager.updateConfig(current.copy(tempUnit = nextUnit))
    }

    fun toggleCollapsed() {
        val current = config.value
        prefsManager.updateCollapsed(!current.isCollapsed)
    }
}

