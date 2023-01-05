package com.zhushenwudi.libiot.model

import androidx.annotation.Keep

@Keep
data class TimestampResp(
    val deviceSendTime: String,
    val serverRecvTime: String,
    val serverSendTime: String
)