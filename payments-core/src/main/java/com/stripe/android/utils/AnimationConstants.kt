@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.utils

import android.app.Activity
import android.app.Activity.OVERRIDE_TRANSITION_CLOSE
import android.os.Build
import androidx.annotation.AnimRes
import androidx.annotation.RestrictTo
import com.stripe.android.R
import com.stripe.android.utils.AnimationConstants.FADE_IN
import com.stripe.android.utils.AnimationConstants.FADE_OUT

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object AnimationConstants {
    @AnimRes
    val FADE_IN = R.anim.stripe_paymentsheet_transition_fade_in

    @AnimRes
    val FADE_OUT = R.anim.stripe_paymentsheet_transition_fade_out
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Activity.fadeOut() {
    if (Build.VERSION.SDK_INT >= 34) {
        overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, FADE_IN, FADE_OUT)
    } else {
        @Suppress("DEPRECATION")
        overridePendingTransition(FADE_IN, FADE_OUT)
    }
}
