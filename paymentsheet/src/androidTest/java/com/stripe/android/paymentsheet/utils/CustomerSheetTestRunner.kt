package com.stripe.android.paymentsheet.utils

import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentConfiguration
import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.customersheet.CustomerSheetActivity
import com.stripe.android.customersheet.CustomerSheetResultCallback
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi
import com.stripe.android.link.account.LinkStore
import com.stripe.android.paymentsheet.MainActivity
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCustomerSheetApi::class)
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

@OptIn(ExperimentalCustomerSheetApi::class)
internal fun ActivityScenarioRule<MainActivity>.runCustomerSheetTest(
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
        countDownLatch = countDownLatch,
        makeCustomerSheet = factory::make,
        block = block,
    )
}

@OptIn(ExperimentalCustomerSheetApi::class)
private fun ActivityScenarioRule<MainActivity>.runCustomerSheetTest(
    countDownLatch: CountDownLatch,
    makeCustomerSheet: (ComponentActivity) -> CustomerSheet,
    block: (CustomerSheetTestRunnerContext) -> Unit,
) {
    scenario.moveToState(Lifecycle.State.CREATED)

    scenario.onActivity {
        PaymentConfiguration.init(it, "pk_test_123")
        LinkStore(it.applicationContext).clear()
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

    assertThat(countDownLatch.await(5, TimeUnit.SECONDS)).isTrue()
}
