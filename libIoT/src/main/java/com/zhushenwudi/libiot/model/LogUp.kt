package com.zhushenwudi.libiot.model

import android.os.Build
import androidx.annotation.Keep

@Keep
data class LogUp(
    val id: String,
    val devSN: String = Build.SERIAL,
    val time: Long = System.currentTimeMillis() / 1000,
    val type: String = "log",
    val group: String,
    val data: DataBean
) {
    @Keep
    data class DataBean(
        val begin: Long,
        val end: Long,
        val logs: ArrayList<String>
    )
}