package com.stripe.android.view

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface AuthActivityStarter<ArgsType> {
    fun start(args: ArgsType)
}
