package com.dzboot.ovpn.fragments

import android.os.Bundle
import android.view.View
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.widget.ViewPager2
import com.dzboot.ovpn.R
import com.dzboot.ovpn.activities.IntroActivity
import com.dzboot.ovpn.adapters.ViewPagerAdapter
import com.dzboot.ovpn.base.BaseFragment
import com.dzboot.ovpn.databinding.FragmentIntroBinding
import java.util.ArrayList


class IntroFragment : BaseFragment<IntroActivity, FragmentIntroBinding>() {

    companion object {

        const val STATIC_TAG = "IntroFragment"
    }

    override val TAG = STATIC_TAG

    override fun initializeBinding() =
        FragmentIntroBinding.inflate(requireActivity().layoutInflater)

    override fun getPageTitle() = 0
    private var titleList = ArrayList<String>()
    private var descList = ArrayList<String>()
    private var imageList = ArrayList<Int>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.continueToApp.setOnClickListener { activity?.changeScreen(FirstLoadFragment()) }
        clearList()
        postTolist()
        binding.viewPager?.adapter = ViewPagerAdapter(titleList, descList, imageList)
        binding.viewPager?.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        val indicator = binding.indicator
        indicator?.setViewPager(binding.viewPager)



        binding.privacyInnerLayout.setOnClickListener {
            activity?.changeToDisplayFragment(DisplayFragment.DisplayType.PRIVACY_POLICY)
        }

        binding.termsInnerLayout.setOnClickListener {
            activity?.changeToDisplayFragment(DisplayFragment.DisplayType.TERMS)
        }

        binding.close.setOnClickListener { activity?.finishAffinity() }
    }

    private fun clearList() {
        titleList.clear()
        descList.clear()
        imageList.clear()
    }

    private fun addToList(title: String, descrption: String, images: Int) {

        titleList.add(title)
        descList.add(descrption)
        imageList.add(images)
    }

    private fun postTolist() {
        for (k in 1..1) {
            addToList(
                "Best VPN Unlimited Service ",
                "Nic VPN is one of the best\n virtual private network service to\n protect all data,to surf the web\n anonymously ",
                R.drawable.vpnkhan
            )
            addToList(
                "Browse Faster Internet ",
                "Browse faster Internet Free\n server. Upgrade Premium Service\n Super fast Browser ",
                R.drawable.t1
            )
            addToList(
                "Free and VIP Servers ",
                "Your Identification will be secure.\n Let's Browse now Free and VIP \n Servers ",
                R.drawable.t2
            )

        }
    }
}