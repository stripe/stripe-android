@file:OptIn(CheckoutSessionPreview::class)

package com.stripe.android.checkout.ece

import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.GooglePayConfiguration
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.TestFactory
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.ui.LinkButtonState
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardBrandFilter
import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardFundingFilter
import com.stripe.android.model.DisplayablePaymentDetails
import com.stripe.android.model.LinkBrand
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.GooglePayButtonType
import com.stripe.android.paymentsheet.state.LinkState
import org.junit.Test

internal class ExpressButtonTest {

    @Test
    fun `Link create uses link brand from payment method metadata`() {
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            linkBrand = LinkBrand.Onelink,
        )

        val button = ExpressButton.Link.create(
            paymentMethodMetadata = paymentMethodMetadata,
            linkAccountInfo = LinkAccountUpdate.Value(null),
        )

        assertThat(button.linkBrand).isEqualTo(LinkBrand.Onelink)
    }

    @Test
    fun `Link create uses link brand from account when logged in`() {
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            linkBrand = LinkBrand.Onelink,
        )
        val linkAccount = LinkAccount(
            consumerSession = TestFactory.CONSUMER_SESSION.copy(linkBrand = LinkBrand.Link)
        )

        val button = ExpressButton.Link.create(
            paymentMethodMetadata = paymentMethodMetadata,
            linkAccountInfo = LinkAccountUpdate.Value(linkAccount),
        )

        assertThat(button.linkBrand).isEqualTo(LinkBrand.Link)
    }

    @Test
    fun `Link create uses link account email`() {
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create()

        val button = ExpressButton.Link.create(
            paymentMethodMetadata = paymentMethodMetadata,
            linkAccountInfo = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT),
        )

        assertThat(button.state).isEqualTo(LinkButtonState.Email(TestFactory.LINK_ACCOUNT.email))
    }

    @Test
    fun `Link create uses link account payment details when enabled`() {
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            linkState = LinkState(
                configuration = TestFactory.LINK_CONFIGURATION.copy(
                    enableDisplayableDefaultValuesInEce = true,
                ),
                loginState = LinkState.LoginState.LoggedIn,
                signupMode = null,
            ),
        )
        val linkAccount = TestFactory.LINK_ACCOUNT.copy(
            displayablePaymentDetails = DisplayablePaymentDetails(
                defaultCardBrand = "VISA",
                defaultPaymentType = "CARD",
                last4 = "4242",
                numberOfSavedPaymentDetails = 3L,
            ),
        )

        val button = ExpressButton.Link.create(
            paymentMethodMetadata = paymentMethodMetadata,
            linkAccountInfo = LinkAccountUpdate.Value(linkAccount),
        )

        val buttonState = button.state as LinkButtonState.DefaultPayment
        assertThat(buttonState.paymentUI.last4).isEqualTo("4242")
    }

    @Test
    fun `GooglePay create uses billing details collection configuration from payment method metadata`() {
        val billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
            phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
            address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
        )
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
        )

        val button = createGooglePayExpressButton(
            paymentMethodMetadata = paymentMethodMetadata,
        )

        assertThat(button.billingAddressParameters)
            .isEqualTo(billingDetailsCollectionConfiguration.toBillingAddressParameters())
    }

    @Test
    fun `GooglePay create uses button type from google pay configuration`() {
        val button = createGooglePayExpressButton(
            googlePayConfiguration = createGooglePayConfiguration(
                buttonType = GooglePayConfiguration.ButtonType.Checkout,
            ),
        )

        assertThat(button.googlePayButtonType).isEqualTo(GooglePayButtonType.Checkout)
    }

    @Test
    fun `GooglePay create uses additional enabled networks from google pay configuration`() {
        val button = createGooglePayExpressButton(
            googlePayConfiguration = createGooglePayConfiguration(
                additionalEnabledNetworks = listOf("INTERAC"),
            ),
        )

        assertThat(button.additionalEnabledNetworks).containsExactly("INTERAC")
    }

    @Test
    fun `GooglePay create uses card brand filter from payment method metadata`() {
        val cardBrandFilter = PaymentSheetCardBrandFilter(
            cardBrandAcceptance = PaymentSheet.CardBrandAcceptance.Allowed(
                brands = listOf(PaymentSheet.CardBrandAcceptance.BrandCategory.Visa),
            ),
        )
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            cardBrandFilter = cardBrandFilter,
        )

        val button = createGooglePayExpressButton(
            paymentMethodMetadata = paymentMethodMetadata,
        )

        assertThat(button.cardBrandFilter).isSameInstanceAs(cardBrandFilter)
    }

    @Test
    fun `GooglePay create uses card funding filter from payment method metadata`() {
        val cardFundingFilter = PaymentSheetCardFundingFilter(
            allowedCardFundingTypes = listOf(PaymentSheet.CardFundingType.Debit),
        )
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            cardFundingFilter = cardFundingFilter,
        )

        val button = createGooglePayExpressButton(
            paymentMethodMetadata = paymentMethodMetadata,
        )

        assertThat(button.cardFundingFilter).isSameInstanceAs(cardFundingFilter)
    }

    private fun createGooglePayExpressButton(
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        googlePayConfiguration: GooglePayConfiguration.State = createGooglePayConfiguration()
    ): ExpressButton.GooglePay {
         return ExpressButton.GooglePay.create(
             paymentMethodMetadata = paymentMethodMetadata,
             googlePayConfiguration = googlePayConfiguration
         )
    }

    private fun createGooglePayConfiguration(
        buttonType: GooglePayConfiguration.ButtonType = GooglePayConfiguration.ButtonType.Pay,
        additionalEnabledNetworks: List<String> = emptyList(),
    ): GooglePayConfiguration.State {
        return GooglePayConfiguration(
            GooglePayConfiguration.Environment.Test,
        )
            .buttonType(buttonType)
            .additionalEnabledNetworks(additionalEnabledNetworks)
            .build()
    }
}
