package com.stripe.android.paymentsheet

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.ui.inline.InlineSignupViewState
import com.stripe.android.link.ui.inline.LinkSignupMode
import com.stripe.android.link.ui.inline.SignUpConsentAction
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.ui.PrimaryButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

class LinkInlineHandlerTest {
    private val incompleteInlineSignupViewState = InlineSignupViewState(
        userInput = null,
        merchantName = "Example, Inc.",
        signupMode = LinkSignupMode.InsteadOfSaveForFutureUse,
        fields = emptyList(),
        prefillEligibleFields = emptySet(),
        isExpanded = false,
    )

    private val filledOutInlineSignupViewState = InlineSignupViewState(
        userInput = UserInput.SignUp(
            email = "email@email.com",
            phone = "1234567890",
            country = "US",
            name = null,
            consentAction = SignUpConsentAction.Checkbox,
        ),
        merchantName = "Example, Inc.",
        signupMode = LinkSignupMode.InsteadOfSaveForFutureUse,
        fields = emptyList(),
        prefillEligibleFields = emptySet(),
        isExpanded = true,
    )

    @Test
    fun `initialization causes no side effects`() = runScenario {
        assertThat(linkInlineHandler).isNotNull()
    }

    @Test
    fun `onStateUpdated with valid selection updates LinkPrimaryButtonUiState`() {
        var updatedLinkPrimaryButtonUiState: PrimaryButton.UIState? = null
        runScenario(
            updateLinkPrimaryButtonUiState = { updatedLinkPrimaryButtonUiState = it },
        ) {
            selection.value = PaymentMethodFixtures.CARD_PAYMENT_SELECTION
            primaryButtonLabel.value = resolvableString("Continue")
            linkInlineHandler.onStateUpdated(filledOutInlineSignupViewState)
        }
        assertThat(updatedLinkPrimaryButtonUiState).isNotNull()
    }

    @Test
    fun `onStateUpdated with valid incomplete user input updates LinkPrimaryButtonUiState`() {
        var hasCalledUpdateLinkPrimaryButtonUiState = false
        runScenario(
            updateLinkPrimaryButtonUiState = {
                assertThat(it).isNull()
                hasCalledUpdateLinkPrimaryButtonUiState = true
            }
        ) {
            selection.value = PaymentMethodFixtures.CARD_PAYMENT_SELECTION
            primaryButtonLabel.value = resolvableString("Continue")
            linkInlineHandler.onStateUpdated(incompleteInlineSignupViewState)
        }
        assertThat(hasCalledUpdateLinkPrimaryButtonUiState).isTrue()
    }

    @Test
    fun `updating selection away from card and link removes custom primary state`() {
        var updatedLinkPrimaryButtonUiState: PrimaryButton.UIState? = null

        runScenario(
            updateLinkPrimaryButtonUiState = { updatedLinkPrimaryButtonUiState = it },
        ) {
            selection.value = PaymentMethodFixtures.CARD_PAYMENT_SELECTION
            primaryButtonLabel.value = resolvableString("Continue")

            linkInlineHandler.onStateUpdated(filledOutInlineSignupViewState)
            assertThat(updatedLinkPrimaryButtonUiState).isNotNull()

            selection.value = PaymentMethodFixtures.GENERIC_PAYMENT_SELECTION
            assertThat(updatedLinkPrimaryButtonUiState).isNull()
        }
    }

    @Test
    fun `updating selection to us bank does not reset custom primary state`() {
        var updatedLinkPrimaryButtonUiState: PrimaryButton.UIState? = null

        runScenario(
            updateLinkPrimaryButtonUiState = { updatedLinkPrimaryButtonUiState = it },
        ) {
            selection.value = PaymentMethodFixtures.CARD_PAYMENT_SELECTION
            primaryButtonLabel.value = resolvableString("Continue")

            linkInlineHandler.onStateUpdated(filledOutInlineSignupViewState)
            assertThat(updatedLinkPrimaryButtonUiState).isNotNull()

            selection.value = PaymentMethodFixtures.US_BANK_PAYMENT_SELECTION
            assertThat(updatedLinkPrimaryButtonUiState).isNotNull()
        }
    }

    @Test
    fun `calling onClick on updatedLinkPrimaryButtonUiState calls payWithLink`() {
        var updatedLinkPrimaryButtonUiState: PrimaryButton.UIState? = null
        var hasCalledPayWithLink = false
        runScenario(
            updateLinkPrimaryButtonUiState = { updatedLinkPrimaryButtonUiState = it },
            payWithLink = { userInput, paymentSelection, shouldCompleteLinkFlowInline ->
                assertThat(userInput).isEqualTo(filledOutInlineSignupViewState.userInput)
                assertThat(paymentSelection).isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
                assertThat(shouldCompleteLinkFlowInline).isTrue()
                hasCalledPayWithLink = true
            }
        ) {
            selection.value = PaymentMethodFixtures.CARD_PAYMENT_SELECTION
            primaryButtonLabel.value = resolvableString("Continue")
            linkInlineHandler.onStateUpdated(filledOutInlineSignupViewState)
            assertThat(updatedLinkPrimaryButtonUiState).isNotNull()
            assertThat(hasCalledPayWithLink).isFalse()
            updatedLinkPrimaryButtonUiState!!.onClick()
            assertThat(hasCalledPayWithLink).isTrue()
        }
    }

    private fun runScenario(
        updateLinkPrimaryButtonUiState: (PrimaryButton.UIState?) -> Unit = {
            throw AssertionError("Not implemented")
        },
        payWithLink: suspend (
            userInput: UserInput?,
            paymentSelection: PaymentSelection?,
            shouldCompleteLinkInlineFlow: Boolean,
        ) -> Unit = { _, _, _ -> throw AssertionError("Not implemented") },
        block: suspend Scenario.() -> Unit,
    ) {
        runTest {
            val selection: MutableStateFlow<PaymentSelection?> = MutableStateFlow(null)
            val primaryButtonLabel: MutableStateFlow<ResolvableString?> = MutableStateFlow(null)
            val linkInlineHandler = LinkInlineHandler(
                coroutineScope = CoroutineScope(UnconfinedTestDispatcher()),
                payWithLink = payWithLink,
                selection = selection,
                updateLinkPrimaryButtonUiState = updateLinkPrimaryButtonUiState,
                primaryButtonLabel = primaryButtonLabel,
                shouldCompleteLinkFlowInline = true,
            )
            Scenario(
                linkInlineHandler = linkInlineHandler,
                selection = selection,
                primaryButtonLabel = primaryButtonLabel,
            ).apply { block() }
        }
    }

    private class Scenario(
        val linkInlineHandler: LinkInlineHandler,
        val selection: MutableStateFlow<PaymentSelection?>,
        val primaryButtonLabel: MutableStateFlow<ResolvableString?>,
    )
}
