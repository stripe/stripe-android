package com.stripe.android

import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.utils.ParcelUtils
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SetupIntentResultTest {

    @Test
    fun testBuilder() {
        assertEquals(
            SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT,
            SetupIntentResult(SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT).intent
        )
    }

    @Test
    fun shouldImplementParcelableCorrectly() {
        ParcelUtils.verifyParcelRoundtrip(
            SetupIntentResult(
                setupIntent = SetupIntentFixtures.SI_WITH_LAST_PAYMENT_ERROR,
                outcome = StripeIntentResult.Outcome.CANCELED
            )
        )
    }
}
