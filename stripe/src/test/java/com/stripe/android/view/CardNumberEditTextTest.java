package com.stripe.android.view;

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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test class for {@link CardNumberEditText}. Note that we have to test against SDK 22
 * because of a <a href="https://github.com/robolectric/robolectric/issues/1932">known issue</a> in
 * Robolectric.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 22)
public class CardNumberEditTextTest {

    private static final String VALID_VISA_WITH_SPACES = "4242 4242 4242 4242";
    private static final String VALID_AMEX_WITH_SPACES = "3782 822463 10005";
    private CardNumberEditText mCardNumberEditText;

    @Before
    public void setup() {
        ActivityController activityController =
                Robolectric.buildActivity(CardInputTestActivity.class).create().start();

        mCardNumberEditText =
                ((CardInputTestActivity) activityController.get()).getCardNumberEditText();
        mCardNumberEditText.setText("");
    }

    @Test
    public void updateSelectionIndex_whenVisa_increasesIndexWhenGoingPastTheSpaces() {
        // Directly setting card brand as a testing hack (annotated in source)
        mCardNumberEditText.mCardBrand = Card.VISA;

        // Adding 1 character, starting at position 4, with a final string length 6
        assertEquals(6, mCardNumberEditText.updateSelectionIndex(6, 4, 1));
        assertEquals(6, mCardNumberEditText.updateSelectionIndex(8, 4, 1));
        assertEquals(11, mCardNumberEditText.updateSelectionIndex(11, 9, 1));
        assertEquals(16, mCardNumberEditText.updateSelectionIndex(16, 14, 1));
    }

    @Test
    public void updateSelectionIndex_whenAmEx_increasesIndexWhenGoingPastTheSpaces() {
        mCardNumberEditText.mCardBrand = Card.AMERICAN_EXPRESS;
        assertEquals(6, mCardNumberEditText.updateSelectionIndex(6, 4, 1));
        assertEquals(13, mCardNumberEditText.updateSelectionIndex(13, 11, 1));
    }

    @Test
    public void updateSelectionIndex_whenDinersClub_decreasesIndexWhenDeletingPastTheSpaces() {
        mCardNumberEditText.mCardBrand = Card.DINERS_CLUB;
        assertEquals(4, mCardNumberEditText.updateSelectionIndex(6, 5, 0));
        assertEquals(9, mCardNumberEditText.updateSelectionIndex(13, 10, 0));
        assertEquals(14, mCardNumberEditText.updateSelectionIndex(17, 15, 0));
    }

    @Test
    public void updateSelectionIndex_whenDeletingNotOnGaps_doesNotDecreaseIndex() {
        mCardNumberEditText.mCardBrand = Card.DINERS_CLUB;
        assertEquals(7, mCardNumberEditText.updateSelectionIndex(12, 7, 0));
    }

    @Test
    public void updateSelectionIndex_whenAmEx_decreasesIndexWhenDeletingPastTheSpaces() {
        mCardNumberEditText.mCardBrand = Card.AMERICAN_EXPRESS;
        assertEquals(4, mCardNumberEditText.updateSelectionIndex(10, 5, 0));
        assertEquals(11, mCardNumberEditText.updateSelectionIndex(13, 12, 0));
    }

    @Test
    public void updateSelectionIndex_whenSelectionInTheMiddle_increasesIndexOverASpace() {
        mCardNumberEditText.mCardBrand = Card.VISA;
        assertEquals(6, mCardNumberEditText.updateSelectionIndex(10, 4, 1));
    }

    @Test
    public void updateSelectionIndex_whenPastingIntoAGap_includesTheGapJump() {
        mCardNumberEditText.mCardBrand = Card.UNKNOWN;
        assertEquals(11, mCardNumberEditText.updateSelectionIndex(12, 8, 2));
    }

    @Test
    public void updateSelectionIndex_whenPastingOverAGap_includesTheGapJump() {
        mCardNumberEditText.mCardBrand = Card.UNKNOWN;
        assertEquals(9, mCardNumberEditText.updateSelectionIndex(12, 3, 5));
    }

    @Test
    public void updateSelectionIndex_whenIndexWouldGoOutOfBounds_setsToEndOfString() {
        mCardNumberEditText.mCardBrand = Card.VISA;
        // This case could happen when you paste over 5 digits with only 2
        assertEquals(3, mCardNumberEditText.updateSelectionIndex(3, 3, 2));
    }

    @Test
    public void setText_whenTextIsValidCommonLengthNumber_changesCardValidState() {
        mCardNumberEditText.setText(VALID_VISA_WITH_SPACES);

        assertTrue(mCardNumberEditText.isCardNumberValid());
    }

    @Test
    public void setText_whenTextIsValidAmExDinersClubLengthNumber_changesCardValidState() {
        mCardNumberEditText.setText(VALID_AMEX_WITH_SPACES);

        assertTrue(mCardNumberEditText.isCardNumberValid());
    }

    @Test
    public void setText_whenTextChangesFromValidToInvalid_changesCardValidState() {
        mCardNumberEditText.setText(VALID_VISA_WITH_SPACES);
        String mutable = mCardNumberEditText.getText().toString();
        // Removing a single character should make this invalid
        mutable = mutable.substring(0, 18);
        mCardNumberEditText.setText(mutable);
        assertFalse(mCardNumberEditText.isCardNumberValid());
    }
}
