package com.stripe.android.paymentsheet.ui

import android.os.Build
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.stripe.android.testing.createComposeCleanupRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
internal class SepaMandateScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @Test
    fun testContinueClick() {
        val clickCountDownLatch = CountDownLatch(1)
        composeRule.setContent {
            SepaMandateScreen(
                merchantName = "Example, Inc.",
                acknowledgedCallback = { clickCountDownLatch.countDown() },
                closeCallback = { throw AssertionError("No expected") },
            )
        }

        composeRule.onNode(hasTestTag("SEPA_MANDATE_CONTINUE_BUTTON")).performClick()

        assertThat(clickCountDownLatch.await(1, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun testCloseClick() {
        val clickCountDownLatch = CountDownLatch(1)
        composeRule.setContent {
            SepaMandateScreen(
                merchantName = "Example, Inc.",
                acknowledgedCallback = { throw AssertionError("No expected") },
                closeCallback = { clickCountDownLatch.countDown() },
            )
        }

        composeRule.onNode(hasTestTag("SEPA_MANDATE_CLOSE_BUTTON")).performClick()

        assertThat(clickCountDownLatch.await(1, TimeUnit.SECONDS)).isTrue()
    }
}
