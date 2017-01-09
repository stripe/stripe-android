package com.stripe.android.view;

import android.app.Activity;

import com.stripe.android.model.Card;
import com.stripe.android.testharness.CardInputTestActivity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ActivityController;

import static org.junit.Assert.assertEquals;

/**
 * Test class for {@link CardNumberEditText}. Note that we have to test against SDK 22
 * because of a <a href="https://github.com/robolectric/robolectric/issues/1932">known issue</a> in
 * Robolectric.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 22)
public class CardNumberEditTextTest {

    private CardNumberEditText mCardNumberEditText;

    @Before
    public void setup() {
        ActivityController activityController =
                Robolectric.buildActivity(CardInputTestActivity.class).create().start();

        mCardNumberEditText =
                ((CardInputTestActivity) activityController.get()).getCardNumberEditText();
    }

    @Test
    public void updateSelectionIndex_whenVisa_increasesIndexWhenGoingPastTheSpaces() {
        // Directly setting card brand as a testing hack (annotated in source)
        mCardNumberEditText.mCardBrand = Card.VISA;
        assertEquals(6, mCardNumberEditText.updateSelectionIndex(4, 4, 6));
        assertEquals(11, mCardNumberEditText.updateSelectionIndex(9, 9, 11));
        assertEquals(16, mCardNumberEditText.updateSelectionIndex(14, 14, 16));
    }

    @Test
    public void updateSelectionIndex_whenAmEx_increasesIndexWhenGoingPastTheSpaces() {
        mCardNumberEditText.mCardBrand = Card.AMERICAN_EXPRESS;
        assertEquals(6, mCardNumberEditText.updateSelectionIndex(4, 4, 6));
        assertEquals(13, mCardNumberEditText.updateSelectionIndex(11, 11, 13));
    }

    @Test
    public void updateSelectionIndex_whenDinersClub_decreasesIndexWhenDeletingPastTheSpaces() {
        mCardNumberEditText.mCardBrand = Card.DINERS_CLUB;
        assertEquals(4, mCardNumberEditText.updateSelectionIndex(6, 6, 4));
        assertEquals(9, mCardNumberEditText.updateSelectionIndex(11, 11, 9));
        assertEquals(14, mCardNumberEditText.updateSelectionIndex(16, 16, 14));
    }

    @Test
    public void updateSelectionIndex_whenAmEx_decreasesIndexWhenDeletingPastTheSpaces() {
        mCardNumberEditText.mCardBrand = Card.AMERICAN_EXPRESS;
        assertEquals(4, mCardNumberEditText.updateSelectionIndex(6, 6, 4));
        assertEquals(11, mCardNumberEditText.updateSelectionIndex(13, 13, 11));
    }
}
