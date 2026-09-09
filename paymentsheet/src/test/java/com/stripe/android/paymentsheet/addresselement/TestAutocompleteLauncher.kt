package com.stripe.android.paymentsheet.addresselement

import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.LifecycleOwner
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.core.ApiConfiguration

internal val TEST_API_CONFIGURATION = ApiConfiguration.State(
    publishableKey = "pk_test_123",
    stripeAccountId = "acct_123",
)

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
        apiConfiguration: ApiConfiguration.State,
        resultHandler: AutocompleteLauncherResultHandler,
    ) {
        launchCalls.add(
            LaunchCall(
                country = country,
                googlePlacesApiKey = googlePlacesApiKey,
                apiConfiguration = apiConfiguration,
                resultHandler = resultHandler,
            )
        )
    }

    class LaunchCall(
        val country: String,
        val googlePlacesApiKey: String,
        val apiConfiguration: ApiConfiguration.State,
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
