package com.dzboot.ovpn.helpers

import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.dzboot.ovpn.BuildConfig
import com.dzboot.ovpn.base.BaseApplication
import com.dzboot.ovpn.data.models.Server
import de.blinkt.openvpn.core.VpnProfile
import io.michaelrocks.paranoid.Obfuscate


@Obfuscate
object PrefsHelper {

    private const val FIRST_RUN = "first_run"
    private const val FIRST_CONNECT = "first_connect"
    private const val APPS_NOT_USING = "not_allowed_apps"
    private const val CONNECT_SERVER_ID = "connect_server_id"
    private const val ORIGINAL_IP = "original_ip"
    private const val ADS_INITIALIZED = "setup"
    private const val SAVED_USERNAME_KEY = "save_auth_username"
    private const val SAVED_PASSWORD_KEY = "save_auth_password"
    private const val USE_DEFAULT_DNS = "use_default_dns"
    private const val PRIMARY_DNS = "primary_dns"
    private const val SECONDARY_DNS = "secondary_dns"


    val sp: SharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(BaseApplication.instance) }


    fun isFirstRun() = sp.getBoolean(FIRST_RUN, true)
    fun disableFirstRun() = sp.edit().putBoolean(FIRST_RUN, false).apply()

    fun isFirstConnect() = sp.getBoolean(FIRST_CONNECT, true)
    fun disableFirstConnect() = sp.edit().putBoolean(FIRST_CONNECT, false).apply()

    fun setAllAppsUsing() = sp.edit().putStringSet(APPS_NOT_USING, null).apply()
    fun saveAppsNotUsing(inactiveApps: HashSet<String>) = sp.edit().putStringSet(APPS_NOT_USING, inactiveApps).apply()
    fun getAppsNotUsing(): HashSet<String> = sp.getStringSet(APPS_NOT_USING, HashSet<String>()) as HashSet<String>

    fun getDisplayMode() = sp.getString(BuildConfig.DISPLAY_MODE_KEY, "default")
    fun getLanguage() = sp.getString(BuildConfig.LANG_KEY, "default")
    fun getAutoMode() = sp.getString(BuildConfig.AUTO_MODE_KEY, Server.LOAD)

    fun shouldShowPersistentNotif() = sp.getBoolean(BuildConfig.PERSISTENT_NOTIF_KEY, true)

    fun getSavedServerId() = sp.getInt(CONNECT_SERVER_ID, -1)
    fun saveServer(id: Int) = sp.edit().putInt(CONNECT_SERVER_ID, id).apply()

    fun saveOriginalIP(ip: String?) = sp.edit().putString(ORIGINAL_IP, ip).apply()
    fun getOriginalIP() = sp.getString(ORIGINAL_IP, null)

    fun setAdsInitialized(value: Boolean) = sp.edit().putBoolean(ADS_INITIALIZED, value).apply()
    fun getAdsInitialization() = sp.getBoolean(ADS_INITIALIZED, false)

    fun getSavedUsername() = sp.getString(SAVED_USERNAME_KEY, "") ?: ""
    fun getSavedPassword() = sp.getString(SAVED_PASSWORD_KEY, "") ?: ""

    fun saveUserCredentials(username: String, password: String) =
        sp.edit().putString(SAVED_USERNAME_KEY, username).putString(SAVED_PASSWORD_KEY, password).apply()

    fun notConsentedToPersonalizedAds() = sp.getString("IABTCF_VendorConsents", null) == "0"

    fun isUseDefaultDNS() = sp.getBoolean(USE_DEFAULT_DNS, true)
    fun getPrimaryDNS() = sp.getString(PRIMARY_DNS, VpnProfile.DEFAULT_DNS1)
    fun getSecondaryDNS() = sp.getString(SECONDARY_DNS, VpnProfile.DEFAULT_DNS2)

    fun saveDNSPrefs(useDefault: Boolean, dns1: String, dns2: String) {
        sp.edit()
            .putBoolean(USE_DEFAULT_DNS, useDefault)
            .putString(PRIMARY_DNS, dns1)
            .putString(SECONDARY_DNS, dns2)
            .apply()
    }
}