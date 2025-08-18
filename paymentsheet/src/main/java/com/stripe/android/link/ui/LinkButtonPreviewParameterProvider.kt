@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.link.ui

import androidx.annotation.RestrictTo
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.stripe.android.link.ui.wallet.toDefaultPaymentUI
import com.stripe.android.model.DisplayablePaymentDetails
import com.stripe.android.paymentsheet.PaymentSheet

internal class LinkButtonPreviewParameterProvider : PreviewParameterProvider<LinkButtonPreviewData> {
    override val values = sequenceOf(
        LinkButtonPreviewData(
            state = LinkButtonState.Default,
            theme = PaymentSheet.ButtonThemes.LinkButtonTheme.DEFAULT,
            enabled = true,
            name = "Default - Default Theme"
        ),
        LinkButtonPreviewData(
            state = LinkButtonState.Email("user@example.com"),
            theme = PaymentSheet.ButtonThemes.LinkButtonTheme.DEFAULT,
            enabled = true,
            name = "Email - Default Theme"
        ),
        LinkButtonPreviewData(
            state = LinkButtonState.Email("theop@email.com"),
            theme = PaymentSheet.ButtonThemes.LinkButtonTheme.DEFAULT,
            enabled = false,
            name = "Email Disabled - Default Theme"
        ),
        LinkButtonPreviewData(
            state = LinkButtonState.DefaultPayment(
                paymentUI = DisplayablePaymentDetails(
                    defaultCardBrand = "mastercard",
                    last4 = "4242",
                    defaultPaymentType = "CARD",
                ).toDefaultPaymentUI(true)!!
            ),
            theme = PaymentSheet.ButtonThemes.LinkButtonTheme.DEFAULT,
            enabled = true,
            name = "Mastercard - Default Theme"
        ),
        LinkButtonPreviewData(
            state = LinkButtonState.DefaultPayment(
                paymentUI = DisplayablePaymentDetails(
                    defaultCardBrand = "visa",
                    last4 = "1234",
                    defaultPaymentType = "CARD",
                ).toDefaultPaymentUI(true)!!
            ),
            theme = PaymentSheet.ButtonThemes.LinkButtonTheme.DEFAULT,
            enabled = true,
            name = "Visa - Default Theme"
        ),
        LinkButtonPreviewData(
            state = LinkButtonState.Default,
            theme = PaymentSheet.ButtonThemes.LinkButtonTheme.WHITE,
            enabled = true,
            name = "Default - White Theme"
        ),
        LinkButtonPreviewData(
            state = LinkButtonState.Email("user@example.com"),
            theme = PaymentSheet.ButtonThemes.LinkButtonTheme.WHITE,
            enabled = true,
            name = "Email - White Theme"
        ),
        LinkButtonPreviewData(
            state = LinkButtonState.Email("theop@email.com"),
            theme = PaymentSheet.ButtonThemes.LinkButtonTheme.WHITE,
            enabled = false,
            name = "Email Disabled - White Theme"
        ),
        LinkButtonPreviewData(
            state = LinkButtonState.DefaultPayment(
                paymentUI = DisplayablePaymentDetails(
                    defaultCardBrand = "mastercard",
                    last4 = "4242",
                    defaultPaymentType = "CARD",
                ).toDefaultPaymentUI(true)!!
            ),
            theme = PaymentSheet.ButtonThemes.LinkButtonTheme.WHITE,
            enabled = true,
            name = "Mastercard - White Theme"
        ),
        LinkButtonPreviewData(
            state = LinkButtonState.DefaultPayment(
                paymentUI = DisplayablePaymentDetails(
                    defaultCardBrand = "visa",
                    last4 = "1234",
                    defaultPaymentType = "CARD",
                ).toDefaultPaymentUI(true)!!
            ),
            theme = PaymentSheet.ButtonThemes.LinkButtonTheme.WHITE,
            enabled = true,
            name = "Visa - White Theme"
        ),
    )
}

/**
 * Data class for LinkButton preview parameters
 */
internal data class LinkButtonPreviewData(
    val state: LinkButtonState,
    val theme: PaymentSheet.ButtonThemes.LinkButtonTheme,
    val enabled: Boolean,
    val name: String
)
