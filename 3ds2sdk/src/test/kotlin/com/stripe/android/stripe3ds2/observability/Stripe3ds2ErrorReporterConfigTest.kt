package com.stripe.android.stripe3ds2.observability

import com.google.common.truth.Truth.assertThat
import com.stripe.android.stripe3ds2.ChallengeMessageFixtures
import kotlin.test.Test

class Stripe3ds2ErrorReporterConfigTest {

    @Test
    fun `customTags should return expected value`() {
        assertThat(
            Stripe3ds2ErrorReporterConfig(ChallengeMessageFixtures.SDK_TRANS_ID).customTags
        ).isEqualTo(
            mapOf("sdk_transaction_id" to ChallengeMessageFixtures.SDK_TRANS_ID.value)
        )
    }
}
