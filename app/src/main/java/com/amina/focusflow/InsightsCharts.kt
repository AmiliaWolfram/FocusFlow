package com.amina.focusflow

// Custom charts drawn directly on Canvas — без сторонней chart library.
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View

class FocusBarChart(context: Context) : View(context) {
    var values: List<Int> = emptyList()
    var labels: List<String> = emptyList()
    var highlightIndex: Int = -1
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gridPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFE9E8E1.toInt()
            strokeWidth = Ui.dp(context, 1).toFloat()
        }
    private val axisText =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Ui.muted
            textSize = Ui.dp(context, 10).toFloat()
            textAlign = Paint.Align.RIGHT
        }
    private val labelText =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Ui.muted
            textSize = Ui.dp(context, 10).toFloat()
            textAlign = Paint.Align.CENTER
        }
    private val valueText =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Ui.dark
            textSize = Ui.dp(context, 9).toFloat()
            textAlign = Paint.Align.CENTER
        }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        if (values.isEmpty()) return
        val maxValue = values.maxOrNull() ?: 0
        val scaleMax = niceScale(maxValue)
        val left = Ui.dp(context, 34).toFloat()
        val right = Ui.dp(context, 8).toFloat()
        val top = Ui.dp(context, 22).toFloat()
        val base = height - Ui.dp(context, 28).toFloat()
        val plotHeight = base - top
        // Three guide lines are enough here; больше линий визуально перегружали график.
        for (step in 0..2) {
            val value = scaleMax * step / 2
            val y = base - plotHeight * step / 2f
            c.drawLine(left, y, width - right, y, gridPaint)
            c.drawText(formatAxis(value), left - Ui.dp(context, 6), y + Ui.dp(context, 3), axisText)
        }
        val usableWidth = width - left - right
        val slot = usableWidth / values.size.coerceAtLeast(1)
        val minBar =
            if (values.size > 14) Ui.dp(context, 3).toFloat() else Ui.dp(context, 7).toFloat()
        val bw = (slot * .52f).coerceIn(minBar, Ui.dp(context, 22).toFloat())
        values.forEachIndexed { i, v ->
            val x = left + slot * i + slot / 2f
            val barHeight =
                if (v <= 0) 0f
                else
                    (plotHeight * v / scaleMax.toFloat()).coerceAtLeast(Ui.dp(context, 5).toFloat())
            if (v > 0) {
                barPaint.color = if (i == highlightIndex) Ui.orange else Ui.green2
                c.drawRoundRect(
                    RectF(x - bw / 2, base - barHeight, x + bw / 2, base),
                    bw / 2,
                    bw / 2,
                    barPaint,
                )
                if (values.size <= 14 || i == highlightIndex || v == maxValue) {
                    c.drawText(
                        formatValue(v),
                        x,
                        (base - barHeight - Ui.dp(context, 5)).coerceAtLeast(
                            Ui.dp(context, 10).toFloat()
                        ),
                        valueText,
                    )
                }
            }
            labelText.color = if (i == highlightIndex) Ui.dark else Ui.muted
            c.drawText(
                labels.getOrElse(i) {
                    ""
                },
                x,
                height - Ui.dp(context, 7).toFloat(),
                labelText,
            )
        }
    }

    private fun niceScale(maxValue: Int): Int {
        if (maxValue <= 0) return 10
        val candidates = listOf(5, 10, 15, 30, 45, 60, 90, 120, 180, 240, 360, 480, 600, 720)
        return candidates.firstOrNull {
            it >= maxValue
        } ?: (((maxValue + 59) / 60) * 60)
    }

    private fun formatAxis(minutes: Int): String =
        when {
            minutes == 0 -> "0"
            minutes < 60 -> "${minutes}m"
            minutes % 60 == 0 -> "${minutes / 60}h"
            else -> "${minutes / 60}h${minutes % 60}"
        }

    private fun formatValue(minutes: Int): String =
        when {
            minutes < 60 -> "${minutes}m"
            minutes % 60 == 0 -> "${minutes / 60}h"
            else -> "${minutes / 60}h ${minutes % 60}m"
        }
}

class DonutChart(context: Context) : View(context) {
    var values: List<Int> = emptyList()
    var colors: List<Int> = listOf(Ui.green, Ui.orange, Ui.green2, Ui.red)
    private val p =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.BUTT
        }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val total = values.sum().coerceAtLeast(1)
        val stroke = Ui.dp(context, 18).toFloat()
        p.strokeWidth = stroke
        val pad = stroke / 2 + 2
        val rect = RectF(pad, pad, width - pad, height - pad)
        var start = -90f
        if (values.sum() == 0) {
            p.color = Ui.sage
            c.drawArc(rect, 0f, 360f, false, p)
            return
        }
        values.forEachIndexed { i, v ->
            val sweep = 360f * v / total
            p.color = colors[i % colors.size]
            c.drawArc(rect, start, sweep, false, p)
            start += sweep
        }
    }
}
