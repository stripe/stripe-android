package com.stripe.android.common.analytics
import com.stripe.android.elements.Appearance.Embedded.RowStyle
import com.stripe.android.elements.CardBrandAcceptance

internal fun CardBrandAcceptance.toAnalyticsValu(): Boolean {
    return this !is CardBrandAcceptance.All
}

@Suppress("LongMethod")
fun getRow(a: Appearance.Embedded.RowStyle): Int {
    return when (a) {
        is Appearance.Embedded.RowStyle.FlatWithRadio -> 1
        is Appearance.Embedded.RowStyle.FlatWithCheckmark -> 2
        is Appearance.Embedded.RowStyle.FlatWithChevron -> 4
        is Appearance.Embedded.RowStyle.FloatingButton -> 8
    }
}