package com.stripe.android.model.wallets

import com.stripe.android.utils.ParcelUtils
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.json.JSONObject
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WalletFactoryTest {

    @Test
    fun testCreateVisaCheckoutWallet() {
        val wallet = WalletFactory().create(VISA_WALLET_JSON)
        assertTrue(wallet is VisaCheckoutWallet)
    }

    @Test
    fun testCreateMasterpassWallet() {
        val wallet = WalletFactory().create(MASTERPASS_WALLET_JSON)
        assertTrue(wallet is MasterpassWallet)
    }

    @Test
    fun testCreateAmexExpressCheckoutWallet() {
        val wallet = WalletFactory().create(AMEX_EXPRESS_CHECKOUT_WALLET_JSON)
        assertTrue(wallet is AmexExpressCheckoutWallet)
    }

    @Test
    fun testCreateApplePayWallet() {
        val wallet = WalletFactory().create(APPLE_PAY_WALLET_JSON)
        assertTrue(wallet is ApplePayWallet)
    }

    @Test
    fun testCreateGooglePayWallet() {
        val wallet = WalletFactory().create(GOOGLE_PAY_WALLET_JSON)
        assertTrue(wallet is GooglePayWallet)
    }

    @Test
    fun testCreateSamsungPayWallet() {
        val wallet = WalletFactory().create(SAMSUNG_PAY_WALLET_JSON)
        assertTrue(wallet is SamsungPayWallet)
    }

    @Test
    fun testParcelable_shouldBeEqualAfterParcel() {
        val walletFactory = WalletFactory()

        val samsungPayWallet =
            walletFactory.create(SAMSUNG_PAY_WALLET_JSON) as SamsungPayWallet
        assertEquals(
            samsungPayWallet,
            ParcelUtils.create(samsungPayWallet, SamsungPayWallet.CREATOR)
        )

        val visaWallet =
            walletFactory.create(VISA_WALLET_JSON) as VisaCheckoutWallet
        assertEquals(
            visaWallet,
            ParcelUtils.create(visaWallet, VisaCheckoutWallet.CREATOR)
        )
    }

    companion object {
        private val VISA_WALLET_JSON = JSONObject(
            """
            {
                "type": "visa_checkout",
                "dynamic_last4": "1234",
                "visa_checkout": {
                    "billing_address": {
                        "city": "San Francisco",
                        "country": "US",
                        "line1": "510 Townsend St",
                        "postal_code": "94103",
                        "state": "CA"
                    },
                    "email": "me@example.com",
                    "name": "John Doe",
                    "shipping_address": {
                        "city": "San Francisco",
                        "country": "US",
                        "line1": "1355 Market St",
                        "postal_code": "94103",
                        "state": "CA"
                    }
                }
            }
            """.trimIndent()
        )

        private val MASTERPASS_WALLET_JSON = JSONObject(
            """
            {
                "type": "master_pass",
                "dynamic_last4": "1234",
                "master_pass": {
                    "billing_address": {
                        "city": "San Francisco",
                        "country": "US",
                        "line1": "510 Townsend St",
                        "postal_code": "94103",
                        "state": "CA"
                    },
                    "email": "me@example.com",
                    "name": "John Doe",
                    "shipping_address": {
                        "city": "San Francisco",
                        "country": "US",
                        "line1": "1355 Market St",
                        "postal_code": "94103",
                        "state": "CA"
                    }
                }
            }
            """.trimIndent()
        )

        private val AMEX_EXPRESS_CHECKOUT_WALLET_JSON = JSONObject(
            """
            {
                "type": "amex_express_checkout",
                "dynamic_last4": "1234",
                "amex_express_checkout": {}
            }
            """.trimIndent()
        )

        private val APPLE_PAY_WALLET_JSON = JSONObject(
            """
            {
                "type": "apple_pay",
                "dynamic_last4": "1234",
                "apple_pay": {}
            }
            """.trimIndent()
        )

        private val GOOGLE_PAY_WALLET_JSON = JSONObject(
            """
            {
                "type": "google_pay",
                "dynamic_last4": "1234",
                "google_pay": {}
            }
            """.trimIndent()
        )

        private val SAMSUNG_PAY_WALLET_JSON = JSONObject(
            """
            {
                "type": "samsung_pay",
                "dynamic_last4": "1234",
                "samsung_pay": {}
            }
            """.trimIndent()
        )
    }
}
