package com.stripe.android.lpm

import androidx.test.platform.app.InstrumentationRegistry
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
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** These tests are special; they don't fail and aren't meant to run in normal CI jobs.
Instead, they measure MPE load times under various configurations and report the
results under the `mpe.synthetic_latency` analytic via [MpeSyntheticsEventReporter]. */
@RunWith(Parameterized::class)
internal class MPELatencyTest(
    private val testName: String,
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

    @Test
    fun latencyTest() {
        assumeRunningInSyntheticsWorkflow()

        testDriver.runLatencyTest(
            testParameters = TestParameters.create(
                paymentMethodCode = "card",
                authorizationAction = null,
                playgroundSettingsBlock = testConfig.playgroundSettingsBlock,
            ),
            isReturningCustomer = testConfig.isReturningCustomer,
            onLaunch = {
                syntheticsEventReporter.onStart()
            },
            onLoad = {
                syntheticsEventReporter.onLoad(testName)
            }
        )
    }

    private fun assumeRunningInSyntheticsWorkflow() {
        assumeTrue(
            "PaymentSheet load synthetics only run when explicitly enabled.",
            InstrumentationRegistry.getArguments()
                .getString(MPE_SYNTHETICS_ENABLED_ARGUMENT)
                .equals("true", ignoreCase = true)
        )
    }

    class TestConfig(
        val isReturningCustomer: Boolean,
        val playgroundSettingsBlock: (PlaygroundSettings) -> Unit,
    )

    companion object {
        private const val MPE_SYNTHETICS_ENABLED_ARGUMENT = "mpe_synthetics_enabled"

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun testConfigs(): List<Array<Any>> {
            return listOf(
                testConfig(
                    testName = "test_link_off_with_no_customer",
                    isReturningCustomer = false,
                ) { settings: PlaygroundSettings ->
                    settings[LinkSettingsDefinition] = false
                    settings[CustomerSettingsDefinition] = CustomerType.GUEST
                },
                testConfig(
                    testName = "test_link_off_with_ek",
                    isReturningCustomer = true,
                ) { settings: PlaygroundSettings ->
                    settings[LinkSettingsDefinition] = false
                    settings[CustomerSettingsDefinition] = CustomerType.RETURNING
                    settings[CustomerSessionSettingsDefinition] = false
                },
                testConfig(
                    testName = "test_link_off_with_cs",
                    isReturningCustomer = true,
                ) { settings: PlaygroundSettings ->
                    settings[LinkSettingsDefinition] = false
                    settings[CustomerSettingsDefinition] = CustomerType.RETURNING
                    settings[CustomerSessionSettingsDefinition] = true
                },
                testConfig(
                    testName = "test_link_on_with_no_customer",
                    isReturningCustomer = false,
                ) { settings: PlaygroundSettings ->
                    settings[LinkSettingsDefinition] = true
                    settings[CustomerSettingsDefinition] = CustomerType.GUEST
                },
                testConfig(
                    testName = "test_link_on_with_ek",
                    isReturningCustomer = true,
                ) { settings: PlaygroundSettings ->
                    settings[LinkSettingsDefinition] = true
                    settings[CustomerSettingsDefinition] = CustomerType.RETURNING
                    settings[CustomerSessionSettingsDefinition] = false
                },
                testConfig(
                    testName = "test_link_on_with_cs",
                    isReturningCustomer = true,
                ) { settings: PlaygroundSettings ->
                    settings[LinkSettingsDefinition] = true
                    settings[CustomerSettingsDefinition] = CustomerType.RETURNING
                    settings[CustomerSessionSettingsDefinition] = true
                },
                testConfig(
                    testName = "test_link_on_with_ek_default_email",
                    isReturningCustomer = true,
                ) { settings: PlaygroundSettings ->
                    settings[LinkSettingsDefinition] = true
                    settings[CustomerSettingsDefinition] = CustomerType.RETURNING
                    settings[CustomerSessionSettingsDefinition] = false
                    settings[DefaultBillingAddressSettingsDefinition] = DefaultBillingAddress.On
                },
                testConfig(
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

        private fun testConfig(
            testName: String,
            isReturningCustomer: Boolean,
            playgroundSettingsBlock: (PlaygroundSettings) -> Unit,
        ): Array<Any> {
            return arrayOf(
                testName,
                TestConfig(
                    isReturningCustomer = isReturningCustomer,
                    playgroundSettingsBlock = playgroundSettingsBlock,
                )
            )
        }
    }
}
