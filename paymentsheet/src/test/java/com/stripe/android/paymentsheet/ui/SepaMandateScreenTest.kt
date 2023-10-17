package com.stripe.android.paymentsheet.ui

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
internal class SepaMandateScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun testClick() {
        val clickCountDownLatch = CountDownLatch(1)
        composeRule.setContent {
            SepaMandateScreen(merchantName = "Example, Inc.") {
                clickCountDownLatch.countDown()
            }
        }

        composeRule.onNode(hasTestTag("SEPA_MANDATE_CONTINUE_BUTTON")).performClick()

        assertThat(clickCountDownLatch.await(1, TimeUnit.SECONDS)).isTrue()
    }
}
