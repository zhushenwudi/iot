package com.zhushenwudi.libiot.service

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import java.util.*

@SuppressLint("InvalidWakeLockTag", "SimpleDateFormat")
class TickTimeReceiver(private val callback: () -> Unit) : BroadcastReceiver() {
    private var scope: CoroutineScope? = null

    override fun onReceive(context: Context, intent: Intent) {
        Log.e(TAG, "--- time_tick ---")
        scope?.cancel()
        scope = CoroutineScope(Dispatchers.IO).apply {
            val cal = Calendar.getInstance()
            val min = cal[Calendar.MINUTE]
            if (min % 5 == 0) {
                callback()
            }
        }
    }

    companion object {
        const val TAG = "mqtt"
    }
}