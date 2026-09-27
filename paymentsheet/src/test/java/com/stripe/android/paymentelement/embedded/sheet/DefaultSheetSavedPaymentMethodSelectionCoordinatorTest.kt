package com.stripe.android.paymentelement.embedded.sheet

import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.CheckoutSessionTaxRegionUpdater
import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.checkouttesting.checkoutUpdate
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.link.account.LinkAccountHolder
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
import com.stripe.android.paymentelement.embedded.EmbeddedActivityResult
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.FakeCustomerStateHolder
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionRepository
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.testing.FakeAnalyticsRequestExecutor
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class DefaultSheetSavedPaymentMethodSelectionCoordinatorTest {

    @get:Rule
    val networkRule = NetworkRule()

    @Test
    fun `selection without tax update sets selection and emits complete result`() = runScenario(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        initialSelection = PaymentSelection.Saved(PaymentMethodFixtures.US_BANK_ACCOUNT),
        selection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
    ) {
        val result = coordinator.select(selection)

        assertThat(result.isSuccess).isTrue()
        assertThat(selectionHolder.selection.value).isEqualTo(selection)

        val complete = stateHolder.resultTurbine.awaitItem() as EmbeddedActivityResult.Complete
        assertThat(complete.selection).isEqualTo(selection)
        assertThat(complete.checkoutSessionResponse).isNull()
        assertThat(complete.shouldInvokeSelectionCallback).isTrue()
        assertThat(complete.launchMode).isEqualTo(LAUNCH_MODE)
    }

    @Test
    fun `successful tax update emits refreshed response`() {
        networkRule.checkoutUpdate { response ->
            response.testBodyFromFile("checkout-session-init.json")
        }

        runScenario(
            paymentMethodMetadata = CHECKOUT_SESSION_METADATA,
            initialSelection = PaymentSelection.Saved(PaymentMethodFixtures.US_BANK_ACCOUNT),
            selection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
        ) {
            val result = coordinator.select(selection)

            assertThat(result.isSuccess).isTrue()
            assertThat(selectionHolder.selection.value).isEqualTo(selection)

            val complete = stateHolder.resultTurbine.awaitItem() as EmbeddedActivityResult.Complete
            assertThat(complete.selection).isEqualTo(selection)
            assertThat(complete.checkoutSessionResponse?.id).isEqualTo(DEFAULT_CHECKOUT_SESSION_ID)
            assertThat(complete.shouldInvokeSelectionCallback).isTrue()
            assertThat(complete.launchMode).isEqualTo(LAUNCH_MODE)
        }
    }

    @Test
    fun `failed tax update leaves selection and result unchanged`() {
        networkRule.checkoutUpdate { response ->
            response.setResponseCode(400)
            response.setBody("""{"error":{"message":"Tax region update failed"}}""")
        }

        val initialSelection = PaymentSelection.Saved(PaymentMethodFixtures.US_BANK_ACCOUNT)
        val selection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        runScenario(
            paymentMethodMetadata = CHECKOUT_SESSION_METADATA,
            initialSelection = initialSelection,
            selection = selection,
        ) {
            val result = coordinator.select(selection)

            assertThat(result.isFailure).isTrue()
            assertThat(selectionHolder.selection.value).isEqualTo(initialSelection)
            stateHolder.resultTurbine.expectNoEvents()
        }
    }

    private fun runScenario(
        paymentMethodMetadata: PaymentMethodMetadata,
        initialSelection: PaymentSelection?,
        selection: PaymentSelection.Saved,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val selectionHolder = DefaultEmbeddedSelectionHolder(SavedStateHandle()).apply {
            setSelection(initialSelection)
        }
        val customerStateHolder = FakeCustomerStateHolder()
        val stateHolder = FakeSheetActivityStateHolder()
        val coordinator = DefaultSheetSavedPaymentMethodSelectionCoordinator(
            taxRegionUpdater = SheetTaxRegionUpdater(
                taxRegionUpdater = checkoutSessionTaxRegionUpdater(),
            ),
            paymentMethodMetadata = paymentMethodMetadata,
            stateHolder = stateHolder,
            selectionHolder = selectionHolder,
            customerStateHolder = customerStateHolder,
            linkAccountHolder = LinkAccountHolder(SavedStateHandle()),
            launchMode = LAUNCH_MODE,
        )

        Scenario(
            coordinator = coordinator,
            stateHolder = stateHolder,
            selectionHolder = selectionHolder,
            customerStateHolder = customerStateHolder,
            selection = selection,
        ).block()

        stateHolder.updateProcessingTurbine.expectNoEvents()
        stateHolder.updateErrorTurbine.expectNoEvents()
        stateHolder.validate()
        customerStateHolder.validate()
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
        val coordinator: DefaultSheetSavedPaymentMethodSelectionCoordinator,
        val stateHolder: FakeSheetActivityStateHolder,
        val selectionHolder: EmbeddedSelectionHolder,
        val customerStateHolder: FakeCustomerStateHolder,
        val selection: PaymentSelection.Saved,
    )

    private companion object {
        val LAUNCH_MODE = EmbeddedLaunchMode.Manage
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
