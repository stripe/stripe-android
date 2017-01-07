package com.stripe.android.view;

import android.content.Context;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.widget.EditText;

import com.stripe.android.model.Card;
import com.stripe.android.util.StripeTextUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * An {@link EditText} that handles spacing out the digits of a credit card.
 */
public class CardNumberEditText extends EditText {

    private static final int MAX_LENGTH_COMMON = 19;
    // Note that AmEx and Diners Club have the same length
    // because Diners Club has one more space, but one less digit.
    private static final int MAX_LENGTH_AMEX_DINERS = 17;

    private static final Integer[] SPACES_ARRAY_COMMON = {4, 9, 14};
    private static final Set<Integer> SPACE_SET_COMMON =
            new HashSet<>(Arrays.asList(SPACES_ARRAY_COMMON));

    private static final Integer[] SPACES_ARRAY_AMEX = {4, 11};
    private static final Set<Integer> SPACE_SET_AMEX =
            new HashSet<>(Arrays.asList(SPACES_ARRAY_AMEX));

    @Card.CardBrand private String mCardBrand = Card.UNKNOWN;
    private int mLengthMax = 19;

    public CardNumberEditText(Context context) {
        super(context);
        listenForTextChanges();
    }

    public CardNumberEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        listenForTextChanges();
    }

    public CardNumberEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        listenForTextChanges();
    }

    private void listenForTextChanges() {
        addTextChangedListener(new TextWatcher() {
            boolean ignoreChanges = false;
            int beforeStringLength = 0;
            int beforeSelectionIndex = 0;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (!ignoreChanges) {
                    beforeStringLength = getText().toString().length();
                    beforeSelectionIndex = getSelectionEnd();
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (ignoreChanges) {
                    return;
                }

                if (start < 4) {
                    updateCardBrand(s.toString());
                    // update the card icon here
                }

                if (beforeSelectionIndex > 16) {
                    // no need to do formatting if we're past all of the spaces.
                    return;
                }

                String spacelessNumber = StripeTextUtils.convertToSpacelessNumber(s.toString());
                if (spacelessNumber == null) {
                    return;
                }

                String[] cardParts = StripeTextUtils.separateCardNumberGroups(
                        spacelessNumber, mCardBrand);
                StringBuilder formattedNumberBuilder = new StringBuilder();
                for (int i = 0; i < cardParts.length; i++) {
                    if (cardParts[i] == null) {
                        break;
                    }

                    if (i != 0) {
                        formattedNumberBuilder.append(' ');
                    }
                    formattedNumberBuilder.append(cardParts[i]);
                }

                String formattedNumber = formattedNumberBuilder.toString();
                int cursorPosition = updateSelectionIndex(
                        beforeSelectionIndex,
                        beforeStringLength,
                        formattedNumber.length());

                ignoreChanges = true;
                setText(formattedNumber);
                setSelection(cursorPosition);
                ignoreChanges = false;

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private int updateSelectionIndex(int oldPosition,
                                     int oldLength,
                                     int newLength) {

        int increment, newPosition;
        Set<Integer> spaceSet = Card.AMERICAN_EXPRESS.equals(mCardBrand)
                ? SPACE_SET_AMEX
                : SPACE_SET_COMMON;

        boolean growing = newLength > oldLength;

        if (growing) {
            increment = spaceSet.contains(oldPosition) ? 2 : 1;
            newPosition = oldPosition + increment;
            return newPosition <= newLength ? newPosition : newLength;
        } else {
            increment = spaceSet.contains(oldPosition - 2) ? 2 : 1;
            newPosition = oldPosition - increment;
            return newPosition > 0 ? newPosition : 0;
        }
    }

    private void updateCardBrand(String partialNumber) {
        @Card.CardBrand String oldBrand = mCardBrand;
        mCardBrand = StripeTextUtils.getPossibleCardType(partialNumber);
        if (mCardBrand.equals(oldBrand)) {
            return;
        }

        int oldLength = mLengthMax;
        mLengthMax = getLengthForBrand(mCardBrand);
        if (oldLength == mLengthMax) {
            return;
        }

        setFilters(new InputFilter[] {new InputFilter.LengthFilter(mLengthMax)});
    }

    private static int getLengthForBrand(@Card.CardBrand String cardBrand) {
        if (Card.AMERICAN_EXPRESS.equals(cardBrand) || Card.DINERS_CLUB.equals(cardBrand)) {
            return MAX_LENGTH_AMEX_DINERS;
        } else {
            return MAX_LENGTH_COMMON;
        }
    }
}
