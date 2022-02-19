package com.dzboot.ovpn.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.dzboot.ovpn.R
import com.dzboot.ovpn.databinding.ActivityMainBinding
import com.dzboot.ovpn.databinding.ActivityProtocolBinding
import com.dzboot.ovpn.fragments.NotificationsFragment
import com.dzboot.ovpn.fragments.SubscriptionFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.inmobi.media.id

class ProtocolActivity : AppCompatActivity(){
    private lateinit var binding: ActivityProtocolBinding
    private lateinit var selectedFruits: String
    private var selectedFruitsIndex: Int = 0
    private val fruits = arrayOf("Default", "www.bing.com", "www.google.com", "get.adobe.com", "www.mozilla.org",
        "www.firefox.com", "www.apple.com", "www.microsoft.com", "weather.com")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityProtocolBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnCross.setOnClickListener {
            onBackPressed()
        }
       binding.btnSelectDomain.setOnClickListener {

          MaterialAlertDialogBuilder(this)
              .setTitle("Domains")
              .setSingleChoiceItems(fruits, selectedFruitsIndex) { dialog_, which ->
                  selectedFruitsIndex = which
                  selectedFruits = fruits[which]
              }
              .setPositiveButton("Ok") { dialog, which ->
                  Toast.makeText(this, "$selectedFruits Selected", Toast.LENGTH_SHORT)
                      .show()
              }
              .setNegativeButton("Cancel") { dialog, which ->
                  dialog.dismiss()
              }
              .show()
       }

    }




}