package com.zhushenwudi.libiot.mqtt.linksdk

import android.util.Log
import com.aliyun.alink.linkkit.api.LinkKit
import com.aliyun.alink.linksdk.cmp.connect.channel.MqttPublishRequest
import com.aliyun.alink.linksdk.cmp.connect.channel.MqttSubscribeRequest
import com.aliyun.alink.linksdk.cmp.core.base.ARequest
import com.aliyun.alink.linksdk.cmp.core.base.AResponse
import com.aliyun.alink.linksdk.cmp.core.listener.IConnectSendListener
import com.aliyun.alink.linksdk.cmp.core.listener.IConnectSubscribeListener
import com.aliyun.alink.linksdk.cmp.core.listener.IConnectUnscribeListener
import com.aliyun.alink.linksdk.tools.AError

class AliMQTT(onMQTTConnectFailed: (message: String, aError: AError) -> Unit) {
    private val mSubscribeListener = object: IConnectSubscribeListener {
        override fun onSuccess() {
        }

        override fun onFailure(aError: AError) {
            onMQTTConnectFailed(SUBSCRIBE_FAIL, aError)
        }
    }

    private val mConnectSendListener = object: IConnectSendListener {
        override fun onResponse(aRequest: ARequest, aResponse: AResponse) {
        }

        override fun onFailure(aRequest: ARequest, aError: AError) {
            onMQTTConnectFailed(PUBLISH_FAIL, aError)
        }
    }

    private val mCancelSubscribeListener = object: IConnectUnscribeListener {
        override fun onSuccess() {
        }

        override fun onFailure(aError: AError) {
            onMQTTConnectFailed(UNSUBSCRIBE_FAIL, aError)
        }
    }
    /**
     * 发布
     *
     * @param topic   主题
     * @param message 消息
     */
    fun publish(topic: String, message: String) {
        val request = MqttPublishRequest()
        request.isRPC = false
        request.topic = topic
        request.qos = 0
        request.payloadObj = message
        Log.d(TAG, "$topic -- $message")
        LinkKit.getInstance().publish(request, mConnectSendListener)
    }

    /**
     * 订阅一个主题
     *
     * @param topic 主题
     */
    fun subscribe(topic: String) {
        val subscribeRequest = MqttSubscribeRequest()
        subscribeRequest.topic = topic
        subscribeRequest.isSubscribe = true
        subscribeRequest.qos = 0 // 支持0或者1
        LinkKit.getInstance().subscribe(subscribeRequest, mSubscribeListener)
    }

    /**
     * 订阅多个主题
     *
     * @param topicList 主题列表
     */
    fun subscribe(topicList: Array<String>) {
        topicList.forEach {
            val subscribeRequest = MqttSubscribeRequest()
            subscribeRequest.topic = it
            subscribeRequest.isSubscribe = true
            subscribeRequest.qos = 0 // 支持0或者1
            LinkKit.getInstance().subscribe(subscribeRequest, mSubscribeListener)
        }
    }

    /**
     * 取消订阅
     *
     * @param topicList 主题列表
     */
    fun cancelSubscribe(topicList: Array<String>) {
        topicList.forEach {
            val unSubRequest = MqttSubscribeRequest()
            unSubRequest.topic = it
            unSubRequest.isSubscribe = false
            LinkKit.getInstance().unsubscribe(unSubRequest, mCancelSubscribeListener)
        }
    }

    companion object {
        const val TAG = "mqtt"
        const val SUBSCRIBE_FAIL = "订阅失败"
        const val UNSUBSCRIBE_FAIL = "取消订阅失败"
        const val PUBLISH_FAIL = "发布失败"
    }
}