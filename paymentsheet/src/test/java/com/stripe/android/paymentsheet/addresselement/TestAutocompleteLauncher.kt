package com.stripe.android.paymentsheet.addresselement

import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.LifecycleOwner
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine

internal class TestAutocompleteLauncher private constructor() : AutocompleteActivityLauncher {
    private val launchCalls = Turbine<LaunchCall>()
    private val registerCalls = Turbine<RegisterCall>()

    override fun register(activityResultCaller: ActivityResultCaller, lifecycleOwner: LifecycleOwner) {
        registerCalls.add(
            RegisterCall(
                activityResultCaller,
                lifecycleOwner,
            )
        )
    }

    override fun launch(
        country: String,
        googlePlacesApiKey: String,
        resultHandler: AutocompleteLauncherResultHandler,
    ) {
        launchCalls.add(
            LaunchCall(
                country = country,
                googlePlacesApiKey = googlePlacesApiKey,
                resultHandler = resultHandler,
            )
        )
    }

    class LaunchCall(
        val country: String,
        val googlePlacesApiKey: String,
        val resultHandler: AutocompleteLauncherResultHandler,
    )

    class RegisterCall(
        val activityResultCaller: ActivityResultCaller,
        val lifecycleOwner: LifecycleOwner,
    )

    class Scenario(
        val launcher: AutocompleteActivityLauncher,
        val launchCalls: ReceiveTurbine<LaunchCall>,
        val registerCalls: ReceiveTurbine<RegisterCall>,
    )

    companion object {
        suspend fun test(test: suspend Scenario.() -> Unit) {
            val launcher = TestAutocompleteLauncher()

            test(
                Scenario(
                    launcher = launcher,
                    launchCalls = launcher.launchCalls,
                    registerCalls = launcher.registerCalls,
                )
            )

            launcher.launchCalls.ensureAllEventsConsumed()
            launcher.registerCalls.ensureAllEventsConsumed()
        }

        fun noOp(): AutocompleteActivityLauncher = TestAutocompleteLauncher()
    }
}
