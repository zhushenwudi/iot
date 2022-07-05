package com.zhushenwudi.libiot.model

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.Keep

@Keep
@SuppressLint("HardwareIds")
data class CommandResp(
    val devSN: String = Build.SERIAL,
    val time: Long = System.currentTimeMillis() / 1000,
    var type: String,
    val data: DataBean,
    var id: String,
    var group: String
) {
    @Keep
    data class DataBean(
        var cmd: String,
        var status: String
    )
}
