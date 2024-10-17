package com.stripe.android.view

import android.webkit.ConsoleMessage
import com.google.common.truth.Truth.assertThat
import com.stripe.android.testing.FakeLogger
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class PaymentAuthWebChromeClientTest {

    private val logger = FakeLogger()
    private val webChromeClient = PaymentAuthWebChromeClient(
        mock(),
        logger
    )

    @Test
    fun `onConsoleMessage should write to logger`() {
        webChromeClient.onConsoleMessage(
            ConsoleMessage("hello world", "", 0, ConsoleMessage.MessageLevel.DEBUG)
        )
        assertThat(logger.debugLogs)
            .containsExactly("hello world")
    }
}
