package com.zhushenwudi.libiot.model

import androidx.annotation.Keep

@Keep
data class CommandReq<T>(
    val id: String,
    val cmd: String,
    val data: T?
) {
    @Keep
    data class VersionInfo(
        val version: String?,
        val url: String?,
        val md5: String?
    )

    @Keep
    data class LogReqBean(
        val begin: String,
        val end: String
    )

    @Keep
    data class SetGroupBean(
        val group: String
    )
}
