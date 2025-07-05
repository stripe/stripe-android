package com.stripe.android.iap

abstract class InAppPurchasePluginLookupType internal constructor() {
    internal abstract suspend fun lookup(plugins: List<InAppPurchasePlugin>): InAppPurchasePlugin

    class Default : InAppPurchasePluginLookupType() {
        override suspend fun lookup(plugins: List<InAppPurchasePlugin>): InAppPurchasePlugin {
            return plugins.first()
        }
    }

    class Checkout : InAppPurchasePluginLookupType() {
        override suspend fun lookup(plugins: List<InAppPurchasePlugin>): InAppPurchasePlugin {
            return plugins.first { it is CheckoutInAppPurchasePlugin }
        }
    }


    class GooglePlay : InAppPurchasePluginLookupType() {
        override suspend fun lookup(plugins: List<InAppPurchasePlugin>): InAppPurchasePlugin {
            return plugins.first { it is GooglePlayInAppPurchasePlugin }
        }
    }
}
