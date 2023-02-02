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
    url = url,
    queryLogFunc = { start, end ->
        arrayListOf("111", "222")
    },
    setGroupFunc = {
        // todo: 写入文件
        App.iotGroup = it
    },
    mGroup = App.iotGroup
) {

    override fun respCallBack(topic: String, message: String) {
        super.respCallBack(topic, message)
        when (topic) {
            cmdTopic -> {

            }
        }
    }
}