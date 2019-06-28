package com.stripe.android.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SetupIntentTest {

    private static final String SETUP_INTENT_NEXT_ACTION_REDIRECT = "{\n" +
            "  \"id\": \"seti_1Eq6tqGMT9dGPIDGhdilBKsu\",\n" +
            "  \"object\": \"setup_intent\",\n" +
            "  \"cancellation_reason\": null,\n" +
            "  \"client_secret\": \"seti_1Eq6tqGMT9dGPIDGhdilBKsu_secret_FKmSUxflgdu2gMlMLNzj89t90SRIZTn\",\n" +
            "  \"created\": 1561677666,\n" +
            "  \"description\": \"a description\",\n" +
            "  \"last_setup_error\": null,\n" +
            "  \"livemode\": false,\n" +
            "  \"next_action\": {\n" +
            "    \"type\": \"use_stripe_sdk\",\n" +
            "    \"use_stripe_sdk\": {\n" +
            "      \"type\": \"three_d_secure_redirect\",\n" +
            "      \"stripe_js\": \"https://hooks.stripe.com/redirect/authenticate/src_1Eq6tzGMT9dGPIDGL26pAgjJ?client_secret=src_client_secret_FKmSF6ewY3iTFmfdIT5784LI\"\n" +
            "    }\n" +
            "  },\n" +
            "  \"payment_method\": \"pm_1Eq6tlGMT9dGPIDGFkaLeB0v\",\n" +
            "  \"payment_method_types\": [\n" +
            "    \"card\"\n" +
            "  ],\n" +
            "  \"status\": \"requires_action\",\n" +
            "  \"usage\": \"off_session\"\n" +
            "}";


    @Test
    public void parseIdFromClientSecret_correctIdParsed() {
        final String id = SetupIntent.parseIdFromClientSecret(
                "seti_1Eq5kyGMT9dGPIDGxiSp4cce_secret_FKlHb3yTI0YZWe4iqghS8ZXqwwMoMmy");
        assertEquals("seti_1Eq5kyGMT9dGPIDGxiSp4cce", id);
    }

    @Test
    public void fromJsonStringWithNextAction_createsSetupIntentWithNextAction() {
        final SetupIntent setupIntent = SetupIntent.fromString(SETUP_INTENT_NEXT_ACTION_REDIRECT);
        assertNotNull(setupIntent);
        assertEquals("seti_1Eq6tqGMT9dGPIDGhdilBKsu", setupIntent.getId());
        assertEquals("seti_1Eq6tqGMT9dGPIDGhdilBKsu_secret_FKmSUxflgdu2gMlMLNzj89t90SRIZTn",
                setupIntent.getClientSecret());
        assertEquals(1561677666, (long)setupIntent.getCreated());
        assertEquals("a description", setupIntent.getDescription());
        assertFalse(setupIntent.getLiveMode());
        assertTrue(setupIntent.requiresAction());
        assertEquals(StripeIntent.Status.RequiresAction, setupIntent.getStatus());
        assertEquals(StripeIntent.Usage.OffSession, setupIntent.getUsage());

        final StripeIntent.SdkData sdkData = setupIntent.getStripeSdkData();
        assertNotNull(sdkData);
        assertTrue(sdkData.is3ds1());
    }

}
