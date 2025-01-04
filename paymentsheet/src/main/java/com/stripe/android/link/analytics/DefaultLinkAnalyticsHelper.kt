package com.stripe.android.link.analytics

import com.stripe.android.link.LinkActivityResult
import javax.inject.Inject

internal class DefaultLinkAnalyticsHelper @Inject internal constructor(
    private val linkEventsReporter: LinkEventsReporter,
) : LinkAnalyticsHelper {
    override fun onLinkLaunched() {
        linkEventsReporter.onPopupShow()
    }

    override fun onLinkResult(linkActivityResult: LinkActivityResult) {
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

            is LinkActivityResult.PaymentMethodObtained -> {
                linkEventsReporter.onPopupSuccess()
            }

            is LinkActivityResult.Failed -> {
                linkEventsReporter.onPopupError(linkActivityResult.error)
            }
            LinkActivityResult.Completed -> Unit
        }
    }

    override fun onLinkPopupSkipped() {
        linkEventsReporter.onPopupSkipped()
    }
}
