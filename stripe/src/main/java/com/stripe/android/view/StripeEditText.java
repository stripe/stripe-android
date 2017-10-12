package com.stripe.android.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.TextInputEditText;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;

import com.stripe.android.R;

import static com.stripe.android.view.ViewUtils.isColorDark;

/**
 * Extension of {@link AppCompatEditText} that listens for users pressing the delete key when
 * there is no text present. Google has actually made this
 * <a href="https://code.google.com/p/android/issues/detail?id=42904">somewhat difficult</a>,
 * but we listen here for hardware key presses, older Android soft keyboard delete presses,
 * and modern Google Keyboard delete key presses.
 */
public class StripeEditText extends TextInputEditText {

    @Nullable private AfterTextChangedListener mAfterTextChangedListener;
    @Nullable private DeleteEmptyListener mDeleteEmptyListener;
    @Nullable private ColorStateList mCachedColorStateList;
    private boolean mShouldShowError;
    @ColorRes private int mDefaultErrorColorResId;
    @ColorInt private int mErrorColor;

    private Handler mHandler;
    private String mErrorMessage;
    private ErrorMessageListener mErrorMessageListener;

    public StripeEditText(Context context) {
        super(context);
        initView();
    }

    public StripeEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public StripeEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
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
    @SuppressWarnings("deprecation")
    public int getDefaultErrorColorInt() {
        @ColorInt int errorColor;
        // It's possible that we need to verify this value again
        // in case the user programmatically changes the text color.
        determineDefaultErrorColor();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            errorColor = getResources().getColor(mDefaultErrorColorResId, null);
        } else {
            // Resources#getColor(int) is deprecated, but the replacement only exists in
            // SDK 23 and above.
            errorColor = getResources().getColor(mDefaultErrorColorResId);
        }

        return errorColor;
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        InputConnection inputConnection = super.onCreateInputConnection(outAttrs);
        if (inputConnection == null) {
            return null;
        }
        return new SoftDeleteInputConnection(super.onCreateInputConnection(outAttrs), true);
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

    /**
     * Sets whether or not the text should be put into "error mode," which displays
     * the text in an error color determined by the original text color.
     *
     * @param shouldShowError whether or not we should display text in an error state.
     */
    @SuppressWarnings("deprecation")
    public void setShouldShowError(boolean shouldShowError) {
        if (mErrorMessage != null && mErrorMessageListener != null) {
            String errorMessage = shouldShowError ? mErrorMessage : null;
            mErrorMessageListener.displayErrorMessage(errorMessage);
            mShouldShowError = shouldShowError;
        } else {
            mShouldShowError = shouldShowError;
            if (mShouldShowError) {
                setTextColor(mErrorColor);
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
        mHandler = new Handler();
        listenForTextChanges();
        listenForDeleteEmpty();
        determineDefaultErrorColor();
        mCachedColorStateList = getTextColors();
    }

    private void determineDefaultErrorColor() {
        mCachedColorStateList = getTextColors();
        int color = mCachedColorStateList.getDefaultColor();
        if (isColorDark(color)) {
            // Note: if the _text_ color is dark, then this is a
            // light theme, and vice-versa.
            mDefaultErrorColorResId = R.color.error_text_light_theme;
        } else {
            mDefaultErrorColorResId = R.color.error_text_dark_theme;
        }
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

        SoftDeleteInputConnection(InputConnection target, boolean mutable) {
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
