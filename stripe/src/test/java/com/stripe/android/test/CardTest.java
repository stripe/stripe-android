package com.stripe.android.test;

import org.junit.Test;

import com.stripe.android.model.Card;

import static org.junit.Assert.assertTrue;

public class CardTest {

    @Test
    public void canInitializeWithMinimalArguments() {
        Card card = new Card("4242-4242-4242-4242", 12, 2050, "123");
        assertTrue(card.validateNumber());
    }
}