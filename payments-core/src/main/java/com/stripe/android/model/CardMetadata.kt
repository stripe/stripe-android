package com.stripe.android.model

import com.stripe.android.cards.Bin
import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class CardMetadata internal constructor(
    val bin: Bin,
    val accountRanges: List<AccountRange>
) : StripeModel
