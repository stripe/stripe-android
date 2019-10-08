package com.stripe.android

import com.stripe.android.model.SetupIntentFixtures
import kotlin.test.Test
import kotlin.test.assertEquals

class SetupIntentResultTest {

    @Test
    fun testBuilder() {
        assertEquals(
            SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT,
            SetupIntentResult(SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT).intent
        )
    }
}
