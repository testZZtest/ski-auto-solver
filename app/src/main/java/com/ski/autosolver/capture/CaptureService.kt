package com.ski.autosolver.capture

import android.app.*
import android.content.*
import android.graphics.Bitmap
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import com.ski.autosolver.engine.ActionEngine
import com.ski.autosolver.engine.GameConfiguration
import com.ski.autosolver.model.GameType
import com.ski.autosolver.model.TapAction
import com.ski.autosolver.vision.FrameBus
import com.ski.autosolver.vision.WaterSortVision
import com.ski.autosolver.solver.WaterSortSolver
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class CaptureService : Service() {
    companion object {
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        const val CHANNEL = "ski_capture"
        const val NOTIFICATION_ID = 4001
    }

    private var projection: MediaProjection? = null
    private var display: VirtualDisplay? = null
    private var reader: ImageReader? = null
    private var lastWidth = 0
    private var lastHeight = 0
    private var executor = Executors.newSingleThreadScheduledExecutor()
    private var solverBusy = false

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFICATION_ID, notification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
        val data = intent?.parcelable<Intent>(EXTRA_RESULT_DATA)
        if (resultCode != Activity.RESULT_CANCELED && data != null) startCapture(resultCode, data)
        return START_STICKY
    }

    private fun startCapture(resultCode: Int, data: Intent) {
        stopCapture()
        val metrics = resources.displayMetrics
        lastWidth = metrics.widthPixels
        lastHeight = metrics.heightPixels
        val density = metrics.densityDpi
        reader = ImageReader.newInstance(lastWidth, lastHeight, android.graphics.PixelFormat.RGBA_8888, 2)
        reader!!.setOnImageAvailableListener({ ir ->
            val image = ir.acquireLatestImage() ?: return@setOnImageAvailableListener
            try {
                val plane = image.planes[0]
                val pixelStride = plane.pixelStride
                val rowStride = plane.rowStride
                val rowPadding = rowStride - pixelStride * lastWidth
                val paddedWidth = lastWidth + rowPadding / pixelStride
                val bitmap = Bitmap.createBitmap(paddedWidth, lastHeight, Bitmap.Config.ARGB_8888)
                bitmap.copyPixelsFromBuffer(plane.buffer)
                val clean = if (paddedWidth == lastWidth) bitmap else Bitmap.createBitmap(bitmap, 0, 0, lastWidth, lastHeight)
                if (clean !== bitmap && !bitmap.isRecycled) bitmap.recycle()
                FrameBus.publish(clean)
            } finally { image.close() }
        }, Handler(Looper.getMainLooper()))

        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projection = manager.getMediaProjection(resultCode, data)
        display = projection!!.createVirtualDisplay(
            "SkiAutoSolver", lastWidth, lastHeight, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader!!.surface, null, null
        )
        scheduleSolver()
    }

    private fun scheduleSolver() {
        executor.shutdownNow()
        executor = Executors.newSingleThreadScheduledExecutor()
        executor.scheduleAtFixedRate({ runSolverTick() }, 800L, (1000L / GameConfiguration.frameRateHz.coerceIn(1, 15)), TimeUnit.MILLISECONDS)
    }

    private fun runSolverTick() {
        if (!GameConfiguration.enabled || solverBusy || GameConfiguration.gameType != GameType.WATER_SORT) return
        solverBusy = true
        try {
            val frame = FrameBus.latestCopy() ?: return
            // Tube count is intentionally configurable in the UI in a future revision; 8 is the common first target.
            val detected = WaterSortVision.detect(frame, GameConfiguration.waterTubeCount) ?: return
            val (state, samples) = detected
            if (state.tubes.any { it.size > 4 }) return
            val solution = WaterSortSolver.solve(state) ?: return
            val move = solution.firstOrNull() ?: return
            val a = samples[move.from]; val b = samples[move.to]
            // Deliberately execute ONE move only. The next tick must observe the changed board.
            ActionEngine.execute(listOf(
                TapAction(a.x.toFloat(), a.yBottom.toFloat(), 70L),
                TapAction(b.x.toFloat(), b.yBottom.toFloat(), GameConfiguration.actionDelayMs)
            ))
        } finally { solverBusy = false }
    }

    private fun stopCapture() {
        display?.release(); display = null
        reader?.close(); reader = null
        projection?.stop(); projection = null
        FrameBus.clear()
    }

    override fun onDestroy() {
        stopCapture(); executor.shutdownNow(); super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Ski Auto Solver", NotificationManager.IMPORTANCE_LOW))
    }

    private fun notification(): Notification = Notification.Builder(this, CHANNEL)
        .setContentTitle("Ski Auto Solver")
        .setContentText("Captura de tela ativa")
        .setSmallIcon(android.R.drawable.ic_menu_view)
        .setOngoing(true)
        .build()

    private inline fun <reified T : Parcelable> Intent.parcelable(key: String): T? =
        if (Build.VERSION.SDK_INT >= 33) getParcelableExtra(key, T::class.java) else @Suppress("DEPRECATION") getParcelableExtra(key)
}
