package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.BorderNavy

@Composable
fun WaveformCanvas(
    amplitudes: List<Float>,
    modifier: Modifier = Modifier.fillMaxWidth().height(48.dp),
    barColor: Color = AmberPrimary
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f

        val totalBars = 32
        val barWidth = 6.dp.toPx()
        val spacing = (width - (totalBars * barWidth)) / (totalBars + 1)

        val amps = if (amplitudes.isEmpty()) {
            List(totalBars) { 0.1f }
        } else if (amplitudes.size < totalBars) {
            List(totalBars - amplitudes.size) { 0.1f } + amplitudes
        } else {
            amplitudes.takeLast(totalBars)
        }

        for (i in 0 until totalBars) {
            val normAmp = (amps.getOrNull(i) ?: 0.1f).coerceIn(0.08f, 1f)
            val barHeight = (height * 0.85f * normAmp).coerceAtLeast(4.dp.toPx())
            val x = spacing + i * (barWidth + spacing)
            val top = centerY - (barHeight / 2f)

            drawRect(
                color = if (normAmp > 0.15f) barColor else BorderNavy,
                topLeft = Offset(x, top),
                size = Size(barWidth, barHeight)
            )
        }
    }
}
