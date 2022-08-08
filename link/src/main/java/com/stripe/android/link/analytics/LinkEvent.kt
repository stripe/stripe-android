package com.stripe.android.link.analytics

import com.stripe.android.core.networking.AnalyticsEvent

internal sealed class LinkEvent : AnalyticsEvent {

    object SignUpCheckboxChecked : LinkEvent() {
        override val eventName = "link.signup.checkbox_checked"
    }

    object SignUpFlowPresented : LinkEvent() {
        override val eventName = "link.signup.flow_presented"
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

    object TwoFAStart : LinkEvent() {
        override val eventName = "link.2fa.start"
    }

    object TwoFAStartFailure : LinkEvent() {
        override val eventName = "link.2fa.start_failure"
    }

    object TwoFAComplete : LinkEvent() {
        override val eventName = "link.2fa.complete"
    }

    object TwoFAFailure : LinkEvent() {
        override val eventName = "link.2fa.failure"
    }

    object TwoFACancel : LinkEvent() {
        override val eventName = "link.2fa.cancel"
    }
}
