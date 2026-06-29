package com.stripe.android.utils

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine

internal class FakeActivityLauncher<I>(
    private var throwOnNextLaunch: Throwable? = null,
) : ActivityResultLauncher<I>() {
    private val _launchCall = Turbine<Unit>()
    val launchCall: ReceiveTurbine<Unit> = _launchCall
    override val contract: ActivityResultContract<I, *>
        get() = throw NotImplementedError("Not implemented!")

    override fun launch(input: I, options: ActivityOptionsCompat?) {
        _launchCall.add(Unit)

        throwOnNextLaunch?.let { error ->
            throwOnNextLaunch = null
            throw error
        }
    }

    override fun unregister() {
        throw NotImplementedError("Not implemented!")
    }

    fun validate() {
        _launchCall.ensureAllEventsConsumed()
    }
}
