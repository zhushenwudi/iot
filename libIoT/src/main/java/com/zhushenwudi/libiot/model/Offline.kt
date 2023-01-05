package com.zhushenwudi.libiot.model

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.Keep
import com.zhushenwudi.libiot.mqtt.MQTTHelper

@Keep
@SuppressLint("HardwareIds")
data class Offline(
    val devSN: String = Build.SERIAL,
    var time: Long = System.currentTimeMillis() / 1000,
    val type: String = "offline",
    val group: String,
    val data: DataBean = DataBean(0, 0)
) {
    @Keep
    data class DataBean(
        var start: Long,
        var end: Long,
        val reason: String = "net"
    )
}
