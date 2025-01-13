package com.stripe.android.paymentelement.embedded

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
@RunWith(RobolectricTestRunner::class)
internal class PaymentOptionDisplayDataFactoryTest {

    private val displayDataFactory = PaymentOptionDisplayDataFactory(
        iconLoader = mock(),
        context = ApplicationProvider.getApplicationContext(),
    )

    @Test
    fun `create attaches PaymentMethod BillingDetails as PaymentSheet BillingDetails `() {
        val option = displayDataFactory.create(
            selection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION.copy(
                paymentMethodCreateParams = PaymentMethodCreateParams(
                    code = "card",
                    requiresMandate = false,
                    billingDetails = paymentMethodBillingDetails
                )
            ),
            paymentMethodMetadata = paymentMethodMetadata,
        )

        assertThat(option?.billingDetails).isEqualTo(paymentSheetBillingDetails)
    }

    @Test
    fun `create does not attach BillingDetails for Google Pay`() {
        val option = displayDataFactory.create(
            selection = PaymentSelection.GooglePay,
            paymentMethodMetadata = paymentMethodMetadata
        )

        assertThat(option?.billingDetails).isNull()
    }

    @Test
    fun `selecting saved card does not attach mandate to paymentMethodMetadata`() {
        val option = displayDataFactory.create(
            selection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
            paymentMethodMetadata = paymentMethodMetadata
        )

        assertThat(option?.mandateText).isNull()
    }

    @Test
    fun `selecting new card does attach mandate to paymentMethodMetadata`() {
        val option = displayDataFactory.create(
            selection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION,
            paymentMethodMetadata = paymentMethodMetadata
        )

        assertThat(option?.mandateText).isNotNull()
    }

    @Test
    fun `selecting google pay does not attach mandate to paymentMethodMetadata`() {
        val option = displayDataFactory.create(
            selection = PaymentSelection.GooglePay,
            paymentMethodMetadata = paymentMethodMetadata
        )

        assertThat(option?.mandateText).isNull()
    }

    companion object {
        private val paymentSheetBillingDetails = PaymentSheet.BillingDetails(
            name = "Jenny Rosen",
            email = "foo@bar.com",
            phone = "+13105551234",
            address = PaymentSheet.Address(
                postalCode = "94111",
                country = "US",
            ),
        )
        private val paymentMethodBillingDetails = PaymentMethod.BillingDetails(
            address = Address(
                postalCode = "94111",
                country = "US",
            ),
            email = "foo@bar.com",
            name = "Jenny Rosen",
            phone = "+13105551234"
        )

        private val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = SetupIntentFixtures.SI_SUCCEEDED.copy(
                paymentMethodTypes = listOf("card", "cashapp", "google_pay"),
            ),
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(),
            allowsDelayedPaymentMethods = false,
            allowsPaymentMethodsRequiringShippingAddress = false,
            isGooglePayReady = true,
            cbcEligibility = CardBrandChoiceEligibility.Ineligible,
        )
    }
}
