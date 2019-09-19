package com.stripe.android.model.wallets

import android.os.Parcel
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WalletFactoryTest {

    @Test
    @Throws(JSONException::class)
    fun testCreateVisaCheckoutWallet() {
        val wallet = WalletFactory().create(VISA_WALLET_JSON)
        assertTrue(wallet is VisaCheckoutWallet)
    }

    @Test
    @Throws(JSONException::class)
    fun testCreateMasterpassWallet() {
        val wallet = WalletFactory().create(MASTERPASS_WALLET_JSON)
        assertTrue(wallet is MasterpassWallet)
    }

    @Test
    @Throws(JSONException::class)
    fun testCreateAmexExpressCheckoutWallet() {
        val wallet = WalletFactory().create(AMEX_EXPRESS_CHECKOUT_WALLET_JSON)
        assertTrue(wallet is AmexExpressCheckoutWallet)
    }

    @Test
    @Throws(JSONException::class)
    fun testCreateApplePayWallet() {
        val wallet = WalletFactory().create(APPLE_PAY_WALLET_JSON)
        assertTrue(wallet is ApplePayWallet)
    }

    @Test
    @Throws(JSONException::class)
    fun testCreateGooglePayWallet() {
        val wallet = WalletFactory().create(GOOGLE_PAY_WALLET_JSON)
        assertTrue(wallet is GooglePayWallet)
    }

    @Test
    @Throws(JSONException::class)
    fun testCreateSamsungPayWallet() {
        val wallet = WalletFactory().create(SAMSUNG_PAY_WALLET_JSON)
        assertTrue(wallet is SamsungPayWallet)
    }

    @Test
    @Throws(JSONException::class)
    fun testParcelable_shouldBeEqualAfterParcel() {
        val walletFactory = WalletFactory()

        val samsungPayWallet =
            walletFactory.create(SAMSUNG_PAY_WALLET_JSON) as SamsungPayWallet?
        assertNotNull(samsungPayWallet)
        val samsungWalletParcel = Parcel.obtain()
        samsungPayWallet!!.writeToParcel(samsungWalletParcel, samsungPayWallet.describeContents())
        samsungWalletParcel.setDataPosition(0)
        val parcelSamsungWallet =
            SamsungPayWallet.CREATOR.createFromParcel(samsungWalletParcel)
        assertEquals(samsungPayWallet, parcelSamsungWallet)

        val visaWallet =
            walletFactory.create(VISA_WALLET_JSON) as VisaCheckoutWallet?
        assertNotNull(visaWallet)
        val visaParcel = Parcel.obtain()
        visaWallet!!.writeToParcel(visaParcel, visaWallet.describeContents())
        visaParcel.setDataPosition(0)
        val parcelVisaWallet =
            VisaCheckoutWallet.CREATOR.createFromParcel(visaParcel)
        assertEquals(visaWallet, parcelVisaWallet)
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
