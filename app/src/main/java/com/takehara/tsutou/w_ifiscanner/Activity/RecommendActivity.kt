package com.takehara.tsutou.w_ifiscanner.Activity

import android.content.ContentValues
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.takehara.tsutou.w_ifiscanner.Fragment.PositionComponent
import com.takehara.tsutou.w_ifiscanner.R
import kotlinx.android.synthetic.main.activity_recommend.*
import kotlinx.android.synthetic.main.activity_recommend.building_spinner
import kotlinx.android.synthetic.main.activity_recommend.classroom_spinner
import kotlinx.android.synthetic.main.activity_recommend.confirm
import kotlinx.android.synthetic.main.activity_recommend.floor_spinner
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
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class RecommendActivity : AppCompatActivity() {

    private var jsonString: ArrayList<Location>? = ArrayList()
    private val gson = Gson()
    private var postData = ArrayList<Upload>()
    data class Upload (
        @SerializedName("disinfectionId") var disinfectionId: String,
        @SerializedName("location") var location: Location,
        @SerializedName("timestamp") var timestamp: Int
    )
    data class Location (
        @SerializedName("building") var building: String,
        @SerializedName("floor") var floor: String,
        @SerializedName("name") var classroom: String
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recommend)

        val building_types = mutableListOf<String>("綜合科館", "共同科館")
        val building_spinner = building_spinner as NiceSpinner
        building_spinner.setTextColor(Color.BLACK)
        building_spinner.attachDataSource(building_types)

        // Floor spinner
        val floor_types = mutableListOf<String>("1F", "2F", "3F")
        val floor_spinner = floor_spinner as NiceSpinner
        floor_spinner.setTextColor(Color.BLACK)
        floor_spinner.attachDataSource(floor_types)


        val classroom_1F_types = mutableListOf<String>("104", "105", "106", "107-1", "108", "108-1", "108-2", "109-1", "第一演講廳")
        val classroom_2F_types = mutableListOf<String>("201", "204", "205", "206-1", "206-2", "208", "210", "211")
        val classroom_3F_types = mutableListOf<String>("305", "306", "308", "311-1", "311-2")
        val classroom_spinner = classroom_spinner as NiceSpinner
        classroom_spinner.setTextColor(Color.BLACK)
        classroom_spinner.attachDataSource(classroom_1F_types)
        floor_spinner.addOnItemClickListener(object : AdapterView.OnItemClickListener {
            override fun onItemClick(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                when (p2){
                    0->  classroom_spinner.attachDataSource(classroom_1F_types)
                    1->  classroom_spinner.attachDataSource(classroom_2F_types)
                    2->  classroom_spinner.attachDataSource(classroom_3F_types)
                }
            }
        })

        confirm.setOnClickListener {
            post()
            finish()
        }
    }
    fun post(){
        val intent = getIntent()
        val disinfectionId : String = intent.getStringExtra("disinfectionId")
        val second=System.currentTimeMillis()/1000
        val time = second.toInt()
        val newLocation =
            Location(
                building = building_spinner.text.toString(),
                floor = floor_spinner.text.toString(),
                classroom = classroom_spinner.text.toString()
            )

        val data =
            jsonString.let {
                Upload(
                    disinfectionId = disinfectionId,
                    timestamp = time,
                    location = newLocation
                )
            }
        postData.add(data)
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
        //   .url("https://podm.chc.nctu.me/api/uploadLocatedResult")
            .url("https://140.124.73.63:3003/api/user/addtest")
           .post(formBody)
            .addHeader("Content-Type","application/json")
            .build()
        val context = applicationContext
        val success = "送出成功!"
        val fail = "送出失敗!"
        val duration = Toast.LENGTH_SHORT
        val toastsuccess = Toast.makeText(context, success, duration)
        val toastfail = Toast.makeText(context, fail, duration)

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                toastfail.show()
            }
            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response)  {
                if (response.code == 200) {
                    Log.i("response", "success")
                    toastsuccess.show()

                    if (response.body != null) {
                        try {

                        }
                        catch (e: Exception) {
                            toastfail.show()
                            Log.i("error code", e.toString())
                        }
                    }
                }
                else {
                    Log.i("server", "failed")
                    toastfail.show()
                }
            }
        })
    }
}
