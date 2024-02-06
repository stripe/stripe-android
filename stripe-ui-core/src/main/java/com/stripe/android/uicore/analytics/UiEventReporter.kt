package com.stripe.android.uicore.analytics

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface UiEventReporter {
    fun onAutofillEvent(type: String)
    fun onFieldInteracted()
}
