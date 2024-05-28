package com.stripe.android.paymentsheet.verticalmode

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.definitions.CardDefinition
import com.stripe.android.lpmfoundations.paymentmethod.formElements
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.FormPage
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.ui.FORM_ELEMENT_TEST_TAG
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.ui.core.elements.events.LocalCardNumberCompletedEventReporter
import com.stripe.android.utils.FakeLinkConfigurationCoordinator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class VerticalModeFormUITest {
    @get:Rule
    val composeRule = createComposeRule()

    private val formPage = FormPage(composeRule)

    @Test
    fun testWithCardProperties() = runScenario(createCardState()) {
        viewActionRecorder.consume(VerticalModeFormInteractor.ViewAction.FormFieldValuesChanged(null))
        assertThat(viewActionRecorder.viewActions).isEmpty()
        formPage.cardNumber.performTextInput("4242")
        viewActionRecorder.consume(VerticalModeFormInteractor.ViewAction.FieldInteraction)
        assertThat(viewActionRecorder.viewActions).isEmpty()
        formPage.cardNumber.performTextReplacement("4242424242424242")
        assertThat(viewActionRecorder.viewActions).isEmpty()
        formPage.fillOutCardDetails(fillOutCardNumber = false)
        assertThat(viewActionRecorder.viewActions).hasSize(1)
        val viewAction = viewActionRecorder.viewActions[0]
            as VerticalModeFormInteractor.ViewAction.FormFieldValuesChanged
        assertThat(viewAction.formValues).isNotNull()
    }

    @Test
    fun testLpmWithNoFieldsTriggersFormFieldValuesChangedOnInitialization() = runScenario(createCashAppPayState()) {
        assertThat(viewActionRecorder.viewActions).hasSize(1)
        val viewAction = viewActionRecorder.viewActions[0]
            as VerticalModeFormInteractor.ViewAction.FormFieldValuesChanged
        assertThat(viewAction.formValues).isNotNull()
    }

    private fun runScenario(
        initialState: VerticalModeFormInteractor.State,
        block: Scenario.() -> Unit
    ) {
        val stateFlow = MutableStateFlow(initialState)
        val viewActionRecorder = ViewActionRecorder()
        val interactor = createInteractor(stateFlow, viewActionRecorder)

        composeRule.setContent {
            CompositionLocalProvider(
                LocalCardNumberCompletedEventReporter provides { }
            ) {
                VerticalModeFormUI(interactor)
            }
        }

        composeRule.onNodeWithTag(FORM_ELEMENT_TEST_TAG).assertExists()

        Scenario(stateFlow, viewActionRecorder).apply(block)
    }

    private fun createInteractor(
        stateFlow: StateFlow<VerticalModeFormInteractor.State> = MutableStateFlow(createCardState()),
        viewActionRecorder: ViewActionRecorder = ViewActionRecorder(),
    ): VerticalModeFormInteractor {
        return object : VerticalModeFormInteractor {
            override val state: StateFlow<VerticalModeFormInteractor.State> = stateFlow

            override fun handleViewAction(viewAction: VerticalModeFormInteractor.ViewAction) {
                viewActionRecorder.record(viewAction)
            }
        }
    }

    private fun createCardState(): VerticalModeFormInteractor.State {
        return VerticalModeFormInteractor.State(
            selectedPaymentMethodCode = PaymentMethod.Type.Card.code,
            isProcessing = false,
            usBankAccountFormArguments = mock(),
            formArguments = FormArguments(
                paymentMethodCode = PaymentMethod.Type.Card.code,
                cbcEligibility = CardBrandChoiceEligibility.Ineligible,
                merchantName = "Example, Inc.",
                amount = Amount(1000, "USD"),
                billingDetails = null,
                shippingDetails = null,
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(),
            ),
            formElements = CardDefinition.formElements(),
            linkSignupMode = null,
            linkConfigurationCoordinator = FakeLinkConfigurationCoordinator(),
        )
    }

    private fun createCashAppPayState(): VerticalModeFormInteractor.State {
        return VerticalModeFormInteractor.State(
            selectedPaymentMethodCode = PaymentMethod.Type.CashAppPay.code,
            isProcessing = false,
            usBankAccountFormArguments = mock(),
            formArguments = FormArguments(
                paymentMethodCode = PaymentMethod.Type.CashAppPay.code,
                cbcEligibility = CardBrandChoiceEligibility.Ineligible,
                merchantName = "Example, Inc.",
                amount = Amount(1000, "USD"),
                billingDetails = null,
                shippingDetails = null,
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(),
            ),
            formElements = emptyList(),
            linkSignupMode = null,
            linkConfigurationCoordinator = FakeLinkConfigurationCoordinator(),
        )
    }

    private class ViewActionRecorder {
        private val _viewActions: MutableList<VerticalModeFormInteractor.ViewAction> = mutableListOf()
        val viewActions: List<VerticalModeFormInteractor.ViewAction>
            get() = _viewActions.toList()

        fun record(viewAction: VerticalModeFormInteractor.ViewAction) {
            _viewActions += viewAction
        }

        fun consume(viewAction: VerticalModeFormInteractor.ViewAction) {
            assertThat(_viewActions.size).isGreaterThan(0)
            assertThat(_viewActions[0]).isEqualTo(viewAction)
            _viewActions.removeAt(0)
        }
    }

    private data class Scenario(
        val stateFlow: MutableStateFlow<VerticalModeFormInteractor.State>,
        val viewActionRecorder: ViewActionRecorder,
    )
}
