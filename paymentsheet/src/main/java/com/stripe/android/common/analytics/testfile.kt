package com.stripe.android.common.analytics
import com.stripe.android.paymentsheet.PaymentSheet

internal fun PaymentSheet.CardBrandAcceptance.toAnalyticsValu(): Boolean {
    return this !is PaymentSheet.CardBrandAcceptance.All
}

@Suppress("LongMethod")
fun getRow(a: PaymentSheet.Appearance.Embedded.RowStyle): Int {
    return when (a) {
        is PaymentSheet.Appearance.Embedded.RowStyle.FlatWithRadio -> 1
        is PaymentSheet.Appearance.Embedded.RowStyle.FlatWithCheckmark -> 2
        is PaymentSheet.Appearance.Embedded.RowStyle.FlatWithChevron -> 4
        is PaymentSheet.Appearance.Embedded.RowStyle.FloatingButton -> 8
    }
}