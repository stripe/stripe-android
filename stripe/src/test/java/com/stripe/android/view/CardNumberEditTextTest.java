package com.stripe.android.view;

import com.stripe.android.BuildConfig;
import com.stripe.android.model.Card;
import com.stripe.android.testharness.ViewTestUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import static com.stripe.android.view.CardInputTestActivity.VALID_AMEX_NO_SPACES;
import static com.stripe.android.view.CardInputTestActivity.VALID_AMEX_WITH_SPACES;
import static com.stripe.android.view.CardInputTestActivity.VALID_DINERS_CLUB_NO_SPACES;
import static com.stripe.android.view.CardInputTestActivity.VALID_DINERS_CLUB_WITH_SPACES;
import static com.stripe.android.view.CardInputTestActivity.VALID_VISA_NO_SPACES;
import static com.stripe.android.view.CardInputTestActivity.VALID_VISA_WITH_SPACES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Test class for {@link CardNumberEditText}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 25)
public class CardNumberEditTextTest {

    @Mock CardNumberEditText.CardNumberCompleteListener mCardNumberCompleteListener;
    @Mock CardNumberEditText.CardBrandChangeListener mCardBrandChangeListener;
    private CardNumberEditText mCardNumberEditText;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        ActivityController<CardInputTestActivity> activityController =
                Robolectric.buildActivity(CardInputTestActivity.class).create().start();

        mCardNumberEditText =
                activityController.get().getCardNumberEditText();
        mCardNumberEditText.setText("");
        mCardNumberEditText.setCardNumberCompleteListener(mCardNumberCompleteListener);
        mCardNumberEditText.setCardBrandChangeListener(mCardBrandChangeListener);
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
        verify(mCardNumberCompleteListener, times(1)).onCardNumberComplete();
    }

    @Test
    public void setText_whenTextIsSpacelessValidNumber_changesToSpaceNumberAndValidates() {
        mCardNumberEditText.setText(VALID_VISA_NO_SPACES);

        assertTrue(mCardNumberEditText.isCardNumberValid());
        verify(mCardNumberCompleteListener, times(1)).onCardNumberComplete();
    }

    @Test
    public void setText_whenTextIsValidAmExDinersClubLengthNumber_changesCardValidState() {
        mCardNumberEditText.setText(VALID_AMEX_WITH_SPACES);

        assertTrue(mCardNumberEditText.isCardNumberValid());
        verify(mCardNumberCompleteListener, times(1)).onCardNumberComplete();
    }

    @Test
    public void setText_whenTextChangesFromValidToInvalid_changesCardValidState() {
        mCardNumberEditText.setText(VALID_VISA_WITH_SPACES);
        // Simply setting the value interacts with this mock once -- that is tested elsewhere
        reset(mCardNumberCompleteListener);

        String mutable = mCardNumberEditText.getText().toString();
        // Removing a single character should make this invalid
        mutable = mutable.substring(0, 18);
        mCardNumberEditText.setText(mutable);
        assertFalse(mCardNumberEditText.isCardNumberValid());
        verifyZeroInteractions(mCardNumberCompleteListener);
    }

    @Test
    public void setText_whenTextIsInvalidCommonLengthNumber_doesNotNotifyListener() {
        // This creates a full-length but not valid number: 4242 4242 4242 4243
        String almostValid = VALID_VISA_WITH_SPACES.substring(0, 18) + "3";
        mCardNumberEditText.setText(almostValid);
        assertFalse(mCardNumberEditText.isCardNumberValid());
        verifyZeroInteractions(mCardNumberCompleteListener);
    }

    @Test
    public void whenNotFinishedTyping_doesNotSetErrorValue() {
        // We definitely shouldn't start out in an error state.
        assertFalse(mCardNumberEditText.getShouldShowError());

        mCardNumberEditText.append("123");
        assertFalse(mCardNumberEditText.getShouldShowError());
    }

    @Test
    public void finishTypingCommonLengthCardNumber_whenValidCard_doesNotSetErrorValue() {
        String almostThere = VALID_VISA_WITH_SPACES.substring(0, 18);
        mCardNumberEditText.setText(almostThere);
        assertFalse(mCardNumberEditText.getShouldShowError());
        // We now have the valid 4242 Visa
        mCardNumberEditText.append("2");
        assertFalse(mCardNumberEditText.getShouldShowError());
    }

    @Test
    public void finishTypingCommonLengthCardNumber_whenInvalidCard_setsErrorValue() {
        String almostThere = VALID_VISA_WITH_SPACES.substring(0, 18);
        mCardNumberEditText.setText(almostThere);
        // This makes the number officially invalid
        mCardNumberEditText.append("3");
        assertTrue(mCardNumberEditText.getShouldShowError());
    }

    @Test
    public void finishTypingInvalidCardNumber_whenFollowedByDelete_setsErrorBackToFalse() {
        String notQuiteValid = VALID_VISA_WITH_SPACES.substring(0, 18) + "3";
        mCardNumberEditText.setText(notQuiteValid);
        assertTrue(mCardNumberEditText.getShouldShowError());

        // Now that we're in an error state, back up by one
        ViewTestUtils.sendDeleteKeyEvent(mCardNumberEditText);
        assertFalse(mCardNumberEditText.getShouldShowError());
    }

    @Test
    public void finishTypingDinersClub_whenInvalid_setsErrorValueAndRemovesItAppropriately() {
        String notQuiteValid = VALID_DINERS_CLUB_WITH_SPACES.substring(0, 16) + "3";
        mCardNumberEditText.setText(notQuiteValid);
        assertTrue(mCardNumberEditText.getShouldShowError());

        // Now that we're in an error state, back up by one
        ViewTestUtils.sendDeleteKeyEvent(mCardNumberEditText);
        assertFalse(mCardNumberEditText.getShouldShowError());

        mCardNumberEditText.append("4");
        assertFalse(mCardNumberEditText.getShouldShowError());
    }

    @Test
    public void finishTypingAmEx_whenInvalid_setsErrorValueAndRemovesItAppropriately() {
        String notQuiteValid = VALID_AMEX_WITH_SPACES.substring(0, 16) + "3";
        mCardNumberEditText.setText(notQuiteValid);
        assertTrue(mCardNumberEditText.getShouldShowError());

        // Now that we're in an error state, back up by one
        ViewTestUtils.sendDeleteKeyEvent(mCardNumberEditText);
        assertFalse(mCardNumberEditText.getShouldShowError());

        mCardNumberEditText.append("5");
        assertFalse(mCardNumberEditText.getShouldShowError());
    }

    @Test
    public void setCardBrandChangeListener_callsSetCardBrand() {
        verify(mCardBrandChangeListener, times(1)).onCardBrandChanged(Card.UNKNOWN);
    }

    @Test
    public void addVisaPrefix_callsBrandListener() {
        // We reset because just attaching the listener calls the method once.
        clearInvocations(mCardBrandChangeListener);
        // There is only one Visa Prefix.
        mCardNumberEditText.append(Card.PREFIXES_VISA[0]);
        verify(mCardBrandChangeListener, times(1)).onCardBrandChanged(Card.VISA);
    }

    @Test
    public void addAmExPrefix_callsBrandListener() {
        for (String prefix : Card.PREFIXES_AMERICAN_EXPRESS) {
            // Reset inside the loop so we don't count each prefix
            clearInvocations(mCardBrandChangeListener);
            mCardNumberEditText.append(prefix);
            verify(mCardBrandChangeListener, times(1)).onCardBrandChanged(Card.AMERICAN_EXPRESS);
            mCardNumberEditText.setText("");
        }
    }

    @Test
    public void addDinersClubPrefix_callsBrandListener() {
        for (String prefix : Card.PREFIXES_DINERS_CLUB) {
            clearInvocations(mCardBrandChangeListener);
            mCardNumberEditText.append(prefix);
            verify(mCardBrandChangeListener, times(1)).onCardBrandChanged(Card.DINERS_CLUB);
            mCardNumberEditText.setText("");
        }
    }

    @Test
    public void addDiscoverPrefix_callsBrandListener() {
        for (String prefix : Card.PREFIXES_DISCOVER) {
            clearInvocations(mCardBrandChangeListener);
            mCardNumberEditText.append(prefix);
            verify(mCardBrandChangeListener, times(1)).onCardBrandChanged(Card.DISCOVER);
            mCardNumberEditText.setText("");
        }
    }

    @Test
    public void addMasterCardPrefix_callsBrandListener() {
        for (String prefix : Card.PREFIXES_MASTERCARD) {
            clearInvocations(mCardBrandChangeListener);
            mCardNumberEditText.append(prefix);
            verify(mCardBrandChangeListener, times(1)).onCardBrandChanged(Card.MASTERCARD);
            mCardNumberEditText.setText("");
        }
    }

    @Test
    public void addJCBPrefix_callsBrandListener() {
        for (String prefix : Card.PREFIXES_JCB) {
            clearInvocations(mCardBrandChangeListener);
            mCardNumberEditText.append(prefix);
            verify(mCardBrandChangeListener, times(1)).onCardBrandChanged(Card.JCB);
            mCardNumberEditText.setText("");
        }
    }

    @Test
    public void enterCompleteNumberInParts_onlyCallsBrandListenerOnce() {
        clearInvocations(mCardBrandChangeListener);
        String prefix = VALID_AMEX_WITH_SPACES.substring(0, 2);
        String suffix = VALID_AMEX_WITH_SPACES.substring(2);
        mCardNumberEditText.append(prefix);
        mCardNumberEditText.append(suffix);
        verify(mCardBrandChangeListener, times(1)).onCardBrandChanged(Card.AMERICAN_EXPRESS);
    }

    @Test
    public void enterBrandPrefix_thenDelete_callsUpdateWithUnknown() {
        clearInvocations(mCardBrandChangeListener);
        String dinersPrefix = Card.PREFIXES_DINERS_CLUB[0];
        // All the Diners Club prefixes are longer than one character, but I specifically want
        // to test that a nonempty string still triggers this action, so this test will fail if
        // the Diners Club prefixes are ever changed.
        assertTrue(dinersPrefix.length() > 1);

        mCardNumberEditText.append(dinersPrefix);
        verify(mCardBrandChangeListener, times(1)).onCardBrandChanged(Card.DINERS_CLUB);

        ViewTestUtils.sendDeleteKeyEvent(mCardNumberEditText);
        verify(mCardBrandChangeListener, times(1)).onCardBrandChanged(Card.UNKNOWN);
    }

    @Test
    public void enterBrandPrefix_thenClearAllText_callsUpdateWithUnknown() {
        clearInvocations(mCardBrandChangeListener);
        String prefixVisa = Card.PREFIXES_VISA[0];
        mCardNumberEditText.append(prefixVisa);
        verify(mCardBrandChangeListener, times(1)).onCardBrandChanged(Card.VISA);

        // Just adding some other text. Not enough to invalidate the card or complete it.
        mCardNumberEditText.append("123");
        verify(mCardBrandChangeListener, times(0)).onCardBrandChanged(Card.UNKNOWN);

        // This simulates the user selecting all text and deleting it.
        mCardNumberEditText.setText("");

        verify(mCardBrandChangeListener, times(1)).onCardBrandChanged(Card.UNKNOWN);
    }

    @Test
    public void getCardNumber_whenValidCard_returnsCardNumberWithoutSpaces() {
        mCardNumberEditText.setText(VALID_VISA_WITH_SPACES);
        assertEquals(VALID_VISA_NO_SPACES, mCardNumberEditText.getCardNumber());

        mCardNumberEditText.setText(VALID_AMEX_WITH_SPACES);
        assertEquals(VALID_AMEX_NO_SPACES, mCardNumberEditText.getCardNumber());

        mCardNumberEditText.setText(VALID_DINERS_CLUB_WITH_SPACES);
        assertEquals(VALID_DINERS_CLUB_NO_SPACES, mCardNumberEditText.getCardNumber());
    }

    @Test
    public void getCardNumber_whenIncompleteCard_returnsNull() {
        mCardNumberEditText.setText(
                VALID_DINERS_CLUB_WITH_SPACES
                        .substring(0, VALID_DINERS_CLUB_WITH_SPACES.length() - 2));
        assertNull(mCardNumberEditText.getCardNumber());
    }

    @Test
    public void getCardNumber_whenInvalidCardNumber_returnsNull() {
        String almostVisa =
                VALID_VISA_WITH_SPACES.substring(0, VALID_VISA_WITH_SPACES.length() - 1);
        almostVisa += "3"; // creates the 4242 4242 4242 4243 bad number
        mCardNumberEditText.setText(almostVisa);
        assertNull(mCardNumberEditText.getCardNumber());
    }

    @Test
    public void getCardNumber_whenValidNumberIsChangedToInvalid_returnsNull() {
        mCardNumberEditText.setText(VALID_AMEX_WITH_SPACES);
        ViewTestUtils.sendDeleteKeyEvent(mCardNumberEditText);

        assertNull(mCardNumberEditText.getCardNumber());
    }
}
