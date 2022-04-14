package com.stripe.android.link.injection

import androidx.annotation.RestrictTo
import com.stripe.android.link.LinkPaymentLauncher
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory

@AssistedFactory
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface LinkPaymentLauncherFactory {
    fun create(
        @Assisted(MERCHANT_NAME) merchantName: String,
        @Assisted(CUSTOMER_EMAIL) customerEmail: String?
    ): LinkPaymentLauncher
}
