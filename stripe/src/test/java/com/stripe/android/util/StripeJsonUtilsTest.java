package com.stripe.android.util;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * Test class for {@link StripeJsonUtils}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23)
public class StripeJsonUtilsTest {

    @Test
    public void nullIfNullOrEmpty_returnsNullForNull() {
        assertNull(StripeJsonUtils.nullIfNullOrEmpty("null"));
    }

    @Test
    public void nullIfNullOrEmpty_returnsNullForEmpty() {
        assertNull(StripeJsonUtils.nullIfNullOrEmpty(""));
    }

    @Test
    public void nullIfNullOrEmpty_returnsInputIfNotNull() {
        final String notANull = "notANull";
        assertEquals(notANull, StripeJsonUtils.nullIfNullOrEmpty(notANull));
    }

    @Test
    public void getString_whenFieldPresent_findsAndReturnsField() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("key", "value");
            assertEquals("value", StripeJsonUtils.getString(jsonObject, "key"));
        } catch (JSONException jex) {
            fail("No exception expected");
        }
    }

    @Test
    public void getString_whenFieldContainsRawNull_returnsNull() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("key", "null");
            assertNull(StripeJsonUtils.getString(jsonObject, "key"));
        } catch (JSONException jex) {
            fail("No exception expected");
        }
    }

    @Test(expected = JSONException.class)
    public void getString_whenFieldNotPresent_throwsJsonException() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("key", "value");

        StripeJsonUtils.getString(jsonObject, "differentKey");
        fail("Expected an exception.");
    }

    @Test
    public void optString_whenFieldPresent_findsAndReturnsField() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("key", "value");
            assertEquals("value", StripeJsonUtils.optString(jsonObject, "key"));
        } catch (JSONException jex) {
            fail("No exception expected");
        }
    }

    @Test
    public void optString_whenFieldContainsRawNull_returnsNull() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("key", "null");
            assertNull(StripeJsonUtils.optString(jsonObject, "key"));
        } catch (JSONException jex) {
            fail("No exception expected");
        }
    }

    @Test
    public void optString_whenFieldNotPresent_returnsNull() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("key", "value");
            Object ob  = StripeJsonUtils.optString(jsonObject, "nokeyshere");
            assertNull(ob);
        } catch (JSONException jex) {
            fail("No exception expected");
        }
    }
}
