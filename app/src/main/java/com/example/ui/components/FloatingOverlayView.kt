package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FloatingWindowStyle
import com.example.data.model.HardwareMetrics
import com.example.data.model.OverlayConfig
import com.example.data.model.TemperatureUnit
import com.example.ui.theme.CpuGreen
import com.example.ui.theme.FpsPurple
import com.example.ui.theme.GpuCyan
import com.example.ui.theme.ObsidianDark
import com.example.ui.theme.RamAmber
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TempCoral
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FloatingOverlayView(
    metrics: HardwareMetrics,
    config: OverlayConfig,
    modifier: Modifier = Modifier,
    onDragDelta: ((Float, Float) -> Unit)? = null,
    onToggleCollapse: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    onToggleLock: (() -> Unit)? = null,
    isInteractivePreview: Boolean = false
) {
    val alphaAnim by animateFloatAsState(
        targetValue = config.transparency.coerceIn(0.2f, 1.0f),
        label = "transparencyAlpha"
    )

    val dragModifier = if (!config.isLocked && onDragDelta != null) {
        Modifier.pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
                change.consume()
                onDragDelta(dragAmount.x, dragAmount.y)
            }
        }
    } else {
        Modifier
    }

    val isAnyAlertActive = config.alertOverlayHighlight && (
        (config.alertGpuEnabled && metrics.gpuUsage >= config.alertGpuThreshold) ||
        (config.alertCpuEnabled && metrics.cpuUsage >= config.alertCpuThreshold) ||
        (config.alertRamEnabled && metrics.ramUsagePercent >= config.alertRamThreshold) ||
        (config.alertTempEnabled && metrics.effectiveTemp >= config.alertTempThreshold)
    )

    val overlayShape = when (config.style) {
        FloatingWindowStyle.COMPACT_PILL -> RoundedCornerShape(24.dp)
        FloatingWindowStyle.MINIMAL_BADGE -> RoundedCornerShape(18.dp)
        FloatingWindowStyle.DETAILED_HUD -> RoundedCornerShape(24.dp)
        FloatingWindowStyle.FULL_CARD -> RoundedCornerShape(28.dp)
    }

    val borderBrush = if (isAnyAlertActive) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFFF3B30),
                Color(0xFFFF9500),
                Color(0xFFFF3B30)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0x80475569),
                Color(0x26334155)
            )
        )
    }

    Box(
        modifier = modifier
            .then(dragModifier)
            .alpha(alphaAnim)
            .shadow(
                elevation = if (isAnyAlertActive) 24.dp else 16.dp,
                shape = overlayShape,
                ambientColor = if (isAnyAlertActive) Color(0x99FF3B30) else Color(0x66000000),
                spotColor = if (isAnyAlertActive) Color(0xCCFF3B30) else Color(0x99000000)
            )
            .clip(overlayShape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xF01C1B1F),
                        Color(0xFA141418)
                    )
                )
            )
            .border(
                width = if (isAnyAlertActive) 1.5.dp else 1.dp,
                brush = borderBrush,
                shape = overlayShape
            )
    ) {
        if (config.isCollapsed) {
            CollapsedPillView(
                metrics = metrics,
                config = config,
                onToggleCollapse = onToggleCollapse
            )
        } else {
            when (config.style) {
                FloatingWindowStyle.COMPACT_PILL -> {
                    CompactPillContent(
                        metrics = metrics,
                        config = config,
                        onToggleCollapse = onToggleCollapse,
                        onClose = onClose,
                        onToggleLock = onToggleLock,
                        isInteractivePreview = isInteractivePreview
                    )
                }
                FloatingWindowStyle.MINIMAL_BADGE -> {
                    MinimalBadgeContent(
                        metrics = metrics,
                        config = config,
                        onToggleCollapse = onToggleCollapse,
                        onClose = onClose
                    )
                }
                FloatingWindowStyle.DETAILED_HUD -> {
                    DetailedHudContent(
                        metrics = metrics,
                        config = config,
                        onToggleCollapse = onToggleCollapse,
                        onClose = onClose,
                        onToggleLock = onToggleLock,
                        isInteractivePreview = isInteractivePreview
                    )
                }
                FloatingWindowStyle.FULL_CARD -> {
                    FullCardContent(
                        metrics = metrics,
                        config = config,
                        onToggleCollapse = onToggleCollapse,
                        onClose = onClose,
                        onToggleLock = onToggleLock,
                        isInteractivePreview = isInteractivePreview
                    )
                }
            }
        }
    }
}

@Composable
private fun CollapsedPillView(
    metrics: HardwareMetrics,
    config: OverlayConfig,
    onToggleCollapse: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .clickable { onToggleCollapse?.invoke() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(GpuCyan)
        )
        Text(
            text = "GPU ${metrics.gpuUsage.toInt()}%",
            color = GpuCyan,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "CPU ${metrics.cpuUsage.toInt()}%",
            color = CpuGreen,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = "Expand",
            tint = TextSecondary,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun CompactPillContent(
    metrics: HardwareMetrics,
    config: OverlayConfig,
    onToggleCollapse: (() -> Unit)?,
    onClose: (() -> Unit)?,
    onToggleLock: (() -> Unit)?,
    isInteractivePreview: Boolean
) {
    Row(
        modifier = Modifier
            .wrapContentHeight()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Drag icon / indicator
        Icon(
            imageVector = Icons.Default.DragHandle,
            contentDescription = "Drag Handle",
            tint = TextSecondary.copy(alpha = 0.7f),
            modifier = Modifier.size(16.dp)
        )

        if (config.showGpu) {
            PillMetricItem(
                label = "GPU",
                value = "${metrics.gpuUsage.toInt()}%",
                color = GpuCyan
            )
        }

        if (config.showCpu) {
            PillMetricItem(
                label = "CPU",
                value = "${metrics.cpuUsage.toInt()}%",
                color = CpuGreen
            )
        }

        if (config.showRam) {
            PillMetricItem(
                label = "RAM",
                value = "${metrics.ramUsagePercent.toInt()}%",
                color = RamAmber
            )
        }

        if (config.showTemp) {
            val tempVal = formatTemp(metrics.effectiveTemp, config.tempUnit)
            PillMetricItem(
                label = "TEMP",
                value = tempVal,
                color = TempCoral
            )
        }

        if (config.showFps) {
            PillMetricItem(
                label = "FPS",
                value = "${metrics.fps}",
                color = FpsPurple
            )
        }

        // Quick mini action buttons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            onToggleCollapse?.let {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .clickable { it() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Collapse",
                        tint = TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            if (!isInteractivePreview && onClose != null) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MinimalBadgeContent(
    metrics: HardwareMetrics,
    config: OverlayConfig,
    onToggleCollapse: (() -> Unit)?,
    onClose: (() -> Unit)?
) {
    Column(
        modifier = Modifier
            .widthIn(min = 90.dp)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "GPU MON",
                color = TextSecondary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = null,
                tint = TextSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(12.dp)
            )
        }

        if (config.showGpu) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("GPU", color = GpuCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("${metrics.gpuUsage.toInt()}%", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }

        if (config.showCpu) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("CPU", color = CpuGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("${metrics.cpuUsage.toInt()}%", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }

        if (config.showRam) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("RAM", color = RamAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("${metrics.ramUsagePercent.toInt()}%", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }

        if (config.showTemp) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("TMP", color = TempCoral, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(formatTemp(metrics.effectiveTemp, config.tempUnit), color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
private fun DetailedHudContent(
    metrics: HardwareMetrics,
    config: OverlayConfig,
    onToggleCollapse: (() -> Unit)?,
    onClose: (() -> Unit)?,
    onToggleLock: (() -> Unit)?,
    isInteractivePreview: Boolean
) {
    Column(
        modifier = Modifier
            .widthIn(min = 230.dp, max = 280.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top HUD Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x661E293B))
                .padding(horizontal = 14.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(GpuCyan)
                        .shadow(6.dp, CircleShape, spotColor = GpuCyan, ambientColor = GpuCyan)
                )
                Text(
                    text = "SYSTEM HUD PRO",
                    color = TextPrimary.copy(alpha = 0.85f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                onToggleLock?.let {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .clickable { it() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (config.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Lock",
                            tint = if (config.isLocked) TempCoral else TextTertiary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                onToggleCollapse?.let {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .clickable { it() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Collapse",
                            tint = TextTertiary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                if (!isInteractivePreview && onClose != null) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .clickable { onClose() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextTertiary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }

        // Main HUD Body
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // GPU Gauge Row
            if (config.showGpu) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "GRAPHICS GPU",
                            color = TextSecondary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${metrics.gpuUsage.toInt()}",
                                color = GpuCyan,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Light,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "%",
                                color = GpuCyan.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 2.dp, start = 1.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .width(84.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF1E293B))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = (metrics.gpuUsage / 100f).coerceIn(0f, 1f))
                                .fillMaxSize()
                                .clip(RoundedCornerShape(3.dp))
                                .background(GpuCyan)
                        )
                    }
                }
            }

            // CPU Gauge Row
            if (config.showCpu) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "PROCESSOR CPU",
                            color = TextSecondary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${metrics.cpuUsage.toInt()}",
                                color = CpuGreen,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Light,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "%",
                                color = CpuGreen.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 2.dp, start = 1.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .width(84.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF1E293B))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = (metrics.cpuUsage / 100f).coerceIn(0f, 1f))
                                .fillMaxSize()
                                .clip(RoundedCornerShape(3.dp))
                                .background(CpuGreen)
                        )
                    }
                }
            }

            // Per-core mini temperature row (when enabled)
            if (config.showCoreTemps && metrics.coreUsages.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x401E293B))
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    metrics.coreUsages.take(8).forEach { core ->
                        val tempColor = when {
                            core.temperatureCelsius >= 52f -> TempCoral
                            core.temperatureCelsius >= 43f -> RamAmber
                            else -> CpuGreen
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("C${core.coreIndex}", fontSize = 7.sp, color = TextTertiary, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${core.temperatureCelsius.toInt()}°",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = tempColor,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // 2-Column Grid for Temp & Memory (RAM)
            if (config.showTemp || config.showRam) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (config.showTemp) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x661E293B))
                                .border(0.8.dp, Color(0x1AFFFFFF), RoundedCornerShape(10.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Column {
                                Text("TEMP", color = TextTertiary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = formatTemp(metrics.effectiveTemp, config.tempUnit),
                                    color = TempCoral,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                    }

                    if (config.showRam) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x661E293B))
                                .border(0.8.dp, Color(0x1AFFFFFF), RoundedCornerShape(10.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Column {
                                Text("MEMORY", color = TextTertiary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = metrics.ramUsedGbFormatted,
                                    color = RamAmber,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            if (config.showFps) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("REFRESH FPS", color = TextTertiary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${metrics.fps} FPS",
                        color = FpsPurple,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Grab handle bar at bottom
        Box(
            modifier = Modifier
                .padding(bottom = 6.dp)
                .width(44.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0x6664748B))
        )
    }
}

@Composable
private fun FullCardContent(
    metrics: HardwareMetrics,
    config: OverlayConfig,
    onToggleCollapse: (() -> Unit)?,
    onClose: (() -> Unit)?,
    onToggleLock: (() -> Unit)?,
    isInteractivePreview: Boolean
) {
    Column(
        modifier = Modifier
            .widthIn(min = 240.dp, max = 300.dp)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "GPU/CPU TELEMETRY",
                color = GpuCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                onToggleCollapse?.let {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Collapse",
                        tint = TextSecondary,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { it() }
                    )
                }
                if (!isInteractivePreview && onClose != null) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onClose() }
                    )
                }
            }
        }

        // Mini Realtime trend chart
        if (metrics.gpuLoadHistory.isNotEmpty() && config.showGpu) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(SurfaceCard.copy(alpha = 0.5f))
                    .padding(4.dp)
            ) {
                RealtimeChart(
                    dataPoints = metrics.gpuLoadHistory,
                    lineColor = GpuCyan,
                    showGrid = false
                )
            }
        }

        // Detailed Metrics
        if (config.showGpu) {
            HudBarItem(
                label = "GPU",
                valueText = "${metrics.gpuUsage.toInt()}%",
                subText = "${metrics.gpuFreqMhz} MHz",
                fraction = metrics.gpuUsage / 100f,
                accentColor = GpuCyan
            )
        }

        if (config.showCpu) {
            HudBarItem(
                label = "CPU",
                valueText = "${metrics.cpuUsage.toInt()}%",
                subText = "${metrics.cpuCoreCount} Cores",
                fraction = metrics.cpuUsage / 100f,
                accentColor = CpuGreen
            )
        }

        if (config.showRam) {
            HudBarItem(
                label = "RAM",
                valueText = "${metrics.ramUsagePercent.toInt()}%",
                subText = "${metrics.ramUsedGbFormatted}/${metrics.ramTotalGbFormatted}",
                fraction = metrics.ramUsagePercent / 100f,
                accentColor = RamAmber
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (config.showTemp) {
                Text(
                    text = "TEMP: ${formatTemp(metrics.effectiveTemp, config.tempUnit)}",
                    color = TempCoral,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            if (config.showFps) {
                Text(
                    text = "FPS: ${metrics.fps}",
                    color = FpsPurple,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun PillMetricItem(
    label: String,
    value: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun HudBarItem(
    label: String,
    valueText: String,
    subText: String,
    fraction: Float,
    accentColor: Color
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = label,
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subText,
                    color = TextSecondary.copy(alpha = 0.6f),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Text(
                text = valueText,
                color = accentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        LinearProgressIndicator(
            progress = { fraction.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = accentColor,
            trackColor = SurfaceCard
        )
    }
}

fun formatTemp(tempC: Float, unit: TemperatureUnit): String {
    return when (unit) {
        TemperatureUnit.CELSIUS -> String.format("%.1f°C", tempC)
        TemperatureUnit.FAHRENHEIT -> String.format("%.1f°F", (tempC * 9f / 5f) + 32f)
    }
}
