package com.stripe.android.paymentsheet.ui

import com.stripe.android.core.strings.ResolvableString

internal data class EditPaymentMethodViewState(
    val status: Status,
    val last4: String,
    val displayName: ResolvableString,
    val canUpdate: Boolean,
    val selectedBrand: CardBrandChoice,
    val availableBrands: List<CardBrandChoice>,
    val canRemove: Boolean,
    val confirmRemoval: Boolean = false,
    val error: ResolvableString? = null,
) {
    enum class Status {
        Idle,
        Updating,
        Removing
    }
}
