package com.takehara.tsutou.w_ifiscanner.Fragment

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
import androidx.core.content.ContextCompat.checkSelfPermission
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
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.app.ActivityCompat


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER


/**
 * A simple [Fragment] subclass.
 * Use the [PositionComponent.newInstance] factory method to
 * create an instance of this fragment.
 */
class PositionComponent : Fragment() {
    // TODO: Rename and change types of parameters


    private val gson = Gson()
    data class Upload(
        @SerializedName("test") var test: Boolean,
        @SerializedName("data") var data: ArrayList<List<PositionComponent.Data>>
    )
    data class Data (
        @SerializedName("mac") var mac: String,
        @SerializedName("rssi") var rssi: String,
        @SerializedName("ssid") var ssid: String
    )
    private var wifiReceiverRegistered: Boolean = false
    private var jsonString: ArrayList<List<PositionComponent.Data>>? = ArrayList()

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
    private fun wifiList() {
        val APdata = ArrayList<PositionComponent.Data>()
        val results = wifiManager.scanResults as ArrayList<ScanResult>
        for (result in results) {
            val mac = result.BSSID.replace(":", "")
            val newData =
                PositionComponent.Data(mac = mac, rssi = result.level.toString(), ssid = result.SSID)
            APdata.add(newData)
        }

        jsonString = arrayListOf(APdata)
        Log.i("",jsonString.toString())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        wifiList()
        UploadAPI()
        val view = inflater.inflate(R.layout.fragment_position_component, container, false)
        // view.classroom.setText("綜合科館109-2")

        return view

    }

    private fun UploadAPI() {

        val data =
            jsonString?.let {
                PositionComponent.Upload(
                    test = true,
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
            override fun onResponse(call: Call, response: Response) {
                Log.d("STATUS", response.body!!.string())
            }
        })
    }

    override fun onStart() {
        super.onStart()
        startScanning()
    }


    override fun onStop() {
        stopScanning()
        super.onStop()
    }

    private fun startScanning() {
        if (checkPermissions()) {
            activity?.applicationContext?.registerReceiver(wifiReceiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
            Log.i("","scan")
            wifiReceiverRegistered = true
        }
    }

    private fun stopScanning() {
        if (wifiReceiverRegistered) {
            activity?.applicationContext?.unregisterReceiver(wifiReceiver)
            wifiReceiverRegistered = false
        }
    }
    private fun refreshList() {
        wifiManager.startScan()
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