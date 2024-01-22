package com.stripe.android.paymentsheet.state

import androidx.annotation.StringRes
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher.BillingAddressConfig
import com.stripe.android.model.PaymentMethod.Type.Card
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.model.GooglePayButtonType
import com.stripe.android.paymentsheet.model.PaymentSheetViewState
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen

internal data class WalletsState(
    val link: Link?,
    val googlePay: GooglePay?,
    val buttonsEnabled: Boolean,
    @StringRes val dividerTextResource: Int,
    val onGooglePayPressed: () -> Unit,
    val onLinkPressed: () -> Unit,
) {

    data class Link(
        val email: String?,
    )

    data class GooglePay(
        val buttonState: PaymentSheetViewState?,
        val buttonType: GooglePayButtonType,
        val allowCreditCards: Boolean,
        val billingAddressParameters: GooglePayJsonFactory.BillingAddressParameters?,
    )

    companion object {

        fun create(
            isLinkAvailable: Boolean?,
            linkEmail: String?,
            googlePayState: GooglePayState,
            googlePayButtonState: PaymentSheetViewState?,
            googlePayButtonType: GooglePayButtonType,
            buttonsEnabled: Boolean,
            paymentMethodTypes: List<String>,
            googlePayLauncherConfig: GooglePayPaymentMethodLauncher.Config?,
            screen: PaymentSheetScreen,
            isCompleteFlow: Boolean,
            onGooglePayPressed: () -> Unit,
            onLinkPressed: () -> Unit,
        ): WalletsState? {
            if (!screen.showsWalletsHeader(isCompleteFlow)) {
                return null
            }

            val link = Link(email = linkEmail).takeIf { isLinkAvailable == true }

            val googlePay = GooglePay(
                buttonState = googlePayButtonState,
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
                }
            ).takeIf { googlePayState.isReadyForUse }

            return if (link != null || googlePay != null) {
                WalletsState(
                    link = link,
                    googlePay = googlePay,
                    buttonsEnabled = buttonsEnabled,
                    dividerTextResource = if (paymentMethodTypes.singleOrNull() == Card.code) {
                        R.string.stripe_paymentsheet_or_pay_with_card
                    } else {
                        R.string.stripe_paymentsheet_or_pay_using
                    },
                    onGooglePayPressed = onGooglePayPressed,
                    onLinkPressed = onLinkPressed,
                )
            } else {
                null
            }
        }
    }
}
