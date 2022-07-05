package com.zhushenwudi.libiot.model

import androidx.annotation.Keep

@Keep
data class CommandDown(
    val id: String,
    val cmd: String,
    val data: DataBean?,
    var group: String
) {
    @Keep
    data class DataBean(
        val group: String
    )
}
