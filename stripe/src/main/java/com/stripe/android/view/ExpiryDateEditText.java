package com.stripe.android.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Size;
import android.support.annotation.VisibleForTesting;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;

import com.stripe.android.model.Card;
import com.stripe.android.util.DateUtils;

import java.util.Set;

/**
 * An {@link EditText} that handles putting numbers around a central divider character.
 */
public class ExpiryDateEditText extends EditText {

    static final int INVALID_INPUT = -1;

    private boolean mIsDateValid;
    private int mInputMonth = INVALID_INPUT;
    private int mInputYear = INVALID_INPUT;

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

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        return super.onCreateInputConnection(outAttrs);
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
                Log.d("expiry", String.format("BeforeTC. start: %d, count: %d, after %d", start, count, after));
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (ignoreChanges || s.length() == 0) {
                    return;
                }
                Log.d("expiry", String.format("OnTC. start: %d, before: %d, count %d", start, before, count));

                String rawNumericInput = s.toString().replaceAll("/", "");
                String[] parts = DateUtils.separateDateStringParts(rawNumericInput);

                StringBuilder formattedDateBuilder = new StringBuilder();
                formattedDateBuilder.append(parts[0]);
                if ((parts[0].length() == 2 && latestInsertionSize > 0)
                        || rawNumericInput.length() > 2) {
                    formattedDateBuilder.append("/");
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
//                if (s.length() == 2 && latestChangeStart < 2) {
//                    ignoreChanges = true;
//                    s.append('/');
//                    ExpiryDateEditText.this.setSelection(3);
//                    ignoreChanges = false;
//                }
            }
        });
    }

    void updateInputValues(@NonNull @Size(2) String[] parts) {
        mInputMonth = parts[0].length() == 2 ? Integer.parseInt(parts[0]) : INVALID_INPUT;
        mInputYear = parts[1].length() == 2
                ? DateUtils.convertTwoDigitYearToFour(Integer.parseInt(parts[1]))
                : INVALID_INPUT;
        mIsDateValid = DateUtils.isExpiryDataValid(mInputMonth, mInputYear);
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
}
