package com.stripe.android.uicore.utils

import android.app.Activity
import android.os.Build
import androidx.annotation.AnimRes
import androidx.annotation.RestrictTo
import com.stripe.android.uicore.R
import com.stripe.android.uicore.utils.AnimationConstants.FADE_IN
import com.stripe.android.uicore.utils.AnimationConstants.FADE_OUT

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object AnimationConstants {
    @AnimRes
    val FADE_IN = R.anim.stripe_transition_fade_in

    @AnimRes
    val FADE_OUT = R.anim.stripe_transition_fade_out
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Activity.fadeOut(
    fadeIn: Int = FADE_IN,
    fadeOut: Int = FADE_OUT
) {
    if (Build.VERSION.SDK_INT >= 34) {
        overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, fadeIn, fadeOut)
    } else {
        @Suppress("DEPRECATION")
        overridePendingTransition(fadeIn, fadeOut)
    }
}
