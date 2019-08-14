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
        assertEquals(1561677666, setupIntent.getCreated());
        assertEquals("a description", setupIntent.getDescription());
        assertEquals("pm_1EqTSoGMT9dGPIDG7dgafX1H", setupIntent.getPaymentMethodId());
        assertFalse(setupIntent.isLiveMode());
        assertTrue(setupIntent.requiresAction());
        assertEquals(StripeIntent.Status.RequiresAction, setupIntent.getStatus());
        assertEquals(StripeIntent.Usage.OffSession, setupIntent.getUsage());

        final StripeIntent.RedirectData redirectData = setupIntent.getRedirectData();
        assertNotNull(redirectData);
        assertNotNull(redirectData.returnUrl);
        assertNotNull(setupIntent.getRedirectUrl());
        assertEquals("stripe://setup_intent_return", redirectData.returnUrl);
        assertEquals("https://hooks.stripe.com/redirect/authenticate/src_1EqTStGMT9dGPIDGJGPkqE6B" +
                "?client_secret=src_client_secret_FL9m741mmxtHykDlRTC5aQ02", redirectData.url.toString());
        assertEquals("https://hooks.stripe.com/redirect/authenticate/src_1EqTStGMT9dGPIDGJGPkqE6B" +
                "?client_secret=src_client_secret_FL9m741mmxtHykDlRTC5aQ02",
                setupIntent.getRedirectUrl().toString());
    }

    @Test
    public void getLastSetupError_parsesCorrectly() {
        final SetupIntent.Error lastSetupError = SetupIntentFixtures.SI_WITH_LAST_PAYMENT_ERROR.getLastSetupError();
        assertNotNull(lastSetupError);
        assertNotNull(lastSetupError.paymentMethod);
        assertEquals("pm_1F7J1bCRMbs6FrXfQKsYwO3U", lastSetupError.paymentMethod.id);
        assertEquals("payment_intent_authentication_failure", lastSetupError.code);
        assertEquals(SetupIntent.Error.Type.InvalidRequestError, lastSetupError.type);
        assertEquals(
                "https://stripe.com/docs/error-codes/payment-intent-authentication-failure",
                lastSetupError.docUrl
        );
        assertEquals(
                "The provided PaymentMethod has failed authentication. You can provide payment_method_data or a new PaymentMethod to attempt to fulfill this PaymentIntent again.",
                lastSetupError.message
        );
    }
}
