package com.stripe.android.paymentsheet.example.playground

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.SystemClock
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

internal data class PlaygroundLifecycleResourceUiState(
    val isRunning: Boolean = false,
    val isDestroyed: Boolean = false,
    val startCount: Int = 0,
    val stopCount: Int = 0,
    val frameCount: Int = 0,
    val sessionId: Int = 0,
) {
    fun onStart(): PlaygroundLifecycleResourceUiState {
        return copy(
            isRunning = true,
            isDestroyed = false,
            startCount = startCount + 1,
            frameCount = 0,
            sessionId = sessionId + 1,
        )
    }

    fun onStop(): PlaygroundLifecycleResourceUiState {
        return copy(
            isRunning = false,
            stopCount = stopCount + 1,
        )
    }

    fun onDestroy(): PlaygroundLifecycleResourceUiState {
        return copy(
            isRunning = false,
            isDestroyed = true,
        )
    }
}

@Composable
internal fun PlaygroundLifecycleResourceSection(
    state: PlaygroundLifecycleResourceUiState,
    onViewReady: (PlaygroundLifecycleSurfaceView) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colors.surface,
        elevation = 2.dp,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Lifecycle resource demo",
                style = MaterialTheme.typography.h6,
            )
            Text(
                text = when {
                    state.isDestroyed -> "SURFACE DESTROYED"
                    state.isRunning -> "ACTIVE MEDIA SURFACE"
                    else -> "SURFACE STOPPED"
                },
                color = ComposeColor.White,
                modifier = Modifier
                    .background(
                        color = when {
                            state.isDestroyed -> ComposeColor(0xFF424242)
                            state.isRunning -> ComposeColor(0xFF1B5E20)
                            else -> ComposeColor(0xFF8E2430)
                        },
                        shape = MaterialTheme.shapes.small,
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
            Text(
                text = "Starts ${state.startCount} | Stops ${state.stopCount} | Frames ${state.frameCount} | Destroyed ${state.isDestroyed}",
                style = MaterialTheme.typography.body2,
            )
            Text(
                text = "This activity-owned surface starts in onStart(), tears down in onStop(), and is released in onDestroy().",
                style = MaterialTheme.typography.body2,
            )
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                factory = { context ->
                    PlaygroundLifecycleSurfaceView(context).also(onViewReady)
                },
                update = { view ->
                    onViewReady(view)
                    if (state.isRunning) {
                        view.startRendering()
                    } else {
                        view.stopRendering()
                    }
                }
            )
        }
    }
}

internal class PlaygroundLifecycleSurfaceView(
    context: Context,
) : SurfaceView(context), SurfaceHolder.Callback {
    private val density = resources.displayMetrics.density

    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 22f.dp()
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(230, 255, 255, 255)
        textSize = 14f.dp()
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(140, 96, 225, 163)
        style = Paint.Style.STROKE
        strokeWidth = 2f.dp()
    }
    private val beamPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f.dp()
    }
    private val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(73, 232, 139)
    }

    @Volatile
    private var resourceActive = false

    @Volatile
    private var surfaceAvailable = false

    private var renderThread: Thread? = null
    private var frameCount = 0
    private var renderSessionId = 0

    var onFrameRendered: (Int, Int) -> Unit = { _, _ -> }

    init {
        holder.addCallback(this)
    }

    fun startRendering() {
        if (!resourceActive) {
            renderSessionId += 1
            frameCount = 0
        }
        resourceActive = true
        maybeStartRenderThread()
    }

    fun stopRendering() {
        resourceActive = false
        stopRenderThread()
        frameCount = 0
        if (surfaceAvailable) {
            drawStoppedFrame()
        }
    }

    fun release() {
        stopRendering()
        holder.removeCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        surfaceAvailable = true
        if (resourceActive) {
            maybeStartRenderThread()
        } else {
            drawStoppedFrame()
        }
    }

    override fun surfaceChanged(
        holder: SurfaceHolder,
        format: Int,
        width: Int,
        height: Int,
    ) {
        if (!resourceActive) {
            drawStoppedFrame()
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        surfaceAvailable = false
        stopRenderThread()
    }

    private fun maybeStartRenderThread() {
        if (!resourceActive || !surfaceAvailable) {
            return
        }
        val existingThread = renderThread
        if (existingThread?.isAlive == true) {
            return
        }

        renderThread = Thread {
            while (resourceActive && !Thread.currentThread().isInterrupted) {
                val canvas = runCatching { holder.lockCanvas() }.getOrNull()
                if (canvas == null) {
                    SystemClock.sleep(FRAME_DELAY_MS)
                    continue
                }

                try {
                    drawRunningFrame(canvas)
                } finally {
                    runCatching { holder.unlockCanvasAndPost(canvas) }
                }

                frameCount += 1
                if (frameCount % FRAME_CALLBACK_INTERVAL == 0) {
                    val latestSessionId = renderSessionId
                    val latestFrameCount = frameCount
                    post { onFrameRendered(latestSessionId, latestFrameCount) }
                }

                try {
                    Thread.sleep(FRAME_DELAY_MS)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            }
        }.apply {
            name = "PlaygroundLifecycleSurfaceView"
            start()
        }
    }

    private fun stopRenderThread() {
        val thread = renderThread ?: return
        renderThread = null
        thread.interrupt()
        runCatching { thread.join(200) }
    }

    private fun drawRunningFrame(canvas: Canvas) {
        val width = canvas.width.toFloat()
        val height = canvas.height.toFloat()
        val centerX = width / 2f
        val centerY = height / 2f
        val minDimension = min(width, height)
        val elapsedSeconds = frameCount / FRAME_RATE.toFloat()

        canvas.drawColor(Color.rgb(6, 19, 25))
        borderPaint.color = Color.argb(140, 96, 225, 163)

        for (index in 1 until 6) {
            val y = height * index / 6f
            borderPaint.alpha = 32
            canvas.drawLine(0f, y, width, y, borderPaint)
        }

        val beamWidth = width * 0.18f
        val beamCenter = ((frameCount % 120) / 120f) * (width + beamWidth) - beamWidth / 2f
        beamPaint.color = Color.argb(48, 73, 232, 139)
        canvas.drawRect(
            beamCenter - beamWidth,
            0f,
            beamCenter + beamWidth,
            height,
            beamPaint,
        )
        beamPaint.color = Color.argb(96, 73, 232, 139)
        canvas.drawRect(
            beamCenter - beamWidth / 3f,
            0f,
            beamCenter + beamWidth / 3f,
            height,
            beamPaint,
        )

        repeat(4) { index ->
            val pulse = ((sin(elapsedSeconds * 2.4f + index) + 1f) / 2f)
            ringPaint.color = Color.argb(
                (60 + (110 * pulse)).roundToInt(),
                73,
                232,
                139,
            )
            val radius = minDimension * (0.18f + (index * 0.10f) + (pulse * 0.02f))
            canvas.drawCircle(centerX, centerY, radius, ringPaint)
        }

        val orbitRadius = minDimension * 0.28f
        val angle = elapsedSeconds * 1.8f
        val dotX = centerX + (cos(angle) * orbitRadius).toFloat()
        val dotY = centerY + (sin(angle) * orbitRadius).toFloat()
        canvas.drawCircle(dotX, dotY, 8f.dp(), accentPaint)

        borderPaint.alpha = 140
        canvas.drawRect(0f, 0f, width, height, borderPaint)

        canvas.drawText("RESOURCE ACTIVE", 16f.dp(), 32f.dp(), titlePaint)
        canvas.drawText("Frame $frameCount", 16f.dp(), 54f.dp(), detailPaint)
        canvas.drawText("Synthetic media surface", 16f.dp(), height - 28f.dp(), detailPaint)
        canvas.drawText("Stops when onStop() fires", 16f.dp(), height - 10f.dp(), detailPaint)
    }

    private fun drawStoppedFrame() {
        val canvas = runCatching { holder.lockCanvas() }.getOrNull() ?: return
        try {
            val width = canvas.width.toFloat()
            val height = canvas.height.toFloat()

            canvas.drawColor(Color.rgb(37, 16, 20))
            borderPaint.color = Color.argb(180, 240, 108, 120)
            canvas.drawRect(0f, 0f, width, height, borderPaint)

            canvas.drawText("RESOURCE STOPPED", 16f.dp(), 32f.dp(), titlePaint)
            canvas.drawText("No frames are rendering.", 16f.dp(), 54f.dp(), detailPaint)
            canvas.drawText("Waiting for onStart()", 16f.dp(), height - 10f.dp(), detailPaint)
        } finally {
            runCatching { holder.unlockCanvasAndPost(canvas) }
        }
    }

    private fun Float.dp(): Float {
        return this * density
    }

    private companion object {
        const val FRAME_CALLBACK_INTERVAL = 12
        const val FRAME_DELAY_MS = 33L
        const val FRAME_RATE = 30
    }
}
