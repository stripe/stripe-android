package com.stripe.android.link.ui.updatecard

import androidx.compose.runtime.Immutable
import com.stripe.android.paymentsheet.CardUpdateParams

@Immutable
internal data class UpdateCardScreenState(
    val paymentDetailsId: String? = null,
    val isDefault: Boolean = false,
    val cardUpdateParams: CardUpdateParams? = null,
)
