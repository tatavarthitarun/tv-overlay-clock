package com.tatav.tvclock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A foreground service that draws a live clock in the top-right corner of the
 * screen using a WindowManager overlay, so it stays visible over every app.
 */
class ClockOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var clockView: TextView? = null
    private val handler = Handler(Looper.getMainLooper())

    // 12-hour clock, e.g. "9:41 PM". Change to "HH:mm" for 24-hour.
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    private val tick = object : Runnable {
        override fun run() {
            clockView?.text = timeFormat.format(Date())
            // Re-align to the next whole second so the display stays accurate.
            handler.postDelayed(this, 1000 - System.currentTimeMillis() % 1000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundWithNotification()
        addOverlay()
    }

    private fun addOverlay() {
        if (clockView != null) return // already showing
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager = wm

        val view = TextView(this).apply {
            text = timeFormat.format(Date())
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            setShadowLayer(6f, 0f, 0f, Color.BLACK)
            setBackgroundColor(Color.argb(120, 0, 0, 0)) // semi-transparent pill
            setPadding(dp(10), dp(4), dp(10), dp(4))
        }
        clockView = view

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            // Top-right, nudged in a bit to clear TV overscan.
            gravity = Gravity.TOP or Gravity.END
            x = dp(28)
            y = dp(20)
        }

        wm.addView(view, params)
        handler.post(tick)
    }

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics
        ).toInt()

    private fun startForegroundWithNotification() {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID, "TV Clock Overlay", NotificationManager.IMPORTANCE_LOW
                )
            )
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        val notification = builder
            .setContentTitle("TV Clock is running")
            .setContentText("Showing the clock overlay")
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .build()
        startForeground(NOTIF_ID, notification)
    }

    // Restart if the system kills us to reclaim memory.
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(tick)
        clockView?.let { windowManager?.removeView(it) }
        clockView = null
    }

    override fun onBind(intent: Intent?): IBinder? = null // not a bound service

    companion object {
        private const val CHANNEL_ID = "tvclock_overlay"
        private const val NOTIF_ID = 1
    }
}
