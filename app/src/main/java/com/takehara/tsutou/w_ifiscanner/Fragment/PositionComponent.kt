package com.takehara.tsutou.w_ifiscanner.Fragment

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.takehara.tsutou.w_ifiscanner.R
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import android.content.Intent
import android.os.Handler
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.fragment_position_component.*
import kotlinx.android.synthetic.main.fragment_position_component.view.*
import java.util.*
import kotlin.collections.ArrayList

class PositionComponent : Fragment() {

    private val gson = Gson()
    private var taskHandler = Handler()

    private var wifiReceiverRegistered: Boolean = false

    private var wifidata: ArrayList<Data>? = ArrayList()
    private var beforewifidata: ArrayList<Data>? = ArrayList()
    private var jsonString: ArrayList<Data>? = ArrayList()


    data class Upload(
        @SerializedName("timestamp") var timestamp: Int,
        @SerializedName("disinfectionId") var disinfectionId: String,
        @SerializedName("data") var data: ArrayList<Data>
    )

    data class Data (
        @SerializedName("mac") var mac: String,
        @SerializedName("rssi") var rssi: Int,
        @SerializedName("ssid") var ssid: String
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_position_component, container, false)
        val car_types = mutableListOf<String>("N95", "N96")
        val time = System.currentTimeMillis()
        Log.i("timestamp",time.toString())
        view.classroom.setText("綜合科館109-2")
        return view
    }

    private fun wifiList() {
        val APdata = ArrayList<Data>()
        val results = wifiManager.scanResults as ArrayList<ScanResult>
        for (result in results) {
            val mac = result.BSSID.replace(":", "").toUpperCase(Locale.ROOT)
            val newData =
                Data(mac = mac, rssi = result.level, ssid = result.SSID)
            APdata.add(newData)
        }

        jsonString = APdata
        wifidata = jsonString
        Log.i("wifitest",jsonString.toString())
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE_ACCESS_COARSE_LOCATION = 120
    }

    private val wifiManager: WifiManager
        get() =this.activity?.applicationContext?.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private val wifiReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val results = wifiManager.scanResults as ArrayList<ScanResult>
        }
    }
    private fun UploadAPI() {
        val second=System.currentTimeMillis()/1000
        val time = second.toInt()
        Log.i("timestamp",time.toString())
        val data =
            jsonString?.let {
                Upload(
                    timestamp = time,
                    disinfectionId = "FUCK",
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
            Log.i(ContentValues.TAG, "ssl")
            okHttpClient.sslSocketFactory(
                sslSocketFactory,
                trustAllCerts.first() as X509TrustManager
            )
            val allow = HostnameVerifier { _, _ -> true }
            okHttpClient.hostnameVerifier(allow)
            Log.i(ContentValues.TAG, "ssl2")
        }

        client = okHttpClient.build()

        val formBody = json.toRequestBody()
        val request = Request.Builder()
            .url("https://140.124.73.63:3003/api/user/addtest")
            .post(formBody)
            .addHeader("Content-Type","application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response
            )  {
                val room =response.body!!.string()
                Log.d("STATUS", room)
                Thread(Runnable {
                    activity?.runOnUiThread(java.lang.Runnable {
                        classroom.setText(room)
                    })
                }).start()
            }
        })
    }

    private var runnable = object:Runnable {
        override fun run() {
            beforewifidata = wifidata
            wifiManager.startScan()
            wifiList()
            if (beforewifidata != wifidata) {
                beforewifidata = wifidata
                UploadAPI()
            }
            taskHandler.postDelayed(this,5000)
        }
    }

    override fun onResume() {
        super.onResume()
        wifiManager.startScan()
        wifiList()
        UploadAPI()
        taskHandler.postDelayed(runnable,5000)
    }

    override fun onStop() {
        stopScanning()
        super.onStop()
        taskHandler.removeCallbacksAndMessages(null)
    }

    private fun startScanning() {
        if (checkPermissions()) {
            activity?.applicationContext?.registerReceiver(wifiReceiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
            wifiReceiverRegistered = true
        }
    }

    private fun stopScanning() {
        if (wifiReceiverRegistered) {
            activity?.applicationContext?.unregisterReceiver(wifiReceiver)
            wifiReceiverRegistered = false
        }
    }

    private fun checkPermissions(): Boolean {
        val context = context ?: return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            && ActivityCompat.checkSelfPermission( context,Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
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