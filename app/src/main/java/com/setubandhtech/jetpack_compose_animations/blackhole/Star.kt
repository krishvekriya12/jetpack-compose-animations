package com.setubandhtech.jetpack_compose_animations.blackhole

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*
import kotlin.random.Random


data class Star(
    var x: Float,
    var y: Float,
    var size: Float,
    var brightness: Float,
    var twinkleSeed: Float
)

data class RingStreak(
    val angle: Float,
    val radiusFrac: Float,
    val angularLength: Float,
    val thickness: Float,
    val alphaBase: Float,
    val driftSpeed: Float
)

data class NebulaBlob(
    val xFrac: Float,
    val yFrac: Float,
    val radiusFrac: Float,
    val color: Color
)

@Composable
fun BlackHole() {
    var screenW by remember { mutableStateOf(0f) }
    var screenH by remember { mutableStateOf(0f) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    val stars = remember { mutableStateListOf<Star>() }

    val nebulaBlobs = remember {
        listOf(
            NebulaBlob(0.12f, 0.18f, 0.6f, Color(0xFF241537)),
            NebulaBlob(0.88f, 0.8f, 0.65f, Color(0xFF14213A)),
            NebulaBlob(0.78f, 0.12f, 0.42f, Color(0xFF1A1220)),
            NebulaBlob(0.15f, 0.88f, 0.5f, Color(0xFF20142E)),
            NebulaBlob(0.5f, 0.05f, 0.35f, Color(0xFF241A2E))
        )
    }

    val ringStreaks = remember {
        List(420) {
            RingStreak(
                angle = Random.nextFloat() * 2f * PI.toFloat(),
                radiusFrac = Random.nextFloat().pow(0.6f),
                angularLength = Random.nextFloat() * 0.035f + 0.008f,
                thickness = Random.nextFloat() * 1.4f + 0.5f,
                alphaBase = Random.nextFloat() * 0.5f + 0.3f,
                driftSpeed = (Random.nextFloat() * 0.4f + 0.1f) * (if (Random.nextBoolean()) 1f else -1f)
            )
        }
    }

    val infinite = rememberInfiniteTransition(label = "bh")
    val ringSpin by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(55000, easing = LinearEasing)),
        label = "ringSpin"
    )

    val shimmer by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing)),
        label = "shimmer"
    )

    val haloPulse by infinite.animateFloat(
        initialValue = 0.92f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )

    LaunchedEffect(screenW, screenH) {
        if (screenW == 0f || stars.isNotEmpty()) return@LaunchedEffect
        repeat(650) {
            stars.add(
                Star(
                    x = Random.nextFloat() * screenW,
                    y = Random.nextFloat() * screenH,
                    size = Random.nextFloat().let { r -> if (r > 0.94f) Random.nextFloat() * 2.0f + 2.2f else Random.nextFloat() * 1.6f + 0.6f },
                    brightness = Random.nextFloat() * 0.6f + 0.3f,
                    twinkleSeed = Random.nextFloat() * 100f
                )
            )
        }
    }

    // Advance star twinkle timing / occlusion is computed live in the draw
    // block below (based on the hole's current cx, cy), so no per-frame
    // state mutation is needed here.

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectDragGestures { _, dragAmount ->
                    dragOffset += dragAmount * 0.35f
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            screenW = size.width
            screenH = size.height

            val cx = size.width / 2f + dragOffset.x
            val cy = size.height / 2f + dragOffset.y

            val shadowR = 130f
            val photonR = shadowR * 1.06f
            val haloInnerR = shadowR * 1.05f
            val haloOuterR = shadowR * 2.05f
            val hideR = shadowR * 1.02f     // stars this close to centre are treated as "under" the hole
            val influenceR = shadowR * 2.6f // once the hole gets this close to a star, it starts pulling it in

            // ---------- deep space background ----------
            drawRect(color = Color(0xFF040407))
            nebulaBlobs.forEach { blob ->
                val bx = blob.xFrac * size.width
                val by = blob.yFrac * size.height
                val br = blob.radiusFrac * size.width
                drawCircle(
                    brush = Brush.radialGradient(colors = listOf(blob.color.copy(alpha = 0.4f), Color.Transparent), center = Offset(bx, by), radius = br),
                    radius = br,
                    center = Offset(bx, by)
                )
            }
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color(0xFF000000).copy(alpha = 0.5f)),
                    center = Offset(cx, cy),
                    radius = size.width * 0.85f
                )
            )

            fun haloColor(t: Float): Color {
                val near = Color(0xFFFFFFFF)
                val mid = Color(0xFFCBB9EE)
                val far = Color(0xFF6E77B8)
                return when {
                    t < 0.4f -> lerp(near, mid, t / 0.4f)
                    else -> lerp(mid, far, ((t - 0.4f) / 0.6f).coerceIn(0f, 1f))
                }
            }

            // ---------- starfield — plain twinkle far away; as the hole approaches a
            // star it's gradually pulled inward and swept into a curved streak that
            // spirals into the halo ring, then vanishes under it — like real
            // gravitational light-bending, not a screen-spanning effect. ----------
            stars.forEach { star ->
                val dx = star.x - cx
                val dy = star.y - cy
                val dist = sqrt(dx * dx + dy * dy)
                val angle = atan2(dy, dx)
                val twinkle = (sin(shimmer + star.twinkleSeed) * 0.3f + 0.7f)

                when {
                    dist <= hideR -> { /* fully under the hole */ }
                    dist < influenceR -> {
                        // 0 = just entering the influence zone, 1 = right at the edge
                        val pull = (((influenceR - dist) / (influenceR - hideR)).coerceIn(0f, 1f)).pow(1.6f)

                        val mergeRadius = haloInnerR + (haloOuterR - haloInnerR) * 0.25f
                        val leadRadius = dist + (mergeRadius - dist) * pull
                        val trailRadius = dist + (mergeRadius - dist) * (pull * 0.35f)

                        val sweep = pull * 0.55f // always swept the same way the ring itself spins
                        val a0 = angle - sweep * 0.15f
                        val a1 = angle + sweep
                        val aMid = angle + sweep * 0.5f
                        val midRadius = (leadRadius + trailRadius) / 2f * 1.04f

                        val path = Path().apply {
                            moveTo(cx + cos(a0) * trailRadius, cy + sin(a0) * trailRadius)
                            quadraticTo(
                                cx + cos(aMid) * midRadius, cy + sin(aMid) * midRadius,
                                cx + cos(a1) * leadRadius, cy + sin(a1) * leadRadius
                            )
                        }

                        val alpha = (star.brightness * twinkle * (0.55f + pull * 0.45f)).coerceIn(0f, 1f)
                        val width = star.size * (0.7f + pull * 1.1f)
                        drawPath(path, color = Color.White.copy(alpha = alpha), style = Stroke(width = width, cap = StrokeCap.Round))
                    }
                    else -> {
                        drawCircle(color = Color.White.copy(alpha = (star.brightness * twinkle).coerceIn(0f, 1f)), radius = star.size, center = Offset(star.x, star.y))
                    }
                }
            }

            // ---------- the halo: fine hair-strand texture that rotates rigidly, slowly ----------
            ringStreaks.forEach { s ->
                val liveAngle = s.angle + ringSpin + shimmer * 0.015f * s.driftSpeed
                val r = haloInnerR + (haloOuterR - haloInnerR) * s.radiusFrac
                val a0 = liveAngle - s.angularLength
                val a1 = liveAngle + s.angularLength
                val col = haloColor(s.radiusFrac)
                val fade = (1f - s.radiusFrac).coerceIn(0.15f, 1f)
                val path = Path().apply {
                    moveTo(cx + cos(a0) * r, cy + sin(a0) * r)
                    lineTo(cx + cos(a1) * r, cy + sin(a1) * r)
                }
                drawPath(
                    path = path,
                    color = col.copy(alpha = (s.alphaBase * fade * haloPulse).coerceIn(0f, 1f)),
                    style = Stroke(width = s.thickness)
                )
            }

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.10f * haloPulse), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = haloOuterR * 1.1f
                ),
                radius = haloOuterR * 1.1f,
                center = Offset(cx, cy)
            )

            // ---------- photon ring ----------
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.9f * haloPulse), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = photonR
                ),
                radius = photonR,
                center = Offset(cx, cy)
            )

            // ---------- the shadow ----------
            drawCircle(color = Color.Black, radius = shadowR, center = Offset(cx, cy))

            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)),
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = size.width * 0.75f
                )
            )
        }

        Column(modifier = Modifier.align(Alignment.TopStart).padding(24.dp)) {
            Text(
                text = "Black Hole",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.9f)
            )
            Text(
                text = "Slowly spinning halo • nearby stars spiral in",
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.3f),
                letterSpacing = 1.sp
            )
        }

        Text(
            text = "Drag to move",
            fontSize = 10.sp,
            color = Color.White.copy(alpha = 0.2f),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)
        )
    }
}

private fun lerp(a: Color, b: Color, t: Float): Color {
    val tt = t.coerceIn(0f, 1f)
    return Color(
        red = a.red + (b.red - a.red) * tt,
        green = a.green + (b.green - a.green) * tt,
        blue = a.blue + (b.blue - a.blue) * tt,
        alpha = a.alpha + (b.alpha - a.alpha) * tt
    )
}