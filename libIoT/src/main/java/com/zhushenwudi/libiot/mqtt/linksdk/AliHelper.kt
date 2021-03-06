package com.zhushenwudi.libiot.mqtt.linksdk

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Environment
import android.util.Log
import com.aliyun.alink.dm.model.ResponseModel
import com.aliyun.alink.linkkit.api.ILinkKitConnectListener
import com.aliyun.alink.linkkit.api.LinkKit
import com.aliyun.alink.linksdk.channel.core.persistent.mqtt.MqttConfigure
import com.aliyun.alink.linksdk.cmp.core.base.AMessage
import com.aliyun.alink.linksdk.cmp.core.base.ARequest
import com.aliyun.alink.linksdk.cmp.core.base.AResponse
import com.aliyun.alink.linksdk.cmp.core.base.ConnectState
import com.aliyun.alink.linksdk.cmp.core.listener.IConnectNotifyListener
import com.aliyun.alink.linksdk.cmp.core.listener.IConnectSendListener
import com.aliyun.alink.linksdk.tools.AError
import com.zhushenwudi.libiot.AppUtils
import com.zhushenwudi.libiot.AppUtils.fromJson
import com.zhushenwudi.libiot.AppUtils.toJson
import com.zhushenwudi.libiot.model.*
import com.zhushenwudi.libiot.mqtt.MQTTHelper
import com.zhushenwudi.libiot.service.TickTimeReceiver
import dev.utils.app.ManifestUtils
import dev.utils.app.NetWorkUtils
import dev.utils.common.FileIOUtils.readFileToString
import dev.utils.common.FileIOUtils.writeFileFromString
import dev.utils.common.FileUtils.createOrExistsFile
import dev.utils.common.FileUtils.isFileExists
import kotlinx.coroutines.*
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

abstract class AliHelper(
    private val applicationContext: Context,
    private val productKey: String,
    private val productSecret: String,
    private val subscribeTopic: MutableList<String> = mutableListOf()
): MQTTHelper() {

    private var initial = true
    private var mqttConnected = false
    private val handleInit = AtomicBoolean()
    private var scope: CoroutineScope? = null
    private var timer: TickTimeReceiver? = null
    private var offlineStartTime = 0L
    private var mqttStatusCallback: ((status: Boolean) -> Unit)? = null

    private fun setMqttStatus(status: Boolean) {
        mqttConnected = status
        handleInit.set(false)
        mqttCallBack(status)
    }

    // ????????? MQTT ??????
    private val aliMQTT: AliMQTT by lazy {
        AliMQTT(
            onMQTTConnectFailed = { message, aError ->
                // mqtt ??????
                Log.e(TAG, "$message - ${aError.msg} - ${aError.code}")
                setMqttStatus(false)
            }
        )
    }

    // MQTT ????????????
    private val notifyListener by lazy {
        object : IConnectNotifyListener {
            override fun onNotify(connectId: String, topic: String, msg: AMessage) {
                respCallBack(topic, String((msg.data as ByteArray)))
            }

            /**
             * ??????topic
             * @param connectId ??????????????????????????????????????? connectId == ConnectSDK.getInstance().getPersistentConnectId()
             * @param topic ??????
             * @return true:?????????onNotify, false:????????????
             */
            override fun shouldHandle(connectId: String, topic: String): Boolean {
                return true
            }

            override fun onConnectStateChange(connectId: String, connectState: ConnectState) {
                when (connectState) {
                    ConnectState.CONNECTED -> {
                        setMqttStatus(true)
                        // ????????????
                        subscribeTopic.toTypedArray().run {
                            aliMQTT.subscribe(this)
                        }
                    }
                    ConnectState.DISCONNECTED -> {
                        setMqttStatus(false)
                    }
                    ConnectState.CONNECTING -> {}
                    ConnectState.CONNECTFAIL -> {
                        setMqttStatus(false)
                    }
                }
            }
        }
    }

    /**
     * ???????????????MQTT
     */
    final override fun initMqtt(mqttStatusCallback: ((status: Boolean) -> Unit)?) {
        topicHeader = SEPARATOR + productKey + TOPIC_BEHIND
        this.mqttStatusCallback = mqttStatusCallback
        if (initial) {
            initial = false
            subscribeTopic.add(topicHeader + "cmd")
            MqttConfigure.setKeepAliveInterval(30)
            MqttConfigure.automaticReconnect = false
        }
        if (handleInit.get()) {
            return
        }
        handleInit.set(true)
        loopPing()
        startTimer()
        val path = sdcardPath + "deviceSecret"
        if (isFileExists(path)) {
            LinkUtil.deInit(notifyListener)
            val deviceSecret = readFileToString(path)
            if (deviceSecret != null) {
                connectLink(productKey, productSecret, deviceSecret)
            }
        } else {
            registerDevice(productKey, productSecret, path)
        }
    }

    /**
     * ?????? Link
     */
    private fun connectLink(productKey: String, productSecret: String, deviceSecret: String) {
        LinkUtil.init(
            applicationContext,
            productKey,
            SERIAL,
            deviceSecret,
            productSecret,
            object : ILinkKitConnectListener {
                override fun onError(aError: AError) {
                    Log.d(TAG, "LINK SDK ????????????:  ${aError.msg} - ${aError.code}")
                }

                override fun onInitDone(p0: Any?) {
                    // ?????? MQTT ??????
                    LinkKit.getInstance().registerOnPushListener(notifyListener)
                }
            })
    }

    /**
     * ???????????? Link
     */
    private fun registerDevice(productKey: String, productSecret: String, path: String) {
        LinkUtil.registerDevice(
            applicationContext,
            productKey,
            SERIAL,
            productSecret,
            object : IConnectSendListener {
                override fun onResponse(aRequest: ARequest, aResponse: AResponse) {
                    Log.e(TAG, aResponse.data.toString())
                    val response =
                        fromJson<ResponseModel<Map<String, String>>>(aResponse.data.toString())
                    response?.apply {
                        if ("200" == code) {
                            val deviceSecret = data["deviceSecret"]
                            if (!deviceSecret.isNullOrEmpty()) {
                                createOrExistsFile(path)
                                writeFileFromString(path, deviceSecret)
                                connectLink(productKey, productSecret, deviceSecret)
                            }
                        } else {
                            Log.d(TAG, "LINK SDK ????????????: $code")
                        }
                    }
                }

                override fun onFailure(aRequest: ARequest, aError: AError) {
                    Log.d(TAG, "LINK SDK ????????????: ${aError.msg} - ${aError.code}")
                }
            })
    }

    /**
     * ?????? MQTT
     */
    private fun loopPing() {
        if (scope == null) {
            scope = CoroutineScope(Dispatchers.IO)
            scope?.launch {
                delay(3000)
                while (true) {
                    // ??????mqtt????????????
                    while (!mqttConnected) {
                        withTimeoutOrNull(TIMEOUT) {
                            if (NetWorkUtils.isAvailableByPing()) {
                                initMqtt(mqttStatusCallback)
                            } else {
                                Log.d("aaa", "net disconnect...")
                            }
                            delay(1000)
                        }
                    }
                    delay(1000)
                }
            }
        }
    }

    /**
     * mqtt????????????
     */
    final override fun mqttCallBack(status: Boolean) {
        mqttStatusCallback?.invoke(status)
        if (status) {
            versionUp(ManifestUtils.getAppVersionName())
            if (offlineStartTime != 0L) {
                netOfflineUp(offlineStartTime)
                offlineStartTime = 0L
            }
        } else {
            offlineStartTime = System.currentTimeMillis()
        }
    }

    /**
     * ?????????????????????
     */
    private fun startTimer() {
        if (timer == null) {
            timer = TickTimeReceiver {
                heartBeatUp()
            }
            applicationContext.registerReceiver(timer, IntentFilter(Intent.ACTION_TIME_TICK))
        }
    }

    /**
     * ????????????
     */
    final override fun versionUp(versionName: String) {
        val postVersion = topicHeader + "version"
        val message = toJson(VersionUp(data = VersionUp.DataBean(versionName)))
        publish(postVersion, message)
    }

    /**
     * ????????????
     */
    final override fun netOfflineUp(start: Long) {
        val postOffline = topicHeader + "offline"
        val message = toJson(Offline(data = Offline.DataBean(start = start, end = System.currentTimeMillis())))
        publish(postOffline, message)
    }

    /**
     * ????????????
     */
    final override fun heartBeatUp() {
        val postHeardBeats = topicHeader + "heartbeat"
        val message = toJson(HeartBeatUp(data = AppUtils.genHeartBeatDataBean(applicationContext)))
        publish(postHeardBeats, message)
    }

    /**
     * ?????? topic ??????
     */
    final override fun subscribe(topicList: Array<String>) {
        subscribeTopic.addAll(topicList)
        aliMQTT.subscribe(topicList)
    }

    /**
     * ?????? topic ??????
     */
    final override fun publish(topic: String, msg: String) {
        aliMQTT.publish(topic, msg)
    }

    /**
     * ????????????
     *
     * @param topics ???????????????
     */
    final override fun cancelSubscribe(topics: Array<String>) {
        aliMQTT.cancelSubscribe(topics)
    }

    /**
     * ?????? mqtt ??????
     */
    final override fun release() {
        try {
            aliMQTT.cancelSubscribe(subscribeTopic.toTypedArray())
        } catch (e: Exception) {
        } finally {
            handleInit.set(false)
            initial = true
            subscribeTopic.clear()
            LinkUtil.deInit(notifyListener)
            scope?.cancel()
        }
        try {
            applicationContext.unregisterReceiver(timer)
        } catch (e: Exception) {
        } finally {
            timer = null
        }
    }

    companion object {
        const val TAG = "mqtt"
        private val SEPARATOR: String = File.separator
        private val sdcardPath = Environment.getExternalStorageDirectory().absolutePath + SEPARATOR
        const val TIMEOUT = 5000L
        private val SERIAL = Build.SERIAL
        val TOPIC_BEHIND = SEPARATOR + SERIAL + SEPARATOR + "user" + SEPARATOR + "group" + SEPARATOR
    }
}