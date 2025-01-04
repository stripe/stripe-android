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
import com.stripe.android.utils.RecordingLinkStore
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class LinkPaymentLauncherTest {

    @Test
    fun `register with ActivityResultRegistry should set up correct launcher`() {
        val linkActivityContract: LinkActivityContract = mock()
        val activityResultRegistry: ActivityResultRegistry = mock()
        val activityResultLauncher: ActivityResultLauncher<LinkActivityContract.Args> = mock()

        val linkPaymentLauncher = createLinkPaymentLauncher(linkActivityContract = linkActivityContract)

        whenever(
            activityResultRegistry.register(
                eq("LinkPaymentLauncher"),
                any<ActivityResultContract<LinkActivityContract.Args, LinkActivityResult>>(),
                any()
            )
        ).thenReturn(activityResultLauncher)

        linkPaymentLauncher.register(activityResultRegistry) {}

        verify(activityResultRegistry).register(
            eq("LinkPaymentLauncher"),
            eq(linkActivityContract),
            any()
        )
    }

    @Test
    fun `register with ActivityResultCaller should set up correct launcher`() {
        val linkActivityContract: LinkActivityContract = mock()
        val activityResultCaller: ActivityResultCaller = mock()
        val activityResultLauncher: ActivityResultLauncher<LinkActivityContract.Args> = mock()

        val linkPaymentLauncher = createLinkPaymentLauncher(linkActivityContract = linkActivityContract)

        whenever(
            activityResultCaller.registerForActivityResult(
                eq(linkActivityContract),
                any()
            )
        ).thenReturn(activityResultLauncher)

        linkPaymentLauncher.register(activityResultCaller) {}

        verify(activityResultCaller).registerForActivityResult(
            eq(linkActivityContract),
            any()
        )
    }

    @Test
    fun `unregister should call unregister on ActivityResultLauncher`() {
        val linkActivityContract: LinkActivityContract = mock()
        val activityResultCaller: ActivityResultCaller = mock()
        val activityResultLauncher: ActivityResultLauncher<LinkActivityContract.Args> = mock()

        val linkPaymentLauncher = createLinkPaymentLauncher(linkActivityContract = linkActivityContract)

        whenever(
            activityResultCaller.registerForActivityResult(
                eq(linkActivityContract),
                any()
            )
        ).thenReturn(activityResultLauncher)

        linkPaymentLauncher.register(activityResultCaller) {}

        linkPaymentLauncher.unregister()

        verify(activityResultLauncher).unregister()
    }

    @Test
    fun `present should launch with correct args and trigger analytics`() {
        val linkAnalyticsHelper = object : LauncherLinkAnalyticsHelper() {
            var callCount = 0
            override fun onLinkLaunched() {
                callCount += 1
            }
        }
        val activityResultCaller: ActivityResultCaller = mock()
        val activityResultLauncher: ActivityResultLauncher<LinkActivityContract.Args> = mock()

        val linkPaymentLauncher = createLinkPaymentLauncher(linkAnalyticsHelper = linkAnalyticsHelper)

        whenever(
            activityResultCaller.registerForActivityResult(
                any<LinkActivityContract>(),
                any()
            )
        ).thenReturn(activityResultLauncher)

        linkPaymentLauncher.register(activityResultCaller) {}

        linkPaymentLauncher.present(TestFactory.LINK_CONFIGURATION)

        verify(activityResultLauncher).launch(
            LinkActivityContract.Args(TestFactory.LINK_CONFIGURATION)
        )
        assertThat(linkAnalyticsHelper.callCount).isEqualTo(1)
    }

    @Test
    fun `ActivityResultRegistry callback should handle completed result correctly`() = runTest {
        RecordingLinkStore.test {
            val activityResultRegistry: ActivityResultRegistry = mock()
            val linkAnalyticsHelper = TrackingLinkAnalyticsHelper()

            val linkPaymentLauncher = createLinkPaymentLauncher(linkStore = linkStore, linkAnalyticsHelper = linkAnalyticsHelper)

            whenever(
                activityResultRegistry.register(
                    any(),
                    any<ActivityResultContract<LinkActivityContract.Args, LinkActivityResult>>(),
                    any()
                )
            ).thenAnswer { invocation ->
                val callback = invocation.getArgument<ActivityResultCallback<LinkActivityResult>>(2)
                callback.onActivityResult(LinkActivityResult.Completed)
                mock<ActivityResultLauncher<LinkActivityContract.Args>>()
            }

            var callbackParam: LinkActivityResult? = null
            linkPaymentLauncher.register(activityResultRegistry) {
                callbackParam = it
            }

            verify(linkStore).markLinkAsUsed()
            assertThat(callbackParam).isEqualTo(LinkActivityResult.Completed)
            assertThat(markAsUsedCalls.cancelAndConsumeRemainingEvents().size).isEqualTo(1)
            assertThat(linkAnalyticsHelper.lastResult).isEqualTo(LinkActivityResult.Completed)
        }
    }

    @Test
    fun `ActivityResultCaller callback should handle PaymentMethodObtained result correctly`() = runTest {
        RecordingLinkStore.test {
            val activityResultCaller: ActivityResultCaller = mock()
            val linkAnalyticsHelper = TrackingLinkAnalyticsHelper()

            val linkPaymentLauncher = createLinkPaymentLauncher(linkStore = linkStore, linkAnalyticsHelper = linkAnalyticsHelper)

            whenever(
                activityResultCaller.registerForActivityResult(
                    any<LinkActivityContract>(),
                    any()
                )
            ).thenAnswer { invocation ->
                val callback = invocation.getArgument<ActivityResultCallback<LinkActivityResult>>(1)
                callback.onActivityResult(LinkActivityResult.PaymentMethodObtained(mock()))
                mock<ActivityResultLauncher<LinkActivityContract.Args>>()
            }

            var callbackParam: LinkActivityResult? = null
            linkPaymentLauncher.register(activityResultCaller) {
                callbackParam = it
            }

            verify(linkStore).markLinkAsUsed()
            assertThat(callbackParam).isInstanceOf(LinkActivityResult.PaymentMethodObtained::class.java)
            assertThat(markAsUsedCalls.cancelAndConsumeRemainingEvents().size).isEqualTo(1)
            assertThat(linkAnalyticsHelper.lastResult).isInstanceOf(LinkActivityResult.PaymentMethodObtained::class.java)
        }
    }

    @Test
    fun `ActivityResultRegistry callback should handle Canceled result correctly`() = runTest {
        RecordingLinkStore.test {
            val activityResultRegistry: ActivityResultRegistry = mock()
            val linkAnalyticsHelper = TrackingLinkAnalyticsHelper()

            val linkPaymentLauncher = createLinkPaymentLauncher(linkStore = linkStore, linkAnalyticsHelper = linkAnalyticsHelper)

            whenever(
                activityResultRegistry.register(
                    any(),
                    any<ActivityResultContract<LinkActivityContract.Args, LinkActivityResult>>(),
                    any()
                )
            ).thenAnswer { invocation ->
                val callback = invocation.getArgument<ActivityResultCallback<LinkActivityResult>>(2)
                callback.onActivityResult(LinkActivityResult.Canceled())
                mock<ActivityResultLauncher<LinkActivityContract.Args>>()
            }

            var callbackParam: LinkActivityResult? = null
            linkPaymentLauncher.register(activityResultRegistry) {
                callbackParam = it
            }

            verify(linkStore, never()).markLinkAsUsed()
            assertThat(callbackParam).isInstanceOf(LinkActivityResult.Canceled::class.java)
            assertThat(markAsUsedCalls.cancelAndConsumeRemainingEvents()).isEmpty()
            assertThat(linkAnalyticsHelper.lastResult).isInstanceOf(LinkActivityResult.Canceled::class.java)
        }
    }

    @Test
    fun `ActivityResultCaller callback should handle Failed result correctly`() = runTest {
        RecordingLinkStore.test {
            val activityResultCaller: ActivityResultCaller = mock()
            val linkAnalyticsHelper = TrackingLinkAnalyticsHelper()

            val linkPaymentLauncher = createLinkPaymentLauncher(linkStore = linkStore, linkAnalyticsHelper = linkAnalyticsHelper)

            whenever(
                activityResultCaller.registerForActivityResult(
                    any<LinkActivityContract>(),
                    any()
                )
            ).thenAnswer { invocation ->
                val callback = invocation.getArgument<ActivityResultCallback<LinkActivityResult>>(1)
                callback.onActivityResult(LinkActivityResult.Failed(Exception("Test exception")))
                mock<ActivityResultLauncher<LinkActivityContract.Args>>()
            }

            var callbackParam: LinkActivityResult? = null
            linkPaymentLauncher.register(activityResultCaller) {
                callbackParam = it
            }

            verify(linkStore, never()).markLinkAsUsed()
            assertThat(callbackParam).isInstanceOf(LinkActivityResult.Failed::class.java)
            assertThat(markAsUsedCalls.cancelAndConsumeRemainingEvents()).isEmpty()
            assertThat(linkAnalyticsHelper.lastResult).isInstanceOf(LinkActivityResult.Failed::class.java)
        }
    }

    private fun createLinkPaymentLauncher(
        linkActivityContract: LinkActivityContract = mock(),
        linkAnalyticsHelper: LinkAnalyticsHelper = LauncherLinkAnalyticsHelper(),
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

private open class LauncherLinkAnalyticsHelper : FakeLinkAnalyticsHelper() {
    override fun onLinkLaunched() = Unit
}

private class TrackingLinkAnalyticsHelper : LauncherLinkAnalyticsHelper() {
    var lastResult: LinkActivityResult? = null
    override fun onLinkResult(linkResult: LinkActivityResult) {
        lastResult = linkResult
    }
}
