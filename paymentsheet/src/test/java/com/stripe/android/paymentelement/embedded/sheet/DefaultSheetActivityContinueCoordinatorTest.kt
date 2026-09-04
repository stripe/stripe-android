package com.stripe.android.paymentelement.embedded.sheet

import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.CheckoutSessionTaxRegionUpdater
import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.checkouttesting.checkoutUpdate
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
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
import com.stripe.android.paymentsheet.repositories.ElementsSessionClientParams
import com.stripe.android.testing.FakeAnalyticsRequestExecutor
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class DefaultSheetActivityContinueCoordinatorTest {

    @get:Rule
    val networkRule = NetworkRule()

    @Test
    fun `without tax region update returns complete synchronously`() = runScenario(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
    ) {
        continueCoordinator.onContinue()

        assertThat(stateHolder.resultTurbine.awaitItem()).isEqualTo(
            completeResult(checkoutSessionResponse = null)
        )
        stateHolder.updateProcessingTurbine.expectNoEvents()
    }

    @Test
    fun `successful tax region update returns updated response`() {
        networkRule.checkoutUpdate { response ->
            response.testBodyFromFile("checkout-session-init.json")
        }

        runScenario(paymentMethodMetadata = CHECKOUT_SESSION_METADATA) {
            continueCoordinator.onContinue()
            selectionHolder.setSelection(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
            testScope.runCurrent()

            assertThat(stateHolder.updateProcessingTurbine.awaitItem()).isTrue()
            val result = stateHolder.resultTurbine.awaitItem() as EmbeddedActivityResult.Complete
            assertThat(result).isEqualTo(
                completeResult(
                    selection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION,
                    checkoutSessionResponse = result.checkoutSessionResponse,
                )
            )
            assertThat(result.checkoutSessionResponse?.id).isEqualTo(DEFAULT_CHECKOUT_SESSION_ID)
        }
    }

    @Test
    fun `failed tax region update stops processing and shows error`() {
        networkRule.checkoutUpdate { response ->
            response.setResponseCode(400)
            response.setBody("""{"error":{"message":"$ERROR_MESSAGE"}}""")
        }

        runScenario(paymentMethodMetadata = CHECKOUT_SESSION_METADATA) {
            continueCoordinator.onContinue()
            testScope.runCurrent()

            assertThat(stateHolder.updateProcessingTurbine.awaitItem()).isTrue()
            assertThat(stateHolder.updateProcessingTurbine.awaitItem()).isFalse()
            assertThat(stateHolder.updateErrorTurbine.awaitItem())
                .isEqualTo(IllegalStateException(ERROR_MESSAGE).stripeErrorMessage())
            stateHolder.resultTurbine.expectNoEvents()
        }
    }

    private fun runScenario(
        paymentMethodMetadata: PaymentMethodMetadata,
        selection: PaymentSelection? = PaymentMethodFixtures.CARD_PAYMENT_SELECTION,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val selectionHolder = DefaultEmbeddedSelectionHolder(SavedStateHandle()).apply {
            setSelection(selection)
        }
        val customerStateHolder = FakeCustomerStateHolder()
        val stateHolder = FakeSheetActivityStateHolder()
        val taxRegionUpdater = SheetTaxRegionUpdater(
            taxRegionUpdater = checkoutSessionTaxRegionUpdater(),
        )
        val continueCoordinator = DefaultSheetActivityContinueCoordinator(
            taxRegionUpdater = taxRegionUpdater,
            paymentMethodMetadata = paymentMethodMetadata,
            stateHolder = stateHolder,
            selectionHolder = selectionHolder,
            customerStateHolder = customerStateHolder,
            launchMode = LAUNCH_MODE,
            coroutineScope = this,
        )

        Scenario(
            continueCoordinator = continueCoordinator,
            stateHolder = stateHolder,
            selectionHolder = selectionHolder,
            customerStateHolder = customerStateHolder,
            testScope = this,
        ).block()

        stateHolder.validate()
        customerStateHolder.validate()
    }

    private fun checkoutSessionTaxRegionUpdater(): CheckoutSessionTaxRegionUpdater {
        val checkoutSessionRepository = CheckoutSessionRepository(
            clientParams = ElementsSessionClientParams(
                mobileAppId = "com.stripe.android.paymentsheet.test",
                mobileSessionIdProvider = { "test_session" },
            ),
            stripeNetworkClient = DefaultStripeNetworkClient(),
            analyticsRequestExecutor = FakeAnalyticsRequestExecutor(),
            paymentAnalyticsRequestFactory = PaymentAnalyticsRequestFactory(
                context = ApplicationProvider.getApplicationContext(),
                publishableKey = "pk_test_123",
            ),
            publishableKeyProvider = { "pk_test_123" },
            stripeAccountIdProvider = { null },
        )

        return CheckoutSessionTaxRegionUpdater(checkoutSessionRepository)
    }

    private data class Scenario(
        val continueCoordinator: DefaultSheetActivityContinueCoordinator,
        val stateHolder: FakeSheetActivityStateHolder,
        val selectionHolder: EmbeddedSelectionHolder,
        val customerStateHolder: FakeCustomerStateHolder,
        val testScope: TestScope,
    ) {
        fun completeResult(
            selection: PaymentSelection? = PaymentMethodFixtures.CARD_PAYMENT_SELECTION,
            checkoutSessionResponse: CheckoutSessionResponse?,
        ): EmbeddedActivityResult.Complete {
            return EmbeddedActivityResult.Complete(
                selection = selection,
                previousNewSelections = selectionHolder.previousNewSelections,
                hasBeenConfirmed = false,
                customerState = customerStateHolder.customer.value,
                checkoutSessionResponse = checkoutSessionResponse,
                shouldInvokeSelectionCallback = false,
                launchMode = LAUNCH_MODE,
            )
        }
    }

    private companion object {
        const val ERROR_MESSAGE = "Tax region update failed"
        val LAUNCH_MODE = EmbeddedLaunchMode.PaymentOptions(isLoading = false)
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
