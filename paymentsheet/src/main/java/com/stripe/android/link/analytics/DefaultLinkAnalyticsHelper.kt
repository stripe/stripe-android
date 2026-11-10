package com.stripe.android.link.analytics

import com.stripe.android.link.LinkActivityResult
import javax.inject.Inject

internal class DefaultLinkAnalyticsHelper @Inject internal constructor(
    private val linkEventsReporter: LinkEventsReporter,
) : LinkAnalyticsHelper {
    override fun onLinkLaunched(checkoutSessionId: String?) {
        linkEventsReporter.onPopupShow(checkoutSessionId)
    }

    override fun onLinkResult(linkActivityResult: LinkActivityResult, checkoutSessionId: String?) {
        when (linkActivityResult) {
            is LinkActivityResult.Canceled -> {
                when (linkActivityResult.reason) {
                    LinkActivityResult.Canceled.Reason.BackPressed -> {
                        linkEventsReporter.onPopupCancel(checkoutSessionId)
                    }

                    LinkActivityResult.Canceled.Reason.LoggedOut -> {
                        linkEventsReporter.onPopupLogout(checkoutSessionId)
                    }
                    LinkActivityResult.Canceled.Reason.PayAnotherWay -> Unit
                }
            }

            is LinkActivityResult.PaymentMethodObtained,
            is LinkActivityResult.Completed -> {
                linkEventsReporter.onPopupSuccess(checkoutSessionId)
            }

            is LinkActivityResult.Failed -> {
                linkEventsReporter.onPopupError(linkActivityResult.error, checkoutSessionId)
            }
        }
    }

    override fun onLinkPopupSkipped(checkoutSessionId: String?) {
        linkEventsReporter.onPopupSkipped(checkoutSessionId)
    }
}
