package com.stripe.android.paymentelement.confirmation.gpay

import android.content.Context
import android.os.Parcelable
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.core.strings.ResolvableString
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class GooglePayDisplayItem(
    val label: ResolvableString,
    val type: GooglePayJsonFactory.DisplayItem.Type,
    val price: Long,
) : Parcelable {
    fun resolve(context: Context): GooglePayJsonFactory.DisplayItem {
        return GooglePayJsonFactory.DisplayItem(
            label = label.resolve(context),
            type = type,
            price = price,
        )
    }
}
