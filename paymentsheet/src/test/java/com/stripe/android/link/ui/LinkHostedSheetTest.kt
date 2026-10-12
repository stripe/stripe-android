package com.stripe.android.link.ui

import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.stripe.android.testing.createComposeCleanupRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class LinkHostedSheetTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @Test
    fun `hiding removes the sheet`() = runScenario {
        visible = false

        composeRule.onNodeWithTag(CONTENT_TAG).assertDoesNotExist()
    }

    @Test
    fun `tapping outside a sheet shown again while hiding closes it`() = runScenario {
        composeRule.mainClock.autoAdvance = false
        visible = false
        composeRule.mainClock.advanceTimeBy(HALF_HIDE_MILLIS)
        visible = true
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Close sheet").performClick()
        composeRule.waitForIdle()

        assertThat(closeCount).isEqualTo(1)
    }

    private fun runScenario(block: Scenario.() -> Unit) {
        val scenario = Scenario()
        composeRule.setContent {
            LinkHostedSheet(visible = scenario.visible, onClose = { scenario.closeCount++ }) {
                Text("Sheet content", Modifier.testTag(CONTENT_TAG))
            }
        }
        composeRule.onNodeWithTag(CONTENT_TAG).assertExists()
        scenario.block()
    }

    private class Scenario {
        var visible by mutableStateOf(true)
        var closeCount = 0
    }

    private companion object {
        const val CONTENT_TAG = "LinkHostedSheetContent"

        // Partway through the hide animation.
        const val HALF_HIDE_MILLIS = 150L
    }
}
