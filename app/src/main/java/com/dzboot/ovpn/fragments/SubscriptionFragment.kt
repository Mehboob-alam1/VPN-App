package com.dzboot.ovpn.fragments

import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails
import com.dzboot.ovpn.R
import com.dzboot.ovpn.activities.MainActivity
import com.dzboot.ovpn.base.BaseFragment
import com.dzboot.ovpn.databinding.FragmentProductsBinding
import com.dzboot.ovpn.helpers.SubscriptionManager
import com.dzboot.ovpn.helpers.invisible
import com.dzboot.ovpn.helpers.visible


class SubscriptionFragment : BaseFragment<MainActivity, FragmentProductsBinding>() {

    companion object {

        const val STATIC_TAG = "SubscriptionFragment"
    }

    override val TAG = STATIC_TAG

    override fun initializeBinding() =
        FragmentProductsBinding.inflate(requireActivity().layoutInflater)

    override fun getPageTitle(): Int = R.string.subscribe

    private lateinit var monthlySku: SkuDetails
    private lateinit var yearlySku: SkuDetails

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }
}