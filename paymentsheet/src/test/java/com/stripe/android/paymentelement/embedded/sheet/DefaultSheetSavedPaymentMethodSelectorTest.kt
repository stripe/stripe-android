package com.stripe.android.paymentelement.embedded.sheet

import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.CheckoutSessionTaxRegionUpdater
import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.checkouttesting.checkoutUpdate
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.DEFAULT_API_CONFIG
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionRepository
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.paymentsheet.state.SavedPaymentMethodSelectionState
import com.stripe.android.testing.FakeAnalyticsRequestExecutor
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class DefaultSheetSavedPaymentMethodSelectorTest {

    @get:Rule
    val networkRule = NetworkRule()

    @Test
    fun `selection without tax update clears prior response and commits selection`() = runScenario(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        initialSelection = PaymentSelection.Saved(PaymentMethodFixtures.US_BANK_ACCOUNT),
        selection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
        initialResponse = CheckoutSessionResponseFactory.create(id = "previous_response"),
    ) {
        val result = selector.select(selection)

        assertThat(result.isSuccess).isTrue()
        assertThat(selectionHolder.selection.value).isEqualTo(selection)
        assertThat(sheetActivityStateHolder.checkoutSessionResponse).isNull()
        assertThat(sheetActivityStateHolder.checkoutSessionResponseCalls.awaitItem()).isNull()
    }

    @Test
    fun `successful tax update stores refreshed response and commits selection`() {
        networkRule.checkoutUpdate { response ->
            response.testBodyFromFile("checkout-session-init.json")
        }

        runScenario(
            paymentMethodMetadata = CHECKOUT_SESSION_METADATA,
            initialSelection = PaymentSelection.Saved(PaymentMethodFixtures.US_BANK_ACCOUNT),
            selection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
            initialResponse = null,
        ) {
            val result = selector.select(selection)

            assertThat(result.isSuccess).isTrue()
            assertThat(selectionHolder.selection.value).isEqualTo(selection)
            assertThat(sheetActivityStateHolder.checkoutSessionResponse?.id).isEqualTo(DEFAULT_CHECKOUT_SESSION_ID)
            assertThat(sheetActivityStateHolder.checkoutSessionResponseCalls.awaitItem()?.id)
                .isEqualTo(DEFAULT_CHECKOUT_SESSION_ID)
        }
    }

    @Test
    fun `failed tax update leaves selection and stored response unchanged`() {
        networkRule.checkoutUpdate { response ->
            response.setResponseCode(400)
            response.setBody("""{"error":{"message":"Tax region update failed"}}""")
        }

        val initialSelection = PaymentSelection.Saved(PaymentMethodFixtures.US_BANK_ACCOUNT)
        val selection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        val initialResponse = CheckoutSessionResponseFactory.create(id = "previous_response")
        runScenario(
            paymentMethodMetadata = CHECKOUT_SESSION_METADATA,
            initialSelection = initialSelection,
            selection = selection,
            initialResponse = initialResponse,
        ) {
            val result = selector.select(selection)

            assertThat(result.isFailure).isTrue()
            assertThat(selectionHolder.selection.value).isEqualTo(initialSelection)
            assertThat(sheetActivityStateHolder.checkoutSessionResponse).isEqualTo(initialResponse)
            sheetActivityStateHolder.checkoutSessionResponseCalls.expectNoEvents()
        }
    }

    @Test
    fun `selection without tax update never marks pending and ends idle`() = runScenario(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        initialSelection = PaymentSelection.Saved(PaymentMethodFixtures.US_BANK_ACCOUNT),
        selection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
        initialResponse = null,
    ) {
        sheetActivityStateHolder.savedPaymentMethodSelectionState.test {
            assertThat(awaitItem()).isEqualTo(SavedPaymentMethodSelectionState.Idle)

            val result = selector.select(selection)

            assertThat(result.isSuccess).isTrue()
            assertThat(sheetActivityStateHolder.checkoutSessionResponseCalls.awaitItem()).isNull()
            expectNoEvents()
        }
    }

    @Test
    fun `successful tax update marks pending during update then idle`() = runTest {
        val selection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        val selectionHolder = DefaultEmbeddedSelectionHolder(SavedStateHandle()).apply {
            setSelection(PaymentSelection.Saved(PaymentMethodFixtures.US_BANK_ACCOUNT))
        }
        val sheetActivityStateHolder = FakeSheetActivityStateHolder()
        val updateGate = CompletableDeferred<Result<CheckoutSessionResponse>>()
        val selector = DefaultSheetSavedPaymentMethodSelector(
            taxRegionUpdater = SheetTaxRegionUpdater(updateTaxRegion = { _, _, _ -> updateGate.await() }),
            paymentMethodMetadata = CHECKOUT_SESSION_METADATA,
            selectionHolder = selectionHolder,
            sheetActivityStateHolder = sheetActivityStateHolder,
        )

        sheetActivityStateHolder.savedPaymentMethodSelectionState.test {
            assertThat(awaitItem()).isEqualTo(SavedPaymentMethodSelectionState.Idle)

            var result: Result<Unit>? = null
            val job = launch(start = CoroutineStart.UNDISPATCHED) {
                result = selector.select(selection)
            }

            assertThat(awaitItem()).isEqualTo(
                SavedPaymentMethodSelectionState.Pending(selection.paymentMethod.id)
            )

            val refreshedResponse = CheckoutSessionResponseFactory.create(id = "refreshed_response")
            updateGate.complete(Result.success(refreshedResponse))
            advanceUntilIdle()

            assertThat(awaitItem()).isEqualTo(SavedPaymentMethodSelectionState.Idle)
            assertThat(job.isCompleted).isTrue()
            assertThat(result?.isSuccess).isTrue()
            assertThat(selectionHolder.selection.value).isEqualTo(selection)
            assertThat(sheetActivityStateHolder.checkoutSessionResponse).isEqualTo(refreshedResponse)
            sheetActivityStateHolder.checkoutSessionResponseCalls.awaitItem()
            expectNoEvents()
        }
        sheetActivityStateHolder.validate()
    }

    @Test
    fun `failed tax update marks failed and leaves selection and response unchanged`() = runTest {
        val initialSelection = PaymentSelection.Saved(PaymentMethodFixtures.US_BANK_ACCOUNT)
        val selection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        val initialResponse = CheckoutSessionResponseFactory.create(id = "previous_response")
        val selectionHolder = DefaultEmbeddedSelectionHolder(SavedStateHandle()).apply {
            setSelection(initialSelection)
        }
        val sheetActivityStateHolder = FakeSheetActivityStateHolder().apply {
            setInitialCheckoutSessionResponse(initialResponse)
        }
        val error = RuntimeException("Tax region update failed")
        val selector = DefaultSheetSavedPaymentMethodSelector(
            taxRegionUpdater = SheetTaxRegionUpdater(updateTaxRegion = { _, _, _ -> Result.failure(error) }),
            paymentMethodMetadata = CHECKOUT_SESSION_METADATA,
            selectionHolder = selectionHolder,
            sheetActivityStateHolder = sheetActivityStateHolder,
        )

        sheetActivityStateHolder.savedPaymentMethodSelectionState.test {
            assertThat(awaitItem()).isEqualTo(SavedPaymentMethodSelectionState.Idle)

            val result = selector.select(selection)

            assertThat(awaitItem()).isEqualTo(
                SavedPaymentMethodSelectionState.Pending(selection.paymentMethod.id)
            )
            assertThat(awaitItem()).isEqualTo(
                SavedPaymentMethodSelectionState.Failed(selection.paymentMethod.id, error.stripeErrorMessage())
            )
            assertThat(result.isFailure).isTrue()
            assertThat(selectionHolder.selection.value).isEqualTo(initialSelection)
            assertThat(sheetActivityStateHolder.checkoutSessionResponse).isEqualTo(initialResponse)
            expectNoEvents()
        }
        sheetActivityStateHolder.validate()
    }

    private fun runScenario(
        paymentMethodMetadata: PaymentMethodMetadata,
        initialSelection: PaymentSelection?,
        selection: PaymentSelection.Saved,
        initialResponse: CheckoutSessionResponse?,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val selectionHolder = DefaultEmbeddedSelectionHolder(SavedStateHandle()).apply {
            setSelection(initialSelection)
        }
        val sheetActivityStateHolder = FakeSheetActivityStateHolder().apply {
            setInitialCheckoutSessionResponse(initialResponse)
        }
        val selector = DefaultSheetSavedPaymentMethodSelector(
            taxRegionUpdater = SheetTaxRegionUpdater(
                taxRegionUpdater = checkoutSessionTaxRegionUpdater(),
            ),
            paymentMethodMetadata = paymentMethodMetadata,
            selectionHolder = selectionHolder,
            sheetActivityStateHolder = sheetActivityStateHolder,
        )

        val scenario = Scenario(
            selector = selector,
            selectionHolder = selectionHolder,
            sheetActivityStateHolder = sheetActivityStateHolder,
            selection = selection,
        )
        scenario.block()
        sheetActivityStateHolder.validate()
    }

    private fun checkoutSessionTaxRegionUpdater(): CheckoutSessionTaxRegionUpdater {
        val checkoutSessionRepository = CheckoutSessionRepository(
            stripeNetworkClient = DefaultStripeNetworkClient(),
            analyticsRequestExecutor = FakeAnalyticsRequestExecutor(),
            paymentAnalyticsRequestFactory = PaymentAnalyticsRequestFactory(
                context = ApplicationProvider.getApplicationContext(),
                publishableKey = "pk_test_123",
            ),
            apiRequestOptionsProvider = {
                ApiRequest.Options(
                    apiKey = DEFAULT_API_CONFIG.publishableKey,
                    stripeAccount = DEFAULT_API_CONFIG.stripeAccountId,
                )
            },
        )

        return CheckoutSessionTaxRegionUpdater(checkoutSessionRepository)
    }

    private data class Scenario(
        val selector: DefaultSheetSavedPaymentMethodSelector,
        val selectionHolder: EmbeddedSelectionHolder,
        val sheetActivityStateHolder: FakeSheetActivityStateHolder,
        val selection: PaymentSelection.Saved,
    )

    private companion object {
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
