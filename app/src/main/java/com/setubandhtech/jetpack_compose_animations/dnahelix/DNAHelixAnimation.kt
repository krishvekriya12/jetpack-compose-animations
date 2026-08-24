package com.setubandhtech.jetpack_compose_animations.dnahelix

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DNAHelixAnimation() {

    val infiniteTransition = rememberInfiniteTransition(label = "dna")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dna_rotation"
    )

    val strand1Color = Color(0xFF6200EE)
    val strand2Color = Color(0xFF03DAC5)
    val bridgeColor = Color(0xFFFFFFFF)
    val glowColor1 = Color(0xFF9C27B0)
    val glowColor2 = Color(0xFF00BCD4)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0F)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "DNA Helix 🧬",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Canvas(
                modifier = Modifier
                    .width(200.dp)
                    .height(500.dp)
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val centerX = canvasWidth / 2
                val amplitude = canvasWidth * 0.35f
                val numPoints = 40
                val spacing = canvasHeight / numPoints

                for (i in 0 until numPoints) {
                    val y = i * spacing
                    val angle = rotation + (i.toFloat() / numPoints) * 2f * Math.PI.toFloat() * 2f

                    val x1 = centerX + amplitude * cos(angle)
                    val x2 = centerX + amplitude * cos(angle + Math.PI.toFloat())

                    val depth1 = (cos(angle) + 1f) / 2f
                    val depth2 = (cos(angle + Math.PI.toFloat()) + 1f) / 2f

                    val dot1Size = 6f + depth1 * 10f
                    val dot2Size = 6f + depth2 * 10f

                    val bridgeAlpha = abs(sin(angle)) * 0.6f
                    if (i % 3 == 0) {
                        drawLine(
                            color = bridgeColor.copy(alpha = bridgeAlpha),
                            start = Offset(x1, y),
                            end = Offset(x2, y),
                            strokeWidth = 2f,
                            cap = StrokeCap.Round
                        )

                        val midX = (x1 + x2) / 2
                        drawCircle(
                            color = Color(0xFFFFD700).copy(alpha = bridgeAlpha),
                            radius = 4f,
                            center = Offset(midX, y)
                        )
                    }

                    drawCircle(
                        color = glowColor1.copy(alpha = depth1 * 0.3f),
                        radius = dot1Size * 2f,
                        center = Offset(x1, y)
                    )

                    drawCircle(
                        color = glowColor2.copy(alpha = depth2 * 0.3f),
                        radius = dot2Size * 2f,
                        center = Offset(x2, y)
                    )

                    drawCircle(
                        color = strand1Color.copy(alpha = 0.5f + depth1 * 0.5f),
                        radius = dot1Size,
                        center = Offset(x1, y)
                    )

                    drawCircle(
                        color = strand2Color.copy(alpha = 0.5f + depth2 * 0.5f),
                        radius = dot2Size,
                        center = Offset(x2, y)
                    )

                    if (i > 0) {
                        val prevAngle = rotation + ((i - 1).toFloat() / numPoints) * 2f * Math.PI.toFloat() * 2f
                        val prevY = (i - 1) * spacing

                        val prevX1 = centerX + amplitude * cos(prevAngle)
                        val prevX2 = centerX + amplitude * cos(prevAngle + Math.PI.toFloat())

                        val prevDepth1 = (cos(prevAngle) + 1f) / 2f
                        val prevDepth2 = (cos(prevAngle + Math.PI.toFloat()) + 1f) / 2f

                        drawLine(
                            color = strand1Color.copy(
                                alpha = ((depth1 + prevDepth1) / 2f) * 0.8f
                            ),
                            start = Offset(prevX1, prevY),
                            end = Offset(x1, y),
                            strokeWidth = 3f + depth1 * 3f,
                            cap = StrokeCap.Round
                        )

                        drawLine(
                            color = strand2Color.copy(
                                alpha = ((depth2 + prevDepth2) / 2f) * 0.8f
                            ),
                            start = Offset(prevX2, prevY),
                            end = Offset(x2, y),
                            strokeWidth = 3f + depth2 * 3f,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Pure Canvas • No Library • 3D Effect 🧬",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.3f),
                letterSpacing = 1.sp
            )
        }
    }
}