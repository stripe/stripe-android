package com.stripe.android.testing

import androidx.compose.ui.test.junit4.ComposeTestRule
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock

internal class ComposeTestRuleExtensionsTest {
    @Test
    fun `waitUntilWithIdle waits for idle before polling the condition`() {
        val composeTestRule = mock<ComposeTestRule>()
        val condition = { true }
        val conditionCaptor = argumentCaptor<() -> Boolean>()

        composeTestRule.waitUntilWithIdle(condition)

        inOrder(composeTestRule) {
            verify(composeTestRule).waitForIdle()
            verify(composeTestRule).waitUntil(eq(5_000), conditionCaptor.capture())
        }
        assertThat(conditionCaptor.firstValue()).isTrue()
    }

    @Test
    fun `waitUntilWithIdle forwards the condition description`() {
        val composeTestRule = mock<ComposeTestRule>()
        val condition = { true }
        val conditionCaptor = argumentCaptor<() -> Boolean>()

        composeTestRule.waitUntilWithIdle("condition description", condition)

        inOrder(composeTestRule) {
            verify(composeTestRule).waitForIdle()
            verify(composeTestRule).waitUntil(
                eq("condition description"),
                eq(5_000),
                conditionCaptor.capture(),
            )
        }
        assertThat(conditionCaptor.firstValue()).isTrue()
    }
}
