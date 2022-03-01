package com.stripe.android

import android.os.Environment
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

        this.mDefaultScreenshotPath = File(
            File(
                InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(
                    Environment.DIRECTORY_PICTURES
                ),
                "payment_sheet_instrumentation_tests",
            ).absolutePath,
            "screenshots/$date/"
        )
        Log.d("STRIPE", mDefaultScreenshotPath.absolutePath)
    }

    override fun getFilename(prefix: String): String = prefix
}
