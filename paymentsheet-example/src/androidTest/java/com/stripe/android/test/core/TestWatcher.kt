package com.stripe.android.test.core

import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.screenshot.Screenshot
import androidx.test.uiautomator.UiDevice
import com.stripe.android.test.core.ui.BrowserUI
import com.stripe.android.test.core.ui.Selectors
import org.junit.runner.Description
import java.io.File
import java.io.IOException

class TestWatcher : org.junit.rules.TestWatcher() {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val processor = MyScreenCaptureProcessor()

    override fun finished(description: Description?) {
        super.finished(description)
        val filename = "info-${description?.testClass?.simpleName}-${description?.methodName}"

        // close out of any open browsers.
        BrowserUI.values().forEach {
            var isFound = true
            while (isFound) {
                isFound = Selectors.browserWindow(device, it)?.exists() == true
                device.pressBack()
            }
        }

        // Close paymentsheet if open
        device.pressBack()

        val verifyFinalScreen = Screenshot.capture()
        verifyFinalScreen.name = "$filename-verifyPaymentSheetClosed"
        verifyFinalScreen.format = Bitmap.CompressFormat.PNG

        try {
            verifyFinalScreen.process(setOf(processor))
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun failed(e: Throwable, description: Description) {
        super.failed(e, description)

        val filename = "error-${description.testClass.simpleName}-${description.methodName}"

        val capture = Screenshot.capture()
        capture.name = filename
        capture.format = Bitmap.CompressFormat.PNG

        try {
            capture.process(setOf(processor))
        } catch (e: IOException) {
            e.printStackTrace()
        }

        device.dumpWindowHierarchy(File(testArtifactDirectoryOnDevice, "$filename-window"))
    }
}
