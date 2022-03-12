package com.stripe.android.test.core

import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.screenshot.Screenshot
import androidx.test.uiautomator.UiDevice
import org.junit.runner.Description
import java.io.File
import java.io.IOException

class TestWatcher : org.junit.rules.TestWatcher() {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val processor = MyScreenCaptureProcessor()

    override fun finished(description: Description?) {
        val filename = "error-${description?.testClass?.simpleName}-${description?.methodName}"

        // close out of any open browsers.
        setOf(Browser.Chrome, Browser.Firefox).forEach {
            while (AuthorizeWindow.exists(device, it)) {
                device.pressBack()
            }
        }

        // Close paymentsheet if open
        device.pressBack()
        device.pressBack()
        device.pressBack()

        val capture2 = Screenshot.capture()
        capture2.name = "$filename-verifyPaymentSheetClosed"
        capture2.format = Bitmap.CompressFormat.PNG

        try {
            capture2.process(setOf(processor))
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
