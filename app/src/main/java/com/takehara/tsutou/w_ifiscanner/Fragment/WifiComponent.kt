package com.takehara.tsutou.w_ifiscanner.Fragment

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.takehara.tsutou.w_ifiscanner.Activity.WifiActivity
import com.takehara.tsutou.w_ifiscanner.R
import kotlinx.android.synthetic.main.fragment_wifi_component.view.*

class WifiComponent : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_wifi_component, container, false)
        view.btn_scan.setOnClickListener {
            val intent = Intent(activity, WifiActivity::class.java)
            startActivity(intent)
        }
        return view
    }

}
