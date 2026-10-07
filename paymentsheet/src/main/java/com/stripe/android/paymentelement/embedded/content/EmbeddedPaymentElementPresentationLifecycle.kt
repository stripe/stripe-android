package com.stripe.android.paymentelement.embedded.content

import androidx.annotation.MainThread
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

@MainThread
internal class EmbeddedPaymentElementPresentationLifecycle(
    parent: LifecycleOwner,
) : LifecycleOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle = lifecycleRegistry

    private val parentLifecycle = parent.lifecycle
    private val parentObserver = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_DESTROY) {
            destroy()
        } else {
            lifecycleRegistry.handleLifecycleEvent(event)
        }
    }

    init {
        require(parentLifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
            "Embedded presentation requires a host that has completed onCreate."
        }
        parentLifecycle.addObserver(parentObserver)
    }

    fun destroy() {
        if (lifecycleRegistry.currentState != Lifecycle.State.DESTROYED) {
            parentLifecycle.removeObserver(parentObserver)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }
    }
}
