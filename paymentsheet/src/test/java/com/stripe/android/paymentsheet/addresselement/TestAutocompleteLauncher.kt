package com.stripe.android.paymentsheet.addresselement

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine

internal class TestAutocompleteLauncher private constructor() : AutocompleteLauncher {
    private val launchCalls = Turbine<LaunchCall>()

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

    class Scenario(
        val launcher: AutocompleteLauncher,
        val launchCalls: ReceiveTurbine<LaunchCall>
    )

    companion object {
        suspend fun test(test: suspend Scenario.() -> Unit) {
            val launcher = TestAutocompleteLauncher()

            test(
                Scenario(
                    launcher = launcher,
                    launchCalls = launcher.launchCalls
                )
            )

            launcher.launchCalls.ensureAllEventsConsumed()
        }

        fun noOp(): AutocompleteLauncher = TestAutocompleteLauncher()
    }
}
