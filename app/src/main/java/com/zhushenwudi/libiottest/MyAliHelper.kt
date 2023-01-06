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
    productSecret = productSecret,
    queryLogFunc = { start, end ->
        arrayListOf("111", "222")
    }
)