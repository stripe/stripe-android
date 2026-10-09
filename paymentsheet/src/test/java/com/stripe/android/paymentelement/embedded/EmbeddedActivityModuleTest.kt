package com.stripe.android.paymentelement.embedded

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.embedded.manage.ManageLaunchSavedPaymentMethodSelector
import com.stripe.android.paymentelement.embedded.sheet.SheetTaxRegionUpdater
import com.stripe.android.paymentsheet.addresselement.AUTOCOMPLETE_DEFAULT_COUNTRIES
import com.stripe.android.paymentsheet.addresselement.BillingInlineAutocompleteAddressInteractor
import com.stripe.android.paymentsheet.addresselement.FakeStripeAutocompleteRepository
import com.stripe.android.paymentsheet.addresselement.PaymentElementAutocompleteAddressInteractor
import com.stripe.android.paymentsheet.addresselement.analytics.FakeAddressLauncherEventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(CheckoutSessionPreview::class)
internal class EmbeddedActivityModuleTest {

    @Test
    fun `autocomplete factory uses Stripe-hosted inline interactor when enabled`() = runScenario(
        shouldUseAutocompleteProxyEndpoints = true,
    ) {
        assertThat(interactor).isInstanceOf(BillingInlineAutocompleteAddressInteractor::class.java)
        assertThat(interactor.autocompleteConfig.googlePlacesApiKey).isNull()
        assertThat(interactor.autocompleteConfig.autocompleteCountries)
            .isEqualTo(AUTOCOMPLETE_DEFAULT_COUNTRIES)
        assertThat(interactor.autocompleteConfig.isInlineAutocompleteEnabled).isTrue()
        assertThat(interactor.autocompleteConfig.shouldUseStripeHostedAutocomplete).isTrue()
    }

    @Test
    fun `autocomplete factory falls back to manual interactor when Stripe-hosted autocomplete is disabled`() =
        runScenario(
            shouldUseAutocompleteProxyEndpoints = false,
        ) {
            assertThat(interactor).isInstanceOf(PaymentElementAutocompleteAddressInteractor::class.java)
            assertThat(interactor.autocompleteConfig.googlePlacesApiKey).isNull()
            assertThat(interactor.autocompleteConfig.shouldUseStripeHostedAutocomplete).isFalse()
        }

    @Test
    fun `Manage launch provides the Manage launch selector`() {
        val selectionHolder = DefaultEmbeddedSelectionHolder(SavedStateHandle())
        val manageLaunchSelector = ManageLaunchSavedPaymentMethodSelector(
            taxRegionUpdater = SheetTaxRegionUpdater { _, _, _ ->
                error("Tax update is not invoked by this selector test")
            },
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            selectionHolder = selectionHolder,
        )

        val selector = EmbeddedActivityModule.provideManageScreenSavedPaymentMethodSelector(
            launchMode = EmbeddedLaunchMode.Manage,
            selectionHolder = selectionHolder,
            manageLaunchSelector = { manageLaunchSelector },
        )

        assertThat(selector).isSameInstanceAs(manageLaunchSelector)
    }

    @Test
    fun `non-Manage launches commit selection without creating the Manage launch selector`() = runTest {
        listOf(
            EmbeddedLaunchMode.PaymentOptions,
            EmbeddedLaunchMode.Form(selectedPaymentMethodCode = "card"),
        ).forEach { launchMode ->
            val selectionHolder = DefaultEmbeddedSelectionHolder(SavedStateHandle())
            val selector = EmbeddedActivityModule.provideManageScreenSavedPaymentMethodSelector(
                launchMode = launchMode,
                selectionHolder = selectionHolder,
                manageLaunchSelector = { error("Manage launch selector should not be created") },
            )
            val selection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)

            assertThat(selector.select(selection).isSuccess).isTrue()
            assertThat(selectionHolder.selection.value).isEqualTo(selection)

            val original = selection.paymentMethod
            val updated = original.copy(
                billingDetails = original.billingDetails?.toBuilder()
                    ?.setAddress(original.billingDetails?.address?.copy(postalCode = "10001"))
                    ?.build(),
            )
            assertThat(selector.syncBillingAfterEdit(original, updated).isSuccess).isTrue()
            assertThat(selectionHolder.selection.value).isEqualTo(selection)
            assertThat(selector.checkoutSessionResponse).isNull()
        }
    }

    private fun runScenario(
        shouldUseAutocompleteProxyEndpoints: Boolean,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val eventReporter = FakeAddressLauncherEventReporter()
        val factory = EmbeddedActivityModule.provideAutocompleteAddressInteractorFactory(
            stripeAutocompleteRepository = FakeStripeAutocompleteRepository(),
            coroutineScope = this,
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                shouldUseAutocompleteProxyEndpoints = shouldUseAutocompleteProxyEndpoints,
            ),
            eventReporter = eventReporter,
        )

        Scenario(factory.create()).block()

        eventReporter.validate()
    }

    private data class Scenario(
        val interactor: AutocompleteAddressInteractor,
    )
}
