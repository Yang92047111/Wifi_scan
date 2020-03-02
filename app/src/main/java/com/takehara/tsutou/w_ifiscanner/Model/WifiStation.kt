package com.takehara.tsutou.w_ifiscanner.Model

import android.net.wifi.ScanResult
import java.io.Serializable
import javax.xml.transform.Result

data class WifiStation(
    val ssid: String?,
    val bssid: String? = null,
    val frequency: Int? = null,
    val level: Int? = null) : Serializable {

    companion object {

        fun newInstance(sr: ScanResult):WifiStation{
            return WifiStation(
                ssid = sr.SSID ,
                bssid = sr.BSSID,
                frequency = sr.frequency,
                level = sr.level
            )
        }

        fun newList(srs:List<ScanResult>):List<WifiStation>{
            return srs.map { newInstance(it) }
        }
    }
}