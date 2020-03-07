package com.takehara.tsutou.w_ifiscanner.Activity

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.opengl.Visibility
import android.os.Build
import android.os.Bundle
import android.os.Handler
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
import kotlinx.android.synthetic.main.fragment_label_component.*
import kotlinx.android.synthetic.main.fragment_label_component.view.*
import okhttp3.*
import java.io.IOException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

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
        @SerializedName("rssi") var rssi: String,
        @SerializedName("ssid") var ssid: String
    )

    companion object {
        private const val PERMISSION_REQUEST_CODE_ACCESS_COARSE_LOCATION = 120
    }

    private val wifiManager: WifiManager
        get() = this.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private var listFragment: LabelWifiList? = null

    private var listFragmentVisible: Boolean = false
    private var wifiReceiverRegistered: Boolean = false

    private val wifiReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val results = wifiManager.scanResults
            if (listFragmentVisible && results != null) {
                listFragment?.updateItems(results)
                restart.visibility = View.VISIBLE
                finish.visibility = View.VISIBLE
            }

            Log.i("wifi", results.toString())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_label)
        setTitle(R.string.title_wifi)

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

        val formBody = FormBody.Builder()
            .add("pressure", "Hi")
            .build()
        val request = Request.Builder()
            .url("https://140.124.73.63:3003/api/user/addtest")
            .post(formBody)
            .addHeader("Content-Type","application/json")
            .build()

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
        var newData: ArrayList<List<Data>> = ArrayList()
        val AP1 = Data(
            mac = "3C2D5E326429",
            rssi = "-20",
            ssid = "fuck"
        )
        val AP2 = Data(
            mac = "3C2D5E326428",
            rssi = "-20",
            ssid = "NTUT"
        )
        newData.add(listOf(AP1))
        newData.add(listOf(AP2))
        val data =
            Upload(
                test = true,
                location = newLocation,
                data = newData
            )
        val jsonString = gson.toJson(data)
        Log.i("json", jsonString)

        transitionToList()

        if (!wifiManager.isWifiEnabled) {
            Toast.makeText(this, R.string.prompt_enabling_wifi, Toast.LENGTH_SHORT).show()
            wifiManager.isWifiEnabled()
        }
        restart.setOnClickListener(){
            refreshList()
            restart.visibility = View.GONE
            finish.visibility = View.GONE
        }
        finish.setOnClickListener {
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {

                }

                @Throws(IOException::class)
                override fun onResponse(call: Call, response: Response) {
                    Log.d("STATUS", response.body!!.string())
                }
            })

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
    fun api(){

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
