package com.stripe.android.paymentelement.embedded

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodMessageLearnMore
import com.stripe.android.model.PaymentMethodMessagePromotion
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.embedded.manage.ManageLaunchSavedPaymentMethodSelector
import com.stripe.android.paymentelement.embedded.sheet.SheetTaxRegionUpdater
import com.stripe.android.paymentsheet.addresselement.AUTOCOMPLETE_DEFAULT_COUNTRIES
import com.stripe.android.paymentsheet.addresselement.BillingInlineAutocompleteAddressInteractor
import com.stripe.android.paymentsheet.addresselement.FakeStripeAutocompleteRepository
import com.stripe.android.paymentsheet.addresselement.PaymentElementAutocompleteAddressInteractor
import com.stripe.android.paymentsheet.addresselement.analytics.FakeAddressLauncherEventReporter
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.paymentsheet.repositories.PaymentMethodMessagePromotionsHelper
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
        }
    }

    @Test
    fun `checkout sheets do not provide promotions or report promotion displays`() = runPromotionsScenario(
        integrationMetadata = IntegrationMetadata.CheckoutSession(
            id = "cs_123",
            instancesKey = "checkout",
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(),
            collectedEmail = null,
        ),
    ) { helper ->
        assertThat(helper.getPromotions()).isNull()
        assertThat(helper.getPromotionIfAvailableForCode(PaymentMethod.Type.Affirm.code, metadata)).isNull()
        assertThat(helper.getPromotionProvider(PaymentMethod.Type.Affirm.code, metadata)).isNull()
        helper.reportPromotionDisplayed(PaymentMethod.Type.Affirm.code, metadata)
        eventReporter.pmmPromotionsDisplayed.expectNoEvents()
    }

    @Test
    fun `standalone embedded sheets continue to provide promotions and report displays`() = runPromotionsScenario(
        integrationMetadata = IntegrationMetadata.IntentFirst("secret"),
    ) { helper ->
        assertThat(helper.getPromotions()).containsExactly(promotion)
        assertThat(helper.getPromotionIfAvailableForCode(PaymentMethod.Type.Affirm.code, metadata)).isEqualTo(promotion)
        assertThat(helper.getPromotionProvider(PaymentMethod.Type.Affirm.code, metadata)?.invoke()).isEqualTo(promotion)
        helper.reportPromotionDisplayed(PaymentMethod.Type.Affirm.code, metadata)
        assertThat(eventReporter.pmmPromotionsDisplayed.awaitItem()).isTrue()
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

    private fun runPromotionsScenario(
        integrationMetadata: IntegrationMetadata,
        block: suspend PromotionsScenario.(PaymentMethodMessagePromotionsHelper) -> Unit,
    ) = runTest {
        val eventReporter = FakeEventReporter()
        val metadata = PaymentMethodMetadataFactory.create(
            integrationMetadata = integrationMetadata,
            experimentsData = ElementsSession.ExperimentsData(
                arbId = "arb_123",
                experimentAssignments = mapOf(
                    ElementsSession.ExperimentAssignment.OCS_MOBILE_PAYMENT_METHOD_MESSAGING_PROMOTIONS to "treatment"
                ),
            ),
        )
        val helper = EmbeddedActivityModule.providesPaymentMethodMessagePromotionHelper(
            promotions = listOf(promotion),
            eventReporter = eventReporter,
            paymentMethodMetadata = metadata,
        )
        PromotionsScenario(metadata, eventReporter).block(helper)
        eventReporter.validate()
    }

    private data class PromotionsScenario(
        val metadata: PaymentMethodMetadata,
        val eventReporter: FakeEventReporter,
    )

    private companion object {
        val promotion = PaymentMethodMessagePromotion(
            paymentMethodType = PaymentMethod.Type.Affirm.code,
            message = "Promotion",
            learnMore = PaymentMethodMessageLearnMore(message = "Learn more", url = "https://example.com"),
        )
    }
}
