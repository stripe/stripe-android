package com.stripe.android

import com.stripe.android.model.SetupIntentFixtures
import kotlin.test.Test
import kotlin.test.assertEquals

class SetupIntentResultTest {

    @Test
    fun testBuilder() {
        assertEquals(SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT,
            SetupIntentResult.Builder()
                .setSetupIntent(SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT)
                .build()
                .intent)
    }
}
