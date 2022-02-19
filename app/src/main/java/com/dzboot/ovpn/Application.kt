package com.dzboot.ovpn

import android.annotation.TargetApi
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.widget.Toast
import com.dzboot.ovpn.activities.MainActivity
import com.dzboot.ovpn.base.BaseApplication
import com.dzboot.ovpn.constants.Config
import com.dzboot.ovpn.helpers.AdsManager


class Application : BaseApplication<MainActivity>(MainActivity::class.java, false, false) {

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createNotificationChannels()

        if (Config.isNotSet()) {
            Toast.makeText(this, "App not configured", Toast.LENGTH_SHORT).show()
            throw RuntimeException("Config file is not set. Please follow tutorial before running app")
        }

        AdsManager.init(this)
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun createNotificationChannels() {
        val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val name = getString(R.string.app_name)
        val mChannel = NotificationChannel(
            BuildConfig.APPLICATION_ID,
            name, NotificationManager.IMPORTANCE_HIGH
        )

        mChannel.description = name
        mChannel.enableVibration(true)
        mChannel.lightColor = Color.CYAN
        mNotificationManager.createNotificationChannel(mChannel)
    }
}