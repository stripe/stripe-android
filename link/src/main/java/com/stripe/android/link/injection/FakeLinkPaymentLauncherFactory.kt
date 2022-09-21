package com.stripe.android.link.injection

import androidx.annotation.RestrictTo
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.ui.core.elements.IdentifierSpec

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class FakeLinkPaymentLauncherFactory(
    val launcher: LinkPaymentLauncher
) : LinkPaymentLauncherFactory {
    override fun create(
        merchantName: String,
        customerEmail: String?,
        customerPhone: String?,
        customerName: String?,
        shippingValues: Map<IdentifierSpec, String?>?
    ) = launcher
}
