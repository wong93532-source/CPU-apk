package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CoreMetric
import com.example.data.model.TemperatureUnit
import com.example.ui.theme.CpuGreen
import com.example.ui.theme.GpuCyan
import com.example.ui.theme.GpuOrange
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardBorder
import com.example.ui.theme.TempRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CoreUsageGrid(
    cores: List<CoreMetric>,
    tempUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    showTemp: Boolean = true,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        maxItemsInEachRow = 4
    ) {
        cores.forEach { core ->
            val tempColor = when {
                core.temperatureCelsius >= 52f -> TempRed
                core.temperatureCelsius >= 43f -> GpuOrange
                else -> CpuGreen
            }

            val tempFormatted = if (tempUnit == TemperatureUnit.CELSIUS) {
                "${String.format("%.1f", core.temperatureCelsius)}°C"
            } else {
                "${String.format("%.1f", core.temperatureCelsius * 1.8f + 32f)}°F"
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceCard)
                    .border(0.5.dp, SurfaceCardBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 6.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "C${core.coreIndex}",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${core.usagePercent.toInt()}%",
                            color = CpuGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    LinearProgressIndicator(
                        progress = { (core.usagePercent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp)),
                        color = CpuGreen,
                        trackColor = SurfaceCardBorder
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${core.curFreqMhz}M",
                            color = TextSecondary.copy(alpha = 0.7f),
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        if (showTemp && core.temperatureCelsius > 0f) {
                            Text(
                                text = tempFormatted,
                                color = tempColor,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

