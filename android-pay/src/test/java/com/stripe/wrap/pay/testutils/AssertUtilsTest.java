package com.stripe.wrap.pay.testutils;

import android.support.annotation.NonNull;

import com.google.android.gms.identity.intents.model.CountrySpecification;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Test class for {@link AssertUtils}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 25)
public class AssertUtilsTest {

    @Test
    public void assertListEmpty_whenEmpty_passes() {
        List<String> emptyList = new LinkedList<>();
        AssertUtils.assertEmpty(emptyList);
    }

    @Test(expected = AssertionError.class)
    public void assertListEmpty_whenNull_fails() {
        AssertUtils.assertEmpty((List) null);
    }

    @Test(expected = AssertionError.class)
    public void assertListEmpty_whenNotEmpty_fails() {
        List<String> notEmpty = new LinkedList<>();
        notEmpty.add("hello");
        AssertUtils.assertEmpty(notEmpty);
    }

    @Test
    public void assertMapEmpty_whenEmpty_passes() {
        Map<String, String> emptyMap = new HashMap<>();
        AssertUtils.assertEmpty(emptyMap);
    }

    @Test(expected = AssertionError.class)
    public void assertMapEmpty_whenNull_fails() {
        AssertUtils.assertEmpty((Map) null);
    }

    @Test(expected = AssertionError.class)
    public void assertMapEmpty_whenNotEmpty_fails() {
        Map<Integer, String> notEmptyMap = new HashMap<>();
        notEmptyMap.put(1, "one");
        AssertUtils.assertEmpty(notEmptyMap);
    }

    @Test
    public void assertListEquals_whenBothAreNull_passes() {
        AssertUtils.assertListEquals(null, null);
    }

    @Test
    public void assertUnorderedListEquals_whenBothAreNull_passes() {
        AssertUtils.assertUnorderedListEquals(null, null);
    }

    @Test(expected = AssertionError.class)
    public void assertListEquals_whenOnlyFirstNull_fails() {
        List<String> emptyList = new ArrayList<>();
        AssertUtils.assertListEquals(null, emptyList);
    }

    @Test(expected = AssertionError.class)
    public void assertListEquals_whenOnlySecondNull_fails() {
        List<String> emptyList = new ArrayList<>();
        AssertUtils.assertListEquals(emptyList, null);
    }

    @Test(expected = AssertionError.class)
    public void assertUnorderedListEquals_whenOnlyFirstNull_fails() {
        List<String> emptyList = new ArrayList<>();
        AssertUtils.assertUnorderedListEquals(null, emptyList);
    }

    @Test(expected = AssertionError.class)
    public void assertUnorderedListEquals_whenOnlySecondNull_fails() {
        List<String> emptyList = new ArrayList<>();
        AssertUtils.assertUnorderedListEquals(emptyList, null);
    }

    @Test
    public void assertListEqual_withIdenticalLists_passes() {
        List<String> sampleList = new ArrayList<>();
        sampleList.add("one");
        sampleList.add("two");
        sampleList.add("three");

        AssertUtils.assertListEquals(sampleList, sampleList);
    }

    @Test
    public void assertUnorderedListEquals_withIdenticalLists_passes() {
        List<String> sampleList = new ArrayList<>();
        sampleList.add("one");
        sampleList.add("two");
        sampleList.add("three");

        AssertUtils.assertUnorderedListEquals(sampleList, sampleList);
    }

    @Test(expected = AssertionError.class)
    public void assertListEquals_withMixedOrderLists_fails() {
        List<String> sampleList = new ArrayList<>();
        sampleList.add("one");
        sampleList.add("two");
        sampleList.add("three");

        List<String> secondList = new ArrayList<>();
        secondList.add("two");
        secondList.add("three");
        secondList.add("one");
        AssertUtils.assertListEquals(sampleList, secondList);
    }

    @Test
    public void assertUnorderedListEquals_withMixedOrderLists_passes() {
        List<SampleObject> sampleList = new ArrayList<>();
        sampleList.add(new SampleObject("one"));
        sampleList.add(new SampleObject("two"));
        sampleList.add(new SampleObject("three"));

        List<SampleObject> secondList = new ArrayList<>();
        secondList.add(new SampleObject("two"));
        secondList.add(new SampleObject("three"));
        secondList.add(new SampleObject("one"));
        AssertUtils.assertUnorderedListEquals(sampleList, secondList);
    }

    @Test(expected = AssertionError.class)
    public void assertUnorderedListEquals_withUnequalLists_fails() {
        List<String> sampleList = new ArrayList<>();
        sampleList.add("one");
        sampleList.add("two");
        sampleList.add("three");

        List<String> secondList = new ArrayList<>();
        secondList.add("two");
        secondList.add("three");
        secondList.add("four");
        AssertUtils.assertUnorderedListEquals(sampleList, secondList);
    }

    @Test(expected = AssertionError.class)
    public void assertUnorderedListEquals_withUnequalListsSubset_fails() {
        List<String> sampleList = new ArrayList<>();
        sampleList.add("one");
        sampleList.add("two");
        sampleList.add("three");

        List<String> secondList = new ArrayList<>();
        secondList.add("two");
        secondList.add("three");
        secondList.add("two");
        AssertUtils.assertUnorderedListEquals(sampleList, secondList);
    }

    @Test(expected = AssertionError.class)
    public void assertListEquals_withDifferentSizes_fails() {
        List<Integer> sampleList = new ArrayList<>();
        sampleList.add(1);
        sampleList.add(2);
        sampleList.add(3);

        List<Integer> secondList = new ArrayList<>();
        secondList.add(1);
        AssertUtils.assertListEquals(sampleList, secondList);
    }

    @Test(expected = AssertionError.class)
    public void assertUnorderedListEquals_withDifferentSizes_fails() {
        List<Integer> sampleList = new ArrayList<>();
        sampleList.add(1);
        sampleList.add(2);
        sampleList.add(3);

        List<Integer> secondList = new ArrayList<>();
        secondList.add(1);
        AssertUtils.assertUnorderedListEquals(sampleList, secondList);
    }

    @Test(expected = AssertionError.class)
    public void assertUnorderedListEquals_withDuplicates_fails() {
        List<String> sampleList = new ArrayList<>();
        sampleList.add("one");
        sampleList.add("two");
        sampleList.add("three");

        List<String> secondList = new ArrayList<>();
        secondList.add("one");
        secondList.add("one");
        secondList.add("two");
        secondList.add("three");
        AssertUtils.assertUnorderedListEquals(sampleList, secondList);
    }

    @Test
    public void assertContainsEqual_whenContainsEqual_passes() {
        List<SampleObject> sampleObjects = new ArrayList<>();
        sampleObjects.add(new SampleObject("hello"));
        sampleObjects.add(new SampleObject("world"));
        AssertUtils.assertContainsEqual(
                new SampleObject("world"), new HashSet<>(sampleObjects));
    }

    @Test
    public void assertContainsEqual_whenCountrySpecifications_passes() {
        List<CountrySpecification> sampleCountries = new ArrayList<>();
        sampleCountries.add(new CountrySpecification("US"));
        sampleCountries.add(new CountrySpecification("GB"));
        AssertUtils.assertContainsEqual(
                new CountrySpecification("US"), new HashSet<>(sampleCountries));
    }

    @Test(expected = AssertionError.class)
    public void assertContainsEqual_whenNotContained_fails() {
        List<SampleObject> sampleObjects = new ArrayList<>();
        sampleObjects.add(new SampleObject("hello"));
        sampleObjects.add(new SampleObject("world"));
        AssertUtils.assertContainsEqual(
                new SampleObject("foo"), new HashSet<>(sampleObjects));
    }

    private class SampleObject {
        @NonNull String property;

        SampleObject(@NonNull String property) {
            this.property = property;
        }

        @Override
        public boolean equals(Object obj) {
            return obj != null
                    && obj instanceof SampleObject
                    && ((SampleObject) obj).property.equals(this.property);
        }

        @Override
        public int hashCode() {
            return this.property.hashCode();
        }
    }
}
