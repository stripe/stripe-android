package com.stripe.android

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.screenshot.BasicScreenCaptureProcessor
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

class MyScreenCaptureProcessor : BasicScreenCaptureProcessor() {

    init {
        val pattern = "yyyy-MM-dd-HH-mm-ss"
        val simpleDateFormat = SimpleDateFormat(pattern)
        val date: String = simpleDateFormat.format(Date())

        // Path is /data/user/0/com.stripe.android.paymentsheet.example/files/screenshots-yyyy-MM-dd-HH-mm-ss/
        this.mDefaultScreenshotPath = File(
            InstrumentationRegistry.getInstrumentation().targetContext.filesDir,
            "screenshots-$date/"
        )
        Log.d("STRIPE", mDefaultScreenshotPath.absolutePath)
    }

    override fun getFilename(prefix: String): String = prefix
}
