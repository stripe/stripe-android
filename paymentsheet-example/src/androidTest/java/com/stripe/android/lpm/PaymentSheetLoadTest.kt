package com.stripe.android.lpm

import android.os.SystemClock
import android.util.Log
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.networking.DefaultAnalyticsRequestExecutor
import com.stripe.android.core.networking.NetworkTypeDetector
import com.stripe.android.core.utils.ContextUtils.packageInfo
import com.stripe.android.core.utils.DefaultDurationProvider
import com.stripe.android.paymentsheet.example.playground.settings.CustomerSessionSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerType
import com.stripe.android.paymentsheet.example.playground.settings.DefaultBillingAddress
import com.stripe.android.paymentsheet.example.playground.settings.DefaultBillingAddressSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.LinkSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.PlaygroundSettings
import com.stripe.android.test.core.TestParameters
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import javax.inject.Provider

@RunWith(Parameterized::class)
internal class PaymentSheetLoadTest(
    val testConfig: TestConfig,
) : BasePlaygroundTest() {
    private val appContext by lazy {
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
    }

    private val syntheticsEventReporter by lazy {
        MpeSyntheticsEventReporter(
            analyticsRequestExecutor = DefaultAnalyticsRequestExecutor(),
            analyticsRequestFactory = AnalyticsRequestFactory(
                packageManager = appContext.packageManager,
                packageInfo = appContext.packageInfo,
                packageName = appContext.packageName,
                publishableKeyProvider = {
                    PaymentConfiguration.getInstance(appContext).publishableKey
                },
                networkTypeProvider = NetworkTypeDetector(appContext)::invoke,
            ),
            durationProvider = DefaultDurationProvider.instance,
        )
    }

    companion object {
        private const val LOG_TAG = "PaymentSheetLoadTest"
        private const val RESULT_PREFIX = "RESULT"

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun testConfigs(): List<TestConfig> {
            return listOf(
                TestConfig(
                    testName = "test_link_off_with_no_customer",
                    isReturningCustomer = false,
                ) { settings: PlaygroundSettings ->
                    settings[LinkSettingsDefinition] = false
                    settings[CustomerSettingsDefinition] = CustomerType.GUEST
                },
                TestConfig(
                    testName = "test_link_off_with_ek",
                    isReturningCustomer = true,
                ) { settings: PlaygroundSettings ->
                    settings[LinkSettingsDefinition] = false
                    settings[CustomerSettingsDefinition] = CustomerType.RETURNING
                    settings[CustomerSessionSettingsDefinition] = false
                },
                TestConfig(
                    testName = "test_link_off_with_cs",
                    isReturningCustomer = true,
                ) { settings: PlaygroundSettings ->
                    settings[LinkSettingsDefinition] = false
                    settings[CustomerSettingsDefinition] = CustomerType.RETURNING
                    settings[CustomerSessionSettingsDefinition] = true
                },
                TestConfig(
                    testName = "test_link_on_with_no_customer",
                    isReturningCustomer = false,
                ) { settings: PlaygroundSettings ->
                    settings[LinkSettingsDefinition] = true
                    settings[CustomerSettingsDefinition] = CustomerType.GUEST
                },
                TestConfig(
                    testName = "test_link_on_with_ek",
                    isReturningCustomer = true,
                ) { settings: PlaygroundSettings ->
                    settings[LinkSettingsDefinition] = true
                    settings[CustomerSettingsDefinition] = CustomerType.RETURNING
                    settings[CustomerSessionSettingsDefinition] = false
                },
                TestConfig(
                    testName = "test_link_on_with_cs",
                    isReturningCustomer = true,
                ) { settings: PlaygroundSettings ->
                    settings[LinkSettingsDefinition] = true
                    settings[CustomerSettingsDefinition] = CustomerType.RETURNING
                    settings[CustomerSessionSettingsDefinition] = true
                },
                TestConfig(
                    testName = "test_link_on_with_ek_default_email",
                    isReturningCustomer = true,
                ) { settings: PlaygroundSettings ->
                    settings[LinkSettingsDefinition] = true
                    settings[CustomerSettingsDefinition] = CustomerType.RETURNING
                    settings[CustomerSessionSettingsDefinition] = false
                    settings[DefaultBillingAddressSettingsDefinition] = DefaultBillingAddress.On
                },
                TestConfig(
                    testName = "test_link_on_with_cs_default_email",
                    isReturningCustomer = true,
                ) { settings: PlaygroundSettings ->
                    settings[LinkSettingsDefinition] = true
                    settings[CustomerSettingsDefinition] = CustomerType.RETURNING
                    settings[CustomerSessionSettingsDefinition] = true
                    settings[DefaultBillingAddressSettingsDefinition] = DefaultBillingAddress.On
                },
            )
        }
    }

    class TestConfig(
        val testName: String,
        val isReturningCustomer: Boolean,
        val playgroundSettingsBlock: (PlaygroundSettings) -> Unit,
    ) {
        override fun toString(): String = testName
    }

    @Test
    fun testCardPaymentSheetLoads() {
        var loadStartTimeMs = 0L

        testDriver.loadComplete(
            testParameters = TestParameters.create(
                paymentMethodCode = "card",
                authorizationAction = null,
                playgroundSettingsBlock = testConfig.playgroundSettingsBlock,
            ),
            isReturningCustomer = testConfig.isReturningCustomer,
            afterLaunch = {
                loadStartTimeMs = SystemClock.elapsedRealtime()
                syntheticsEventReporter.onStart()
            },
            afterLoad = {
                val loadDurationMs = SystemClock.elapsedRealtime() - loadStartTimeMs
                syntheticsEventReporter.onLoad(testConfig.testName)
                Log.i(
                    LOG_TAG,
                    "Initial screen displayed for ${testConfig.testName} in ${loadDurationMs}ms"
                )
                Log.i(LOG_TAG, "$RESULT_PREFIX,${testConfig.testName},${loadDurationMs}")
            }
        )
    }
}
