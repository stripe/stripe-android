package com.stripe.android.connect

import com.stripe.android.connect.webview.serialization.SetOnLoadError
import com.stripe.android.connect.webview.serialization.SetOnLoaderStart
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
internal open class ComponentListenerDelegate<Listener : StripeEmbeddedComponentListener> {
    open fun Listener.delegateMessage(message: SetterFunctionCalledMessage) {
        // Implement me.
    }

    fun Listener.delegate(event: ComponentEvent) {
        when (event) {
            is ComponentEvent.LoadError -> {
                onLoadError(event.error)
            }
            is ComponentEvent.Message -> {
                when (val value = event.message.value) {
                    is SetOnLoaderStart -> onLoaderStart()
                    is SetOnLoadError -> {
                        // TODO - wrap error better
                        onLoadError(RuntimeException("${value.error.type}: ${value.error.message}"))
                    }
                    else -> {
                        delegateMessage(event.message)
                    }
                }
            }
        }
    }
}

internal sealed interface ComponentEvent {
    data class LoadError(val error: Throwable) : ComponentEvent
    data class Message(val message: SetterFunctionCalledMessage) : ComponentEvent
}
