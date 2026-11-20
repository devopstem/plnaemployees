package by.smartplanogram.spmob.ui.splash

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import by.smartplanogram.spmob.R

/**
 * Full-screen brand loader while Room seed / first sync warm up.
 * Animated rack shelves + arc spinner — no bitmap dependency.
 */
@Composable
fun SplashLoadingScreen(
    statusText: String = stringResource(R.string.splash_loading),
) {
    val transition = rememberInfiniteTransition(label = "splash")
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "spin",
    )
    val shelfPulse by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    val bg = MaterialTheme.colorScheme.background
    val purple = MaterialTheme.colorScheme.primary
    val cyan = MaterialTheme.colorScheme.secondary
    val onBg = MaterialTheme.colorScheme.onBackground

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationZ = spin },
                ) {
                    drawArc(
                        color = cyan.copy(alpha = 0.9f),
                        startAngle = -90f,
                        sweepAngle = 110f,
                        useCenter = false,
                        style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round),
                        size = Size(size.minDimension, size.minDimension),
                        topLeft = Offset.Zero,
                    )
                    drawArc(
                        color = purple.copy(alpha = 0.45f),
                        startAngle = 80f,
                        sweepAngle = 80f,
                        useCenter = false,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
                        size = Size(size.minDimension, size.minDimension),
                        topLeft = Offset.Zero,
                    )
                }
                Canvas(modifier = Modifier.size(72.dp)) {
                    val w = size.width
                    val h = size.height
                    val left = w * 0.18f
                    val right = w * 0.82f
                    val stroke = 4.dp.toPx()
                    // Uprights
                    drawRoundRect(
                        color = purple,
                        topLeft = Offset(left, h * 0.12f),
                        size = Size(stroke * 1.4f, h * 0.72f),
                        cornerRadius = CornerRadius(2f, 2f),
                    )
                    drawRoundRect(
                        color = purple,
                        topLeft = Offset(right - stroke * 1.4f, h * 0.12f),
                        size = Size(stroke * 1.4f, h * 0.72f),
                        cornerRadius = CornerRadius(2f, 2f),
                    )
                    // Shelves with pulse alpha
                    val shelfColor = cyan.copy(alpha = 0.45f + 0.55f * shelfPulse)
                    listOf(0.32f, 0.50f, 0.68f).forEach { yFrac ->
                        drawRoundRect(
                            color = shelfColor,
                            topLeft = Offset(left, h * yFrac),
                            size = Size(right - left, stroke * 1.2f),
                            cornerRadius = CornerRadius(2f, 2f),
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = purple,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = onBg.copy(alpha = 0.72f),
            )
            Spacer(Modifier.height(16.dp))
            // Thin progress bar shimmer
            Canvas(
                modifier = Modifier
                    .width(140.dp)
                    .height(4.dp),
            ) {
                drawRoundRect(
                    color = purple.copy(alpha = 0.2f),
                    cornerRadius = CornerRadius(4f, 4f),
                    size = size,
                )
                val barW = size.width * (0.28f + 0.35f * shelfPulse)
                drawRoundRect(
                    color = cyan,
                    cornerRadius = CornerRadius(4f, 4f),
                    size = Size(barW, size.height),
                    topLeft = Offset((size.width - barW) * shelfPulse, 0f),
                )
            }
        }
    }
}
