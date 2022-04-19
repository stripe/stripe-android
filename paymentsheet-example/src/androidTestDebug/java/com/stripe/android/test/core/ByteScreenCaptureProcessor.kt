package com.stripe.android.test.core

import androidx.test.runner.screenshot.BasicScreenCaptureProcessor
import androidx.test.runner.screenshot.ScreenCapture
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayOutputStream
import java.io.OutputStream

class ByteScreenCaptureProcessor() : BasicScreenCaptureProcessor() {
    val byteArrayOutputStream = ByteArrayOutputStream()

    override fun process(capture: ScreenCapture): String {
        capture.bitmap.compress(capture.format, 100, byteArrayOutputStream)
        return ""
    }
    private fun getBytes(): ByteArray {
        return byteArrayOutputStream.toByteArray()
    }

    fun compare(screenCaptureProcessor: ByteScreenCaptureProcessor){
        assertThat(getBytes()).isEqualTo(screenCaptureProcessor.getBytes())
    }
}
