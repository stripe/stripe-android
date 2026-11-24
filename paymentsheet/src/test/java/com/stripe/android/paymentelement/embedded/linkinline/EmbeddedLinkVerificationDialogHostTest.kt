package com.stripe.android.paymentelement.embedded.linkinline

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import com.google.common.truth.Truth.assertThat
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.createComposeCleanupRule
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class EmbeddedLinkVerificationDialogHostTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    @Test
    fun `composed host can present the dialog`() = runTest {
        val linkInlineAuth = FakeEmbeddedLinkInlineAuth()

        composeRule.setContent {
            EmbeddedLinkVerificationDialogHost(linkInlineAuth)
        }
        composeRule.waitForIdle()

        val isReady = linkInlineAuth.registerDialogHostCalls.awaitItem()
        assertThat(isReady()).isTrue()
        linkInlineAuth.ensureAllEventsConsumed()
    }

    @Test
    fun `host leaving composition stops presenting the dialog`() = runTest {
        val linkInlineAuth = FakeEmbeddedLinkInlineAuth()
        var isComposed by mutableStateOf(true)

        composeRule.setContent {
            if (isComposed) {
                EmbeddedLinkVerificationDialogHost(linkInlineAuth)
            }
        }
        composeRule.waitForIdle()
        linkInlineAuth.registerDialogHostCalls.awaitItem()

        isComposed = false
        composeRule.waitForIdle()

        linkInlineAuth.closeDialogHostCalls.awaitItem()
        linkInlineAuth.ensureAllEventsConsumed()
    }
}
