package com.dzboot.ovpn.fragments

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.view.animation.Animation
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieAnimationView
import com.dzboot.ovpn.R
import com.dzboot.ovpn.activities.MainActivity
import com.dzboot.ovpn.activities.ProtocolActivity
import com.dzboot.ovpn.base.BaseApplication.Companion.runningOnTV
import com.dzboot.ovpn.base.BaseFragment
import com.dzboot.ovpn.constants.TunnelProtocol
import com.dzboot.ovpn.custom.ReverseInterpolator
import com.dzboot.ovpn.custom.Timer
import com.dzboot.ovpn.data.db.AppDB
import com.dzboot.ovpn.data.models.Server
import com.dzboot.ovpn.databinding.FragmentMainBinding
import com.dzboot.ovpn.helpers.*
import com.dzboot.ovpn.helpers.AdsManager.Companion.showInterstitialAd
import com.dzboot.ovpn.helpers.AdsManager.Companion.showMoreTimeAd
import com.dzboot.ovpn.helpers.VPNHelper.requestVPNPermission
import com.dzboot.ovpn.services.DNSTunnelService
import com.dzboot.ovpn.services.VPNService
import com.google.android.ads.nativetemplates.NativeTemplateStyle
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.play.core.review.ReviewManagerFactory
import com.skyfishjy.library.RippleBackground
import de.blinkt.openvpn.core.ConnectionStatus
import de.blinkt.openvpn.core.ProfileManager
import de.blinkt.openvpn.core.VpnProfile
import de.blinkt.openvpn.core.VpnStatus
import io.michaelrocks.paranoid.Obfuscate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber


@Obfuscate
class MainFragment : BaseFragment<MainActivity, FragmentMainBinding>(), VpnStatus.StateListener,
    VpnStatus.ByteCountListener, VPNService.UpdateTimerCallback, VPNService.AskPassword {

    companion object {

        const val STATIC_TAG = "MainFragment"
        const val PROMPT_DISCONNECT = "prompt_disconnect"
        private const val SELECTED_SERVER_KEY = "selected_server"
    }

    override val TAG = STATIC_TAG
    override fun getPageTitle() = R.string.app_name
    override fun initializeBinding(): FragmentMainBinding =
        FragmentMainBinding.inflate(requireActivity().layoutInflater)

    //selectedLocation and connectLocation are different in Auto mode
    private var selectedServer: Server = Server.auto()
    private var service: VPNService? = null

    private var isServiceBound = false
    //Meri karistani


    private val reviewManager by lazy { ReviewManagerFactory.create(requireContext()) }
    private val animator = ValueAnimator.ofFloat(0f, 1f)
    private val serversDao by lazy { AppDB.getInstance(requireContext()).serversDao() }

    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, binder: IBinder) {
            service = (binder as VPNService.LocalBinder).service

            service!!.askPassword = this@MainFragment
            if (!SubscriptionManager.getInstance().isSubscribed)
                service!!.updateTimerCallback = this@MainFragment

            VpnStatus.addStateListener(this@MainFragment)
            VpnStatus.addByteCountListener(this@MainFragment)
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            service = null
            service!!.updateTimerCallback = null
        }
    }

    private val startVPNForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            onRequestVPNPermissionResult(result.resultCode)
        }

    private fun onRequestVPNPermissionResult(resultCode: Int) {
        if (resultCode == Activity.RESULT_OK) {
            val intent = Intent(context, DNSTunnelService::class.java)
            val tunnelProtocol = TunnelProtocol()
            tunnelProtocol.setType(TunnelProtocol.Tunnel.FAST_DNS)

            intent.putExtra(DNSTunnelService.EXTRA_TYPE_CONNECTION, tunnelProtocol.getName())
            intent.putExtra(DNSTunnelService.EXTRA_HOST, selectedServer.ip)
            intent.putExtra(DNSTunnelService.EXTRA_PORT, "53")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requireContext().startForegroundService(intent)
            } else {
                requireContext().startService(intent)
            }

            VPNHelper.startVPNIntent<VPNService>(requireContext(), selectedServer)
        } else if (resultCode == Activity.RESULT_CANCELED) {
            // User does not want us to start, so we just vanish
            VpnStatus.updateStateString(
                "USER_VPN_PERMISSION_CANCELLED",
                "",
                R.string.state_user_vpn_permission_cancelled,
                ConnectionStatus.LEVEL_DISCONNECTED
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                DialogHelper.otherVPNAppAlwaysAllowed(requireContext())
        }
    }

    private fun connectBtnClicked() {
    binding.ivStatusBackground.resumeAnimation()

        Timber.d("Connect button clicked")
        if (VpnStatus.isVPNActiveOrConnecting()) {
            Timber.d("Already connected or connecting. Stopping...")
            val intent = Intent(context, DNSTunnelService::class.java)
            requireContext().stopService(intent)
            stopVPN()

            //show rating dialog
            reviewManager.requestReviewFlow().addOnCompleteListener { request ->
                if (isAdded && request.isSuccessful)
                    reviewManager.launchReviewFlow(requireActivity(), request.result)
            }
            return
        }

        //Click app if Ads are not initialized
        if (!SubscriptionManager.getInstance().isSubscribed && !PrefsHelper.getAdsInitialization()) {
            throw RuntimeException()
        }

        if (!Utils.isConnected(requireContext())) {
            Timber.d("Not connected to internet")
            Toast.makeText(requireContext(), R.string.log_no_internet, Toast.LENGTH_SHORT).show()
            return
        }

        //don't show ad first time to connect
        if (PrefsHelper.isFirstConnect())
            PrefsHelper.disableFirstConnect()
        else
            activity?.showInterstitialAd()

        binding.tvLog.setText(R.string.connecting)
        binding.connect?.setText(R.string.connecting)
        startAnim()

        if (requireContext().requestVPNPermission(startVPNForResult)) {
            VpnStatus.updateStateString(
                "USER_VPN_PERMISSION",
                "",
                R.string.state_user_vpn_permission,
                ConnectionStatus.LEVEL_WAITING_FOR_USER_INPUT
            )
        } else
            onRequestVPNPermissionResult(Activity.RESULT_OK)
    }

    //region Fragment lifecycle
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.ivStatusBackground.pauseAnimation()
        binding.ivStatusBackground.visible()
         binding.man?.invisible()
        binding.moreProtocolLay?.setOnClickListener{moreProtocolClicked()}
        binding.connectLayout.setOnClickListener { connectBtnClicked() }

        binding.connect?.setOnClickListener { connectBtnClicked() }
        binding.currentLocationLayout?.setOnClickListener {
            activity?.showInterstitialAd()
            val serversFragment = ServersFragment()
            val args = Bundle()
            args.putSerializable(ServersFragment.CONNECTED_SERVER, service?.connectServer)
            serversFragment.arguments = args
            activity?.changeScreen(serversFragment, false)
        }

        binding.info?.setOnClickListener {
            if (binding.infoLayout.container.isVisible)
                hideInfoLayoutOnNonTV()
            else {
                showInfoLayoutOnNonTV()
            }
        }

        binding.moreTime.setOnClickListener { activity?.showMoreTimeAd { service?.addMoreTime() } }

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            selectedServer = if (savedInstanceState == null) {
                serversDao.getServer(PrefsHelper.getSavedServerId()) ?: Server.auto()
            } else {
                savedInstanceState.getSerializable(SELECTED_SERVER_KEY) as Server
            }
            withContext(Dispatchers.Main) { onServerSelected() }
        }
    }

    private fun moreProtocolClicked() {
     val intent=Intent(requireContext(),ProtocolActivity::class.java)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()

        val intent = Intent(requireContext(), VPNService::class.java)
        requireContext().bindService(intent, connection, Context.BIND_AUTO_CREATE)
        isServiceBound = true

        if (!VpnStatus.isVPNActiveOrConnecting()) {
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                VPNHelper.saveOriginalIP(
                    requireContext()
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()

        if (isServiceBound) {
            requireContext().unbindService(connection)
            isServiceBound = false
        }

        VpnStatus.removeStateListener(this)
        VpnStatus.removeByteCountListener(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(SELECTED_SERVER_KEY, selectedServer)
    }
    //endregion

    //region Functions Related to OpenVPN
    private fun stopVPN() {

        setUIToDisconnected()
        try {
            service?.stopVPN()
        } catch (e: RemoteException) {
            Timber.e(e)
        }
    }

    override fun updateState(
        state: String?,
        logMessage: String?,
        localizedResId: Int,
        level: ConnectionStatus,
        Intent: Intent?
    ) {
        if (isAdded)
            activity?.runOnUiThread {
                if (state == "NOPROCESS") {
                    setUIToDisconnected()
                } else when (VpnStatus.mLastLevel) {
                    ConnectionStatus.LEVEL_CONNECTED -> setUIToConnected()
                    ConnectionStatus.LEVEL_DISCONNECTED -> setUIToDisconnected()
                    ConnectionStatus.LEVEL_INVALID_CERTIFICATE -> {
                        setUIToDisconnected()
                        DialogHelper.invalidCertificate(requireContext())
                    }
                    else -> {
                        //connecting
                    }
                }
            }
    }

    override fun updateByteCount(inTraffic: Long, outTraffic: Long, p2: Long, p3: Long) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            updateTrafficCount(
                inTraffic,
                outTraffic
            )
        }
    }
    //endregion

    //region Update UI
    fun onServerSelected() {
        val newServer = arguments?.get(ServersFragment.NEW_SERVER) as Server?
        if (newServer != null) {
            activity?.showInterstitialAd()
            selectedServer = newServer
            setSelectedServer()
        }

        if (VpnStatus.isVPNActive() && arguments?.getBoolean(PROMPT_DISCONNECT, false) == true) {
            arguments?.remove(PROMPT_DISCONNECT)
            DialogHelper.disconnect(
                requireContext(),
                R.string.apps_using_change_disconnect_alert_message
            ) { stopVPN() }
        }
    }

    private fun setSelectedServer() {
        binding.locationFlag?.setImageDrawable(getDrawableCompat(R.drawable.ic_random)?.let {
            selectedServer.getFlagDrawable(requireContext(), it)
        })
        binding.locationTitle?.text = selectedServer.getLocationName(requireContext())

        with(selectedServer.city) {
            if (isBlank()) {
                binding.locationSubtitle?.visibility = View.GONE
            } else {
                binding.locationSubtitle?.visibility = View.VISIBLE
                binding.locationSubtitle?.text = this
            }
        }
    }

    private fun updateTrafficCount(inTraffic: Long, outTraffic: Long) = with(binding.infoLayout) {
        dataDown.text =
            getString(R.string.data_down, Utils.humanReadableByteCount(inTraffic, false, resources))
        dataUp.text =
            getString(R.string.data_up, Utils.humanReadableByteCount(outTraffic, false, resources))
    }

    fun showNativeAd(nativeAd: NativeAd) {
        binding.nativeAdView?.visible()
        binding.nativeAdView?.setStyles(
            NativeTemplateStyle.Builder().build()
        )
        binding.nativeAdView?.setNativeAd(nativeAd)
    }

    fun hideNativeAd() {
        binding.nativeAdView?.gone()
        try {
            binding.nativeAdView?.destroyNativeAd()
        } catch (ignore: Exception) {
        }
    }

    private fun showInfoLayoutOnNonTV() {
        binding.infoLayout.container.visible()
        binding.info?.setImageResource(R.drawable.ic_baseline_close_24)
        binding.info?.contentDescription = getString(R.string.open_info_description)
    }

    private fun hideInfoLayoutOnNonTV() {
        binding.infoLayout.container.gone()
        binding.info?.setImageResource(R.drawable.ic_baseline_error_24)
        binding.info?.contentDescription = getString(R.string.close_info_description)
    }

    private fun setUIToConnected() {
        if (!isAdded)
            return
       binding.ivStatusBackground.invisible()
        binding.man?.visible()
        binding.ivStatusForeground.invisible()
        binding.man?.resumeAnimation()
       animator.end()
        with(binding.tvLog) {
            setTextColor(getColorCompat(R.color.primary))
            text = getString(
                R.string.connected_to,
                service?.connectServer?.getLocationName(requireContext())
            )
            setCompoundDrawablesRelativeWithIntrinsicBounds(
                null,
                null,
                getDrawableCompat(R.drawable.ic_random)?.let { autoDrawable ->
                    service?.connectServer?.getFlagDrawable(requireContext(), autoDrawable)?.let {
                        Utils.resizeDrawable(requireContext(), it, 24, 20)
                    }
                },
                null
            )
        }

        binding.info?.visible()
        if (runningOnTV)
            binding.infoLayout.container.visible()

        PrefsHelper.getOriginalIP()?.let { ip ->
            binding.infoLayout.originalIp.text = getString(R.string.original_ip, ip)
        }
        binding.infoLayout.newIp.text = getString(R.string.new_ip, service?.connectServer?.ip)
        service?.connectServer?.let { server ->
            if (server.connectedDevices != 0)
                binding.infoLayout.connectedDevices.text =
                    getString(R.string.connected_devices, server.connectedDevices)
        }

        if (SubscriptionManager.getInstance().isSubscribed || service?.connectServer?.freeConnectDuration == 0) {
            binding.timeLeftProgress.gone()
            binding.timeLeft.gone()
            binding.moreTime.gone()
        } else {
            binding.timeLeftProgress.visible()
            binding.timeLeft.visible()
            binding.moreTime.visible()
        }

        binding.connect?.setText(R.string.disconnect)
//        binding.ivStatusBackground.resumeAnimation()
//        binding.ivStatusBackground.background = getDrawableCompat(R.color.white)
        val connectString = getString(R.string.connected)
//        binding.ivStatusBackground.contentDescription = connectString
//        binding.ivStatusForeground.contentDescription = connectString
//        DrawableCompat.setTint(
//            DrawableCompat.wrap(binding.ivStatusForeground.background),
//            getColorCompat(R.color.background)
//        )
    }

    private fun setUIToDisconnected() {
        animator.end()
  //
        binding.ivStatusBackground.visible()
        binding.man?.invisible()
        binding.ivStatusForeground.visible()

        binding.ivStatusBackground.pauseAnimation()
        with(binding.tvLog) {
            setTextColor(getColorCompat(android.R.color.tab_indicator_text))
            setText(R.string.not_connected)
            setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, null, null)
        }

        binding.info?.gone()
        if (runningOnTV)
            binding.infoLayout.container.gone()
        else
            hideInfoLayoutOnNonTV()

        binding.timeLeftProgress.gone()
        binding.moreTime.gone()
        binding.timeLeft.gone()
        binding.connect?.setText(R.string.connect)

        val notConnectedString = getString(R.string.not_connected)
       binding.ivStatusBackground.contentDescription = notConnectedString
       binding.ivStatusForeground.contentDescription = notConnectedString
        binding.ivStatusForeground.setImageResource(R.drawable.ic_key)
//        DrawableCompat.setTint(
//           DrawableCompat.wrap(binding.ivStatusForeground.background),
//            getColorCompat(R.color.gray)
//        )
    }
    //endregion

    private fun startAnim() {
        animator.duration = 1000
        animator.repeatMode = ValueAnimator.REVERSE
        animator.repeatCount = Animation.INFINITE
//        binding.ivStatusBackground.visible()
        val hsv = FloatArray(3)
        val from = FloatArray(3)
        val to = FloatArray(3)
        Color.colorToHSV(ContextCompat.getColor(requireContext(), R.color.background), from)
        Color.colorToHSV(ContextCompat.getColor(requireContext(), R.color.primary), to)

        binding.ivStatusForeground.contentDescription = getString(R.string.connecting)

        val drawable = DrawableCompat.wrap(binding.ivStatusForeground.background)
        animator.addUpdateListener { animation ->
            hsv[0] = (from[0] + to[0] * animation.animatedFraction)
            hsv[1] = (from[1] + to[1] * animation.animatedFraction)
            hsv[2] = (from[2] + to[2] * animation.animatedFraction)
            DrawableCompat.setTint(drawable, Color.HSVToColor(hsv))
        }

        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                animation.removeListener(this)
                animation.duration = 0
                animation.interpolator = ReverseInterpolator()
                animation.start()
            }
        })

        animator.start()
    }

    override fun updateTimeLeft(timer: Timer) {
        if (isAdded) {
            binding.timeLeftProgress.progress = timer.getProgress()
            binding.timeLeft.text = getString(R.string.time_left, timer.getTimeLeftString())
        }
    }

    override fun showPasswordDialog(profile: VpnProfile, onSubmitAuthCreds: () -> Unit) {
        if (!isAdded)
            return

        val entry = EditText(requireContext())

        entry.setSingleLine()
        entry.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        entry.transformationMethod = PasswordTransformationMethod()

        val dialog = AlertDialog.Builder(requireContext())
        dialog.setTitle(getString(R.string.pw_request_dialog_title, getString(R.string.password)))
        dialog.setMessage(getString(R.string.pw_request_dialog_prompt, profile.mName))


        val userPasswordLayout: View = layoutInflater.inflate(R.layout.userpass_layout, null, false)

        val usernameET = userPasswordLayout.findViewById<EditText>(R.id.username)
        val passwordET = userPasswordLayout.findViewById<EditText>(R.id.password)
        val saveCredentialsET = userPasswordLayout.findViewById<CheckBox>(R.id.save_credentials)
        val showPasswordET = userPasswordLayout.findViewById<CheckBox>(R.id.show_password)

        profile.mUsername = PrefsHelper.getSavedUsername()
        profile.mPassword = PrefsHelper.getSavedPassword()
        usernameET.setText(profile.mUsername)
        passwordET.setText(profile.mPassword)
        saveCredentialsET.isChecked = profile.mUsername != ""

        showPasswordET.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            if (isChecked)
                passwordET.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            else
                passwordET.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        dialog.setView(userPasswordLayout)

        dialog.setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
            profile.mUsername = usernameET.text.toString()
            profile.mPassword = passwordET.text.toString()
            if (saveCredentialsET.isChecked)
                PrefsHelper.saveUserCredentials(profile.mUsername, profile.mPassword)
            else
                PrefsHelper.saveUserCredentials("", "")
            ProfileManager.getInstance(requireContext()).saveProfile(profile)

            onSubmitAuthCreds()
        }

        dialog.setNegativeButton(android.R.string.cancel) { _: DialogInterface?, _: Int ->
            VpnStatus.updateStateString(
                "USER_VPN_PASSWORD_CANCELLED",
                "",
                R.string.state_user_vpn_password_cancelled,
                ConnectionStatus.LEVEL_DISCONNECTED
            )
        }

        activity?.runOnUiThread { dialog.show() }
    }
}