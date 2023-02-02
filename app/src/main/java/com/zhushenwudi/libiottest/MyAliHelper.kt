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
    productSecret: String,
    sku: String
) : AliHelper(
    applicationContext = applicationContext,
    productKey = productKey,
    productSecret = productSecret,
    sku = sku,
    queryLogFunc = { start, end ->
        arrayListOf("111", "222")
    },
    setGroupFunc = {
        // todo: 写入文件
        App.iotGroup = it
    },
    mGroup = App.iotGroup
)