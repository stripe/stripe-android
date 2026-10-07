package com.stripe.android.paymentelement

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.common.ui.PaymentElementActivityResultCaller
import com.stripe.android.core.reactnative.ReactNativeSdkInternal
import com.stripe.android.core.utils.StatusBarCompat
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentelement.embedded.content.DaggerEmbeddedPaymentElementViewModelComponent
import com.stripe.android.paymentelement.embedded.content.EmbeddedPaymentElementPresentationLifecycle
import com.stripe.android.paymentelement.embedded.content.EmbeddedPaymentElementSavedState
import com.stripe.android.paymentelement.embedded.content.EmbeddedPaymentElementViewModelComponent
import com.stripe.android.paymentsheet.utils.applicationIsTaskOwner
import kotlinx.coroutines.cancel

/**
 * Owns one React Native Embedded integration independently of its Activity presentation.
 * Retain this object across configuration changes and call [destroy] on permanent removal.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ReactNativeSdkInternal
class EmbeddedPaymentElementIntegration internal constructor(
    private val component: EmbeddedPaymentElementViewModelComponent,
    private val savedState: EmbeddedPaymentElementSavedState,
    private val integrationName: String,
) {
    private val contentHelperDelegate = lazy { component.contentHelper }
    private val contentHelper by contentHelperDelegate
    private var presentation: Presentation? = null
    private var destroyed = false

    /**
     * Binds an element and the current callbacks to [activity], replacing the previous presentation.
     * Call after Activity creation. The returned element must not be retained across Activity recreation.
     */
    @MainThread
    fun createElement(
        activity: ComponentActivity,
        builder: EmbeddedPaymentElement.Builder,
    ): EmbeddedPaymentElement {
        check(!destroyed) { "Cannot present a destroyed Embedded integration." }
        presentation?.destroy()
        val lifecycle = EmbeddedPaymentElementPresentationLifecycle(activity)
        val binding = Presentation(
            lifecycle = lifecycle,
            caller = PaymentElementActivityResultCaller(
                key = "EmbeddedPaymentElement(instance = $integrationName)",
                registryOwner = activity,
            ),
        )
        presentation = binding

        lateinit var element: EmbeddedPaymentElement
        // Restored content reads the callback behavior while the runtime graph is constructed.
        PaymentElementCallbackReferences[integrationName] = builder.createCallbacks { element }

        // A restored result can arrive synchronously while launchers are being registered.
        val pendingResults = mutableListOf<EmbeddedPaymentElement.Result>()
        var initialized = false
        val subcomponent = component.embeddedPaymentElementSubcomponentFactory.build(
            activityResultCaller = binding,
            lifecycleOwner = lifecycle,
            resultCallback = { result ->
                if (initialized) {
                    builder.resultCallback.onResult(result)
                } else {
                    pendingResults.add(result)
                }
            },
        )
        element = EmbeddedPaymentElement(
            confirmationHelper = subcomponent.confirmationHelper,
            contentHelper = contentHelper,
            selectionHolder = component.selectionHolder,
            paymentOptionDisplayDataHolder = component.paymentOptionDisplayDataHolder,
            configurationCoordinator = component.configurationCoordinator,
            stateHelper = component.stateHelper,
        )
        subcomponent.initializer.initialize(
            applicationIsTaskOwner = activity.applicationIsTaskOwner(),
            removeCallbacksOnDestroy = false,
        )
        initialized = true
        pendingResults.forEach(builder.resultCallback::onResult)
        pendingResults.clear()
        return element
    }

    /** Releases presentation and runtime resources and removes this integration's persisted state. */
    @MainThread
    fun destroy() {
        if (destroyed) return
        destroyed = true
        presentation?.destroy()
        presentation = null
        component.coroutineScope.cancel()
        if (contentHelperDelegate.isInitialized()) {
            contentHelper.embeddedContent.value?.close()
        }
        PaymentElementCallbackReferences.remove(integrationName)
        savedState.clear()
    }

    private class Presentation(
        private val lifecycle: EmbeddedPaymentElementPresentationLifecycle,
        private val caller: ActivityResultCaller,
    ) : ActivityResultCaller {
        private val launchers = mutableListOf<ActivityResultLauncher<*>>()

        init {
            lifecycle.lifecycle.addObserver(
                object : DefaultLifecycleObserver {
                    override fun onDestroy(owner: LifecycleOwner) {
                        // Some confirmation definitions leave unregistering to their caller.
                        launchers.forEach { it.unregister() }
                        launchers.clear()
                    }
                }
            )
        }

        override fun <I, O> registerForActivityResult(
            contract: ActivityResultContract<I, O>,
            callback: ActivityResultCallback<O>,
        ): ActivityResultLauncher<I> {
            return caller.registerForActivityResult(contract, callback).also(launchers::add)
        }

        override fun <I, O> registerForActivityResult(
            contract: ActivityResultContract<I, O>,
            registry: ActivityResultRegistry,
            callback: ActivityResultCallback<O>,
        ): ActivityResultLauncher<I> {
            return caller.registerForActivityResult(contract, registry, callback).also(launchers::add)
        }

        fun destroy() {
            lifecycle.destroy()
        }
    }

    companion object {
        /**
         * Creates an integration using a parent handle connected to the host's saved-state registry.
         * [integrationName] must be unique among live integrations and stable across restoration.
         * The Activity supplies application context and initial window appearance; it is not retained.
         */
        @MainThread
        fun create(
            activity: ComponentActivity,
            savedStateHandle: SavedStateHandle,
            integrationName: String,
        ): EmbeddedPaymentElementIntegration {
            val savedState = EmbeddedPaymentElementSavedState(savedStateHandle, integrationName)
            val component = DaggerEmbeddedPaymentElementViewModelComponent.factory().build(
                savedStateHandle = savedState.handle,
                application = activity.application,
                paymentElementCallbackIdentifier = integrationName,
                statusBarColor = StatusBarCompat.color(activity),
            )
            return EmbeddedPaymentElementIntegration(component, savedState, integrationName)
        }
    }
}
