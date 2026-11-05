package com.stripe.android.paymentelement.embedded

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.taptoadd.FakeTapToAddHelper
import com.stripe.android.common.taptoadd.TapToAddMode
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
import com.stripe.android.paymentsheet.FakeCustomerStateHolder
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AUTOCOMPLETE_DEFAULT_COUNTRIES
import com.stripe.android.paymentsheet.addresselement.BillingInlineAutocompleteAddressInteractor
import com.stripe.android.paymentsheet.addresselement.FakeStripeAutocompleteRepository
import com.stripe.android.paymentsheet.addresselement.PaymentElementAutocompleteAddressInteractor
import com.stripe.android.paymentsheet.addresselement.TestAutocompleteLauncher
import com.stripe.android.paymentsheet.addresselement.analytics.FakeAddressLauncherEventReporter
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.flowcontroller.asEmbeddedSheetConfiguration
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.paymentsheet.repositories.PaymentMethodMessagePromotionsHelper
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock

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
            EmbeddedLaunchMode.PaymentOptions(flowController = null),
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

    @Test
    fun `FlowController autocomplete factory uses the configured Google Places API key`() = runScenario(
        shouldUseAutocompleteProxyEndpoints = false,
        launchMode = flowControllerLaunchMode(googlePlacesApiKey = "places_key"),
    ) {
        assertThat(interactor).isInstanceOf(PaymentElementAutocompleteAddressInteractor::class.java)
        assertThat(interactor.autocompleteConfig.googlePlacesApiKey).isEqualTo("places_key")
        assertThat(interactor.autocompleteConfig.isInlineAutocompleteEnabled).isTrue()
    }

    @Test
    fun `FlowController launch uses the custom event reporter mode`() {
        assertThat(EmbeddedActivityModule.provideEventReporterMode(flowControllerLaunchMode()))
            .isEqualTo(EventReporter.Mode.Custom)
    }

    @Test
    fun `embedded payment options launch uses the embedded event reporter mode`() {
        assertThat(
            EmbeddedActivityModule.provideEventReporterMode(EmbeddedLaunchMode.PaymentOptions(flowController = null))
        ).isEqualTo(EventReporter.Mode.Embedded)
    }

    @Test
    fun `FlowController creates tap to add helper with Continue mode`() = runTest {
        FakeTapToAddHelper.Factory.test {
            EmbeddedActivityModule.providesTapToAddHelper(
                coroutineScope = this@runTest,
                configuration = PaymentSheet.Configuration("Example, Inc.").asEmbeddedSheetConfiguration(),
                tapToAddHelperFactory = tapToAddHelperFactory,
                embeddedSelectionHolder = DefaultEmbeddedSelectionHolder(SavedStateHandle()),
                customerStateHolder = FakeCustomerStateHolder(),
                paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
                statusBarColor = null,
            )

            assertThat(createCalls.awaitItem().tapToAddMode).isEqualTo(TapToAddMode.Continue)
        }
    }

    private fun runScenario(
        shouldUseAutocompleteProxyEndpoints: Boolean,
        launchMode: EmbeddedLaunchMode = EmbeddedLaunchMode.PaymentOptions(flowController = null),
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val eventReporter = FakeAddressLauncherEventReporter()
        val factory = EmbeddedActivityModule.providePaymentElementAutocompleteAddressInteractorFactory(
            context = mock(),
            launchMode = launchMode,
            autocompleteLauncher = TestAutocompleteLauncher.noOp(),
            stripeAutocompleteRepository = FakeStripeAutocompleteRepository(),
            coroutineScope = this,
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                shouldUseAutocompleteProxyEndpoints = shouldUseAutocompleteProxyEndpoints,
            ),
            eventReporter = eventReporter,
            errorReporter = FakeErrorReporter(),
        )

        Scenario(factory.create()).block()

        eventReporter.validate()
    }

    private fun flowControllerLaunchMode(
        googlePlacesApiKey: String? = null,
    ) = EmbeddedLaunchMode.PaymentOptions(
        flowController = EmbeddedLaunchMode.FlowControllerOptions(
            visibleWallets = emptyList(),
            googlePlacesApiKey = googlePlacesApiKey,
        ),
    )

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
