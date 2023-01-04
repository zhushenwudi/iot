package com.zhushenwudi.libiot.service

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.zhushenwudi.libiot.mqtt.MQTTHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import java.util.*

@SuppressLint("InvalidWakeLockTag", "SimpleDateFormat")
class TickTimeReceiver(private val heartBeatInterval: Int, private val callback: () -> Unit) : BroadcastReceiver() {
    private var scope: CoroutineScope? = null

    override fun onReceive(context: Context, intent: Intent) {
        scope?.cancel()
        scope = CoroutineScope(Dispatchers.IO).apply {
            val cal = Calendar.getInstance()
            val min = cal[Calendar.MINUTE]
            if (heartBeatInterval > 0 && min % heartBeatInterval == 0) {
                callback()
            }
        }
    }
}