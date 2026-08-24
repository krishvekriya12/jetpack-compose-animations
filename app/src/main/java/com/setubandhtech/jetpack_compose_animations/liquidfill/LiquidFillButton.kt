package com.setubandhtech.jetpack_compose_animations.liquidfill

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin

@Composable
fun LiquidFillButton() {
    var isFilling by remember { mutableStateOf(false) }
    var isFilled by remember { mutableStateOf(false) }

    val fillProgress by animateFloatAsState(
        targetValue = if (isFilling) 1f else 0f,
        animationSpec = tween(
            durationMillis = 1500,
            easing = LinearEasing
        ),
        finishedListener = {
            if (it == 1f) isFilled = true
        },
        label = "fill_progress"
    )

    val waveOffset by rememberInfiniteTransition(label = "wave")
        .animateFloat(
            initialValue = 0f,
            targetValue = 2f * Math.PI.toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "wave_offset"
        )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0F)),
        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                text = when {
                    isFilled -> "Charged! ⚡"
                    isFilling -> "Charging..."
                    else -> "Hold to Fill"
                },
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 48.dp)
            )

            Box(
                modifier = Modifier
                    .width(240.dp)
                    .height(72.dp)
                    .clip(RoundedCornerShape(36.dp))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isFilling = true
                                isFilled = false
                                tryAwaitRelease()
                                if (!isFilled) {
                                    isFilling = false
                                }
                            }
                        )
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height

                    drawRect(color = Color(0xFF1A1A2E))

                    val buttonPath = Path().apply {
                        addRoundRect(
                            androidx.compose.ui.geometry.RoundRect(
                                rect = Rect(0f, 0f, width, height),
                                radiusX = height / 2,
                                radiusY = height / 2
                            )
                        )
                    }

                    clipPath(buttonPath, clipOp = ClipOp.Intersect) {
                        val fillLevel = height - (height * fillProgress)

                        val wavePath = Path().apply {
                            moveTo(0f, fillLevel)

                            var x = 0f
                            while (x <= width) {
                                val y = fillLevel + sin(
                                    (x / width * 2f * Math.PI + waveOffset).toDouble()
                                ).toFloat() * 8f
                                lineTo(x, y)
                                x += 2f
                            }

                            lineTo(width, height)
                            lineTo(0f, height)
                            close()
                        }

                        drawPath(
                            path = wavePath,
                            color = Color(0xFF6200EE).copy(
                                alpha = if (isFilling || isFilled) 1f else 0f
                            )
                        )

                        val wavePath2 = Path().apply {
                            moveTo(0f, fillLevel)

                            var x = 0f
                            while (x <= width) {
                                val y = fillLevel + sin(
                                    (x / width * 2f * Math.PI + waveOffset + 1f).toDouble()
                                ).toFloat() * 6f
                                lineTo(x, y)
                                x += 2f
                            }

                            lineTo(width, height)
                            lineTo(0f, height)
                            close()
                        }

                        drawPath(
                            path = wavePath2,
                            color = Color(0xFF03DAC5).copy(
                                alpha = if (isFilling || isFilled) 0.5f else 0f
                            )
                        )
                    }

                    drawRoundRect(
                        color = Color(0xFF6200EE),
                        size = size,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                            height / 2,
                            height / 2
                        ),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 3f
                        )
                    )
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when {
                            isFilled -> "✅ Done!"
                            isFilling -> "${(fillProgress * 100).toInt()}%"
                            else -> "⚡ CHARGE"
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (isFilled) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(Color(0xFF1A1A2E))
                        .pointerInput(Unit) {
                            detectTapGestures {
                                isFilled = false
                                isFilling = false
                            }
                        }
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Reset 🔄",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Pure Canvas • Wave Math • No Library",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.3f),
                letterSpacing = 1.sp
            )
        }
    }
}