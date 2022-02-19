package com.dzboot.ovpn.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.dzboot.ovpn.BuildConfig
import com.dzboot.ovpn.R
import com.dzboot.ovpn.activities.MainActivity
import com.dzboot.ovpn.constants.TunnelProtocol
import com.nicadevelop.dnstunnel.Dnstt
import com.nicadevelop.dnstunnel.FastDns
import com.nicadevelop.dnstunnel.SlowDns
import com.nicadevelop.dnstunnel.UdpMode

class DNSTunnelService : Service() {
    companion object {
        private const val LISTEN_HOST = "127.0.0.1"
        private const val LISTEN_PORT = "2323"

        const val EXTRA_HOST = "intent_server"
        const val EXTRA_PORT = "intent_port"
        const val EXTRA_TYPE_CONNECTION = "intent_type_connection"
        const val EXTRA_DNS_TT_RESOLVER = "intent_dns_tt_resolver"
        const val EXTRA_DNS_TT_HOST = "intent_dns_tt_host"
        const val EXTRA_DNS_TT_PUBLIC_KEY = "intent_dns_tt_public_key"
    }

    private val tunnelProtocol = TunnelProtocol()

    private var fastDns: FastDns? = null
    private var slowDns: SlowDns? = null
    private var udpMode: UdpMode? = null
    private var dnsTT: Dnstt? = null

    private var intentServer: String? = ""
    private var intentPort: String? = ""
    private var intentDnsTTResolver: String? = ""
    private var intentDnsTTHost: String? = ""
    private var intentDnsTTPublicKey: String? = ""

    private val mProcessLock = Any()

    private var notificationManager: NotificationManager? = null
    private val notificationChannel = BuildConfig.APPLICATION_ID
    private val notificationId = notificationChannel.hashCode()


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }


    override fun onDestroy() {
        synchronized(mProcessLock) {
            when (tunnelProtocol.getType()) {
                TunnelProtocol.Tunnel.DNS_TT -> dnsTT!!.stop()
                TunnelProtocol.Tunnel.FAST_DNS -> fastDns!!.stop()
                TunnelProtocol.Tunnel.SLOW_DNS -> slowDns!!.stop()
                TunnelProtocol.Tunnel.UDP_MODE -> udpMode!!.stop()
                else -> {}
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(notificationId)
        } else {
            notificationManager!!.cancel(notificationId)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        getSerializableObjects(intent)

        initNotification()
        synchronized(mProcessLock) {
            when (tunnelProtocol.getType()) {
                TunnelProtocol.Tunnel.DNS_TT -> initDnsTT()
                TunnelProtocol.Tunnel.FAST_DNS -> initFastDns()
                TunnelProtocol.Tunnel.SLOW_DNS -> initSlowDns()
                TunnelProtocol.Tunnel.UDP_MODE -> initUdpMode()
                else -> {}
            }
        }
        return START_STICKY
    }

    private fun getSerializableObjects(intent: Intent?) {
        if (intent != null && intent.extras != null) {
            intentServer = getStringObjectFromIntent(intent, EXTRA_HOST)
            intentPort = getStringObjectFromIntent(intent, EXTRA_PORT)
            val connection = getStringObjectFromIntent(intent, EXTRA_TYPE_CONNECTION)
            tunnelProtocol.setType(connection!!)
            intentDnsTTResolver = getStringObjectFromIntent(intent, EXTRA_DNS_TT_RESOLVER)
            intentDnsTTHost = getStringObjectFromIntent(intent, EXTRA_DNS_TT_HOST)
            intentDnsTTPublicKey = getStringObjectFromIntent(intent, EXTRA_DNS_TT_PUBLIC_KEY)
        }
    }

    private fun getStringObjectFromIntent(intent: Intent?, key: String): String? {
        if (intent!!.hasExtra(key)) return intent.extras!!.getString(key)
        return null
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun initNotification() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)

        val notificationBuilder = Notification.Builder(this)
        notificationBuilder.setContentTitle(getString(R.string.app_name))
        notificationBuilder.setContentText(getString(R.string.background_connected))
        notificationBuilder.setOnlyAlertOnce(true)
        notificationBuilder.setContentIntent(pendingIntent)
        notificationBuilder.setOngoing(true)
        notificationBuilder.setSmallIcon(R.drawable.ic_launcher_192)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) notificationBuilder.setChannelId(
            notificationChannel
        )
        val notification = notificationBuilder.notification
        startForeground(notificationId, notification)
    }

    private fun initUdpMode() {
        udpMode = UdpMode(baseContext, intentServer, intentPort, LISTEN_HOST, LISTEN_PORT)
        udpMode!!.start()
    }

    private fun initSlowDns() {
        slowDns = SlowDns(baseContext, intentServer, intentPort, LISTEN_HOST, LISTEN_PORT)
        slowDns!!.start()
    }

    private fun initFastDns() {
        fastDns = FastDns(baseContext, intentServer, intentPort, LISTEN_HOST, LISTEN_PORT)
        fastDns!!.start()
    }

    private fun initDnsTT() {
        dnsTT = Dnstt(
            baseContext,
            getDnsTTResolver(),
            intentDnsTTHost,
            intentDnsTTPublicKey,
            intentServer,
            LISTEN_HOST,
            LISTEN_PORT
        )
        dnsTT!!.start()
    }

    private fun getDnsTTResolver(): Dnstt.Resolver {
        if (intentDnsTTResolver == "DOT") {
            return Dnstt.Resolver.DOT
        } else if (intentDnsTTResolver == "DOH") {
            return Dnstt.Resolver.DOH
        }
        return Dnstt.Resolver.UDP
    }
}