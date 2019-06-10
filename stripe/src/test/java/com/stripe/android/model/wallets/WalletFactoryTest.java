package com.stripe.android.model.wallets;

import android.os.Parcel;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class WalletFactoryTest {
    private static final String VISA_WALLET_JSON = "{\n" +
            "\t\"type\": \"visa_checkout\",\n" +
            "\t\"dynamic_last4\": \"1234\",\n" +
            "\t\"visa_checkout\": {\n" +
            "\t\t\"billing_address\": {\n" +
            "\t\t\t\"city\": \"San Francisco\",\n" +
            "\t\t\t\"country\": \"US\",\n" +
            "\t\t\t\"line1\": \"510 Townsend St\",\n" +
            "\t\t\t\"postal_code\": \"94103\",\n" +
            "\t\t\t\"state\": \"CA\"\n" +
            "\t\t},\n" +
            "\t\t\"email\": \"me@example.com\",\n" +
            "\t\t\"name\": \"John Doe\",\n" +
            "\t\t\"shipping_address\": {\n" +
            "\t\t\t\"city\": \"San Francisco\",\n" +
            "\t\t\t\"country\": \"US\",\n" +
            "\t\t\t\"line1\": \"1355 Market St\",\n" +
            "\t\t\t\"postal_code\": \"94103\",\n" +
            "\t\t\t\"state\": \"CA\"\n" +
            "\t\t}\n" +
            "\t}\n" +
            "}";

    private static final String MASTERPASS_WALLET_JSON = "{\n" +
            "\t\"type\": \"master_pass\",\n" +
            "\t\"dynamic_last4\": \"1234\",\n" +
            "\t\"master_pass\": {\n" +
            "\t\t\"billing_address\": {\n" +
            "\t\t\t\"city\": \"San Francisco\",\n" +
            "\t\t\t\"country\": \"US\",\n" +
            "\t\t\t\"line1\": \"510 Townsend St\",\n" +
            "\t\t\t\"postal_code\": \"94103\",\n" +
            "\t\t\t\"state\": \"CA\"\n" +
            "\t\t},\n" +
            "\t\t\"email\": \"me@example.com\",\n" +
            "\t\t\"name\": \"John Doe\",\n" +
            "\t\t\"shipping_address\": {\n" +
            "\t\t\t\"city\": \"San Francisco\",\n" +
            "\t\t\t\"country\": \"US\",\n" +
            "\t\t\t\"line1\": \"1355 Market St\",\n" +
            "\t\t\t\"postal_code\": \"94103\",\n" +
            "\t\t\t\"state\": \"CA\"\n" +
            "\t\t}\n" +
            "\t}\n" +
            "}";

    private static final String AMEX_EXPRESS_CHECKOUT_WALLET_JSON = "{\n" +
            "\t\"type\": \"amex_express_checkout\",\n" +
            "\t\"dynamic_last4\": \"1234\",\n" +
            "\t\"amex_express_checkout\": {}\n" +
            "}";

    private static final String APPLE_PAY_WALLET_JSON = "{\n" +
            "\t\"type\": \"apple_pay\",\n" +
            "\t\"dynamic_last4\": \"1234\",\n" +
            "\t\"apple_pay\": {}\n" +
            "}";

    private static final String GOOGLE_PAY_WALLET_JSON = "{\n" +
            "\t\"type\": \"google_pay\",\n" +
            "\t\"dynamic_last4\": \"1234\",\n" +
            "\t\"google_pay\": {}\n" +
            "}";

    private static final String SAMSUNG_PAY_WALLET_JSON = "{\n" +
            "\t\"type\": \"samsung_pay\",\n" +
            "\t\"dynamic_last4\": \"1234\",\n" +
            "\t\"samsung_pay\": {}\n" +
            "}";

    @Test
    public void testCreateVisaCheckoutWallet() throws JSONException {
        final JSONObject walletJson = new JSONObject(VISA_WALLET_JSON);
        final Wallet wallet = new WalletFactory().create(walletJson);
        assertTrue(wallet instanceof VisaCheckoutWallet);
    }

    @Test
    public void testCreateMasterpassWallet() throws JSONException {
        final JSONObject walletJson = new JSONObject(MASTERPASS_WALLET_JSON);
        final Wallet wallet = new WalletFactory().create(walletJson);
        assertTrue(wallet instanceof MasterpassWallet);
    }

    @Test
    public void testCreateAmexExpressCheckoutWallet() throws JSONException {
        final JSONObject walletJson = new JSONObject(AMEX_EXPRESS_CHECKOUT_WALLET_JSON);
        final Wallet wallet = new WalletFactory().create(walletJson);
        assertTrue(wallet instanceof AmexExpressCheckoutWallet);
    }

    @Test
    public void testCreateApplePayWallet() throws JSONException {
        final JSONObject walletJson = new JSONObject(APPLE_PAY_WALLET_JSON);
        final Wallet wallet = new WalletFactory().create(walletJson);
        assertTrue(wallet instanceof ApplePayWallet);
    }

    @Test
    public void testCreateGooglePayWallet() throws JSONException {
        final JSONObject walletJson = new JSONObject(GOOGLE_PAY_WALLET_JSON);
        final Wallet wallet = new WalletFactory().create(walletJson);
        assertTrue(wallet instanceof GooglePayWallet);
    }

    @Test
    public void testCreateSamsungPayWallet() throws JSONException {
        final JSONObject walletJson = new JSONObject(SAMSUNG_PAY_WALLET_JSON);
        final Wallet wallet = new WalletFactory().create(walletJson);
        assertTrue(wallet instanceof SamsungPayWallet);
    }

    @Test
    public void testParcelable_shouldBeEqualAfterParcel() throws JSONException {
        final WalletFactory walletFactory = new WalletFactory();

        final SamsungPayWallet samsungPayWallet =
                (SamsungPayWallet) walletFactory.create(new JSONObject(SAMSUNG_PAY_WALLET_JSON));
        assertNotNull(samsungPayWallet);
        final Parcel samsungWalletParcel = Parcel.obtain();
        samsungPayWallet.writeToParcel(samsungWalletParcel, samsungPayWallet.describeContents());
        samsungWalletParcel.setDataPosition(0);
        final SamsungPayWallet parcelSamsungWallet =
                SamsungPayWallet.CREATOR.createFromParcel(samsungWalletParcel);
        assertEquals(samsungPayWallet, parcelSamsungWallet);

        final VisaCheckoutWallet visaWallet =
                (VisaCheckoutWallet) walletFactory.create(new JSONObject(VISA_WALLET_JSON));
        assertNotNull(visaWallet);
        final Parcel visaParcel = Parcel.obtain();
        visaWallet.writeToParcel(visaParcel, visaWallet.describeContents());
        visaParcel.setDataPosition(0);
        final VisaCheckoutWallet parcelVisaWallet =
                VisaCheckoutWallet.CREATOR.createFromParcel(visaParcel);
        assertEquals(visaWallet, parcelVisaWallet);
    }
}