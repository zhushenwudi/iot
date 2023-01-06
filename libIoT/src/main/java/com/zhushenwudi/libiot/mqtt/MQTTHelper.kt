package com.zhushenwudi.libiot.mqtt

import com.zhushenwudi.libiot.AppUtils
import com.zhushenwudi.libiot.model.CommandResp
import com.zhushenwudi.libiot.model.ErrorEventUp
import com.zhushenwudi.libiot.model.LogUp

abstract class MQTTHelper {
    var heartBeatInterval = 1
    var topicHeader: String? = null
    var group: String = "group"

    abstract fun initMqtt(mqttStatusCallback: ((status: Boolean) -> Unit)? = null)

    abstract fun versionUp(versionName: String, sku: String)
    abstract fun netOfflineUp(start: Long)
    abstract fun heartBeatUp()

    abstract fun subscribe(topicList: Array<String>)
    abstract fun publish(topic: String, msg: String)
    abstract fun cancelSubscribe(topics: Array<String>)

    abstract fun respCallBack(topic: String, message: String)
    abstract fun mqttCallBack(status: Boolean)

    abstract fun release()

    /**
     * 反向控制响应
     * @param id     控制id
     * @param cmd    控制命令
     * @param status 满足可控条件
     */
    fun commandResp(id: String, cmd: String, status: String) {
        val topic = topicHeader?.replace("group/", "") + "cmd/response"
        publish(
            topic, AppUtils.toJson(
                CommandResp(
                    id = id,
                    data = CommandResp.DataBean(
                        cmd = cmd,
                        status = status
                    )
                )
            )
        )
    }

    /**
     * 抓取时间段内日志
     */
    fun queryLog(id: String, begin: Long, end: Long, logs: ArrayList<String>) {
        val postData = topicHeader + "data"
        val message = AppUtils.toJson(
            LogUp(
                id = id,
                data = LogUp.DataBean(begin = begin, end = end, logs = logs),
                group = group
            )
        )
        publish(postData, message)
    }

    /**
     * 外设异常事件上报
     * @param code       错误代码
     * @param peripheral 硬件类型
     * @param msg        错误信息
     */
    fun errorEventUp(code: Int, peripheral: String, msg: String) {
        val postErrorEvent = topicHeader + "event"
        val message = AppUtils.toJson(
            ErrorEventUp(
                data = ErrorEventUp.DataBean(
                    code = code,
                    peripheral = peripheral,
                    msg = msg
                ), group = group
            )
        )
        publish(postErrorEvent, message)
    }
}