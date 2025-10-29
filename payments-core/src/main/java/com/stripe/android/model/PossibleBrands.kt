package com.stripe.android.model

import com.stripe.android.core.model.StripeModel
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

@Parcelize
@Poko
class PossibleBrands(
    val brands: List<CardBrand>
) : StripeModel
