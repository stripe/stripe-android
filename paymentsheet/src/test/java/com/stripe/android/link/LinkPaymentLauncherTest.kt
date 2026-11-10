package com.stripe.android.link

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.account.LinkStore
import com.stripe.android.link.analytics.FakeLinkAnalyticsHelper
import com.stripe.android.link.analytics.FakeLinkEventsReporter
import com.stripe.android.link.analytics.LinkAnalyticsHelper
import com.stripe.android.link.analytics.LinkEventsReporter
import com.stripe.android.link.injection.LinkComponent
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.confirmation.asCallbackFor
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.DummyActivityResultCaller
import com.stripe.android.utils.FakeActivityResultRegistry
import com.stripe.android.utils.FakeLinkComponent
import com.stripe.android.utils.FakeLinkConfigurationCoordinator
import com.stripe.android.utils.FakeLinkStore
import com.stripe.android.utils.RecordingLinkStore
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

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
    fun `result and launch reach the presented configuration's helper after the coordinator moves on`() = runTest {
        DummyActivityResultCaller.test {
            val helperA = TrackingLinkAnalyticsHelper()
            val helperB = TrackingLinkAnalyticsHelper()
            var component: LinkComponent = FakeLinkComponent(linkAnalyticsHelper = helperA)
            val coordinator = object : LinkConfigurationCoordinator by FakeLinkConfigurationCoordinator() {
                override fun getComponent(configuration: LinkConfiguration): LinkComponent = component
            }
            val linkPaymentLauncher = createLinkPaymentLauncher(linkConfigurationCoordinator = coordinator)
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
            component = FakeLinkComponent(linkAnalyticsHelper = helperB)

            val result = LinkActivityResult.Canceled(linkAccountUpdate = LinkAccountUpdate.None)
            awaitRegisterCall().callback.asCallbackFor<LinkActivityResult>().onActivityResult(result)

            assertThat(callbackParam).isEqualTo(result)
            assertThat(helperA.launchCount).isEqualTo(1)
            assertThat(helperA.results).containsExactly(result)
            assertThat(helperB.launchCount).isEqualTo(0)
            assertThat(helperB.results).isEmpty()

            awaitLaunchCall()
            awaitNextRegisteredLauncher()
        }
    }

    @Test
    fun `result before any presentation goes to the fallback helper`() = runTest {
        DummyActivityResultCaller.test {
            val linkEventsReporter = RecordingLinkEventsReporter()
            val linkPaymentLauncher = createLinkPaymentLauncher(linkEventsReporter = linkEventsReporter)
            linkPaymentLauncher.register(activityResultCaller) {}

            val result = LinkActivityResult.Canceled(linkAccountUpdate = LinkAccountUpdate.None)
            awaitRegisterCall().callback.asCallbackFor<LinkActivityResult>().onActivityResult(result)

            linkEventsReporter.popupCancels.awaitItem()
            linkEventsReporter.popupCancels.ensureAllEventsConsumed()

            awaitNextRegisteredLauncher()
        }
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
        assertThat(linkAnalyticsHelper.results).containsExactly(linkActivityResult)
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
        linkAnalyticsHelper: LinkAnalyticsHelper = TrackingLinkAnalyticsHelper(),
        linkConfigurationCoordinator: LinkConfigurationCoordinator = FakeLinkConfigurationCoordinator(
            component = FakeLinkComponent(linkAnalyticsHelper = linkAnalyticsHelper),
        ),
        linkEventsReporter: LinkEventsReporter = FakeLinkEventsReporter(),
        linkStore: LinkStore = FakeLinkStore()
    ): LinkPaymentLauncher {
        return LinkPaymentLauncher(
            linkConfigurationCoordinator = linkConfigurationCoordinator,
            linkEventsReporter = linkEventsReporter,
            linkActivityContract = linkActivityContract,
            linkStore = linkStore,
            paymentElementCallbackIdentifier = "PaymentElementCallbackIdentifier"
        )
    }
}

private class TrackingLinkAnalyticsHelper : FakeLinkAnalyticsHelper() {
    val results = arrayListOf<LinkActivityResult>()
    var launchCount = 0

    override fun onLinkLaunched() {
        launchCount++
    }

    override fun onLinkResult(linkActivityResult: LinkActivityResult) {
        results.add(linkActivityResult)
    }
}

private class RecordingLinkEventsReporter : FakeLinkEventsReporter() {
    val popupCancels = Turbine<Unit>()

    override fun onPopupCancel() {
        popupCancels.add(Unit)
    }
}
