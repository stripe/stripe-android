package com.stripe.android.cardverificationsheet.payment

import androidx.annotation.Keep

@Keep
internal data class FrameDetails(
    val panSideConfidence: Float,
    val noPanSideConfidence: Float,
    val noCardConfidence: Float,
    val hasPan: Boolean,
)
