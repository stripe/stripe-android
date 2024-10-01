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

private open class FakeLinkEventsReporter : LinkEventsReporter {
    var calledCount = 0
    override fun onInvalidSessionState(state: LinkEventsReporter.SessionState) {
        throw NotImplementedError()
    }

    override fun onInlineSignupCheckboxChecked() {
        throw NotImplementedError()
    }

    override fun onSignupFlowPresented() {
        throw NotImplementedError()
    }

    override fun onSignupStarted(isInline: Boolean) {
        throw NotImplementedError()
    }

    override fun onSignupCompleted(isInline: Boolean) {
        throw NotImplementedError()
    }

    override fun onSignupFailure(isInline: Boolean, error: Throwable) {
        throw NotImplementedError()
    }

    override fun onAccountLookupFailure(error: Throwable) {
        throw NotImplementedError()
    }

    override fun onPopupShow() {
        throw NotImplementedError()
    }

    override fun onPopupSuccess() {
        throw NotImplementedError()
    }

    override fun onPopupCancel() {
        throw NotImplementedError()
    }

    override fun onPopupError(error: Throwable) {
        throw NotImplementedError()
    }

    override fun onPopupLogout() {
        throw NotImplementedError()
    }

    override fun onPopupSkipped() {
        throw NotImplementedError()
    }
}
