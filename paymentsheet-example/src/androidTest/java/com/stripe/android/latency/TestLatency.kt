package com.stripe.android.latency

import android.util.Log
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.paymentsheet.example.BuildConfig
import com.stripe.android.paymentsheet.example.playground.settings.CustomerSessionSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerType
import com.stripe.android.paymentsheet.example.playground.settings.DefaultBillingAddress
import com.stripe.android.paymentsheet.example.playground.settings.DefaultBillingAddressSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.DisablePassiveCaptchaWarmupDefinition
import com.stripe.android.paymentsheet.example.playground.settings.GooglePayMode
import com.stripe.android.paymentsheet.example.playground.settings.GooglePaySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.LinkSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Merchant
import com.stripe.android.paymentsheet.example.playground.settings.MerchantSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.PlaygroundSettings
import com.stripe.android.test.core.TestParameters
import org.junit.Assume
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * This test is special -- it's intended to be used to measure loading latency of PaymentSheet. As such, it doesn't
 * run in CI, but can be run locally (to test changes) and via the latency benchmarking script:
 * scripts/measure_latency_difference.rb.
 * */
@RunWith(Parameterized::class)
internal class TestLatency(
    val testConfig: TestConfig,
) : BasePlaygroundTest() {
    @Test
    fun testLatency() {
        Assume.assumeFalse(BuildConfig.IS_RUNNING_IN_CI)

        if (!testConfig.skipMeasurement) {
            Log.d(LOG_TAG, "LATENCY_TEST_CASE_STARTED: ${testConfig.name}")
        }

        runTestWithIterations(
            requiredSuccessfulSamples = if (testConfig.skipMeasurement) {
                1
            } else {
                BuildConfig.LATENCY_EXPERIMENT_ITERATIONS
            }
        ) {
            testDriver.loadComplete(
                testParameters = TestParameters.create(
                    paymentMethodCode = "card",
                    playgroundSettingsBlock = testConfig.playgroundSettingsBlock,
                ),
                isReturningCustomer = testConfig.isReturningCustomer,
            )
        }

        if (!testConfig.skipMeasurement) {
            Log.d(LOG_TAG, "LATENCY_TEST_CASE_FINISHED: ${testConfig.name}")
        }
    }

    fun runTestWithIterations(
        requiredSuccessfulSamples: Int = BuildConfig.LATENCY_EXPERIMENT_ITERATIONS,
        block: () -> Unit,
    ) {
        var successfulSamples = 0
        var failedAttempts = 0
        var lastFailure: Throwable? = null

        while (successfulSamples < requiredSuccessfulSamples && failedAttempts < MAX_FAILED_ATTEMPTS) {
            runCatching {
                block()
            }.onSuccess {
                successfulSamples += 1
            }.onFailure { error ->
                failedAttempts += 1
                lastFailure = error
            }
        }

        if (successfulSamples < requiredSuccessfulSamples) {
            throw AssertionError(
                "Collected $successfulSamples/$requiredSuccessfulSamples samples " +
                    "for ${testConfig.name} after $failedAttempts failed attempts",
                lastFailure,
            )
        }
    }

    class TestConfig(
        val name: String,
        val isReturningCustomer: Boolean,
        val playgroundSettingsBlock: (PlaygroundSettings) -> Unit,
        val skipMeasurement: Boolean = false,
    ) {
        override fun toString() = name
    }

    companion object {
        private const val LOG_TAG = "StripeSdk"
        private const val MAX_FAILED_ATTEMPTS = 3

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun testConfigs(): List<Array<Any>> {
            return listOf(
                // Warm the process and network stack before collecting measured samples.
                testConfig(
                    testName = "warm_up_test_link_off_with_no_customer",
                    isReturningCustomer = false,
                    skipMeasurement = true,
                ) { settings: PlaygroundSettings ->
                    settings[MerchantSettingsDefinition] = Merchant.US
                    settings[LinkSettingsDefinition] = false
                    settings[CustomerSettingsDefinition] = CustomerType.GUEST
                    settings[GooglePaySettingsDefinition] = GooglePayMode.Off
                },
                testConfig(
                    testName = "test_link_off_with_no_customer",
                    isReturningCustomer = false,
                ) { settings: PlaygroundSettings ->
                    settings[MerchantSettingsDefinition] = Merchant.US
                    settings[LinkSettingsDefinition] = false
                    settings[CustomerSettingsDefinition] = CustomerType.GUEST
                    settings[GooglePaySettingsDefinition] = GooglePayMode.Off
                },
                testConfig(
                    testName = "test_link_off_with_ek",
                    isReturningCustomer = true,
                ) { settings: PlaygroundSettings ->
                    settings[MerchantSettingsDefinition] = Merchant.US
                    settings[LinkSettingsDefinition] = false
                    settings[CustomerSettingsDefinition] = CustomerType.RETURNING
                    settings[CustomerSessionSettingsDefinition] = false
                    settings[GooglePaySettingsDefinition] = GooglePayMode.Off
                },
                testConfig(
                    testName = "test_link_off_with_cs",
                    isReturningCustomer = true,
                ) { settings: PlaygroundSettings ->
                    settings[MerchantSettingsDefinition] = Merchant.US
                    settings[LinkSettingsDefinition] = false
                    settings[CustomerSettingsDefinition] = CustomerType.RETURNING
                    settings[CustomerSessionSettingsDefinition] = true
                    settings[GooglePaySettingsDefinition] = GooglePayMode.Off
                },
                testConfig(
                    testName = "test_link_on_with_no_customer",
                    isReturningCustomer = false,
                ) { settings: PlaygroundSettings ->
                    settings[MerchantSettingsDefinition] = Merchant.US
                    settings[LinkSettingsDefinition] = true
                    settings[CustomerSettingsDefinition] = CustomerType.GUEST
                    settings[GooglePaySettingsDefinition] = GooglePayMode.Off
                },
                testConfig(
                    testName = "test_link_on_with_ek",
                    isReturningCustomer = true,
                ) { settings: PlaygroundSettings ->
                    settings[MerchantSettingsDefinition] = Merchant.US
                    settings[LinkSettingsDefinition] = true
                    settings[CustomerSettingsDefinition] = CustomerType.RETURNING
                    settings[CustomerSessionSettingsDefinition] = false
                    settings[GooglePaySettingsDefinition] = GooglePayMode.Off
                },
                testConfig(
                    testName = "test_link_on_with_cs",
                    isReturningCustomer = true,
                ) { settings: PlaygroundSettings ->
                    settings[MerchantSettingsDefinition] = Merchant.US
                    settings[LinkSettingsDefinition] = true
                    settings[CustomerSettingsDefinition] = CustomerType.RETURNING
                    settings[CustomerSessionSettingsDefinition] = true
                    settings[GooglePaySettingsDefinition] = GooglePayMode.Off
                },
                testConfig(
                    testName = "test_link_on_with_ek_default_email",
                    isReturningCustomer = true,
                ) { settings: PlaygroundSettings ->
                    settings[MerchantSettingsDefinition] = Merchant.US
                    settings[LinkSettingsDefinition] = true
                    settings[CustomerSettingsDefinition] = CustomerType.RETURNING
                    settings[CustomerSessionSettingsDefinition] = false
                    settings[DefaultBillingAddressSettingsDefinition] = DefaultBillingAddress.On
                    settings[GooglePaySettingsDefinition] = GooglePayMode.Off
                },
                testConfig(
                    testName = "test_link_on_with_cs_default_email",
                    isReturningCustomer = true,
                ) { settings: PlaygroundSettings ->
                    settings[MerchantSettingsDefinition] = Merchant.US
                    settings[LinkSettingsDefinition] = true
                    settings[CustomerSettingsDefinition] = CustomerType.RETURNING
                    settings[CustomerSessionSettingsDefinition] = true
                    settings[DefaultBillingAddressSettingsDefinition] = DefaultBillingAddress.On
                    settings[GooglePaySettingsDefinition] = GooglePayMode.Off
                },
                testConfig(
                    testName = "test_google_pay_on_link_off_with_no_customer",
                    isReturningCustomer = false,
                ) { settings: PlaygroundSettings ->
                    settings[GooglePaySettingsDefinition] = GooglePayMode.Test
                    settings[MerchantSettingsDefinition] = Merchant.US
                    settings[LinkSettingsDefinition] = false
                    settings[CustomerSettingsDefinition] = CustomerType.GUEST
                },
                testConfig(
                    testName = "test_google_pay_on_link_off_with_ek",
                    isReturningCustomer = true,
                ) { settings: PlaygroundSettings ->
                    settings[GooglePaySettingsDefinition] = GooglePayMode.Test
                    settings[MerchantSettingsDefinition] = Merchant.US
                    settings[LinkSettingsDefinition] = false
                    settings[CustomerSettingsDefinition] = CustomerType.RETURNING
                    settings[CustomerSessionSettingsDefinition] = false
                },
                testConfig(
                    testName = "test_google_pay_on_link_off_with_cs",
                    isReturningCustomer = true,
                ) { settings: PlaygroundSettings ->
                    settings[GooglePaySettingsDefinition] = GooglePayMode.Test
                    settings[MerchantSettingsDefinition] = Merchant.US
                    settings[LinkSettingsDefinition] = false
                    settings[CustomerSettingsDefinition] = CustomerType.RETURNING
                    settings[CustomerSessionSettingsDefinition] = true
                },
                testConfig(
                    testName = "test_google_pay_on_link_on_with_cs",
                    isReturningCustomer = true,
                ) { settings: PlaygroundSettings ->
                    settings[GooglePaySettingsDefinition] = GooglePayMode.Test
                    settings[MerchantSettingsDefinition] = Merchant.US
                    settings[LinkSettingsDefinition] = true
                    settings[CustomerSettingsDefinition] = CustomerType.RETURNING
                    settings[CustomerSessionSettingsDefinition] = true
                },
            )
        }

        private fun testConfig(
            testName: String,
            isReturningCustomer: Boolean,
            skipMeasurement: Boolean = false,
            playgroundSettingsBlock: (PlaygroundSettings) -> Unit,
        ): Array<Any> {
            return arrayOf(
                TestConfig(
                    name = testName,
                    isReturningCustomer = isReturningCustomer,
                    playgroundSettingsBlock = { settings ->
                        settings[DisablePassiveCaptchaWarmupDefinition] = true
                        playgroundSettingsBlock(settings)
                    },
                    skipMeasurement = skipMeasurement,
                )
            )
        }
    }
}
