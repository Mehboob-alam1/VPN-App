package com.dzboot.ovpn.fragments

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.dzboot.ovpn.BuildConfig
import com.dzboot.ovpn.R
import com.dzboot.ovpn.activities.MainActivity
import com.dzboot.ovpn.base.BaseFragmentInterface
import com.dzboot.ovpn.data.models.Server
import com.dzboot.ovpn.helpers.AdsManager.Companion.resetGDPRConsent
import com.dzboot.ovpn.helpers.AdsManager.Companion.showResetGDPRConsent
import com.dzboot.ovpn.helpers.LocalesHelper
import com.dzboot.ovpn.helpers.NotificationsHelper
import com.dzboot.ovpn.helpers.PrefsHelper
import com.dzboot.ovpn.helpers.ThemeHelper
import com.dzboot.ovpn.helpers.Utils.goToAppInfoPage
import com.dzboot.ovpn.helpers.VPNHelper.openVPNSettings


class PreferencesFragment : PreferenceFragmentCompat(), BaseFragmentInterface,
    SharedPreferences.OnSharedPreferenceChangeListener {

    companion object {

        const val STATIC_TAG = "PreferencesFragment"
    }

    override val TAG = STATIC_TAG
    override fun getPageTitle() = R.string.settings
    override fun toString() = TAG

    private val displayPref by lazy { findPreference<ListPreference>(BuildConfig.DISPLAY_MODE_KEY) }
    private val autoModePref by lazy { findPreference<ListPreference>(BuildConfig.AUTO_MODE_KEY) }


    override fun onResume() {
        super.onResume()
        preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            findPreference<Preference>(BuildConfig.GO_TO_VPN_SETTINGS_KEY)?.isVisible = true
        }

        if ((activity as MainActivity).showResetGDPRConsent()) {
            findPreference<PreferenceCategory>("ads_category")?.isVisible = true
            findPreference<Preference>(BuildConfig.RESET_GDPR_KEY)?.isVisible = true
        }

        val langPref = findPreference<ListPreference>(BuildConfig.LANG_KEY)
        langPref?.entries = LocalesHelper.getLanguagesEntries(requireContext())
        langPref?.entryValues = LocalesHelper.getLanguagesValues()
        langPref?.summary = getString(R.string.lang_summary, LocalesHelper.getDefaultLanguage(requireContext()))

        autoModePref?.entries = Server.getAutoModeEntries(requireContext())
        autoModePref?.entryValues = Server.getAutoModeValues()
        setAutoModeSummary(PrefsHelper.getAutoMode())

        displayPref?.entries = ThemeHelper.getLanguagesEntries(requireContext())
        displayPref?.entryValues = ThemeHelper.getDisplayValues()
        PrefsHelper.getDisplayMode()?.let { setDisplayModeSummary(it) }
    }

    @SuppressLint("NewApi")
    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        when (preference?.key) {
            BuildConfig.RESET_GDPR_KEY -> with(activity as MainActivity) { resetGDPRConsent() }
            BuildConfig.GO_TO_VPN_SETTINGS_KEY -> activity?.openVPNSettings()
            BuildConfig.GO_TO_INFO_PAGE_KEY -> activity?.goToAppInfoPage()
            else -> return super.onPreferenceTreeClick(preference)
        }
        return true
    }

    override fun onSharedPreferenceChanged(sp: SharedPreferences, key: String) {

        when (key) {
            BuildConfig.LANG_KEY -> (activity as MainActivity).updateLocale(
                LocalesHelper.getLocaleFromLanguageCode(sp.getString(BuildConfig.LANG_KEY, "default"))
            )
            BuildConfig.DISPLAY_MODE_KEY -> {
                val theme = sp.getString(BuildConfig.DISPLAY_MODE_KEY, "default")
                setDisplayModeSummary(theme)
                ThemeHelper.applyTheme(theme)
            }
            BuildConfig.PERSISTENT_NOTIF_KEY ->
                if (sp.getBoolean(BuildConfig.PERSISTENT_NOTIF_KEY, true))
                    NotificationsHelper.showPersistentNotification(requireContext(), MainActivity::class.java)
                else
                    NotificationsHelper.cancelPersistentNotification(requireContext())
            BuildConfig.AUTO_MODE_KEY -> setAutoModeSummary(sp.getString(key, Server.LOAD))
        }
    }

    private fun setDisplayModeSummary(themeMode: String?) {
        displayPref?.summary =
            getString(R.string.display_mode_summary, getString(ThemeHelper.getDisplayModeResId(themeMode)))
    }

    private fun setAutoModeSummary(autoMode: String?) {
        autoModePref?.summary =
            getString(R.string.auto_mode_summary, Server.getAutoModeString(requireContext(), autoMode))
    }
}