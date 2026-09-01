package com.example.data.collector

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.model.HardwareMetrics
import com.example.data.model.OverlayConfig
import com.example.data.model.TemperatureUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ActiveAlertState(
    val hasGpuAlert: Boolean = false,
    val hasCpuAlert: Boolean = false,
    val hasRamAlert: Boolean = false,
    val hasTempAlert: Boolean = false,
    val alertMessages: List<String> = emptyList()
) {
    val hasAnyAlert: Boolean
        get() = hasGpuAlert || hasCpuAlert || hasRamAlert || hasTempAlert
}

class HardwareAlertManager(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    private val _alertState = MutableStateFlow(ActiveAlertState())
    val alertState: StateFlow<ActiveAlertState> = _alertState.asStateFlow()

    // Cooldown trackers per metric (timestamp in ms)
    private var lastGpuAlertNotifyTime: Long = 0L
    private var lastCpuAlertNotifyTime: Long = 0L
    private var lastRamAlertNotifyTime: Long = 0L
    private var lastTempAlertNotifyTime: Long = 0L

    private val alertCooldownMs = 15_000L // 15 seconds cooldown between push notifications

    init {
        createAlertNotificationChannel()
    }

    private fun createAlertNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "硬件阈值告警提醒 (Heads-up Alerts)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "当 GPU/CPU/内存/温度 超出设定阈值时发出高优先级弹窗提醒"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 150, 300)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun evaluateMetrics(metrics: HardwareMetrics, config: OverlayConfig) {
        val now = System.currentTimeMillis()
        val messages = mutableListOf<String>()

        val isGpuExceeded = config.alertGpuEnabled && metrics.gpuUsage >= config.alertGpuThreshold
        val isCpuExceeded = config.alertCpuEnabled && metrics.cpuUsage >= config.alertCpuThreshold
        val isRamExceeded = config.alertRamEnabled && metrics.ramUsagePercent >= config.alertRamThreshold
        val isTempExceeded = config.alertTempEnabled && metrics.effectiveTemp >= config.alertTempThreshold

        if (isGpuExceeded) {
            val msg = "GPU 负载达到 ${String.format("%.1f", metrics.gpuUsage)}% (预警阈值: ${config.alertGpuThreshold.toInt()}%)"
            messages.add(msg)
            if (config.alertNotificationEnabled && (now - lastGpuAlertNotifyTime > alertCooldownMs)) {
                lastGpuAlertNotifyTime = now
                postAlertNotification(
                    notificationId = NOTIF_ID_GPU_ALERT,
                    title = "⚡ GPU 核心过载预警",
                    text = msg,
                    vibrate = config.alertVibrationEnabled
                )
            }
        }

        if (isCpuExceeded) {
            val msg = "CPU 占用达到 ${String.format("%.1f", metrics.cpuUsage)}% (预警阈值: ${config.alertCpuThreshold.toInt()}%)"
            messages.add(msg)
            if (config.alertNotificationEnabled && (now - lastCpuAlertNotifyTime > alertCooldownMs)) {
                lastCpuAlertNotifyTime = now
                postAlertNotification(
                    notificationId = NOTIF_ID_CPU_ALERT,
                    title = "⚠️ CPU 高负荷预警",
                    text = msg,
                    vibrate = config.alertVibrationEnabled
                )
            }
        }

        if (isRamExceeded) {
            val msg = "内存使用达到 ${String.format("%.1f", metrics.ramUsagePercent)}% (已用 ${metrics.ramUsedGbFormatted})"
            messages.add(msg)
            if (config.alertNotificationEnabled && (now - lastRamAlertNotifyTime > alertCooldownMs)) {
                lastRamAlertNotifyTime = now
                postAlertNotification(
                    notificationId = NOTIF_ID_RAM_ALERT,
                    title = "📦 RAM 内存高占预警",
                    text = msg,
                    vibrate = config.alertVibrationEnabled
                )
            }
        }

        if (isTempExceeded) {
            val tempStr = if (config.tempUnit == TemperatureUnit.CELSIUS) {
                "${String.format("%.1f", metrics.effectiveTemp)}°C"
            } else {
                "${String.format("%.1f", metrics.effectiveTemp * 1.8f + 32f)}°F"
            }
            val threshStr = if (config.tempUnit == TemperatureUnit.CELSIUS) {
                "${config.alertTempThreshold.toInt()}°C"
            } else {
                "${(config.alertTempThreshold * 1.8f + 32f).toInt()}°F"
            }
            val msg = "手机温度升至 $tempStr (预警阈值: $threshStr)"
            messages.add(msg)
            if (config.alertNotificationEnabled && (now - lastTempAlertNotifyTime > alertCooldownMs)) {
                lastTempAlertNotifyTime = now
                postAlertNotification(
                    notificationId = NOTIF_ID_TEMP_ALERT,
                    title = "🔥 硬件高温过热预警",
                    text = msg,
                    vibrate = config.alertVibrationEnabled
                )
            }
        }

        _alertState.value = ActiveAlertState(
            hasGpuAlert = isGpuExceeded,
            hasCpuAlert = isCpuExceeded,
            hasRamAlert = isRamExceeded,
            hasTempAlert = isTempExceeded,
            alertMessages = messages
        )
    }

    private fun postAlertNotification(notificationId: Int, title: String, text: String, vibrate: Boolean) {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setSound(soundUri)

        if (vibrate) {
            builder.setVibrate(longArrayOf(0, 300, 150, 300))
            triggerVibration()
        }

        notificationManager?.notify(notificationId, builder.build())
    }

    private fun triggerVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 250, 100, 250), -1))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(350, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(350)
                }
            }
        } catch (_: Exception) {}
    }

    fun triggerTestNotification(config: OverlayConfig) {
        postAlertNotification(
            notificationId = NOTIF_ID_TEST_ALERT,
            title = "🔔 硬件阈值告警测试",
            text = "这是一条测试通知：告警系统正常工作中！当前阈值配置有效。",
            vibrate = config.alertVibrationEnabled
        )
    }

    companion object {
        const val ALERT_CHANNEL_ID = "gpu_hardware_alerts"
        const val NOTIF_ID_GPU_ALERT = 1001
        const val NOTIF_ID_CPU_ALERT = 1002
        const val NOTIF_ID_RAM_ALERT = 1003
        const val NOTIF_ID_TEMP_ALERT = 1004
        const val NOTIF_ID_TEST_ALERT = 1099
    }
}
