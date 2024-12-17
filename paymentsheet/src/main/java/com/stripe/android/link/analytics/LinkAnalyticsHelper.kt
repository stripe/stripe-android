package com.stripe.android.link.analytics

import com.stripe.android.link.LinkActivityResult
import javax.inject.Inject

internal class LinkAnalyticsHelper @Inject internal constructor(
    private val linkEventsReporter: LinkEventsReporter,
) {
    fun onLinkLaunched() {
        linkEventsReporter.onPopupShow()
    }

    fun onLinkResult(linkActivityResult: LinkActivityResult) {
        when (linkActivityResult) {
            is LinkActivityResult.Canceled -> {
                when (linkActivityResult.reason) {
                    LinkActivityResult.Canceled.Reason.BackPressed -> {
                        linkEventsReporter.onPopupCancel()
                    }

                    LinkActivityResult.Canceled.Reason.LoggedOut -> {
                        linkEventsReporter.onPopupLogout()
                    }
                    LinkActivityResult.Canceled.Reason.PayAnotherWay -> Unit
                }
            }

            is LinkActivityResult.Completed -> {
                linkEventsReporter.onPopupSuccess()
            }

            is LinkActivityResult.Failed -> {
                linkEventsReporter.onPopupError(linkActivityResult.error)
            }
        }
    }

    fun onLinkPopupSkipped() {
        linkEventsReporter.onPopupSkipped()
    }
}
