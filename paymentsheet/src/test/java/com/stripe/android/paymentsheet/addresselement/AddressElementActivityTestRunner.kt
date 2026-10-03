package com.stripe.android.paymentsheet.addresselement

import android.app.Application
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkouttesting.checkoutUpdate
import com.stripe.android.common.ui.PRIMARY_BUTTON_LOADING_INDICATOR_TEST_TAG
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.testing.waitUntilWithIdle
import com.stripe.paymentelementtestpages.AddressElementPage
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

internal class AddressElementActivityTestRunner(
    private val composeTestRule: ComposeTestRule,
    private val networkRule: NetworkRule,
    private val addressPage: AddressElementPage,
) {
    fun run(
        args: AddressElementActivityContract.Args,
        block: Scenario.() -> Unit,
    ) {
        val applicationContext = ApplicationProvider.getApplicationContext<Application>()
        val intent = when (args) {
            is AddressElementActivityContract.Args.Standalone ->
                AddressElementActivityContract.Standalone.createIntent(applicationContext, args)
            is AddressElementActivityContract.Args.CheckoutShipping ->
                AddressElementActivityContract.CheckoutShipping.createIntent(applicationContext, args)
        }
        ActivityScenario.launchActivityForResult<AddressElementActivity>(intent).use { activityScenario ->
            lateinit var activity: AddressElementActivity
            activityScenario.onActivity { activity = it }
            Scenario(
                activityScenario = activityScenario,
                activity = activity,
                args = args,
            ).block()
        }
    }

    internal inner class Scenario(
        val activityScenario: ActivityScenario<AddressElementActivity>,
        var activity: AddressElementActivity,
        val args: AddressElementActivityContract.Args,
    ) {
        val checkoutSessionResponse: CheckoutSessionResponse
            get() = (args as AddressElementActivityContract.Args.CheckoutShipping).checkoutSessionResponse

        fun enqueueTaxUpdate(fails: Boolean = false): TaxUpdate {
            val requestReceived = CountDownLatch(1)
            val releaseResponse = CountDownLatch(1)
            networkRule.checkoutUpdate { response ->
                if (fails) {
                    response.setResponseCode(400)
                    response.setBody("""{"error":{"message":"Invalid tax region"}}""")
                } else {
                    response.testBodyFromFile("checkout-session-init.json")
                }
                requestReceived.countDown()
                check(releaseResponse.await(10, TimeUnit.SECONDS))
            }

            return TaxUpdate(requestReceived, releaseResponse)
        }

        fun startTaxUpdate(taxUpdate: TaxUpdate) {
            addressPage.assertReadyToSave()
            addressPage.clickSave()
            composeTestRule.waitUntilWithIdle {
                taxUpdate.requestReceived.count == 0L
            }
        }

        fun assertSaving() {
            assertThat(activityScenario.state).isEqualTo(Lifecycle.State.RESUMED)
            addressPage.assertSaving(PRIMARY_BUTTON_LOADING_INDICATOR_TEST_TAG)
        }

        fun awaitResult(): AddressElementActivityContract.CheckoutShippingResult {
            waitUntilFinished()
            val result = activityScenario.result
            return AddressElementActivityContract.CheckoutShipping.parseResult(
                result.resultCode,
                result.resultData,
            )
        }

        fun awaitStandaloneResult(): AddressLauncherResult {
            waitUntilFinished()
            val result = activityScenario.result
            return AddressElementActivityContract.Standalone.parseResult(
                result.resultCode,
                result.resultData,
            )
        }

        private fun waitUntilFinished() {
            composeTestRule.waitUntilWithIdle(conditionDescription = "address Activity to finish") {
                composeTestRule.runOnIdle { activity.isFinishing }
            }
        }
    }

    internal data class TaxUpdate(
        val requestReceived: CountDownLatch,
        val releaseResponse: CountDownLatch,
    )
}
