package com.stripe.android.paymentsheet.wallet.wrapping

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner

internal class SavedPaymentMethodsWrappedControllerFactory(
    private val viewModelStoreOwner: ViewModelStoreOwner,
    private val lifecycleOwner: LifecycleOwner,
    private val activityResultCaller: ActivityResultCaller,
    private val statusBarColor: () -> Int?,
    private val callback: SavedPaymentMethodsControllerResultCallback
) {
    constructor(
        activity: ComponentActivity,
        callback: SavedPaymentMethodsControllerResultCallback,
    ) : this(
        viewModelStoreOwner = activity,
        lifecycleOwner = activity,
        activityResultCaller = activity,
        statusBarColor = { activity.window.statusBarColor },
        callback = callback
    )

    fun create(): SavedPaymentMethodsWrappedController =
        DefaultSavedPaymentMethodsWrappedController(
            viewModelStoreOwner = viewModelStoreOwner,
            lifecycleOwner = lifecycleOwner,
            activityResultCaller = activityResultCaller,
            statusBarColor = statusBarColor,
            callback = callback
        )
}
