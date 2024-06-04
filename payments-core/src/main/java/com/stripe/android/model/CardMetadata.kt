package com.stripe.android.model

import androidx.annotation.RestrictTo
import com.stripe.android.cards.Bin
import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize

@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class CardMetadata internal constructor(
    val bin: Bin,
    val accountRanges: List<AccountRange>
) : StripeModel
