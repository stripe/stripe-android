package com.stripe.android.paymentelement.confirmation.gpay

import android.content.Context
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.core.strings.ResolvableString

internal data class GooglePayDisplayItem(
    val label: ResolvableString,
    val type: GooglePayJsonFactory.DisplayItem.Type,
    val price: Long,
) {
    fun resolve(context: Context): GooglePayJsonFactory.DisplayItem {
        return GooglePayJsonFactory.DisplayItem(
            label = label.resolve(context),
            type = type,
            price = price,
        )
    }
}
