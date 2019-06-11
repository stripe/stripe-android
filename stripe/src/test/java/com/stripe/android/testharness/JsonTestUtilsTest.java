package com.stripe.android.testharness;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class JsonTestUtilsTest {

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
