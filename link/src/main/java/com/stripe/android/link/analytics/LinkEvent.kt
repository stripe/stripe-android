package com.stripe.android.link.analytics

import com.stripe.android.core.networking.AnalyticsEvent

internal sealed class LinkEvent : AnalyticsEvent {

    object SignUpCheckboxChecked : LinkEvent() {
        override val eventName = "link.signup.checkbox_checked"
    }

    object SignUpStart : LinkEvent() {
        override val eventName = "link.signup.start"
    }

    object SignUpComplete : LinkEvent() {
        override val eventName = "link.signup.complete"
    }

    object SignUpFailure : LinkEvent() {
        override val eventName = "link.signup.failure"
    }

    object AccountLookupFailure : LinkEvent() {
        override val eventName = "link.account_lookup.failure"
    }

    object PopupShow : LinkEvent() {
        override val eventName = "link_popup_show"
    }

    object PopupSuccess : LinkEvent() {
        override val eventName = "link_popup_success"
    }

    object PopupCancel : LinkEvent() {
        override val eventName = "link_popup_cancel"
    }

    object PopupError : LinkEvent() {
        override val eventName = "link_popup_error"
    }

    object PopupLogout : LinkEvent() {
        override val eventName = "link_popup_logout"
    }

    object PopupSkipped : LinkEvent() {
        override val eventName = "link_popup_skipped"
    }
}
