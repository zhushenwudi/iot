package com.zhushenwudi.libiot.mqtt

abstract class MQTTHelper {
    var heartBeatInterval = 1
    var topicHeader: String? = null
    var group: String = "group"

    abstract fun initMqtt(mqttStatusCallback: ((status: Boolean) -> Unit)? = null)

    abstract fun versionUp(versionName: String)
    abstract fun netOfflineUp(start: Long)
    abstract fun heartBeatUp()

    abstract fun subscribe(topicList: Array<String>)
    abstract fun publish(topic: String, msg: String)
    abstract fun cancelSubscribe(topics: Array<String>)

    abstract fun respCallBack(topic: String, message: String)
    abstract fun mqttCallBack(status: Boolean)

    abstract fun release()
}