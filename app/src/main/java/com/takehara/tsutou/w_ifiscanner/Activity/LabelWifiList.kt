package com.takehara.tsutou.w_ifiscanner.Activity

import android.content.Context
import android.net.wifi.ScanResult
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.fragment.app.ListFragment
import com.takehara.tsutou.w_ifiscanner.Fragment.LabelActivity
import com.takehara.tsutou.w_ifiscanner.Model.WifiStation
import com.takehara.tsutou.w_ifiscanner.R
import kotlinx.android.synthetic.main.fragment_label_wifi_list.*
import kotlinx.android.synthetic.main.fragment_wifi_list.*
import kotlinx.android.synthetic.main.list_item_wifi.view.*

class LabelWifiList : ListFragment() {

    companion object {
        fun newInstance() =
            LabelWifiList()
    }

    private var emptyView: View? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_label_wifi_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        emptyView = label_wifi_scan_view
        listAdapter = activity?.let {
            WifiListAdapter(it)
        }
        listView.emptyView = emptyView
    }

    override fun onResume() {
        super.onResume()
        val activity = this.activity
        if (activity is LabelActivity) {
            activity.onResumeFragment(this)
        }
    }

    override fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {
        super.onListItemClick(l, v, position, id)

        val activity = this.activity

        if (activity is LabelActivity) {
            val item = l.getItemAtPosition(position) as WifiStation
            activity.transitionToDetail(item)
        }
    }

    fun updateItems(stations: List<ScanResult>? = null) {
        val listAdapter = this.listAdapter
        if (listAdapter is WifiListAdapter) {
            listAdapter.clear()
            if (stations != null) {
                emptyView?.visibility = if (stations.isNotEmpty()) View.VISIBLE else View.GONE
                listAdapter.addAll(WifiStation.newList(stations))
            }
            listAdapter.notifyDataSetChanged()
        }
    }

    fun clearItems() = updateItems()

    class WifiListAdapter(context: Context) : ArrayAdapter<WifiStation>(context, 0) {
        private val inflater: LayoutInflater = LayoutInflater.from(context)

        @Suppress("IfThenToElvis")
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val item = getItem(position)
            val view = if (convertView != null) {
                convertView
            } else {
                inflater.inflate(R.layout.list_item_wifi, parent, false)
            }

            if (item != null) {
                view.txt_ssid.text = item.ssid
            }
            if (item != null) {
                view.txt_bssid.text = item.bssid
            }
            if (item != null) {
                view.txt_frequency.text = context.getString(R.string.station_frequency, item.frequency.toString())
            }
            if (item != null) {
                view.txt_level.text = context.getString(R.string.station_level, item.level.toString())
            }

            return view
        }
    }
}
