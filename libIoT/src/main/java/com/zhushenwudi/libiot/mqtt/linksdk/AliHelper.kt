package com.zhushenwudi.libiot.mqtt.linksdk

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Environment
import android.util.Log
import com.aliyun.alink.dm.api.IOta
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
import com.google.gson.JsonObject
import com.zhushenwudi.libiot.AppUtils
import com.zhushenwudi.libiot.AppUtils.fromJson
import com.zhushenwudi.libiot.AppUtils.toJson
import com.zhushenwudi.libiot.model.*
import com.zhushenwudi.libiot.mqtt.MQTTHelper
import com.zhushenwudi.libiot.service.TickTimeReceiver
import dev.utils.app.ADBUtils
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
    private val queryLogFunc: ((start: Long, end: Long) -> ArrayList<String>)? = null,
    private val subscribeTopic: MutableSet<String> = mutableSetOf()
) : MQTTHelper(), IOta.OtaListener {

    private var initial = true
    private var mqttConnected = false
    private val handleInit = AtomicBoolean()
    private var scope: CoroutineScope? = null
    private var timer: TickTimeReceiver? = null
    private var offlineStartTime = 0L
    private var mqttStatusCallback: ((status: Boolean) -> Unit)? = null
    private var mOta: IOta? = null

    lateinit var cmdTopic: String
    private lateinit var ntpTopic: String

    private fun setMqttStatus(status: Boolean) {
        mqttConnected = status
        handleInit.set(false)
        mqttCallBack(status)
    }

    // 阿里云 MQTT 单例
    private val aliMQTT: AliMQTT by lazy {
        AliMQTT(
            onMQTTConnectFailed = { message, aError ->
                // mqtt 断开
                Log.e(TAG, "$message - ${aError.msg} - ${aError.code}")
                setMqttStatus(false)
            }
        )
    }

    // MQTT 接收回调
    private val notifyListener by lazy {
        object : IConnectNotifyListener {
            override fun onNotify(connectId: String, topic: String, msg: AMessage) {
                respCallBack(topic, String((msg.data as ByteArray)))
            }

            /**
             * 过滤topic
             * @param connectId 连接类型，这里判断是否长链 connectId == ConnectSDK.getInstance().getPersistentConnectId()
             * @param topic 主题
             * @return true:回调到onNotify, false:事件拦截
             */
            override fun shouldHandle(connectId: String, topic: String): Boolean {
                return true
            }

            override fun onConnectStateChange(connectId: String, connectState: ConnectState) {
                when (connectState) {
                    ConnectState.CONNECTED -> {
                        setMqttStatus(true)
                        // 订阅指令
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
     * 初始化阿里MQTT
     */
    final override fun initMqtt(mqttStatusCallback: ((status: Boolean) -> Unit)?) {
        topicHeader = "/$productKey/$SERIAL/user/group/"
        this.mqttStatusCallback = mqttStatusCallback
        if (initial) {
            initial = false
            cmdTopic = topicHeader?.replace("group/", "") + "cmd"
            ntpTopic = "/ext/ntp/${productKey}/$SERIAL/response"
            subscribeTopic.add(cmdTopic)
            subscribeTopic.add(ntpTopic)
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
     * 连接 Link
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
                    Log.d(TAG, "LINK SDK 连接失败:  ${aError.msg} - ${aError.code}")
                }

                override fun onInitDone(p0: Any?) {
                    // 注册 MQTT 服务
                    LinkKit.getInstance().registerOnPushListener(notifyListener)
                    // 配置 OTA 服务
                    configOTA()
                    // 向ntp服务器请求
                    val obj = JsonObject()
                    obj.addProperty("deviceSendTime", System.currentTimeMillis())
                    publish("/ext/ntp/${productKey}/${SERIAL}/request", toJson(obj))
                }
            })
    }

    /**
     * 动态注册 Link
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
                            Log.d(TAG, "LINK SDK 注册失败: $code")
                        }
                    }
                }

                override fun onFailure(aRequest: ARequest, aError: AError) {
                    Log.d(TAG, "LINK SDK 注册失败: ${aError.msg} - ${aError.code}")
                }
            })
    }

    /**
     * 连接 MQTT
     */
    private fun loopPing() {
        if (scope == null) {
            scope = CoroutineScope(Dispatchers.IO)
            scope?.launch {
                delay(3000)
                while (true) {
                    // 如果mqtt没有连上
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
     * mqtt状态回调
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
     * 启动心跳定时器
     */
    private fun startTimer() {
        if (timer == null) {
            timer = TickTimeReceiver(heartBeatInterval) {
                heartBeatUp()
            }
            applicationContext.registerReceiver(timer, IntentFilter(Intent.ACTION_TIME_TICK))
        }
    }

    /**
     * 配置 OTA
     */
    fun configOTA() {
        mOta = LinkKit.getInstance().ota
        val config = IOta.OtaConfig()
        LinkUtil.configOTA(applicationContext, config)
        mOta?.tryStartOta(config, this)
    }

    override fun onOtaProgress(step: Int, otaResult: IOta.OtaResult): Boolean {
        return LinkUtil.observeOTA(step, otaResult, mOta)
    }

    /**
     * 版本上报
     */
    final override fun versionUp(versionName: String) {
        val postVersion = topicHeader + "version"
        val message = toJson(VersionUp(data = VersionUp.DataBean(versionName), group = group))
        publish(postVersion, message)
    }

    /**
     * 断线上报
     */
    final override fun netOfflineUp(start: Long) {
        val postOffline = topicHeader + "offline"
        val message = toJson(
            Offline(
                data = Offline.DataBean(start = start, end = System.currentTimeMillis()),
                group = group
            )
        )
        publish(postOffline, message)
    }

    /**
     * 心跳上报
     */
    final override fun heartBeatUp() {
        val postHeardBeats = topicHeader + "heartbeat"
        val message = toJson(
            HeartBeatUp(
                data = AppUtils.genHeartBeatDataBean(applicationContext),
                group = group
            )
        )
        publish(postHeardBeats, message)
    }

    /**
     * 订阅 topic 消息
     */
    final override fun subscribe(topicList: Array<String>) {
        subscribeTopic.addAll(topicList)
        aliMQTT.subscribe(topicList)
    }

    /**
     * 发布 topic 消息
     */
    final override fun publish(topic: String, msg: String) {
        aliMQTT.publish(topic, msg)
    }

    /**
     * 取消订阅
     *
     * @param topics 多主题数组
     */
    final override fun cancelSubscribe(topics: Array<String>) {
        aliMQTT.cancelSubscribe(topics)
    }

    /**
     * 释放 mqtt 连接
     */
    final override fun release() {
        // 尝试结束 OTA
        mOta?.tryStopOta()
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

    override fun respCallBack(topic: String, message: String) {
        try {
            when (topic) {
                ntpTopic -> {
                    val dr = System.currentTimeMillis()
                    val resp = fromJson<TimestampResp>(message)
                    resp?.run {
                        val sr = serverRecvTime.toLongOrNull()
                        val ss = serverSendTime.toLongOrNull()
                        val ds = deviceSendTime.toLongOrNull()
                        if (sr != null && ss != null && ds != null) {
                            val trulyTimestamp = (sr + ss + dr - ds) / 2
                            ADBUtils.setSystemTime2(trulyTimestamp)
                        }
                    }
                }

                cmdTopic -> {
                    fromJson<CommandReq<Any>>(message)?.run {
                        when (cmd) {
                            "QueryLog" -> {
                                fromJson<CommandReq<CommandReq.LogReqBean>>(message)?.run {
                                    data?.run {
                                        val begin = begin.toLongOrNull() ?: 0
                                        val end = end.toLongOrNull() ?: 0

                                        commandResp(id = id, cmd = cmd, status = "OK")
                                        val logs = queryLogFunc?.invoke(begin, end)
                                        queryLog(
                                            id = id,
                                            begin = begin,
                                            end = end,
                                            logs = logs ?: arrayListOf()
                                        )
                                    }
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        } catch (ignored: Exception) {
        }
    }

    companion object {
        const val TAG = "mqtt"
        private val SEPARATOR: String = File.separator
        private val sdcardPath = Environment.getExternalStorageDirectory().absolutePath + SEPARATOR
        const val TIMEOUT = 5000L
        private val SERIAL = Build.SERIAL
    }
}