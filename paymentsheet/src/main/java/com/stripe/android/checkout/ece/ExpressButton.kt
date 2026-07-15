package com.stripe.android.checkout.ece

import com.stripe.android.CardBrandFilter
import com.stripe.android.CardFundingFilter
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.link.ui.LinkButtonState
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardBrandFilter
import com.stripe.android.model.LinkBrand
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
                paymentMethodMetadata: PaymentMethodMetadata
            ): Link {
                val linkConfiguration = paymentMethodMetadata.linkState?.configuration
                return Link(
                    state = LinkButtonState.create(
                        enableDefaultValues = linkConfiguration?.enableDisplayableDefaultValuesInEce == true,
                        linkEmail = null,
                        paymentDetails = null,
                    ),
                    theme = PaymentSheet.ButtonThemes.LinkButtonTheme.DEFAULT,
                    linkBrand = paymentMethodMetadata.linkBrand,
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
            ): GooglePay {
                return GooglePay(
                    allowCreditCards = true,
                    googlePayButtonType = GooglePayButtonType.Pay,
                    cardBrandFilter = paymentMethodMetadata.cardBrandFilter,
                    cardFundingFilter = paymentMethodMetadata.cardFundingFilter,
                    billingAddressParameters = paymentMethodMetadata.billingDetailsCollectionConfiguration
                        .toBillingAddressParameters(),
                    additionalEnabledNetworks = emptyList(),
                )
            }
        }
    }
}
