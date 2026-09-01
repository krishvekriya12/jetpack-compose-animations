package com.setubandhtech.jetpack_compose_animations.spidermanweb

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

data class WebShot(
    val id: Int,
    val origin: Offset,
    val target: Offset,
    val progress: Float = 0f,
    val rings: Int = (5..9).random(),
    val spokes: Int = (10..16).random(),
    val swayPhase: Float = (0..360).random().toFloat()
)

data class AnchorWeb(
    val id: Int,
    val p1: Offset,
    val p2: Offset,
    val sag: Float,
    val alpha: Float = 1f
)

@Composable
fun SpiderManWebShooter() {
    var webShots by remember { mutableStateOf<List<WebShot>>(emptyList()) }
    var anchorWebs by remember { mutableStateOf<List<AnchorWeb>>(emptyList()) }
    var shotCounter by remember { mutableIntStateOf(0) }
    var lastTarget by remember { mutableStateOf<Offset?>(null) }

    val infiniteTransition = rememberInfiniteTransition(label = "web")

    val tick by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(16, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "tick"
    )

    val swayAngle by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sway"
    )

    val shimmer by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    LaunchedEffect(tick) {
        webShots = webShots
            .map { it.copy(progress = (it.progress + 0.025f).coerceAtMost(1f)) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080C14))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val origin = Offset(size.width / 2f, size.height.toFloat())

                    lastTarget?.let { prev ->
                        val sag = (offset.y - prev.y).absoluteValue * 0.3f + 20f
                        anchorWebs = (anchorWebs + AnchorWeb(
                            id = shotCounter,
                            p1 = prev,
                            p2 = offset,
                            sag = sag
                        )).takeLast(15)
                    }

                    webShots = (webShots + WebShot(
                        id = shotCounter++,
                        origin = origin,
                        target = offset
                    )).takeLast(6)

                    lastTarget = offset
                }
            }
    ) {

        Canvas(modifier = Modifier.fillMaxSize()) {

            anchorWebs.forEach { web ->
                drawSilkThread(
                    start = web.p1,
                    end = web.p2,
                    sag = web.sag,
                    color = Color.White,
                    alpha = 0.25f,
                    strokeWidth = 1f,
                    shimmer = shimmer
                )
            }

            webShots.forEach { shot ->
                drawRealisticWeb(
                    shot = shot,
                    swayAngle = swayAngle,
                    shimmer = shimmer
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🕷️ Spider-Man",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE53935),
                modifier = Modifier.padding(top = 32.dp)
            )
            Text(
                text = "Tap anywhere to shoot 🕸️",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.padding(bottom = 48.dp)
            )
        }
    }
}

fun DrawScope.drawRealisticWeb(
    shot: WebShot,
    swayAngle: Float,
    shimmer: Float
) {
    val progress = shot.progress
    val origin = shot.origin
    val target = shot.target

    val sag = (target.y - origin.y).absoluteValue * 0.15f + 10f
    val midX = (origin.x + target.x) / 2f + swayAngle * 3f
    val midY = (origin.y + target.y) / 2f + sag

    val tipX = lerp(origin.x, target.x, progress)
    val tipY = lerp(origin.y, target.y, progress) +
            sag * 4f * progress * (1f - progress)

    val tipOffset = Offset(tipX, tipY)

    drawCatenaryThread(
        start = origin,
        end = tipOffset,
        sag = sag * progress,
        color = Color(0xFFE53935),
        alpha = 0.15f,
        strokeWidth = 6f
    )

    drawCatenaryThread(
        start = origin,
        end = tipOffset,
        sag = sag * progress,
        color = Color(0xFFCCCCCC),
        alpha = 0.5f,
        strokeWidth = 2f
    )

    val shimmerAlpha = 0.7f + sin(shimmer * 2f * Math.PI.toFloat() + shot.swayPhase) * 0.3f
    drawCatenaryThread(
        start = origin,
        end = tipOffset,
        sag = sag * progress,
        color = Color.White,
        alpha = shimmerAlpha,
        strokeWidth = 0.8f
    )

    if (progress > 0.85f) {
        val webAlpha = ((progress - 0.85f) / 0.15f).coerceIn(0f, 1f)
        drawSpiderWeb(
            center = target,
            maxRadius = 90f * webAlpha,
            spokes = shot.spokes,
            rings = shot.rings,
            swayAngle = swayAngle,
            shimmer = shimmer,
            alpha = webAlpha
        )
    }

    if (progress < 1f) {
        drawCircle(
            color = Color.White.copy(alpha = 0.9f),
            radius = 4f,
            center = tipOffset
        )
        drawCircle(
            color = Color(0xFFE53935).copy(alpha = 0.3f),
            radius = 10f,
            center = tipOffset
        )
    }
}

fun DrawScope.drawSpiderWeb(
    center: Offset,
    maxRadius: Float,
    spokes: Int,
    rings: Int,
    swayAngle: Float,
    shimmer: Float,
    alpha: Float
) {
    val spokeEndpoints = List(spokes) { i ->
        val angle = (i.toFloat() / spokes) * 2f * Math.PI.toFloat() - Math.PI.toFloat() / 2f
        val swayX = swayAngle * 2f * sin(angle)
        Offset(
            x = center.x + cos(angle) * maxRadius + swayX,
            y = center.y + sin(angle) * maxRadius +
                    abs(swayAngle) * 3f
        )
    }

    spokeEndpoints.forEach { endpoint ->
        drawLine(
            color = Color(0xFFE53935).copy(alpha = 0.1f * alpha),
            start = center,
            end = endpoint,
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.White.copy(alpha = 0.4f * alpha),
            start = center,
            end = endpoint,
            strokeWidth = 0.8f,
            cap = StrokeCap.Round
        )
    }

    repeat(rings) { ring ->
        val ringRatio = (ring + 1).toFloat() / rings
        val ringRadius = maxRadius * ringRatio

        val ringPoints = List(spokes + 1) { i ->
            val idx = i % spokes
            val angle = (idx.toFloat() / spokes) * 2f * Math.PI.toFloat() -
                    Math.PI.toFloat() / 2f
            val swayX = swayAngle * 2f * sin(angle) * ringRatio
            Offset(
                x = center.x + cos(angle) * ringRadius + swayX,
                y = center.y + sin(angle) * ringRadius +
                        abs(swayAngle) * 3f * ringRatio
            )
        }

        for (i in 0 until spokes) {
            val p1 = ringPoints[i]
            val p2 = ringPoints[i + 1]
            val segSag = (ringRadius / spokes) * 0.3f

            val segShimmer = sin(shimmer * 2f * Math.PI.toFloat() +
                    i * 0.5f + ring * 0.3f) * 0.2f + 0.8f

            drawSilkThread(
                start = p1,
                end = p2,
                sag = segSag,
                color = Color(0xFFE53935),
                alpha = 0.08f * alpha,
                strokeWidth = 3f,
                shimmer = shimmer
            )

            drawSilkThread(
                start = p1,
                end = p2,
                sag = segSag,
                color = Color.White,
                alpha = (0.3f + (1f - ringRatio) * 0.4f) * alpha * segShimmer,
                strokeWidth = 0.7f,
                shimmer = shimmer
            )
        }
    }

    repeat(rings) { ring ->
        val ringRatio = (ring + 1).toFloat() / rings
        val ringRadius = maxRadius * ringRatio
        repeat(spokes) { spoke ->
            val angle = (spoke.toFloat() / spokes) * 2f * Math.PI.toFloat() -
                    Math.PI.toFloat() / 2f + (1f / spokes / 2f) * 2f * Math.PI.toFloat()
            val swayX = swayAngle * 2f * sin(angle) * ringRatio
            val dropX = center.x + cos(angle) * ringRadius + swayX
            val dropY = center.y + sin(angle) * ringRadius +
                    abs(swayAngle) * 3f * ringRatio

            if ((ring + spoke) % 3 == 0) {
                drawCircle(
                    color = Color(0xFF64B5F6).copy(alpha = 0.4f * alpha),
                    radius = 2.5f,
                    center = Offset(dropX, dropY)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.6f * alpha),
                    radius = 1f,
                    center = Offset(dropX - 0.5f, dropY - 0.5f)
                )
            }
        }
    }

    drawCircle(
        color = Color(0xFFE53935).copy(alpha = 0.3f * alpha),
        radius = 8f,
        center = center
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.8f * alpha),
        radius = 3f,
        center = center
    )
}

fun DrawScope.drawCatenaryThread(
    start: Offset,
    end: Offset,
    sag: Float,
    color: Color,
    alpha: Float,
    strokeWidth: Float
) {
    val path = Path()
    val steps = 30

    path.moveTo(start.x, start.y)

    for (i in 1..steps) {
        val t = i.toFloat() / steps
        val x = lerp(start.x, end.x, t)
        val sagY = sag * 4f * t * (1f - t)
        val y = lerp(start.y, end.y, t) + sagY
        path.lineTo(x, y)
    }

    drawPath(
        path = path,
        color = color.copy(alpha = alpha),
        style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round
        )
    )
}

fun DrawScope.drawSilkThread(
    start: Offset,
    end: Offset,
    sag: Float,
    color: Color,
    alpha: Float,
    strokeWidth: Float,
    shimmer: Float
) {
    val path = Path()
    val steps = 20

    path.moveTo(start.x, start.y)

    for (i in 1..steps) {
        val t = i.toFloat() / steps
        val x = lerp(start.x, end.x, t)
        val sagY = sag * 4f * t * (1f - t)
        val y = lerp(start.y, end.y, t) + sagY
        path.lineTo(x, y)
    }

    drawPath(
        path = path,
        color = color.copy(alpha = alpha),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )
}

fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
fun Float.absoluteValue() = if (this < 0) -this else this