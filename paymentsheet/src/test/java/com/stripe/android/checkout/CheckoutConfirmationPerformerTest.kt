package com.stripe.android.checkout

import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.LinkBrand
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.confirmation.gpay.GooglePayConfirmationOption
import com.stripe.android.paymentelement.confirmation.link.LinkConfirmationOption
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.utils.LinkTestUtils
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class CheckoutConfirmationPerformerTest {

    @Test
    fun `confirm does nothing when state is not loaded`() = runScenario(state = null) {
        performer.confirm()
    }

    @Test
    fun `confirm does nothing when there is no selection`() = runScenario(
        state = googlePayState(paymentSelection = null),
    ) {
        performer.confirm()
    }

    @Test
    fun `confirm does nothing when the selection cannot be converted to a confirmation option`() = runScenario(
        // Google Pay is selected but the configuration has no Google Pay set, so toConfirmationOption
        // returns null and there is nothing to confirm.
        state = CheckoutControllerStateFactory.create(
            paymentSelection = PaymentSelection.GooglePay,
        ),
    ) {
        performer.confirm()
    }

    @Test
    fun `confirm starts confirmation with a Google Pay option`() = runScenario(
        statusBarColor = STATUS_BAR_COLOR,
        state = googlePayState(paymentSelection = PaymentSelection.GooglePay),
    ) {
        performer.confirm()

        val args = confirmationHandler.startTurbine.awaitItem()
        assertThat(args.confirmationOption).isInstanceOf<GooglePayConfirmationOption>()
        assertThat(args.paymentMethodMetadata).isEqualTo(stateHolder.state?.paymentMethodMetadata)
        assertThat(args.statusBarColor).isEqualTo(STATUS_BAR_COLOR)
    }

    @Test
    fun `confirm applies qualified automatic tax Google Pay presentation`() {
        val response = CheckoutSessionResponseFactory.create(
            amount = 1100L,
            automaticTaxEnabled = true,
            taxAddressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
            taxStatus = CheckoutSessionResponse.TaxStatus.READY,
            totalSummary = CheckoutSessionResponse.TotalSummaryResponse(
                subtotal = 1000L,
                totalDueToday = 1100L,
                totalAmountDue = 1100L,
                discountAmounts = emptyList(),
                taxAmounts = listOf(
                    CheckoutSessionResponse.TaxAmount(
                        amount = 100L,
                        inclusive = false,
                        displayName = "Sales tax",
                        percentage = 10.0,
                    )
                ),
                shippingRate = null,
                appliedBalance = null,
            ),
        )
        val state = googlePayState(paymentSelection = PaymentSelection.GooglePay).copy(
            checkoutSessionResponse = response,
            hasQualifiedDefaultBillingTaxEstimate = true,
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(checkoutSessionResponse = response),
        )

        runScenario(state = state) {
            performer.confirm()

            val option = confirmationHandler.startTurbine.awaitItem().confirmationOption
                as GooglePayConfirmationOption
            assertThat(option.config.customLabel).isEqualTo("Estimated total (final tax may vary)")
            assertThat(option.config.totalPriceStatus)
                .isEqualTo(GooglePayJsonFactory.TransactionInfo.TotalPriceStatus.Estimated)
            assertThat(option.config.forceBillingAddressCollection).isTrue()
            assertThat(option.config.displayItems).hasSize(2)
        }
    }

    @Test
    fun `confirm starts confirmation with a Link option`() = runScenario(
        state = CheckoutControllerStateFactory.create(
            paymentSelection = PaymentSelection.Link(brand = LinkBrand.Link),
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                linkState = LinkState(
                    configuration = LinkTestUtils.createLinkConfiguration(),
                    loginState = LinkState.LoginState.NeedsVerification,
                    signupMode = null,
                ),
            ),
        ),
    ) {
        performer.confirm()

        val args = confirmationHandler.startTurbine.awaitItem()
        assertThat(args.confirmationOption).isInstanceOf<LinkConfirmationOption>()
    }

    private fun googlePayState(
        paymentSelection: PaymentSelection?,
    ): CheckoutControllerState {
        return CheckoutControllerStateFactory.create(
            paymentSelection = paymentSelection,
            embeddedConfiguration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.")
                .googlePay(
                    PaymentSheet.GooglePayConfiguration(
                        environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                        countryCode = "US",
                    )
                )
                .build(),
        )
    }

    private fun runScenario(
        state: CheckoutControllerState?,
        statusBarColor: Int? = null,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val confirmationHandler = FakeConfirmationHandler()
        val stateHolder = CheckoutControllerStateFactory.createStateHolder(SavedStateHandle())
        stateHolder.state = state
        val performer = CheckoutConfirmationPerformer(
            confirmationHandler = confirmationHandler,
            stateHolder = stateHolder,
            resources = ApplicationProvider.getApplicationContext<android.content.Context>().resources,
            statusBarColor = statusBarColor,
            viewModelScope = backgroundScope,
        )

        Scenario(
            performer = performer,
            confirmationHandler = confirmationHandler,
            stateHolder = stateHolder,
        ).block()

        confirmationHandler.validate()
    }

    private class Scenario(
        val performer: CheckoutConfirmationPerformer,
        val confirmationHandler: FakeConfirmationHandler,
        val stateHolder: CheckoutControllerStateHolder,
    )

    private companion object {
        const val STATUS_BAR_COLOR = 0x00FF00
    }
}
