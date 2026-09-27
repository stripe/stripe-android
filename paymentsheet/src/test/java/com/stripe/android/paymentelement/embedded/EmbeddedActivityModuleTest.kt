package com.stripe.android.paymentelement.embedded

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.embedded.sheet.FakeSheetSavedPaymentMethodSelectionCoordinator
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.addresselement.AUTOCOMPLETE_DEFAULT_COUNTRIES
import com.stripe.android.paymentsheet.addresselement.BillingInlineAutocompleteAddressInteractor
import com.stripe.android.paymentsheet.addresselement.FakeStripeAutocompleteRepository
import com.stripe.android.paymentsheet.addresselement.PaymentElementAutocompleteAddressInteractor
import com.stripe.android.paymentsheet.addresselement.analytics.FakeAddressLauncherEventReporter
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.verticalmode.SelectionBehavior
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

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
    fun `Manage launch coordinates saved selection and reports selected option`() =
        runSelectionBehaviorScenario(EmbeddedLaunchMode.Manage) {
            val behavior = selectionBehavior as SelectionBehavior.Coordinated
            val result = behavior.selectPaymentMethod(displayableSavedPaymentMethod)

            assertThat(result.isSuccess).isTrue()
            assertThat(selectionCoordinator.selectCalls.awaitItem()).isEqualTo(selection)
            verify(eventReporter).onSelectPaymentOption(selection)
            assertThat(selectionHolder.selection.value).isNull()
        }

    @Test
    fun `PaymentOptions launch selects immediately and reports selected option`() =
        runSelectionBehaviorScenario(EmbeddedLaunchMode.PaymentOptions) {
            selectImmediately()
        }

    @Test
    fun `Form launch selects immediately and reports selected option`() =
        runSelectionBehaviorScenario(EmbeddedLaunchMode.Form(selectedPaymentMethodCode = "card")) {
            selectImmediately()
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

    private fun runSelectionBehaviorScenario(
        launchMode: EmbeddedLaunchMode,
        block: suspend SelectionScenario.() -> Unit,
    ) = runTest {
        val selectionHolder = DefaultEmbeddedSelectionHolder(SavedStateHandle())
        val selectionCoordinator = FakeSheetSavedPaymentMethodSelectionCoordinator(Result.success(Unit))
        val eventReporter = mock<EventReporter>()
        val displayableSavedPaymentMethod = PaymentMethodFixtures.displayableCard()
        val selection = PaymentSelection.Saved(displayableSavedPaymentMethod.paymentMethod)
        val selectionBehavior = EmbeddedActivityModule.provideManageScreenSelectionBehavior(
            launchMode = launchMode,
            eventReporter = eventReporter,
            selectionHolder = selectionHolder,
            selectionCoordinator = selectionCoordinator,
        )

        SelectionScenario(
            selectionBehavior = selectionBehavior,
            displayableSavedPaymentMethod = displayableSavedPaymentMethod,
            selection = selection,
            selectionHolder = selectionHolder,
            selectionCoordinator = selectionCoordinator,
            eventReporter = eventReporter,
        ).block()

        selectionCoordinator.validate()
    }

    private suspend fun SelectionScenario.selectImmediately() {
        val behavior = selectionBehavior as SelectionBehavior.Immediate
        behavior.onSelectPaymentMethod(displayableSavedPaymentMethod)

        assertThat(selectionHolder.selection.value).isEqualTo(selection)
        verify(eventReporter).onSelectPaymentOption(selection)
        selectionCoordinator.selectCalls.expectNoEvents()
    }

    private data class Scenario(
        val interactor: AutocompleteAddressInteractor,
    )

    private data class SelectionScenario(
        val selectionBehavior: SelectionBehavior,
        val displayableSavedPaymentMethod: DisplayableSavedPaymentMethod,
        val selection: PaymentSelection.Saved,
        val selectionHolder: DefaultEmbeddedSelectionHolder,
        val selectionCoordinator: FakeSheetSavedPaymentMethodSelectionCoordinator,
        val eventReporter: EventReporter,
    )
}
