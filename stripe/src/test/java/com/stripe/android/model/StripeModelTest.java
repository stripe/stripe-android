package com.stripe.android.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
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
}
