package com.setubandhtech.jetpack_compose_animations.mandelbrot

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.*
import kotlin.math.*

/**
 * A Mandelbrot explorer that renders IMMEDIATELY on every camera change — no debounce,
 * no low-res preview step. Every pan/pinch/fling frame triggers a fresh full-resolution
 * render right away. This trades some smoothness at high zoom/large screens for
 * on-the-spot visual feedback, which is what was asked for.
 */

private data class Target(val cx: Float, val cy: Float, val zoom: Float, val label: String)

private val TOUR_STOPS = listOf(
    Target(-0.743644f, 0.131826f, 4.2e4f, "Seahorse Valley"),
    Target(-0.1592f, 1.0317f, 1.8e3f, "Elephant Valley"),
    Target(-0.5251993f, 0.5251993f, 6.0e2f, "Spiral Galaxy"),
    Target(0.3750001f, 0.2166f, 9.0e5f, "Mini Mandelbrot"),
    Target(-1.7687f, 0.0f, 3.5e3f, "Feather Region"),
    Target(-0.7453f, 0.1127f, 1.2e4f, "Tendrils"),
    Target(-0.5f, 0.0f, 1.0f, "Full Set")
)

/** Continuous cosine palette (Inigo Quilez style) — cheap, no banding, no per-stop branching. */
private fun paletteColor(smoothIter: Double, maxIter: Int): Color {
    if (smoothIter >= maxIter) return Color(0xFF04050A)
    val t = (smoothIter / maxIter).coerceIn(0.0, 1.0)
    val freq = doubleArrayOf(1.0, 1.0, 0.6)
    val phase = doubleArrayOf(0.0, 0.20, 0.45)
    val hue = t * 3.2 + 0.15
    val r = 0.5 + 0.5 * cos(2 * PI * (freq[0] * hue + phase[0]))
    val g = 0.5 + 0.5 * cos(2 * PI * (freq[1] * hue + phase[1]))
    val b = 0.5 + 0.5 * cos(2 * PI * (freq[2] * hue + phase[2]))
    return Color(
        red = r.toFloat().coerceIn(0f, 1f),
        green = g.toFloat().coerceIn(0f, 1f),
        blue = b.toFloat().coerceIn(0f, 1f)
    )
}

private fun formatZoom(z: Float): String = when {
    z < 1000f -> "${"%.0f".format(z)}\u00D7"
    z < 1_000_000f -> "${"%.1f".format(z / 1000f)}k\u00D7"
    else -> "${"%.2f".format(z / 1_000_000f)}M\u00D7"
}

@Composable
fun MandelbrotZoom() {
    val scope = rememberCoroutineScope()

    val camX = remember { Animatable(-0.5f) }
    val camY = remember { Animatable(0f) }
    val camLogZoom = remember { Animatable(0f) }
    val zoom by remember { derivedStateOf { 10f.pow(camLogZoom.value) } }

    var screenW by remember { mutableStateOf(0) }
    var screenH by remember { mutableStateOf(0) }
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var isRendering by remember { mutableStateOf(false) }
    var autoTour by remember { mutableStateOf(true) }
    var tourIndex by remember { mutableStateOf(0) }
    val tourLabel by remember { derivedStateOf { TOUR_STOPS[tourIndex % TOUR_STOPS.size].label } }

    var renderJob by remember { mutableStateOf<Job?>(null) }
    var renderToken by remember { mutableStateOf(0) }

    // Single render path, always full resolution. Called directly from the
    // LaunchedEffect below every time camX/camY/camLogZoom change, with no delay.
    fun render() {
        if (screenW == 0 || screenH == 0) return
        val myToken = ++renderToken
        renderJob?.cancel()
        renderJob = scope.launch(Dispatchers.Default) {
            val cx = camX.value.toDouble()
            val cy = camY.value.toDouble()
            val z = zoom.toDouble()
            val w = screenW
            val h = screenH

            isRendering = true

            val aspect = w.toDouble() / h
            val span = 3.5 / z
            val xMin = cx - span * aspect / 2
            val yMin = cy - span / 2
            val dx = span * aspect / w
            val dy = span / h
            val iter = (100 + ln(z + 1.0) * 40).toInt().coerceIn(80, 2000)
            val bailout = 4.0

            val pixels = IntArray(w * h)
            for (py in 0 until h) {
                if (!isActive || myToken != renderToken) return@launch
                val cIm0 = yMin + py * dy
                for (px in 0 until w) {
                    val cRe = xMin + px * dx
                    var x = 0.0; var y = 0.0
                    var x2 = 0.0; var y2 = 0.0
                    var it = 0
                    while (x2 + y2 <= bailout && it < iter) {
                        y = 2.0 * x * y + cIm0
                        x = x2 - y2 + cRe
                        x2 = x * x
                        y2 = y * y
                        it++
                    }
                    val smooth = if (it >= iter) it.toDouble() else {
                        val logZn = ln(x2 + y2) / 2.0
                        val nu = ln(logZn / ln(2.0)) / ln(2.0)
                        it + 1 - nu
                    }
                    val col = paletteColor(smooth, iter)
                    pixels[py * w + px] = android.graphics.Color.argb(
                        255,
                        (col.red * 255).toInt(),
                        (col.green * 255).toInt(),
                        (col.blue * 255).toInt()
                    )
                }
            }
            if (myToken != renderToken) return@launch
            val androidBmp = android.graphics.Bitmap.createBitmap(pixels, w, h, android.graphics.Bitmap.Config.ARGB_8888)
            withContext(Dispatchers.Main) {
                if (myToken == renderToken) {
                    bitmap = androidBmp.asImageBitmap()
                    isRendering = false
                }
            }
        }
    }

    // Fires immediately on every camera value change — no delay, no preview step.
    LaunchedEffect(camX.value, camY.value, camLogZoom.value, screenW, screenH) {
        render()
    }

    LaunchedEffect(autoTour, screenW, screenH) {
        if (!autoTour || screenW == 0) return@LaunchedEffect
        while (autoTour) {
            val stop = TOUR_STOPS[tourIndex % TOUR_STOPS.size]
            val flightSpec = tween<Float>(durationMillis = 5200, easing = FastOutSlowInEasing)

            coroutineScope {
                launch { camX.animateTo(stop.cx, flightSpec) }
                launch { camY.animateTo(stop.cy, flightSpec) }
                launch { camLogZoom.animateTo(log10(stop.zoom), flightSpec) }
            }
            if (!autoTour) break
            delay(1800)
            tourIndex = (tourIndex + 1) % TOUR_STOPS.size
        }
    }

    fun stopTour() {
        if (autoTour) {
            autoTour = false
            scope.launch { camX.stop() }
            scope.launch { camY.stop() }
            scope.launch { camLogZoom.stop() }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF060608))
            .onSizeChanged { size -> screenW = size.width; screenH = size.height }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    stopTour()

                    val velocityTracker = VelocityTracker()
                    var zoomedOrPanned = false

                    do {
                        val event = awaitPointerEvent()
                        val pan = event.calculatePan()
                        val gestureZoom = event.calculateZoom()
                        val centroid = event.calculateCentroid()

                        if (pan != Offset.Zero || gestureZoom != 1f) {
                            zoomedOrPanned = true
                            val aspect = screenW.toFloat() / screenH
                            val span = 3.5f / zoom

                            // Pan: applied instantly via snapTo, which immediately triggers
                            // the LaunchedEffect above -> immediate re-render, every frame.
                            scope.launch {
                                camX.snapTo(camX.value - pan.x / screenW * span * aspect)
                                camY.snapTo(camY.value - pan.y / screenH * span)
                            }
                            if (gestureZoom != 1f) {
                                val beforeCx = camX.value + (centroid.x / screenW - 0.5f) * span * aspect
                                val beforeCy = camY.value + (centroid.y / screenH - 0.5f) * span
                                scope.launch {
                                    camLogZoom.snapTo(camLogZoom.value + log10(gestureZoom))
                                    val newSpan = 3.5f / zoom
                                    camX.snapTo(beforeCx - (centroid.x / screenW - 0.5f) * newSpan * aspect)
                                    camY.snapTo(beforeCy - (centroid.y / screenH - 0.5f) * newSpan)
                                }
                            }
                        }
                        event.changes.forEach { velocityTracker.addPosition(it.uptimeMillis, it.position) }
                    } while (event.changes.any { it.pressed })

                    if (zoomedOrPanned) {
                        val velocity = velocityTracker.calculateVelocity()
                        if (hypot(velocity.x, velocity.y) > 400f) {
                            val aspect = screenW.toFloat() / screenH
                            val span = 3.5f / zoom
                            val decay = splineBasedDecay<Float>(this@pointerInput)

                            scope.launch {
                                val flingX = Animatable(0f)
                                var prev = 0f
                                flingX.animateDecay(velocity.x, decay) {
                                    val step = this.value - prev
                                    prev = this.value
                                    scope.launch { camX.snapTo(camX.value - step / screenW * span * aspect) }
                                }
                            }
                            scope.launch {
                                val flingY = Animatable(0f)
                                var prev = 0f
                                flingY.animateDecay(velocity.y, decay) {
                                    val step = this.value - prev
                                    prev = this.value
                                    scope.launch { camY.snapTo(camY.value - step / screenH * span) }
                                }
                            }
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (!autoTour) autoTour = true
                    },
                    onDoubleTap = { offset ->
                        stopTour()
                        val aspect = screenW.toFloat() / screenH
                        val span = 3.5f / zoom
                        val targetCx = camX.value + (offset.x / screenW - 0.5f) * span * aspect
                        val targetCy = camY.value + (offset.y / screenH - 0.5f) * span
                        val spec = tween<Float>(durationMillis = 650, easing = FastOutSlowInEasing)
                        scope.launch {
                            launch { camX.animateTo(targetCx, spec) }
                            launch { camY.animateTo(targetCy, spec) }
                            launch { camLogZoom.animateTo(camLogZoom.value + log10(2.4f), spec) }
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            bitmap?.let { bmp ->
                drawImage(
                    image = bmp,
                    dstSize = IntSize(size.width.toInt(), size.height.toInt())
                )
            }
            if (isRendering) {
                drawRect(
                    color = Color(0xFF7CE0FF).copy(alpha = 0.9f),
                    topLeft = Offset(0f, 0f),
                    size = Size(3.dp.toPx(), size.height)
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(20.dp)
        ) {
            Text(
                text = "MANDELBROT",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
                color = Color.White.copy(alpha = 0.92f)
            )
            Text(
                text = "z\u00B2 + c",
                fontSize = 11.sp,
                color = Color(0xFF7CE0FF).copy(alpha = 0.8f)
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(20.dp)
        ) {
            Text(
                text = formatZoom(zoom),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFB9FFCB)
            )
            Text(
                text = if (autoTour) tourLabel else "manual",
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.45f)
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf("drag \u00B7 pan", "pinch \u00B7 zoom", "double-tap \u00B7 dive").forEach {
                Text(text = it, fontSize = 9.sp, color = Color.White.copy(alpha = 0.35f))
            }
        }

        Text(
            text = if (autoTour) "\u25CF touring" else "\u25CB tap to resume tour",
            fontSize = 10.sp,
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        )
    }
}