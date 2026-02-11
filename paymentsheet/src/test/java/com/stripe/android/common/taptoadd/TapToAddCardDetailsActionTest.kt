package com.stripe.android.common.taptoadd

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class TapToAddCardDetailsActionTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `clicking button calls startPaymentMethodCollection`() = runTest {
        FakeTapToAddHelper.test {
            val action = TapToAddCardDetailsAction(tapToAddHelper = helper)

            composeTestRule.setContent {
                action.Content(enabled = true)
            }

            composeTestRule.onNodeWithText("Tap to add card").performClick()

            assertThat(collectCalls.awaitItem()).isEqualTo(Unit)
        }
    }

    @Test
    fun `clicking disabled button does not call startPaymentMethodCollection`() = runTest {
        FakeTapToAddHelper.test {
            val action = TapToAddCardDetailsAction(tapToAddHelper = helper)

            composeTestRule.setContent {
                action.Content(enabled = false)
            }

            composeTestRule.onNodeWithText("Tap to add card").performClick()

            // No collect calls should have been made
            collectCalls.expectNoEvents()
        }
    }
}
