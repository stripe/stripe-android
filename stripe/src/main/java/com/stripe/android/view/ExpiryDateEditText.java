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
public class ExpiryDateEditText extends EditText {

    static final int INVALID_INPUT = -1;

    private ExpiryDateEditListener mExpiryDateEditListener;
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

    public void setExpiryDateEditListener(ExpiryDateEditListener expiryDateEditListener) {
        mExpiryDateEditListener = expiryDateEditListener;
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        return new SoftDeleteInputConnection(super.onCreateInputConnection(outAttrs), true);
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
                if (ignoreChanges || s.length() == 0) {
                    return;
                }

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
                if (s.length() == 5) {
                    String completeText = s.toString();
                    String[] parts = {completeText.substring(0, 2), completeText.substring(3,5)};
                    boolean wasComplete = mIsDateValid;
                    updateInputValues(parts);
                    if (!wasComplete && mIsDateValid && mExpiryDateEditListener != null) {
                        mExpiryDateEditListener.onExpiryDateComplete();
                    }
                }
            }
        });
    }

    void updateInputValues(@NonNull @Size(2) String[] parts) {
        if (parts[0].length() != 2) {
            mInputMonth = INVALID_INPUT;
        } else {
            try {
                mInputMonth = Integer.parseInt(parts[0]);
            } catch (NumberFormatException numEx) {
                mInputMonth = INVALID_INPUT;
            }
        }

        if (parts[1].length() != 2) {
            mInputYear = INVALID_INPUT;
        } else {
            try {
                mInputYear = DateUtils.convertTwoDigitYearToFour(Integer.parseInt(parts[1]));
            } catch (NumberFormatException numEx) {
                mInputYear = INVALID_INPUT;
            }
        }

        mIsDateValid = DateUtils.isExpiryDataValid(mInputMonth, mInputYear);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d("keyspy", String.format("OKD %d", keyCode));
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        Log.d("keyspy", String.format("OnPreIme %d", keyCode));
        return super.onKeyPreIme(keyCode, event);
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

    interface ExpiryDateEditListener {
        void onExpiryDateComplete();
        void onDeleteEmpty();
    }

    private class SoftDeleteInputConnection extends InputConnectionWrapper {

        public SoftDeleteInputConnection(InputConnection target, boolean mutable) {
            super(target, mutable);
        }

        @Override
        public boolean performEditorAction(int editorAction) {
            Log.d("keyspy", String.format("keyspy", "performEditorAction %d", editorAction));
            return super.performEditorAction(editorAction);
        }

        @Override
        public boolean deleteSurroundingText(int beforeLength, int afterLength) {
            Log.d("keyspy", String.format("delSur - before: %d, after: %d", beforeLength, afterLength));
            if (getTextBeforeCursor(1, 0).length() == 0 && mExpiryDateEditListener != null) {
                Log.d("keyspy", "I want to move");
                mExpiryDateEditListener.onDeleteEmpty();
            }
            return super.deleteSurroundingText(beforeLength, afterLength);
        }

        @Override
        public boolean sendKeyEvent(KeyEvent event) {
            if (event.getAction() == KeyEvent.ACTION_DOWN
                    && event.getKeyCode() == KeyEvent.KEYCODE_DEL
                    && getText().length() == 0
                    && mExpiryDateEditListener != null) {
                mExpiryDateEditListener.onDeleteEmpty();
            }
            return super.sendKeyEvent(event);
        }
    }
}
