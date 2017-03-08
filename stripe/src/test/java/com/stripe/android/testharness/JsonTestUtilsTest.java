package com.stripe.android.testharness;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.fail;

/**
 * Test class for the test utils class {@link JsonTestUtils}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23)
public class JsonTestUtilsTest {

    @Test
    public void assertJsonEquals_forEmptyJson_passes() {
        JsonTestUtils.assertJsonEquals(new JSONObject(), new JSONObject());
    }

    @Test
    public void assertJsonEquals_forBothNull_passes() {
        JsonTestUtils.assertJsonEquals(null, null);
    }

    @Test(expected = AssertionError.class)
    public void assertJsonEquals_forNullAndNonNull_fails() {
        JsonTestUtils.assertJsonEquals(new JSONObject(), null);
    }

    @Test
    public void assertJsonEquals_forSimpleObjects_passes() {
        String simpleJson = "{\"key\": \"value\"}";
        try {
            JSONObject first = new JSONObject(simpleJson);
            JSONObject second = new JSONObject(simpleJson);
            JsonTestUtils.assertJsonEquals(first, second);
        } catch (JSONException dataException) {
            fail("Test data failure: " + dataException.getLocalizedMessage());
        }
    }

    @Test
    public void assertJsonEquals_forSimpleObjectsWithNumbers_passes() {
        String simpleJson = "{\"key\": \"value\", \"key2\": 100}";
        try {
            JSONObject first = new JSONObject(simpleJson);
            JSONObject second = new JSONObject(simpleJson);
            JsonTestUtils.assertJsonEquals(first, second);
        } catch (JSONException dataException) {
            fail("Test data failure: " + dataException.getLocalizedMessage());
        }
    }

    @Test(expected = AssertionError.class)
    public void assertJsonEquals_forSimpleUnequalObjects_fails() {
        String simpleJson = "{\"key\": \"value\"}";
        String differentJson = "{\"key2\": \"value2\"}";
        try {
            JSONObject first = new JSONObject(simpleJson);
            JSONObject second = new JSONObject(differentJson);
            JsonTestUtils.assertJsonEquals(first, second);
        } catch (JSONException dataException) {
            fail("Test data failure: " + dataException.getLocalizedMessage());
        }
    }

    @Test(expected = AssertionError.class)
    public void assertJsonEquals_forSimpleObjectsOfDifferentSize_fails() {
        String simpleJson = "{\"key\": \"value\"}";
        String differentJson = "{\"key\": \"value\", \"key2\": \"value2\"}";
        try {
            JSONObject first = new JSONObject(simpleJson);
            JSONObject second = new JSONObject(differentJson);
            JsonTestUtils.assertJsonEquals(first, second);
        } catch (JSONException dataException) {
            fail("Test data failure: " + dataException.getLocalizedMessage());
        }
    }

    @Test(expected = AssertionError.class)
    public void assertJsonEquals_forSimpleUnequalObjectsWithSameKey_fails() {
        String simpleJson = "{\"key\": \"value\"}";
        String differentJson = "{\"key\": \"value2\"}";
        try {
            JSONObject first = new JSONObject(simpleJson);
            JSONObject second = new JSONObject(differentJson);
            JsonTestUtils.assertJsonEquals(first, second);
        } catch (JSONException dataException) {
            fail("Test data failure: " + dataException.getLocalizedMessage());
        }
    }

    @Test
    public void assertJsonEquals_withNestedEqualObjects_passes() {
        String nestedJson = "{\n" +
                "    \"top_key\": {\n" +
                "        \"first_inner_key\": {\n" +
                "            \"innermost_key\": \"inner_value\",\n" +
                "            \"second_innermost_key\": \"second_inner_value\"\n" +
                "        },\n" +
                "        \"second_inner_key\": \"just a value\"\n" +
                "    },\n" +
                "    \"second_outer_key\": {\n" +
                "        \"another_inner_key\": \"a value\"\n" +
                "    }\n" +
                "}";
        try {
            JSONObject first = new JSONObject(nestedJson);
            JSONObject second = new JSONObject(nestedJson);
            JsonTestUtils.assertJsonEquals(first, second);
        } catch (JSONException dataException) {
            fail("Test data failure: " + dataException.getLocalizedMessage());
        }
    }

    @Test(expected = AssertionError.class)
    public void assertJsonEquals_withNestedUnequalObjects_fails() {
        String nestedJson = "{\n" +
                "    \"top_key\": {\n" +
                "        \"first_inner_key\": {\n" +
                "            \"innermost_key\": \"inner_value\",\n" +
                "            \"second_innermost_key\": \"second_inner_value\"\n" +
                "        },\n" +
                "        \"second_inner_key\": \"just a value\"\n" +
                "    },\n" +
                "    \"second_outer_key\": {\n" +
                "        \"another_inner_key\": \"a value\"\n" +
                "    }\n" +
                "}";
        String alteredNestedJson = "{\n" +
                "    \"top_key\": {\n" +
                "        \"first_inner_key\": {\n" +
                "            \"innermost_key\": \"inner_value\",\n" +
                "            \"second_innermost_key\": \"SOMETHING QUITE DIFFERENT\"\n" +
                "        },\n" +
                "        \"second_inner_key\": \"just a value\"\n" +
                "    },\n" +
                "    \"second_outer_key\": {\n" +
                "        \"another_inner_key\": \"a value\"\n" +
                "    }\n" +
                "}";
        try {
            JSONObject first = new JSONObject(nestedJson);
            JSONObject second = new JSONObject(alteredNestedJson);
            JsonTestUtils.assertJsonEquals(first, second);
        } catch (JSONException dataException) {
            fail("Test data failure: " + dataException.getLocalizedMessage());
        }
    }

    @Test
    public void assertJsonEquals_forNestedEqualObjectsWithArrays_passes() {
        String complicatedJson = "{\n" +
                "    \"top_key\": {\n" +
                "        \"first_inner_key\": {\n" +
                "            \"innermost_key\": \"inner_value\",\n" +
                "            \"second_innermost_key\": \"second_inner_value\"\n" +
                "        },\n" +
                "        \"second_inner_key\": \"just a value\"\n" +
                "    },\n" +
                "    \"second_outer_key\": {\n" +
                "        \"items\": [\n" +
                "            {\"id\": 123},\n" +
                "            {\"id\": \"this time with letters\"},\n" +
                "            \"a string item\",\n" +
                "            256,\n" +
                "            [ 1, 2, \"C\", 4],\n" +
                "            [ {\"deep\": \"deepvalue\"} ]\n" +
                "        ],\n" +
                "        \"anotherkey\": \"a simple value this time\"\n" +
                "    }\n" +
                "}";
        try {
            JSONObject first = new JSONObject(complicatedJson);
            JSONObject second = new JSONObject(complicatedJson);
            JsonTestUtils.assertJsonEquals(first, second);
        } catch (JSONException dataException) {
            fail("Test data failure: " + dataException.getLocalizedMessage());
        }
    }

    @Test(expected = AssertionError.class)
    public void assertJsonEquals_forNestedUnEqualObjectsWithArrays_fails() {
        String complicatedJson = "{\n" +
                "    \"top_key\": {\n" +
                "        \"first_inner_key\": {\n" +
                "            \"innermost_key\": \"inner_value\",\n" +
                "            \"second_innermost_key\": \"second_inner_value\"\n" +
                "        },\n" +
                "        \"second_inner_key\": \"just a value\"\n" +
                "    },\n" +
                "    \"second_outer_key\": {\n" +
                "        \"items\": [\n" +
                "            {\"id\": 123},\n" +
                "            {\"id\": \"this time with letters\"},\n" +
                "            \"a string item\",\n" +
                "            256,\n" +
                "            [ 1, 2, \"C\", 4],\n" +
                "            [ {\"deep\": \"deepvalue\"} ]\n" +
                "        ],\n" +
                "        \"anotherkey\": \"a simple value this time\"\n" +
                "    }\n" +
                "}";

        String alteredComplicatedJson = "{\n" +
                "    \"top_key\": {\n" +
                "        \"first_inner_key\": {\n" +
                "            \"innermost_key\": \"inner_value\",\n" +
                "            \"second_innermost_key\": \"second_inner_value\"\n" +
                "        },\n" +
                "        \"second_inner_key\": \"just a value\"\n" +
                "    },\n" +
                "    \"second_outer_key\": {\n" +
                "        \"items\": [\n" +
                "            {\"id\": 789},\n" + // THIS IS THE ALTERED LINE
                "            {\"id\": \"this time with letters\"},\n" +
                "            \"a string item\",\n" +
                "            256,\n" +
                "            [ 1, 2, \"C\", 4],\n" +
                "            [ {\"deep\": \"deepvalue\"} ]\n" +
                "        ],\n" +
                "        \"anotherkey\": \"a simple value this time\"\n" +
                "    }\n" +
                "}";
        try {
            JSONObject first = new JSONObject(complicatedJson);
            JSONObject second = new JSONObject(alteredComplicatedJson);
            JsonTestUtils.assertJsonEquals(first, second);
        } catch (JSONException dataException) {
            fail("Test data failure: " + dataException.getLocalizedMessage());
        }
    }

    // Note: more complicated list and array functions are tested in the JsonObject and Map
    // functions, which are the core functions in the test class.
    @Test
    public void assertJsonArrayEquals_forBothNull_passes() {
        JsonTestUtils.assertJsonArrayEquals(null, null);
    }

    @Test
    public void assertListEquals_forBothNull_passes() {
        JsonTestUtils.assertListEquals(null, null);
    }

    @Test(expected = AssertionError.class)
    public void assertListEquals_forDifferentOrders_fails() {
        List<String> firstList = Arrays.asList("first", "second", "third", "fourth");
        List<String> secondList = Arrays.asList("first", "third", "second", "fourth");
        JsonTestUtils.assertListEquals(firstList, secondList);
    }

    @Test
    public void assertMapEquals_forEmptyMapsOfDifferentTypes_passes() {
        Map<String, Object> emptyHashMap = new HashMap<>();
        Map<String, Object> emptyTreeMap = new TreeMap<>();
        JsonTestUtils.assertMapEquals(emptyHashMap, emptyTreeMap);
    }

    @Test
    public void assertMapEquals_forTwoNullMaps_passes() {
        JsonTestUtils.assertMapEquals(null, null);
    }

    @Test(expected = AssertionError.class)
    public void assertMapEquals_forDifferentNullity_fails() {
        Map<String, Object> emptyHashMap = new HashMap<>();
        JsonTestUtils.assertMapEquals(emptyHashMap, null);
    }

    @Test
    public void assertMapEquals_forSimpleMaps_passes() {
        Map<String, Object> firstMap = new HashMap<>();
        Map<String, Object> secondMap = new HashMap<>();
        firstMap.put("key", "value");
        secondMap.put("key", "value");
        JsonTestUtils.assertMapEquals(firstMap, secondMap);
    }

    @Test(expected = AssertionError.class)
    public void assertMapEquals_forSimpleDifferentMaps_fails() {
        Map<String, Object> firstMap = new HashMap<>();
        Map<String, Object> secondMap = new HashMap<>();
        firstMap.put("key", "value");
        secondMap.put("key", "a different value");
        JsonTestUtils.assertMapEquals(firstMap, secondMap);
    }

    @Test
    public void assertMapEquals_forNestedEqualMaps_passes() {
        Map<String, Object> firstMap = new HashMap<>();
        Map<String, Object> secondMap = new HashMap<>();
        Map<String, Object> nestedMap = new HashMap<>();
        Map<String, Object> secondNestedMap = new HashMap<>();
        Map<String, Object> deepMap = new HashMap<>();

        firstMap.put("key", "value");
        secondMap.put("key", "value");

        nestedMap.put("nestKey", "nestValue");
        deepMap.put("deepKey", "deepValue");
        secondNestedMap.put("recursiveKey", deepMap);
        secondNestedMap.put("notARecursiveKey", "just a value");

        Map<String, Object> copyNestedMap = new HashMap<>(nestedMap);
        firstMap.put("nestedMapKey", nestedMap);
        secondMap.put("nestedMapKey", copyNestedMap);

        // This doesn't make a deep copy of the deepMap object.
        Map<String, Object> copySecondNestedMap = new HashMap<>(secondNestedMap);
        firstMap.put("secondNestedMapKey", secondNestedMap);
        secondMap.put("secondNestedMapKey", copySecondNestedMap);

        JsonTestUtils.assertMapEquals(firstMap, secondMap);
    }

    @Test(expected = AssertionError.class)
    public void assertMapEquals_forNestedDifferentMaps_fails() {
        Map<String, Object> firstMap = new HashMap<>();
        Map<String, Object> secondMap = new HashMap<>();
        Map<String, Object> nestedMap = new HashMap<>();
        Map<String, Object> secondNestedMap = new HashMap<>();
        Map<String, Object> alteredSecondNestedMap = new HashMap<>();
        Map<String, Object> deepMap = new HashMap<>();
        Map<String, Object> alteredDeepMap = new HashMap<>();

        firstMap.put("key", "value");
        secondMap.put("key", "value");

        nestedMap.put("nestKey", "nestValue");
        deepMap.put("deepKey", "deepValue");
        deepMap.put("secondDeepKey", 42);
        alteredDeepMap.put("deepKey", "deepValue");

        secondNestedMap.put("recursiveKey", deepMap);
        secondNestedMap.put("notARecursiveKey", "just a value");
        alteredSecondNestedMap.put("recursiveKey", alteredDeepMap);
        alteredSecondNestedMap.put("notARecursiveKey", "just a value");

        Map<String, Object> copyNestedMap = new HashMap<>(nestedMap);
        firstMap.put("nestedMapKey", nestedMap);
        secondMap.put("nestedMapKey", copyNestedMap);

        // This doesn't make a deep copy of the deepMap object.
        firstMap.put("secondNestedMapKey", secondNestedMap);
        secondMap.put("secondNestedMapKey", alteredSecondNestedMap);

        JsonTestUtils.assertMapEquals(firstMap, secondMap);
    }

    @Test
    public void assertMapEquals_forEqualMapsWithListEntry_passes() {
        List<Integer> intList = Arrays.asList(1, 2, 4, 8, 16);
        List<Integer> secondIntList = new LinkedList<>(intList);
        Map<String, Object> firstMap = new HashMap<>();
        Map<String, Object> secondMap = new HashMap<>();

        firstMap.put("key", "stringValue");
        secondMap.put("key", "stringValue");
        firstMap.put("listKey", intList);
        secondMap.put("listKey", secondIntList);

        JsonTestUtils.assertMapEquals(firstMap, secondMap);
    }

    @Test(expected = AssertionError.class)
    public void assertMapEquals_forUnequalMapsWithListEntry_passes() {
        List<Integer> intList = Arrays.asList(1, 2, 4, 8, 16);
        List<Integer> secondIntList = new LinkedList<>(intList);
        secondIntList.add(7);
        Map<String, Object> firstMap = new HashMap<>();
        Map<String, Object> secondMap = new HashMap<>();

        firstMap.put("key", "stringValue");
        secondMap.put("key", "stringValue");
        firstMap.put("listKey", intList);
        secondMap.put("listKey", secondIntList);

        JsonTestUtils.assertMapEquals(firstMap, secondMap);
    }

    @Test
    public void assertMapEquals_forEqualMapsWithComplicatedListHierarchies_passes() {
        List<Integer> intList = Arrays.asList(1, 2, 4, 8, 16);
        List<Integer> secondIntList = new LinkedList<>(intList);
        Map<String, Object> firstMap = new HashMap<>();
        Map<String, Object> secondMap = new HashMap<>();

        List<Object> genericList = new ArrayList<>();
        genericList.add(intList);
        genericList.add(new HashMap<String, Object>());
        genericList.add("string entry");
        genericList.add(11);

        List<Object> secondGenericList = new LinkedList<>();
        secondGenericList.add(secondIntList);
        secondGenericList.add(new TreeMap<>());
        secondGenericList.add("string entry");
        secondGenericList.add(11);

        firstMap.put("key", "stringValue");
        secondMap.put("key", "stringValue");
        firstMap.put("listKey", genericList);
        secondMap.put("listKey", secondGenericList);

        JsonTestUtils.assertMapEquals(firstMap, secondMap);
    }

    @Test(expected = AssertionError.class)
    public void assertMapEquals_forUnequalMapsWithComplicatedListHierarchies_fails() {
        List<Integer> intList = Arrays.asList(1, 2, 4, 8, 16);
        List<Integer> secondIntList = new LinkedList<>(intList);
        Map<String, Object> firstMap = new HashMap<>();
        Map<String, Object> secondMap = new HashMap<>();

        List<Object> genericList = new ArrayList<>();
        genericList.add(intList);
        genericList.add(new HashMap<String, Object>());
        genericList.add("string entry");
        genericList.add(11);

        List<Object> secondGenericList = new LinkedList<>();
        secondGenericList.add(secondIntList);
        secondGenericList.add(new TreeMap<>());
        secondGenericList.add("a different string entry");
        secondGenericList.add(11);

        firstMap.put("key", "stringValue");
        secondMap.put("key", "stringValue");
        firstMap.put("listKey", genericList);
        secondMap.put("listKey", secondGenericList);

        JsonTestUtils.assertMapEquals(firstMap, secondMap);
    }
}
