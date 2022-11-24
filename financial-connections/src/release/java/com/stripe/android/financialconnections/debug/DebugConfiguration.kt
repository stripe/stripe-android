package com.stripe.android.financialconnections.debug

import android.app.Application
import javax.inject.Inject

internal class DebugConfiguration @Inject constructor(
    @Suppress("unused") context: Application
) {

    internal val overridenNative: Boolean?
        get() = null
}
