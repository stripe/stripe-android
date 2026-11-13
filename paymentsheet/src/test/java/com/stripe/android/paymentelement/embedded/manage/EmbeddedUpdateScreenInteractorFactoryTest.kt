package com.stripe.android.paymentelement.embedded.manage

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.sheet.SheetTaxRegionUpdater
import com.stripe.android.paymentsheet.DefaultCustomerStateHolder
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.TestAutocompleteAddressInteractor
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.paymentsheet.ui.DefaultUpdatePaymentMethodInteractor
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.ui.core.elements.BillingAddressElement
import com.stripe.android.uicore.elements.AutocompleteAddressElement
import com.stripe.android.uicore.elements.FormFieldId
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import javax.inject.Provider

@OptIn(CheckoutSessionPreview::class)
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
        val eventReporter = FakeEventReporter()
        val interactor = createInteractor(paymentMethodMetadata, eventReporter)
        val billingAddressElement = interactor.billingAddressElement()

        assertThat(billingAddressElement.addressElement)
            .isInstanceOf(AutocompleteAddressElement::class.java)

        interactor.close()
        eventReporter.validate()
    }

    @Test
    fun `saved card update form collects automatic tax fields when tax uses billing address`() = runTest {
        val checkoutSessionResponse = CheckoutSessionResponseFactory.create(
            automaticTaxEnabled = true,
            taxAddressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
        )
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            hasCustomerConfiguration = true,
            canUpdateCardExpiryAndBillingDetails = true,
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic,
            ),
            integrationMetadata = IntegrationMetadata.CheckoutSession(
                collectedEmail = null,
                instancesKey = "key",
                checkoutSessionResponse = checkoutSessionResponse,
            ),
        )
        val eventReporter = FakeEventReporter()
        val interactor = createInteractor(paymentMethodMetadata, eventReporter)
        val billingAddressElement = interactor.billingAddressElement()

        billingAddressElement.countryElement.controller.onRawValueChange("US")

        assertThat(billingAddressElement.hiddenIdentifiers.value).containsNoneOf(
            FormFieldId.Line1,
            FormFieldId.City,
            FormFieldId.State,
            FormFieldId.PostalCode,
        )

        interactor.close()
        eventReporter.validate()
    }

    private fun createInteractor(
        paymentMethodMetadata: PaymentMethodMetadata,
        eventReporter: FakeEventReporter,
    ): DefaultUpdatePaymentMethodInteractor {
        val selectionHolder = DefaultEmbeddedSelectionHolder(SavedStateHandle())
        val customerStateHolder = DefaultCustomerStateHolder(
            customerMetadata = stateFlowOf(paymentMethodMetadata.customerMetadata),
            paymentMethodMetadataFlow = stateFlowOf(paymentMethodMetadata),
            savedStateHandle = SavedStateHandle(),
            selection = selectionHolder.selection,
        )
        val factory = DefaultEmbeddedUpdateScreenInteractorFactory(
            savedPaymentMethodMutatorProvider = Provider { error("Not expected") },
            paymentMethodMetadata = paymentMethodMetadata,
            customerStateHolder = customerStateHolder,
            selectionHolder = selectionHolder,
            savedPaymentMethodSelector = ManageLaunchSavedPaymentMethodSelector(
                taxRegionUpdater = SheetTaxRegionUpdater { _, _, _ -> error("Not expected") },
                paymentMethodMetadata = paymentMethodMetadata,
                selectionHolder = selectionHolder,
            ),
            eventReporter = eventReporter,
            embeddedNavigatorProvider = Provider { error("Not expected") },
            autocompleteAddressInteractorFactory = TestAutocompleteAddressInteractor.noOpFactory(),
        )

        return factory.createUpdateScreenInteractor(
            PaymentMethodFixtures.displayableCard()
        ) as DefaultUpdatePaymentMethodInteractor
    }

    private fun DefaultUpdatePaymentMethodInteractor.billingAddressElement(): BillingAddressElement {
        return editCardDetailsInteractor.state.value.billingDetailsForm
            ?.addressSectionElement
            ?.fields
            ?.single() as BillingAddressElement
    }
}
