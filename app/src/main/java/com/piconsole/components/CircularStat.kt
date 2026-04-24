package com.piconsole.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.piconsole.ui.theme.PiConsoleTheme

@Composable
fun CircularStat(
    percentage: Float,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    radius: Dp = 60.dp,
    strokeWidth: Dp = 8.dp
) {
    var animationPlayed by remember { mutableFloatStateOf(0f) }
    val currentPercentage = animateFloatAsState(
        targetValue = animationPlayed,
        animationSpec = tween(durationMillis = 1000),
        label = "progress_animation"
    )

    LaunchedEffect(percentage) {
        animationPlayed = percentage
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(radius * 2f)
    ) {
        Canvas(modifier = Modifier.size(radius * 2f)) {
            // Background arc
            drawArc(
                color = color.copy(alpha = 0.2f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(strokeWidth.toPx(), cap = StrokeCap.Round)
            )
            // Foreground arc
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360 * (currentPercentage.value / 100f),
                useCenter = false,
                style = Stroke(strokeWidth.toPx(), cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${currentPercentage.value.toInt()}%",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CircularStatPreview() {
    PiConsoleTheme {
        CircularStat(
            percentage = 65f,
            label = "CPU Load",
            color = Color.Cyan,
            modifier = Modifier.padding(16.dp)
        )
    }
}
