package com.takehara.tsutou.w_ifiscanner.Fragment

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.takehara.tsutou.w_ifiscanner.Activity.LabelActivity
import com.takehara.tsutou.w_ifiscanner.R
import kotlinx.android.synthetic.main.fragment_label_component.*
import kotlinx.android.synthetic.main.fragment_label_component.view.*
import kotlinx.android.synthetic.main.fragment_position_component.*
import kotlinx.android.synthetic.main.fragment_position_component.view.*
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import org.angmarch.views.NiceSpinner
import java.io.IOException
import java.lang.Exception
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.*
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlin.collections.ArrayList

class PositionComponent : Fragment() {

    private val gson = Gson()
    private var taskHandler = Handler()
    private var wifiReceiverRegistered: Boolean = false
    private var wifidata: ArrayList<Data>? = ArrayList()
    private var beforewifidata: ArrayList<Data>? = ArrayList()
    private var jsonString: ArrayList<Data>? = ArrayList()

    private var postData = ArrayList<Upload>()
    private var tempData  =
        jsonString?.let {
            Upload(
                timestamp = 1,
                disinfectionId = "0",
                data = it
            )
        }



    data class Upload (
        @SerializedName("timestamp") var timestamp: Int,
        @SerializedName("disinfectionId") var disinfectionId: String,
        @SerializedName("data") var data: ArrayList<Data>
    )

    data class Data (
        @SerializedName("mac") var mac: String,
        @SerializedName("rssi") var rssi: Int,
        @SerializedName("ssid") var ssid: String
    )

    data class ResponseData(
        val code: Int = 0,
        val result: ResponseResult? = null,
        val message: String
    )

    data class ResponseResult (
        val disinfectionId: String = "",
        val location: ResponseLocation? = null,
        val timestamp: Int = 0
    )

    data class ResponseLocation (
        val building: String = "",
        val floor: String = "",
        val name: String = ""
    )

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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_position_component, container, false)
        view.classroom.setText(" ")
        val car_spinnertype = mutableListOf<String>("01", "02", "03")
        val  car_spinner = view. car_spinner as NiceSpinner
        car_spinner.setTextColor(Color.BLACK)
        car_spinner.attachDataSource(car_spinnertype)
        var locate_status =0

        view.locate_btn.setOnClickListener {
            locate_status = locate_status +1
            if(locate_status==1) {
                locate_animate.playAnimation();
                wifiManager.startScan()
                wifiList()
                UploadAPI()
                taskHandler.postDelayed(runnable, 10000)
                view.locate_btn.setText("停止定位")
                view.locate_btn.setTextColor(Color.parseColor("#f44336"))
            }
            else if(locate_status==2)
               {
                    locate_animate.pauseAnimation();
                    taskHandler.removeCallbacksAndMessages(null)
                    view.locate_btn.setText("開始定位")
                   locate_status = locate_status-2
                   view.locate_btn.setTextColor(Color.parseColor("#437BFE"))
                }
        }
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

    private fun UploadAPI() {
        val second=System.currentTimeMillis()/1000
        val time = second.toInt()
        val data =
            jsonString?.let {
                Upload(
                    timestamp = time,
                    disinfectionId = car_spinner.text.toString(),
                    data = it
                )
            }
//        Log.i("data",data.toString())
        if (data != null && tempData?.disinfectionId=="0" && data?.data.isNullOrEmpty()==false) {
            postData.add(data)
            Log.i("que",postData.toString())
        }
        else if (data != null && tempData!=null && data?.data.isNullOrEmpty()==false) {

            Log.i("beforeque",postData.toString())

            postData.add(data)
            Log.i("afterque",postData.toString())
        }
        else if (data != null && data?.data.isNullOrEmpty()==true ){
            Log.i("no wifi",postData.toString())
        }


        val json = gson.toJson(postData)
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
//            .url("https://podm.chc.nctu.me/api/locate")
            .url("https://140.124.73.63:3003/api/user/addtest")

            .post(formBody)
            .addHeader("Content-Type","application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    UpgradeLocation("", "中斷連線")
                    tempData=data
                }
                @Throws(IOException::class)
                override fun onResponse(call: Call, response: Response)  {
                    Log.i("server", "failed")
                    val locationJson = response.body
                    postData.clear()
                    if (response.code == 200) {
                        Log.i("response", "locationString")
                        tempData  =
                            jsonString?.let {
                                Upload(
                                    timestamp = time,
                                    disinfectionId = "0",
                                    data = it
                                )
                            }

                        if (response.body != null) {
                            try {
                                val locationData: ResponseData = gson.fromJson(locationJson!!.charStream(), ResponseData::class.java)
                                Log.i("fomJson", locationData.toString())
                                when (locationData.code) {
                                    0 ->locationData.result?.location?.building?.let { UpgradeLocation(it,
                                        locationData.result.location.name) }
                                    1 ->UpgradeLocation("", "WiFi scan 錯誤")
                                    2 ->UpgradeLocation("", "定位失敗")
                                }
                            }
                            catch (e:Exception) {
                                UpgradeLocation("", "定位失敗")
                                Log.i("error code", e.toString())
                            }
                        }
                        else
                        {
                            UpgradeLocation("", "")
                        }
                    }
                    else {
                        Log.i("server", "failed")
                        UpgradeLocation("", "中斷連線")
                        tempData=data
                    }
                }
            }
        )
    }

    private fun UpgradeLocation (Build: String = "", Classroom: String = "未定位") {
        Thread(Runnable {
            activity?.runOnUiThread(java.lang.Runnable {
                classroom.setText(Build + Classroom)
            })
        }).start()
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
            taskHandler.postDelayed(this,10000)
        }
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