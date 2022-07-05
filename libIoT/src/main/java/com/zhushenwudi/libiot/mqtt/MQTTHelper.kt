package com.zhushenwudi.libiot.mqtt

abstract class MQTTHelper {
    open fun initMqtt() {}

    open fun versionUp(versionName: String) {}
    open fun netOfflineUp(start: Long) {}
    open fun heartBeatUp() {}

    open fun subscribe(topicList: Array<String>) {}
    open fun publish(topic: String, msg: String) {}
    open fun cancelSubscribe(topics: Array<String>) {}

    open fun respCallBack(topic: String, message: String) {}
    open fun mqttCallBack(status: Boolean) {}

    open fun release() {}
}