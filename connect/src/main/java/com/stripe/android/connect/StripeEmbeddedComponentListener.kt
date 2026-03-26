package com.stripe.android.connect

import com.stripe.android.connect.webview.serialization.SetOnLoadError
import com.stripe.android.connect.webview.serialization.SetOnLoaderStart
import com.stripe.android.connect.webview.serialization.SetterFunctionCalledMessage

interface StripeEmbeddedComponentListener {
    /**
     * The component executes this callback function before any UI is displayed to the user.
     */
    fun onLoaderStart() {}

    /**
     * The component executes this callback function when a load failure occurs.
     *
     * @param error The error that occurred. This will be an [EmbeddedComponentError] which
     * provides access to the error [EmbeddedComponentError.type] and [EmbeddedComponentError.message].
     */
    fun onLoadError(error: Throwable) {}
}

/**
 * Handles generic component events and delegates others to a component event listener.
 */
internal open class ComponentListenerDelegate<Listener : StripeEmbeddedComponentListener> {
    open fun delegate(listener: Listener, message: SetterFunctionCalledMessage) {
        // Override me.
    }

    fun delegate(listener: Listener, event: ComponentEvent) {
        when (event) {
            is ComponentEvent.LoadError -> {
                listener.onLoadError(event.error)
            }
            is ComponentEvent.Message -> {
                when (val value = event.message.value) {
                    is SetOnLoaderStart -> listener.onLoaderStart()
                    is SetOnLoadError -> {
                        listener.onLoadError(
                            EmbeddedComponentError(
                                type = value.error.type,
                                message = value.error.message
                            )
                        )
                    }
                    else -> {
                        delegate(listener, event.message)
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
