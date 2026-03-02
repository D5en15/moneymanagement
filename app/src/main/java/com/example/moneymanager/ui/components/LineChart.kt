package com.example.moneymanager.ui.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun LineChart(
    data: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    fillBrush: Brush? = null
) {
    if (data.isEmpty()) return

    val density = LocalDensity.current
    val textPaint = remember(density) {
        Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = density.run { 10.sp.toPx() }
            textAlign = Paint.Align.LEFT
        }
    }
    val defaultBrush = Brush.verticalGradient(
        colors = listOf(
            lineColor.copy(alpha = 0.3f),
            Color.Transparent
        )
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val paddingVertical = 24.dp.toPx() // Space for text
        val chartHeight = height - (paddingVertical * 2)
        
        // 1. Find Range (Auto-Zoom to data with padding)
        val dataMax = data.maxOrNull() ?: 0f
        val dataMin = data.minOrNull() ?: 0f
        
        val diff = dataMax - dataMin
        val padding = if (diff == 0f) 1f else diff * 0.1f // 10% padding
        
        val max = dataMax + padding
        val min = dataMin - padding
        val range = max - min
        
        // 2. Map to Points
        val points = if (data.size < 2) {
            // Single point: Draw a straight line across? Or just a dot?
            // Let's fake 2 points for a line
            listOf(
                Offset(0f, paddingVertical + chartHeight / 2),
                Offset(width, paddingVertical + chartHeight / 2)
            )
        } else {
            data.mapIndexed { index, value ->
                val x = index * (width / (data.size - 1))
                // y = height - ((value - min) / range * height)
                // Add padding
                val ratio = (value - min) / range
                val y = (paddingVertical + chartHeight) - (ratio * chartHeight)
                Offset(x, y)
            }
        }

        // 3. Draw Zero Line (if within range)
        if (min < 0 && max > 0) {
            val zeroRatio = (0 - min) / range
            val zeroY = (paddingVertical + chartHeight) - (zeroRatio * chartHeight)
            drawLine(
                color = Color.Gray.copy(alpha = 0.5f),
                start = Offset(0f, zeroY),
                end = Offset(width, zeroY),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
            )
        }

        // 4. Create Line Path
        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                lineTo(points[i].x, points[i].y)
            }
        }

        // 5. Draw Fill
        val fillPath = Path().apply {
            addPath(path)
            lineTo(points.last().x, height - paddingVertical) // To bottom of chart area
            lineTo(points.first().x, height - paddingVertical)
            close()
        }
        drawPath(
            path = fillPath,
            brush = fillBrush ?: defaultBrush
        )

        // 6. Draw Line
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(
                width = 3.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // 7. Draw Labels
        drawIntoCanvas { canvas ->
            // Max Value
            val maxLabel = "Max: ${max.roundToInt()}"
            textPaint.textAlign = Paint.Align.LEFT
            canvas.nativeCanvas.drawText(maxLabel, 10f, paddingVertical - 10f, textPaint)

            // Min Value
            val minLabel = "Min: ${min.roundToInt()}"
            canvas.nativeCanvas.drawText(minLabel, 10f, height - 5f, textPaint) // Bottom

            // X Axis Labels (Start, Mid, End)
            if (data.isNotEmpty()) {
                val endLabel = "${data.size}"
                textPaint.textAlign = Paint.Align.RIGHT
                canvas.nativeCanvas.drawText(endLabel, width - 10f, height - 5f, textPaint)
                
                val startLabel = "1"
                textPaint.textAlign = Paint.Align.LEFT
                canvas.nativeCanvas.drawText(startLabel, 10f + 100f, height - 5f, textPaint) // Offset from Min label
            }
        }
    }
}
