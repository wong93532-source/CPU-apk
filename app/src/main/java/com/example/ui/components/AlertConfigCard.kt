package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.collector.ActiveAlertState
import com.example.data.model.OverlayConfig
import com.example.data.model.TemperatureUnit
import com.example.ui.theme.CpuGreen
import com.example.ui.theme.GpuCyan
import com.example.ui.theme.GpuOrange
import com.example.ui.theme.RamPurple
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardBorder
import com.example.ui.theme.TempRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AlertConfigCard(
    config: OverlayConfig,
    activeAlertState: ActiveAlertState,
    onUpdateGpuThreshold: (Float) -> Unit,
    onToggleGpuAlert: (Boolean) -> Unit,
    onUpdateCpuThreshold: (Float) -> Unit,
    onToggleCpuAlert: (Boolean) -> Unit,
    onUpdateRamThreshold: (Float) -> Unit,
    onToggleRamAlert: (Boolean) -> Unit,
    onUpdateTempThreshold: (Float) -> Unit,
    onToggleTempAlert: (Boolean) -> Unit,
    onToggleNotification: (Boolean) -> Unit,
    onToggleVibration: (Boolean) -> Unit,
    onToggleOverlayHighlight: (Boolean) -> Unit,
    onTestAlert: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceCard)
            .border(1.dp, SurfaceCardBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(TempRed.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "告警设置",
                            tint = TempRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "硬件负载与温度告警",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "超阈值自动弹窗通知与触觉提醒",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                // Test Alert Button
                OutlinedButton(
                    onClick = onTestAlert,
                    modifier = Modifier
                        .testTag("test_alert_button")
                        .height(32.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = GpuCyan
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GpuCyan.copy(alpha = 0.5f)),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = "测试告警",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Live Alert Trigger Banner (when thresholds are exceeded)
            AnimatedVisibility(
                visible = activeAlertState.hasAnyAlert,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(TempRed.copy(alpha = 0.18f))
                        .border(1.dp, TempRed.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "警告中",
                            tint = TempRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "当前硬件触发预警阈值！",
                                color = TempRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            activeAlertState.alertMessages.forEach { msg ->
                                Text(
                                    text = "• $msg",
                                    color = TextPrimary.copy(alpha = 0.9f),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            // Notification Delivery Options (Switches)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.25f))
                    .border(0.5.dp, SurfaceCardBorder, RoundedCornerShape(10.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // System Notification
                DeliveryToggleItem(
                    label = "系统通知栏弹窗",
                    sublabel = "Heads-up 顶层弹窗",
                    checked = config.alertNotificationEnabled,
                    onCheckedChange = onToggleNotification
                )

                // Vibration
                DeliveryToggleItem(
                    label = "触觉振动提醒",
                    sublabel = "马达高频震动",
                    checked = config.alertVibrationEnabled,
                    onCheckedChange = onToggleVibration
                )
            }

            // Threshold Sliders
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // GPU Threshold
                ThresholdSliderItem(
                    name = "GPU 使用率阈值",
                    value = config.alertGpuThreshold,
                    unit = "%",
                    color = GpuCyan,
                    enabled = config.alertGpuEnabled,
                    range = 50f..99f,
                    onToggle = onToggleGpuAlert,
                    onValueChange = onUpdateGpuThreshold
                )

                // CPU Threshold
                ThresholdSliderItem(
                    name = "CPU 占用率阈值",
                    value = config.alertCpuThreshold,
                    unit = "%",
                    color = CpuGreen,
                    enabled = config.alertCpuEnabled,
                    range = 50f..99f,
                    onToggle = onToggleCpuAlert,
                    onValueChange = onUpdateCpuThreshold
                )

                // RAM Threshold
                ThresholdSliderItem(
                    name = "内存 (RAM) 阈值",
                    value = config.alertRamThreshold,
                    unit = "%",
                    color = RamPurple,
                    enabled = config.alertRamEnabled,
                    range = 50f..99f,
                    onToggle = onToggleRamAlert,
                    onValueChange = onUpdateRamThreshold
                )

                // Temperature Threshold
                val isCelsius = config.tempUnit == TemperatureUnit.CELSIUS
                val displayTemp = if (isCelsius) config.alertTempThreshold else config.alertTempThreshold * 1.8f + 32f
                val unitSymbol = config.tempUnit.symbol
                val range = if (isCelsius) 35f..60f else 95f..140f

                ThresholdSliderItem(
                    name = "手机核心温度阈值",
                    value = displayTemp,
                    unit = unitSymbol,
                    color = TempRed,
                    enabled = config.alertTempEnabled,
                    range = range,
                    onToggle = onToggleTempAlert,
                    onValueChange = { newVal ->
                        val celsiusVal = if (isCelsius) newVal else (newVal - 32f) / 1.8f
                        onUpdateTempThreshold(celsiusVal)
                    }
                )
            }
        }
    }
}

@Composable
private fun DeliveryToggleItem(
    label: String,
    sublabel: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column {
            Text(text = label, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(text = sublabel, color = TextSecondary, fontSize = 9.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag("toggle_${label}"),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = GpuCyan,
                uncheckedTrackColor = SurfaceCardBorder
            )
        )
    }
}

@Composable
private fun ThresholdSliderItem(
    name: String,
    value: Float,
    unit: String,
    color: Color,
    enabled: Boolean,
    range: ClosedFloatingPointRange<Float>,
    onToggle: (Boolean) -> Unit,
    onValueChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (enabled) color else Color.Gray)
                )
                Text(
                    text = name,
                    color = if (enabled) TextPrimary else TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${value.toInt()} $unit",
                    color = if (enabled) color else TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = color,
                        uncheckedTrackColor = SurfaceCardBorder
                    )
                )
            }
        }

        if (enabled) {
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = range,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp),
                colors = SliderDefaults.colors(
                    thumbColor = color,
                    activeTrackColor = color,
                    inactiveTrackColor = SurfaceCardBorder
                )
            )
        }
    }
}
