package com.stripe.android.paymentelement.callbacks

import androidx.annotation.MainThread
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import java.util.UUID

internal class LifecyclePaymentElementCallbackReferences private constructor(
    private val lifecycle: Lifecycle,
    private val id: UUID,
) {
    @MainThread
    constructor(lifecycle: Lifecycle) : this(lifecycle, UUID.randomUUID())

    private val referenceKeys = mutableSetOf<CallbacksKey>()
    private var disposed = false
    private val observer = object : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) {
            dispose()
        }
    }

    init {
        lifecycle.addObserver(observer)
    }

    @MainThread
    operator fun set(key: String, callbacks: PaymentElementCallbacks) {
        if (!disposed && lifecycle.currentState != Lifecycle.State.DESTROYED) {
            val resolvedKey = key(key)
            referenceKeys.add(resolvedKey)
            PaymentElementCallbackReferences.set(key = resolvedKey, callbacks = callbacks)
        }
    }

    fun key(key: String): CallbacksKey = LifecycleCallbacksKey(key, id)

    private fun dispose() {
        disposed = true
        referenceKeys.forEach(PaymentElementCallbackReferences::remove)
        referenceKeys.clear()
        lifecycle.removeObserver(observer)
    }

    companion object {
        /** Keeps callback keys stable across recreation while binding cleanup to the current lifecycle. */
        @MainThread
        fun get(
            lifecycle: Lifecycle,
            owner: ViewModelStoreOwner,
            key: String,
        ): LifecyclePaymentElementCallbackReferences {
            return ViewModelProvider(owner, ScopeViewModel.Factory)
                .get("PaymentElementCallbacks($key)", ScopeViewModel::class.java)
                .references(lifecycle)
        }
    }

    private class ScopeViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {
        private var references: LifecyclePaymentElementCallbackReferences? = null

        fun references(lifecycle: Lifecycle): LifecyclePaymentElementCallbackReferences {
            val current = references
            if (current != null && current.lifecycle === lifecycle) {
                return current
            }
            check(lifecycle.currentState != Lifecycle.State.DESTROYED)
            current?.dispose()
            val id = savedStateHandle.get<UUID>(SCOPE_ID) ?: UUID.randomUUID().also {
                savedStateHandle[SCOPE_ID] = it
            }
            return LifecyclePaymentElementCallbackReferences(lifecycle, id).also {
                references = it
            }
        }

        override fun onCleared() {
            references?.dispose()
        }

        object Factory : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return ScopeViewModel(extras.createSavedStateHandle()) as T
            }
        }

        private companion object {
            const val SCOPE_ID = "scope_id"
        }
    }
}
