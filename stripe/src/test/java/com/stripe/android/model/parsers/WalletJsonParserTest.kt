package com.stripe.android.model.parsers

import com.stripe.android.model.Address
import com.stripe.android.model.wallets.Wallet
import com.stripe.android.utils.ParcelUtils
import org.json.JSONObject
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class WalletJsonParserTest {

    @Test
    fun testParseVisaCheckoutWallet() {
        val actualWallet = parse(VISA_WALLET_JSON)
        val expectedWallet = Wallet.VisaCheckoutWallet(
            email = "me@example.com",
            name = "John Doe",
            dynamicLast4 = "1234",
            billingAddress = Address(
                line1 = "510 Townsend St",
                city = "San Francisco",
                state = "CA",
                postalCode = "94103",
                country = "US"
            ),
            shippingAddress = Address(
                line1 = "1355 Market St",
                city = "San Francisco",
                state = "CA",
                postalCode = "94103",
                country = "US"
            )
        )
        assertEquals(expectedWallet, actualWallet)
    }

    @Test
    fun testParseMasterpassWallet() {
        val actualWallet = parse(MASTERPASS_WALLET_JSON)
        val expectedWallet = Wallet.MasterpassWallet(
            email = "me@example.com",
            name = "John Doe",
            billingAddress = Address(
                line1 = "510 Townsend St",
                city = "San Francisco",
                state = "CA",
                postalCode = "94103",
                country = "US"
            ),
            shippingAddress = Address(
                line1 = "1355 Market St",
                city = "San Francisco",
                state = "CA",
                postalCode = "94103",
                country = "US"
            )
        )

        assertEquals(expectedWallet, actualWallet)
    }

    @Test
    fun testParseAmexExpressCheckoutWallet() {
        val wallet = parse(AMEX_EXPRESS_CHECKOUT_WALLET_JSON)
        assertEquals(Wallet.AmexExpressCheckoutWallet("1234"), wallet)
    }

    @Test
    fun testParseApplePayWallet() {
        val wallet = parse(APPLE_PAY_WALLET_JSON)
        assertEquals(Wallet.ApplePayWallet("1234"), wallet)
    }

    @Test
    fun testParseGooglePayWallet() {
        val wallet = parse(GOOGLE_PAY_WALLET_JSON)
        assertEquals(Wallet.GooglePayWallet("1234"), wallet)
    }

    @Test
    fun testParseSamsungPayWallet() {
        val wallet = parse(SAMSUNG_PAY_WALLET_JSON)
        assertEquals(Wallet.SamsungPayWallet("1234"), wallet)
    }

    @Test
    fun testParcelable_shouldBeEqualAfterParcel() {
        val samsungPayWallet = parse(SAMSUNG_PAY_WALLET_JSON) as Wallet.SamsungPayWallet
        assertEquals(Wallet.Type.SamsungPay, samsungPayWallet.walletType)
        assertEquals(samsungPayWallet, ParcelUtils.create(samsungPayWallet))

        val visaWallet = parse(VISA_WALLET_JSON) as Wallet.VisaCheckoutWallet
        assertEquals(visaWallet, ParcelUtils.create(visaWallet))
    }

    private companion object {
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

        private fun parse(json: JSONObject): Wallet? {
            return WalletJsonParser().parse(json)
        }
    }
}
