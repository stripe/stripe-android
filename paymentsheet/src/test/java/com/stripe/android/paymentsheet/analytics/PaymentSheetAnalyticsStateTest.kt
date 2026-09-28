package com.stripe.android.paymentsheet.analytics

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

internal class PaymentSheetAnalyticsStateTest {
    @Test
    fun `previouslySentDeepLinkEvent defaults to false and persists updates`() = runScenario {
        assertThat(savedStateHandle.previouslySentDeepLinkEvent).isFalse()

        savedStateHandle.previouslySentDeepLinkEvent = true

        assertThat(savedStateHandle.previouslySentDeepLinkEvent).isTrue()
        assertThat(savedStateHandle.get<Boolean>("previously_sent_deep_link_event")).isTrue()
    }

    @Test
    fun `reportBillingAddressCompleted reports persisted autocomplete analytics`() = runScenario {
        savedStateHandle.persistBillingAnalytics(
            paymentSelection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION,
            autocompleteFilledAddress = autocompleteAddress,
        )

        savedStateHandle.reportBillingAddressCompleted(
            paymentSelection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION,
            eventReporter = eventReporter,
        )

        val call = eventReporter.billingAddressCompletedCalls.awaitItem()
        assertThat(call.addressCountryCode).isEqualTo("US")
        assertThat(call.autocompleteResultSelected).isTrue()
        assertThat(call.editDistance).isEqualTo(1)
    }

    @Test
    fun `reportBillingAddressCompleted reports when autocomplete was not used`() = runScenario {
        savedStateHandle.persistBillingAnalytics(
            paymentSelection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION,
            autocompleteFilledAddress = null,
        )

        savedStateHandle.reportBillingAddressCompleted(
            paymentSelection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION,
            eventReporter = eventReporter,
        )

        val call = eventReporter.billingAddressCompletedCalls.awaitItem()
        assertThat(call.autocompleteResultSelected).isFalse()
        assertThat(call.editDistance).isNull()
    }

    @Test
    fun `reportBillingAddressCompleted clears persisted analytics`() = runScenario {
        savedStateHandle.persistBillingAnalytics(
            paymentSelection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION,
            autocompleteFilledAddress = autocompleteAddress,
        )
        savedStateHandle.reportBillingAddressCompleted(
            paymentSelection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION,
            eventReporter = eventReporter,
        )
        eventReporter.billingAddressCompletedCalls.awaitItem()

        savedStateHandle.reportBillingAddressCompleted(
            paymentSelection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION,
            eventReporter = eventReporter,
        )

        val call = eventReporter.billingAddressCompletedCalls.awaitItem()
        assertThat(call.autocompleteResultSelected).isFalse()
        assertThat(call.editDistance).isNull()
    }

    @Test
    fun `reportBillingAddressCompleted ignores saved payment selections`() = runScenario {
        savedStateHandle.reportBillingAddressCompleted(
            paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
            eventReporter = eventReporter,
        )

        eventReporter.billingAddressCompletedCalls.expectNoEvents()
    }

    @Test
    fun `reportBillingAddressCompleted ignores selections without a billing address`() = runScenario {
        val paymentSelection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION.copy(
            paymentMethodCreateParams = PaymentMethodCreateParams.create(
                card = PaymentMethodCreateParamsFixtures.CARD,
            )
        )
        savedStateHandle.persistBillingAnalytics(
            paymentSelection = paymentSelection,
            autocompleteFilledAddress = autocompleteAddress,
        )

        savedStateHandle.reportBillingAddressCompleted(
            paymentSelection = paymentSelection,
            eventReporter = eventReporter,
        )

        eventReporter.billingAddressCompletedCalls.expectNoEvents()
    }

    private fun runScenario(
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val eventReporter = FakeEventReporter()
        Scenario(
            savedStateHandle = SavedStateHandle(),
            eventReporter = eventReporter,
        ).block()
        eventReporter.validate()
    }

    private data class Scenario(
        val savedStateHandle: SavedStateHandle,
        val eventReporter: FakeEventReporter,
    )

    private companion object {
        val autocompleteAddress = Address(
            line1 = "123 Main St",
            city = "San Francisco",
            state = "CA",
            country = "US",
            postalCode = "94111",
        )
    }
}
