package com.stripe.android.link.analytics

import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.model.PaymentMethod
import org.junit.Test

internal class DefaultLinkAnalyticsHelperTest {
    @Test
    fun testOnLinkLaunchedCalls_onPopupShow() {
        val eventReporter = object : FakeLinkEventsReporter() {
            override fun onPopupShow() {
                calledCount++
            }
        }
        val analyticsHelper = DefaultLinkAnalyticsHelper(eventReporter)
        analyticsHelper.onLinkLaunched()
        assertThat(eventReporter.calledCount).isEqualTo(1)
    }

    @Test
    fun testOnLinkResultCalls_onPopupSuccess() {
        val eventReporter = object : FakeLinkEventsReporter() {
            override fun onPopupSuccess() {
                calledCount++
            }
        }
        val analyticsHelper = DefaultLinkAnalyticsHelper(eventReporter)
        analyticsHelper.onLinkResult(
            linkActivityResult = LinkActivityResult.PaymentMethodObtained(
                paymentMethod = PaymentMethod(
                    id = null,
                    created = null,
                    liveMode = false,
                    code = null,
                    type = null
                )
            )
        )
        assertThat(eventReporter.calledCount).isEqualTo(1)
    }

    @Test
    fun testOnLinkResultCalls_onPopupError() {
        val eventReporter = object : FakeLinkEventsReporter() {
            override fun onPopupError(error: Throwable) {
                calledCount++
            }
        }
        val analyticsHelper = DefaultLinkAnalyticsHelper(eventReporter)
        analyticsHelper.onLinkResult(LinkActivityResult.Failed(IllegalStateException()))
        assertThat(eventReporter.calledCount).isEqualTo(1)
    }

    @Test
    fun testOnLinkResultCalls_onPopupCancel() {
        val eventReporter = object : FakeLinkEventsReporter() {
            override fun onPopupCancel() {
                calledCount++
            }
        }
        val analyticsHelper = DefaultLinkAnalyticsHelper(eventReporter)
        analyticsHelper.onLinkResult(LinkActivityResult.Canceled(LinkActivityResult.Canceled.Reason.BackPressed))
        assertThat(eventReporter.calledCount).isEqualTo(1)
    }

    @Test
    fun testOnLinkResultCalls_onPopupLogout() {
        val eventReporter = object : FakeLinkEventsReporter() {
            override fun onPopupLogout() {
                calledCount++
            }
        }
        val analyticsHelper = DefaultLinkAnalyticsHelper(eventReporter)
        analyticsHelper.onLinkResult(LinkActivityResult.Canceled(LinkActivityResult.Canceled.Reason.LoggedOut))
        assertThat(eventReporter.calledCount).isEqualTo(1)
    }

    @Test
    fun testOnLinkSkippedCalls_onPopupSkipped() {
        val eventReporter = object : FakeLinkEventsReporter() {
            override fun onPopupSkipped() {
                calledCount++
            }
        }
        val analyticsHelper = DefaultLinkAnalyticsHelper(eventReporter)
        analyticsHelper.onLinkPopupSkipped()
        assertThat(eventReporter.calledCount).isEqualTo(1)
    }
}
