package com.stripe.android.paymentelement.embedded.sheet

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentsheet.FakeSelectSavedPaymentMethodsInteractor
import com.stripe.android.paymentsheet.model.MandateText
import com.stripe.android.paymentsheet.ui.PRIMARY_BUTTON_TEST_TAG
import com.stripe.android.paymentsheet.ui.PrimaryButtonIcon
import com.stripe.android.paymentsheet.ui.PrimaryButtonProcessingState
import com.stripe.android.paymentsheet.ui.SHEET_ERROR_TEST_TAG
import com.stripe.android.paymentsheet.ui.SHEET_MANDATE_TEST_TAG
import com.stripe.android.testing.createComposeCleanupRule
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class HorizontalSavedPaymentOptionsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @Test
    fun `mandate text is shown below error when showAbove is false`() = runScenario(
        mandateText = MandateText(text = "some text".resolvableString, showAbovePrimaryButton = false),
    ) {
        assertThat(positionOf(SHEET_MANDATE_TEST_TAG)).isGreaterThan(positionOf(SHEET_ERROR_TEST_TAG))
        assertThat(positionOf(SHEET_MANDATE_TEST_TAG)).isGreaterThan(positionOf(PRIMARY_BUTTON_TEST_TAG))

        mandateText.value = null
        composeRule.onNodeWithTag(SHEET_MANDATE_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun `mandate text is shown above error when showAbove is true`() = runScenario(
        mandateText = MandateText(text = "some text".resolvableString, showAbovePrimaryButton = true),
    ) {
        assertThat(positionOf(SHEET_MANDATE_TEST_TAG)).isLessThan(positionOf(SHEET_ERROR_TEST_TAG))
        assertThat(positionOf(SHEET_MANDATE_TEST_TAG)).isLessThan(positionOf(PRIMARY_BUTTON_TEST_TAG))

        mandateText.value = null
        composeRule.onNodeWithTag(SHEET_MANDATE_TEST_TAG).assertDoesNotExist()
    }

    private fun positionOf(tag: String): Float {
        return composeRule.onNodeWithTag(tag).assertIsDisplayed().fetchSemanticsNode().positionInRoot.y
    }

    private fun runScenario(
        mandateText: MandateText?,
        block: Scenario.() -> Unit,
    ) {
        val mandateTextFlow = MutableStateFlow(mandateText)
        val screen = EmbeddedNavigator.Screen.HorizontalSavedPaymentOptions(
            interactor = FakeSelectSavedPaymentMethodsInteractor(),
            sheetActivityState = stateFlowOf(
                SheetActivityStateHolder.State(
                    primaryButtonLabel = "Continue".resolvableString,
                    isEnabled = true,
                    processingState = PrimaryButtonProcessingState.Idle(null),
                    isProcessing = false,
                    primaryButtonIcon = PrimaryButtonIcon.None,
                    error = "error".resolvableString,
                )
            ),
            mandateText = mandateTextFlow,
            isContinueButtonVisible = stateFlowOf(true),
            onContinueClick = {},
            onPrimaryButtonDisabledClick = {},
        )

        composeRule.setContent {
            Column {
                screen.Content(showsWalletsHeader = false)
            }
        }

        Scenario(mandateText = mandateTextFlow).block()
    }

    private class Scenario(
        val mandateText: MutableStateFlow<MandateText?>,
    )
}
