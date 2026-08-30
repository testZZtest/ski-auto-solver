package com.ski.autosolver.overlay

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.provider.Settings
import android.view.*
import android.widget.ImageButton
import com.ski.autosolver.R
import com.ski.autosolver.engine.GameConfiguration
import com.ski.autosolver.ui.MainActivity

class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private var button: View? = null

    override fun onCreate() {
        super.onCreate()
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return }
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val b = ImageButton(this).apply {
            setImageResource(R.drawable.ic_floating)
            background = null
            setOnClickListener {
                GameConfiguration.enabled = !GameConfiguration.enabled
                alpha = if (GameConfiguration.enabled) 1f else .65f
            }
            setOnLongClickListener {
                startActivity(Intent(this@OverlayService, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                true
            }
        }
        val type = if (android.os.Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
        val lp = WindowManager.LayoutParams(64, 64, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT)
        lp.gravity = Gravity.TOP or Gravity.END
        lp.x = 18; lp.y = 180

        b.setOnTouchListener(object : View.OnTouchListener {
            var downX = 0f; var downY = 0f; var startX = 0; var startY = 0
            override fun onTouch(v: View, e: MotionEvent): Boolean {
                when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN -> { downX=e.rawX; downY=e.rawY; startX=lp.x; startY=lp.y; return false }
                    MotionEvent.ACTION_MOVE -> { lp.x = startX + (downX - e.rawX).toInt() * -1; lp.y = startY + (e.rawY - downY).toInt(); wm.updateViewLayout(v, lp); return true }
                }
                return false
            }
        })
        wm.addView(b, lp); button = b
    }

    override fun onDestroy() { button?.let { wm.removeView(it) }; button=null; super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
}
