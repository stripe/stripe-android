package com.stripe.android.model;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ConfirmSetupIntentParamsTest {

    @Test
    public void shouldUseStripeSdk() {
        final ConfirmSetupIntentParams confirmSetupIntentParams =
                ConfirmSetupIntentParams.create(
                        "pm_123", "client_secret", "return_url");
        assertFalse(confirmSetupIntentParams.shouldUseStripeSdk());

        assertTrue(confirmSetupIntentParams
                .withShouldUseStripeSdk(true)
                .shouldUseStripeSdk());
    }
}
