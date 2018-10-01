package com.stripe.android.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Size;
import android.support.annotation.VisibleForTesting;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.widget.EditText;

/**
 * An {@link EditText} that handles putting numbers around a central divider character.
 */
public class ExpiryDateEditText extends StripeEditText {

    static final int INVALID_INPUT = -1;
    private static final int MAX_INPUT_LENGTH = 5;

    private ExpiryDateEditListener mExpiryDateEditListener;
    private boolean mIsDateValid;

    public ExpiryDateEditText(Context context) {
        super(context);
        listenForTextChanges();
    }

    public ExpiryDateEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        listenForTextChanges();
    }

    public ExpiryDateEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        listenForTextChanges();
    }

    /**
     * Gets whether or not the date currently entered is valid and not yet
     * passed.
     *
     * @return {@code true} if the text entered represents a valid expiry date that has not
     * yet passed, and {@code false} if not.
     */
    public boolean isDateValid() {
        return mIsDateValid;
    }

    /**
     * Gets the expiry date displayed on this control if it is valid, or {@code null} if it is not.
     * The return value is given as a length-2 {@code int} array, where the first entry is the
     * two-digit month (from 01-12) and the second entry is the four-digit year (2017, not 17).
     *
     * @return an {@code int} array of the form {month, year} if the date is valid, or {@code null}
     * if it is not
     */
    @Nullable
    @Size(2)
    public int[] getValidDateFields() {
        if (!mIsDateValid) {
            return null;
        }

        int [] monthYearPair = new int[2];
        String rawNumericInput = getText().toString().replaceAll("/", "");
        String[] dateFields = DateUtils.separateDateStringParts(rawNumericInput);

        try {
            monthYearPair[0] = Integer.parseInt(dateFields[0]);
            monthYearPair[1] = DateUtils.convertTwoDigitYearToFour(Integer.parseInt(dateFields[1]));
        } catch (NumberFormatException numEx) {
            // Given that the date should already be valid when getting to this method, we should
            // not his this exception. Returning null to indicate error if we do.
            return null;
        }

        return monthYearPair;
    }

    public void setExpiryDateEditListener(ExpiryDateEditListener expiryDateEditListener) {
        mExpiryDateEditListener = expiryDateEditListener;
    }

    private void listenForTextChanges() {
        addTextChangedListener(new TextWatcher() {
            boolean ignoreChanges = false;
            int latestChangeStart;
            int latestInsertionSize;
            String[] parts = new String[2];

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (ignoreChanges) {
                    return;
                }
                latestChangeStart = start;
                latestInsertionSize = after;
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (ignoreChanges) {
                    return;
                }

                boolean inErrorState = false;

                String rawNumericInput = s.toString().replaceAll("/", "");

                if (rawNumericInput.length() == 1
                        && latestChangeStart == 0
                        && latestInsertionSize == 1) {
                    char first = rawNumericInput.charAt(0);
                    if (!(first == '0' || first == '1')) {
                        // If the first digit typed isn't 0 or 1, then it can't be a valid
                        // two-digit month. Hence, we assume the user is inputting a one-digit
                        // month. We bump it to the preferred input, so "4" becomes "04", which
                        // later in this method goes to "04/".
                        rawNumericInput = "0" + rawNumericInput;
                        latestInsertionSize++;
                    }
                } else if (rawNumericInput.length() == 2
                        && latestChangeStart == 2
                        && latestInsertionSize == 0) {
                    // This allows us to delete past the separator, so that if a user presses
                    // delete when the current string is "12/", the resulting string is "1," since
                    // we pretend that the "/" isn't really there. The case that we also want,
                    // where "12/3" + DEL => "12" is handled elsewhere.
                    rawNumericInput = rawNumericInput.substring(0, 1);
                }

                // Date input is MM/YY, so the separated parts will be {MM, YY}
                parts = DateUtils.separateDateStringParts(rawNumericInput);

                if (!DateUtils.isValidMonth(parts[0])) {
                    inErrorState = true;
                }

                StringBuilder formattedDateBuilder = new StringBuilder();
                formattedDateBuilder.append(parts[0]);
                // parts[0] is the two-digit month
                if ((parts[0].length() == 2 && latestInsertionSize > 0 && !inErrorState)
                        || rawNumericInput.length() > 2) {
                    formattedDateBuilder.append("/");
                }
                // parts[1] is the two-digit year
                formattedDateBuilder.append(parts[1]);

                String formattedDate = formattedDateBuilder.toString();
                int cursorPosition = updateSelectionIndex(
                        formattedDate.length(),
                        latestChangeStart,
                        latestInsertionSize,
                        MAX_INPUT_LENGTH);

                ignoreChanges = true;
                setText(formattedDate);
                setSelection(cursorPosition);
                ignoreChanges = false;
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Note: we want to show an error state if the month is invalid or the
                // final, complete date is in the past. We don't want to show an error state for
                // incomplete entries.
                boolean shouldShowError = false;
                if (parts[0].length() == 2 && !DateUtils.isValidMonth(parts[0])) {
                    // This covers the case where the user has entered a month of 15, for instance.
                    shouldShowError = true;
                }

                // Note that we have to check the parts array because afterTextChanged has odd
                // behavior when it comes to pasting, where a paste of "1212" triggers this
                // function for the strings "12/12" (what it actually becomes) and "1212",
                // so we might not be properly catching an error state.
                if (parts[0].length() == 2 && parts[1].length() == 2) {
                    boolean wasComplete = mIsDateValid;
                    updateInputValues(parts);
                    // Here, we have a complete date, so if we've made an invalid one, we want
                    // to show an error.
                    shouldShowError = !mIsDateValid;
                    if (!wasComplete && mIsDateValid && mExpiryDateEditListener != null) {
                        mExpiryDateEditListener.onExpiryDateComplete();
                    }
                } else {
                    mIsDateValid = false;
                }

                setShouldShowError(shouldShowError);
            }
        });
    }

    /**
     * Updates the selection index based on the current (pre-edit) index, and
     * the size change of the number being input.
     *
     * @param newLength the post-edit length of the string
     * @param editActionStart the position in the string at which the edit action starts
     * @param editActionAddition the number of new characters going into the string (zero for
     *                           delete)
     * @return an index within the string at which to put the cursor
     */
    @VisibleForTesting
    int updateSelectionIndex(
            int newLength,
            int editActionStart,
            int editActionAddition,
            int maxInputLength) { int newPosition, gapsJumped = 0;

        boolean skipBack = false;
        if (editActionStart <= 2 && editActionStart + editActionAddition >= 2) {
            gapsJumped = 1;
        }

        // editActionAddition can only be 0 if we are deleting,
        // so we need to check whether or not to skip backwards one space
        if (editActionAddition == 0 && editActionStart == 3) {
            skipBack = true;
        }

        newPosition = editActionStart + editActionAddition + gapsJumped;
        if (skipBack && newPosition > 0) {
            newPosition--;
        }
        int untruncatedPosition = newPosition <= newLength ? newPosition : newLength;
        return Math.min(maxInputLength, untruncatedPosition);
    }

    private void updateInputValues(@NonNull @Size(2) String[] parts) {
        int inputMonth;
        int inputYear;
        if (parts[0].length() != 2) {
            inputMonth = INVALID_INPUT;
        } else {
            try {
                inputMonth = Integer.parseInt(parts[0]);
            } catch (NumberFormatException numEx) {
                inputMonth = INVALID_INPUT;
            }
        }

        if (parts[1].length() != 2) {
            inputYear = INVALID_INPUT;
        } else {
            try {
                inputYear = DateUtils.convertTwoDigitYearToFour(Integer.parseInt(parts[1]));
            } catch (NumberFormatException numEx) {
                inputYear = INVALID_INPUT;
            }
        }

        mIsDateValid = DateUtils.isExpiryDataValid(inputMonth, inputYear);
    }

    interface ExpiryDateEditListener {
        void onExpiryDateComplete();
    }
}
