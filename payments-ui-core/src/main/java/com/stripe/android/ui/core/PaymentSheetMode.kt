package com.stripe.android.ui.core

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed interface PaymentSheetMode {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    object Payment : PaymentSheetMode

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    object Setup : PaymentSheetMode
}
