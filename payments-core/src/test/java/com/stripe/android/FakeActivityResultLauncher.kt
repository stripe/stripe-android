package com.stripe.android

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat

internal class FakeActivityResultLauncher<I>(
    override val contract: ActivityResultContract<I, *>
) : ActivityResultLauncher<I>() {
    val launchArgs = mutableListOf<I>()
    var unregisterInvocations = 0

    override fun launch(
        input: I,
        options: ActivityOptionsCompat?
    ) {
        launchArgs.add(input)
    }

    override fun unregister() {
        unregisterInvocations++
    }
}
