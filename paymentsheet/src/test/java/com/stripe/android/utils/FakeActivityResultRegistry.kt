package com.stripe.android.utils

import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat

internal class FakeActivityResultRegistry<T>(
    private val result: T
) : ActivityResultRegistry() {

    override fun <I : Any?, O : Any?> onLaunch(
        requestCode: Int,
        contract: ActivityResultContract<I, O>,
        input: I,
        options: ActivityOptionsCompat?
    ) {
        dispatchResult(
            requestCode,
            result
        )
    }
}
