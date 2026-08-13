@file:OptIn(CheckoutSessionPreview::class)
package com.stripe.android.elements.ece

import com.stripe.android.CardBrandFilter
import com.stripe.android.CardFundingFilter
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.checkout.GooglePayConfiguration
import com.stripe.android.checkout.asGooglePayButtonType
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkExpressMode
import com.stripe.android.link.ui.LinkButtonState
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.WalletType
import com.stripe.android.model.CardFunding
import com.stripe.android.model.LinkBrand
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.GooglePayButtonType
import com.stripe.android.paymentsheet.model.PaymentSelection

internal sealed interface ExpressButton {

    fun toSelection(): PaymentSelection
    fun toWalletType(): WalletType

    data class Link(
        val state: LinkButtonState,
        val linkBrand: LinkBrand,
        val theme: PaymentSheet.ButtonThemes.LinkButtonTheme,
    ) : ExpressButton {

        override fun toSelection(): PaymentSelection {
            return PaymentSelection.Link(
                brand = linkBrand,
                linkExpressMode = LinkExpressMode.DISABLED,
            )
        }

        override fun toWalletType(): WalletType = WalletType.Link

        companion object {
            fun create(
                paymentMethodMetadata: PaymentMethodMetadata,
                linkAccountInfo: LinkAccountUpdate.Value,
            ): Link {
                val linkConfiguration = paymentMethodMetadata.linkState?.configuration
                val linkAccount = linkAccountInfo.account
                return Link(
                    state = LinkButtonState.create(
                        enableDefaultValues = linkConfiguration?.enableDisplayableDefaultValuesInEce == true,
                        linkEmail = linkAccount?.email,
                        paymentDetails = linkAccount?.displayablePaymentDetails,
                    ),
                    theme = PaymentSheet.ButtonThemes.LinkButtonTheme.DEFAULT,
                    linkBrand = paymentMethodMetadata.effectiveLinkBrand(linkAccount),
                )
            }
        }
    }

    data class GooglePay(
        val googlePayButtonType: GooglePayButtonType,
        val billingAddressParameters: GooglePayJsonFactory.BillingAddressParameters,
        val allowCreditCards: Boolean,
        val cardBrandFilter: CardBrandFilter,
        val cardFundingFilter: CardFundingFilter,
        val additionalEnabledNetworks: List<String>,
        val shippingAddressRequired: Boolean,
    ) : ExpressButton {

        override fun toSelection(): PaymentSelection = PaymentSelection.GooglePay

        override fun toWalletType(): WalletType = WalletType.GooglePay

        companion object {
            fun create(
                paymentMethodMetadata: PaymentMethodMetadata,
                googlePayConfiguration: GooglePayConfiguration.State,
                shippingAddressRequired: Boolean,
            ): GooglePay {
                return GooglePay(
                    allowCreditCards = paymentMethodMetadata.cardFundingFilter.isAccepted(CardFunding.Credit),
                    googlePayButtonType = googlePayConfiguration.buttonType.asGooglePayButtonType(),
                    cardBrandFilter = paymentMethodMetadata.cardBrandFilter,
                    cardFundingFilter = paymentMethodMetadata.cardFundingFilter,
                    billingAddressParameters = paymentMethodMetadata.billingDetailsCollectionConfiguration
                        .toBillingAddressParameters(),
                    additionalEnabledNetworks = googlePayConfiguration.additionalEnabledNetworks,
                    shippingAddressRequired = shippingAddressRequired,
                )
            }
        }
    }
}
