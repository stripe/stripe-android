@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.paymentsheet.addresselement

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.CheckoutController
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertFailsWith

internal class CheckoutShippingAddressProcessorTest {
    @Test
    fun `process validates converts and returns updated response`() = runTest {
        val updatedResponse = CHECKOUT_SESSION_RESPONSE.copy(liveMode = true)
        val updater = FakeUpdateTaxRegion(Result.success(updatedResponse))
        val processor = CheckoutShippingAddressProcessor(updater::invoke)

        val result = processor.process(CHECKOUT_SESSION_RESPONSE, ADDRESS_DETAILS)

        assertThat(result.getOrThrow()).isSameInstanceAs(updatedResponse)
        assertThat(updater.calls.awaitItem()).isEqualTo(
            FakeUpdateTaxRegion.Call(
                response = CHECKOUT_SESSION_RESPONSE,
                source = CheckoutSessionResponse.TaxAddressSource.SHIPPING,
                address = CHECKOUT_ADDRESS,
            )
        )
        updater.calls.ensureAllEventsConsumed()
    }

    @Test
    fun `process rejects a disallowed country without updating`() = runTest {
        val updater = FakeUpdateTaxRegion(Result.success(CHECKOUT_SESSION_RESPONSE))
        val processor = CheckoutShippingAddressProcessor(updater::invoke)
        val response = CHECKOUT_SESSION_RESPONSE.copy(allowedShippingCountries = listOf("CA"))

        val result = processor.process(response, ADDRESS_DETAILS)

        assertThat(result.isFailure).isTrue()
        updater.calls.expectNoEvents()
        updater.calls.ensureAllEventsConsumed()
    }

    @Test
    fun `process converts thrown errors to failure`() = runTest {
        val processor = CheckoutShippingAddressProcessor { _, _, _ ->
            error("failed")
        }

        val result = processor.process(CHECKOUT_SESSION_RESPONSE, ADDRESS_DETAILS)

        assertThat(result.exceptionOrNull()).hasMessageThat().isEqualTo("failed")
    }

    @Test
    fun `process rethrows cancellation`() = runTest {
        assertFailsWith<CancellationException> {
            val processor = CheckoutShippingAddressProcessor { _, _, _ ->
                throw CancellationException()
            }
            processor.process(CHECKOUT_SESSION_RESPONSE, ADDRESS_DETAILS)
        }
    }

    private class FakeUpdateTaxRegion(
        private val result: Result<CheckoutSessionResponse>,
    ) {
        val calls = Turbine<Call>()

        suspend operator fun invoke(
            response: CheckoutSessionResponse,
            source: CheckoutSessionResponse.TaxAddressSource,
            address: CheckoutController.Address.State,
        ): Result<CheckoutSessionResponse> {
            calls.add(Call(response, source, address))
            return result
        }

        data class Call(
            val response: CheckoutSessionResponse,
            val source: CheckoutSessionResponse.TaxAddressSource,
            val address: CheckoutController.Address.State,
        )
    }

    private companion object {
        val CHECKOUT_SESSION_RESPONSE = CheckoutSessionResponseFactory.create(
            automaticTaxEnabled = true,
            taxAddressSource = CheckoutSessionResponse.TaxAddressSource.SHIPPING,
        )
        val ADDRESS_DETAILS = AddressDetails(
            address = PaymentSheet.Address(
                city = "San Francisco",
                country = "US",
                line1 = "510 Townsend St",
                line2 = "Floor 2",
                postalCode = "94103",
                state = "CA",
            )
        )
        val CHECKOUT_ADDRESS = CheckoutController.Address.State(
            city = "San Francisco",
            country = "US",
            line1 = "510 Townsend St",
            line2 = "Floor 2",
            postalCode = "94103",
            state = "CA",
        )
    }
}
