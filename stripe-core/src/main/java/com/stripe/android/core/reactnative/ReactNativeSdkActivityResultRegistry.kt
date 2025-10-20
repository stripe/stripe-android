package com.stripe.android.core.reactnative

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import java.util.UUID

@ReactNativeSdkInternal
fun <I, O> registerForReactNativeActivityResult(
    activity: ComponentActivity,
    signal: UnregisterSignal,
    contract: ActivityResultContract<I, O>,
    callback: ActivityResultCallback<O>
): ActivityResultLauncher<I> {
    val activityResultLauncher = activity.activityResultRegistry.register(
        UUID.randomUUID().toString(),
        contract,
        callback,
    )

    signal.addListener {
        activityResultLauncher.unregister()
    }

    return activityResultLauncher
}
