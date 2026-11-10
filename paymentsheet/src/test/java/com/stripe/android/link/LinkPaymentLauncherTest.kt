package com.stripe.android.link

import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.link.account.LinkStore
import com.stripe.android.link.analytics.DefaultLinkAnalyticsHelper
import com.stripe.android.link.analytics.FakeLinkAnalyticsHelper
import com.stripe.android.link.analytics.FakeLinkEventsReporter
import com.stripe.android.link.analytics.LinkAnalyticsHelper
import com.stripe.android.link.analytics.LinkEventsReporter
import com.stripe.android.link.injection.LinkAccountAnalyticsModule
import com.stripe.android.link.injection.LinkCommonModule
import com.stripe.android.link.injection.LinkComponent
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.paymentelement.confirmation.asCallbackFor
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.DummyActivityResultCaller
import com.stripe.android.testing.FakeAnalyticsRequestExecutor
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.utils.FakeActivityResultRegistry
import com.stripe.android.utils.FakeDurationProvider
import com.stripe.android.utils.FakeLinkComponent
import com.stripe.android.utils.FakeLinkConfigurationCoordinator
import com.stripe.android.utils.FakeLinkStore
import com.stripe.android.utils.RecordingLinkStore
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class LinkPaymentLauncherTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `register call should register ActivityResultLauncher with ActivityResultRegistry`() {
        val linkActivityContract: LinkActivityContract = mock()
        val activityResultRegistry: ActivityResultRegistry = mock()
        val activityResultLauncher: ActivityResultLauncher<LinkActivityContract.Args> = mock()

        val linkPaymentLauncher = createLinkPaymentLauncher(linkActivityContract = linkActivityContract)

        setupActivityResultRegistryMock(activityResultRegistry, activityResultLauncher)

        linkPaymentLauncher.register(activityResultRegistry = activityResultRegistry) {}

        verifyActivityResultRegistryRegister(activityResultRegistry, linkActivityContract)
    }

    @Test
    fun `register call should register ActivityResultLauncher with ActivityResultCaller`() = runTest {
        DummyActivityResultCaller.test {
            val linkPaymentLauncher = createLinkPaymentLauncher()

            linkPaymentLauncher.register(activityResultCaller) {}

            val registerCall = awaitRegisterCall()
            assertThat(registerCall).isNotNull()

            awaitNextRegisteredLauncher()
        }
    }

    @Test
    fun `unregister call should unregister ActivityResultLauncher`() = runTest {
        DummyActivityResultCaller.test {
            val linkPaymentLauncher = createLinkPaymentLauncher()

            linkPaymentLauncher.register(activityResultCaller) {}

            awaitRegisterCall()

            linkPaymentLauncher.unregister()

            val registeredLauncher = awaitNextRegisteredLauncher()
            val unregisteredLauncher = awaitNextUnregisteredLauncher()

            assertThat(unregisteredLauncher).isEqualTo(registeredLauncher)
        }
    }

    @Test
    fun `present should launch with correct args and trigger analytics`() = runTest {
        DummyActivityResultCaller.test {
            val linkPaymentLauncher = createLinkPaymentLauncher()

            linkPaymentLauncher.register(activityResultCaller) {}

            val registerCall = awaitRegisterCall()
            assertThat(registerCall).isNotNull()

            linkPaymentLauncher.present(
                configuration = TestFactory.LINK_CONFIGURATION,
                paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
                linkAccountInfo = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT),
                linkExpressMode = LinkExpressMode.ENABLED,
                launchMode = LinkLaunchMode.Full(showSecondaryButton = true),
                statusBarColor = 0x00FF00,
            )

            val launchCall = awaitLaunchCall()

            assertThat(launchCall)
                .isEqualTo(
                    LinkActivityContract.Args(
                        configuration = TestFactory.LINK_CONFIGURATION,
                        paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
                        linkExpressMode = LinkExpressMode.ENABLED,
                        linkAccountInfo = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT),
                        launchMode = LinkLaunchMode.Full(showSecondaryButton = true),
                        statusBarColor = 0x00FF00,
                    )
                )

            awaitNextRegisteredLauncher()
        }
    }

    @Test
    fun `present should launch with correct args when useLinkExpress is false`() = runTest {
        DummyActivityResultCaller.test {
            val linkPaymentLauncher = createLinkPaymentLauncher()

            linkPaymentLauncher.register(activityResultCaller) {}

            val registerCall = awaitRegisterCall()
            assertThat(registerCall).isNotNull()

            linkPaymentLauncher.present(
                configuration = TestFactory.LINK_CONFIGURATION,
                paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
                linkAccountInfo = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT),
                linkExpressMode = LinkExpressMode.DISABLED,
                launchMode = LinkLaunchMode.Full(showSecondaryButton = true),
                statusBarColor = null,
            )

            val launchCall = awaitLaunchCall() as? LinkActivityContract.Args
            assertThat(launchCall?.linkExpressMode).isEqualTo(LinkExpressMode.DISABLED)
            awaitNextRegisteredLauncher()
        }
    }

    @Test
    fun `present should launch with correct args when attestOnIntentConfirmation is true`() = runTest {
        testPresentWithAttestOnIntentConfirmation(attestOnIntentConfirmation = true)
    }

    @Test
    fun `present should launch with correct args when attestOnIntentConfirmation is false`() = runTest {
        testPresentWithAttestOnIntentConfirmation(attestOnIntentConfirmation = false)
    }

    private suspend fun testPresentWithAttestOnIntentConfirmation(attestOnIntentConfirmation: Boolean) {
        DummyActivityResultCaller.test {
            val linkPaymentLauncher = createLinkPaymentLauncher()

            linkPaymentLauncher.register(activityResultCaller) {}

            val registerCall = awaitRegisterCall()
            assertThat(registerCall).isNotNull()

            linkPaymentLauncher.present(
                configuration = TestFactory.LINK_CONFIGURATION,
                paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                    attestOnIntentConfirmation = attestOnIntentConfirmation
                ),
                linkAccountInfo = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT),
                linkExpressMode = LinkExpressMode.ENABLED,
                launchMode = LinkLaunchMode.Full(showSecondaryButton = true),
                statusBarColor = null,
            )

            val launchCall = awaitLaunchCall() as? LinkActivityContract.Args
            assertThat(launchCall?.paymentMethodMetadata?.attestOnIntentConfirmation)
                .isEqualTo(attestOnIntentConfirmation)
            awaitNextRegisteredLauncher()
        }
    }

    @Test
    fun `ActivityResultRegistry callback should handle Completed result correctly`() {
        testActivityResultCallbackWithActivityResultRegistry(
            linkActivityResult = LinkActivityResult.Completed(
                linkAccountUpdate = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT),
                selectedPayment = null,
            ),
            expectedMarkAsUsedCalls = 1,
        )
    }

    @Test
    fun `ActivityResultRegistry callback should handle PaymentMethodObtained result correctly`() {
        testActivityResultCallbackWithActivityResultRegistry(
            linkActivityResult = LinkActivityResult.PaymentMethodObtained(PaymentMethodFixtures.LINK_PAYMENT_METHOD),
            expectedMarkAsUsedCalls = 1,
        )
    }

    @Test
    fun `ActivityResultRegistry callback should handle Canceled result correctly`() {
        testActivityResultCallbackWithActivityResultRegistry(
            linkActivityResult = LinkActivityResult.Canceled(
                linkAccountUpdate = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT)
            ),
            expectedMarkAsUsedCalls = 0,
        )
    }

    @Test
    fun `ActivityResultRegistry callback should handle Failed result correctly`() {
        testActivityResultCallbackWithActivityResultRegistry(
            linkActivityResult = LinkActivityResult.Failed(
                error = Exception("oops"),
                linkAccountUpdate = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT)
            ),
            expectedMarkAsUsedCalls = 0,
        )
    }

    @Test
    fun `ActivityResultCaller callback should handle Completed result correctly`() {
        testActivityResultCallbackWithResultCaller(
            linkActivityResult = LinkActivityResult.Completed(
                linkAccountUpdate = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT),
                selectedPayment = null
            ),
            expectedMarkAsUsedCalls = 1,
        )
    }

    @Test
    fun `ActivityResultCaller callback should handle PaymentMethodObtained result correctly`() {
        testActivityResultCallbackWithResultCaller(
            linkActivityResult = LinkActivityResult.PaymentMethodObtained(PaymentMethodFixtures.LINK_PAYMENT_METHOD),
            expectedMarkAsUsedCalls = 1,
        )
    }

    @Test
    fun `ActivityResultCaller callback should handle Canceled result correctly`() {
        testActivityResultCallbackWithResultCaller(
            linkActivityResult = LinkActivityResult.Canceled(
                linkAccountUpdate = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT)
            ),
            expectedMarkAsUsedCalls = 0,
        )
    }

    @Test
    fun `ActivityResultCaller callback should handle Failed result correctly`() {
        testActivityResultCallbackWithResultCaller(
            linkActivityResult = LinkActivityResult.Failed(
                error = Exception("oops"),
                linkAccountUpdate = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT)
            ),
            expectedMarkAsUsedCalls = 0,
        )
    }

    @Test
    fun `present and result analytics carry the configuration's checkout session ID`() = runAnalyticsScenario {
        presentAndComplete(checkoutConfiguration("cs_test_123"), expectedCheckoutSessionId = "cs_test_123")
    }

    @Test
    fun `subsequent presentations report their own checkout session ID`() = runAnalyticsScenario {
        presentAndComplete(checkoutConfiguration("cs_test_first"), expectedCheckoutSessionId = "cs_test_first")
        presentAndComplete(checkoutConfiguration("cs_test_second"), expectedCheckoutSessionId = "cs_test_second")
    }

    @Test
    fun `presentation without checkout omits a previous checkout session ID`() = runAnalyticsScenario {
        presentAndComplete(checkoutConfiguration("cs_test_first"), expectedCheckoutSessionId = "cs_test_first")
        presentAndComplete(TestFactory.LINK_CONFIGURATION, expectedCheckoutSessionId = null)
    }

    @Test
    fun `result retains the presented helper when the coordinator moves to another configuration`() =
        runAnalyticsScenario {
            present(checkoutConfiguration("cs_test_first"))
            assertPopupEvent("link.popup.show", expectedCheckoutSessionId = "cs_test_first")

            coordinator.getComponent(checkoutConfiguration("cs_test_second"))

            complete()
            assertPopupEvent("link.popup.success", expectedCheckoutSessionId = "cs_test_first")
        }

    @Test
    fun `pending result before presentation reports analytics without a checkout session ID`() = runAnalyticsScenario {
        val result = LinkActivityResult.Canceled(linkAccountUpdate = LinkAccountUpdate.None)

        callback.onActivityResult(result)

        assertThat(resultCalls.awaitItem()).isSameInstanceAs(result)
        assertPopupEvent("link.popup.cancel", expectedCheckoutSessionId = null)
    }

    private fun testActivityResultCallbackWithActivityResultRegistry(
        linkActivityResult: LinkActivityResult,
        expectedMarkAsUsedCalls: Int,
    ) = runTest {
        RecordingLinkStore.test {
            val activityResultRegistry: ActivityResultRegistry = FakeActivityResultRegistry(linkActivityResult)

            val linkAnalyticsHelper = TrackingLinkAnalyticsHelper()
            val linkPaymentLauncher = createLinkPaymentLauncher(
                linkAnalyticsHelper = linkAnalyticsHelper,
                linkStore = linkStore
            )

            var callbackParam: LinkActivityResult? = null
            linkPaymentLauncher.register(activityResultRegistry = activityResultRegistry) { callbackParam = it }

            linkPaymentLauncher.present(
                configuration = TestFactory.LINK_CONFIGURATION,
                paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
                linkAccountInfo = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT),
                linkExpressMode = LinkExpressMode.ENABLED,
                launchMode = LinkLaunchMode.Full(showSecondaryButton = true),
                statusBarColor = null,
            )

            verifyActivityResultCallback(
                linkActivityResult = linkActivityResult,
                linkAnalyticsHelper = linkAnalyticsHelper,
                expectedMarkAsUsedCalls = expectedMarkAsUsedCalls,
                callbackResult = callbackParam,
            )
        }
    }

    private fun testActivityResultCallbackWithResultCaller(
        linkActivityResult: LinkActivityResult,
        expectedMarkAsUsedCalls: Int,
    ) = runTest {
        RecordingLinkStore.test {
            DummyActivityResultCaller.test {
                val linkAnalyticsHelper = TrackingLinkAnalyticsHelper()
                val linkPaymentLauncher = createLinkPaymentLauncher(
                    linkAnalyticsHelper = linkAnalyticsHelper,
                    linkStore = linkStore
                )

                var callbackParam: LinkActivityResult? = null
                linkPaymentLauncher.register(activityResultCaller) { callbackParam = it }

                linkPaymentLauncher.present(
                    configuration = TestFactory.LINK_CONFIGURATION,
                    paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
                    linkAccountInfo = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT),
                    linkExpressMode = LinkExpressMode.ENABLED,
                    launchMode = LinkLaunchMode.Full(showSecondaryButton = true),
                    statusBarColor = null,
                )

                val registerCall = awaitRegisterCall()
                registerCall.callback.asCallbackFor<LinkActivityResult>().onActivityResult(linkActivityResult)

                verifyActivityResultCallback(
                    linkActivityResult = linkActivityResult,
                    linkAnalyticsHelper = linkAnalyticsHelper,
                    expectedMarkAsUsedCalls = expectedMarkAsUsedCalls,
                    callbackResult = callbackParam,
                )

                awaitLaunchCall()
                awaitNextRegisteredLauncher()
            }
        }
    }

    private suspend fun RecordingLinkStore.Scenario.verifyActivityResultCallback(
        linkActivityResult: LinkActivityResult,
        linkAnalyticsHelper: TrackingLinkAnalyticsHelper,
        expectedMarkAsUsedCalls: Int,
        callbackResult: LinkActivityResult?,
    ) {
        assertThat(callbackResult).isEqualTo(linkActivityResult)
        assertThat(markAsUsedCalls.cancelAndConsumeRemainingEvents().size).isEqualTo(expectedMarkAsUsedCalls)
        linkAnalyticsHelper.launches.awaitItem()
        assertThat(linkAnalyticsHelper.results.awaitItem()).isSameInstanceAs(linkActivityResult)
        linkAnalyticsHelper.ensureAllEventsConsumed()
    }

    private fun setupActivityResultRegistryMock(
        activityResultRegistry: ActivityResultRegistry,
        launcher: ActivityResultLauncher<LinkActivityContract.Args>
    ) {
        whenever(
            activityResultRegistry.register(
                eq("LinkPaymentLauncher"),
                any<ActivityResultContract<LinkActivityContract.Args, LinkActivityResult>>(),
                any()
            )
        ).thenReturn(launcher)
    }

    private fun verifyActivityResultRegistryRegister(
        activityResultRegistry: ActivityResultRegistry,
        linkActivityContract: LinkActivityContract
    ) {
        verify(activityResultRegistry).register(
            eq("PaymentElementCallbackIdentifier_LinkPaymentLauncher"),
            eq(linkActivityContract),
            any()
        )
    }

    private fun createLinkPaymentLauncher(
        linkActivityContract: LinkActivityContract = mock(),
        linkAnalyticsHelper: LinkAnalyticsHelper = FakeLaunchOnlyLinkAnalyticsHelper(),
        linkConfigurationCoordinator: LinkConfigurationCoordinator = FakeLinkConfigurationCoordinator(
            component = FakeLinkComponent(linkAnalyticsHelper = linkAnalyticsHelper),
        ),
        linkEventsReporter: LinkEventsReporter = FakeLinkEventsReporter(),
        linkStore: LinkStore = FakeLinkStore(),
    ): LinkPaymentLauncher {
        return LinkPaymentLauncher(
            linkConfigurationCoordinator = linkConfigurationCoordinator,
            linkEventsReporter = linkEventsReporter,
            linkActivityContract = linkActivityContract,
            linkStore = linkStore,
            paymentElementCallbackIdentifier = "PaymentElementCallbackIdentifier",
        )
    }

    private fun checkoutConfiguration(checkoutSessionId: String): LinkConfiguration {
        return TestFactory.LINK_CONFIGURATION.copy(
            clientAttributionMetadata = PaymentMethodMetadataFixtures.CHECKOUT_SESSION_CLIENT_ATTRIBUTION_METADATA.copy(
                checkoutSessionId = checkoutSessionId,
            ),
        )
    }

    @Suppress("LongMethod")
    private fun runAnalyticsScenario(block: suspend AnalyticsScenario.() -> Unit) = runTest {
        val analyticsRequestExecutor = FakeAnalyticsRequestExecutor()
        val errorReporter = FakeErrorReporter()
        val durationProvider = FakeDurationProvider()
        val linkStore = FakeLinkStore()
        val workContext = coroutineContext
        val requestFactory = PaymentAnalyticsRequestFactory(
            context = ApplicationProvider.getApplicationContext(),
            publishableKey = "pk_test",
        )
        val coordinator = RealLinkConfigurationCoordinator(
            linkComponentFactory = object : LinkComponent.Factory {
                override fun create(configuration: LinkConfiguration): LinkComponent {
                    val reporter = LinkAccountAnalyticsModule.provideLinkEventsReporter(
                        configuration = configuration,
                        analyticsRequestExecutor = analyticsRequestExecutor,
                        paymentAnalyticsRequestFactory = requestFactory,
                        errorReporter = errorReporter,
                        workContext = workContext,
                        logger = Logger.noop(),
                        durationProvider = durationProvider,
                    )
                    return FakeLinkComponent(
                        configuration = configuration,
                        linkAnalyticsHelper = DefaultLinkAnalyticsHelper(reporter),
                    )
                }
            },
        )
        val launcher = createLinkPaymentLauncher(
            linkConfigurationCoordinator = coordinator,
            linkEventsReporter = LinkCommonModule.provideLinkEventsReporter(
                analyticsRequestExecutor = analyticsRequestExecutor,
                paymentAnalyticsRequestFactory = requestFactory,
                errorReporter = errorReporter,
                workContext = workContext,
                logger = Logger.noop(),
                durationProvider = durationProvider,
            ),
            linkStore = linkStore,
        )
        val resultCalls = Turbine<LinkActivityResult>()

        DummyActivityResultCaller.test {
            launcher.register(activityResultCaller) { resultCalls.add(it) }
            val callback = awaitRegisterCall().callback.asCallbackFor<LinkActivityResult>()
            awaitNextRegisteredLauncher()

            AnalyticsScenario(
                launcher = launcher,
                coordinator = coordinator,
                callerScenario = this,
                callback = callback,
                resultCalls = resultCalls,
                analyticsRequestExecutor = analyticsRequestExecutor,
                linkStore = linkStore,
                testScheduler = testScheduler,
            ).block()
        }

        resultCalls.ensureAllEventsConsumed()
        analyticsRequestExecutor.validate()
        errorReporter.ensureAllEventsConsumed()
        durationProvider.validate()
        linkStore.ensureAllEventsConsumed()
    }

    private class AnalyticsScenario(
        val launcher: LinkPaymentLauncher,
        val coordinator: LinkConfigurationCoordinator,
        val callerScenario: DummyActivityResultCaller.Scenario,
        val callback: ActivityResultCallback<LinkActivityResult>,
        val resultCalls: Turbine<LinkActivityResult>,
        val analyticsRequestExecutor: FakeAnalyticsRequestExecutor,
        val linkStore: FakeLinkStore,
        val testScheduler: TestCoroutineScheduler,
    ) {
        suspend fun present(configuration: LinkConfiguration) {
            launcher.present(
                configuration = configuration,
                paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
                linkAccountInfo = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT),
                linkExpressMode = LinkExpressMode.ENABLED,
                launchMode = LinkLaunchMode.Full(showSecondaryButton = true),
                statusBarColor = null,
            )
            val args = callerScenario.awaitLaunchCall() as LinkActivityContract.Args
            assertThat(args.configuration).isEqualTo(configuration)
        }

        suspend fun complete() {
            val result = LinkActivityResult.Completed(linkAccountUpdate = LinkAccountUpdate.None)
            callback.onActivityResult(result)
            assertThat(resultCalls.awaitItem()).isSameInstanceAs(result)
            linkStore.markAsUsedCalls.awaitItem()
        }

        suspend fun assertPopupEvent(eventName: String, expectedCheckoutSessionId: String?) {
            testScheduler.runCurrent()
            val params = analyticsRequestExecutor.awaitEvent(eventName)
            if (expectedCheckoutSessionId == null) {
                assertThat(params).doesNotContainKey("checkout_session_id")
            } else {
                assertThat(params["checkout_session_id"]).isEqualTo(expectedCheckoutSessionId)
            }
        }

        suspend fun presentAndComplete(configuration: LinkConfiguration, expectedCheckoutSessionId: String?) {
            present(configuration)
            assertPopupEvent("link.popup.show", expectedCheckoutSessionId)
            complete()
            assertPopupEvent("link.popup.success", expectedCheckoutSessionId)
        }
    }
}

private class TrackingLinkAnalyticsHelper : FakeLinkAnalyticsHelper() {
    val launches = Turbine<Unit>()
    val results = Turbine<LinkActivityResult>()

    override fun onLinkLaunched() {
        launches.add(Unit)
    }

    override fun onLinkResult(linkActivityResult: LinkActivityResult) {
        results.add(linkActivityResult)
    }

    fun ensureAllEventsConsumed() {
        launches.ensureAllEventsConsumed()
        results.ensureAllEventsConsumed()
    }
}

private class FakeLaunchOnlyLinkAnalyticsHelper : FakeLinkAnalyticsHelper() {
    override fun onLinkLaunched() = Unit
}
