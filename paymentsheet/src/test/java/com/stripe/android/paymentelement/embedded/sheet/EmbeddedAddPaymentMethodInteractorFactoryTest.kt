package com.stripe.android.paymentelement.embedded.sheet

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.common.taptoadd.FakeTapToAddHelper
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.TestFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.EmbeddedFormHelperFactory
import com.stripe.android.paymentsheet.DefaultCustomerStateHolder
import com.stripe.android.paymentsheet.addresselement.TestAutocompleteAddressInteractor
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.model.PaymentMethodIncentive
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.ui.AddPaymentMethodInteractor
import com.stripe.android.paymentsheet.ui.PrimaryButtonIcon
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import com.stripe.android.uicore.utils.stateFlowOf
import com.stripe.android.utils.FakeIsNfcScanningAvailable
import com.stripe.android.utils.FakeLinkConfigurationCoordinator
import com.stripe.android.utils.FakePaymentMethodMessagePromotionsHelper
import com.stripe.android.utils.NullCardAccountRangeRepositoryFactory
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.runner.RunWith
import kotlin.test.Test

@RunWith(TestParameterInjector::class)
internal class EmbeddedAddPaymentMethodInteractorFactoryTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `exposes the metadata's sorted supported payment methods`() = runScenario {
        assertThat(interactor.state.value.supportedPaymentMethods)
            .isEqualTo(paymentMethodMetadata.sortedSupportedPaymentMethods())
    }

    @Test
    fun `initial code defaults to the first supported payment method when there is no new selection`() = runScenario {
        assertThat(interactor.state.value.selectedPaymentMethodCode)
            .isEqualTo(paymentMethodMetadata.supportedPaymentMethodTypes().first())
    }

    @Test
    fun `initial code seeds from the current new selection`() = runScenario(
        initialSelection = PaymentSelection.New.GenericPaymentMethod(
            label = "Cash App Pay".resolvableString,
            iconResource = 0,
            iconResourceNight = null,
            lightThemeIconUrl = null,
            darkThemeIconUrl = null,
            paymentMethodCreateParams = PaymentMethodCreateParams.createCashAppPay(),
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest,
        ),
    ) {
        assertThat(interactor.state.value.selectedPaymentMethodCode).isEqualTo("cashapp")
    }

    @Test
    fun `initial code seeds from the current custom selection`() = runScenario(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            displayableCustomPaymentMethods = listOf(PaymentMethodFixtures.PAYPAL_CUSTOM_PAYMENT_METHOD),
        ),
        initialSelection = PaymentMethodFixtures.createCustomPaymentMethod(
            PaymentMethodFixtures.PAYPAL_CUSTOM_PAYMENT_METHOD,
        ),
    ) {
        assertThat(interactor.state.value.selectedPaymentMethodCode).isEqualTo("cpmt_paypal")
    }

    @Test
    fun `initial code seeds from the current external selection`() = runScenario(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            externalPaymentMethodSpecs = listOf(PaymentMethodFixtures.PAYPAL_EXTERNAL_PAYMENT_METHOD_SPEC),
        ),
        initialSelection = PaymentMethodFixtures.createExternalPaymentMethod(
            PaymentMethodFixtures.PAYPAL_EXTERNAL_PAYMENT_METHOD_SPEC,
        ),
    ) {
        assertThat(interactor.state.value.selectedPaymentMethodCode).isEqualTo("external_paypal")
    }

    @Test
    fun `OnPaymentMethodSelected updates the selected code`() = runScenario {
        interactor.handleViewAction(
            AddPaymentMethodInteractor.ViewAction.OnPaymentMethodSelected("cashapp")
        )

        assertThat(interactor.state.value.selectedPaymentMethodCode).isEqualTo("cashapp")
    }

    @Test
    fun `US bank account arguments receive autocomplete factory`() = runScenario {
        assertThat(interactor.state.value.usBankAccountFormArguments.autocompleteAddressInteractorFactory)
            .isSameInstanceAs(autocompleteAddressInteractorFactory)
    }

    @Test
    fun `US bank account arguments receive the sheet primary button icon`(
        @TestParameter primaryButtonIcon: PrimaryButtonIcon,
    ) = runScenario(primaryButtonIcon = primaryButtonIcon) {
        val arguments = interactor.state.value.usBankAccountFormArguments
        assertThat(arguments.isCompleteFlow).isFalse()
        arguments.primaryButtonIcon.test {
            assertThat(awaitItem()).isEqualTo(primaryButtonIcon)
        }
    }

    @Test
    fun `linked bank account removes incentive`() {
        val incentive = PaymentMethodIncentive(
            identifier = "link_instant_debits",
            displayText = "$5",
        )

        runScenario(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf("link"),
                ),
                linkState = LinkState(
                    configuration = TestFactory.LINK_CONFIGURATION_WITH_INSTANT_DEBITS_ONBOARDING,
                    loginState = LinkState.LoginState.LoggedOut,
                    signupMode = null,
                ),
                paymentMethodIncentive = incentive,
            ),
        ) {
            interactor.state.test {
                val initialState = awaitItem()
                assertThat(initialState.incentive).isEqualTo(incentive)

                initialState.usBankAccountFormArguments.onLinkedBankAccountChanged(
                    PaymentMethodFixtures.US_BANK_PAYMENT_SELECTION
                )

                awaitItem()
                assertThat(awaitItem().incentive).isNull()
                assertThat(sheetActivityStateHolder.updateErrorTurbine.awaitItem()).isNull()
            }
        }
    }

    private fun runScenario(
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "cashapp"),
            ),
        ),
        initialSelection: PaymentSelection? = null,
        primaryButtonIcon: PrimaryButtonIcon = PrimaryButtonIcon.Lock,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        // A separate scope so the interactor's never-completing state collectors don't keep runTest from finishing.
        val viewModelScope = TestScope(UnconfinedTestDispatcher())
        val selectionHolder = DefaultEmbeddedSelectionHolder(SavedStateHandle()).apply {
            setSelection(initialSelection)
        }
        val embeddedFormHelperFactory = EmbeddedFormHelperFactory(
            linkConfigurationCoordinator = FakeLinkConfigurationCoordinator(),
            embeddedSelectionHolder = selectionHolder,
            cardAccountRangeRepositoryFactory = NullCardAccountRangeRepositoryFactory,
            savedStateHandle = SavedStateHandle(),
            isNfcScanningAvailable = FakeIsNfcScanningAvailable(result = false),
        )
        val customerStateHolder = DefaultCustomerStateHolder(
            customerMetadata = stateFlowOf(null),
            paymentMethodMetadataFlow = stateFlowOf(paymentMethodMetadata),
            savedStateHandle = SavedStateHandle(),
            selection = selectionHolder.selection,
        )
        val autocompleteAddressInteractorFactory = TestAutocompleteAddressInteractor.noOpFactory()
        val sheetActivityStateHolder = FakeSheetActivityStateHolder().apply {
            updateState { it.copy(primaryButtonIcon = primaryButtonIcon) }
        }
        val factory = EmbeddedAddPaymentMethodInteractorFactory(
            paymentMethodMetadata = paymentMethodMetadata,
            embeddedSelectionHolder = selectionHolder,
            embeddedFormHelperFactory = embeddedFormHelperFactory,
            viewModelScope = viewModelScope,
            sheetActivityStateHolder = sheetActivityStateHolder,
            tapToAddHelper = FakeTapToAddHelper.noOp(),
            eventReporter = FakeEventReporter(),
            paymentMethodMessagePromotionsHelper = FakePaymentMethodMessagePromotionsHelper(),
            customerStateHolder = customerStateHolder,
            autocompleteAddressInteractorFactory = autocompleteAddressInteractorFactory,
        )
        val interactor = factory.create()
        assertThat(sheetActivityStateHolder.updateErrorTurbine.awaitItem()).isNull()

        Scenario(
            interactor = interactor,
            sheetActivityStateHolder = sheetActivityStateHolder,
            paymentMethodMetadata = paymentMethodMetadata,
            autocompleteAddressInteractorFactory = autocompleteAddressInteractorFactory,
        ).apply { block() }

        sheetActivityStateHolder.validate()
        interactor.close()
        viewModelScope.cancel()
    }

    private data class Scenario(
        val interactor: AddPaymentMethodInteractor,
        val sheetActivityStateHolder: FakeSheetActivityStateHolder,
        val paymentMethodMetadata: PaymentMethodMetadata,
        val autocompleteAddressInteractorFactory: AutocompleteAddressInteractor.Factory,
    )
}
