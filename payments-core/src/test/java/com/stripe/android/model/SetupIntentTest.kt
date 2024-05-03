package com.stripe.android.model

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

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
        assertEquals(
            "seti_1EqTSZGMT9dGPIDGVzCUs6dV_secret_FL9mS9ILygVyGEOSmVNqHT83rxkqy0Y",
            setupIntent.clientSecret
        )
        assertEquals(1561677666, setupIntent.created)
        assertEquals("a description", setupIntent.description)
        assertEquals("pm_1EqTSoGMT9dGPIDG7dgafX1H", setupIntent.paymentMethodId)
        assertFalse(setupIntent.isLiveMode)
        assertTrue(setupIntent.requiresAction())
        assertEquals(StripeIntent.Status.RequiresAction, setupIntent.status)
        assertEquals(StripeIntent.Usage.OffSession, setupIntent.usage)

        assertEquals(
            StripeIntent.NextActionData.RedirectToUrl(
                Uri.parse(
                    "https://hooks.stripe.com/redirect/authenticate/src_1EqTStGMT9dGP" +
                        "IDGJGPkqE6B?client_secret=src_client_secret_FL9m741mmxtHykDlRTC5aQ02"
                ),
                returnUrl = "stripe://setup_intent_return"
            ),
            setupIntent.nextActionData
        )
    }

    @Test
    fun fromJsonStringWithNextAction_createsSetupIntentWithNextActionVerifyWithMicrodeposits() {
        val setupIntent = SetupIntentFixtures.SI_NEXT_ACTION_VERIFY_WITH_MICRODEPOSITS
        assertEquals("seti_1Kd5ncLu5o3P18ZpOYpGt5BF", setupIntent.id)
        assertEquals(
            "seti_1Kd5ncLu5o3P18ZpOYpGt5BF_secret_LJjGof4HuzSfxwvNCYP5UhdSQKSC9kS",
            setupIntent.clientSecret
        )
        assertEquals(1647233188, setupIntent.created)
        assertEquals("Example SetupIntent", setupIntent.description)
        assertEquals("pm_1Kd5ndLu5o3P18ZpLVuthxK2", setupIntent.paymentMethodId)
        assertFalse(setupIntent.isLiveMode)
        assertTrue(setupIntent.requiresAction())
        assertEquals(StripeIntent.Status.RequiresAction, setupIntent.status)
        assertEquals(StripeIntent.Usage.OffSession, setupIntent.usage)

        assertEquals(
            PaymentMethod.USBankAccount(
                accountHolderType = PaymentMethod.USBankAccount.USBankAccountHolderType.INDIVIDUAL,
                accountType = PaymentMethod.USBankAccount.USBankAccountType.CHECKING,
                bankName = "STRIPE TEST BANK",
                fingerprint = "FFDMA0xfhBjWSZLu",
                last4 = "6789",
                financialConnectionsAccount = null,
                networks = PaymentMethod.USBankAccount.USBankNetworks("ach", listOf("ach")),
                routingNumber = "110000000"
            ),
            setupIntent.paymentMethod?.usBankAccount
        )

        assertEquals(
            StripeIntent.NextActionData.VerifyWithMicrodeposits(
                arrivalDate = 1647327600,
                hostedVerificationUrl = "https://payments.stripe.com/microdeposit/sacs_test_YWNjdF8" +
                    "xSHZUSTdMdTVvM1AxOFpwLHNhX25vbmNlX0xKakc4NzlEYjNZaWxQT09Ma0RaZDROTklPcUVHb2s00" +
                    "00d7kDmkhf",
                microdepositType = MicrodepositType.AMOUNTS
            ),
            setupIntent.nextActionData
        )
    }

    @Test
    fun getLastSetupError_parsesCorrectly() {
        val lastSetupError = SetupIntentFixtures.SI_WITH_LAST_PAYMENT_ERROR.lastSetupError
        assertNotNull(lastSetupError)
        assertNotNull(lastSetupError.paymentMethod)
        assertEquals("pm_1F7J1bCRMbs6FrXfQKsYwO3U", lastSetupError.paymentMethod?.id)
        assertEquals("setup_intent_authentication_failure", lastSetupError.code)
        assertEquals(SetupIntent.Error.Type.InvalidRequestError, lastSetupError.type)
        assertEquals(
            "https://stripe.com/docs/error-codes/payment-intent-authentication-failure",
            lastSetupError.docUrl
        )
        assertEquals(
            "The provided PaymentMethod has failed authentication. You can provide " +
                "payment_method_data or a new PaymentMethod to attempt to fulfill this " +
                "PaymentIntent again.",
            lastSetupError.message
        )
    }

    @Test
    fun testCanceled() {
        assertEquals(
            StripeIntent.Status.Canceled,
            SetupIntentFixtures.CANCELLED.status
        )
        assertEquals(
            SetupIntent.CancellationReason.Abandoned,
            SetupIntentFixtures.CANCELLED.cancellationReason
        )
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

    @Test
    fun `getPaymentMethodOptions returns expected results`() {
        val setupIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD.copy(
            paymentMethodOptionsJsonString = """
                {
                    "card": {
                        "mandate_options": null,
                        "network": null,
                        "request_three_d_secure": "automatic"
                    },
                    "us_bank_account": {
                        "financial_connections": {
                            "permissions": "balances"
                        },
                        "setup_future_usage": "on_session",
                        "verification_method": "automatic"
                    }
                }
            """.trimIndent()
        )

        assertThat(setupIntent.getPaymentMethodOptions())
            .isEqualTo(
                mapOf(
                    "card" to mapOf(
                        "mandate_options" to null,
                        "network" to null,
                        "request_three_d_secure" to "automatic"
                    ),
                    "us_bank_account" to mapOf(
                        "financial_connections" to mapOf(
                            "permissions" to "balances"
                        ),
                        "setup_future_usage" to "on_session",
                        "verification_method" to "automatic"
                    )
                )
            )
    }
}
