package com.stripe.android.model

import kotlinx.parcelize.Parcelize

@Parcelize
data class RadarSession(
    val id: String
) : StripeModel
