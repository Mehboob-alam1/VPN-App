package com.dzboot.ovpn.services

import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Toast
import com.dzboot.ovpn.R
import com.dzboot.ovpn.activities.MainActivity
import com.dzboot.ovpn.custom.Timer
import com.dzboot.ovpn.data.db.AppDB
import com.dzboot.ovpn.data.models.Server
import com.dzboot.ovpn.data.remote.NetworkCaller
import com.dzboot.ovpn.helpers.*
import com.google.firebase.installations.FirebaseInstallations
import de.blinkt.openvpn.core.ConnectionStatus
import de.blinkt.openvpn.core.OpenVPNService
import de.blinkt.openvpn.core.VpnProfile
import de.blinkt.openvpn.core.VpnStatus
import de.blinkt.openvpn.custom.DataCleanManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber


class VPNService : OpenVPNService() {

    interface UpdateTimerCallback {
        fun updateTimeLeft(timer: Timer)
    }

    interface AskPassword {
        fun showPasswordDialog(profile: VpnProfile, onSubmitAuthCreds: () -> Unit)
    }

    private val serversDao by lazy { AppDB.getInstance(this).serversDao() }

    private var connectJob: Job? = null
    private var autoConnectBySystem: Boolean = false
    private var serverId: Int = 0
    private val mBinder = LocalBinder()
    private lateinit var selectedServer: Server
    var askPassword: AskPassword? = null
    var updateTimerCallback: UpdateTimerCallback? = null
    var connectServer: Server? = null

    private val timer = object : Timer() {
        override fun onTick(millisLeft: Long) {
            updateNotification(VpnStatus.mLastLevel, millisLeft)
            updateTimerCallback?.updateTimeLeft(this)
        }

        override fun onFinish() {
            stopVPN()
        }
    }

    override fun onBind(intent: Intent?): IBinder = mBinder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("VPNService action=${intent?.action}")

        if (intent == null)
            return START_NOT_STICKY

        serverId = intent.getIntExtra(SELECTED_SERVER_ID_EXTRA, Server.AUTO_ID)
        autoConnectBySystem = intent.getBooleanExtra(IS_AUTO_CONNECT, true)

        when (intent.action) {
            START_SERVICE -> return START_STICKY
            START_SERVICE_STICKY -> return START_REDELIVER_INTENT
            STOP_VPN -> {
                stopVPN()
                return START_NOT_STICKY
            }
            START_VPN -> {
                //don't auto start vpn if not subscribed or Always-ON VPN is enabled and AutoConnect is not
                if (autoConnectBySystem && (!SubscriptionManager.getInstance().isSubscribed)) {
                    stopVPN()
                    return START_NOT_STICKY
                }
            }
            //need to prevent vpn autostart for non subscribed users when VPN Always-On is set in system Settings
            else -> if (!SubscriptionManager.getInstance().isSubscribed) {
                Toast.makeText(this, R.string.subscribed_vpn_always_on_feature, Toast.LENGTH_SHORT).show()
                return START_NOT_STICKY
            }
        }

        DataCleanManager.cleanCache(this)

        startForeground(
            NotificationsHelper.DEFAULT_NOTIFICATION_ID,
            NotificationsHelper.showStatusNotification<MainActivity, VPNService>(
                this,
                ConnectionStatus.LEVEL_PREPARING,
                null
            )
        )

        startConnectProcess()
        return START_STICKY
    }

    private fun startConnectProcess() {
        connectJob = CoroutineScope(Dispatchers.Default).launch {
            VPNHelper.saveOriginalIP(this@VPNService)

            selectedServer = if (autoConnectBySystem) {
                Server.auto()
            } else {
                serversDao.getServer(serverId) ?: Server.auto()
            }

            Timber.d("Connecting to id=${selectedServer.id} and code=${selectedServer.countryCode}")
            if (selectedServer.isAuto()) {
                when (PrefsHelper.getAutoMode()) {
                    Server.DISTANCE -> {
                        Timber.d("Getting nearest server")
                        connectServer = serversDao.getNearestServer()
                        Timber.d("Nearest server found ${connectServer?.id}")
                    }
                    Server.RANDOM -> {
                        connectServer = serversDao.getRandomServer()
                        Timber.d("Connecting to random server ${connectServer?.id}")
                    }
                    else -> try {
                        Timber.d("Getting least loaded server")
                        val bestServersResponse = NetworkCaller.getApi(this@VPNService).getBestServerId()
                        val serverId = bestServersResponse.body()

                        if (bestServersResponse.isSuccessful && serverId != null) {
                            connectServer = serversDao.getServer(serverId)
                            Timber.d("Least loaded server found $serverId")
                        } else
                            throw Exception("Error while loading best server")
                    } catch (e: Exception) {
                        Timber.w(e)
                        Timber.d("Can not fetch best server. Falling back to local database")
                        connectServer = serversDao.getLeastLoadedServer()
                    }
                }
            } else {
                connectServer = selectedServer
            }

            Timber.d("fetchProfileAndStartVPN connectServer=$connectServer")
            VpnStatus.mLastLevel = ConnectionStatus.LEVEL_PREPARING
            connectServer?.let {
                ProfileFetcher.getProfile(this@VPNService, it, object : ProfileFetcher.ConnectCallback {
                    override fun connect() {
                        Timber.d("Profile fetched")
                        checkAuthAndStartVPN(it.getProfileName(), 0)
                    }

                    override fun error(message: String?) {
                        Timber.e(message)
                        CoroutineScope(Dispatchers.Main).launch {
                            Toast.makeText(this@VPNService, R.string.connect_failed, Toast.LENGTH_SHORT).show()
                        }
                        stopVPN()
                    }
                })
            }
        }
    }

    override fun updateNotification(status: ConnectionStatus, timeLeft: Long) {
        NotificationsHelper.showStatusNotification<MainActivity, VPNService>(
            this, status, connectServer ?: selectedServer, timeLeft
        )
    }

    fun addMoreTime() = timer.prolong()

    inner class LocalBinder : Binder() {

        val service: VPNService get() = this@VPNService
    }

    override fun onVPNConnected() {
        logConnect()

        val initialConnectDuration = connectServer?.freeConnectDuration ?: 0
        if (!SubscriptionManager.getInstance().isSubscribed && initialConnectDuration != 0) {
            timer.start(initialConnectDuration)
        }
    }

    override fun onVPNDisconnected() {
        NotificationsHelper.showPersistentNotification(this, MainActivity::class.java)
        logDisconnect()
        timer.cancel()
    }

    override fun askForPW(profile: VpnProfile) {
        askPassword?.showPasswordDialog(profile) { startVPN() }
    }

    private fun logConnect() {
        val profileId = profile?.id ?: return

        Handler(Looper.getMainLooper()).postDelayed({
            if (NetworkCaller.isUsingLocalServer())
                NetworkCaller.getApi(this).logConnect(profileId, "local", "local", selectedServer.isAuto())
                    .enqueue(object : Callback<Void> {
                        override fun onResponse(call: Call<Void>, response: Response<Void>) {
                            if (response.isSuccessful)
                                Timber.d("logConnect success")
                            else {
                                Timber.d("logConnect error ${response.code()}")
                                logConnect()
                            }
                        }

                        override fun onFailure(call: Call<Void>, t: Throwable) {
                            Timber.d("logConnect fail: ${t.message}")

                            //retry until success
                            if (VpnStatus.isVPNActive())
                                logConnect()
                        }
                    })
            else
                PrefsHelper.getOriginalIP()?.let { ip ->
                    FirebaseInstallations.getInstance().id.addOnSuccessListener { firebaseId ->
                        NetworkCaller.getApi(this)
                            .logConnect(profileId, firebaseId, ip, selectedServer.isAuto())
                            .enqueue(object : Callback<Void> {
                                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                                    if (response.isSuccessful)
                                        Timber.d("logConnect success")
                                    else {
                                        Timber.d("logConnect error ${response.code()}")
                                        logConnect()
                                    }
                                }

                                override fun onFailure(call: Call<Void>, t: Throwable) {
                                    Timber.d("logConnect fail: ${t.message}")

                                    //retry until success
                                    if (VpnStatus.isVPNActive())
                                        logConnect()
                                }
                            })
                    }
                }
        }, 1000L) // wait for the connection to stabilize
    }

    private fun logDisconnect() {
        Handler(Looper.getMainLooper()).postDelayed({
            if (NetworkCaller.isUsingLocalServer())
                NetworkCaller.getApi(this).logDisconnect("local")
                    .enqueue(object : Callback<Void> {
                        override fun onResponse(call: Call<Void>, response: Response<Void>) {
                            if (response.isSuccessful)
                                Timber.d("logDisconnect success")
                            else {
                                Timber.d("logDisconnect error ${response.code()}")
                                logDisconnect()
                            }
                        }

                        override fun onFailure(call: Call<Void>, t: Throwable) {
                            Timber.d("logDisconnect fail: ${t.message}")

                            //retry until success
                            if (!VpnStatus.isVPNActive())
                                logDisconnect()
                        }
                    })
            else
                FirebaseInstallations.getInstance().id.addOnSuccessListener {
                    NetworkCaller.getApi(this).logDisconnect(it)
                        .enqueue(object : Callback<Void> {
                            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                                if (response.isSuccessful)
                                    Timber.d("logDisconnect success")
                                else {
                                    Timber.d("logDisconnect error ${response.code()}")
                                    logDisconnect()
                                }
                            }

                            override fun onFailure(call: Call<Void>, t: Throwable) {
                                Timber.d("logDisconnect fail: ${t.message}")

                                //retry until success
                                if (!VpnStatus.isVPNActive())
                                    logDisconnect()
                            }
                        })
                }
        }, 1000L) // wait a second for the connection to stabilize
    }
}