package com.zhushenwudi.libiot.model

import android.os.Build
import androidx.annotation.Keep

@Keep
data class ErrorEventUp(
    val devSN: String = Build.SERIAL,
    val time: Long = System.currentTimeMillis() / 1000,
    val type: String = "event",
    val group: String,
    val data: DataBean
) {
    @Keep
    data class DataBean(
        val code: Int,
        val peripheral: String,
        val time: Long = System.currentTimeMillis() / 1000,
        val msg: String
    )
}
