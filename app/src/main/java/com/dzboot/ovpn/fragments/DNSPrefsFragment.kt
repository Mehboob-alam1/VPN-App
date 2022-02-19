package com.dzboot.ovpn.fragments

import android.os.Bundle
import android.view.View
import com.dzboot.ovpn.R
import com.dzboot.ovpn.activities.MainActivity
import com.dzboot.ovpn.base.BaseFragment
import com.dzboot.ovpn.databinding.FragmentDnsPrefsBinding
import com.dzboot.ovpn.helpers.PrefsHelper


class DNSPrefsFragment : BaseFragment<MainActivity, FragmentDnsPrefsBinding>() {

    override fun initializeBinding() = FragmentDnsPrefsBinding.inflate(layoutInflater)

    override val TAG = "DNSPrefsFragment"

    override fun getPageTitle() = R.string.dns_prefs

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dns1 = PrefsHelper.getPrimaryDNS()
        val dns2 = PrefsHelper.getSecondaryDNS()
        val isDefault = PrefsHelper.isUseDefaultDNS()

        binding.dns1.setText(dns1)
        binding.dns2.setText(dns2)
        binding.defaultPref.isChecked = isDefault
        binding.dns1Layout.isEnabled = !isDefault
        binding.dns2Layout.isEnabled = !isDefault

        binding.defaultPref.setOnCheckedChangeListener { _, isChecked ->
            binding.dns1Layout.isEnabled = !isChecked
            binding.dns2Layout.isEnabled = !isChecked
        }

        binding.save.setOnClickListener {
            PrefsHelper.saveDNSPrefs(
                binding.defaultPref.isChecked,
                binding.dns1.text.toString(),
                binding.dns2.text.toString()
            )
        }
    }
}