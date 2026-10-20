package com.stripe.android.checkout

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class CheckoutPresenterLifecycle @Inject constructor() {
    private val owners = mutableSetOf<PresenterLifecycleOwner>()

    fun create(activity: LifecycleOwner): LifecycleOwner {
        check(activity.lifecycle.currentState != Lifecycle.State.DESTROYED) {
            "Cannot create a checkout presenter for a destroyed activity."
        }
        val owner = PresenterLifecycleOwner(
            activity = activity,
            onDestroyed = owners::remove,
        )
        owners.add(owner)
        return owner
    }

    fun destroy() {
        owners.toList().forEach { it.destroy() }
    }

    private class PresenterLifecycleOwner(
        private val activity: LifecycleOwner,
        private val onDestroyed: (PresenterLifecycleOwner) -> Unit,
    ) : LifecycleOwner, LifecycleEventObserver {
        private val lifecycleRegistry = LifecycleRegistry(this)

        override val lifecycle: Lifecycle = lifecycleRegistry

        init {
            activity.lifecycle.addObserver(this)
        }

        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            if (event == Lifecycle.Event.ON_DESTROY) {
                destroy()
            } else if (lifecycleRegistry.currentState != Lifecycle.State.DESTROYED) {
                lifecycleRegistry.handleLifecycleEvent(event)
            }
        }

        fun destroy() {
            if (lifecycleRegistry.currentState == Lifecycle.State.DESTROYED) return

            activity.lifecycle.removeObserver(this)
            // LifecycleRegistry requires CREATED before it can transition to DESTROYED.
            if (lifecycleRegistry.currentState == Lifecycle.State.INITIALIZED) {
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            }
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
            onDestroyed(this)
        }
    }
}
