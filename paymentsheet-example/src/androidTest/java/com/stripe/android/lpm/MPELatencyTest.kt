package com.stripe.android.lpm

import android.os.Bundle
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

private const val MPE_SYNTHETICS_ENABLED_ARGUMENT = "mpe_synthetics_enabled"
private const val MPE_BENCHMARK_ENABLED_ARGUMENT = "mpe_benchmark_enabled"

/** These tests are special; they don't fail and aren't meant to run in normal CI jobs.
Instead, they measure MPE load times under various configurations and report the
results either via analytics for synthetics or via logcat for commit benchmarking. */
@RunWith(Parameterized::class)
internal class MPELatencyTest(
    private val testName: String,
    val testConfig: TestConfig,
) : BasePlaygroundTest() {
    private val appContext by lazy {
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
    }

    private val reportingMode by lazy {
        ReportingMode.fromInstrumentationArgs()
    }

    private val latencyReporter by lazy {
        when (reportingMode) {
            ReportingMode.Benchmark -> {
                MpeBenchmarkEventReporter(
                    durationProvider = DefaultDurationProvider.instance,
                )
            }
            ReportingMode.Synthetics -> {
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
            null -> {
                error("MPELatencyTest should only run when a reporting mode is enabled.")
            }
        }
    }

    @Test
    fun latencyTest() {
        assumeRunningInLatencyWorkflow()

        testDriver.runLatencyTest(
            testParameters = TestParameters.create(
                paymentMethodCode = "card",
                authorizationAction = null,
                playgroundSettingsBlock = testConfig.playgroundSettingsBlock,
            ),
            isReturningCustomer = testConfig.isReturningCustomer,
            onLaunch = {
                latencyReporter.onStart()
            },
            onLoad = {
                latencyReporter.onLoad(testName)
            }
        )
    }

    private fun assumeRunningInLatencyWorkflow() {
        assumeTrue(
            "PaymentSheet latency tests only run when explicitly enabled.",
            reportingMode != null
        )
    }

    class TestConfig(
        val isReturningCustomer: Boolean,
        val playgroundSettingsBlock: (PlaygroundSettings) -> Unit,
    )

    companion object {
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

    private enum class ReportingMode {
        Benchmark,
        Synthetics;

        companion object {
            fun fromInstrumentationArgs(): ReportingMode? {
                val arguments = InstrumentationRegistry.getArguments()

                return when {
                    arguments.isEnabled(MPE_BENCHMARK_ENABLED_ARGUMENT) -> Benchmark
                    arguments.isEnabled(MPE_SYNTHETICS_ENABLED_ARGUMENT) -> Synthetics
                    else -> null
                }
            }

            private fun Bundle.isEnabled(key: String): Boolean {
                return getString(key).equals("true", ignoreCase = true)
            }
        }
    }
}
