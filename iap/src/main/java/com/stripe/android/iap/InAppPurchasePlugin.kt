package com.stripe.android.iap

import android.app.Activity
import androidx.activity.result.ActivityResultCaller
import androidx.annotation.RestrictTo

abstract class InAppPurchasePlugin internal constructor() {
    internal abstract fun register(
        activity: Activity,
        activityResultCaller: ActivityResultCaller,
        resultCallback: InAppPurchase.ResultCallback,
    )

    internal abstract fun unregister()

    internal abstract val analyticsName: String

    internal abstract suspend fun purchase(priceId: String)

    companion object {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun stripeCheckout(
            checkoutUrlProvider: suspend (priceId: String) -> String
        ): InAppPurchasePlugin {
            return CheckoutInAppPurchasePlugin(checkoutUrlProvider)
        }

        fun googlePlay(
            customerSessionClientSecretProvider: suspend (priceId: String) -> String
        ): InAppPurchasePlugin {
            return GooglePlayInAppPurchasePlugin(customerSessionClientSecretProvider)
        }
    }
}
