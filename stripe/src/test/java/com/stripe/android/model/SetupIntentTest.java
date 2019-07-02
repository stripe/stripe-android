package com.stripe.android.model;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.stripe.android.model.SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT_JSON;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class SetupIntentTest {

    @Test
    public void parseIdFromClientSecret_correctIdParsed() {
        final String id = SetupIntent.parseIdFromClientSecret(
                "seti_1Eq5kyGMT9dGPIDGxiSp4cce_secret_FKlHb3yTI0YZWe4iqghS8ZXqwwMoMmy");
        assertEquals("seti_1Eq5kyGMT9dGPIDGxiSp4cce", id);
    }

    @Test
    public void fromJsonStringWithNextAction_createsSetupIntentWithNextAction() {
        final SetupIntent setupIntent = SetupIntent.fromString(SI_NEXT_ACTION_REDIRECT_JSON);
        assertNotNull(setupIntent);
        assertEquals("seti_1EqTSZGMT9dGPIDGVzCUs6dV", setupIntent.getId());
        assertEquals("seti_1EqTSZGMT9dGPIDGVzCUs6dV_secret_FL9mS9ILygVyGEOSmVNqHT83rxkqy0Y",
                setupIntent.getClientSecret());
        assertEquals(1561677666, (long)setupIntent.getCreated());
        assertEquals("a description", setupIntent.getDescription());
        assertEquals("pm_1EqTSoGMT9dGPIDG7dgafX1H", setupIntent.getPaymentMethodId());
        assertFalse(setupIntent.getLiveMode());
        assertTrue(setupIntent.requiresAction());
        assertEquals(StripeIntent.Status.RequiresAction, setupIntent.getStatus());
        assertEquals(StripeIntent.Usage.OffSession, setupIntent.getUsage());

        final StripeIntent.RedirectData redirectData = setupIntent.getRedirectData();
        assertNotNull(redirectData);
        assertNotNull(redirectData.returnUrl);
        assertNotNull(setupIntent.getRedirectUrl());
        assertEquals("stripe://setup_intent_return", redirectData.returnUrl.toString());
        assertEquals("https://hooks.stripe.com/redirect/authenticate/src_1EqTStGMT9dGPIDGJGPkqE6B" +
                "?client_secret=src_client_secret_FL9m741mmxtHykDlRTC5aQ02", redirectData.url.toString());
        assertEquals("https://hooks.stripe.com/redirect/authenticate/src_1EqTStGMT9dGPIDGJGPkqE6B" +
                "?client_secret=src_client_secret_FL9m741mmxtHykDlRTC5aQ02",
                setupIntent.getRedirectUrl().toString());
    }

}
