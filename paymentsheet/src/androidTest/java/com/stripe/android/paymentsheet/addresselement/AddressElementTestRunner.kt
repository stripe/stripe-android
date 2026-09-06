package com.stripe.android.paymentsheet.addresselement

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import app.cash.turbine.Turbine
import app.cash.turbine.withTurbineTimeout
import com.stripe.android.paymentsheet.MainActivity
import com.stripe.android.paymentsheet.utils.ActivityLaunchObserver
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.seconds

internal class AddressElementTestRunnerContext(
    private val scenario: ActivityScenario<MainActivity>,
    private val addressLauncher: AddressLauncher,
    val page: AddressElementPage,
    val results: Turbine<AddressLauncherResult>,
) {
    fun present() {
        val activityLaunchObserver = ActivityLaunchObserver(AddressElementActivity::class.java)
        scenario.onActivity {
            activityLaunchObserver.prepareForLaunch(it)
            addressLauncher.present(
                publishableKey = "pk_test_123",
                configuration = AddressLauncher.Configuration(
                    allowedCountries = setOf("US"),
                    autocompleteCountries = emptySet(),
                ),
            )
        }
        activityLaunchObserver.awaitLaunch()
        page.waitUntilVisible()
    }
}

internal fun runAddressElementTest(
    page: AddressElementPage,
    block: suspend AddressElementTestRunnerContext.() -> Unit,
) {
    val results = Turbine<AddressLauncherResult>()

    ActivityScenario.launch(MainActivity::class.java).use { scenario ->
        scenario.moveToState(Lifecycle.State.CREATED)

        lateinit var addressLauncher: AddressLauncher
        scenario.onActivity { activity ->
            addressLauncher = AddressLauncher(activity) { result ->
                results.add(result)
            }
        }

        scenario.moveToState(Lifecycle.State.RESUMED)

        runBlocking {
            withTurbineTimeout(5.seconds) {
                AddressElementTestRunnerContext(
                    scenario = scenario,
                    addressLauncher = addressLauncher,
                    page = page,
                    results = results,
                ).block()
                results.ensureAllEventsConsumed()
            }
        }
    }
}
