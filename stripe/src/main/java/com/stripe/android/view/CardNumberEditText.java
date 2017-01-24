package com.stripe.android.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.widget.EditText;

import com.stripe.android.model.Card;
import com.stripe.android.util.CardUtils;
import com.stripe.android.util.StripeTextUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * An {@link EditText} that handles spacing out the digits of a credit card.
 */
public class CardNumberEditText extends StripeEditText {

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

    @VisibleForTesting @Card.CardBrand String mCardBrand = Card.UNKNOWN;
    private CardNumberCompleteListener mCardNumberCompleteListener;
    private int mLengthMax = 19;
    private boolean mIgnoreChanges = false;
    private boolean mIsCardNumberValid = false;

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

    /**
     * Check whether or not the card number is valid
     * @return
     */
    public boolean isCardNumberValid() {
        return mIsCardNumberValid;
    }

    void setCardNumberCompleteListener(@NonNull CardNumberCompleteListener listener) {
        mCardNumberCompleteListener = listener;
    }

    /**
     * Updates the selection index based on the current (pre-edit) index, and
     * the size change of the number being input.
     *
     * @param newLength the post-edit length of the string
     * @param editActionStart the position in the string at which the edit action starts
     * @param editActionAddition the number of new characters going into the string (zero for delete)
     * @return an index within the string at which to put the cursor
     */
    @VisibleForTesting
    int updateSelectionIndex(
            int newLength,
            int editActionStart,
            int editActionAddition) {
        int newPosition, gapsJumped = 0;
        Set<Integer> gapSet = Card.AMERICAN_EXPRESS.equals(mCardBrand)
                ? SPACE_SET_AMEX
                : SPACE_SET_COMMON;
        boolean skipBack = false;
        for (Integer gap : gapSet) {
            if (editActionStart <= gap && editActionStart + editActionAddition > gap) {
                gapsJumped++;
            }

            // editActionAddition can only be 0 if we are deleting,
            // so we need to check whether or not to skip backwards one space
            if (editActionAddition == 0 && editActionStart == gap + 1) {
                skipBack = true;
            }
        }

        newPosition = editActionStart + editActionAddition + gapsJumped;
        if (skipBack && newPosition > 0) {
            newPosition--;
        }

        return newPosition <= newLength ? newPosition : newLength;
    }

    private void listenForTextChanges() {
        addTextChangedListener(new TextWatcher() {
            int latestChangeStart;
            int latestInsertionSize;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (!mIgnoreChanges) {
                    latestChangeStart = start;
                    latestInsertionSize = after;
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (mIgnoreChanges) {
                    return;
                }

                if (start < 4) {
                    updateCardBrand(s.toString());
                    // update the card icon here
                }

                if (start > 16) {
                    // no need to do formatting if we're past all of the spaces.
                    return;
                }

                String spacelessNumber = StripeTextUtils.removeSpacesAndHyphens(s.toString());
                if (spacelessNumber == null) {
                    return;
                }

                String[] cardParts = CardUtils.separateCardNumberGroups(
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
                        formattedNumber.length(),
                        latestChangeStart,
                        latestInsertionSize);

                mIgnoreChanges = true;
                setText(formattedNumber);
                setSelection(cursorPosition);
                mIgnoreChanges = false;
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == mLengthMax) {
                    boolean before = mIsCardNumberValid;
                    mIsCardNumberValid = CardUtils.isValidCardNumber(s.toString());
                    if (!before && mIsCardNumberValid && mCardNumberCompleteListener != null) {
                        mCardNumberCompleteListener.onCardNumberComplete();
                    }
                } else {
                    mIsCardNumberValid = false;
                }
            }
        });
    }

    private void updateCardBrand(String partialNumber) {
        @Card.CardBrand String oldBrand = mCardBrand;
        mCardBrand = CardUtils.getPossibleCardType(partialNumber);
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

    interface CardNumberCompleteListener {
        void onCardNumberComplete();
    }
}
