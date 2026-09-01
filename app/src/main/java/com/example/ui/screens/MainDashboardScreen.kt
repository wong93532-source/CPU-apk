package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.FloatingWindowStyle
import com.example.data.model.HardwareMetrics
import com.example.data.model.HistoryMetricType
import com.example.data.model.HistoryTimeWindow
import com.example.data.model.OverlayConfig
import com.example.data.model.TemperatureUnit
import com.example.ui.components.AlertConfigCard
import com.example.ui.components.CoreUsageGrid
import com.example.ui.components.FloatingOverlayView
import com.example.ui.components.HistoricalPerformanceCard
import com.example.ui.components.LargeGaugeCard
import com.example.ui.components.RealtimeChart
import com.example.ui.components.formatTemp
import com.example.ui.theme.CpuGreen
import com.example.ui.theme.CyberBluePrimary
import com.example.ui.theme.CyberBlueSecondary
import com.example.ui.theme.FpsPurple
import com.example.ui.theme.GpuCyan
import com.example.ui.theme.ObsidianDark
import com.example.ui.theme.RamAmber
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardBorder
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TempCoral
import com.example.ui.theme.TempRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.viewmodel.MonitorViewModel
import com.example.viewmodel.PositionPreset
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainDashboardScreen(
    metrics: HardwareMetrics,
    config: OverlayConfig,
    hasOverlayPermission: Boolean,
    viewModel: MonitorViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val selectedTimeWindow by viewModel.selectedTimeWindow.collectAsStateWithLifecycle()
    val selectedMetricType by viewModel.selectedMetricType.collectAsStateWithLifecycle()
    val activeAlertState by viewModel.activeAlertState.collectAsStateWithLifecycle()
    val timeframeSeries = viewModel.getTimeframeDataSeries(selectedTimeWindow)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF161828),
                        ObsidianDark,
                        Color(0xFF0A0A0D)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Header
            DashboardHeader(
                isOverlayEnabled = config.isOverlayEnabled,
                onToggleService = { viewModel.toggleOverlayService(context) }
            )

            // Permission Alert if needed
            if (!hasOverlayPermission) {
                PermissionBanner(
                    onRequestPermission = { viewModel.requestOverlayPermission(context) }
                )
            }

            // Master Floating Window Service Control Banner
            FloatingControlBanner(
                isOverlayEnabled = config.isOverlayEnabled,
                hasPermission = hasOverlayPermission,
                onToggle = { viewModel.toggleOverlayService(context) },
                onRequestPermission = { viewModel.requestOverlayPermission(context) }
            )

            // Interactive In-App Floating Window Drag & Customization Sandbox
            FloatingSandboxSection(
                metrics = metrics,
                config = config,
                viewModel = viewModel
            )

            // Customizable Alert Thresholds & Notification Configuration Card
            AlertConfigCard(
                config = config,
                activeAlertState = activeAlertState,
                onUpdateGpuThreshold = { viewModel.setGpuThreshold(it) },
                onToggleGpuAlert = { viewModel.toggleGpuAlert(it) },
                onUpdateCpuThreshold = { viewModel.setCpuThreshold(it) },
                onToggleCpuAlert = { viewModel.toggleCpuAlert(it) },
                onUpdateRamThreshold = { viewModel.setRamThreshold(it) },
                onToggleRamAlert = { viewModel.toggleRamAlert(it) },
                onUpdateTempThreshold = { viewModel.setTempThreshold(it) },
                onToggleTempAlert = { viewModel.toggleTempAlert(it) },
                onToggleNotification = { viewModel.toggleAlertNotification(it) },
                onToggleVibration = { viewModel.toggleAlertVibration(it) },
                onToggleOverlayHighlight = { viewModel.toggleAlertOverlayHighlight(it) },
                onTestAlert = { viewModel.triggerTestAlert() }
            )

            // Multi-Timeframe Historical Performance Curve Chart Card (1m, 5m, 15m)
            HistoricalPerformanceCard(
                timeframeSeries = timeframeSeries,
                selectedTimeWindow = selectedTimeWindow,
                selectedMetric = selectedMetricType,
                tempUnit = config.tempUnit,
                onTimeWindowSelected = { viewModel.setTimeWindow(it) },
                onMetricSelected = { viewModel.setMetricType(it) }
            )

            // Realtime Hardware Telemetry Gauges (GPU & CPU)
            Text(
                text = "实时核心监控遥测 (HARDWARE TELEMETRY)",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LargeGaugeCard(
                    title = "GPU 占用率",
                    valuePercent = metrics.gpuUsage,
                    subValue = "${metrics.gpuFreqMhz} MHz | ${formatTemp(metrics.effectiveGpuTemp, config.tempUnit)}",
                    accentColor = GpuCyan,
                    modifier = Modifier.weight(1f),
                    historyData = metrics.gpuLoadHistory
                )

                LargeGaugeCard(
                    title = "CPU 占用率",
                    valuePercent = metrics.cpuUsage,
                    subValue = "${metrics.cpuFreqMhz} MHz | ${formatTemp(metrics.effectiveCpuTemp, config.tempUnit)}",
                    accentColor = CpuGreen,
                    modifier = Modifier.weight(1f),
                    historyData = metrics.cpuLoadHistory
                )
            }

            // RAM & Temperature Dual Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // RAM Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(
                                    SurfaceDark,
                                    Color(0xFF141418)
                                )
                            )
                        )
                        .border(
                            1.dp,
                            Brush.verticalGradient(
                                listOf(
                                    RamAmber.copy(alpha = 0.5f),
                                    Color(0x26334155)
                                )
                            ),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("内存使用 (RAM)", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${metrics.ramUsagePercent.toInt()}%",
                                color = RamAmber,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Text(
                            text = "${metrics.ramUsedGbFormatted} / ${metrics.ramTotalGbFormatted}",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x401E293B))
                                .padding(2.dp)
                        ) {
                            RealtimeChart(
                                dataPoints = metrics.ramLoadHistory,
                                lineColor = RamAmber,
                                minValue = 0f,
                                maxValue = 100f,
                                showGrid = false
                            )
                        }
                    }
                }

                // Temperature Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(
                                    SurfaceDark,
                                    Color(0xFF141418)
                                )
                            )
                        )
                        .border(
                            1.dp,
                            Brush.verticalGradient(
                                listOf(
                                    TempCoral.copy(alpha = 0.5f),
                                    Color(0x26334155)
                                )
                            ),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("手机温度 (TEMP)", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = formatTemp(metrics.effectiveTemp, config.tempUnit),
                                color = TempCoral,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.clickable { viewModel.toggleTempUnit() }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val status = when {
                                metrics.effectiveTemp < 38f -> "温控良好"
                                metrics.effectiveTemp < 45f -> "轻微发热"
                                else -> "高温负载"
                            }
                            Text(
                                text = status,
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                            Text(
                                text = "电量 ${metrics.batteryLevel}%",
                                color = TextTertiary,
                                fontSize = 10.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x401E293B))
                                .padding(2.dp)
                        ) {
                            RealtimeChart(
                                dataPoints = metrics.tempHistory,
                                lineColor = TempCoral,
                                minValue = 20f,
                                maxValue = 60f,
                                showGrid = false
                            )
                        }
                    }
                }
            }

            // Multi-Core CPU Visualizer with Per-Core Temperatures
            if (metrics.coreUsages.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfaceDark)
                        .border(1.dp, Color(0x33475569), RoundedCornerShape(20.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CPU 各核心温度与负载 (${metrics.cpuCoreCount} 核心)",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "FPS: ${metrics.fps}",
                                color = FpsPurple,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        CoreUsageGrid(
                            cores = metrics.coreUsages,
                            tempUnit = config.tempUnit,
                            showTemp = config.showCoreTemps
                        )
                    }
                }
            }

            // Hardware System Specs Card
            HardwareSpecsCard(metrics = metrics)

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DashboardHeader(
    isOverlayEnabled: Boolean,
    onToggleService: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "PERF MONITOR",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isOverlayEnabled) GpuCyan.copy(alpha = 0.2f) else SurfaceCard)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isOverlayEnabled) "浮窗运行中" else "未开启",
                        color = if (isOverlayEnabled) GpuCyan else TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                text = "实时GPU / CPU占用 · 核心温度 · 历史趋势 · 阈值告警",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun PermissionBanner(
    onRequestPermission: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .border(1.dp, RamAmber.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(RamAmber.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Permission",
                    tint = RamAmber,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "需要开启“悬浮窗权限”",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "开启后即可在任意游戏或应用上方常驻显示GPU/CPU占用率",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = RamAmber,
                    contentColor = ObsidianDark
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("去授权", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FloatingControlBanner(
    isOverlayEnabled: Boolean,
    hasPermission: Boolean,
    onToggle: () -> Unit,
    onRequestPermission: () -> Unit
) {
    val animatedBg by animateColorAsState(
        targetValue = if (isOverlayEnabled) Color(0x331E293B) else SurfaceDark,
        label = "bannerBg"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(animatedBg)
            .border(
                1.dp,
                if (isOverlayEnabled) Brush.linearGradient(listOf(GpuCyan.copy(alpha = 0.6f), CyberBluePrimary.copy(alpha = 0.6f)))
                else Brush.linearGradient(listOf(Color(0x33475569), Color(0x1A334155))),
                RoundedCornerShape(22.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isOverlayEnabled) Brush.linearGradient(listOf(GpuCyan, CyberBluePrimary))
                            else Brush.linearGradient(listOf(Color(0xFF2D3748), Color(0xFF1E293B)))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = if (isOverlayEnabled) ObsidianDark else TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (isOverlayEnabled) "全局悬浮窗监控已启动" else "启动全局屏幕悬浮窗",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isOverlayEnabled) "支持拖拽位置，可跨任意应用显示" else "点击开关即刻在屏幕上方显示实时硬件参数",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Switch(
                checked = isOverlayEnabled,
                onCheckedChange = {
                    if (!hasPermission && it) {
                        onRequestPermission()
                    } else {
                        onToggle()
                    }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ObsidianDark,
                    checkedTrackColor = GpuCyan,
                    uncheckedThumbColor = TextSecondary,
                    uncheckedTrackColor = Color(0xFF24252C)
                )
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FloatingSandboxSection(
    metrics: HardwareMetrics,
    config: OverlayConfig,
    viewModel: MonitorViewModel
) {
    val density = LocalDensity.current

    // Local in-sandbox drag offset
    var previewOffsetX by remember { mutableFloatStateOf(30f) }
    var previewOffsetY by remember { mutableFloatStateOf(20f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(SurfaceDark)
            .border(1.dp, Color(0x33475569), RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = CpuGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "浮窗实时预览与自定义调优",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "自由拖拽测试",
                    color = TextTertiary,
                    fontSize = 11.sp
                )
            }

            // Interactive Drag Area Sandbox
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0D0E12))
                    .border(0.8.dp, Color(0x26475569), RoundedCornerShape(16.dp))
            ) {
                Text(
                    text = "在此区域内可自由拖拽测试浮窗位置",
                    color = TextTertiary.copy(alpha = 0.35f),
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.Center)
                )

                // Draggable Preview Overlay
                FloatingOverlayView(
                    metrics = metrics,
                    config = config,
                    modifier = Modifier
                        .offset { IntOffset(previewOffsetX.roundToInt(), previewOffsetY.roundToInt()) }
                        .pointerInput(config.isLocked) {
                            if (!config.isLocked) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    previewOffsetX = (previewOffsetX + dragAmount.x).coerceIn(0f, 150f)
                                    previewOffsetY = (previewOffsetY + dragAmount.y).coerceIn(0f, 80f)
                                    viewModel.setPosition(previewOffsetX.toInt(), previewOffsetY.toInt())
                                }
                            }
                        },
                    onToggleCollapse = { viewModel.toggleCollapsed() },
                    onToggleLock = { viewModel.toggleLock() },
                    isInteractivePreview = true
                )
            }

            // Customization 1: Transparency Slider
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = GpuCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "浮窗透明度 (Opacity)",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(
                        text = "${(config.transparency * 100).toInt()}%",
                        color = GpuCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Slider(
                    value = config.transparency,
                    onValueChange = { viewModel.setTransparency(it) },
                    valueRange = 0.20f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = GpuCyan,
                        activeTrackColor = GpuCyan,
                        inactiveTrackColor = Color(0xFF24252C)
                    )
                )
            }

            // Customization 2: Position Presets
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "快速定位预设 (Position Presets)",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PositionPreset.values().forEach { preset ->
                        OutlinedButton(
                            onClick = {
                                viewModel.applyPositionPreset(preset, 800, 1600)
                                previewOffsetX = when (preset) {
                                    PositionPreset.TOP_CENTER -> 50f
                                    PositionPreset.TOP_LEFT -> 10f
                                    PositionPreset.TOP_RIGHT -> 120f
                                    PositionPreset.BOTTOM_CENTER -> 50f
                                    PositionPreset.MIDDLE_RIGHT -> 120f
                                }
                                previewOffsetY = when (preset) {
                                    PositionPreset.TOP_CENTER, PositionPreset.TOP_LEFT, PositionPreset.TOP_RIGHT -> 15f
                                    PositionPreset.BOTTOM_CENTER, PositionPreset.MIDDLE_RIGHT -> 70f
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = TextPrimary
                            ),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0x33475569))
                        ) {
                            Text(preset.label, fontSize = 11.sp)
                        }
                    }
                }
            }

            // Customization 3: Floating Window Styles
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "浮窗形态风格 (Window Style)",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FloatingWindowStyle.values().forEach { style ->
                        val isSelected = config.style == style
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setStyle(style) },
                            label = { Text(style.label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0x4D2563EB),
                                selectedLabelColor = TextPrimary,
                                containerColor = Color(0xFF1E293B),
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = Color(0x33475569),
                                selectedBorderColor = CpuGreen
                            )
                        )
                    }
                }
            }

            // Customization 4: Metrics Toggles
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "显示指标开关 (Metrics Selection)",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MetricToggleChip("GPU 占用率", config.showGpu, GpuCyan) { viewModel.toggleMetric(gpu = !config.showGpu) }
                    MetricToggleChip("CPU 占用率", config.showCpu, CpuGreen) { viewModel.toggleMetric(cpu = !config.showCpu) }
                    MetricToggleChip("内存使用", config.showRam, RamAmber) { viewModel.toggleMetric(ram = !config.showRam) }
                    MetricToggleChip("手机温度", config.showTemp, TempCoral) { viewModel.toggleMetric(temp = !config.showTemp) }
                    MetricToggleChip("每核温度", config.showCoreTemps, TempRed) { viewModel.toggleCoreTemps() }
                    MetricToggleChip("历史曲线", config.showHistoryCharts, GpuCyan) { viewModel.toggleHistoryCharts() }
                    MetricToggleChip("FPS 帧率", config.showFps, FpsPurple) { viewModel.toggleMetric(fps = !config.showFps) }
                }
            }

            // Customization 5: Update Refresh Interval
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "采样刷新率",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(500L to "0.5s 高频", 1000L to "1.0s 标准", 2000L to "2.0s 省电").forEach { (ms, text) ->
                        val isSelected = config.updateIntervalMs == ms
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0x4D2563EB) else Color(0xFF1E293B))
                                .border(0.8.dp, if (isSelected) CpuGreen else Color(0x26475569), RoundedCornerShape(8.dp))
                            .clickable { viewModel.setUpdateInterval(ms) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = text,
                                color = if (isSelected) TextPrimary else TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricToggleChip(
    label: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(label, fontSize = 11.sp) },
        leadingIcon = if (isSelected) {
            {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
            }
        } else null,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = accentColor.copy(alpha = 0.2f),
            selectedLabelColor = TextPrimary,
            containerColor = Color(0xFF1E293B),
            labelColor = TextSecondary
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = isSelected,
            borderColor = Color(0x33475569),
            selectedBorderColor = accentColor
        )
    )
}

@Composable
private fun HardwareSpecsCard(metrics: HardwareMetrics) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceDark)
            .border(1.dp, Color(0x33475569), RoundedCornerShape(20.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "设备硬件规格 (SYSTEM HARDWARE)",
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            SpecRow("GPU 渲染器", metrics.gpuModel)
            SpecRow("CPU 处理器", "${android.os.Build.HARDWARE} (${metrics.cpuCoreCount} 核)")
            SpecRow("物理内存总计", metrics.ramTotalGbFormatted)
            SpecRow("Android 版本", "Android ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
            SpecRow("设备型号", "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
        }
    }
}

@Composable
private fun SpecRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextSecondary, fontSize = 11.sp)
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium
        )
    }
}

