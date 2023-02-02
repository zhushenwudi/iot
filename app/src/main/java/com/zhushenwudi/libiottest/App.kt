package com.zhushenwudi.libiottest

import android.app.Application
import android.os.Environment
import dev.utils.common.FileIOUtils
import dev.utils.common.FileUtils
import java.io.File

class App: Application() {

    override fun onCreate() {
        super.onCreate()
        val storagePath =
            Environment.getExternalStorageDirectory().absolutePath + File.separator.toString() + "group"
        if (FileUtils.isFileExists(storagePath)) {
            iotGroup = FileIOUtils.readFileToString(storagePath)
        }
    }

    companion object {
        var iotGroup: String = "group"
    }
}