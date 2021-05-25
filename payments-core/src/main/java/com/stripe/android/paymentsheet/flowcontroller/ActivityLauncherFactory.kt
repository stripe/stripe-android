package com.stripe.android.paymentsheet.flowcontroller

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.fragment.app.Fragment

internal sealed class ActivityLauncherFactory {
    /**
     * Registers a [callback] to handle the [contract] and returns an [ActivityResultLauncher].
     */
    abstract fun <I, O> create(
        contract: ActivityResultContract<I, O>,
        callback: ActivityResultCallback<O>
    ): ActivityResultLauncher<I>

    internal class ActivityHost(
        private val activity: ComponentActivity
    ) : ActivityLauncherFactory() {
        override fun <I, O> create(
            contract: ActivityResultContract<I, O>,
            callback: ActivityResultCallback<O>
        ): ActivityResultLauncher<I> {
            return activity.registerForActivityResult(
                contract,
                callback
            )
        }
    }

    internal class FragmentHost(
        private val fragment: Fragment
    ) : ActivityLauncherFactory() {
        override fun <I, O> create(
            contract: ActivityResultContract<I, O>,
            callback: ActivityResultCallback<O>
        ): ActivityResultLauncher<I> {
            return fragment.registerForActivityResult(
                contract,
                callback
            )
        }
    }
}
