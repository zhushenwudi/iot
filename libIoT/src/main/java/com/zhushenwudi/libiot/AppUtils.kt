package com.zhushenwudi.libiot

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Process
import com.google.gson.Gson
import com.zhushenwudi.libiot.model.HeartBeatUp
import dev.utils.app.MemoryUtils
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader

object AppUtils {
    val gson = Gson()

    // json 字符串 -> 对象
    inline fun <reified T : Any> fromJson(json: String): T? {
        return gson.fromJson(json, T::class.java)
    }

    // 对象 -> json 字符串
    inline fun <reified T : Any> toJson(obj: T): String {
        return gson.toJson(obj)
    }

    fun genHeartBeatDataBean(context: Context): HeartBeatUp.DataBean {
        var rssi = 0
        var module = "wifi"
        when (getNetConnectionType(context)) {
            0 -> {
                module = "ethernet"
            }
            1 -> {
                val wifiManager =
                    context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                rssi = wifiManager.connectionInfo.rssi
            }
        }

        return HeartBeatUp.DataBean(
            rssi = rssi,
            module = module,
            cpu = getProcessCpuRate(),
            memory = getMemoryRate()
        )
    }

    @SuppressLint("MissingPermission")
    fun getNetConnectionType(context: Context): Int {
        val connectManager =
            context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val ethNetInfo = connectManager.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET)
        val wifiNetInfo = connectManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)

        if (ethNetInfo != null && ethNetInfo.isConnected) {
            return 0
        }
        if (wifiNetInfo != null && wifiNetInfo.isConnected) {
            return 1
        }
        return -1
    }

    private fun getProcessCpuRate(): String {
        val result = try {
            val totalCpuTime1 = getTotalCpuTime().toFloat()
            val processCpuTime1 = getAppCpuTime().toFloat()
            try {
                Thread.sleep(360) //sleep一段时间
            } catch (e: Exception) {
            }
            val totalCpuTime2 = getTotalCpuTime().toFloat()
            val processCpuTime2 = getAppCpuTime().toFloat()
            100 * (processCpuTime2 - processCpuTime1) / (totalCpuTime2 - totalCpuTime1)
        } catch (e: Exception) {
            e.printStackTrace()
            -1f
        }
        return String.format("%.2f", result)
    }

    // 获取系统总CPU使用时间
    private fun getTotalCpuTime(): Long {
        var result: Long = 0
        try {
            val reader = BufferedReader(
                InputStreamReader(
                    FileInputStream("/proc/stat")
                ), 1000
            )
            val load: String = reader.readLine()
            reader.close()
            val cpuInfos = load.split(" ").toTypedArray()
            result = cpuInfos[2].toLong() + cpuInfos[3].toLong() + cpuInfos[4]
                .toLong() + cpuInfos[6].toLong() + cpuInfos[5].toLong() + cpuInfos[7]
                .toLong() + cpuInfos[8].toLong()
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
        return result
    }

    // 获取应用占用的CPU时间
    private fun getAppCpuTime(): Long {
        var result: Long = 0
        try {
            val pid = Process.myPid()
            val reader = BufferedReader(
                InputStreamReader(
                    FileInputStream("/proc/$pid/stat")
                ), 1000
            )
            val load: String = reader.readLine()
            reader.close()
            val cpuInfos = load.split(" ").toTypedArray()
            result =
                cpuInfos[13].toLong() + cpuInfos[14].toLong() + cpuInfos[15].toLong() + cpuInfos[16].toLong()
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
        return result
    }

    private fun getMemoryRate(): String {
        val result = try {
            val total = MemoryUtils.getTotalMemory().toFloat()
            val avail = MemoryUtils.getAvailMemory().toFloat()
            100 * (total - avail) / avail
        } catch (e: Exception) {
            e.printStackTrace()
            -1f
        }
        return String.format("%.2f", result)
    }
}