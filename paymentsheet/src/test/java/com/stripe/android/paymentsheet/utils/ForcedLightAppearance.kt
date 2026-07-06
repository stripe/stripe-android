package com.stripe.android.paymentsheet.utils

import com.stripe.android.paymentelement.AppearanceAPIAdditionsPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.parseAppearance
import com.stripe.android.screenshottesting.PaparazziConfigOption
import com.stripe.android.utils.screenshots.PaymentSheetAppearance.DefaultAppearance

@OptIn(AppearanceAPIAdditionsPreview::class)
internal data object ForcedLightAppearance : PaparazziConfigOption {
    private val appearance = PaymentSheet.Appearance.Builder()
        .style(PaymentSheet.UserInterfaceStyle.AlwaysLight)
        .build()

    override fun initialize() {
        appearance.parseAppearance()
    }

    override fun reset() {
        DefaultAppearance.appearance.parseAppearance()
    }
}
