package com.zhushenwudi.libiottest

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.zhushenwudi.libiot.BuildConfig
import com.zhushenwudi.libiot.mqtt.MQTTHelper
import com.zhushenwudi.libiot.mqtt.linksdk.AliHelper
import com.zhushenwudi.libiot.R
import com.zhushenwudi.libiot.mqtt.emq.EMQHelper

class MainActivity : AppCompatActivity() {

    private lateinit var aliHelper: MQTTHelper
    private lateinit var emqHelper: MQTTHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        aliHelper = MyAliHelper(
            applicationContext = applicationContext,
            productKey = BuildConfig.LINK_PRODUCT_KEY,
            productSecret = BuildConfig.LINK_PRODUCT_SECRET
        )
        aliHelper.initMqtt{
            Log.e("aaa", "status: $it")
        }

//        emqHelper = MyEMQHelper(
//            applicationContext = applicationContext,
//            productKey = BuildConfig.EMQ_PRODUCT_KEY,
//            url = BuildConfig.EMQ_URL
//        )
//        emqHelper.initMqtt()
    }

    override fun onStop() {
        super.onStop()
        if (this::aliHelper.isInitialized) {
            aliHelper.release()
        }
        if (this::emqHelper.isInitialized) {
            emqHelper.release()
        }
    }
}