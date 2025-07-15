package com.stripe.android.model

import com.stripe.android.core.model.StripeModel
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

@Parcelize
@Poko
class RadarSession(
    val id: String
) : StripeModel
