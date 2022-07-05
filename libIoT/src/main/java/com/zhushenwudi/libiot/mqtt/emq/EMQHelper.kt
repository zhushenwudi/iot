package com.zhushenwudi.libiot.mqtt.emq

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Environment
import android.util.Log
import com.zhushenwudi.libiot.AppUtils
import com.zhushenwudi.libiot.AppUtils.toJson
import com.zhushenwudi.libiot.model.CommandDown
import com.zhushenwudi.libiot.model.HeartBeatUp
import com.zhushenwudi.libiot.model.Offline
import com.zhushenwudi.libiot.model.VersionUp
import com.zhushenwudi.libiot.mqtt.MQTTHelper
import com.zhushenwudi.libiot.service.TickTimeReceiver
import dev.utils.app.ManifestUtils
import dev.utils.common.FileIOUtils
import dev.utils.common.FileUtils
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import java.io.File

open class EMQHelper(
    private val applicationContext: Context,
    private val productKey: String,
    private val url: String,
    private val username: String? = "public",
    private val password: String? = "admin",
    private val subscribeTopic: MutableList<String> = mutableListOf()
): MQTTHelper() {

    private var initial = true
    var mqttClient: MqttAndroidClient? = null
    private var timer: TickTimeReceiver? = null
    private var offlineStartTime = 0L
    private val topicHeader = File.separator + productKey + File.separator + SERIAL + "/user/group" + File.separator

    /**
     * 连接 MQTT
     */
    override fun initMqtt() {
        try {
            if (initial) {
                initial = false
                subscribeTopic.add(topicHeader + "cmd")
            }
            if (mqttClient != null) {
                return
            }
            mqttClient = MqttAndroidClient(applicationContext, url, SERIAL)
            mqttClient?.setCallback(object : MqttCallbackExtended {
                override fun connectionLost(cause: Throwable?) {
                    cause?.printStackTrace()
                    mqttCallBack(false)
                }

                override fun messageArrived(topic: String, message: MqttMessage) {
                    respCallBack(topic, message.toString())
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                }

                override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                    mqttCallBack(true)
                    subscribeTopic.toTypedArray().run {
                        subscribe(this)
                    }
                }
            })

            val mqttConnectOptions = MqttConnectOptions()
            // 配置用户名密码
            username?.run { mqttConnectOptions.userName = this }
            password?.run { mqttConnectOptions.password = toCharArray() }
            // 设置心跳
            mqttConnectOptions.keepAliveInterval = 20
            // 设置自动重连
            mqttConnectOptions.isAutomaticReconnect = true
            // 设置建立连接时清空会话
            mqttConnectOptions.isCleanSession = true
            mqttConnectOptions.connectionTimeout = 10

            mqttClient?.connect(mqttConnectOptions, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {

                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    if (exception.toString().contains("已连接客户机")) {
                        mqttCallBack(true)
                    } else {
                        mqttCallBack(false)
                        exception?.printStackTrace()
                    }
                }
            })

            startTimer()
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    /**
     * mqtt状态回调
     */
    override fun mqttCallBack(status: Boolean) {
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
     * 处理接到的消息
     */
    override fun respCallBack(topic: String, message: String) {
        Log.d(TAG, "$topic - $message")
        val command = AppUtils.fromJson<CommandDown>(message)
        command?.let { c ->
            when (c.cmd) {
                "SetGroup" -> {
                    // 设置 Group
                    val group = c.data?.group
                    group?.run {
                        val path = sdcardPath + "group"
                        FileUtils.createOrExistsFile(path)
                        FileIOUtils.writeFileFromString(path, this)
                    }
                }
                else -> {}
            }
        }
    }

    /**
     * 启动心跳定时器
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
     * 订阅多主题
     * @param topics 主题数组
     * @param qos 连接方式
     */
    private fun subscribeList(topics: Array<String>, qos: IntArray? = null) {
        val qosArray = qos?.asList() ?: kotlin.run {
            val temp = arrayListOf<Int>()
            topics.map {
                temp.add(0)
            }
            temp
        }
        try {
            mqttClient?.subscribe(topics, qosArray.toIntArray(), null, null)
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    /**
     * 版本上报
     */
    override fun versionUp(versionName: String) {
        publish(topicHeader + "version", toJson(VersionUp(data = VersionUp.DataBean(versionName, productKey), group = "dev")))
    }

    /**
     * 断线上报
     */
    override fun netOfflineUp(start: Long) {
        publish(topicHeader + "offline", toJson(Offline(data = Offline.DataBean(start = start, end = System.currentTimeMillis()), group = "dev")))
    }

    /**
     * 心跳上报
     */
    override fun heartBeatUp() {
        val message = toJson(HeartBeatUp(data = AppUtils.genHeartBeatDataBean(applicationContext), group = "dev"))
        publish(topicHeader + "heartbeat", message)
    }

    /**
     * 订阅 topic 消息
     */
    override fun subscribe(topicList: Array<String>) {
        subscribeList(topicList)
    }

    /**
     * 发布 topic 消息
     */
    override fun publish(topic: String, msg: String) {
        try {
            val message = MqttMessage()
            message.payload = msg.toByteArray()
            Log.d(TAG, "$topic -- $message")
            mqttClient?.publish(topic, message)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 取消订阅
     *
     * @param topics 多主题数组
     */
    override fun cancelSubscribe(topics: Array<String>) {
        topics.forEach { topic ->
            try {
                mqttClient?.unsubscribe(topic)
            } catch (e: MqttException) {
            }
        }
    }

    /**
     * 释放 mqtt 连接
     */
    override fun release() {
        try {
            cancelSubscribe(subscribeTopic.toTypedArray())
        } catch (e: Exception) {
        }
        try {
            mqttClient?.disconnect()
            mqttClient = null
        } catch (e: MqttException) {
        } finally {
            initial = true
            subscribeTopic.clear()
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
        private val SERIAL = Build.SERIAL
        val sdcardPath = Environment.getExternalStorageDirectory().absolutePath + File.separator
    }
}