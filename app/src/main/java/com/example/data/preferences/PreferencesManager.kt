package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.FloatingWindowStyle
import com.example.data.model.OverlayConfig
import com.example.data.model.TemperatureUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("gpu_monitor_prefs", Context.MODE_PRIVATE)

    private val _configFlow = MutableStateFlow(loadConfig())
    val configFlow: StateFlow<OverlayConfig> = _configFlow.asStateFlow()

    fun loadConfig(): OverlayConfig {
        val styleName = prefs.getString(KEY_STYLE, FloatingWindowStyle.DETAILED_HUD.name)
        val style = try {
            FloatingWindowStyle.valueOf(styleName ?: FloatingWindowStyle.DETAILED_HUD.name)
        } catch (_: Exception) {
            FloatingWindowStyle.DETAILED_HUD
        }

        val tempUnitName = prefs.getString(KEY_TEMP_UNIT, TemperatureUnit.CELSIUS.name)
        val tempUnit = try {
            TemperatureUnit.valueOf(tempUnitName ?: TemperatureUnit.CELSIUS.name)
        } catch (_: Exception) {
            TemperatureUnit.CELSIUS
        }

        return OverlayConfig(
            isOverlayEnabled = prefs.getBoolean(KEY_IS_ENABLED, false),
            transparency = prefs.getFloat(KEY_TRANSPARENCY, 0.88f),
            posX = prefs.getInt(KEY_POS_X, 40),
            posY = prefs.getInt(KEY_POS_Y, 120),
            style = style,
            showGpu = prefs.getBoolean(KEY_SHOW_GPU, true),
            showCpu = prefs.getBoolean(KEY_SHOW_CPU, true),
            showRam = prefs.getBoolean(KEY_SHOW_RAM, true),
            showTemp = prefs.getBoolean(KEY_SHOW_TEMP, true),
            showFps = prefs.getBoolean(KEY_SHOW_FPS, true),
            showCoreTemps = prefs.getBoolean(KEY_SHOW_CORE_TEMPS, true),
            updateIntervalMs = prefs.getLong(KEY_INTERVAL, 1000L),
            isLocked = prefs.getBoolean(KEY_IS_LOCKED, false),
            tempUnit = tempUnit,
            isCollapsed = prefs.getBoolean(KEY_IS_COLLAPSED, false),
            alertGpuEnabled = prefs.getBoolean(KEY_ALERT_GPU_ENABLED, true),
            alertGpuThreshold = prefs.getFloat(KEY_ALERT_GPU_THRESHOLD, 90f),
            alertCpuEnabled = prefs.getBoolean(KEY_ALERT_CPU_ENABLED, true),
            alertCpuThreshold = prefs.getFloat(KEY_ALERT_CPU_THRESHOLD, 90f),
            alertRamEnabled = prefs.getBoolean(KEY_ALERT_RAM_ENABLED, true),
            alertRamThreshold = prefs.getFloat(KEY_ALERT_RAM_THRESHOLD, 88f),
            alertTempEnabled = prefs.getBoolean(KEY_ALERT_TEMP_ENABLED, true),
            alertTempThreshold = prefs.getFloat(KEY_ALERT_TEMP_THRESHOLD, 45f),
            alertNotificationEnabled = prefs.getBoolean(KEY_ALERT_NOTIFICATION_ENABLED, true),
            alertVibrationEnabled = prefs.getBoolean(KEY_ALERT_VIBRATION_ENABLED, true),
            alertOverlayHighlight = prefs.getBoolean(KEY_ALERT_OVERLAY_HIGHLIGHT, true)
        )
    }

    fun updateConfig(config: OverlayConfig) {
        prefs.edit()
            .putBoolean(KEY_IS_ENABLED, config.isOverlayEnabled)
            .putFloat(KEY_TRANSPARENCY, config.transparency)
            .putInt(KEY_POS_X, config.posX)
            .putInt(KEY_POS_Y, config.posY)
            .putString(KEY_STYLE, config.style.name)
            .putBoolean(KEY_SHOW_GPU, config.showGpu)
            .putBoolean(KEY_SHOW_CPU, config.showCpu)
            .putBoolean(KEY_SHOW_RAM, config.showRam)
            .putBoolean(KEY_SHOW_TEMP, config.showTemp)
            .putBoolean(KEY_SHOW_FPS, config.showFps)
            .putBoolean(KEY_SHOW_CORE_TEMPS, config.showCoreTemps)
            .putLong(KEY_INTERVAL, config.updateIntervalMs)
            .putBoolean(KEY_IS_LOCKED, config.isLocked)
            .putString(KEY_TEMP_UNIT, config.tempUnit.name)
            .putBoolean(KEY_IS_COLLAPSED, config.isCollapsed)
            .putBoolean(KEY_ALERT_GPU_ENABLED, config.alertGpuEnabled)
            .putFloat(KEY_ALERT_GPU_THRESHOLD, config.alertGpuThreshold)
            .putBoolean(KEY_ALERT_CPU_ENABLED, config.alertCpuEnabled)
            .putFloat(KEY_ALERT_CPU_THRESHOLD, config.alertCpuThreshold)
            .putBoolean(KEY_ALERT_RAM_ENABLED, config.alertRamEnabled)
            .putFloat(KEY_ALERT_RAM_THRESHOLD, config.alertRamThreshold)
            .putBoolean(KEY_ALERT_TEMP_ENABLED, config.alertTempEnabled)
            .putFloat(KEY_ALERT_TEMP_THRESHOLD, config.alertTempThreshold)
            .putBoolean(KEY_ALERT_NOTIFICATION_ENABLED, config.alertNotificationEnabled)
            .putBoolean(KEY_ALERT_VIBRATION_ENABLED, config.alertVibrationEnabled)
            .putBoolean(KEY_ALERT_OVERLAY_HIGHLIGHT, config.alertOverlayHighlight)
            .apply()

        _configFlow.value = config
    }

    fun updatePosition(x: Int, y: Int) {
        val current = _configFlow.value
        val updated = current.copy(posX = x, posY = y)
        prefs.edit()
            .putInt(KEY_POS_X, x)
            .putInt(KEY_POS_Y, y)
            .apply()
        _configFlow.value = updated
    }

    fun updateTransparency(transparency: Float) {
        val current = _configFlow.value
        val updated = current.copy(transparency = transparency)
        prefs.edit().putFloat(KEY_TRANSPARENCY, transparency).apply()
        _configFlow.value = updated
    }

    fun updateCollapsed(isCollapsed: Boolean) {
        val current = _configFlow.value
        val updated = current.copy(isCollapsed = isCollapsed)
        prefs.edit().putBoolean(KEY_IS_COLLAPSED, isCollapsed).apply()
        _configFlow.value = updated
    }

    companion object {
        private const val KEY_IS_ENABLED = "key_is_enabled"
        private const val KEY_TRANSPARENCY = "key_transparency"
        private const val KEY_POS_X = "key_pos_x"
        private const val KEY_POS_Y = "key_pos_y"
        private const val KEY_STYLE = "key_style"
        private const val KEY_SHOW_GPU = "key_show_gpu"
        private const val KEY_SHOW_CPU = "key_show_cpu"
        private const val KEY_SHOW_RAM = "key_show_ram"
        private const val KEY_SHOW_TEMP = "key_show_temp"
        private const val KEY_SHOW_FPS = "key_show_fps"
        private const val KEY_SHOW_CORE_TEMPS = "key_show_core_temps"
        private const val KEY_INTERVAL = "key_interval"
        private const val KEY_IS_LOCKED = "key_is_locked"
        private const val KEY_TEMP_UNIT = "key_temp_unit"
        private const val KEY_IS_COLLAPSED = "key_is_collapsed"
        private const val KEY_ALERT_GPU_ENABLED = "key_alert_gpu_enabled"
        private const val KEY_ALERT_GPU_THRESHOLD = "key_alert_gpu_threshold"
        private const val KEY_ALERT_CPU_ENABLED = "key_alert_cpu_enabled"
        private const val KEY_ALERT_CPU_THRESHOLD = "key_alert_cpu_threshold"
        private const val KEY_ALERT_RAM_ENABLED = "key_alert_ram_enabled"
        private const val KEY_ALERT_RAM_THRESHOLD = "key_alert_ram_threshold"
        private const val KEY_ALERT_TEMP_ENABLED = "key_alert_temp_enabled"
        private const val KEY_ALERT_TEMP_THRESHOLD = "key_alert_temp_threshold"
        private const val KEY_ALERT_NOTIFICATION_ENABLED = "key_alert_notification_enabled"
        private const val KEY_ALERT_VIBRATION_ENABLED = "key_alert_vibration_enabled"
        private const val KEY_ALERT_OVERLAY_HIGHLIGHT = "key_alert_overlay_highlight"
    }
}
