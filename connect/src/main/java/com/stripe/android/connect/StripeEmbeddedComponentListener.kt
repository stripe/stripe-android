package com.stripe.android.connect

import com.stripe.android.connect.webview.serialization.SetterFunctionCalledMessage

@PrivateBetaConnectSDK
interface StripeEmbeddedComponentListener {
    /**
     * The component executes this callback function before any UI is displayed to the user.
     */
    fun onLoaderStart() {}

    /**
     * The component executes this callback function when a load failure occurs.
     */
    fun onLoadError(error: Throwable) {}
}

@OptIn(PrivateBetaConnectSDK::class)
internal fun interface ComponentListenerDelegate<Listener : StripeEmbeddedComponentListener> {
    fun Listener.delegate(message: SetterFunctionCalledMessage)

    companion object {
        internal fun <Listener : StripeEmbeddedComponentListener> ignore(): ComponentListenerDelegate<Listener> {
            return ComponentListenerDelegate { _ -> }
        }
    }
}
