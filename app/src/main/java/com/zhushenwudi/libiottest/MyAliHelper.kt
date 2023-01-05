package com.zhushenwudi.libiottest

import android.content.Context
import com.zhushenwudi.libiot.AppUtils.fromJson
import com.zhushenwudi.libiot.AppUtils.toJson
import com.zhushenwudi.libiot.model.CommandReq
import com.zhushenwudi.libiot.model.CommandResp
import com.zhushenwudi.libiot.mqtt.linksdk.AliHelper

class MyAliHelper(
    applicationContext: Context,
    productKey: String,
    productSecret: String
) : AliHelper(
    applicationContext = applicationContext,
    productKey = productKey,
    productSecret = productSecret
) {
    override fun respCallBack(topic: String, message: String) {
        // ntp服务在super里面实现
        super.respCallBack(topic, message)
        try {
            when (topic) {
                cmdTopic -> {
                    fromJson<CommandReq<Any>>(message)?.run {
                        when (cmd) {
                            "QueryLog" -> {
                                val bean = data as LogReqBean
                                commandResp(id = id, cmd = cmd, status = "OK")
                                // 获取日志中...

                                // 发送日志
                                queryLog(id, begin = bean.begin, end = bean.end, arrayListOf())
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}