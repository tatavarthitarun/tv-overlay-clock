package com.tatav.tvclock

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.TextView

/**
 * The launcher entry point. Its only job is to start the overlay service
 * (when the overlay permission is granted) and tell the user what to do if
 * it isn't. The clock itself lives in ClockOverlayService, not here.
 */
class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val canDraw = Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
            Settings.canDrawOverlays(this)

        val tv = TextView(this).apply {
            textSize = 22f
            gravity = Gravity.CENTER
            setPadding(80, 80, 80, 80)
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.BLACK)
        }

        if (canDraw) {
            val svc = Intent(this, ClockOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(svc)
            } else {
                startService(svc)
            }
            tv.text = "TV Clock overlay is running.\n\n" +
                "It now shows in the top-right corner over every app.\n" +
                "You can press Home and leave this screen."
        } else {
            tv.text = "Overlay permission is not granted yet.\n\n" +
                "On Android TV this is usually granted from a computer over ADB:\n\n" +
                "adb shell appops set com.tatav.tvclock SYSTEM_ALERT_WINDOW allow\n\n" +
                "Then relaunch this app."
            // Some TVs do expose the settings screen; try to open it (harmless if absent).
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName")
                        )
                    )
                } catch (_: Exception) {
                }
            }
        }
        setContentView(tv)
    }
}
