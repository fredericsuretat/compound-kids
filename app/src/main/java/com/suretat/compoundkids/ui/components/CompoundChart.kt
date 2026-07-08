package com.suretat.compoundkids.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suretat.compoundkids.data.ScenarioResult
import com.suretat.compoundkids.data.formatEuro
import kotlin.math.roundToInt

@Composable
fun CompoundChart(
    results: List<ScenarioResult>,
    showReal: Boolean,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 9.sp, color = Color(0xFF757575))
    val gridColor  = Color(0xFFEEEEEE)
    val axisColor  = Color(0xFFBDBDBD)

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val padL = 52f
            val padR = 12f
            val padT = 10f
            val padB = 30f

            val chartW = size.width - padL - padR
            val chartH = size.height - padT - padB

            val maxYears = results.firstOrNull()?.points?.lastOrNull()?.year ?: 20
            val maxVal = results.maxOfOrNull { r ->
                r.points.maxOf { p -> if (showReal) p.real else p.nominal }
            }?.takeIf { it > 0.0 } ?: 1.0

            fun xOf(year: Int) = padL + (year.toFloat() / maxYears) * chartW
            fun yOf(v: Double): Float {
                val clamped = v.coerceIn(0.0, maxVal)
                return padT + chartH * (1.0 - clamped / maxVal).toFloat()
            }

            // Horizontal grid + Y labels
            val ySteps = 4
            for (i in 0..ySteps) {
                val v = maxVal * i / ySteps
                val y = yOf(v)
                if (i > 0) {
                    drawLine(gridColor, Offset(padL, y), Offset(size.width - padR, y), strokeWidth = 1f)
                }
                val label = formatEuro(v)
                val measured = textMeasurer.measure(label, style = labelStyle)
                drawText(
                    measured,
                    topLeft = Offset(padL - measured.size.width - 4f, y - measured.size.height / 2f)
                )
            }

            // X-axis year labels
            val yearStep = when {
                maxYears <= 10 -> 2
                maxYears <= 20 -> 5
                else           -> 10
            }
            for (yr in 0..maxYears step yearStep) {
                val x = xOf(yr)
                drawLine(axisColor, Offset(x, padT + chartH), Offset(x, padT + chartH + 4f), strokeWidth = 1f)
                val label = if (yr == 0) "0" else "${yr}a"
                val measured = textMeasurer.measure(label, style = labelStyle)
                drawText(measured, topLeft = Offset(x - measured.size.width / 2f, padT + chartH + 6f))
            }

            // Curves — draw fills first, then strokes on top
            for (result in results) {
                val pts = result.points
                if (pts.size < 2) continue

                val fillPath = Path()
                fillPath.moveTo(xOf(pts.first().year), yOf(0.0))
                for (p in pts) {
                    val v = if (showReal) p.real else p.nominal
                    fillPath.lineTo(xOf(p.year), yOf(v))
                }
                fillPath.lineTo(xOf(pts.last().year), yOf(0.0))
                fillPath.close()
                drawPath(fillPath, color = result.scenario.color.copy(alpha = 0.10f))

                val strokePath = Path()
                var first = true
                for (p in pts) {
                    val v = if (showReal) p.real else p.nominal
                    val x = xOf(p.year)
                    val y = yOf(v)
                    if (first) { strokePath.moveTo(x, y); first = false }
                    else strokePath.lineTo(x, y)
                }
                drawPath(
                    strokePath, color = result.scenario.color,
                    style = Stroke(
                        width = 2.5.dp.toPx(),
                        cap   = StrokeCap.Round,
                        join  = StrokeJoin.Round
                    )
                )

                // Dot + label at end of curve
                val lastPt = pts.last()
                val lastV  = if (showReal) lastPt.real else lastPt.nominal
                val dotX   = xOf(lastPt.year)
                val dotY   = yOf(lastV)
                drawCircle(result.scenario.color, radius = 4.dp.toPx(), center = Offset(dotX, dotY))
                drawCircle(Color.White, radius = 2.dp.toPx(), center = Offset(dotX, dotY))
            }

            // Axes
            drawLine(axisColor, Offset(padL, padT), Offset(padL, padT + chartH), strokeWidth = 1.5f)
            drawLine(axisColor, Offset(padL, padT + chartH), Offset(size.width - padR, padT + chartH), strokeWidth = 1.5f)
        }

        // Legend row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            results.forEach { r ->
                LegendItem(
                    emoji = r.scenario.emoji,
                    name  = r.scenario.name,
                    color = r.scenario.color
                )
            }
        }
    }
}

@Composable
private fun LegendItem(emoji: String, name: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 2.dp)
    ) {
        Canvas(modifier = Modifier.size(10.dp)) {
            drawRect(color)
        }
        Spacer(Modifier.width(3.dp))
        Text(
            text  = "$emoji $name",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF424242)
        )
    }
}
