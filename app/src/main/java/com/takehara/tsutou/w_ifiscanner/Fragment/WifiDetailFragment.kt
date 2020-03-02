package com.takehara.tsutou.w_ifiscanner.Fragment

import androidx.fragment.app.Fragment
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.takehara.tsutou.w_ifiscanner.Model.WifiStation
import com.takehara.tsutou.w_ifiscanner.R
import kotlinx.android.synthetic.main.fragment_wifi_detail.*
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subscriptions.Subscriptions


open class WifiDetailFragment() : Fragment() {

    companion object {
        private const val ARG_STATION = "station"

        fun newInstance(station: WifiStation): WifiDetailFragment {
            val f = WifiDetailFragment()
            f.arguments = Bundle()
            f.arguments!!.putSerializable(ARG_STATION, station)

            return f
        }
    }

    private var contentTextView: TextView? = null

    private var strings: Observable<String>? = null

    private var subscription = Subscriptions.empty()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        strings = Observable.range(0, 10)
            .map { it.toString() }
            .doOnNext { SystemClock.sleep(1000) }
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .cache()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return requireNotNull(inflater).inflate(R.layout.fragment_wifi_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (view != null) {
            super.onViewCreated(view, savedInstanceState)
        }

        txt_content.setText(R.string.prompt_loading)

        subscription = strings?.subscribe { text ->

            val wifiDetail: WifiStation = arguments?.getSerializable("station") as WifiStation
            txt_content.text =
                    "SSID: " + wifiDetail.ssid.toString() + "\n" +
                    "MAC: " + wifiDetail.bssid.toString() + "\n" +
                    "LEVEL: " + wifiDetail.level.toString() + "\n" +
                    "FREQUENCY(20 MHz): " + wifiDetail.frequency.toString() + "\n"

        }
    }

    override fun onDestroyView() {
        subscription?.unsubscribe()

        super.onDestroyView()
    }
}