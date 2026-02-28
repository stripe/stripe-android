package com.stripe.android.core

import android.content.Context
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface IsExampleApp {
    operator fun invoke(): Boolean
}

class DefaultIsExampleApp(
    private val context: Context,
) : IsExampleApp {

    override fun invoke(): Boolean {
        return context.packageName == PAYMENTSHEET_EXAMPLE_PACKAGE
    }

    private companion object {
        const val PAYMENTSHEET_EXAMPLE_PACKAGE = "com.stripe.android.paymentsheet.example"
    }
}
