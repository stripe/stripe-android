package com.stripe.android.testharness

import java.util.HashMap
import java.util.LinkedList
import java.util.TreeMap
import kotlin.test.Test
import kotlin.test.assertFailsWith

class JsonTestUtilsTest {

    @Test
    fun assertListEquals_forBothNull_passes() {
        JsonTestUtils.assertListEquals(null, null)
    }

    @Test
    fun assertListEquals_forDifferentOrders_fails() {
        val firstList = listOf("first", "second", "third", "fourth")
        val secondList = listOf("first", "third", "second", "fourth")
        assertFailsWith<AssertionError> { JsonTestUtils.assertListEquals(firstList, secondList) }
    }

    @Test
    fun assertMapEquals_forEmptyMapsOfDifferentTypes_passes() {
        val emptyTreeMap = TreeMap<String, Any>()
        JsonTestUtils.assertMapEquals(hashMapOf<String, Any>(), emptyTreeMap)
    }

    @Test
    fun assertMapEquals_forTwoNullMaps_passes() {
        JsonTestUtils.assertMapEquals(null, null)
    }

    @Test
    fun assertMapEquals_forDifferentNullity_fails() {
        val emptyHashMap = HashMap<String, Any>()
        assertFailsWith<AssertionError> { JsonTestUtils.assertMapEquals(emptyHashMap, null) }
    }

    @Test
    fun assertMapEquals_forSimpleMaps_passes() {
        val firstMap = HashMap<String, Any>()
        val secondMap = HashMap<String, Any>()
        firstMap["key"] = "value"
        secondMap["key"] = "value"
        JsonTestUtils.assertMapEquals(firstMap, secondMap)
    }

    @Test
    fun assertMapEquals_forSimpleDifferentMaps_fails() {
        val firstMap = HashMap<String, Any>()
        val secondMap = HashMap<String, Any>()
        firstMap["key"] = "value"
        secondMap["key"] = "a different value"
        assertFailsWith<AssertionError> {
            JsonTestUtils.assertMapEquals(firstMap, secondMap)
        }
    }

    @Test
    fun assertMapEquals_forNestedEqualMaps_passes() {
        val firstMap = HashMap<String, Any>()
        val secondMap = HashMap<String, Any>()
        val nestedMap = HashMap<String, Any>()
        val secondNestedMap = HashMap<String, Any>()
        val deepMap = HashMap<String, Any>()

        firstMap["key"] = "value"
        secondMap["key"] = "value"

        nestedMap["nestKey"] = "nestValue"
        deepMap["deepKey"] = "deepValue"
        secondNestedMap["recursiveKey"] = deepMap
        secondNestedMap["notARecursiveKey"] = "just a value"

        val copyNestedMap = HashMap(nestedMap)
        firstMap["nestedMapKey"] = nestedMap
        secondMap["nestedMapKey"] = copyNestedMap

        // This doesn't make a deep copy of the deepMap object.
        val copySecondNestedMap = HashMap(secondNestedMap)
        firstMap["secondNestedMapKey"] = secondNestedMap
        secondMap["secondNestedMapKey"] = copySecondNestedMap

        JsonTestUtils.assertMapEquals(firstMap, secondMap)
    }

    @Test
    fun assertMapEquals_forNestedDifferentMaps_fails() {
        val firstMap = HashMap<String, Any>()
        val secondMap = HashMap<String, Any>()
        val nestedMap = HashMap<String, Any>()
        val secondNestedMap = HashMap<String, Any>()
        val alteredSecondNestedMap = HashMap<String, Any>()
        val deepMap = HashMap<String, Any>()
        val alteredDeepMap = HashMap<String, Any>()

        firstMap["key"] = "value"
        secondMap["key"] = "value"

        nestedMap["nestKey"] = "nestValue"
        deepMap["deepKey"] = "deepValue"
        deepMap["secondDeepKey"] = 42
        alteredDeepMap["deepKey"] = "deepValue"

        secondNestedMap["recursiveKey"] = deepMap
        secondNestedMap["notARecursiveKey"] = "just a value"
        alteredSecondNestedMap["recursiveKey"] = alteredDeepMap
        alteredSecondNestedMap["notARecursiveKey"] = "just a value"

        val copyNestedMap = HashMap(nestedMap)
        firstMap["nestedMapKey"] = nestedMap
        secondMap["nestedMapKey"] = copyNestedMap

        // This doesn't make a deep copy of the deepMap object.
        firstMap["secondNestedMapKey"] = secondNestedMap
        secondMap["secondNestedMapKey"] = alteredSecondNestedMap

        assertFailsWith<AssertionError> { JsonTestUtils.assertMapEquals(firstMap, secondMap) }
    }

    @Test
    fun assertMapEquals_forEqualMapsWithListEntry_passes() {
        val intList = listOf(1, 2, 4, 8, 16)
        val secondIntList = LinkedList(intList)
        val firstMap = HashMap<String, Any>()
        val secondMap = HashMap<String, Any>()

        firstMap["key"] = "stringValue"
        secondMap["key"] = "stringValue"
        firstMap["listKey"] = intList
        secondMap["listKey"] = secondIntList

        JsonTestUtils.assertMapEquals(firstMap, secondMap)
    }

    @Test
    fun assertMapEquals_forUnequalMapsWithListEntry_passes() {
        val intList = listOf(1, 2, 4, 8, 16)
        val secondIntList = LinkedList(intList)
        secondIntList.add(7)
        val firstMap = HashMap<String, Any>()
        val secondMap = HashMap<String, Any>()

        firstMap["key"] = "stringValue"
        secondMap["key"] = "stringValue"
        firstMap["listKey"] = intList
        secondMap["listKey"] = secondIntList

        assertFailsWith<AssertionError> { JsonTestUtils.assertMapEquals(firstMap, secondMap) }
    }

    @Test
    fun assertMapEquals_forEqualMapsWithComplicatedListHierarchies_passes() {
        val intList = listOf(1, 2, 4, 8, 16)
        val secondIntList = LinkedList(intList)
        val firstMap = HashMap<String, Any>()
        val secondMap = HashMap<String, Any>()

        val genericList = listOf(
            intList,
            emptyMap<String, Any>(),
            "string entry",
            11
        )

        val secondGenericList = LinkedList<Any>()
        secondGenericList.add(secondIntList)
        secondGenericList.add(TreeMap<Any, Any>())
        secondGenericList.add("string entry")
        secondGenericList.add(11)

        firstMap["key"] = "stringValue"
        secondMap["key"] = "stringValue"
        firstMap["listKey"] = genericList
        secondMap["listKey"] = secondGenericList

        JsonTestUtils.assertMapEquals(firstMap, secondMap)
    }

    @Test
    fun assertMapEquals_forUnequalMapsWithComplicatedListHierarchies_fails() {
        val intList = listOf(1, 2, 4, 8, 16)
        val secondIntList = LinkedList(intList)
        val firstMap = HashMap<String, Any>()
        val secondMap = HashMap<String, Any>()

        val genericList = listOf(
            intList,
            emptyMap<String, Any>(),
            "string entry",
            11
        )

        val secondGenericList = LinkedList<Any>()
        secondGenericList.add(secondIntList)
        secondGenericList.add(TreeMap<Any, Any>())
        secondGenericList.add("a different string entry")
        secondGenericList.add(11)

        firstMap["key"] = "stringValue"
        secondMap["key"] = "stringValue"
        firstMap["listKey"] = genericList
        secondMap["listKey"] = secondGenericList

        assertFailsWith<AssertionError> { JsonTestUtils.assertMapEquals(firstMap, secondMap) }
    }
}
