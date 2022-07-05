package com.zhushenwudi.libiot.mqtt.linksdk

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import com.aliyun.alink.dm.api.DeviceInfo
import com.aliyun.alink.dm.api.IoTApiClientConfig
import com.aliyun.alink.linkkit.api.*
import com.aliyun.alink.linksdk.cmp.connect.hubapi.HubApiRequest
import com.aliyun.alink.linksdk.cmp.core.listener.IConnectNotifyListener
import com.aliyun.alink.linksdk.cmp.core.listener.IConnectSendListener
import com.aliyun.alink.linksdk.id2.Id2ItlsSdk
import com.aliyun.alink.linksdk.tmp.device.payload.ValueWrapper
import com.aliyun.alink.linksdk.tools.AError
import java.util.*

object LinkUtil {

    /**
     * 如果需要动态注册设备获取设备的deviceSecret， 可以参考本接口实现。
     * 动态注册条件检测：
     * 1.云端开启该设备动态注册功能；
     * 2.首先在云端创建 pk，dn；
     *
     * @param context       上下文
     * @param productKey    产品类型
     * @param deviceName    设备名称 需要现在云端创建
     * @param productSecret 产品密钥
     * @param listener      密钥请求回调
     */
    fun registerDevice(
        context: Context?,
        productKey: String?,
        deviceName: String?,
        productSecret: String?,
        listener: IConnectSendListener?
    ) {
        val myDeviceInfo = DeviceInfo()
        myDeviceInfo.productKey = productKey
        myDeviceInfo.deviceName = deviceName
        myDeviceInfo.productSecret = productSecret
        val params = LinkKitInitParams()
        val config = IoTApiClientConfig()
        config.domain = "iot-auth-global.aliyuncs.com"
        params.connectConfig = config
        // 如果明确需要切换域名，可以设置 connectConfig 中 domain 的值；
        params.deviceInfo = myDeviceInfo
        val hubApiRequest = HubApiRequest()
        hubApiRequest.path = "/auth/register/device"
        // 调用动态注册接口
        LinkKit.getInstance().deviceRegister(context, params, hubApiRequest, listener)
    }

    /**
     * Android 设备端 SDK 初始化示例代码
     *
     * @param context       上下文
     * @param productKey    产品类型
     * @param deviceName    设备名称
     * @param deviceSecret  设备密钥
     * @param productSecret 产品密钥
     * @param callback      初始化建联结果回调
     */
    @SuppressLint("HardwareIds")
    fun init(
        context: Context?,
        productKey: String,
        deviceName: String?,
        deviceSecret: String,
        productSecret: String?,
        callback: ILinkKitConnectListener
    ) {
        // 构造三元组信息对象
        val deviceInfo = DeviceInfo()
        // 产品类型
        deviceInfo.productKey = productKey
        // 设备名称
        deviceInfo.deviceName = deviceName
        // 设备密钥
        deviceInfo.deviceSecret = deviceSecret
        // 产品密钥
        deviceInfo.productSecret = productSecret
        //  全局默认域名
        val userData = IoTApiClientConfig()
        // 设备的一些初始化属性，可以根据云端的注册的属性来设置。
        /**
         * 物模型初始化的初始值
         * 如果这里什么属性都不填，物模型就没有当前设备相关属性的初始值。
         * 用户调用物模型上报接口之后，物模型会有相关数据缓存。
         * 用户根据时间情况设置
         */
        val propertyValues: Map<String, ValueWrapper<*>> = HashMap()
        val params = LinkKitInitParams()
        params.deviceInfo = deviceInfo
        params.propertyValues = propertyValues
        params.connectConfig = userData
        /**
         * 如果用户需要设置域名
         */
        val ioTH2Config = IoTH2Config()
        ioTH2Config.clientId = Build.SERIAL + System.currentTimeMillis()
        ioTH2Config.endPoint = "https://" + productKey + ioTH2Config.endPoint
        params.iotH2InitParams = ioTH2Config
        Id2ItlsSdk.init(context)
        /**
         * 慎用：如不清楚是否要设置，可以不设置该参数
         * Mqtt 相关参数设置
         * 域名、产品密钥、认证安全模式等；
         */
        val clientConfig = IoTMqttClientConfig(productKey, deviceName, deviceSecret)
        // 对应 receiveOfflineMsg = !cleanSession, 默认不接受离线消息 false
        clientConfig.receiveOfflineMsg = true //cleanSession=0 接受离线消息
        clientConfig.receiveOfflineMsg = false //cleanSession=1 不接受离线消息
        clientConfig.channelHost = "$productKey.iot-as-mqtt.cn-shanghai.aliyuncs.com:1883" //线上
        // 设置 mqtt 请求域名，默认"{pk}.iot-as-mqtt.cn-shanghai.aliyuncs.com:1883" ,如果无具体的业务需求，请不要设置。
        // 文件配置测试 itls
        if ("itls_secret" == deviceSecret) {
            clientConfig.channelHost = "$productKey.itls.cn-shanghai.aliyuncs.com:1883" //线上
            clientConfig.productSecret = productSecret
            clientConfig.secureMode = 8
        }
        params.mqttClientConfig = clientConfig
        /**
         * 设备是否支持被飞燕平台APP发现
         * 需要确保开发的APP具备发现该类型设备的权限
         */
        val ioTDMConfig = IoTDMConfig()
        // 是否启用本地通信功能，默认不开启，
        // 启用之后会初始化本地通信CoAP相关模块，设备将允许被生活物联网平台的应用发现、绑定、控制，依赖enableThingModel开启
        ioTDMConfig.enableLocalCommunication = false
        // 是否启用物模型功能，如果不开启，本地通信功能也不支持
        // 默认不开启，开启之后init方法会等到物模型初始化（包含请求云端物模型）完成之后才返回onInitDone
        ioTDMConfig.enableThingModel = false
        // 是否启用网关功能
        // 默认不开启，开启之后，初始化的时候会初始化网关模块，获取云端网关子设备列表
        ioTDMConfig.enableGateway = false
        params.ioTDMConfig = ioTDMConfig
        /**
         * 设备初始化建联
         * onError 初始化建联失败，如果因网络问题导致初始化失败，需要用户重试初始化
         * onInitDone 初始化成功
         */
        LinkKit.getInstance().init(context, params, object : ILinkKitConnectListener {
            override fun onError(error: AError?) {
                callback.onError(error)
            }

            override fun onInitDone(data: Any?) {
                callback.onInitDone(data)
            }
        })
    }

    /**
     * LinkSDK 反初始化
     */
    fun deInit(notifyListener: IConnectNotifyListener?) {
        try {
            LinkKit.getInstance().unRegisterOnPushListener(notifyListener)
            LinkKit.getInstance().deinit()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}