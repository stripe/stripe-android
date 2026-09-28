package com.stripe.android.paymentelement.embedded.manage

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedSelectionHolder
import com.stripe.android.paymentsheet.DefaultCustomerStateHolder
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.TestAutocompleteAddressInteractor
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.ui.DefaultUpdatePaymentMethodInteractor
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.ui.core.elements.BillingAddressElement
import com.stripe.android.uicore.elements.AutocompleteAddressElement
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import javax.inject.Provider

internal class EmbeddedUpdateScreenInteractorFactoryTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `saved card update form receives autocomplete factory`() = runTest {
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            hasCustomerConfiguration = true,
            canUpdateCardExpiryAndBillingDetails = true,
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
            ),
        )
        val selectionHolder = DefaultEmbeddedSelectionHolder(SavedStateHandle())
        val eventReporter = FakeEventReporter()
        val customerStateHolder = DefaultCustomerStateHolder(
            customerMetadata = stateFlowOf(paymentMethodMetadata.customerMetadata),
            paymentMethodMetadataFlow = stateFlowOf(paymentMethodMetadata),
            savedStateHandle = SavedStateHandle(),
            selection = selectionHolder.selection,
        )
        val autocompleteAddressInteractorFactory = TestAutocompleteAddressInteractor.noOpFactory()
        val factory = DefaultEmbeddedUpdateScreenInteractorFactory(
            savedPaymentMethodMutatorProvider = Provider { error("Not expected") },
            paymentMethodMetadata = paymentMethodMetadata,
            customerStateHolder = customerStateHolder,
            selectionHolder = selectionHolder,
            eventReporter = eventReporter,
            embeddedNavigatorProvider = Provider { error("Not expected") },
            autocompleteAddressInteractorFactory = autocompleteAddressInteractorFactory,
        )

        val interactor = factory.createUpdateScreenInteractor(
            PaymentMethodFixtures.displayableCard()
        ) as DefaultUpdatePaymentMethodInteractor
        val billingAddressElement = interactor.editCardDetailsInteractor.state.value.billingDetailsForm
            ?.addressSectionElement
            ?.fields
            ?.single() as BillingAddressElement

        assertThat(billingAddressElement.addressElement)
            .isInstanceOf(AutocompleteAddressElement::class.java)

        interactor.close()
        eventReporter.validate()
    }
}
