package com.stripe.android.utils

import androidx.annotation.AnimRes
import androidx.annotation.RestrictTo
import com.stripe.android.R

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object AnimationConstants {
    @AnimRes
    val FADE_IN = R.anim.stripe_paymentsheet_transition_fade_in

    @AnimRes
    val FADE_OUT = R.anim.stripe_paymentsheet_transition_fade_out
}
