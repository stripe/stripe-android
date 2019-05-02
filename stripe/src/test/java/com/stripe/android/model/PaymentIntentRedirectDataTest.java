package com.stripe.android.model;

import android.net.Uri;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
public class PaymentIntentRedirectDataTest {

    @Test
    public void create_withBothFieldsPopulated_shouldReturnCorrectObject() {
        final String url = "https://example.com";
        final String returnUrl = "yourapp://post-authentication-return-url";
        final Map<String, String> redirectMap = new HashMap<>();
        redirectMap.put(PaymentIntent.RedirectData.FIELD_URL, url);
        redirectMap.put(PaymentIntent.RedirectData.FIELD_RETURN_URL, returnUrl);

        final PaymentIntent.RedirectData redirectData = PaymentIntent.RedirectData.create(redirectMap);
        assertNotNull(redirectData);
        assertEquals(Uri.parse(url), redirectData.url);
        assertEquals(Uri.parse(returnUrl), redirectData.returnUrl);
    }

    @Test
    public void create_withOnlyUrlFieldPopulated_shouldReturnCorrectObject() {
        final String url = "https://example.com";
        final Map<String, String> redirectMap = new HashMap<>();
        redirectMap.put(PaymentIntent.RedirectData.FIELD_URL, url);

        final PaymentIntent.RedirectData redirectData =
                PaymentIntent.RedirectData.create(redirectMap);
        assertNotNull(redirectData);
        assertEquals(Uri.parse(url), redirectData.url);
        assertNull(redirectData.returnUrl);
    }

    @Test
    public void create_withInvalidData_shouldReturnNull() {
        assertNull(PaymentIntent.RedirectData.create(new HashMap<>()));
    }
}