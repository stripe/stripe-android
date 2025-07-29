package com.stripe.android.paymentelement.embedded.content

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.model.CountryCode
import com.stripe.android.elements.BillingDetails
import com.stripe.android.link.LinkPaymentMethod
import com.stripe.android.link.TestFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.Address
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerShippingAddress
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.paymentelement.ShippingDetailsInPaymentOptionPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner

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
    fun `create does not attach BillingDetails for ShopPay`() {
        val option = displayDataFactory.create(
            selection = PaymentSelection.ShopPay,
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

    @Test
    fun `selecting shop pay does not attach mandate to paymentMethodMetadata`() {
        val option = displayDataFactory.create(
            selection = PaymentSelection.ShopPay,
            paymentMethodMetadata = paymentMethodMetadata
        )

        assertThat(option?.mandateText).isNull()
    }

    @OptIn(ShippingDetailsInPaymentOptionPreview::class)
    @Test
    fun `create adds shipping details for verified Link user`() {
        val option = displayDataFactory.create(
            selection = PaymentSelection.Link(
                selectedPayment = LinkPaymentMethod.ConsumerPaymentDetails(
                    details = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD,
                    collectedCvc = null,
                    billingPhone = null,
                ),
                shippingAddress = ConsumerShippingAddress(
                    id = "csmr_addr_123",
                    isDefault = true,
                    address = ConsumerPaymentDetails.BillingAddress(
                        name = "Jenny Rosen",
                        line1 = "123 Main St",
                        line2 = null,
                        locality = "San Francisco",
                        administrativeArea = "CA",
                        postalCode = "94111",
                        countryCode = CountryCode.US,
                    ),
                    unredactedPhoneNumber = "+15555555555",
                ),
            ),
            paymentMethodMetadata = paymentMethodMetadata
        )

        assertThat(option?.shippingDetails).isEqualTo(
            AddressDetails(
                name = "Jenny Rosen",
                phoneNumber = "+15555555555",
                address = PaymentSheet.Address(
                    line1 = "123 Main St",
                    line2 = null,
                    city = "San Francisco",
                    state = "CA",
                    postalCode = "94111",
                    country = "US",
                ),
                isCheckboxSelected = null,
            )
        )
    }

    @OptIn(ShippingDetailsInPaymentOptionPreview::class)
    @Test
    fun `create adds no shipping details for unverified Link user`() {
        val option = displayDataFactory.create(
            selection = PaymentSelection.Link(
                selectedPayment = null,
                shippingAddress = null,
            ),
            paymentMethodMetadata = paymentMethodMetadata
        )

        assertThat(option?.shippingDetails).isNull()
    }

    companion object {
        private val paymentSheetBillingDetails = BillingDetails(
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
            billingDetailsCollectionConfiguration = BillingDetailsCollectionConfiguration(),
            allowsDelayedPaymentMethods = false,
            allowsPaymentMethodsRequiringShippingAddress = false,
            isGooglePayReady = true,
            cbcEligibility = CardBrandChoiceEligibility.Ineligible,
        )
    }
}
