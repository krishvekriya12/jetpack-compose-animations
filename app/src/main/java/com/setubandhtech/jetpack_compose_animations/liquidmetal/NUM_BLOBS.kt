package com.setubandhtech.jetpack_compose_animations.liquidmetal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.Bitmap
import java.util.Arrays
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random


private const val SIM_SCALE = 5
private const val DECAY_RATE = 0.0018f
private const val MAX_DROP_RADIUS_CELLS = 4
private const val DRAG_DROP_INTERVAL_MS = 22L
private const val MIN_DRAG_DISTANCE_PX = 2f

private class InkColor(var r: Float, var g: Float, var b: Float)

private class InkGrid(val cols: Int, val rows: Int) {
    val r = FloatArray(cols * rows)
    val g = FloatArray(cols * rows)
    val b = FloatArray(cols * rows)

    val rNext = FloatArray(cols * rows)
    val gNext = FloatArray(cols * rows)
    val bNext = FloatArray(cols * rows)

    val fiberRight = FloatArray(cols * rows)
    val fiberLeft = FloatArray(cols * rows)
    val fiberDown = FloatArray(cols * rows)
    val fiberUp = FloatArray(cols * rows)

    val grain = FloatArray(cols * rows)

    init {
        val noiseScale1 = 0.15f
        val noiseScale2 = 0.4f
        for (y in 0 until rows) {
            for (x in 0 until cols) {
                val i = idx(x, y)
                val n1 = valueNoise(x * noiseScale1, y * noiseScale1)
                val n2 = valueNoise(x * noiseScale2 + 91.7f, y * noiseScale2 + 13.3f)
                fiberRight[i] = 0.6f + valueNoise(x * 0.2f, y * 0.2f + 5f) * 1.0f
                fiberLeft[i] = 0.6f + valueNoise(x * 0.2f + 50f, y * 0.2f) * 1.0f
                fiberDown[i] = 0.6f + valueNoise(x * 0.2f, y * 0.2f + 50f) * 1.0f
                fiberUp[i] = 0.6f + valueNoise(x * 0.2f + 25f, y * 0.2f + 75f) * 1.0f
                grain[i] = 0.88f + ((n1 * 0.7f + n2 * 0.3f) * 0.24f)
            }
        }
    }

    private fun valueNoise(x: Float, y: Float): Float {
        val ix = floor(x).toInt()
        val iy = floor(y).toInt()
        fun hash(hx: Int, hy: Int): Float {
            var h = hx * 374761393 + hy * 668265263
            h = (h xor (h shr 13)) * 1274126177
            return (((h xor (h shr 16)).toLong() and 0xFFFFFFFFL).toFloat() / 0xFFFFFFFFL.toFloat())
        }
        val fx = x - ix
        val fy = y - iy
        val v00 = hash(ix, iy)
        val v10 = hash(ix + 1, iy)
        val v01 = hash(ix, iy + 1)
        val v11 = hash(ix + 1, iy + 1)
        val sx = fx * fx * (3 - 2 * fx)
        val sy = fy * fy * (3 - 2 * fy)
        val a = v00 + (v10 - v00) * sx
        val b2 = v01 + (v11 - v01) * sx
        return a + (b2 - a) * sy
    }

    fun idx(x: Int, y: Int) = y * cols + x

    fun deposit(cx: Int, cy: Int, radiusCells: Int, color: InkColor, strength: Float) {
        val minX = max(0, cx - radiusCells)
        val maxX = min(cols - 1, cx + radiusCells)
        val minY = max(0, cy - radiusCells)
        val maxY = min(rows - 1, cy + radiusCells)
        for (y in minY..maxY) {
            for (x in minX..maxX) {
                val dx = (x - cx).toFloat()
                val dy = (y - cy).toFloat()
                val d2 = dx * dx + dy * dy
                val r2 = (radiusCells * radiusCells).toFloat()
                if (d2 > r2) continue
                val t = (1f - d2 / r2).coerceIn(0f, 1f)
                val falloff = t.pow(1.6f) * (0.75f + Random.nextFloat() * 0.25f)
                val i = idx(x, y)
                r[i] = (r[i] + color.r * strength * falloff).coerceAtMost(1.4f)
                g[i] = (g[i] + color.g * strength * falloff).coerceAtMost(1.4f)
                b[i] = (b[i] + color.b * strength * falloff).coerceAtMost(1.4f)
            }
        }
    }

    fun step() {
        for (y in 0 until rows) {
            for (x in 0 until cols) {
                val i = idx(x, y)
                var sr = r[i]
                var sg = g[i]
                var sb = b[i]

                if (sr > 0.001f || sg > 0.001f || sb > 0.001f) {
                    val conc = (sr + sg + sb) / 3f
                    val baseFlow = 0.10f + conc.coerceIn(0f, 1f) * 0.10f

                    val iRight = if (x < cols - 1) idx(x + 1, y) else -1
                    val iLeft = if (x > 0) idx(x - 1, y) else -1
                    val iDown = if (y < rows - 1) idx(x, y + 1) else -1
                    val iUp = if (y > 0) idx(x, y - 1) else -1

                    if (iRight >= 0) {
                        val flow = baseFlow * fiberRight[i] * (0.85f + Random.nextFloat() * 0.3f)
                        val dr = sr * flow; val dg = sg * flow; val db = sb * flow
                        rNext[iRight] += dr; gNext[iRight] += dg; bNext[iRight] += db
                        sr -= dr * 0.22f; sg -= dg * 0.22f; sb -= db * 0.22f
                    }
                    if (iLeft >= 0) {
                        val flow = baseFlow * fiberLeft[i] * (0.85f + Random.nextFloat() * 0.3f)
                        val dr = sr * flow; val dg = sg * flow; val db = sb * flow
                        rNext[iLeft] += dr; gNext[iLeft] += dg; bNext[iLeft] += db
                        sr -= dr * 0.22f; sg -= dg * 0.22f; sb -= db * 0.22f
                    }
                    if (iDown >= 0) {
                        val flow = baseFlow * fiberDown[i] * (0.85f + Random.nextFloat() * 0.3f)
                        val dr = sr * flow; val dg = sg * flow; val db = sb * flow
                        rNext[iDown] += dr; gNext[iDown] += dg; bNext[iDown] += db
                        sr -= dr * 0.22f; sg -= dg * 0.22f; sb -= db * 0.22f
                    }
                    if (iUp >= 0) {
                        val flow = baseFlow * fiberUp[i] * (0.85f + Random.nextFloat() * 0.3f)
                        val dr = sr * flow; val dg = sg * flow; val db = sb * flow
                        rNext[iUp] += dr; gNext[iUp] += dg; bNext[iUp] += db
                        sr -= dr * 0.22f; sg -= dg * 0.22f; sb -= db * 0.22f
                    }
                }

                rNext[i] += sr * (1f - DECAY_RATE)
                gNext[i] += sg * (1f - DECAY_RATE)
                bNext[i] += sb * (1f - DECAY_RATE)
            }
        }

        System.arraycopy(rNext, 0, r, 0, r.size)
        System.arraycopy(gNext, 0, g, 0, g.size)
        System.arraycopy(bNext, 0, b, 0, b.size)
        Arrays.fill(rNext, 0f)
        Arrays.fill(gNext, 0f)
        Arrays.fill(bNext, 0f)
    }

    fun renderInto(bitmap: Bitmap) {
        val pixels = IntArray(cols * rows)
        for (y in 0 until rows) {
            for (x in 0 until cols) {
                val i = idx(x, y)
                val cr = r[i].coerceIn(0f, 1.4f)
                val cg = g[i].coerceIn(0f, 1.4f)
                val cb = b[i].coerceIn(0f, 1.4f)
                val rawConc = (cr + cg + cb) / 3f

                val coverage = rawConc.coerceIn(0f, 1f).pow(0.55f)
                val saturation = rawConc.coerceIn(0f, 1f).pow(1.7f)

                val iRight = if (x < cols - 1) idx(x + 1, y) else i
                val iDown = if (y < rows - 1) idx(x, y + 1) else i
                val neighborConc = ((r[iRight] + g[iRight] + b[iRight]) +
                        (r[iDown] + g[iDown] + b[iDown])) / 6f
                val gradient = (rawConc - neighborConc).coerceIn(-1f, 1f)
                val rimDarken = if (gradient > 0.015f) gradient * 0.4f else 0f

                val paperR = 0.965f
                val paperG = 0.94f
                val paperB = 0.87f


                val hueR = if (rawConc > 0.001f) cr / (rawConc * 3f).coerceAtLeast(0.001f) else 0f
                val hueG = if (rawConc > 0.001f) cg / (rawConc * 3f).coerceAtLeast(0.001f) else 0f
                val hueB = if (rawConc > 0.001f) cb / (rawConc * 3f).coerceAtLeast(0.001f) else 0f

                var outR = paperR - (paperR - hueR) * coverage * (0.4f + saturation * 0.6f) - rimDarken
                var outG = paperG - (paperG - hueG) * coverage * (0.4f + saturation * 0.6f) - rimDarken
                var outB = paperB - (paperB - hueB) * coverage * (0.4f + saturation * 0.6f) - rimDarken

                val grainAmt = grain[i]
                outR *= grainAmt
                outG *= grainAmt
                outB *= grainAmt

                val ir = (outR.coerceIn(0f, 1f) * 255).roundToInt().coerceIn(0, 255)
                val ig = (outG.coerceIn(0f, 1f) * 255).roundToInt().coerceIn(0, 255)
                val ib = (outB.coerceIn(0f, 1f) * 255).roundToInt().coerceIn(0, 255)
                pixels[i] = (0xFF shl 24) or (ir shl 16) or (ig shl 8) or ib
            }
        }
        bitmap.setPixels(pixels, 0, cols, 0, 0, cols, rows)
    }
}

private fun randomInkColor(): InkColor {
    val palette = listOf(
        InkColor(0.04f, 0.04f, 0.10f),
        InkColor(0.55f, 0.03f, 0.08f),
        InkColor(0.04f, 0.20f, 0.48f),
        InkColor(0.08f, 0.38f, 0.20f),
        InkColor(0.46f, 0.16f, 0.48f),
        InkColor(0.62f, 0.32f, 0.04f),
        InkColor(0.04f, 0.30f, 0.34f)
    )
    return palette[Random.nextInt(palette.size)]
}

@Composable
fun InkBleed() {
    var screenW by remember { mutableStateOf(0) }
    var screenH by remember { mutableStateOf(0) }

    var grid by remember { mutableStateOf<InkGrid?>(null) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var displayBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    val pendingDrops = remember { ConcurrentLinkedQueue<FloatArray>() }

    LaunchedEffect(screenW, screenH) {
        if (screenW <= 0 || screenH <= 0) return@LaunchedEffect
        val cols = (screenW / SIM_SCALE).coerceAtLeast(1)
        val rows = (screenH / SIM_SCALE).coerceAtLeast(1)
        grid = InkGrid(cols, rows)
        bitmap = Bitmap.createBitmap(cols, rows, Bitmap.Config.ARGB_8888)
    }

    LaunchedEffect(grid) {
        val g = grid ?: return@LaunchedEffect
        val bmp = bitmap ?: return@LaunchedEffect
        while (true) {
            withFrameNanos {
                while (true) {
                    val d = pendingDrops.poll() ?: break
                    val cx = (d[0] / SIM_SCALE).roundToInt()
                    val cy = (d[1] / SIM_SCALE).roundToInt()
                    val color = InkColor(d[2], d[3], d[4])
                    g.deposit(cx, cy, MAX_DROP_RADIUS_CELLS, color, d[5])
                }
                g.step()
                g.renderInto(bmp)
                displayBitmap = bmp.asImageBitmap()
            }
        }
    }

    var lastDragDropTime by remember { mutableStateOf(0L) }
    var currentDragColor by remember { mutableStateOf(randomInkColor()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4EFE1))
            .onSizeChanged {
                screenW = it.width
                screenH = it.height
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(pass = PointerEventPass.Initial)
                    currentDragColor = randomInkColor()
                    var lastX = down.position.x
                    var lastY = down.position.y
                    lastDragDropTime = 0L

                    while (true) {
                        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.isEmpty()) break

                        val c = pressed[0]
                        val dx = c.position.x - lastX
                        val dy = c.position.y - lastY
                        val moved = sqrt(dx * dx + dy * dy)

                        val now = System.currentTimeMillis()
                        if (moved >= MIN_DRAG_DISTANCE_PX && now - lastDragDropTime >= DRAG_DROP_INTERVAL_MS) {
                            pendingDrops.add(
                                floatArrayOf(
                                    c.position.x, c.position.y,
                                    currentDragColor.r, currentDragColor.g, currentDragColor.b,
                                    0.55f
                                )
                            )
                            lastDragDropTime = now
                            lastX = c.position.x
                            lastY = c.position.y
                        }
                        pressed.forEach { it.consume() }
                    }
                }
            }
    ) {
        val bmp = displayBitmap
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (bmp != null) {
                drawImage(
                    image = bmp,
                    dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt())
                )
            } else {
                drawRect(color = Color(0xFFF4EFE1))
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(20.dp)
        ) {
            Text(
                text = "✒ INK BLEED",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3A2E20),
                letterSpacing = 3.sp
            )
            Text(
                text = "drag to paint",
                fontSize = 10.sp,
                color = Color(0xFF8A7A60)
            )
        }
    }
}