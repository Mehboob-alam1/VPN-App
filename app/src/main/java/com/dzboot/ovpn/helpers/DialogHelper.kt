package com.dzboot.ovpn.helpers

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.dzboot.ovpn.R
import com.dzboot.ovpn.helpers.Utils.launchDrawOverlaysPermission
import com.dzboot.ovpn.helpers.VPNHelper.openVPNSettings


object DialogHelper {

    @RequiresApi(Build.VERSION_CODES.N)
    fun otherVPNAppAlwaysAllowed(context: Context): AlertDialog = AlertDialog.Builder(context)
        .setMessage(R.string.nought_alwayson_warning)
        .setPositiveButton(R.string.open_settings) { _, _ -> context.openVPNSettings() }
        .setNegativeButton(android.R.string.cancel, null)
        .setCancelable(false)
        .show()

    fun newUpdateRequired(context: Context, okBtnClicked: () -> Unit):
            AlertDialog = AlertDialog.Builder(context)
        .setTitle(R.string.update_required)
        .setMessage(R.string.update_required_message)
        .setCancelable(false)
        .setPositiveButton(android.R.string.ok) { _, _ -> okBtnClicked() }
        .setNegativeButton(android.R.string.cancel, null)
        .show()

    fun invalidCertificate(context: Context): AlertDialog =
        AlertDialog.Builder(context)
            .setTitle(R.string.disconnect_alert_title)
            .setMessage(R.string.certificate_outdated_update_prompt)
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok) { _, _ -> Utils.openPlayStoreAppPage(context) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()

    fun disconnect(context: Context, @StringRes messageResId: Int, okBtnClicked: () -> Unit): AlertDialog =
        AlertDialog.Builder(context)
            .setTitle(R.string.disconnect_alert_title)
            .setMessage(messageResId)
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok) { _, _ -> okBtnClicked() }
            .setNegativeButton(android.R.string.cancel, null)
            .show()

    fun unlicensed(activity: Activity): AlertDialog =
        AlertDialog.Builder(activity)
            .setTitle(R.string.illegal_copy)
            .setMessage(R.string.unlicensed_copy)
            .setCancelable(false)
            .setPositiveButton(R.string.download) { _, _ ->
                Toast.makeText(activity, R.string.uninstall_toast, Toast.LENGTH_SHORT).show()
                Utils.openPlayStoreAppPage(activity)
                activity.finishAffinity()
            }
            .setNegativeButton(R.string.close_app) { _, _ -> activity.finishAffinity() }
            .show()

    fun notConsentedToPersonalizedAds(activity: AppCompatActivity, reloadForm: () -> Unit): AlertDialog =
        AlertDialog.Builder(activity)
            .setTitle(R.string.not_consented_title)
            .setMessage(R.string.not_consented_message)
            .setCancelable(false)
            .setPositiveButton(R.string.reload_form) { _, _ -> reloadForm() }
            .setNegativeButton(R.string.close_app) { _, _ -> activity.finishAffinity() }
            .show()

    @RequiresApi(Build.VERSION_CODES.M)
    fun promptDrawOverAppsPermissionRequired(
        activity: Activity,
        request: ActivityResultLauncher<Intent>,
        onCancelPermission: () -> Unit
    ): AlertDialog =
        AlertDialog.Builder(activity).setTitle(R.string.prompt_draw_over_apps)
            .setMessage(R.string.prompt_draw_over_apps_title)
            .setCancelable(false)
            .setPositiveButton(R.string.go_to_app_settings) { _, _ -> activity.launchDrawOverlaysPermission(request) }
            .setNegativeButton(android.R.string.cancel) { _, _ -> onCancelPermission() }
            .show()

    @RequiresApi(Build.VERSION_CODES.N)
    fun promptVPNAlwaysOn(context: Context, onCancel: () -> Unit): AlertDialog =
        AlertDialog.Builder(context)
            .setTitle(R.string.prompt_vpn_always_on_title)
            .setMessage(R.string.prompt_vpn_always_on)
            .setCancelable(false)
            .setPositiveButton(R.string.go_to_app_settings) { _, _ -> context.openVPNSettings() }
            .setNegativeButton(android.R.string.cancel) { _, _ -> onCancel() }
            .show()
}