package com.stripe.android.paymentsheet.state

import androidx.annotation.StringRes
import com.stripe.android.CardBrandFilter
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher.BillingAddressConfig
import com.stripe.android.model.PaymentMethod.Type.Card
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.model.GooglePayButtonType

internal data class WalletsState(
    val link: Link?,
    val googlePay: GooglePay?,
    val buttonsEnabled: Boolean,
    @StringRes val dividerTextResource: Int,
    val onGooglePayPressed: () -> Unit,
    val onLinkPressed: () -> Unit,
    val cardBrandFilter: CardBrandFilter
) {

    data class Link(
        val email: String?,
    )

    data class GooglePay(
        val buttonType: GooglePayButtonType,
        val allowCreditCards: Boolean,
        val billingAddressParameters: GooglePayJsonFactory.BillingAddressParameters?,
    )

    companion object {

        fun create(
            isLinkAvailable: Boolean?,
            linkEmail: String?,
            isGooglePayReady: Boolean,
            googlePayButtonType: GooglePayButtonType,
            buttonsEnabled: Boolean,
            paymentMethodTypes: List<String>,
            googlePayLauncherConfig: GooglePayPaymentMethodLauncher.Config?,
            onGooglePayPressed: () -> Unit,
            onLinkPressed: () -> Unit,
            isSetupIntent: Boolean,
            cardBrandFilter: CardBrandFilter
        ): WalletsState? {
            val link = Link(email = linkEmail).takeIf { isLinkAvailable == true }

            val googlePay = GooglePay(
                allowCreditCards = googlePayLauncherConfig?.allowCreditCards ?: false,
                buttonType = googlePayButtonType,
                billingAddressParameters = googlePayLauncherConfig?.let {
                    GooglePayJsonFactory.BillingAddressParameters(
                        isRequired = it.billingAddressConfig.isRequired,
                        format = when (it.billingAddressConfig.format) {
                            BillingAddressConfig.Format.Min -> {
                                GooglePayJsonFactory.BillingAddressParameters.Format.Min
                            }
                            BillingAddressConfig.Format.Full -> {
                                GooglePayJsonFactory.BillingAddressParameters.Format.Full
                            }
                        },
                        isPhoneNumberRequired = it.billingAddressConfig.isPhoneNumberRequired,
                    )
                },
            ).takeIf { isGooglePayReady }

            return if (link != null || googlePay != null) {
                WalletsState(
                    link = link,
                    googlePay = googlePay,
                    buttonsEnabled = buttonsEnabled,
                    dividerTextResource = if (paymentMethodTypes.singleOrNull() == Card.code && !isSetupIntent) {
                        R.string.stripe_paymentsheet_or_pay_with_card
                    } else if (paymentMethodTypes.singleOrNull() == null && !isSetupIntent) {
                        R.string.stripe_paymentsheet_or_pay_using
                    } else if (paymentMethodTypes.singleOrNull() == Card.code && isSetupIntent) {
                        R.string.stripe_paymentsheet_or_use_a_card
                    } else {
                        R.string.stripe_paymentsheet_or_use
                    },
                    onGooglePayPressed = onGooglePayPressed,
                    onLinkPressed = onLinkPressed,
                    cardBrandFilter = cardBrandFilter
                )
            } else {
                null
            }
        }
    }
}
