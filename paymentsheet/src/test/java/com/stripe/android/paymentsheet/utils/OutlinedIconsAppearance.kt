package com.stripe.android.paymentsheet.utils

import com.stripe.android.elements.Appearance
import com.stripe.android.paymentelement.AppearanceAPIAdditionsPreview
import com.stripe.android.paymentsheet.parseAppearance
import com.stripe.android.screenshottesting.PaparazziConfigOption
import com.stripe.android.utils.screenshots.PaymentSheetAppearance.DefaultAppearance

@OptIn(AppearanceAPIAdditionsPreview::class)
internal data object OutlinedIconsAppearance : PaparazziConfigOption {
    private val appearance = Appearance.Builder()
        .iconStyle(Appearance.IconStyle.Outlined)
        .build()

    override fun initialize() {
        appearance.parseAppearance()
    }

    override fun reset() {
        DefaultAppearance.appearance.parseAppearance()
    }
}
