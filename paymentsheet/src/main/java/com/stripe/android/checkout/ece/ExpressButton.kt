@file:OptIn(CheckoutSessionPreview::class)
package com.stripe.android.checkout.ece

import com.stripe.android.CardBrandFilter
import com.stripe.android.CardFundingFilter
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.checkout.GooglePayConfiguration
import com.stripe.android.checkout.asGooglePayButtonType
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.ui.LinkButtonState
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.CardFunding
import com.stripe.android.model.LinkBrand
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.GooglePayButtonType

internal sealed interface ExpressButton {

    data class Link(
        val state: LinkButtonState,
        val linkBrand: LinkBrand,
        val theme: PaymentSheet.ButtonThemes.LinkButtonTheme,
    ) : ExpressButton {
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
    ) : ExpressButton {
        companion object {
            fun create(
                paymentMethodMetadata: PaymentMethodMetadata,
                googlePayConfiguration: GooglePayConfiguration.State,
            ): GooglePay {
                return GooglePay(
                    allowCreditCards = paymentMethodMetadata.cardFundingFilter.isAccepted(CardFunding.Credit),
                    googlePayButtonType = googlePayConfiguration.buttonType.asGooglePayButtonType(),
                    cardBrandFilter = paymentMethodMetadata.cardBrandFilter,
                    cardFundingFilter = paymentMethodMetadata.cardFundingFilter,
                    billingAddressParameters = paymentMethodMetadata.billingDetailsCollectionConfiguration
                        .toBillingAddressParameters(),
                    additionalEnabledNetworks = googlePayConfiguration.additionalEnabledNetworks,
                )
            }
        }
    }
}
