package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface IsPlacesAvailable {
    operator fun invoke(): Boolean
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DefaultIsPlacesAvailable : IsPlacesAvailable {
    override fun invoke(): Boolean = true
}
