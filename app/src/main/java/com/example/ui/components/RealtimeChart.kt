package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun RealtimeChart(
    dataPoints: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = Color(0xFF00E5FF),
    fillGradient: Boolean = true,
    minValue: Float = 0f,
    maxValue: Float = 100f,
    showGrid: Boolean = true
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        if (width <= 0 || height <= 0 || dataPoints.isEmpty()) return@Canvas

        // Draw horizontal grid lines
        if (showGrid) {
            val gridColor = Color.White.copy(alpha = 0.08f)
            val stroke = Stroke(width = 1f)
            for (i in 1..3) {
                val y = height * (i / 4f)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = stroke.width
                )
            }
        }

        val range = (maxValue - minValue).coerceAtLeast(1f)
        val stepX = if (dataPoints.size > 1) width / (dataPoints.size - 1) else width

        val path = Path()
        val fillPath = Path()

        dataPoints.forEachIndexed { index, value ->
            val clamped = value.coerceIn(minValue, maxValue)
            val normY = (clamped - minValue) / range
            val x = index * stepX
            val y = height - (normY * height)

            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, height)
                fillPath.lineTo(x, y)
            } else {
                val prevVal = dataPoints[index - 1].coerceIn(minValue, maxValue)
                val prevNormY = (prevVal - minValue) / range
                val prevX = (index - 1) * stepX
                val prevY = height - (prevNormY * height)

                // Smooth cubic bezier
                val controlX1 = prevX + (x - prevX) / 2f
                val controlY1 = prevY
                val controlX2 = prevX + (x - prevX) / 2f
                val controlY2 = y

                path.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
                fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
            }
        }

        // Complete fill path
        if (dataPoints.size > 1) {
            val lastX = (dataPoints.size - 1) * stepX
            fillPath.lineTo(lastX, height)
            fillPath.close()

            if (fillGradient) {
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            lineColor.copy(alpha = 0.35f),
                            lineColor.copy(alpha = 0.02f)
                        ),
                        startY = 0f,
                        endY = height
                    )
                )
            }
        }

        // Draw line
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(
                width = 3f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Draw active point on the latest data
        if (dataPoints.isNotEmpty()) {
            val lastClamped = dataPoints.last().coerceIn(minValue, maxValue)
            val lastNormY = (lastClamped - minValue) / range
            val lastX = if (dataPoints.size > 1) (dataPoints.size - 1) * stepX else 0f
            val lastY = height - (lastNormY * height)

            drawCircle(
                color = lineColor.copy(alpha = 0.4f),
                radius = 7f,
                center = Offset(lastX, lastY)
            )
            drawCircle(
                color = Color.White,
                radius = 3.5f,
                center = Offset(lastX, lastY)
            )
        }
    }
}
