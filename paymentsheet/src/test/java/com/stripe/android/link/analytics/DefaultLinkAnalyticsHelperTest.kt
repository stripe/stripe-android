package com.stripe.android.link.analytics

import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.model.PaymentMethod
import org.junit.Test

internal class DefaultLinkAnalyticsHelperTest {
    @Test
    fun `test onLinkLaunched calls onPopupShow`() {
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
    fun `test onLinkResult with PaymentMethodObtained calls onPopupSuccess`() {
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
    fun `test onLinkResult with Completed calls onPopupSuccess`() {
        val eventReporter = object : FakeLinkEventsReporter() {
            override fun onPopupSuccess() {
                calledCount++
            }
        }
        val analyticsHelper = DefaultLinkAnalyticsHelper(eventReporter)
        analyticsHelper.onLinkResult(LinkActivityResult.Completed(LinkAccountUpdate.None))
        assertThat(eventReporter.calledCount).isEqualTo(1)
    }

    @Test
    fun `test onLinkResult with Failed calls onPopupError`() {
        val eventReporter = object : FakeLinkEventsReporter() {
            override fun onPopupError(error: Throwable) {
                calledCount++
            }
        }
        val analyticsHelper = DefaultLinkAnalyticsHelper(eventReporter)
        analyticsHelper.onLinkResult(
            linkActivityResult = LinkActivityResult.Failed(
                error = IllegalStateException(),
                linkAccountUpdate = LinkAccountUpdate.None
            )
        )
        assertThat(eventReporter.calledCount).isEqualTo(1)
    }

    @Test
    fun `test onLinkResult with CanceledBackPressed calls onPopupCancel`() {
        val eventReporter = object : FakeLinkEventsReporter() {
            override fun onPopupCancel() {
                calledCount++
            }
        }
        val analyticsHelper = DefaultLinkAnalyticsHelper(eventReporter)
        analyticsHelper.onLinkResult(
            linkActivityResult = LinkActivityResult.Canceled(
                reason = LinkActivityResult.Canceled.Reason.BackPressed,
                linkAccountUpdate = LinkAccountUpdate.None
            )
        )
        assertThat(eventReporter.calledCount).isEqualTo(1)
    }

    @Test
    fun `test onLinkResult with CanceledLoggedOut calls onPopupLogout`() {
        val eventReporter = object : FakeLinkEventsReporter() {
            override fun onPopupLogout() {
                calledCount++
            }
        }
        val analyticsHelper = DefaultLinkAnalyticsHelper(eventReporter)
        analyticsHelper.onLinkResult(
            linkActivityResult = LinkActivityResult.Canceled(
                reason = LinkActivityResult.Canceled.Reason.LoggedOut,
                linkAccountUpdate = LinkAccountUpdate.None
            )
        )
        assertThat(eventReporter.calledCount).isEqualTo(1)
    }

    @Test
    fun `test onLinkPopupSkipped calls onPopupSkipped`() {
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
