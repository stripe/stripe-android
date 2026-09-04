package com.stripe.android.paymentsheet.addresselement

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import app.cash.turbine.Turbine
import app.cash.turbine.withTurbineTimeout
import com.stripe.android.paymentsheet.MainActivity
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.utils.ActivityLaunchObserver
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.seconds

internal class AddressElementTestRunnerContext(
    private val scenario: ActivityScenario<MainActivity>,
    private val addressLauncher: AddressLauncher,
    val page: AddressElementPage,
    val results: Turbine<AddressLauncherResult>,
) {
    fun present(): AddressElementActivity {
        val activityLaunchObserver = ActivityLaunchObserver(AddressElementActivity::class.java)
        scenario.onActivity {
            activityLaunchObserver.prepareForLaunch(it)
            addressLauncher.present(
                publishableKey = "pk_test_123",
                configuration = AddressLauncher.Configuration(
                    address = AddressDetails(
                        address = PaymentSheet.Address(country = "US"),
                    ),
                    allowedCountries = setOf("US"),
                    autocompleteCountries = setOf("CA"),
                ),
            )
        }
        activityLaunchObserver.awaitLaunch()
        page.waitUntilVisible()
        return resumedAddressElementActivity()
    }

    fun recreate(activity: AddressElementActivity): AddressElementActivity {
        val activityLaunchObserver = ActivityLaunchObserver(AddressElementActivity::class.java)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            activityLaunchObserver.prepareForLaunch(activity)
            activity.recreate()
        }
        activityLaunchObserver.awaitLaunch()
        return resumedAddressElementActivity()
    }

    private fun resumedAddressElementActivity(): AddressElementActivity {
        lateinit var addressElementActivity: AddressElementActivity
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            addressElementActivity = ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED)
                .filterIsInstance<AddressElementActivity>()
                .single()
        }
        return addressElementActivity
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
