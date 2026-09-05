package com.setubandhtech.jetpack_compose_animations.growingplant

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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*
import kotlin.random.Random

private class BranchBone(
    val parentIndex: Int,
    val restAngle: Float,
    val length: Float,
    val depth: Int,
    val baseWidth: Float,
    val tipWidth: Float,
    val color: Color,
    val startFrac: Float,
    val growFrac: Float,
    val curlBias: Float,
    val turbPhase: Float = Random.nextFloat() * 2f * PI.toFloat(),
    val flexibility: Float
)

private class LeafAttachment(
    val boneIndex: Int,
    val t: Float,
    val localAngle: Float,
    val size: Float,
    val color: Color,
    val startFrac: Float,
    val growFrac: Float,
    val flutterPhase: Float = Random.nextFloat() * 2f * PI.toFloat(),
    val curl: Float = Random.nextFloat() * 0.5f + 0.7f
)

private class FlowerAttachment(
    val boneIndex: Int,
    val size: Float,
    val startFrac: Float,
    val growFrac: Float,
    val color: Color,
    val petalCount: Int = Random.nextInt(5, 9),
    val flutterPhase: Float = Random.nextFloat() * 2f * PI.toFloat()
)

private data class Particle(
    var x: Float,
    var y: Float,
    var vy: Float,
    var vx: Float,
    var life: Float,
    val color: Color,
    val size: Float,
    var rot: Float = 0f
)

private class BoneWorldState(
    val baseX: Float, val baseY: Float,
    val tipX: Float, val tipY: Float,
    val worldAngle: Float
)

private fun growthEaseRaw(t: Float): Float {
    if (t <= 0f) return 0f
    if (t >= 1f) return 1f
    val c1 = 1.15f
    val c3 = c1 + 1f
    val x = t - 1f
    return 1f + c3 * x.pow(3) + c1 * x.pow(2)
}

private fun localRawProgress(globalT: Float, startFrac: Float, growFrac: Float): Float {
    if (globalT <= startFrac) return 0f
    val raw = ((globalT - startFrac) / growFrac).coerceIn(0f, 1f)
    return growthEaseRaw(raw)
}

private fun localSafeProgress(globalT: Float, startFrac: Float, growFrac: Float): Float =
    localRawProgress(globalT, startFrac, growFrac).coerceIn(0f, 1f)

@Composable
fun GrowingPlant() {
    var bones by remember { mutableStateOf<List<BranchBone>>(emptyList()) }
    var leafAttachments by remember { mutableStateOf<List<LeafAttachment>>(emptyList()) }
    var flowerAttachments by remember { mutableStateOf<List<FlowerAttachment>>(emptyList()) }
    var particles by remember { mutableStateOf<List<Particle>>(emptyList()) }
    var growthComplete by remember { mutableStateOf(false) }
    var rootX by remember { mutableStateOf(0f) }
    var rootY by remember { mutableStateOf(0f) }
    var screenH by remember { mutableStateOf(0f) }

    val growth = remember { Animatable(0f) }

    val windTarget = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            val next = (Random.nextFloat() - 0.5f) * 2f
            windTarget.animateTo(
                targetValue = next,
                animationSpec = tween(
                    durationMillis = 1800 + Random.nextInt(1400),
                    easing = FastOutSlowInEasing
                )
            )
        }
    }
    val windSpring = remember { Animatable(0f) }
    LaunchedEffect(windTarget.value) {
        windSpring.animateTo(
            targetValue = windTarget.value,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessVeryLow
            )
        )
    }

    val turbulenceTime by rememberInfiniteTransition(label = "turb")
        .animateFloat(
            initialValue = 0f,
            targetValue = 2f * PI.toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(2600, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "turb"
        )

    val glowPulse by rememberInfiniteTransition(label = "glow")
        .animateFloat(
            initialValue = 0.65f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glow"
        )

    val sparkleTime by rememberInfiniteTransition(label = "sparkle")
        .animateFloat(
            initialValue = 0f,
            targetValue = 2f * PI.toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(4000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "sparkle"
        )


    fun generatePlant(baseHeight: Float) {
        val newBones = mutableListOf<BranchBone>()
        val newLeaves = mutableListOf<LeafAttachment>()
        val newFlowers = mutableListOf<FlowerAttachment>()
        val maxDepth = 5

        fun addBranch(
            parentIndex: Int,
            angleRelativeToParent: Float,
            length: Float,
            depth: Int,
            parentStartFrac: Float
        ) {
            if (depth > maxDepth || length < 14f) return

            val branchColor = when (depth) {
                0 -> Color(0xFF4A3222)
                1 -> Color(0xFF5D4037)
                2 -> Color(0xFF6D4C41)
                3 -> Color(0xFF795548)
                else -> Color(0xFF8D6E63)
            }

            val startFrac = (parentStartFrac + 0.05f).coerceIn(0f, 0.85f)
            val growFrac = (0.22f - depth * 0.02f).coerceAtLeast(0.10f)
            val baseWidth = (7.5f - depth * 1.1f).coerceAtLeast(0.9f)
            val tipWidth = (baseWidth * 0.55f).coerceAtLeast(0.6f)
            val flexibility = depth / (depth + 2f)
            val curlBias = (Random.nextFloat() - 0.5f) * 0.35f

            val myIndex = newBones.size
            newBones.add(
                BranchBone(
                    parentIndex = parentIndex,
                    restAngle = angleRelativeToParent,
                    length = length,
                    depth = depth,
                    baseWidth = baseWidth,
                    tipWidth = tipWidth,
                    color = branchColor,
                    startFrac = startFrac,
                    growFrac = growFrac,
                    curlBias = curlBias,
                    flexibility = flexibility
                )
            )

            val branchEndFrac = startFrac + growFrac

            if (depth >= 2) {
                val leafCount = Random.nextInt(2, 5)
                repeat(leafCount) {
                    val t = 0.35f + Random.nextFloat() * 0.65f
                    val localAngle = (if (Random.nextBoolean()) 1 else -1) *
                            (PI.toFloat() / 3.2f + Random.nextFloat() * 0.6f)
                    val leafSize = (22f - depth * 2.2f).coerceAtLeast(9f) * (0.8f + Random.nextFloat() * 0.4f)
                    val greenShade = Random.nextFloat()
                    val leafColor = Color(
                        red = 0.08f + greenShade * 0.14f,
                        green = 0.42f + greenShade * 0.34f,
                        blue = 0.09f + greenShade * 0.10f
                    )
                    newLeaves.add(
                        LeafAttachment(
                            boneIndex = myIndex,
                            t = t,
                            localAngle = localAngle,
                            size = leafSize,
                            color = leafColor,
                            startFrac = (branchEndFrac * t).coerceIn(0f, 0.9f),
                            growFrac = 0.14f + Random.nextFloat() * 0.06f
                        )
                    )
                }
            }

            if (depth == maxDepth - 1 || (depth == maxDepth - 2 && Random.nextFloat() < 0.35f)) {
                val flowerColors = listOf(
                    Color(0xFFFF80AB), Color(0xFFEA80FC), Color(0xFFFFD740),
                    Color(0xFFFF6D00), Color(0xFFFF4081), Color(0xFFFFF8E1)
                )
                newFlowers.add(
                    FlowerAttachment(
                        boneIndex = myIndex,
                        size = 12f + Random.nextFloat() * 9f,
                        startFrac = (branchEndFrac + 0.06f).coerceIn(0f, 0.92f),
                        growFrac = 0.16f,
                        color = flowerColors.random()
                    )
                )
            }

            val spreadAngle = PI.toFloat() / 4.3f + Random.nextFloat() * 0.3f
            val lengthRatio = 0.64f + Random.nextFloat() * 0.10f

            addBranch(myIndex, -spreadAngle + Random.nextFloat() * 0.25f, length * lengthRatio, depth + 1, branchEndFrac)
            addBranch(myIndex, spreadAngle - Random.nextFloat() * 0.25f, length * (lengthRatio - 0.04f), depth + 1, branchEndFrac)
            if (depth < 2) {
                addBranch(myIndex, Random.nextFloat() * 0.35f - 0.17f, length * (lengthRatio - 0.08f), depth + 1, branchEndFrac)
            }
        }

        addBranch(-1, 0f, baseHeight * 0.26f, 0, 0f)

        bones = newBones
        leafAttachments = newLeaves
        flowerAttachments = newFlowers
        particles = emptyList()
    }

    LaunchedEffect(bones.size) {
        if (bones.isEmpty()) return@LaunchedEffect
        growthComplete = false
        growth.snapTo(0f)
        growth.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 3400, easing = LinearEasing)
        )
        growthComplete = true
    }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { }
            particles = particles.mapNotNull { p ->
                val newLife = p.life - 0.014f
                if (newLife <= 0f) null
                else p.copy(
                    x = p.x + p.vx + sin(p.rot) * 0.4f,
                    y = p.y + p.vy,
                    vy = p.vy - 0.22f,
                    rot = p.rot + 0.08f,
                    life = newLife
                )
            }.let { if (it.size > 120) it.takeLast(120) else it } // hard cap
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0A1628), Color(0xFF16233F), Color(0xFF0D1F0D))
                )
            )
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    rootX = offset.x
                    rootY = screenH * 0.85f
                    generatePlant(screenH)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            screenH = size.height
            if (rootX == 0f) rootX = size.width / 2f
            val t = growth.value
            val wind = windSpring.value // -1..1, root gust bias

            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1B5E20).copy(alpha = 0.85f),
                        Color(0xFF2E7D32).copy(alpha = 0.55f),
                        Color.Transparent
                    ),
                    startY = size.height * 0.82f,
                    endY = size.height
                ),
                topLeft = Offset(0f, size.height * 0.82f),
                size = Size(size.width, size.height * 0.18f)
            )
            drawRect(
                color = Color(0xFF69F0AE).copy(alpha = 0.12f),
                topLeft = Offset(0f, size.height * 0.825f),
                size = Size(size.width, 2.5f)
            )

            if (bones.isEmpty()) return@Canvas

            val boneStates = arrayOfNulls<BoneWorldState>(bones.size)

            for (i in bones.indices) {
                val bone = bones[i]
                val growP = localRawProgress(t, bone.startFrac, bone.growFrac) // may overshoot slightly, OK for length/angle
                val safeGrow = localSafeProgress(t, bone.startFrac, bone.growFrac)
                if (safeGrow <= 0f) { boneStates[i] = null; continue }

                val parentState = if (bone.parentIndex >= 0) boneStates[bone.parentIndex] else null
                val originX = parentState?.tipX ?: rootX
                val originY = parentState?.tipY ?: rootY
                val parentWorldAngle = parentState?.worldAngle ?: 0f

                val turb = sin(turbulenceTime + bone.turbPhase) * 0.06f * bone.flexibility
                val windAngle = wind * 0.35f * bone.flexibility + turb
                val growSwing = (1f - safeGrow) * 0.25f // young shoots droop/curl slightly more before fully grown

                val worldAngle = parentWorldAngle + bone.restAngle + bone.curlBias * safeGrow + windAngle + growSwing

                val currentLength = bone.length * growP.coerceAtLeast(0f)

                val tipX = originX + sin(worldAngle) * currentLength
                val tipY = originY - cos(worldAngle) * currentLength

                boneStates[i] = BoneWorldState(originX, originY, tipX, tipY, worldAngle)
            }

            val rootGrow = localSafeProgress(t, bones[0].startFrac, bones[0].growFrac)
            if (rootGrow > 0f) {
                drawOval(
                    color = Color.Black.copy(alpha = 0.35f * rootGrow),
                    topLeft = Offset(rootX - 34f, rootY - 6f),
                    size = Size(68f, 14f)
                )
            }

            for (i in bones.indices) {
                val bone = bones[i]
                val state = boneStates[i] ?: continue
                val safeGrow = localSafeProgress(t, bone.startFrac, bone.growFrac)
                if (safeGrow <= 0f) continue

                val nx = -cos(state.worldAngle)
                val ny = -sin(state.worldAngle)
                val bowSign = if (bone.curlBias >= 0f) 1f else -1f
                val bowAmount = bone.length * 0.10f * bowSign
                val midX = (state.baseX + state.tipX) / 2f + nx * bowAmount
                val midY = (state.baseY + state.tipY) / 2f + ny * bowAmount

                val path = Path().apply {
                    moveTo(state.baseX, state.baseY)
                    quadraticTo(midX, midY, state.tipX, state.tipY)
                }

                val currentWidth = bone.baseWidth - (bone.baseWidth - bone.tipWidth) * safeGrow

                translate(left = 3f, top = 3f) {
                    drawPath(
                        path = path,
                        color = Color.Black.copy(alpha = 0.25f),
                        style = Stroke(width = currentWidth, cap = StrokeCap.Round)
                    )
                }

                drawPath(
                    path = path,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            bone.color,
                            bone.color.copy(
                                red = (bone.color.red * 1.25f).coerceAtMost(1f),
                                green = (bone.color.green * 1.2f).coerceAtMost(1f)
                            )
                        ),
                        start = Offset(state.baseX, state.baseY),
                        end = Offset(state.tipX, state.tipY)
                    ),
                    style = Stroke(width = currentWidth, cap = StrokeCap.Round)
                )

                if (bone.depth <= 1 && safeGrow > 0.3f) {
                    drawPath(
                        path = path,
                        color = Color.White.copy(alpha = 0.06f),
                        style = Stroke(width = currentWidth * 0.3f, cap = StrokeCap.Round)
                    )
                }
            }

            for (leaf in leafAttachments) {
                val state = boneStates[leaf.boneIndex] ?: continue
                val safeGrow = localSafeProgress(t, leaf.startFrac, leaf.growFrac)
                if (safeGrow <= 0f) continue

                val lx = state.baseX + (state.tipX - state.baseX) * leaf.t
                val ly = state.baseY + (state.tipY - state.baseY) * leaf.t

                val flutter = sin(turbulenceTime * 1.6f + leaf.flutterPhase) * 0.09f
                val leafAngle = state.worldAngle + leaf.localAngle + flutter

                val tipX = lx + sin(leafAngle) * leaf.size * safeGrow
                val tipY = ly - cos(leafAngle) * leaf.size * safeGrow
                val spread = leaf.size * leaf.curl * safeGrow
                val ctrl1X = lx + sin(leafAngle - 0.55f) * spread
                val ctrl1Y = ly - cos(leafAngle - 0.55f) * spread
                val ctrl2X = lx + sin(leafAngle + 0.55f) * spread
                val ctrl2Y = ly - cos(leafAngle + 0.55f) * spread

                val leafPath = Path().apply {
                    moveTo(lx, ly)
                    cubicTo(ctrl1X, ctrl1Y, tipX, tipY, tipX, tipY)
                    cubicTo(tipX, tipY, ctrl2X, ctrl2Y, lx, ly)
                    close()
                }

                drawPath(path = leafPath, color = Color.Black.copy(alpha = 0.12f * safeGrow))
                drawPath(
                    path = leafPath,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            leaf.color.copy(alpha = 0.95f * safeGrow),
                            leaf.color.copy(alpha = 0.55f * safeGrow)
                        ),
                        start = Offset(lx, ly),
                        end = Offset(tipX, tipY)
                    )
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.22f * safeGrow),
                    start = Offset(lx, ly),
                    end = Offset(
                        lx + sin(leafAngle) * leaf.size * 0.85f * safeGrow,
                        ly - cos(leafAngle) * leaf.size * 0.85f * safeGrow
                    ),
                    strokeWidth = 0.9f
                )
            }

            for (flower in flowerAttachments) {
                val state = boneStates[flower.boneIndex] ?: continue
                val safeGrow = localSafeProgress(t, flower.startFrac, flower.growFrac)
                if (safeGrow <= 0f) continue

                val cx = state.tipX
                val cy = state.tipY

                drawCircle(
                    color = flower.color.copy(alpha = 0.22f * glowPulse * safeGrow),
                    radius = flower.size * 2.6f * safeGrow,
                    center = Offset(cx, cy)
                )

                repeat(flower.petalCount) { i ->
                    val petalAngle = (i.toFloat() / flower.petalCount) * 2f * PI.toFloat() +
                            sin(turbulenceTime + flower.flutterPhase) * 0.05f
                    val petalX = cx + cos(petalAngle) * flower.size * 0.85f * safeGrow
                    val petalY = cy + sin(petalAngle) * flower.size * 0.85f * safeGrow

                    val petalPath = Path().apply {
                        moveTo(cx, cy)
                        cubicTo(
                            cx + cos(petalAngle - 0.42f) * flower.size * safeGrow,
                            cy + sin(petalAngle - 0.42f) * flower.size * safeGrow,
                            petalX, petalY, petalX, petalY
                        )
                        cubicTo(
                            petalX, petalY,
                            cx + cos(petalAngle + 0.42f) * flower.size * safeGrow,
                            cy + sin(petalAngle + 0.42f) * flower.size * safeGrow,
                            cx, cy
                        )
                        close()
                    }

                    drawPath(
                        path = petalPath,
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.85f * safeGrow),
                                flower.color.copy(alpha = 0.9f * safeGrow)
                            ),
                            center = Offset(cx, cy),
                            radius = (flower.size * safeGrow).coerceAtLeast(0.01f)
                        )
                    )
                }

                drawCircle(color = Color(0xFFFFD740), radius = flower.size * 0.30f * safeGrow, center = Offset(cx, cy))
                drawCircle(color = Color(0xFFFF8F00), radius = flower.size * 0.17f * safeGrow, center = Offset(cx, cy))

                if (growthComplete && safeGrow >= 0.999f && Random.nextFloat() < 0.01f && particles.size < 120) {
                    val pollenColors = listOf(Color(0xFFFFD740), Color(0xFFFF80AB), Color(0xFFFFFDE7))
                    particles = particles + Particle(
                        x = cx, y = cy,
                        vx = Random.nextFloat() * 1.6f - 0.8f,
                        vy = -(Random.nextFloat() * 1.6f + 0.8f),
                        life = 1f,
                        color = pollenColors.random(),
                        size = Random.nextFloat() * 3.5f + 1.8f
                    )
                }
            }

            for (p in particles) {
                val alpha = p.life.coerceIn(0f, 1f)
                drawCircle(color = p.color.copy(alpha = alpha * 0.85f), radius = p.size * alpha, center = Offset(p.x, p.y))
                drawCircle(color = p.color.copy(alpha = alpha * 0.25f), radius = p.size * 2.2f * alpha, center = Offset(p.x, p.y))
            }

            if (growthComplete) {
                repeat(8) { i ->
                    val ffX = size.width * 0.15f + sin(sparkleTime + i * 1.3f) * size.width * 0.65f
                    val ffY = size.height * 0.25f + cos(sparkleTime * 0.7f + i * 1.7f) * size.height * 0.35f
                    val ffAlpha = (sin(sparkleTime * 2f + i) * 0.5f + 0.5f).coerceIn(0f, 1f)

                    drawCircle(color = Color(0xFFFFFF80).copy(alpha = ffAlpha * 0.85f), radius = 3.5f, center = Offset(ffX, ffY))
                    drawCircle(color = Color(0xFFFFFF80).copy(alpha = ffAlpha * 0.22f), radius = 11f, center = Offset(ffX, ffY))
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "🌿 Growing Plant",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF69F0AE)
                )
                Text(
                    text = "Skeletal Wind Physics • Spring-Damped Gusts • FK Chain",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.35f)
                )
            }

            Text(
                text = when {
                    bones.isEmpty() -> "Tap anywhere to grow 🌱"
                    !growthComplete -> "Growing... 🌿"
                    else -> "Tap again for a new plant 🌸"
                },
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.55f),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}