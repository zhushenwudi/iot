package com.zhushenwudi.libiot.model

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.Keep

@Keep
@SuppressLint("HardwareIds")
data class HeartBeatUp(
    val devSN: String = Build.SERIAL,
    val time: Long = System.currentTimeMillis() / 1000,
    val type: String = "event",
    val data: DataBean,
    var group: String
) {
    @Keep
    data class DataBean(
        val code: Int = 0,
        var rssi: Int,
        var cpu: String,
        var memory: String,
        var module: String
    )
}