package com.zhushenwudi.libiot.model

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.Keep

@Keep
@SuppressLint("HardwareIds")
data class VersionUp(
    val devSN: String = Build.SERIAL,
    val time: Long = System.currentTimeMillis() / 1000,
    val type: String = "version",
    val data: DataBean
) {
    @Keep
    data class DataBean(
        var firmware: String,
        var sku: String? = null
    )
}
