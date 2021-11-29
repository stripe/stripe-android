package com.stripe.android.model

import androidx.annotation.Size
import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize
import java.util.Currency

/**
 * Model representing a shipping method in the Android SDK.
 */
@Parcelize
data class ShippingMethod @JvmOverloads constructor(
    /**
     * Human friendly label specifying the shipping method that can be shown in the UI.
     */
    val label: String,

    /**
     * Identifier for the shipping method.
     */
    val identifier: String,

    /**
     * The cost in minor unit based on [currency]
     */
    val amount: Long,

    /**
     * The currency that the specified amount will be rendered in.
     */
    val currency: Currency,

    /**
     * Human friendly information such as estimated shipping times that can be shown in
     * the UI
     */
    val detail: String? = null
) : StripeModel {

    @JvmOverloads
    constructor(
        label: String,
        identifier: String,
        amount: Long,
        @Size(min = 0, max = 3) currencyCode: String,
        detail: String? = null
    ) : this(
        label,
        identifier,
        amount,
        Currency.getInstance(currencyCode),
        detail
    )
}
