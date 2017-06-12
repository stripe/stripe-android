package com.stripe.android.model;

import android.support.annotation.Nullable;

import com.stripe.android.net.RequestOptions;
import com.stripe.android.testharness.JsonTestUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * Test class for {@link CustomerSources} model class.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23)
public class CustomerSourcesTest {

    private static final String NON_LIST_OBJECT =
            "{\n" +
                    "    \"object\": \"not_a_list\",\n" +
                    "    \"has_more\": false,\n" +
                    "    \"total_count\": 22,\n" +
                    "    \"url\": \"http://google.com\"\n" +
                    "}";

    private static final String NO_OBJECT_OBJECT =
            "{\n" +
                    "    \"object\": \"not_a_list\",\n" +
                    "    \"has_more\": false,\n" +
                    "    \"total_count\": 22,\n" +
                    "    \"url\": \"http://google.com\"\n" +
                    "}";

    private static final String LIST_OBJECT =
            "{\n" +
                    "    \"object\": \"list\",\n" +
                    "    \"data\": [ ],\n" +
                    "    \"has_more\": false,\n" +
                    "    \"total_count\": 22,\n" +
                    "    \"url\": \"http://google.com\"\n" +
                    "}";

    @Test
    public void fromJson_whenNotList_returnsNull() {
        JSONObject notListObject = safeReadJson(NON_LIST_OBJECT);
        assertNotNull(notListObject);

        CustomerSources sources = CustomerSources.fromJson(notListObject);
        assertNull(sources);
    }

    @Test
    public void fromJson_whenNoObject_returnsNull() {
        JSONObject notListObject = safeReadJson(NO_OBJECT_OBJECT);
        assertNotNull(notListObject);

        CustomerSources sources = CustomerSources.fromJson(notListObject);
        assertNull(sources);
    }

    @Test
    public void fromString_whenCorrectlyFormatted_createsExpectedObject() {
        CustomerSources sources = CustomerSources.fromString(LIST_OBJECT);
        assertNotNull(sources);
        assertEquals(Integer.valueOf(22), sources.getTotalCount());
        assertEquals(false, sources.getHasMore());
        assertEquals("http://google.com", sources.getUrl());
        JsonTestUtils.assertListEquals(new ArrayList<CustomerSourceData>(), sources.getData());
    }

    @Nullable
    private static JSONObject safeReadJson(String jsonString) {
        try {
            return new JSONObject(jsonString);
        } catch (JSONException exception) {
            fail("Test data failure: " + exception.getMessage());
            return null;
        }
    }
}
