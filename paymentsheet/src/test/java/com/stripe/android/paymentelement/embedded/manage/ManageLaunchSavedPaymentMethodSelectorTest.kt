package com.stripe.android.paymentelement.embedded.manage

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.CheckoutController
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.sheet.SheetTaxRegionUpdater
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.paymentsheet.state.SavedPaymentMethodSelectionState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(CheckoutSessionPreview::class)
internal class ManageLaunchSavedPaymentMethodSelectorTest {

    @Test
    fun `selection without tax update commits selection without entering pending`() = runScenario(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        updateTaxRegion = { error("Tax update should not run") },
    ) {
        selector.selectionState.test {
            assertThat(awaitItem()).isEqualTo(SavedPaymentMethodSelectionState.Idle)

            assertThat(selector.select(selection).isSuccess).isTrue()

            expectNoEvents()
        }
        taxRegionUpdateCalls.expectNoEvents()
        assertThat(selectionHolder.selection.value).isEqualTo(selection)
        assertThat(selector.checkoutSessionResponse).isNull()
    }

    @Test
    fun `tax update is pending until it succeeds, then stores response and selection`() {
        val update = CompletableDeferred<Result<CheckoutSessionResponse>>()
        runScenario(
            paymentMethodMetadata = CHECKOUT_SESSION_METADATA,
            updateTaxRegion = { update.await() },
        ) {
            selector.selectionState.test {
                assertThat(awaitItem()).isEqualTo(SavedPaymentMethodSelectionState.Idle)

                val result = testScope.async(start = CoroutineStart.UNDISPATCHED) { selector.select(selection) }
                assertThat(awaitItem()).isEqualTo(SavedPaymentMethodSelectionState.Pending(selection.paymentMethod.id))
                assertThat(selectionHolder.selection.value).isEqualTo(INITIAL_SELECTION)
                assertTaxUpdateAddress(taxRegionUpdateCalls.awaitItem(), "94111", CHECKOUT_SESSION_RESPONSE)

                val response = CHECKOUT_SESSION_RESPONSE.copy(id = "refreshed_response")
                update.complete(Result.success(response))

                assertThat(awaitItem()).isEqualTo(SavedPaymentMethodSelectionState.Idle)
                assertThat(result.await().isSuccess).isTrue()
                assertThat(selectionHolder.selection.value).isEqualTo(selection)
                assertThat(selector.checkoutSessionResponse).isEqualTo(response)
            }
        }
    }

    @Test
    fun `failed tax update marks the selection failed and keeps prior selection`() {
        val error = IllegalStateException("Tax region update failed")
        val update = CompletableDeferred<Result<CheckoutSessionResponse>>()
        runScenario(
            paymentMethodMetadata = CHECKOUT_SESSION_METADATA,
            updateTaxRegion = { update.await() },
        ) {
            selector.selectionState.test {
                assertThat(awaitItem()).isEqualTo(SavedPaymentMethodSelectionState.Idle)

                val result = testScope.async(start = CoroutineStart.UNDISPATCHED) { selector.select(selection) }
                assertThat(awaitItem()).isEqualTo(SavedPaymentMethodSelectionState.Pending(selection.paymentMethod.id))
                assertThat(selectionHolder.selection.value).isEqualTo(INITIAL_SELECTION)
                assertTaxUpdateAddress(taxRegionUpdateCalls.awaitItem(), "94111", CHECKOUT_SESSION_RESPONSE)

                update.complete(Result.failure(error))

                assertThat(awaitItem()).isEqualTo(SavedPaymentMethodSelectionState.Failed(error.stripeErrorMessage()))
                assertThat(result.await().isFailure).isTrue()
                assertThat(selectionHolder.selection.value).isEqualTo(INITIAL_SELECTION)
                assertThat(selector.checkoutSessionResponse).isNull()
            }
        }
    }

    @Test
    fun `clearError resets a failed selection to idle`() {
        val error = IllegalStateException("Tax region update failed")
        runScenario(
            paymentMethodMetadata = CHECKOUT_SESSION_METADATA,
            updateTaxRegion = { Result.failure(error) },
        ) {
            assertThat(selector.select(selection).isFailure).isTrue()
            assertTaxUpdateAddress(taxRegionUpdateCalls.awaitItem(), "94111", CHECKOUT_SESSION_RESPONSE)

            selector.selectionState.test {
                assertThat(awaitItem()).isEqualTo(SavedPaymentMethodSelectionState.Failed(error.stripeErrorMessage()))

                selector.clearError()

                assertThat(awaitItem()).isEqualTo(SavedPaymentMethodSelectionState.Idle)
            }
            assertThat(selectionHolder.selection.value).isEqualTo(INITIAL_SELECTION)
        }
    }

    @Test
    fun `clearError keeps a pending selection pending`() {
        val update = CompletableDeferred<Result<CheckoutSessionResponse>>()
        runScenario(
            paymentMethodMetadata = CHECKOUT_SESSION_METADATA,
            updateTaxRegion = { update.await() },
        ) {
            val result = testScope.async(start = CoroutineStart.UNDISPATCHED) { selector.select(selection) }

            selector.selectionState.test {
                assertThat(awaitItem()).isEqualTo(SavedPaymentMethodSelectionState.Pending(selection.paymentMethod.id))
                assertTaxUpdateAddress(taxRegionUpdateCalls.awaitItem(), "94111", CHECKOUT_SESSION_RESPONSE)

                selector.clearError()

                expectNoEvents()

                update.complete(Result.success(CheckoutSessionResponseFactory.create()))

                assertThat(awaitItem()).isEqualTo(SavedPaymentMethodSelectionState.Idle)
            }
            assertThat(result.await().isSuccess).isTrue()
        }
    }

    @Test
    fun `selecting another row after an edit uses and replaces the latest response`() {
        val firstResponse = CHECKOUT_SESSION_RESPONSE.copy(id = "edit_response")
        val secondResponse = CHECKOUT_SESSION_RESPONSE.copy(id = "selection_response")
        var nextResponse = firstResponse
        runScenario(
            paymentMethodMetadata = CHECKOUT_SESSION_METADATA,
            updateTaxRegion = { Result.success(nextResponse) },
        ) {
            val updated = selection.paymentMethod.withPostalCode("10001")
            selectionHolder.setSelection(PaymentSelection.Saved(updated))
            val editResponse = selector.syncBillingAfterEdit(selection.paymentMethod, updated).getOrThrow()
            assertThat(editResponse).isEqualTo(firstResponse)
            assertTaxUpdateAddress(taxRegionUpdateCalls.awaitItem(), "10001", CHECKOUT_SESSION_RESPONSE)
            selector.updateCheckoutSessionResponse(requireNotNull(editResponse))

            nextResponse = secondResponse
            val nextSelection = PaymentSelection.Saved(selection.paymentMethod.copy(id = "pm_other"))
            assertThat(selector.select(nextSelection).isSuccess).isTrue()

            assertTaxUpdateAddress(taxRegionUpdateCalls.awaitItem(), "94111", firstResponse)
            assertThat(selector.checkoutSessionResponse).isEqualTo(secondResponse)
            assertThat(selectionHolder.selection.value).isEqualTo(nextSelection)
        }
    }

    @Test
    fun `edit synchronization returns the response without storing it or changing selection or row state`() {
        val update = CompletableDeferred<Result<CheckoutSessionResponse>>()
        runScenario(
            paymentMethodMetadata = CHECKOUT_SESSION_METADATA,
            updateTaxRegion = { update.await() },
        ) {
            val original = selection.paymentMethod
            val updated = original.withPostalCode("10001")
            selectionHolder.setSelection(selection)
            selector.selectionState.test {
                assertThat(awaitItem()).isEqualTo(SavedPaymentMethodSelectionState.Idle)

                val result = testScope.async(start = CoroutineStart.UNDISPATCHED) {
                    selector.syncBillingAfterEdit(original, updated)
                }
                assertTaxUpdateAddress(taxRegionUpdateCalls.awaitItem(), "10001", CHECKOUT_SESSION_RESPONSE)
                assertThat(selectionHolder.selection.value).isEqualTo(selection)
                assertThat(selector.checkoutSessionResponse).isNull()
                expectNoEvents()

                val response = CHECKOUT_SESSION_RESPONSE.copy(id = "edit_response")
                update.complete(Result.success(response))

                assertThat(result.await().getOrThrow()).isEqualTo(response)
                assertThat(selector.checkoutSessionResponse).isNull()
                assertThat(selectionHolder.selection.value).isEqualTo(selection)
                expectNoEvents()

                selector.updateCheckoutSessionResponse(response)
                assertThat(selector.checkoutSessionResponse).isEqualTo(response)
                expectNoEvents()
            }
        }
    }

    @Test
    fun `failed edit synchronization retains the last response without changing row state and can retry`() {
        val response = CHECKOUT_SESSION_RESPONSE.copy(id = "edit_response")
        val error = IllegalStateException("Tax update failed")
        var nextResult: Result<CheckoutSessionResponse> = Result.success(response)
        runScenario(
            paymentMethodMetadata = CHECKOUT_SESSION_METADATA,
            updateTaxRegion = { nextResult },
        ) {
            val updated = selection.paymentMethod.withPostalCode("10001")
            selectionHolder.setSelection(selection)
            val editResponse = selector.syncBillingAfterEdit(selection.paymentMethod, updated).getOrThrow()
            assertThat(editResponse).isEqualTo(response)
            assertTaxUpdateAddress(taxRegionUpdateCalls.awaitItem(), "10001", CHECKOUT_SESSION_RESPONSE)
            selector.updateCheckoutSessionResponse(requireNotNull(editResponse))

            selector.selectionState.test {
                assertThat(awaitItem()).isEqualTo(SavedPaymentMethodSelectionState.Idle)
                nextResult = Result.failure(error)

                assertThat(selector.syncBillingAfterEdit(selection.paymentMethod, updated).exceptionOrNull())
                    .isSameInstanceAs(error)
                assertTaxUpdateAddress(taxRegionUpdateCalls.awaitItem(), "10001", response)
                assertThat(selector.checkoutSessionResponse).isEqualTo(response)
                assertThat(selectionHolder.selection.value).isEqualTo(selection)
                expectNoEvents()

                val retryResponse = CHECKOUT_SESSION_RESPONSE.copy(id = "retry_response")
                nextResult = Result.success(retryResponse)
                assertThat(selector.syncBillingAfterEdit(selection.paymentMethod, updated).getOrThrow())
                    .isEqualTo(retryResponse)
                assertTaxUpdateAddress(taxRegionUpdateCalls.awaitItem(), "10001", response)
                assertThat(selector.checkoutSessionResponse).isEqualTo(response)
                selector.updateCheckoutSessionResponse(retryResponse)
                assertThat(selector.checkoutSessionResponse).isEqualTo(retryResponse)
                expectNoEvents()
            }
        }
    }

    @Test
    fun `editing an unselected saved card does not update tax`() = runScenario(
        paymentMethodMetadata = CHECKOUT_SESSION_METADATA,
        updateTaxRegion = { error("Tax update should not run") },
    ) {
        val original = selection.paymentMethod
        assertThat(selector.syncBillingAfterEdit(original, original.withPostalCode("10001")).getOrThrow()).isNull()

        taxRegionUpdateCalls.expectNoEvents()
        assertThat(selectionHolder.selection.value).isEqualTo(INITIAL_SELECTION)
        assertThat(selector.checkoutSessionResponse).isNull()
    }

    @Test
    fun `editing a saved card with no selection does not update tax`() = runScenario(
        paymentMethodMetadata = CHECKOUT_SESSION_METADATA,
        updateTaxRegion = { error("Tax update should not run") },
    ) {
        selectionHolder.setSelection(null)
        val original = selection.paymentMethod
        assertThat(selector.syncBillingAfterEdit(original, original.withPostalCode("10001")).getOrThrow()).isNull()

        taxRegionUpdateCalls.expectNoEvents()
        assertThat(selectionHolder.selection.value).isNull()
        assertThat(selector.checkoutSessionResponse).isNull()
    }

    @Test
    fun `editing selected card expiry with an unchanged billing address does not update tax`() = runScenario(
        paymentMethodMetadata = CHECKOUT_SESSION_METADATA,
        updateTaxRegion = { error("Tax update should not run") },
    ) {
        selectionHolder.setSelection(selection)
        val response = CHECKOUT_SESSION_RESPONSE.copy(id = "previous_edit_response")
        selector.updateCheckoutSessionResponse(response)
        val original = selection.paymentMethod
        val updated = original.copy(card = original.card?.copy(expiryMonth = 12, expiryYear = 2030))
        assertThat(updated.card).isNotEqualTo(original.card)

        assertThat(selector.syncBillingAfterEdit(original, updated).getOrThrow()).isNull()

        taxRegionUpdateCalls.expectNoEvents()
        assertThat(selectionHolder.selection.value).isEqualTo(selection)
        assertThat(selector.checkoutSessionResponse).isEqualTo(response)
    }

    @Test
    fun `editing selected card address without Checkout tax does not update tax`() = runScenario(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        updateTaxRegion = { error("Tax update should not run") },
    ) {
        selectionHolder.setSelection(selection)
        val original = selection.paymentMethod
        assertThat(selector.syncBillingAfterEdit(original, original.withPostalCode("10001")).getOrThrow()).isNull()

        taxRegionUpdateCalls.expectNoEvents()
        assertThat(selectionHolder.selection.value).isEqualTo(selection)
        assertThat(selector.checkoutSessionResponse).isNull()
    }

    @Test
    fun `row selection uses the latest tax settings and keeps that response when no update is required`() = runScenario(
        paymentMethodMetadata = CHECKOUT_SESSION_METADATA,
        updateTaxRegion = { error("Tax update should not run") },
    ) {
        val response = CHECKOUT_SESSION_RESPONSE.copy(automaticTaxEnabled = false)
        selector.updateCheckoutSessionResponse(response)
        selector.selectionState.test {
            assertThat(awaitItem()).isEqualTo(SavedPaymentMethodSelectionState.Idle)

            assertThat(selector.select(selection).isSuccess).isTrue()

            expectNoEvents()
        }
        taxRegionUpdateCalls.expectNoEvents()
        assertThat(selectionHolder.selection.value).isEqualTo(selection)
        assertThat(selector.checkoutSessionResponse).isEqualTo(response)
    }

    @Test
    fun `failed row selection keeps the response received from edit`() = runScenario(
        paymentMethodMetadata = CHECKOUT_SESSION_METADATA,
        updateTaxRegion = { Result.failure(IllegalStateException("Tax update failed")) },
    ) {
        val editResponse = CHECKOUT_SESSION_RESPONSE.copy(id = "edit_response")
        selector.updateCheckoutSessionResponse(editResponse)

        assertThat(selector.select(selection).isFailure).isTrue()

        assertTaxUpdateAddress(taxRegionUpdateCalls.awaitItem(), "94111", editResponse)
        assertThat(selectionHolder.selection.value).isEqualTo(INITIAL_SELECTION)
        assertThat(selector.checkoutSessionResponse).isEqualTo(editResponse)
    }

    @Test
    fun `failed selection retries tax update`() {
        val error = IllegalStateException("Tax update failed")
        val response = CHECKOUT_SESSION_RESPONSE.copy(id = "retry_response")
        var nextResult: Result<CheckoutSessionResponse> = Result.failure(error)

        runScenario(
            paymentMethodMetadata = CHECKOUT_SESSION_METADATA,
            updateTaxRegion = { nextResult },
        ) {
            assertThat(selector.select(selection).isFailure).isTrue()
            assertTaxUpdateAddress(taxRegionUpdateCalls.awaitItem(), "94111", CHECKOUT_SESSION_RESPONSE)
            assertThat(selector.selectionState.value)
                .isEqualTo(SavedPaymentMethodSelectionState.Failed(error.stripeErrorMessage()))
            assertThat(selectionHolder.selection.value).isEqualTo(INITIAL_SELECTION)
            assertThat(selector.checkoutSessionResponse).isNull()

            nextResult = Result.success(response)
            assertThat(selector.select(selection).isSuccess).isTrue()

            assertTaxUpdateAddress(taxRegionUpdateCalls.awaitItem(), "94111", CHECKOUT_SESSION_RESPONSE)
            assertThat(selector.selectionState.value).isEqualTo(SavedPaymentMethodSelectionState.Idle)
            assertThat(selectionHolder.selection.value).isEqualTo(selection)
            assertThat(selector.checkoutSessionResponse).isEqualTo(response)
        }
    }

    private fun PaymentMethod.withPostalCode(postalCode: String): PaymentMethod {
        return copy(
            billingDetails = billingDetails?.toBuilder()
                ?.setAddress(billingDetails?.address?.copy(postalCode = postalCode))
                ?.build(),
        )
    }

    private fun assertTaxUpdateAddress(
        call: TaxRegionUpdateCall,
        postalCode: String,
        expectedResponse: CheckoutSessionResponse,
    ) {
        assertThat(call.checkoutSessionResponse).isEqualTo(expectedResponse)
        assertThat(call.taxAddressSource).isEqualTo(CheckoutSessionResponse.TaxAddressSource.BILLING)
        assertThat(call.address.postalCode).isEqualTo(postalCode)
    }

    private fun runScenario(
        paymentMethodMetadata: PaymentMethodMetadata,
        updateTaxRegion: suspend () -> Result<CheckoutSessionResponse>,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val selectionHolder = DefaultEmbeddedSelectionHolder(SavedStateHandle()).apply {
            setSelection(INITIAL_SELECTION)
        }
        val taxRegionUpdateCalls = Turbine<TaxRegionUpdateCall>()
        val selector = ManageLaunchSavedPaymentMethodSelector(
            taxRegionUpdater = SheetTaxRegionUpdater(
                updateTaxRegion = { response, taxAddressSource, address ->
                    taxRegionUpdateCalls.add(
                        TaxRegionUpdateCall(
                            checkoutSessionResponse = response,
                            taxAddressSource = taxAddressSource,
                            address = address,
                        )
                    )
                    updateTaxRegion()
                }
            ),
            paymentMethodMetadata = paymentMethodMetadata,
            selectionHolder = selectionHolder,
        )

        Scenario(
            testScope = this,
            selector = selector,
            selectionHolder = selectionHolder,
            taxRegionUpdateCalls = taxRegionUpdateCalls,
        ).block()
        taxRegionUpdateCalls.ensureAllEventsConsumed()
    }

    private class Scenario(
        val testScope: TestScope,
        val selector: ManageLaunchSavedPaymentMethodSelector,
        val selectionHolder: DefaultEmbeddedSelectionHolder,
        val taxRegionUpdateCalls: ReceiveTurbine<TaxRegionUpdateCall>,
    ) {
        val selection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
    }

    private data class TaxRegionUpdateCall(
        val checkoutSessionResponse: CheckoutSessionResponse,
        val taxAddressSource: CheckoutSessionResponse.TaxAddressSource,
        val address: CheckoutController.Address.State,
    )

    private companion object {
        val INITIAL_SELECTION = PaymentSelection.Saved(PaymentMethodFixtures.US_BANK_ACCOUNT)

        val CHECKOUT_SESSION_RESPONSE = CheckoutSessionResponseFactory.create(
            automaticTaxEnabled = true,
            taxAddressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
        )

        val CHECKOUT_SESSION_METADATA = PaymentMethodMetadataFactory.create(
            integrationMetadata = IntegrationMetadata.CheckoutSession(
                collectedEmail = null,
                id = "cs_test_123",
                instancesKey = "test_instances_key",
                checkoutSessionResponse = CHECKOUT_SESSION_RESPONSE,
            )
        )
    }
}
