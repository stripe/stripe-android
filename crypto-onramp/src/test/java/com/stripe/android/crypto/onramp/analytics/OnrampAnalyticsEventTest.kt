package com.stripe.android.crypto.onramp.analytics

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.exception.APIException
import org.junit.Test

class OnrampAnalyticsEventTest {
    @Test
    fun `error occurred includes request ID when available`() {
        val event = OnrampAnalyticsEvent.ErrorOccurred(
            operation = OnrampAnalyticsEvent.ErrorOccurred.Operation.HasLinkAccount,
            error = APIException(requestId = "req_123"),
        )

        assertThat(event.params?.get("operation_name")).isEqualTo("has_link_account")
        assertThat(event.params?.get("error_message")).isEqualTo("apiError")
        assertThat(event.params?.get("request_id")).isEqualTo("req_123")
    }

    @Test
    fun `error occurred omits request ID when unavailable`() {
        val event = OnrampAnalyticsEvent.ErrorOccurred(
            operation = OnrampAnalyticsEvent.ErrorOccurred.Operation.HasLinkAccount,
            error = APIException(),
        )

        assertThat(event.params).doesNotContainKey("request_id")
    }
}
