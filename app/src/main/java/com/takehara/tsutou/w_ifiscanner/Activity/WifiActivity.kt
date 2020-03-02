package com.takehara.tsutou.w_ifiscanner.Activity

import android.Manifest
import androidx.fragment.app.Fragment
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.takehara.tsutou.w_ifiscanner.Fragment.WifiDetailFragment
import com.takehara.tsutou.w_ifiscanner.Fragment.WifiListFragment
import com.takehara.tsutou.w_ifiscanner.Model.WifiStation
import com.takehara.tsutou.w_ifiscanner.R

open class WifiActivity : AppCompatActivity() {

    companion object {
        private const val PERMISSION_REQUEST_CODE_ACCESS_COARSE_LOCATION = 120
    }

    private val wifiManager: WifiManager
        get() = this.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private var listFragment: WifiListFragment? = null
    private var detailFragment: WifiDetailFragment? = null

    private var listFragmentVisible: Boolean = false
    private var wifiReceiverRegistered: Boolean = false

    private val wifiReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val results = wifiManager.scanResults
            if (listFragmentVisible && results != null) {
                listFragment?.updateItems(results)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wifi)
        setTitle(R.string.title_wifi)

        transitionToList()

        if (!wifiManager.isWifiEnabled) {
            Toast.makeText(this, R.string.prompt_enabling_wifi, Toast.LENGTH_SHORT).show()
            wifiManager.isWifiEnabled()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.wifi, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.action_refresh -> {
                refreshList()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onStart() {
        super.onStart()
        startScanning()
    }

    fun onResumeFragment(fragment: Fragment) {
        listFragmentVisible = false

        if (fragment == listFragment) {
            listFragmentVisible = true

            refreshList()
        }
    }

    override fun onStop() {
        stopScanning()
        super.onStop()
    }

    private fun startScanning() {
        if (checkPermissions()) {
            registerReceiver(wifiReceiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
            wifiReceiverRegistered = true
        }
    }

    private fun stopScanning() {
        if (wifiReceiverRegistered) {
            unregisterReceiver(wifiReceiver)
            wifiReceiverRegistered = false
        }
    }

    private fun transition(fragment: Fragment, add: Boolean = false) {
        val manager = supportFragmentManager
        val transaction = manager.beginTransaction()
        transaction.replace(R.id.layout_frame, fragment)
        if (add) {
            transaction.addToBackStack(null)
        }
        transaction.commit()
    }

    private fun transitionToList() {
        listFragment = WifiListFragment.newInstance()
        transition(requireNotNull(listFragment))
    }

    fun transitionToDetail(item: WifiStation) {
        detailFragment = WifiDetailFragment.newInstance(item)
        transition(requireNotNull(detailFragment), true)
    }

    private fun refreshList() {
        listFragment?.clearItems()
        wifiManager.startScan()
    }

    private fun checkPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                PERMISSION_REQUEST_CODE_ACCESS_COARSE_LOCATION
            )
            return false
        } else {
            return true
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CODE_ACCESS_COARSE_LOCATION -> {
                startScanning()
            }
        }
    }

}