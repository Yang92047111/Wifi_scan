package com.takehara.tsutou.w_ifiscanner.Fragment

import android.content.ContentValues.TAG
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.takehara.tsutou.w_ifiscanner.Activity.LabelActivity
import com.takehara.tsutou.w_ifiscanner.R
import kotlinx.android.synthetic.main.fragment_label_component.*
import kotlinx.android.synthetic.main.fragment_label_component.view.*
import okhttp3.*
import org.angmarch.views.NiceSpinner
import java.io.IOException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager


class LabelComponent : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_label_component, container, false)

        // Building spinner
        val building_types = mutableListOf<String>("綜合科館", "共同科館")
        val building_spinner = view.building_spinner as NiceSpinner
        building_spinner.setTextColor(Color.BLACK)
        building_spinner.attachDataSource(building_types)

        // Floor spinner
        val floor_types = mutableListOf<String>("1F", "2F")
        val floor_spinner = view.floor_spinner as NiceSpinner
        floor_spinner.setTextColor(Color.BLACK)
        floor_spinner.attachDataSource(floor_types)

        // Classroom spinner
        val classroom_types = mutableListOf<String>("107-1", "109-1")
        val classroom_spinner = view.classroom_spinner as NiceSpinner
        classroom_spinner.setTextColor(Color.BLACK)
        classroom_spinner.attachDataSource(classroom_types)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
            Log.i(TAG, "ssl")
            okHttpClient.sslSocketFactory(
                sslSocketFactory,
                trustAllCerts.first() as X509TrustManager
            )
            val allow = HostnameVerifier { _, _ -> true }
            okHttpClient.hostnameVerifier(allow)
            Log.i(TAG, "ssl2")
        }

        client = okHttpClient.build()

        val formBody = FormBody.Builder()
            .add("pressure", "Hi")
            .build()
        val request = Request.Builder()
            .url("https://virtserver.swaggerhub.com/chougitom/podm/1.0.0/upload")
            .post(formBody)
            .addHeader("Content-Type","application/json")
            .build()

        view.label_btn.setOnClickListener {

            val intent = Intent(activity, LabelActivity::class.java)

            intent.putExtra("building", building_spinner.text.toString())
            intent.putExtra("floor", floor_spinner.text.toString())
            intent.putExtra("classroom", classroom_spinner.text.toString())

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {

                }

                @Throws(IOException::class)
                override fun onResponse(call: Call, response: Response) {
                    Log.d("STATUS", response.body!!.string())
                }
            })

            startActivity(intent)
        }
    }

}
