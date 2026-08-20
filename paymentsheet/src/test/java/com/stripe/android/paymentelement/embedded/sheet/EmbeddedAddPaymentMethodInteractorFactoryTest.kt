package com.stripe.android.paymentelement.embedded.sheet

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.taptoadd.FakeTapToAddHelper
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.EmbeddedFormHelperFactory
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentsheet.DefaultCustomerStateHolder
import com.stripe.android.paymentsheet.addresselement.TestAutocompleteAddressInteractor
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.ui.AddPaymentMethodInteractor
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
import kotlin.test.Test

internal class EmbeddedAddPaymentMethodInteractorFactoryTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `exposes the metadata's sorted supported payment methods`() = runScenario {
        assertThat(interactor.state.value.supportedPaymentMethods)
            .isEqualTo(paymentMethodMetadata.sortedSupportedPaymentMethods())
    }

    @Test
    fun `is not live mode for a test intent`() = runScenario {
        assertThat(interactor.isLiveMode).isFalse()
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

    private fun runScenario(
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "cashapp"),
            ),
        ),
        initialSelection: PaymentSelection? = null,
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
        val factory = EmbeddedAddPaymentMethodInteractorFactory(
            paymentMethodMetadata = paymentMethodMetadata,
            embeddedSelectionHolder = selectionHolder,
            embeddedFormHelperFactory = embeddedFormHelperFactory,
            viewModelScope = viewModelScope,
            sheetActivityStateHolder = FakeSheetActivityStateHolder(),
            tapToAddHelper = FakeTapToAddHelper.noOp(),
            eventReporter = FakeEventReporter(),
            paymentMethodMessagePromotionsHelper = FakePaymentMethodMessagePromotionsHelper(),
            customerStateHolder = customerStateHolder,
            autocompleteAddressInteractorFactory = autocompleteAddressInteractorFactory,
            launchMode = EmbeddedLaunchMode.Form("card"),
        )
        val interactor = factory.create()

        Scenario(
            interactor = interactor,
            paymentMethodMetadata = paymentMethodMetadata,
            autocompleteAddressInteractorFactory = autocompleteAddressInteractorFactory,
        ).apply { block() }

        interactor.close()
        viewModelScope.cancel()
    }

    private data class Scenario(
        val interactor: AddPaymentMethodInteractor,
        val paymentMethodMetadata: PaymentMethodMetadata,
        val autocompleteAddressInteractorFactory: AutocompleteAddressInteractor.Factory,
    )
}
