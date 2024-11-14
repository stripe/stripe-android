package com.stripe.android.connect.webview

import androidx.annotation.RestrictTo
import com.stripe.android.connect.EmbeddedComponentManager
import com.stripe.android.connect.PrivateBetaConnectSDK

@PrivateBetaConnectSDK
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface StripeConnectWebViewContainer {
    /**
     * Set the [EmbeddedComponentManager] to use for this view.
     * Must be called when this view is created via XML.
     * Cannot be called more than once per instance.
     */
    fun setEmbeddedComponentManager(embeddedComponentManager: EmbeddedComponentManager)
}
