package com.zhushenwudi.libiottest

import android.content.Context
import com.zhushenwudi.libiot.mqtt.emq.EMQHelper

class MyEMQHelper(
    applicationContext: Context,
    productKey: String,
    url: String
): EMQHelper(
    applicationContext = applicationContext,
    productKey = productKey,
    url = url
) {

    override fun respCallBack(topic: String, message: String) {
        when (topic) {
            cmdTopic -> {

            }
        }
    }
}