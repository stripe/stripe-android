package com.stripe.android.ui.core.forms.resources

import com.stripe.android.core.networking.AnalyticsEvent

class LpmSerializeFailureEvent : AnalyticsEvent {
    override val eventName: String = "luxe_serialize_failure"
}
