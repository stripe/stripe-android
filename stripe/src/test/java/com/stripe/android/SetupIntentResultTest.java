package com.stripe.android;

import com.stripe.android.model.SetupIntent;
import com.stripe.android.model.SetupIntentFixtures;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SetupIntentResultTest {

    @Test
    public void testBuilder() {
        assertEquals(SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT,
                new SetupIntentResult.Builder()
                        .setSetupIntent(SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT)
                        .build()
                        .getIntent());
    }
}
