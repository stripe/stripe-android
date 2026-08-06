package com.stripe.android.paymentsheet.utils

import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentConfiguration
import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.customersheet.CustomerSheetActivity
import com.stripe.android.customersheet.CustomerSheetResultCallback
import com.stripe.android.link.account.DefaultLinkStore
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.paymentsheet.MainActivity
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

internal class CustomerSheetTestRunnerContext(
    internal val scenario: ActivityScenario<MainActivity>,
    private val customerSheet: CustomerSheet,
    private val countDownLatch: CountDownLatch,
) {
    fun presentCustomerSheet() {
        val activityLaunchObserver = ActivityLaunchObserver(CustomerSheetActivity::class.java)
        scenario.onActivity {
            activityLaunchObserver.prepareForLaunch(it)
            customerSheet.present()
        }
        activityLaunchObserver.awaitLaunch()
    }

    fun markTestSucceeded() {
        countDownLatch.countDown()
    }
}

/**
 * Runs CustomerSheet against an activity that is already owned by an Android Compose test rule.
 *
 * Keeping the SDK host and Compose rule in the same activity is required for tests that observe
 * window state, such as IME insets.
 */
internal fun runCustomerSheetTest(
    scenario: ActivityScenario<MainActivity>,
    networkRule: NetworkRule,
    integrationType: IntegrationType,
    customerSheetTestType: CustomerSheetTestType,
    configuration: CustomerSheet.Configuration,
    resultCallback: CustomerSheetResultCallback,
    block: (CustomerSheetTestRunnerContext) -> Unit,
) {
    val countDownLatch = CountDownLatch(1)

    val factory = CustomerSheetTestFactory(
        integrationType = integrationType,
        customerSheetTestType = customerSheetTestType,
        configuration = configuration,
        resultCallback = {
            resultCallback.onCustomerSheetResult(it)
            countDownLatch.countDown()
        },
    )

    runCustomerSheetTest(
        scenario = scenario,
        networkRule = networkRule,
        countDownLatch = countDownLatch,
        makeCustomerSheet = factory::make,
        block = block,
    )
}

internal fun runCustomerSheetTest(
    networkRule: NetworkRule,
    integrationType: IntegrationType,
    customerSheetTestType: CustomerSheetTestType,
    configuration: CustomerSheet.Configuration = CustomerSheet.Configuration(
        merchantDisplayName = "Merchant Inc."
    ),
    resultCallback: CustomerSheetResultCallback,
    block: (CustomerSheetTestRunnerContext) -> Unit,
) {
    val countDownLatch = CountDownLatch(1)

    val factory = CustomerSheetTestFactory(
        integrationType = integrationType,
        customerSheetTestType = customerSheetTestType,
        configuration = configuration,
        resultCallback = {
            resultCallback.onCustomerSheetResult(it)
            countDownLatch.countDown()
        },
    )

    runCustomerSheetTest(
        networkRule = networkRule,
        countDownLatch = countDownLatch,
        makeCustomerSheet = factory::make,
        block = block,
    )
}

private fun runCustomerSheetTest(
    networkRule: NetworkRule,
    countDownLatch: CountDownLatch,
    makeCustomerSheet: (ComponentActivity) -> CustomerSheet,
    block: (CustomerSheetTestRunnerContext) -> Unit,
) {
    ActivityScenario.launch(MainActivity::class.java).use { scenario ->
        runCustomerSheetTest(
            scenario = scenario,
            networkRule = networkRule,
            countDownLatch = countDownLatch,
            makeCustomerSheet = makeCustomerSheet,
            block = block,
        )
    }
}

private fun runCustomerSheetTest(
    scenario: ActivityScenario<MainActivity>,
    networkRule: NetworkRule,
    countDownLatch: CountDownLatch,
    makeCustomerSheet: (ComponentActivity) -> CustomerSheet,
    block: (CustomerSheetTestRunnerContext) -> Unit,
) {
    scenario.moveToState(Lifecycle.State.CREATED)

    scenario.onActivity {
        PaymentConfiguration.init(it, "pk_test_123")
        DefaultLinkStore(it.applicationContext).clear()
    }

    var customerSheet: CustomerSheet? = null

    scenario.onActivity { activity ->
        customerSheet = makeCustomerSheet(activity)
    }

    scenario.moveToState(Lifecycle.State.RESUMED)

    val testContext = CustomerSheetTestRunnerContext(
        scenario = scenario,
        customerSheet = customerSheet ?: throw IllegalStateException(
            "CustomerSheet should have been created!"
        ),
        countDownLatch = countDownLatch,
    )
    block(testContext)

    val didCompleteSuccessfully = countDownLatch.await(5, TimeUnit.SECONDS)
    networkRule.validate()
    assertThat(didCompleteSuccessfully).isTrue()
}
