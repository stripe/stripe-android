package com.stripe.android.link

import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.account.LinkStore
import com.stripe.android.link.analytics.FakeLinkAnalyticsHelper
import com.stripe.android.link.analytics.LinkAnalyticsHelper
import com.stripe.android.link.injection.LinkAnalyticsComponent
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.utils.DummyActivityResultCaller
import com.stripe.android.utils.FakeActivityResultRegistry
import com.stripe.android.utils.RecordingLinkStore
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

internal class LinkPaymentLauncherTest {

    @Test
    fun `register call should register ActivityResultLauncher with ActivityResultRegistry`() {
        val linkActivityContract: LinkActivityContract = mock()
        val activityResultRegistry: ActivityResultRegistry = mock()
        val activityResultLauncher: ActivityResultLauncher<LinkActivityContract.Args> = mock()

        val linkPaymentLauncher = createLinkPaymentLauncher(linkActivityContract = linkActivityContract)

        setupActivityResultRegistryMock(activityResultRegistry, activityResultLauncher)

        linkPaymentLauncher.register(activityResultRegistry) {}

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

            val unregisterCall = awaitUnregisterCall()
            assertThat(unregisterCall).isNotNull()

            awaitNextRegisteredLauncher()
        }
    }

    @Test
    fun `present should launch with correct args and trigger analytics`() = runTest {
        DummyActivityResultCaller.test {
            val linkPaymentLauncher = createLinkPaymentLauncher()

            linkPaymentLauncher.register(activityResultCaller) {}

            val registerCall = awaitRegisterCall()
            assertThat(registerCall).isNotNull()

            linkPaymentLauncher.present(TestFactory.LINK_CONFIGURATION)

            val launchCall = awaitLaunchCall()

            assertThat(launchCall).isEqualTo(LinkActivityContract.Args(TestFactory.LINK_CONFIGURATION))

            awaitNextRegisteredLauncher()
        }
    }

    @Test
    fun `ActivityResultRegistry callback should handle Completed result correctly`() {
        testActivityResultCallbackWithActivityResultRegistry(
            linkActivityResult = LinkActivityResult.Completed,
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
            linkActivityResult = LinkActivityResult.Canceled(),
            expectedMarkAsUsedCalls = 0,
        )
    }

    @Test
    fun `ActivityResultRegistry callback should handle Failed result correctly`() {
        testActivityResultCallbackWithActivityResultRegistry(
            linkActivityResult = LinkActivityResult.Failed(Exception("oops")),
            expectedMarkAsUsedCalls = 0,
        )
    }

    @Test
    fun `ActivityResultCaller callback should handle Completed result correctly`() {
        testActivityResultCallbackWithResultCaller(
            linkActivityResult = LinkActivityResult.Completed,
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
            linkActivityResult = LinkActivityResult.Canceled(),
            expectedMarkAsUsedCalls = 0,
        )
    }

    @Test
    fun `ActivityResultCaller callback should handle Failed result correctly`() {
        testActivityResultCallbackWithResultCaller(
            linkActivityResult = LinkActivityResult.Failed(Exception("oops")),
            expectedMarkAsUsedCalls = 0,
        )
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
            linkPaymentLauncher.register(activityResultRegistry) { callbackParam = it }

            linkPaymentLauncher.present(TestFactory.LINK_CONFIGURATION)

            verifyActivityResultCallback(
                linkActivityResult = linkActivityResult,
                linkStore = linkStore,
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
            val activityResultCaller: ActivityResultCaller = mock()
            val activityResultLauncher: ActivityResultLauncher<LinkActivityContract.Args> = mock()

            val linkAnalyticsHelper = TrackingLinkAnalyticsHelper()
            val linkPaymentLauncher = createLinkPaymentLauncher(
                linkAnalyticsHelper = linkAnalyticsHelper,
                linkStore = linkStore
            )

            setupActivityResultCallerMock(
                activityResultCaller = activityResultCaller,
                linkActivityContractProvider = { any() },
                activityResultLauncher = activityResultLauncher
            )

            setupActivityResultCallerCallback(
                activityResultCaller = activityResultCaller,
                linkActivityResult = linkActivityResult
            )

            var callbackParam: LinkActivityResult? = null
            linkPaymentLauncher.register(activityResultCaller) { callbackParam = it }

            linkPaymentLauncher.present(TestFactory.LINK_CONFIGURATION)

            verifyActivityResultCallback(
                linkActivityResult = linkActivityResult,
                linkStore = linkStore,
                linkAnalyticsHelper = linkAnalyticsHelper,
                expectedMarkAsUsedCalls = expectedMarkAsUsedCalls,
                callbackResult = callbackParam,
            )
        }
    }

    private suspend fun RecordingLinkStore.Scenario.verifyActivityResultCallback(
        linkActivityResult: LinkActivityResult,
        linkStore: LinkStore,
        linkAnalyticsHelper: TrackingLinkAnalyticsHelper,
        expectedMarkAsUsedCalls: Int,
        callbackResult: LinkActivityResult?,
    ) {
        if (expectedMarkAsUsedCalls > 0) {
            verify(linkStore, times(expectedMarkAsUsedCalls)).markLinkAsUsed()
        } else {
            verify(linkStore, never()).markLinkAsUsed()
        }

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

    private fun setupActivityResultCallerMock(
        activityResultCaller: ActivityResultCaller,
        activityResultLauncher: ActivityResultLauncher<LinkActivityContract.Args>,
        linkActivityContractProvider: () -> LinkActivityContract
    ) {
        whenever(
            activityResultCaller.registerForActivityResult(
                linkActivityContractProvider(),
                any()
            )
        ).thenReturn(activityResultLauncher)
    }

    private fun verifyActivityResultRegistryRegister(
        activityResultRegistry: ActivityResultRegistry,
        linkActivityContract: LinkActivityContract
    ) {
        verify(activityResultRegistry).register(
            eq("LinkPaymentLauncher"),
            eq(linkActivityContract),
            any()
        )
    }

    private fun setupActivityResultCallerCallback(
        activityResultCaller: ActivityResultCaller,
        linkActivityResult: LinkActivityResult
    ) {
        whenever(
            activityResultCaller.registerForActivityResult(
                any<ActivityResultContract<LinkActivityContract.Args, LinkActivityResult>>(),
                any()
            )
        ).thenAnswer { invocation ->
            val callback = invocation.getArgument<ActivityResultCallback<LinkActivityResult>>(1)
            callback.onActivityResult(linkActivityResult)
            mock<ActivityResultLauncher<LinkActivityContract.Args>>()
        }
    }

    private fun createLinkPaymentLauncher(
        linkActivityContract: LinkActivityContract = mock(),
        linkAnalyticsHelper: LinkAnalyticsHelper = TrackingLinkAnalyticsHelper(),
        linkStore: LinkStore = mock()
    ): LinkPaymentLauncher {
        return LinkPaymentLauncher(
            linkAnalyticsComponentBuilder = object : LinkAnalyticsComponent.Builder {
                override fun build() = object : LinkAnalyticsComponent {
                    override val linkAnalyticsHelper = linkAnalyticsHelper
                }
            },
            linkActivityContract = linkActivityContract,
            linkStore = linkStore
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
