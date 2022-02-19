package com.dzboot.ovpn.helpers

import android.content.Context
import com.dzboot.ovpn.constants.Config
import com.dzboot.ovpn.data.models.Server
import com.dzboot.ovpn.data.remote.NetworkCaller
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import de.blinkt.openvpn.core.Connection
import de.blinkt.openvpn.core.ProfileManager
import de.blinkt.openvpn.core.VpnProfile
import timber.log.Timber
import java.io.InputStreamReader


object ProfileFetcher {

    interface ConnectCallback {
        fun connect()
        fun error(message: String?)
    }

    fun getProfile(context: Context, server: Server, connectCallback: ConnectCallback) {

        Timber.d("getting profile")
        val profile: VpnProfile? = if (server.useFile) {
            val request = Request.Builder()
                .url(NetworkCaller.BASE_URL + "ovpn_files/${server.id}.ovpn")
                .build()

            val response = OkHttpClient().newCall(request).execute()
            if (!response.isSuccessful) {
                connectCallback.error(response.message())
                null
            } else try {
                val configParser = ConfigParser()
                configParser.parseConfig(InputStreamReader(response.body().byteStream()))
                configParser.convertProfile()
            } catch (exception: Exception) {
                Timber.e(exception, "Error parsing profile")
                connectCallback.error("Error parsing profile")
                null
            }
        } else {
            getDefaultProfile(server)
        }

        if (profile == null)
            return

        Timber.d("Profile=$profile")
        profile.apply {
            mName = server.getProfileName()
            id = server.id
            mCountryCode = server.countryCode
            mCity = server.city
            mAllowedAppsVpnAreDisallowed = true
            mAllowedAppsVpn = PrefsHelper.getAppsNotUsing()
            ProfileManager.getInstance(context).saveProfile(this)
        }
        connectCallback.connect()
    }

    private fun getDefaultProfile(server: Server) = with(VpnProfile()) {
        Timber.d("Getting default profile")
        clearDefaults()
        mUsePull = true
        mUseTLSAuth = true
        mTLSAuthDirection = "tls-crypt"
        mCipher = "AES-256-CBC"
        mAuth = "SHA512"
        mAuthenticationType = VpnProfile.TYPE_CERTIFICATES
        mVerb = "3"
        mNobind = true
        mPersistTun = true
        mExpectTLSCert = true
        mUsername = null
        mPassword = null
        mCheckRemoteCN = false
        mUseCustomConfig = true
        mUseDefaultRoute = false
        mUseDefaultRoutev6 = false
        mUseFloat = false
        mUseLzo = false
        mUsePull = false
        mUseRandomHostname = false
        mUserEditable = true
        mUsername = ""
        mExcludedRoutes = "${server.ip}/32"

        if (!PrefsHelper.isUseDefaultDNS()) {
            mOverrideDNS = true
            mDNS1 = PrefsHelper.getPrimaryDNS()
            mDNS2 = PrefsHelper.getSecondaryDNS()
        }

        getConfigValues().let {
            mClientKeyFilename = "[[INLINE]]${it[0]}"
            mCaFilename = "[[INLINE]]${it[1]}"
            mClientCertFilename = "[[INLINE]]${it[2]}"
            mTLSAuthFilename = "[[INLINE]]${it[3]}"
        }

        with(Connection()) {
            mServerName = "127.0.0.1"
            mServerPort = "2323"
//            mServerName = server.ip
//            mServerPort = server.port.toString()
            mUseUdp = server.protocol.equals("udp", true)
            mConnections = arrayOf(this)
        }
        this
    }

    private fun getConfigValues(): List<String> {
        return listOf(Config.CLIENT_KEY, Config.CA_CERT, Config.CLIENT_CERT, Config.TA)
    }
}