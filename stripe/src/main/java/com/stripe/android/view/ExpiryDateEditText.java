package com.stripe.android.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Size;
import android.support.annotation.VisibleForTesting;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import android.widget.EditText;

import com.stripe.android.model.Card;
import com.stripe.android.util.DateUtils;

import java.util.Set;

/**
 * An {@link EditText} that handles putting numbers around a central divider character.
 */
public class ExpiryDateEditText extends DeleteWatchEditText {

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

    public void setExpiryDateEditListener(ExpiryDateEditListener expiryDateEditListener) {
        mExpiryDateEditListener = expiryDateEditListener;
    }

    private void listenForTextChanges() {
        addTextChangedListener(new TextWatcher() {
            boolean ignoreChanges = false;
            int latestChangeStart;
            int latestInsertionSize;

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

                String rawNumericInput = s.toString().replaceAll("/", "");

                if(rawNumericInput.length() == 1 && latestChangeStart == 0 && latestInsertionSize == 1) {
                    char first = rawNumericInput.charAt(0);
                    if (!(first == '0' || first == '1')) {
                        // If the first digit typed isn't 0 or 1, then it can't be a valid
                        // two-digit month. Hence, we assume the user is inputting a one-digit
                        // month. We bump it to the preferred input, so "4" becomes "04", which
                        // later in this method goes to "04/".
                        rawNumericInput = "0" + rawNumericInput;
                        latestInsertionSize++;
                    }
                }

                // Date input is MM/YY, so the separated parts will be {MM, YY}
                String[] parts = DateUtils.separateDateStringParts(rawNumericInput);

                StringBuilder formattedDateBuilder = new StringBuilder();
                formattedDateBuilder.append(parts[0]);
                // parts[0] is the two-digit month
                if ((parts[0].length() == 2 && latestInsertionSize > 0)
                        || rawNumericInput.length() > 2) {
                    formattedDateBuilder.append("/");
                    // parts[1] is the two-digit year
                    formattedDateBuilder.append(parts[1]);
                }

                String formattedDate = formattedDateBuilder.toString();
                int cursorPosition = updateSelectionIndex(
                        formattedDate.length(),
                        latestChangeStart,
                        latestInsertionSize);

                ignoreChanges = true;
                setText(formattedDate);
                setSelection(cursorPosition);
                ignoreChanges = false;
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == MAX_INPUT_LENGTH) {
                    String completeText = s.toString();
                    String[] parts = {completeText.substring(0, 2),
                            completeText.substring(3, MAX_INPUT_LENGTH)};
                    boolean wasComplete = mIsDateValid;
                    updateInputValues(parts);
                    if (!wasComplete && mIsDateValid && mExpiryDateEditListener != null) {
                        mExpiryDateEditListener.onExpiryDateComplete();
                    }
                } else {
                    mIsDateValid = false;
                }
            }
        });
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

        return newPosition <= newLength ? newPosition : newLength;
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
