package com.stripe.android.paymentelement.embedded.manage

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
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

                selector.clearError()

                expectNoEvents()

                update.complete(Result.success(CheckoutSessionResponseFactory.create()))

                assertThat(awaitItem()).isEqualTo(SavedPaymentMethodSelectionState.Idle)
            }
            assertThat(result.await().isSuccess).isTrue()
        }
    }

    private fun runScenario(
        paymentMethodMetadata: PaymentMethodMetadata,
        updateTaxRegion: suspend () -> Result<CheckoutSessionResponse>,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val selectionHolder = DefaultEmbeddedSelectionHolder(SavedStateHandle()).apply {
            setSelection(INITIAL_SELECTION)
        }
        val selector = ManageLaunchSavedPaymentMethodSelector(
            taxRegionUpdater = SheetTaxRegionUpdater(updateTaxRegion = { _, _, _ -> updateTaxRegion() }),
            paymentMethodMetadata = paymentMethodMetadata,
            selectionHolder = selectionHolder,
        )

        Scenario(
            testScope = this,
            selector = selector,
            selectionHolder = selectionHolder,
        ).block()
    }

    private class Scenario(
        val testScope: TestScope,
        val selector: ManageLaunchSavedPaymentMethodSelector,
        val selectionHolder: DefaultEmbeddedSelectionHolder,
    ) {
        val selection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
    }

    private companion object {
        val INITIAL_SELECTION = PaymentSelection.Saved(PaymentMethodFixtures.US_BANK_ACCOUNT)

        val CHECKOUT_SESSION_METADATA = PaymentMethodMetadataFactory.create(
            integrationMetadata = IntegrationMetadata.CheckoutSession(
                id = "cs_test_123",
                instancesKey = "test_instances_key",
                checkoutSessionResponse = CheckoutSessionResponseFactory.create(
                    automaticTaxEnabled = true,
                    taxAddressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
                ),
            )
        )
    }
}
