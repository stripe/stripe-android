package com.stripe.android.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SetupIntentTest {

    @Test
    fun parseIdFromClientSecret_correctIdParsed() {
        val id = SetupIntent.ClientSecret(
            "seti_1Eq5kyGMT9dGPIDGxiSp4cce_secret_FKlHb3yTI0YZWe4iqghS8ZXqwwMoMmy"
        ).setupIntentId
        assertEquals("seti_1Eq5kyGMT9dGPIDGxiSp4cce", id)
    }

    @Test
    fun fromJsonStringWithNextAction_createsSetupIntentWithNextAction() {
        val setupIntent = SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT
        assertEquals("seti_1EqTSZGMT9dGPIDGVzCUs6dV", setupIntent.id)
        assertEquals("seti_1EqTSZGMT9dGPIDGVzCUs6dV_secret_FL9mS9ILygVyGEOSmVNqHT83rxkqy0Y",
            setupIntent.clientSecret)
        assertEquals(1561677666, setupIntent.created)
        assertEquals("a description", setupIntent.description)
        assertEquals("pm_1EqTSoGMT9dGPIDG7dgafX1H", setupIntent.paymentMethodId)
        assertFalse(setupIntent.isLiveMode)
        assertTrue(setupIntent.requiresAction())
        assertEquals(StripeIntent.Status.RequiresAction, setupIntent.status)
        assertEquals(StripeIntent.Usage.OffSession, setupIntent.usage)

        val redirectData = setupIntent.redirectData
        assertNotNull(redirectData)
        assertNotNull(redirectData.returnUrl)
        assertNotNull(setupIntent.redirectUrl)
        assertEquals("stripe://setup_intent_return", redirectData.returnUrl)
        assertEquals("https://hooks.stripe.com/redirect/authenticate/src_1EqTStGMT9dGPIDGJGPkqE6B" + "?client_secret=src_client_secret_FL9m741mmxtHykDlRTC5aQ02", redirectData.url.toString())
        assertEquals("https://hooks.stripe.com/redirect/authenticate/src_1EqTStGMT9dGPIDGJGPkqE6B" + "?client_secret=src_client_secret_FL9m741mmxtHykDlRTC5aQ02",
            setupIntent.redirectUrl?.toString())
    }

    @Test
    fun getLastSetupError_parsesCorrectly() {
        val lastSetupError = SetupIntentFixtures.SI_WITH_LAST_PAYMENT_ERROR.lastSetupError
        assertNotNull(lastSetupError)
        assertNotNull(lastSetupError.paymentMethod)
        assertEquals("pm_1F7J1bCRMbs6FrXfQKsYwO3U", lastSetupError.paymentMethod?.id)
        assertEquals("payment_intent_authentication_failure", lastSetupError.code)
        assertEquals(SetupIntent.Error.Type.InvalidRequestError, lastSetupError.type)
        assertEquals(
            "https://stripe.com/docs/error-codes/payment-intent-authentication-failure",
            lastSetupError.docUrl
        )
        assertEquals(
            "The provided PaymentMethod has failed authentication. You can provide payment_method_data or a new PaymentMethod to attempt to fulfill this PaymentIntent again.",
            lastSetupError.message
        )
    }

    @Test
    fun testCanceled() {
        assertEquals(StripeIntent.Status.Canceled,
            SetupIntentFixtures.CANCELLED.status)
        assertEquals(SetupIntent.CancellationReason.Abandoned,
            SetupIntentFixtures.CANCELLED.cancellationReason)
    }

    @Test
    fun clientSecret_withInvalidKeys_throwsException() {
        assertFailsWith<IllegalArgumentException> {
            SetupIntent.ClientSecret("seti_12345")
        }

        assertFailsWith<IllegalArgumentException> {
            SetupIntent.ClientSecret("seti_12345_secret_")
        }

        assertFailsWith<IllegalArgumentException> {
            SetupIntent.ClientSecret("seti_secret")
        }

        assertFailsWith<IllegalArgumentException> {
            SetupIntent.ClientSecret("seti_secret_a")
        }

        assertFailsWith<IllegalArgumentException> {
            PaymentIntent.ClientSecret("seti_a1b2c3_secret_x7y8z9pi_a1b2c3_secret_x7y8z9")
        }
    }

    @Test
    fun clientSecret_withValidKeys_succeeds() {
        assertEquals(
            "seti_a1b2c3_secret_x7y8z9",
            SetupIntent.ClientSecret("seti_a1b2c3_secret_x7y8z9").value
        )
    }
}
