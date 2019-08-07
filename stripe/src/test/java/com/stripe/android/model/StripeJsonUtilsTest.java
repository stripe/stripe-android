package com.stripe.android.model;

import com.stripe.android.testharness.JsonTestUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Test class for {@link StripeJsonUtils}.
 */
public class StripeJsonUtilsTest {

    private static final String SIMPLE_JSON_TEST_OBJECT =
            "{\n" +
                    "    \"akey\": \"avalue\",\n" +
                    "    \"bkey\": \"bvalue\",\n" +
                    "    \"boolkey\": true,\n" +
                    "    \"numkey\": 123\n" +
                    "}";

    private static final String NESTED_JSON_TEST_OBJECT =
            "{\n" +
                    "    \"top_key\": {\n" +
                    "        \"first_inner_key\": {\n" +
                    "            \"innermost_key\": 1000,\n" +
                    "            \"second_innermost_key\": \"second_inner_value\"\n" +
                    "        },\n" +
                    "        \"second_inner_key\": \"just a value\"\n" +
                    "    },\n" +
                    "    \"second_outer_key\": {\n" +
                    "        \"another_inner_key\": false\n" +
                    "    }\n" +
                    "}";

    private static final String SIMPLE_JSON_TEST_ARRAY = "[ 1, 2, 3, \"a\", true, \"cde\" ]";

    private static final String NESTED_MIXED_ARRAY_OBJECT =
            "{\n" +
                    "    \"outer_key\": {\n" +
                    "        \"items\": [\n" +
                    "            {\"id\": 123},\n" +
                    "            {\"id\": \"this time with letters\"},\n" +
                    "            \"a string item\",\n" +
                    "            256,\n" +
                    "            [ 1, 2, \"C\", 4],\n" +
                    "            [ {\"deep\": \"deepValue\"} ]\n" +
                    "        ],\n" +
                    "        \"another_key\": \"a simple value this time\"\n" +
                    "    },\n" +
                    "    \"other_outer_key\": false\n" +
                    "}";

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
    public void optString_whenFieldPresent_findsAndReturnsField() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("key", "value");
        assertEquals("value", StripeJsonUtils.optString(jsonObject, "key"));
    }

    @Test
    public void optString_whenFieldContainsRawNull_returnsNull() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("key", "null");
        assertNull(StripeJsonUtils.optString(jsonObject, "key"));
    }

    @Test
    public void optString_whenFieldNotPresent_returnsNull() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("key", "value");
        Object ob = StripeJsonUtils.optString(jsonObject, "nokeyshere");
        assertNull(ob);
    }

    @Test
    public void jsonObjectToMap_forNull_returnsNull() {
        assertNull(StripeJsonUtils.jsonObjectToMap(null));
    }

    @Test
    public void jsonArrayToList_forNull_returnsNull() {
        assertNull(StripeJsonUtils.jsonArrayToList(null));
    }

    @Test
    public void jsonObjectToMap_forSimpleObjects_returnsExpectedMap() throws JSONException {
        Map<String, Object> expectedMap = new HashMap<>();
        expectedMap.put("akey", "avalue");
        expectedMap.put("bkey", "bvalue");
        expectedMap.put("boolkey", true);
        expectedMap.put("numkey", 123);

        JSONObject testJsonObject = new JSONObject(SIMPLE_JSON_TEST_OBJECT);
        Map<String, Object> mappedObject = StripeJsonUtils.jsonObjectToMap(testJsonObject);
        JsonTestUtils.assertMapEquals(expectedMap, mappedObject);
    }

    @Test
    public void jsonObjectToStringMap_forSimpleObjects_returnsExpectedMap() throws JSONException {
        Map<String, String> expectedMap = new HashMap<>();
        expectedMap.put("akey", "avalue");
        expectedMap.put("bkey", "bvalue");
        expectedMap.put("boolkey", "true");
        expectedMap.put("numkey", "123");

        JSONObject testJsonObject = new JSONObject(SIMPLE_JSON_TEST_OBJECT);
        Map<String, String> mappedObject =
                StripeJsonUtils.jsonObjectToStringMap(testJsonObject);
        JsonTestUtils.assertMapEquals(expectedMap, mappedObject);
    }

    @Test
    public void jsonObjectToMap_forNestedObjects_returnsExpectedMap() throws JSONException {
        Map<String, Object> expectedMap = new HashMap<>();
        expectedMap.put("top_key",
                new HashMap<String, Object>() {{
                    put("first_inner_key",
                            new HashMap<String, Object>() {{
                                put("innermost_key", 1000);
                                put("second_innermost_key", "second_inner_value");
                            }});
                    put("second_inner_key", "just a value");
                }});
        expectedMap.put("second_outer_key",
                new HashMap<String, Object>() {{
                    put("another_inner_key", false);
                }});

        JSONObject testJsonObject = new JSONObject(NESTED_JSON_TEST_OBJECT);
        Map<String, Object> mappedObject = StripeJsonUtils.jsonObjectToMap(testJsonObject);
        JsonTestUtils.assertMapEquals(expectedMap, mappedObject);
    }

    @Test
    public void jsonObjectToStringMap_forNestedObjects_returnsExpectedFlatMap()
            throws JSONException {
        Map<String, String> expectedMap = new HashMap<>();
        expectedMap.put("top_key", "{\"first_inner_key\":{\"innermost_key\":1000," +
                "\"second_innermost_key\":\"second_inner_value\"}," +
                "\"second_inner_key\":\"just a value\"}");
        expectedMap.put("second_outer_key", "{\"another_inner_key\":false}");

        JSONObject testJsonObject = new JSONObject(NESTED_JSON_TEST_OBJECT);
        Map<String, String> mappedObject =
                StripeJsonUtils.jsonObjectToStringMap(testJsonObject);
        JsonTestUtils.assertMapEquals(expectedMap, mappedObject);
    }

    @Test
    public void jsonObjectToMap_withNestedObjectAndArrays_returnsExpectedMap()
            throws JSONException {
        Map<String, Object> expectedMap = new HashMap<>();
        expectedMap.put("other_outer_key", false);

        final List<Object> itemsList = new ArrayList<>();
        itemsList.add(new HashMap<String, Object>() {{ put("id", 123); }});
        itemsList.add(new HashMap<String, Object>() {{
            put("id", "this time with letters");
        }});
        itemsList.add("a string item");
        itemsList.add(256);
        itemsList.add(Arrays.asList(1, 2, "C", 4));
        itemsList.add(Collections.singletonList(new HashMap<String, Object>() {{
            put("deep", "deepValue");
        }}));
        expectedMap.put("outer_key",
                new HashMap<String, Object>() {{
                    put("items", itemsList);
                    put("another_key", "a simple value this time");
                }});

        JSONObject testJsonObject = new JSONObject(NESTED_MIXED_ARRAY_OBJECT);
        Map<String, Object> convertedMap = StripeJsonUtils.jsonObjectToMap(testJsonObject);
        JsonTestUtils.assertMapEquals(expectedMap, convertedMap);
    }

    @Test
    public void jsonArrayToList_forSimpleList_returnsExpectedList() throws JSONException {
        List<Object> expectedList = new ArrayList<>();
        expectedList.add(1);
        expectedList.add(2);
        expectedList.add(3);
        expectedList.add("a");
        expectedList.add(true);
        expectedList.add("cde");

        JSONArray testJsonArray = new JSONArray(SIMPLE_JSON_TEST_ARRAY);
        List<Object> convertedJsonArray = StripeJsonUtils.jsonArrayToList(testJsonArray);
        JsonTestUtils.assertListEquals(expectedList, convertedJsonArray);
    }
}
