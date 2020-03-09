package com.takehara.tsutou.w_ifiscanner.Activity

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.takehara.tsutou.w_ifiscanner.Fragment.LabelWifiList
import com.takehara.tsutou.w_ifiscanner.R
import kotlinx.android.synthetic.main.activity_label.*
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.*
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlin.collections.ArrayList

open class LabelActivity : AppCompatActivity() {

    private val gson = Gson()

    data class Upload(
        @SerializedName("test") var test: Boolean,
        @SerializedName("location") var location: Location,
        @SerializedName("data") var data: ArrayList<List<Data>>
    )

    data class Location (
        @SerializedName("building") var building: String,
        @SerializedName("floor") var floor: String,
        @SerializedName("name") var classroom: String
    )

    data class Data (
        @SerializedName("mac") var mac: String,
        @SerializedName("rssi") var rssi: Int,
        @SerializedName("ssid") var ssid: String
    )

    private var jsonString: ArrayList<List<Data>>? = ArrayList()

    companion object {
        private const val PERMISSION_REQUEST_CODE_ACCESS_COARSE_LOCATION = 120
    }

    private val wifiManager: WifiManager
        get() = this.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private var listFragment: LabelWifiList? = null
    private var ResponseCode: Int = 0

    private var listFragmentVisible: Boolean = false
    private var wifiReceiverRegistered: Boolean = false
    private var scanAgain: Boolean = false

    private val wifiReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val results = wifiManager.scanResults as ArrayList<ScanResult>
            if (listFragmentVisible && results != null) {
                listFragment?.updateItems(results)
                restart.visibility = View.VISIBLE
                finish.visibility = View.VISIBLE
            }
            scanAgain = true
            wifiList()
        }
    }

    private fun wifiList() {
        val APdata = ArrayList<Data>()
        val results = wifiManager.scanResults as ArrayList<ScanResult>
        if (results != null) {
            for (result in results) {
                val mac = result.BSSID.replace(":", "").toUpperCase(Locale.ROOT)
                val newData = Data(mac = mac, rssi = result.level, ssid = result.SSID)
                APdata.add(newData)
            }
        }
        jsonString = arrayListOf(APdata)
        if (scanAgain) {
            jsonString!!.add(APdata)
        }
        Log.i("afterJson", jsonString.toString())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_label)
        setTitle(R.string.title_wifi)

        transitionToList()

        if (!wifiManager.isWifiEnabled) {
            Toast.makeText(this, R.string.prompt_enabling_wifi, Toast.LENGTH_SHORT).show()
            wifiManager.isWifiEnabled()
        }

        restart.setOnClickListener {
            refreshList()

            scanAgain = true
            restart.visibility = View.GONE
            finish.visibility = View.GONE
        }

        finish.setOnClickListener {
            UploadAPI()
            finish()
        }
    }

    private fun UploadAPI() {

        val intent = getIntent()
        val building : String? = intent.getStringExtra("building")
        val floor: String? = intent.getStringExtra("floor")
        val classroom: String? = intent.getStringExtra("classroom")

        val newLocation =
            Location(
                building = building.toString(),
                floor = floor.toString(),
                classroom = classroom.toString()
            )

        val data =
            jsonString?.let {
                Upload(
                    test = false,
                    location = newLocation,
                    data = it
                )
            }
        val json = gson.toJson(data)
        Log.i("json", json)

        var client = OkHttpClient()
        val okHttpClient = OkHttpClient.Builder()

        // Create a trust manager that does not validate certificate chains
        val trustAllCerts: Array<TrustManager> = arrayOf(object : X509TrustManager {
            override fun checkClientTrusted(
                chain: Array<out X509Certificate>?,
                authType: String?
            ) {
            }

            override fun checkServerTrusted(
                chain: Array<out X509Certificate>?,
                authType: String?
            ) {
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        // Install the all-trusting trust manager
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, SecureRandom())

        // Create an ssl socket factory with our all-trusting manager
        val sslSocketFactory = sslContext.socketFactory
        if (trustAllCerts.isNotEmpty() && trustAllCerts.first() is X509TrustManager) {
            okHttpClient.sslSocketFactory(
                sslSocketFactory,
                trustAllCerts.first() as X509TrustManager
            )
            val allow = HostnameVerifier { _, _ -> true }
            okHttpClient.hostnameVerifier(allow)
        }

        client = okHttpClient.build()

        val formBody = json.toRequestBody()
        val request = Request.Builder()
//            James's server
            .url("https://140.124.73.63:3003/api/user/addtest")
//            .url("https://podm.chc.nctu.me/api/upload")
            .post(formBody)
            .addHeader("Content-Type","application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {

            }
            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                ResponseCode = response.code
                Log.i("STATUS", ResponseCode.toString())
            }
        })
    }

    override fun onStart() {
        super.onStart()
        startScanning()
    }

    fun onResumeFragment(fragment: Fragment) {
        listFragmentVisible = false
        scanAgain = false

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
        transaction.replace(R.id.label_list, fragment)
        if (add) {
            transaction.addToBackStack(null)
        }
        transaction.commit()
    }

    private fun transitionToList() {
        listFragment =
            LabelWifiList.newInstance()
        transition(requireNotNull(listFragment))

    }

    private fun refreshList() {
        listFragment?.clearItems()
        scanAgain = true
        wifiList()
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
