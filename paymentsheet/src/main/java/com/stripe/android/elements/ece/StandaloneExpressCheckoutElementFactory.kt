package com.stripe.android.elements.ece

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCaller
import androidx.core.os.bundleOf
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.stripe.android.common.ui.PaymentElementActivityResultCaller
import com.stripe.android.core.utils.StatusBarCompat
import com.stripe.android.elements.StandaloneExpressCheckoutElement
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbacks
import com.stripe.android.paymentsheet.PaymentSheetResultCallback
import kotlinx.coroutines.launch
import java.util.UUID

internal fun createStandaloneExpressCheckoutElement(
    activity: ComponentActivity,
    builder: StandaloneExpressCheckoutElement.Builder,
    key: String,
): StandaloneExpressCheckoutElement {
    val identifier = standaloneExpressCheckoutElementCallbackIdentifier(activity, key)
    PaymentElementCallbackReferences[identifier] = builder.callbacks()
    return createStandaloneExpressCheckoutElement(
        activity = activity,
        activityResultCaller = PaymentElementActivityResultCaller(
            key = "ExpressCheckoutElement(instance = $identifier)",
            registryOwner = activity,
        ),
        lifecycleOwner = activity,
        viewModelStoreOwner = activity,
        callbackIdentifier = identifier,
        initializedViaCompose = false,
        resultCallback = builder.resultCallback,
    )
}

internal fun standaloneExpressCheckoutElementCallbackIdentifier(activity: ComponentActivity, key: String): String {
    val stateKey = "ExpressCheckoutElement:$key"
    val identifier = activity.savedStateRegistry.consumeRestoredStateForKey(stateKey)
        ?.getString("callback_identifier") ?: UUID.randomUUID().toString()
    activity.savedStateRegistry.registerSavedStateProvider(stateKey) {
        bundleOf("callback_identifier" to identifier)
    }
    return identifier
}

internal fun createStandaloneExpressCheckoutElement(
    activity: Activity,
    activityResultCaller: ActivityResultCaller,
    lifecycleOwner: LifecycleOwner,
    viewModelStoreOwner: ViewModelStoreOwner,
    callbackIdentifier: String,
    initializedViaCompose: Boolean,
    resultCallback: PaymentSheetResultCallback,
): StandaloneExpressCheckoutElement {
    val viewModel = ViewModelProvider(
        viewModelStoreOwner,
        StandaloneExpressCheckoutElementViewModel.Factory(callbackIdentifier, StatusBarCompat.color(activity)),
    ).get(
        "ExpressCheckoutElement(instance = $callbackIdentifier)",
        StandaloneExpressCheckoutElementViewModel::class.java,
    )
    val controller = viewModel.controller
    controller.initializedViaCompose = initializedViaCompose
    controller.confirmationHandler.register(activityResultCaller, lifecycleOwner)
    lifecycleOwner.lifecycleScope.launch {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            controller.result.collect(resultCallback::onPaymentSheetResult)
        }
    }
    lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) {
            PaymentElementCallbackReferences.remove(callbackIdentifier)
        }
    })
    return StandaloneExpressCheckoutElement(controller = controller, interactor = viewModel.interactor)
}

internal fun StandaloneExpressCheckoutElement.Builder.callbacks(): PaymentElementCallbacks =
    PaymentElementCallbacks.Builder()
        .createIntentCallback(createIntentCallback)
        .googlePayPaymentDataUpdateCallback(googlePayPaymentDataUpdateCallback)
        .build()
