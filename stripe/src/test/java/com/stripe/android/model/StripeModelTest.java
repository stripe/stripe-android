package com.stripe.android.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.testharness.JsonTestUtils;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.stripe.android.model.StripeModel.putModelIfNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

public class StripeModelTest {

    @Test
    public void equals_whenEquals_returnsTrue() {
        assertTrue(StripeModel.class.isAssignableFrom(Card.class));

        Card firstCard = Card.fromString(CardTest.JSON_CARD_USD);
        Card secondCard = Card.fromString(CardTest.JSON_CARD_USD);

        assertEquals(firstCard, secondCard);
        // Just confirming for sanity
        assertNotSame(firstCard, secondCard);
    }

    @Test
    public void equals_whenNotEquals_returnsFalse() {
        assertTrue(StripeModel.class.isAssignableFrom(Card.class));
        final Card firstCard = Card.create("4242", null, null, null);
        final Card secondCard = Card.create("4343", null, null, null);
        assertNotEquals(firstCard, secondCard);
    }

    @Test
    public void hashCode_whenEquals_returnsSameValue() {
        assertTrue(StripeModel.class.isAssignableFrom(Card.class));

        Card firstCard = Card.fromString(CardTest.JSON_CARD_USD);
        Card secondCard = Card.fromString(CardTest.JSON_CARD_USD);
        assertNotNull(firstCard);
        assertNotNull(secondCard);

        assertEquals(firstCard.hashCode(), secondCard.hashCode());
    }

    @Test
    public void hashCode_whenNotEquals_returnsDifferentValues() {
        assertTrue(StripeModel.class.isAssignableFrom(Card.class));

        Card usdCard = Card.fromString(CardTest.JSON_CARD_USD);
        Card eurCard = Card.fromString(CardTest.JSON_CARD_EUR);

        assertNotNull(usdCard);
        assertNotNull(eurCard);

        assertNotEquals(usdCard.hashCode(), eurCard.hashCode());
    }

    @Test
    public void putStripeJsonModelListIfNotNull_forMapsWhenNotNull_addsExpectedList() {
        List<ExampleModel> exampleJsonModels = new ArrayList<>();
        exampleJsonModels.add(new ExampleModel());
        exampleJsonModels.add(new ExampleModel());

        final Map<String, Object> originalMap = new HashMap<>();
        putModelIfNotNull(originalMap, "mapkey", exampleJsonModels);

        assertFalse(originalMap.isEmpty());
        assertTrue(originalMap.get("mapkey") instanceof List);

        //noinspection unchecked
        List<Map<String, Object>> modelList = (List) originalMap.get("mapkey");
        List<Map<String, Object>> expectedList = new ArrayList<>();
        expectedList.add(new ExampleModel().toMap());
        expectedList.add(new ExampleModel().toMap());

        JsonTestUtils.assertListEquals(expectedList, modelList);
    }

    private static class ExampleModel extends StripeModel {
        @NonNull
        @Override
        public Map<String, Object> toMap() {
            final Map<String, Object> map = new HashMap<>();
            map.put("akey", "avalue");
            map.put("bkey", "bvalue");
            return map;
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (!(obj instanceof ExampleModel)) {
                return false;
            }

            final ExampleModel other = (ExampleModel) obj;
            final Map<String, Object> myMap = toMap();
            final Map<String, Object> otherMap = other.toMap();
            for (String key : myMap.keySet()) {
                if (!otherMap.containsKey(key) ||
                        Objects.requireNonNull(myMap.get(key)).equals(otherMap.get(key))) {
                    return false;
                }
            }
            return myMap.size() == otherMap.size();
        }
    }
}
