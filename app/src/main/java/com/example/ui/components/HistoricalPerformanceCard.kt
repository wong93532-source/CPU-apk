package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HistoryMetricType
import com.example.data.model.HistoryTimeWindow
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
import com.example.viewmodel.TimeframeDataSeries

@Composable
fun HistoricalPerformanceCard(
    timeframeSeries: TimeframeDataSeries,
    selectedTimeWindow: HistoryTimeWindow,
    selectedMetric: HistoryMetricType,
    tempUnit: TemperatureUnit,
    onTimeWindowSelected: (HistoryTimeWindow) -> Unit,
    onMetricSelected: (HistoryMetricType) -> Unit,
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
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header Row with Time Window Chips
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
                            .background(GpuCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timeline,
                            contentDescription = "历史曲线",
                            tint = GpuCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "多时段历史性能趋势",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "长周期硬件负载与温度分析",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                // Time window pill selector (1m, 5m, 15m)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.35f))
                        .border(0.5.dp, SurfaceCardBorder, RoundedCornerShape(20.dp))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    HistoryTimeWindow.values().forEach { window ->
                        val isSelected = window == selectedTimeWindow
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    if (isSelected) GpuCyan.copy(alpha = 0.25f) else Color.Transparent
                                )
                                .clickable { onTimeWindowSelected(window) }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = window.label,
                                color = if (isSelected) GpuCyan else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Metric Type Horizontal Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HistoryMetricType.values().forEach { type ->
                    val isSelected = type == selectedMetric
                    val typeColor = when (type) {
                        HistoryMetricType.ALL -> Color(0xFFE0E0E0)
                        HistoryMetricType.GPU -> GpuCyan
                        HistoryMetricType.CPU -> CpuGreen
                        HistoryMetricType.RAM -> RamPurple
                        HistoryMetricType.TEMP -> TempRed
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) typeColor.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.03f)
                            )
                            .border(
                                width = if (isSelected) 1.dp else 0.5.dp,
                                color = if (isSelected) typeColor.copy(alpha = 0.7f) else SurfaceCardBorder,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onMetricSelected(type) }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = type.label,
                            color = if (isSelected) typeColor else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            // Summary Stats Row for Selected Window
            StatsSummaryBanner(
                series = timeframeSeries,
                selectedMetric = selectedMetric,
                tempUnit = tempUnit
            )

            // Multi-Timeframe Chart Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.25f))
                    .border(0.5.dp, SurfaceCardBorder.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 10.dp)
            ) {
                MultiSeriesCanvasChart(
                    series = timeframeSeries,
                    selectedMetric = selectedMetric
                )
            }

            // Time Axis Labels below chart
            TimeAxisLabels(window = selectedTimeWindow)
        }
    }
}

@Composable
private fun StatsSummaryBanner(
    series: TimeframeDataSeries,
    selectedMetric: HistoryMetricType,
    tempUnit: TemperatureUnit
) {
    val (minStr, avgStr, maxStr, unitLabel, themeColor) = when (selectedMetric) {
        HistoryMetricType.GPU -> {
            val min = "${series.minGpu.toInt()}%"
            val avg = "${String.format("%.1f", series.avgGpu)}%"
            val max = "${series.maxGpu.toInt()}%"
            Tuple5(min, avg, max, "GPU 负载", GpuCyan)
        }
        HistoryMetricType.CPU -> {
            val min = "${series.minCpu.toInt()}%"
            val avg = "${String.format("%.1f", series.avgCpu)}%"
            val max = "${series.maxCpu.toInt()}%"
            Tuple5(min, avg, max, "CPU 占用", CpuGreen)
        }
        HistoryMetricType.RAM -> {
            val min = "${series.minRam.toInt()}%"
            val avg = "${String.format("%.1f", series.avgRam)}%"
            val max = "${series.maxRam.toInt()}%"
            Tuple5(min, avg, max, "内存占用", RamPurple)
        }
        HistoryMetricType.TEMP -> {
            val unit = tempUnit.symbol
            val formatTemp = { c: Float ->
                if (tempUnit == TemperatureUnit.CELSIUS) String.format("%.1f%s", c, unit)
                else String.format("%.1f%s", c * 1.8f + 32f, unit)
            }
            Tuple5(formatTemp(series.minTemp), formatTemp(series.avgTemp), formatTemp(series.maxTemp), "温度区间", TempRed)
        }
        HistoryMetricType.ALL -> {
            val min = "GPU ${series.avgGpu.toInt()}%"
            val avg = "CPU ${series.avgCpu.toInt()}%"
            val max = "RAM ${series.avgRam.toInt()}%"
            Tuple5(min, avg, max, "多项均值", GpuCyan)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(themeColor.copy(alpha = 0.08f))
            .border(0.5.dp, themeColor.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatItem(label = if (selectedMetric == HistoryMetricType.ALL) "GPU均值" else "最低 (Min)", value = minStr, color = themeColor)
        StatItem(label = if (selectedMetric == HistoryMetricType.ALL) "CPU均值" else "平均 (Avg)", value = avgStr, color = TextPrimary)
        StatItem(label = if (selectedMetric == HistoryMetricType.ALL) "RAM均值" else "最高 (Max)", value = maxStr, color = themeColor)
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = label, color = TextSecondary, fontSize = 10.sp)
        Text(
            text = value,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

private data class Tuple5<A, B, C, D, E>(
    val a: A, val b: B, val c: C, val d: D, val e: E
)

@Composable
private fun MultiSeriesCanvasChart(
    series: TimeframeDataSeries,
    selectedMetric: HistoryMetricType,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        if (width <= 0 || height <= 0 || series.samples.isEmpty()) return@Canvas

        // Draw horizontal subtle reference lines (25%, 50%, 75%)
        val gridColor = Color.White.copy(alpha = 0.06f)
        for (i in 1..3) {
            val y = height * (i / 4f)
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )
        }

        when (selectedMetric) {
            HistoryMetricType.ALL -> {
                // Draw GPU (Cyan), CPU (Green), RAM (Purple), Temp (Red normalized 20..60C)
                drawSingleMetricPath(
                    data = series.samples.map { it.gpuUsage },
                    color = GpuCyan,
                    fill = false,
                    strokeWidth = 2.5f,
                    minValue = 0f,
                    maxValue = 100f
                )
                drawSingleMetricPath(
                    data = series.samples.map { it.cpuUsage },
                    color = CpuGreen,
                    fill = false,
                    strokeWidth = 2.5f,
                    minValue = 0f,
                    maxValue = 100f
                )
                drawSingleMetricPath(
                    data = series.samples.map { it.ramUsagePercent },
                    color = RamPurple,
                    fill = false,
                    strokeWidth = 2.0f,
                    minValue = 0f,
                    maxValue = 100f
                )
            }
            HistoryMetricType.GPU -> {
                drawSingleMetricPath(
                    data = series.samples.map { it.gpuUsage },
                    color = GpuCyan,
                    fill = true,
                    strokeWidth = 3.5f,
                    minValue = 0f,
                    maxValue = 100f
                )
            }
            HistoryMetricType.CPU -> {
                drawSingleMetricPath(
                    data = series.samples.map { it.cpuUsage },
                    color = CpuGreen,
                    fill = true,
                    strokeWidth = 3.5f,
                    minValue = 0f,
                    maxValue = 100f
                )
            }
            HistoryMetricType.RAM -> {
                drawSingleMetricPath(
                    data = series.samples.map { it.ramUsagePercent },
                    color = RamPurple,
                    fill = true,
                    strokeWidth = 3.5f,
                    minValue = 0f,
                    maxValue = 100f
                )
            }
            HistoryMetricType.TEMP -> {
                // Temperature scale from 25°C to 65°C for high dynamic visual curve
                drawSingleMetricPath(
                    data = series.samples.map { it.temperature },
                    color = TempRed,
                    fill = true,
                    strokeWidth = 3.5f,
                    minValue = 25f,
                    maxValue = 65f
                )
            }
        }
    }
}

private fun DrawScope.drawSingleMetricPath(
    data: List<Float>,
    color: Color,
    fill: Boolean,
    strokeWidth: Float,
    minValue: Float,
    maxValue: Float
) {
    if (data.isEmpty()) return
    val width = size.width
    val height = size.height
    val range = (maxValue - minValue).coerceAtLeast(1f)
    val stepX = if (data.size > 1) width / (data.size - 1) else width

    val path = Path()
    val fillPath = Path()

    data.forEachIndexed { index, value ->
        val clamped = value.coerceIn(minValue, maxValue)
        val normY = (clamped - minValue) / range
        val x = index * stepX
        val y = height - (normY * height)

        if (index == 0) {
            path.moveTo(x, y)
            fillPath.moveTo(x, height)
            fillPath.lineTo(x, y)
        } else {
            val prevVal = data[index - 1].coerceIn(minValue, maxValue)
            val prevNormY = (prevVal - minValue) / range
            val prevX = (index - 1) * stepX
            val prevY = height - (prevNormY * height)

            val controlX1 = prevX + (x - prevX) / 2f
            val controlY1 = prevY
            val controlX2 = prevX + (x - prevX) / 2f
            val controlY2 = y

            path.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
            fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
        }
    }

    if (fill && data.size > 1) {
        val lastX = (data.size - 1) * stepX
        fillPath.lineTo(lastX, height)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    color.copy(alpha = 0.35f),
                    color.copy(alpha = 0.02f)
                ),
                startY = 0f,
                endY = height
            )
        )
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )

    // Draw active beacon on last data point
    if (data.isNotEmpty()) {
        val lastClamped = data.last().coerceIn(minValue, maxValue)
        val lastNormY = (lastClamped - minValue) / range
        val lastX = if (data.size > 1) (data.size - 1) * stepX else 0f
        val lastY = height - (lastNormY * height)

        drawCircle(
            color = color.copy(alpha = 0.4f),
            radius = 6f,
            center = Offset(lastX, lastY)
        )
        drawCircle(
            color = Color.White,
            radius = 3f,
            center = Offset(lastX, lastY)
        )
    }
}

@Composable
private fun TimeAxisLabels(window: HistoryTimeWindow) {
    val labels = when (window) {
        HistoryTimeWindow.ONE_MINUTE -> listOf("-60s", "-45s", "-30s", "-15s", "当前 (Now)")
        HistoryTimeWindow.FIVE_MINUTES -> listOf("-5m", "-3.5m", "-2.5m", "-1m", "当前 (Now)")
        HistoryTimeWindow.FIFTEEN_MINUTES -> listOf("-15m", "-10m", "-5m", "-2m", "当前 (Now)")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        labels.forEach { label ->
            Text(
                text = label,
                color = TextSecondary.copy(alpha = 0.7f),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
