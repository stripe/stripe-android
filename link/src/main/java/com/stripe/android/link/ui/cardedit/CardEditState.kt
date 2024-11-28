package com.stripe.android.link.ui.cardedit

import com.stripe.android.core.strings.ResolvableString

data class CardEditState(
    val isProcessing: Boolean,
    val isDefault: Boolean,
    val setAsDefault: Boolean = false,
    val errorMessage: ResolvableString? = null
) {
    val isEnabled: Boolean = isProcessing.not()
}
