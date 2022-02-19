package com.dzboot.ovpn.activities

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import androidx.fragment.app.Fragment
import com.android.billingclient.api.BillingResult
import com.dzboot.ovpn.BuildConfig
import com.dzboot.ovpn.R
import com.dzboot.ovpn.base.BaseActivity
import com.dzboot.ovpn.base.BaseFragmentInterface
import com.dzboot.ovpn.constants.APP_LICENSE_KEY
import com.dzboot.ovpn.constants.SALT
import com.dzboot.ovpn.databinding.ActivityIntroBinding
import com.dzboot.ovpn.fragments.DisplayFragment
import com.dzboot.ovpn.fragments.FirstLoadFragment
import com.dzboot.ovpn.fragments.IntroFragment
import com.dzboot.ovpn.helpers.*
import com.google.android.vending.licensing.AESObfuscator
import com.google.android.vending.licensing.LicenseChecker
import com.google.android.vending.licensing.LicenseCheckerCallback
import com.google.android.vending.licensing.ServerManagedPolicy
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.ktx.Firebase
import timber.log.Timber


class IntroActivity : BaseActivity<ActivityIntroBinding>() {

    companion object {
        private const val BYPASS_TIMEOUT = 3000L
    }

    private var licenseChecker: LicenseChecker? = null
    private var currentFragment: BaseFragmentInterface = IntroFragment()
    private val firstRun = PrefsHelper.isFirstRun()

    override fun initializeBinding() = ActivityIntroBinding.inflate(layoutInflater)

    private val bypassTimer = object : CountDownTimer(BYPASS_TIMEOUT, BYPASS_TIMEOUT) {
        override fun onTick(millisUntilFinished: Long) {
        }

        override fun onFinish() {
            Timber.d("Timer finished. Starting MainActivity anyway")
            startMainActivity()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Firebase.crashlytics.log("IntroActivity onCreate")
        super.onCreate(savedInstanceState)
   binding.hand.resumeAnimation()
        checkSubscriptions()

        if (firstRun) {
            binding.loadingApp.gone()
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, currentFragment as Fragment, currentFragment.toString())
                .addToBackStack(null)
                .commit()
        } else
            bypassTimer.start()
    }

    private fun checkSubscriptions() {
        Timber.d("Checking subscriptions")
        SubscriptionManager.init(object : SubscriptionManager.InitializationCallback() {
            override fun onInitSuccess() {
                Timber.d("Subscription initialization success")
                SubscriptionManager.getInstance().queryPurchases {
                    Timber.d("queryPurchases success $it")
                    if (it) //subscribed
                        startMainActivity()
                    else
                        AdsManager.instance.getGDPRConsent(this@IntroActivity) { startMainActivity() }
                }
            }

            override fun onInitFail(result: BillingResult) {
                super.onInitFail(result)
                AdsManager.instance.getGDPRConsent(this@IntroActivity) { startMainActivity() }
            }
        })
    }

    private fun startMainActivity() {
        Timber.d("Start MainActivity $firstRun")
        if (isDestroyed || firstRun) {
            //don't start MainActivity now
            return
        }
binding.hand.pauseAnimation()
        bypassTimer.cancel()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    fun changeScreen(newFragment: BaseFragmentInterface) {
        currentFragment = newFragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, currentFragment as Fragment, currentFragment.toString())
            .commit()
    }

    override fun onBackPressed() {
        binding.hand.pauseAnimation()
        if (currentFragment is IntroFragment || currentFragment is FirstLoadFragment)
            finish()
        else
            changeScreen(supportFragmentManager.findFragmentByTag(IntroFragment.STATIC_TAG) as IntroFragment)
    }

    override fun onDestroy() {
        super.onDestroy()
        licenseChecker?.onDestroy()
    }

    fun changeToDisplayFragment(type: String) {
        val fragment = DisplayFragment<IntroActivity>()
        val args = Bundle()
        args.putString(DisplayFragment.TYPE, type)
        fragment.arguments = args
        changeScreen(fragment)
    }
}