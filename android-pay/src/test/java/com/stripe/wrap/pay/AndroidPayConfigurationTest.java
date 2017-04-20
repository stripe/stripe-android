package com.stripe.wrap.pay;

import android.os.Bundle;

import com.google.android.gms.wallet.Cart;
import com.google.android.gms.wallet.MaskedWalletRequest;
import com.google.android.gms.wallet.PaymentMethodTokenizationParameters;
import com.google.android.gms.wallet.PaymentMethodTokenizationType;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test class for {@link AndroidPayConfiguration}
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23)
public class AndroidPayConfigurationTest {

    Cart mCart;
    AndroidPayConfiguration mAndroidPayConfiguration;

    @Before
    public void setup() {
        mAndroidPayConfiguration = AndroidPayConfiguration.getInstance();
        mCart = Cart.newBuilder().setTotalPrice("10.00").build();
    }

    @Test
    public void getPaymentMethodTokenizationParameters_whenApiKeyIsNull_returnsNull() {
        assertNull(mAndroidPayConfiguration.getPaymentMethodTokenizationParameters());
    }

    @Test
    public void getPaymentMethodTokenizationParameters_whenApiKeyIsNotNull_returnsExpectedObject() {
        mAndroidPayConfiguration.setPublicApiKey("pk_test_abc123");
        PaymentMethodTokenizationParameters params =
                mAndroidPayConfiguration.getPaymentMethodTokenizationParameters();
        assertNotNull(params);
        assertEquals(PaymentMethodTokenizationType.PAYMENT_GATEWAY,
                params.getPaymentMethodTokenizationType());
        Bundle bundle = params.getParameters();
        assertNotNull(bundle);
        assertEquals("pk_test_abc123", bundle.getString("stripe:publishableKey"));
        assertEquals("stripe", bundle.getString("gateway"));
        assertEquals(com.stripe.android.BuildConfig.VERSION_NAME,
                bundle.getString("stripe:version"));
    }

    @Test
    public void generateMaskedWalletRequest_withJustCart_usesSetDefaults() {
        final String llamaName = "Llama Food Unlimited";
        mAndroidPayConfiguration.setMerchantName(llamaName);
        mAndroidPayConfiguration.setPublicApiKey("pk_test_llama");
        mAndroidPayConfiguration.setPhoneNumberRequired(true);
        mAndroidPayConfiguration.setShippingAddressRequired(false);
        mAndroidPayConfiguration.setCurrencyCode("usd");

        MaskedWalletRequest maskedWalletRequest =
                mAndroidPayConfiguration.generateMaskedWalletRequest(mCart);
        assertNotNull(maskedWalletRequest);
        assertTrue(maskedWalletRequest.isPhoneNumberRequired());
        assertFalse(maskedWalletRequest.isShippingAddressRequired());
        assertEquals("usd", maskedWalletRequest.getCurrencyCode());
        assertEquals(llamaName, maskedWalletRequest.getMerchantName());
        assertEquals(mCart.getTotalPrice(), maskedWalletRequest.getEstimatedTotalPrice());
        assertNotNull(maskedWalletRequest.getPaymentMethodTokenizationParameters());
        Bundle bundle =
                maskedWalletRequest.getPaymentMethodTokenizationParameters().getParameters();
        assertNotNull(bundle);
        assertEquals("pk_test_llama", bundle.getString("stripe:publishableKey"));
    }

    @Test
    public void generateMaskedWalletRequest_withAllData_overridesDefaults() {
        final String llamaName = "Llama Food Unlimited";
        mAndroidPayConfiguration.setMerchantName(llamaName);
        mAndroidPayConfiguration.setPublicApiKey("pk_test_llama");
        mAndroidPayConfiguration.setPhoneNumberRequired(true);
        mAndroidPayConfiguration.setShippingAddressRequired(false);
        mAndroidPayConfiguration.setCurrencyCode("usd");

        MaskedWalletRequest maskedWalletRequest =
                mAndroidPayConfiguration.generateMaskedWalletRequest(
                        mCart,
                        false,
                        true,
                        "eur");
        assertNotNull(maskedWalletRequest);
        assertFalse(maskedWalletRequest.isPhoneNumberRequired());
        assertTrue(maskedWalletRequest.isShippingAddressRequired());
        assertEquals("eur", maskedWalletRequest.getCurrencyCode());
        assertEquals(llamaName, maskedWalletRequest.getMerchantName());
        assertEquals(mCart.getTotalPrice(), maskedWalletRequest.getEstimatedTotalPrice());
        assertNotNull(maskedWalletRequest.getPaymentMethodTokenizationParameters());
        Bundle bundle =
                maskedWalletRequest.getPaymentMethodTokenizationParameters().getParameters();
        assertNotNull(bundle);
        assertEquals("pk_test_llama", bundle.getString("stripe:publishableKey"));
    }

    @Test
    public void generateMaskedWalletRequest_withoutCart_returnsNull() {
        assertNull(mAndroidPayConfiguration.generateMaskedWalletRequest(null));
    }

    @Test
    public void generateMaskedWalletRequest_whenCartHasNoTotalPrice_returnsNull() {
        Cart noPriceCart = Cart.newBuilder().build();
        assertNull(mAndroidPayConfiguration.generateMaskedWalletRequest(noPriceCart));
    }

    @Test
    public void generateMaskedWalletRequest_whenNoCurrencyIsSet_returnsNull() {
        // Need to instantiate a new Android Pay Configuration to avoid conflicts with other
        // tests.
        AndroidPayConfiguration testPayConfiguration = new AndroidPayConfiguration();
        testPayConfiguration.setPublicApiKey("pk_test_123");
        // In this case, we haven't set a currency yet.
        assertNull(testPayConfiguration.generateMaskedWalletRequest(mCart));
    }

    @Test
    public void generateMaskedWalletRequest_whenApiKeyIsSet_returnsNull() {
        // Need to instantiate a new Android Pay Configuration to avoid conflicts with other
        // tests.
        AndroidPayConfiguration testPayConfiguration = new AndroidPayConfiguration();

        // In this case, we haven't set a currency yet.
        assertNull(testPayConfiguration.generateMaskedWalletRequest(mCart));
    }
}
