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
                assertTaxUpdateAddress(taxRegionUpdateCalls.awaitItem(), postalCode = "94111")

                val response = CheckoutSessionResponseFactory.create(id = "refreshed_response")
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
                assertTaxUpdateAddress(taxRegionUpdateCalls.awaitItem(), postalCode = "94111")

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
            assertTaxUpdateAddress(taxRegionUpdateCalls.awaitItem(), postalCode = "94111")

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
                assertTaxUpdateAddress(taxRegionUpdateCalls.awaitItem(), postalCode = "94111")

                selector.clearError()

                expectNoEvents()

                update.complete(Result.success(CheckoutSessionResponseFactory.create()))

                assertThat(awaitItem()).isEqualTo(SavedPaymentMethodSelectionState.Idle)
            }
            assertThat(result.await().isSuccess).isTrue()
        }
    }

    @Test
    fun `selecting a row after an edit requests tax again and retains the latest response`() {
        val firstResponse = CheckoutSessionResponseFactory.create(id = "edit_response")
        val secondResponse = CheckoutSessionResponseFactory.create(id = "selection_response")
        var nextResponse = firstResponse
        runScenario(
            paymentMethodMetadata = CHECKOUT_SESSION_METADATA,
            updateTaxRegion = { Result.success(nextResponse) },
        ) {
            val updated = selection.paymentMethod.withPostalCode("10001")
            selectionHolder.setSelection(PaymentSelection.Saved(updated))
            assertThat(selector.syncBillingAfterEdit(selection.paymentMethod, updated).isSuccess).isTrue()
            assertTaxUpdateAddress(taxRegionUpdateCalls.awaitItem(), postalCode = "10001")
            assertThat(selector.checkoutSessionResponse).isEqualTo(firstResponse)

            nextResponse = secondResponse
            assertThat(selector.select(PaymentSelection.Saved(updated)).isSuccess).isTrue()

            assertTaxUpdateAddress(taxRegionUpdateCalls.awaitItem(), postalCode = "10001")
            assertThat(selector.checkoutSessionResponse).isEqualTo(secondResponse)
        }
    }

    @Test
    fun `edit synchronization stores the response without changing selection or row state`() {
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
                assertTaxUpdateAddress(taxRegionUpdateCalls.awaitItem(), postalCode = "10001")
                assertThat(selectionHolder.selection.value).isEqualTo(selection)
                assertThat(selector.checkoutSessionResponse).isNull()
                expectNoEvents()

                val response = CheckoutSessionResponseFactory.create(id = "edit_response")
                update.complete(Result.success(response))

                assertThat(result.await().isSuccess).isTrue()
                assertThat(selector.checkoutSessionResponse).isEqualTo(response)
                assertThat(selectionHolder.selection.value).isEqualTo(selection)
                expectNoEvents()
            }
        }
    }

    @Test
    fun `failed edit synchronization retains the last response without changing row state and can retry`() {
        val response = CheckoutSessionResponseFactory.create(id = "edit_response")
        val error = IllegalStateException("Tax update failed")
        var nextResult: Result<CheckoutSessionResponse> = Result.success(response)
        runScenario(
            paymentMethodMetadata = CHECKOUT_SESSION_METADATA,
            updateTaxRegion = { nextResult },
        ) {
            val updated = selection.paymentMethod.withPostalCode("10001")
            selectionHolder.setSelection(selection)
            assertThat(selector.syncBillingAfterEdit(selection.paymentMethod, updated).isSuccess).isTrue()
            assertTaxUpdateAddress(taxRegionUpdateCalls.awaitItem(), postalCode = "10001")

            selector.selectionState.test {
                assertThat(awaitItem()).isEqualTo(SavedPaymentMethodSelectionState.Idle)
                nextResult = Result.failure(error)

                assertThat(selector.syncBillingAfterEdit(selection.paymentMethod, updated).exceptionOrNull())
                    .isSameInstanceAs(error)
                assertTaxUpdateAddress(taxRegionUpdateCalls.awaitItem(), postalCode = "10001")
                assertThat(selector.checkoutSessionResponse).isEqualTo(response)
                assertThat(selectionHolder.selection.value).isEqualTo(selection)
                expectNoEvents()

                val retryResponse = CheckoutSessionResponseFactory.create(id = "retry_response")
                nextResult = Result.success(retryResponse)
                assertThat(selector.syncBillingAfterEdit(selection.paymentMethod, updated).isSuccess).isTrue()
                assertTaxUpdateAddress(taxRegionUpdateCalls.awaitItem(), postalCode = "10001")
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
        assertThat(selector.syncBillingAfterEdit(original, original.withPostalCode("10001")).isSuccess).isTrue()

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
        assertThat(selector.syncBillingAfterEdit(original, original.withPostalCode("10001")).isSuccess).isTrue()

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
        val original = selection.paymentMethod
        val updated = original.copy(card = original.card?.copy(expiryMonth = 12, expiryYear = 2030))
        assertThat(updated.card).isNotEqualTo(original.card)

        assertThat(selector.syncBillingAfterEdit(original, updated).isSuccess).isTrue()

        taxRegionUpdateCalls.expectNoEvents()
        assertThat(selectionHolder.selection.value).isEqualTo(selection)
        assertThat(selector.checkoutSessionResponse).isNull()
    }

    @Test
    fun `editing selected card address without Checkout tax does not update tax`() = runScenario(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        updateTaxRegion = { error("Tax update should not run") },
    ) {
        selectionHolder.setSelection(selection)
        val original = selection.paymentMethod
        assertThat(selector.syncBillingAfterEdit(original, original.withPostalCode("10001")).isSuccess).isTrue()

        taxRegionUpdateCalls.expectNoEvents()
        assertThat(selectionHolder.selection.value).isEqualTo(selection)
        assertThat(selector.checkoutSessionResponse).isNull()
    }

    @Test
    fun `changed saved selection requests tax update again and stores new response`() {
        val firstResponse = CheckoutSessionResponseFactory.create(id = "first_response")
        val secondResponse = CheckoutSessionResponseFactory.create(id = "second_response")
        var nextResponse = firstResponse

        runScenario(
            paymentMethodMetadata = CHECKOUT_SESSION_METADATA,
            updateTaxRegion = { Result.success(nextResponse) },
        ) {
            assertThat(selector.select(selection).isSuccess).isTrue()
            assertTaxUpdateAddress(taxRegionUpdateCalls.awaitItem(), postalCode = "94111")

            val changedSelection = PaymentSelection.Saved(selection.paymentMethod.withPostalCode("10001"))
            nextResponse = secondResponse
            assertThat(selector.select(changedSelection).isSuccess).isTrue()

            assertTaxUpdateAddress(taxRegionUpdateCalls.awaitItem(), postalCode = "10001")
            assertThat(selector.checkoutSessionResponse).isEqualTo(secondResponse)
            assertThat(selectionHolder.selection.value).isEqualTo(changedSelection)
        }
    }

    @Test
    fun `failed selection retries tax update`() {
        val error = IllegalStateException("Tax update failed")
        val response = CheckoutSessionResponseFactory.create(id = "retry_response")
        var nextResult: Result<CheckoutSessionResponse> = Result.failure(error)

        runScenario(
            paymentMethodMetadata = CHECKOUT_SESSION_METADATA,
            updateTaxRegion = { nextResult },
        ) {
            assertThat(selector.select(selection).isFailure).isTrue()
            assertTaxUpdateAddress(taxRegionUpdateCalls.awaitItem(), postalCode = "94111")
            assertThat(selector.selectionState.value)
                .isEqualTo(SavedPaymentMethodSelectionState.Failed(error.stripeErrorMessage()))
            assertThat(selectionHolder.selection.value).isEqualTo(INITIAL_SELECTION)
            assertThat(selector.checkoutSessionResponse).isNull()

            nextResult = Result.success(response)
            assertThat(selector.select(selection).isSuccess).isTrue()

            assertTaxUpdateAddress(taxRegionUpdateCalls.awaitItem(), postalCode = "94111")
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
    ) {
        assertThat(call.checkoutSessionResponse).isEqualTo(CHECKOUT_SESSION_RESPONSE)
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
                id = "cs_test_123",
                instancesKey = "test_instances_key",
                checkoutSessionResponse = CHECKOUT_SESSION_RESPONSE,
            )
        )
    }
}
