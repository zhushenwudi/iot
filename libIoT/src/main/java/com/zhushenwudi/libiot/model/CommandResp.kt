package com.zhushenwudi.libiot.model

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.Keep

@Keep
@SuppressLint("HardwareIds")
data class CommandResp(
    val devSN: String = Build.SERIAL,
    val time: Long = System.currentTimeMillis() / 1000,
    var type: String = "response",
    val data: DataBean,
    val group: String,
    var id: String
) {
    @Keep
    data class DataBean(
        val cmd: String,
        val status: String
    )
}
