package com.zhushenwudi.libiottest

import android.content.Context
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

    }
}