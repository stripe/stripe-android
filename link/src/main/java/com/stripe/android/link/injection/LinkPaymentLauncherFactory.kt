package com.stripe.android.link.injection

import androidx.annotation.RestrictTo
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.ui.core.elements.IdentifierSpec
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory

@AssistedFactory
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface LinkPaymentLauncherFactory {
    fun create(
        @Assisted(MERCHANT_NAME) merchantName: String,
        @Assisted(CUSTOMER_EMAIL) customerEmail: String?,
        @Assisted(CUSTOMER_PHONE) customerPhone: String?,
        @Assisted(CUSTOMER_NAME) customerName: String?,
        @Assisted(SHIPPING_VALUES) shippingValues: Map<IdentifierSpec, String?>?
    ): LinkPaymentLauncher
}
