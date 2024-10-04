package com.stripe.android.link.analytics

import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.LinkActivityResult
import org.junit.Test
import org.mockito.Mockito.mock

internal class LinkAnalyticsHelperTest {
    @Test
    fun testOnLinkLaunchedCalls_onPopupShow() {
        val eventReporter = object : FakeLinkEventsReporter() {
            override fun onPopupShow() {
                calledCount++
            }
        }
        val analyticsHelper = LinkAnalyticsHelper(eventReporter)
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
        val analyticsHelper = LinkAnalyticsHelper(eventReporter)
        analyticsHelper.onLinkResult(LinkActivityResult.Completed(mock()))
        assertThat(eventReporter.calledCount).isEqualTo(1)
    }

    @Test
    fun testOnLinkResultCalls_onPopupError() {
        val eventReporter = object : FakeLinkEventsReporter() {
            override fun onPopupError(error: Throwable) {
                calledCount++
            }
        }
        val analyticsHelper = LinkAnalyticsHelper(eventReporter)
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
        val analyticsHelper = LinkAnalyticsHelper(eventReporter)
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
        val analyticsHelper = LinkAnalyticsHelper(eventReporter)
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
        val analyticsHelper = LinkAnalyticsHelper(eventReporter)
        analyticsHelper.onLinkPopupSkipped()
        assertThat(eventReporter.calledCount).isEqualTo(1)
    }
}

internal open class FakeLinkEventsReporter : LinkEventsReporter {
    var calledCount = 0
    override fun onInvalidSessionState(state: LinkEventsReporter.SessionState) = Unit

    override fun onInlineSignupCheckboxChecked() = Unit

    override fun onSignupFlowPresented() = Unit

    override fun onSignupStarted(isInline: Boolean) = Unit

    override fun onSignupCompleted(isInline: Boolean) = Unit

    override fun onSignupFailure(isInline: Boolean, error: Throwable) = Unit

    override fun onAccountLookupFailure(error: Throwable) = Unit

    override fun onPopupShow() = Unit

    override fun onPopupSuccess() = Unit

    override fun onPopupCancel() = Unit

    override fun onPopupError(error: Throwable) = Unit

    override fun onPopupLogout() = Unit

    override fun onPopupSkipped() = Unit
}
