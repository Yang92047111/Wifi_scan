package com.takehara.tsutou.w_ifiscanner.Activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.takehara.tsutou.w_ifiscanner.Fragment.LabelComponent
import com.takehara.tsutou.w_ifiscanner.Fragment.PositionComponent
import com.takehara.tsutou.w_ifiscanner.Fragment.SettingComponent
import com.takehara.tsutou.w_ifiscanner.Fragment.WifiComponent
import com.takehara.tsutou.w_ifiscanner.R
import kotlinx.android.synthetic.main.activity_main.*

open class MainActivity : AppCompatActivity() {

    enum class FragmentType {
        wifi, label, position, setting
    }

    val manager = supportFragmentManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initView()
    }

    private fun initView() {
        // set navigation Listener
        navigation.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener)

        // start from Home
        changeFragmentTo(FragmentType.wifi)
    }

    private val onNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_wifi -> {
                changeFragmentTo(FragmentType.wifi)
                return@OnNavigationItemSelectedListener true
            }

            R.id.navigation_label -> {
                title = "標記"
                changeFragmentTo(FragmentType.label)
                return@OnNavigationItemSelectedListener true
            }

            R.id.navigation_position -> {
                title = "定位"
                changeFragmentTo(FragmentType.position)
                return@OnNavigationItemSelectedListener true
            }

            R.id.navigation_setting -> {
                title = "Setting"
                changeFragmentTo(FragmentType.setting)
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }
//TODO
//    private fun transition(fragment: Fragment, add: Boolean = false) {
//        val transaction = fragmentManager.beginTransaction()
//        transaction.replace(R.id.baseFragment, fragment)
//        if (add) {
//            transaction.addToBackStack(null)
//        }
//        transaction.commit()
//    }

    private fun changeFragmentTo(type: FragmentType) {
        val transaction = manager.beginTransaction()
        when(type) {
            FragmentType.wifi -> {
                title = "Wi-Fi Scanner"
                val wifiFragment = WifiComponent()
                transaction.replace(R.id.baseFragment, wifiFragment)
//TODO
//                transition(requireNotNull(WifiListFragment.newInstance()))
//                transaction.replace(R.id.baseFragment, wifiFragment)
            }

            FragmentType.label -> {
                val labelFragment = LabelComponent()
                transaction.replace(R.id.baseFragment, labelFragment)
            }

            FragmentType.position -> {
                val progressFragment = PositionComponent()
                transaction.replace(R.id.baseFragment, progressFragment)
            }

            FragmentType.setting -> {
                val settingFragment = SettingComponent()
                transaction.replace(R.id.baseFragment, settingFragment)
            }

        }
        transaction.addToBackStack(null)
        transaction.commit()
    }

}

