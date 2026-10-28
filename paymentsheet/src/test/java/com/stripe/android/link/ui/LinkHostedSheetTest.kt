package com.stripe.android.link.ui

import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import app.cash.turbine.Turbine
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.createComposeCleanupRule
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
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

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    @Test
    fun `hiding removes the sheet`() = runScenario {
        visible = false

        composeRule.onNodeWithTag(CONTENT_TAG).assertDoesNotExist()
    }

    @Test
    fun `tapping outside asks the caller to close instead of hiding the sheet`() = runScenario {
        composeRule.onNodeWithContentDescription("Close sheet").performClick()
        composeRule.waitForIdle()

        closeCalls.awaitItem()
        composeRule.onNodeWithTag(CONTENT_TAG).assertIsDisplayed()
    }

    @Test
    fun `tapping outside a sheet shown again while hiding asks the caller to close`() = runScenario {
        composeRule.mainClock.autoAdvance = false
        visible = false
        // Frame by frame, so the hide coroutine runs and the sheet is part-way down when shown again.
        repeat(HALF_HIDE_FRAMES) {
            composeRule.mainClock.advanceTimeByFrame()
            composeRule.waitForIdle()
        }
        visible = true
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(CONTENT_TAG).assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Close sheet").performClick()
        composeRule.waitForIdle()

        closeCalls.awaitItem()
    }

    private fun runScenario(block: suspend Scenario.() -> Unit) = runTest {
        val scenario = Scenario()
        composeRule.setContent {
            LinkHostedSheet(visible = scenario.visible, onClose = { scenario.closeCalls.add(Unit) }) {
                Text("Sheet content", Modifier.testTag(CONTENT_TAG))
            }
        }
        composeRule.onNodeWithTag(CONTENT_TAG).assertIsDisplayed()

        scenario.block()

        scenario.closeCalls.ensureAllEventsConsumed()
    }

    private class Scenario {
        var visible by mutableStateOf(true)
        val closeCalls = Turbine<Unit>()
    }

    private companion object {
        const val CONTENT_TAG = "LinkHostedSheetContent"

        // Partway through the hide animation.
        const val HALF_HIDE_FRAMES = 10
    }
}
