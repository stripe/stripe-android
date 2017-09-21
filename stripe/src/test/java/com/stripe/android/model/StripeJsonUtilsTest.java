package com.stripe.android.model;

import com.stripe.android.testharness.JsonTestUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * Test class for {@link StripeJsonUtils}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 25)
public class StripeJsonUtilsTest {

    private static final String SIMPLE_JSON_TEST_OBJECT =
            "{\n" +
                    "    \"akey\": \"avalue\",\n" +
                    "    \"bkey\": \"bvalue\",\n" +
                    "    \"boolkey\": true,\n" +
                    "    \"numkey\": 123\n" +
                    "}";

    private static final String SIMPLE_JSON_HASH_OBJECT =
            "{\n" +
                    "    \"akey\": \"avalue\",\n" +
                    "    \"bkey\": \"bvalue\",\n" +
                    "    \"ckey\": \"cvalue\",\n" +
                    "    \"dkey\": \"dvalue\"\n" +
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

    @Test
    public void jsonObjectToMap_forNull_returnsNull() {
        assertNull(StripeJsonUtils.jsonObjectToMap(null));
    }

    @Test
    public void mapToJsonObject_forNull_returnsNull() {
        assertNull(StripeJsonUtils.mapToJsonObject(null));
    }

    @Test
    public void listToJsonArray_forNull_returnsNull() {
        assertNull(StripeJsonUtils.listToJsonArray(null));
    }

    @Test
    public void jsonArrayToList_forNull_returnsNull() {
        assertNull(StripeJsonUtils.jsonArrayToList(null));
    }

    @Test
    public void mapToJsonObject_forSimpleObjects_returnsExpectedObject() {
        Map<String, Object> testMap = new HashMap<>();
        testMap.put("akey", "avalue");
        testMap.put("bkey", "bvalue");
        testMap.put("boolkey", true);
        testMap.put("numkey", 123);

        try {
            JSONObject expectedJsonObject = new JSONObject(SIMPLE_JSON_TEST_OBJECT);
            JSONObject testObject = StripeJsonUtils.mapToJsonObject(testMap);
            JsonTestUtils.assertJsonEquals(expectedJsonObject, testObject);
        } catch (JSONException jsonException) {
            fail("Test data failure " + jsonException.getLocalizedMessage());
        }
    }

    @Test
    public void mapToJsonObject_forNestedMaps_returnsExpectedObject() {
        Map<String, Object> testMap = new HashMap<>();
        testMap.put("top_key",
                new HashMap<String, Object>() {{
                    put("first_inner_key",
                            new HashMap<String, Object>() {{
                                put("innermost_key", 1000);
                                put("second_innermost_key", "second_inner_value");
                            }});
                    put("second_inner_key", "just a value");
                }});
        testMap.put("second_outer_key",
                new HashMap<String, Object>() {{
                    put("another_inner_key", false);
                }});

        try {
            JSONObject expectedJsonObject = new JSONObject(NESTED_JSON_TEST_OBJECT);
            JSONObject testJsonObject = StripeJsonUtils.mapToJsonObject(testMap);
            JsonTestUtils.assertJsonEquals(expectedJsonObject, testJsonObject);
        } catch (JSONException jsonException) {
            fail("Test data failure " + jsonException.getLocalizedMessage());
        }
    }

    @Test
    public void mapToJsonObject_withNestedMapAndLists_returnsExpectedObject() {
        Map<String, Object> testMap = new HashMap<>();
        testMap.put("other_outer_key", false);

        final List<Object> itemsList = new ArrayList<>();
        itemsList.add(new HashMap<String, Object>() {{ put("id", 123); }});
        itemsList.add(new HashMap<String, Object>() {{
            put("id", "this time with letters");
        }});
        itemsList.add("a string item");
        itemsList.add(256);
        itemsList.add(Arrays.asList(1, 2, "C", 4));
        itemsList.add(Arrays.asList(new HashMap<String, Object>() {{
            put("deep", "deepValue");
        }}));
        testMap.put("outer_key",
                new HashMap<String, Object>() {{
                    put("items", itemsList);
                    put("another_key", "a simple value this time");
                }});

        try {
            JSONObject expectedJsonObject = new JSONObject(NESTED_MIXED_ARRAY_OBJECT);
            JSONObject testJsonObject = StripeJsonUtils.mapToJsonObject(testMap);
            JsonTestUtils.assertJsonEquals(expectedJsonObject, testJsonObject);
        } catch (JSONException jsonException) {
            fail("Test data failure " + jsonException.getLocalizedMessage());
        }
    }

    @Test
    public void listToJsonArray_forSimpleList_returnsExpectedArray() {
        List<Object> testList = new ArrayList<>();
        testList.add(1);
        testList.add(2);
        testList.add(3);
        testList.add("a");
        testList.add(true);
        testList.add("cde");

        try {
            JSONArray expectedJsonArray = new JSONArray(SIMPLE_JSON_TEST_ARRAY);
            JSONArray testJsonArray = StripeJsonUtils.listToJsonArray(testList);
            JsonTestUtils.assertJsonArrayEquals(expectedJsonArray, testJsonArray);
        } catch (JSONException jsonException) {
            fail("Test data failure " + jsonException.getLocalizedMessage());
        }
    }

    @Test
    public void jsonObjectToMap_forSimpleObjects_returnsExpectedMap() {
        Map<String, Object> expectedMap = new HashMap<>();
        expectedMap.put("akey", "avalue");
        expectedMap.put("bkey", "bvalue");
        expectedMap.put("boolkey", true);
        expectedMap.put("numkey", 123);

        try {
            JSONObject testJsonObject = new JSONObject(SIMPLE_JSON_TEST_OBJECT);
            Map<String, Object> mappedObject = StripeJsonUtils.jsonObjectToMap(testJsonObject);
            JsonTestUtils.assertMapEquals(expectedMap, mappedObject);
        } catch (JSONException jsonException) {
            fail("Test data failure " + jsonException.getLocalizedMessage());
        }
    }

    @Test
    public void jsonObjectToStringMap_forSimpleObjects_returnsExpectedMap() {
        Map<String, String> expectedMap = new HashMap<>();
        expectedMap.put("akey", "avalue");
        expectedMap.put("bkey", "bvalue");
        expectedMap.put("boolkey", "true");
        expectedMap.put("numkey", "123");

        try {
            JSONObject testJsonObject = new JSONObject(SIMPLE_JSON_TEST_OBJECT);
            Map<String, String> mappedObject =
                    StripeJsonUtils.jsonObjectToStringMap(testJsonObject);
            JsonTestUtils.assertMapEquals(expectedMap, mappedObject);
        } catch (JSONException jsonException) {
            fail("Test data failure " + jsonException.getLocalizedMessage());
        }
    }

    @Test
    public void jsonObjectToMap_forNestedObjects_returnsExpectedMap() {
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

        try {
            JSONObject testJsonObject = new JSONObject(NESTED_JSON_TEST_OBJECT);
            Map<String, Object> mappedObject = StripeJsonUtils.jsonObjectToMap(testJsonObject);
            JsonTestUtils.assertMapEquals(expectedMap, mappedObject);
        } catch (JSONException jsonException) {
            fail("Test data failure " + jsonException.getLocalizedMessage());
        }
    }

    @Test
    public void jsonObjectToStringMap_forNestedObjects_returnsExpectedFlatMap() {
        Map<String, String> expectedMap = new HashMap<>();
        expectedMap.put("top_key", "{\"first_inner_key\":{\"innermost_key\":1000," +
                "\"second_innermost_key\":\"second_inner_value\"}," +
                "\"second_inner_key\":\"just a value\"}");
        expectedMap.put("second_outer_key", "{\"another_inner_key\":false}");

        try {
            JSONObject testJsonObject = new JSONObject(NESTED_JSON_TEST_OBJECT);
            Map<String, String> mappedObject =
                    StripeJsonUtils.jsonObjectToStringMap(testJsonObject);
            JsonTestUtils.assertMapEquals(expectedMap, mappedObject);
        } catch (JSONException jsonException) {
            fail("Test data failure " + jsonException.getLocalizedMessage());
        }
    }

    @Test
    public void jsonObjectToMap_withNestedObjectAndArrays_returnsExpectedMap() {
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
        itemsList.add(Arrays.asList(new HashMap<String, Object>() {{
            put("deep", "deepValue");
        }}));
        expectedMap.put("outer_key",
                new HashMap<String, Object>() {{
                    put("items", itemsList);
                    put("another_key", "a simple value this time");
                }});

        try {
            JSONObject testJsonObject = new JSONObject(NESTED_MIXED_ARRAY_OBJECT);
            Map<String, Object> convertedMap = StripeJsonUtils.jsonObjectToMap(testJsonObject);
            JsonTestUtils.assertMapEquals(expectedMap, convertedMap);
        } catch (JSONException jsonException) {
            fail("Test data failure " + jsonException.getLocalizedMessage());
        }
    }

    @Test
    public void jsonArrayToList_forSimpleList_returnsExpectedList() {
        List<Object> expectedList = new ArrayList<>();
        expectedList.add(1);
        expectedList.add(2);
        expectedList.add(3);
        expectedList.add("a");
        expectedList.add(true);
        expectedList.add("cde");

        try {
            JSONArray testJsonArray = new JSONArray(SIMPLE_JSON_TEST_ARRAY);
            List<Object> convertedJsonArray = StripeJsonUtils.jsonArrayToList(testJsonArray);
            JsonTestUtils.assertListEquals(expectedList, convertedJsonArray);
        } catch (JSONException jsonException) {
            fail("Test data failure " + jsonException.getLocalizedMessage());
        }
    }

    @Test
    public void mapToJsonObjectAndBackToMap_forComplicatedObject_isNoOp() {
        try {
            JSONObject testJsonObject = new JSONObject(NESTED_MIXED_ARRAY_OBJECT);
            Map<String, Object> convertedMap = StripeJsonUtils.jsonObjectToMap(testJsonObject);
            JSONObject cycledObject = StripeJsonUtils.mapToJsonObject(convertedMap);
            JsonTestUtils.assertJsonEquals(cycledObject, testJsonObject);
        } catch (JSONException jsonException) {
            fail("Test data failure " + jsonException.getLocalizedMessage());
        }
    }

    @Test
    public void stringHashToJsonObject_returnsExpectedObject() {
        Map<String, String> stringHash = new HashMap<>();
        stringHash.put("akey", "avalue");
        stringHash.put("bkey", "bvalue");
        stringHash.put("ckey", "cvalue");
        stringHash.put("dkey", "dvalue");

        try {
            JSONObject expectedObject = new JSONObject(SIMPLE_JSON_HASH_OBJECT);
            JsonTestUtils.assertJsonEquals(expectedObject,
                    StripeJsonUtils.stringHashToJsonObject(stringHash));
        } catch (JSONException jsonException) {
            fail("Test data failure " + jsonException.getLocalizedMessage());
        }
    }
}
