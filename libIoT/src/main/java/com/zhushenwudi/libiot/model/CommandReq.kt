package com.zhushenwudi.libiot.model

import androidx.annotation.Keep

@Keep
data class CommandReq<T>(
    val devSN: String,
    val cmd: String,
    val data: T,
    val id: String
)