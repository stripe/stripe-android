package com.stripe.android

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.utils.ParcelUtils
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class SetupIntentResultTest {

    @Test
    fun `intent should return expected object`() {
        assertThat(SetupIntentResult(SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT).intent)
            .isEqualTo(SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT)
    }

    @Test
    fun `should parcelize correctly`() {
        ParcelUtils.verifyParcelRoundtrip(
            SetupIntentResult(
                intent = SetupIntentFixtures.SI_WITH_LAST_PAYMENT_ERROR,
                outcomeFromFlow = StripeIntentResult.Outcome.CANCELED
            )
        )
    }
}
