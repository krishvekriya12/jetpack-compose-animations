package com.setubandhtech.jetpack_compose_animations.rotatingglobe

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

// Real geographic coordinates (longitude, latitude) in degrees
val CONTINENT_POINTS = listOf(

    // North America
    Pair(-100f, 50f), Pair(-90f, 45f), Pair(-80f, 40f),
    Pair(-70f, 45f), Pair(-75f, 35f), Pair(-85f, 30f),
    Pair(-95f, 30f), Pair(-105f, 35f), Pair(-115f, 35f),
    Pair(-120f, 40f), Pair(-125f, 45f), Pair(-110f, 50f),
    Pair(-95f, 55f), Pair(-85f, 55f), Pair(-75f, 55f),
    Pair(-65f, 50f), Pair(-60f, 45f), Pair(-130f, 55f),
    Pair(-140f, 60f), Pair(-150f, 62f), Pair(-160f, 65f),
    Pair(-100f, 65f), Pair(-85f, 65f), Pair(-70f, 60f),

    // Greenland
    Pair(-45f, 72f), Pair(-35f, 70f), Pair(-25f, 68f),
    Pair(-50f, 68f), Pair(-55f, 65f), Pair(-40f, 65f),

    // South America
    Pair(-70f, 0f), Pair(-75f, -5f), Pair(-80f, -5f),
    Pair(-75f, -15f), Pair(-65f, -20f), Pair(-60f, -15f),
    Pair(-55f, -10f), Pair(-50f, -5f), Pair(-45f, -5f),
    Pair(-40f, -10f), Pair(-45f, -20f), Pair(-50f, -30f),
    Pair(-55f, -35f), Pair(-60f, -40f), Pair(-65f, -45f),
    Pair(-70f, -50f), Pair(-68f, -55f), Pair(-60f, -5f),

    // Europe
    Pair(10f, 50f), Pair(15f, 50f), Pair(20f, 50f),
    Pair(25f, 50f), Pair(20f, 55f), Pair(15f, 55f),
    Pair(10f, 55f), Pair(5f, 52f), Pair(0f, 50f),
    Pair(-5f, 48f), Pair(2f, 48f), Pair(10f, 45f),
    Pair(15f, 45f), Pair(20f, 45f), Pair(25f, 45f),
    Pair(28f, 48f), Pair(30f, 50f), Pair(25f, 55f),
    Pair(20f, 60f), Pair(15f, 60f), Pair(10f, 60f),
    Pair(5f, 58f), Pair(15f, 65f), Pair(25f, 65f),
    Pair(28f, 70f), Pair(20f, 70f), Pair(10f, 63f),

    // Africa
    Pair(15f, 15f), Pair(20f, 10f), Pair(25f, 5f),
    Pair(30f, 0f), Pair(35f, -5f), Pair(30f, -10f),
    Pair(25f, -15f), Pair(20f, -20f), Pair(18f, -25f),
    Pair(20f, -30f), Pair(25f, -30f), Pair(28f, -25f),
    Pair(30f, -20f), Pair(35f, -15f), Pair(38f, -10f),
    Pair(40f, 0f), Pair(42f, 5f), Pair(40f, 10f),
    Pair(38f, 15f), Pair(35f, 20f), Pair(30f, 20f),
    Pair(25f, 20f), Pair(20f, 20f), Pair(15f, 20f),
    Pair(10f, 15f), Pair(5f, 10f), Pair(0f, 5f),
    Pair(-5f, 5f), Pair(-10f, 10f), Pair(-15f, 15f),
    Pair(-10f, 20f), Pair(-5f, 25f), Pair(0f, 20f),
    Pair(5f, 15f), Pair(10f, 10f), Pair(45f, 10f),

    // Asia
    Pair(60f, 50f), Pair(70f, 50f), Pair(80f, 50f),
    Pair(90f, 50f), Pair(100f, 50f), Pair(110f, 45f),
    Pair(120f, 45f), Pair(125f, 45f), Pair(130f, 40f),
    Pair(120f, 35f), Pair(110f, 35f), Pair(100f, 35f),
    Pair(90f, 30f), Pair(80f, 30f), Pair(75f, 25f),
    Pair(80f, 20f), Pair(85f, 15f), Pair(90f, 20f),
    Pair(95f, 25f), Pair(100f, 20f), Pair(105f, 15f),
    Pair(110f, 20f), Pair(115f, 25f), Pair(120f, 30f),
    Pair(50f, 25f), Pair(55f, 22f), Pair(60f, 25f),
    Pair(65f, 30f), Pair(70f, 35f), Pair(75f, 40f),
    Pair(80f, 45f), Pair(85f, 50f), Pair(90f, 55f),
    Pair(95f, 55f), Pair(100f, 55f), Pair(105f, 55f),
    Pair(110f, 55f), Pair(115f, 55f), Pair(120f, 55f),
    Pair(125f, 50f), Pair(130f, 50f), Pair(135f, 45f),
    Pair(140f, 40f), Pair(135f, 35f), Pair(130f, 35f),
    Pair(60f, 60f), Pair(80f, 60f), Pair(100f, 60f),
    Pair(120f, 60f), Pair(140f, 60f), Pair(160f, 65f),

    // Japan
    Pair(135f, 35f), Pair(138f, 36f), Pair(140f, 38f),
    Pair(141f, 40f), Pair(140f, 42f), Pair(130f, 33f),

    // Indian Subcontinent
    Pair(72f, 22f), Pair(75f, 18f), Pair(78f, 15f),
    Pair(80f, 12f), Pair(77f, 10f), Pair(80f, 25f),
    Pair(85f, 22f), Pair(88f, 22f), Pair(90f, 25f),

    // Southeast Asia
    Pair(100f, 5f), Pair(103f, 2f), Pair(105f, 5f),
    Pair(108f, 10f), Pair(110f, 5f), Pair(115f, 5f),
    Pair(120f, 10f), Pair(125f, 10f),

    // Australia
    Pair(130f, -15f), Pair(135f, -15f), Pair(140f, -15f),
    Pair(145f, -15f), Pair(150f, -20f), Pair(150f, -25f),
    Pair(148f, -30f), Pair(145f, -35f), Pair(140f, -38f),
    Pair(135f, -35f), Pair(130f, -32f), Pair(125f, -30f),
    Pair(120f, -25f), Pair(118f, -20f), Pair(122f, -18f),
    Pair(128f, -15f), Pair(135f, -20f), Pair(140f, -25f),
    Pair(145f, -25f), Pair(148f, -20f), Pair(143f, -15f),

    // New Zealand
    Pair(172f, -40f), Pair(174f, -38f), Pair(175f, -37f),
    Pair(174f, -42f), Pair(172f, -44f), Pair(170f, -46f),

    // Antarctica
    Pair(0f, -75f), Pair(30f, -72f), Pair(60f, -70f),
    Pair(90f, -72f), Pair(120f, -70f), Pair(150f, -72f),
    Pair(180f, -75f), Pair(-30f, -72f), Pair(-60f, -70f),
    Pair(-90f, -72f), Pair(-120f, -70f), Pair(-150f, -72f),

    // Iceland
    Pair(-20f, 65f), Pair(-15f, 65f), Pair(-18f, 63f),

    // UK & Ireland
    Pair(-3f, 53f), Pair(-1f, 52f), Pair(0f, 52f),
    Pair(-8f, 53f), Pair(-6f, 54f),

    // Madagascar
    Pair(47f, -20f), Pair(48f, -18f), Pair(49f, -15f),
    Pair(48f, -22f), Pair(47f, -25f),

    // Sri Lanka
    Pair(81f, 8f), Pair(80f, 7f), Pair(81f, 6f),
)

@Composable
fun RotatingGlobe() {
    val infiniteTransition = rememberInfiniteTransition(label = "globe")

    var manualRotation by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    // Auto rotation — pauses when dragging
    val autoRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "auto_rotation"
    )

    val totalRotation = if (isDragging) manualRotation else autoRotation + manualRotation

    // Atmosphere pulse
    val atmospherePulse by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "atmosphere"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000510)),
        contentAlignment = Alignment.Center
    ) {

        // Stars
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRealisticStars()
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🌍 Earth",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4FC3F7),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Canvas(
                modifier = Modifier
                    .size(320.dp)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { isDragging = true },
                            onDragEnd = { isDragging = false },
                            onDrag = { _, dragAmount ->
                                manualRotation += dragAmount.x * 0.3f
                            }
                        )
                    }
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.width * 0.44f

                drawRealisticGlobe(
                    center = center,
                    radius = radius,
                    rotationDeg = totalRotation,
                    atmospherePulse = atmospherePulse
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (isDragging) "Drag to rotate 🌍" else "Auto rotating • Drag to control",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.4f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Pure Canvas • Real Coordinates • No Library",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.2f),
                letterSpacing = 1.sp
            )
        }
    }
}

fun DrawScope.drawRealisticStars() {
    val random = java.util.Random(42)
    repeat(200) {
        val x = random.nextFloat() * size.width
        val y = random.nextFloat() * size.height
        val starSize = random.nextFloat() * 1.5f + 0.5f
        val alpha = random.nextFloat() * 0.6f + 0.2f
        drawCircle(
            color = Color.White.copy(alpha = alpha),
            radius = starSize,
            center = Offset(x, y)
        )
    }
}

fun DrawScope.drawRealisticGlobe(
    center: Offset,
    radius: Float,
    rotationDeg: Float,
    atmospherePulse: Float
) {
    val rotRad = Math.toRadians(rotationDeg.toDouble()).toFloat()
    val tiltRad = Math.toRadians(23.5).toFloat() // Real Earth axial tilt

    // Atmosphere
    drawCircle(
        color = Color(0xFF0D47A1).copy(alpha = 0.06f * atmospherePulse),
        radius = radius + 30f,
        center = center
    )
    drawCircle(
        color = Color(0xFF1565C0).copy(alpha = 0.10f * atmospherePulse),
        radius = radius + 18f,
        center = center
    )
    drawCircle(
        color = Color(0xFF1976D2).copy(alpha = 0.15f * atmospherePulse),
        radius = radius + 8f,
        center = center
    )

    // Ocean
    drawCircle(
        color = Color(0xFF0C2340),
        radius = radius,
        center = center
    )

    // Grid lines
    val numLines = 12
    val numPoints = 80

    // Latitude lines
    for (lat in -8..8) {
        val latRad = Math.toRadians((lat * 15).toDouble()).toFloat()
        val prevPoints = mutableListOf<Pair<Offset, Float>>()

        for (i in 0..numPoints) {
            val lonRad = (i.toFloat() / numPoints) * 2f * Math.PI.toFloat() + rotRad

            var x = cos(latRad) * cos(lonRad)
            var y = sin(latRad)
            var z = cos(latRad) * sin(lonRad)

            // Apply tilt
            val yT = y * cos(tiltRad) - z * sin(tiltRad)
            val zT = y * sin(tiltRad) + z * cos(tiltRad)
            y = yT; z = zT

            if (z > -0.05f) {
                val sx = center.x + x * radius
                val sy = center.y - y * radius
                val depth = (z + 1f) / 2f
                prevPoints.add(Pair(Offset(sx, sy), depth))
            } else {
                if (prevPoints.size > 1) {
                    for (j in 0 until prevPoints.size - 1) {
                        drawLine(
                            color = Color(0xFF1E88E5).copy(
                                alpha = prevPoints[j].second * 0.15f
                            ),
                            start = prevPoints[j].first,
                            end = prevPoints[j + 1].first,
                            strokeWidth = 0.8f
                        )
                    }
                }
                prevPoints.clear()
            }
        }
        if (prevPoints.size > 1) {
            for (j in 0 until prevPoints.size - 1) {
                drawLine(
                    color = Color(0xFF1E88E5).copy(alpha = prevPoints[j].second * 0.15f),
                    start = prevPoints[j].first,
                    end = prevPoints[j + 1].first,
                    strokeWidth = 0.8f
                )
            }
        }
    }

    // Longitude lines
    for (lon in 0 until numLines) {
        val baseLon = (lon.toFloat() / numLines) * 2f * Math.PI.toFloat() + rotRad
        val prevPoints = mutableListOf<Pair<Offset, Float>>()

        for (i in 0..numPoints) {
            val latRad = (i.toFloat() / numPoints) * Math.PI.toFloat() - Math.PI.toFloat() / 2f

            var x = cos(latRad) * cos(baseLon)
            var y = sin(latRad)
            var z = cos(latRad) * sin(baseLon)

            val yT = y * cos(tiltRad) - z * sin(tiltRad)
            val zT = y * sin(tiltRad) + z * cos(tiltRad)
            y = yT; z = zT

            if (z > -0.05f) {
                val sx = center.x + x * radius
                val sy = center.y - y * radius
                val depth = (z + 1f) / 2f
                prevPoints.add(Pair(Offset(sx, sy), depth))
            } else {
                if (prevPoints.size > 1) {
                    for (j in 0 until prevPoints.size - 1) {
                        drawLine(
                            color = Color(0xFF1E88E5).copy(
                                alpha = prevPoints[j].second * 0.15f
                            ),
                            start = prevPoints[j].first,
                            end = prevPoints[j + 1].first,
                            strokeWidth = 0.8f
                        )
                    }
                }
                prevPoints.clear()
            }
        }
    }

    // Continents
    CONTINENT_POINTS.forEach { (lonDeg, latDeg) ->
        val lonRad = Math.toRadians(lonDeg.toDouble()).toFloat() + rotRad
        val latRad = Math.toRadians(latDeg.toDouble()).toFloat()

        var x = cos(latRad) * cos(lonRad)
        var y = sin(latRad)
        var z = cos(latRad) * sin(lonRad)

        val yT = y * cos(tiltRad) - z * sin(tiltRad)
        val zT = y * sin(tiltRad) + z * cos(tiltRad)
        y = yT; z = zT

        if (z > 0.05f) {
            val sx = center.x + x * radius
            val sy = center.y - y * radius
            val depth = (z + 1f) / 2f
            val dotSize = 3f + depth * 5f

            // Glow
            drawCircle(
                color = Color(0xFF4CAF50).copy(alpha = depth * 0.25f),
                radius = dotSize * 2.5f,
                center = Offset(sx, sy)
            )
            // Land
            drawCircle(
                color = Color(0xFF43A047).copy(alpha = 0.6f + depth * 0.4f),
                radius = dotSize,
                center = Offset(sx, sy)
            )
            // Highlight
            drawCircle(
                color = Color(0xFFA5D6A7).copy(alpha = depth * 0.5f),
                radius = dotSize * 0.4f,
                center = Offset(sx - dotSize * 0.2f, sy - dotSize * 0.2f)
            )
        }
    }

    // Equator line — golden
    val equatorPts = mutableListOf<Pair<Offset, Float>>()
    for (i in 0..200) {
        val lonRad = (i.toFloat() / 200f) * 2f * Math.PI.toFloat() + rotRad
        var x = cos(lonRad)
        var y = 0f
        var z = sin(lonRad)
        val yT = y * cos(tiltRad) - z * sin(tiltRad)
        val zT = y * sin(tiltRad) + z * cos(tiltRad)
        y = yT; z = zT
        if (z > 0f) {
            val sx = center.x + x * radius
            val sy = center.y - y * radius
            equatorPts.add(Pair(Offset(sx, sy), (z + 1f) / 2f))
        }
    }
    for (i in 0 until equatorPts.size - 1) {
        drawLine(
            color = Color(0xFFFFD700).copy(alpha = equatorPts[i].second * 0.5f),
            start = equatorPts[i].first,
            end = equatorPts[i + 1].first,
            strokeWidth = 1.2f
        )
    }

    // Shine
    drawCircle(
        color = Color.White.copy(alpha = 0.06f),
        radius = radius * 0.55f,
        center = Offset(center.x - radius * 0.28f, center.y - radius * 0.28f)
    )

    // Border
    drawCircle(
        color = Color(0xFF42A5F5).copy(alpha = 0.3f),
        radius = radius,
        center = center,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
    )
}