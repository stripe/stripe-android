package com.stripe.android.connect.webview

import androidx.annotation.RestrictTo
import com.stripe.android.connect.EmbeddedComponentManager
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.connect.StripeEmbeddedComponentListener

@PrivateBetaConnectSDK
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface StripeConnectWebViewContainer<Listener, Props>
    where Listener : StripeEmbeddedComponentListener {

    /**
     * Listener of component events.
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    var listener: Listener?

    /**
     * Initializes the view. Must be called exactly once if and only if this view was created
     * through XML layout inflation.
     */
    fun initialize(
        embeddedComponentManager: EmbeddedComponentManager,
        listener: Listener?,
        props: Props,
    )
}
