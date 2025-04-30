package com.stripe.android.paymentelement.confirmation.lpms

import android.app.Application
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.rule.IntentsRule
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.paymentelement.confirmation.lpms.foundations.CreateIntentFactory
import com.stripe.android.paymentelement.confirmation.lpms.foundations.LpmAssertionParams
import com.stripe.android.paymentelement.confirmation.lpms.foundations.LpmNetworkTestActivity
import com.stripe.android.paymentelement.confirmation.lpms.foundations.network.MerchantCountry
import com.stripe.android.paymentelement.confirmation.lpms.foundations.network.PublishableKeyFetcher
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.createTestActivityRule
import com.stripe.android.testing.RetryRule
import com.stripe.android.utils.PaymentElementCallbackTestRule
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Rule
import org.junit.rules.RuleChain
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.fail
import kotlin.time.Duration.Companion.seconds

internal open class BaseLpmNetworkTest(
    private val paymentMethodType: PaymentMethod.Type,
) {
    private val application = ApplicationProvider.getApplicationContext<Application>()

    @get:Rule
    val rules: RuleChain = RuleChain.emptyRuleChain()
        .around(createTestActivityRule<LpmNetworkTestActivity>())
        .around(IntentsRule())
        .around(PaymentElementCallbackTestRule())
        .around(RetryRule(attempts = 3))

    fun test(
        testType: TestType,
        country: MerchantCountry = MerchantCountry.US,
        createParams: PaymentMethodCreateParams,
        optionsParams: PaymentMethodOptionsParams? = null,
        extraParams: PaymentMethodExtraParams? = null,
        shippingDetails: AddressDetails? = null,
        customerRequestedSave: Boolean = false,
        allowsManualConfirmation: Boolean = false,
        assertion: suspend (LpmNetworkTestActivity, LpmAssertionParams) -> Unit,
    ) = runLpmNetworkTest(country = country, allowsManualConfirmation = allowsManualConfirmation) {
        val factory = CreateIntentFactory(
            paymentElementCallbackIdentifier = LPM_NETWORK_PAYMENT_ELEMENT_CALLBACK_TEST_IDENTIFIER,
            paymentMethodType = paymentMethodType,
            testClient = testClient,
        )

        val createIntentData = testType.createIntent(country, factory)

        createIntentData.onSuccess { data ->
            assertion(
                this,
                LpmAssertionParams(
                    intent = data.intent,
                    initializationMode = data.initializationMode,
                    createParams = createParams,
                    optionsParams = optionsParams,
                    extraParams = extraParams,
                    shippingDetails = shippingDetails,
                    customerRequestedSave = customerRequestedSave,
                )
            )
        }.onFailure { exception ->
            fail(exception.message, exception)
        }
    }

    private fun runLpmNetworkTest(
        country: MerchantCountry,
        allowsManualConfirmation: Boolean,
        test: suspend LpmNetworkTestActivity.() -> Unit
    ) = runTest(UnconfinedTestDispatcher()) {
        val result = PublishableKeyFetcher.publishableKey(country)

        assertThat(result.isSuccess).isTrue()

        val publishableKey = result.getOrThrow()

        ActivityScenario.launch<LpmNetworkTestActivity>(
            LpmNetworkTestActivity.createIntent(
                context = application,
                args = LpmNetworkTestActivity.Args(
                    publishableKey = publishableKey,
                    paymentElementCallbackIdentifier = LPM_NETWORK_PAYMENT_ELEMENT_CALLBACK_TEST_IDENTIFIER,
                    allowsManualConfirmation = allowsManualConfirmation,
                ),
            )
        ).use { scenario ->
            val testStarted = CountDownLatch(1)
            lateinit var job: Job

            scenario.onActivity { activity ->
                job = launch {
                    test(activity)
                }
                testStarted.countDown()
            }

            testStarted.await(5, TimeUnit.SECONDS)

            withTimeout(90.seconds) {
                job.join()
            }
        }
    }

    private companion object {
        const val LPM_NETWORK_PAYMENT_ELEMENT_CALLBACK_TEST_IDENTIFIER = "LpmNetworkTestIdentifier"
    }
}
