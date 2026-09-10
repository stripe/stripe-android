package com.stripe.android.checkout

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class CheckoutPresenterLifecycle @Inject constructor() {
    private val owners = mutableSetOf<PresenterLifecycleOwner>()

    fun create(activity: LifecycleOwner): PresenterLifecycleOwner {
        val owner = PresenterLifecycleOwner(
            activity = activity,
            onDestroyed = owners::remove,
        )
        if (owner.lifecycle.currentState != Lifecycle.State.DESTROYED) {
            owners.add(owner)
        }
        return owner
    }

    fun destroy() {
        owners.toList().forEach { it.destroyByController() }
    }
}

internal class PresenterLifecycleOwner(
    private val activity: LifecycleOwner,
    private val onDestroyed: (PresenterLifecycleOwner) -> Unit,
) : LifecycleOwner, DefaultLifecycleObserver {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val controllerDestroyListeners = mutableListOf<() -> Unit>()

    override val lifecycle: Lifecycle = lifecycleRegistry

    init {
        activity.lifecycle.addObserver(this)
        if (activity.lifecycle.currentState == Lifecycle.State.DESTROYED) {
            destroy()
        }
    }

    fun addControllerDestroyListener(listener: () -> Unit) {
        controllerDestroyListeners += listener
    }

    fun destroyByController() {
        controllerDestroyListeners.forEach { it() }
        destroy()
    }

    override fun onCreate(owner: LifecycleOwner) = handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

    override fun onStart(owner: LifecycleOwner) = handleLifecycleEvent(Lifecycle.Event.ON_START)

    override fun onResume(owner: LifecycleOwner) = handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

    override fun onPause(owner: LifecycleOwner) = handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)

    override fun onStop(owner: LifecycleOwner) = handleLifecycleEvent(Lifecycle.Event.ON_STOP)

    override fun onDestroy(owner: LifecycleOwner) = destroy()

    private fun handleLifecycleEvent(event: Lifecycle.Event) {
        if (lifecycleRegistry.currentState != Lifecycle.State.DESTROYED) {
            lifecycleRegistry.handleLifecycleEvent(event)
        }
    }

    private fun destroy() {
        if (lifecycleRegistry.currentState == Lifecycle.State.DESTROYED) return

        activity.lifecycle.removeObserver(this)
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        controllerDestroyListeners.clear()
        onDestroyed(this)
    }
}
