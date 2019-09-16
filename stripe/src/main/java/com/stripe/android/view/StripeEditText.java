package com.stripe.android.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import com.google.android.material.textfield.TextInputEditText;
import com.stripe.android.R;

/**
 * Extension of {@link TextInputEditText} that listens for users pressing the delete key when
 * there is no text present. Google has actually made this
 * <a href="https://code.google.com/p/android/issues/detail?id=42904">somewhat difficult</a>,
 * but we listen here for hardware key presses, older Android soft keyboard delete presses,
 * and modern Google Keyboard delete key presses.
 */
public class StripeEditText extends AppCompatEditText {

    @Nullable private AfterTextChangedListener mAfterTextChangedListener;
    @Nullable private DeleteEmptyListener mDeleteEmptyListener;
    @Nullable private ColorStateList mCachedColorStateList;
    private boolean mShouldShowError;
    @ColorInt private int mDefaultErrorColor;
    @Nullable @ColorInt private Integer mErrorColor;

    @NonNull private final Handler mHandler;
    private String mErrorMessage;
    private ErrorMessageListener mErrorMessageListener;

    public StripeEditText(@NonNull Context context) {
        this(context, null);
    }

    public StripeEditText(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, androidx.appcompat.R.attr.editTextStyle);
    }

    public StripeEditText(@NonNull Context context, @Nullable AttributeSet attrs,
                          int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mHandler = new Handler();
        initView();
    }

    @Nullable
    public ColorStateList getCachedColorStateList() {
        return mCachedColorStateList;
    }

    /**
     * Gets whether or not the text should be displayed in error mode.
     *
     * @return the value of {@link #mShouldShowError}
     */
    public boolean getShouldShowError() {
        return mShouldShowError;
    }

    /**
     * @return the color used for error text.
     */
    @ColorInt
    public int getDefaultErrorColorInt() {
        // It's possible that we need to verify this value again
        // in case the user programmatically changes the text color.
        determineDefaultErrorColor();
        return mDefaultErrorColor;
    }

    @Nullable
    @Override
    public InputConnection onCreateInputConnection(@NonNull EditorInfo outAttrs) {
        final InputConnection inputConnection = super.onCreateInputConnection(outAttrs);
        if (inputConnection == null) {
            return null;
        }
        return new SoftDeleteInputConnection(inputConnection, true);
    }

    /**
     * Sets a listener that can react to changes in text, but only by reflecting the new
     * text in the field.
     *
     * @param afterTextChangedListener the {@link AfterTextChangedListener} to attach to this view
     */
    void setAfterTextChangedListener(
            @Nullable AfterTextChangedListener afterTextChangedListener) {
        mAfterTextChangedListener = afterTextChangedListener;
    }

    /**
     * Sets a listener that can react to the user attempting to delete the empty string.
     *
     * @param deleteEmptyListener the {@link DeleteEmptyListener} to attach to this view
     */
    void setDeleteEmptyListener(@Nullable DeleteEmptyListener deleteEmptyListener) {
        mDeleteEmptyListener = deleteEmptyListener;
    }

    void setErrorMessageListener(@Nullable ErrorMessageListener errorMessageListener) {
        mErrorMessageListener = errorMessageListener;
    }

    void setErrorMessage(@Nullable String errorMessage) {
        mErrorMessage = errorMessage;
    }

    /**
     * Sets the error text color on this {@link StripeEditText}.
     *
     * @param errorColor a {@link ColorInt}
     */
    public void setErrorColor(@ColorInt int errorColor) {
        mErrorColor = errorColor;
    }

    /**
     * Change the hint value of this control after a delay.
     *
     * @param hintResource the string resource for the hint to be set
     * @param delayMilliseconds a delay period, measured in milliseconds
     */
    public void setHintDelayed(@StringRes final int hintResource, long delayMilliseconds) {
        final Runnable hintRunnable = new Runnable() {
            @Override
            public void run() {
                setHint(hintResource);
            }
        };
        mHandler.postDelayed(hintRunnable, delayMilliseconds);
    }

    public void setHintDelayed(@NonNull final String hint, long delayMilliseconds) {
        final Runnable hintRunnable = new Runnable() {
            @Override
            public void run() {
                setHint(hint);
            }
        };
        mHandler.postDelayed(hintRunnable, delayMilliseconds);
    }

    /**
     * Sets whether or not the text should be put into "error mode," which displays
     * the text in an error color determined by the original text color.
     *
     * @param shouldShowError whether or not we should display text in an error state.
     */
    public void setShouldShowError(boolean shouldShowError) {
        if (mErrorMessage != null && mErrorMessageListener != null) {
            final String errorMessage = shouldShowError ? mErrorMessage : null;
            mErrorMessageListener.displayErrorMessage(errorMessage);
            mShouldShowError = shouldShowError;
        } else {
            mShouldShowError = shouldShowError;
            if (mShouldShowError) {
                setTextColor(mErrorColor != null ? mErrorColor : mDefaultErrorColor);
            } else {
                setTextColor(mCachedColorStateList);
            }

            refreshDrawableState();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // Passing a null token removes all callbacks and messages to the handler.
        mHandler.removeCallbacksAndMessages(null);
    }

    private void initView() {
        listenForTextChanges();
        listenForDeleteEmpty();
        determineDefaultErrorColor();
        mCachedColorStateList = getTextColors();
    }

    private void determineDefaultErrorColor() {
        mCachedColorStateList = getTextColors();
        final int color = mCachedColorStateList.getDefaultColor();

        @ColorRes final int defaultErrorColorResId;
        if (StripeColorUtils.isColorDark(color)) {
            // Note: if the _text_ color is dark, then this is a
            // light theme, and vice-versa.
            defaultErrorColorResId = R.color.error_text_light_theme;
        } else {
            defaultErrorColorResId = R.color.error_text_dark_theme;
        }

        mDefaultErrorColor = ContextCompat.getColor(getContext(), defaultErrorColorResId);
    }

    private void listenForTextChanges() {
        addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // purposefully not implemented.
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // purposefully not implemented.
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (mAfterTextChangedListener != null) {
                    mAfterTextChangedListener.onTextChanged(s.toString());
                }
            }
        });
    }

    private void listenForDeleteEmpty() {
        // This method works for hard keyboards and older phones.
        setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_DEL
                        && event.getAction() == KeyEvent.ACTION_DOWN
                        && mDeleteEmptyListener != null
                        && length() == 0) {
                    mDeleteEmptyListener.onDeleteEmpty();
                }
                return false;
            }
        });
    }

    interface DeleteEmptyListener {
        void onDeleteEmpty();
    }

    interface AfterTextChangedListener {
        void onTextChanged(String text);
    }

    interface ErrorMessageListener {
        void displayErrorMessage(@Nullable String message);
    }

    private class SoftDeleteInputConnection extends InputConnectionWrapper {

        private SoftDeleteInputConnection(@NonNull InputConnection target, boolean mutable) {
            super(target, mutable);
        }

        @Override
        public boolean deleteSurroundingText(int beforeLength, int afterLength) {
            // This method works on modern versions of Android with soft keyboard delete.
            if (getTextBeforeCursor(1, 0).length() == 0 && mDeleteEmptyListener != null) {
                mDeleteEmptyListener.onDeleteEmpty();
            }
            return super.deleteSurroundingText(beforeLength, afterLength);
        }
    }
}
