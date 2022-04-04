package com.stripe.android.test.core

import androidx.test.runner.screenshot.BasicScreenCaptureProcessor
import java.io.File

class MyScreenCaptureProcessor(
    directory: File = testArtifactDirectoryOnDevice
) : BasicScreenCaptureProcessor() {

    init {
        this.mDefaultScreenshotPath = directory
    }

    override fun getFilename(prefix: String): String = prefix
}
