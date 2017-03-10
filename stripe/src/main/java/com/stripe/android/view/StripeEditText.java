package com.stripe.android.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatEditText;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;

import com.stripe.android.R;

/**
 * Extension of {@link AppCompatEditText} that listens for users pressing the delete key when there is
 * no text present. Google has actually made this
 * <a href="https://code.google.com/p/android/issues/detail?id=42904">somewhat difficult</a>,
 * but we listen here for hardware key presses, older Android soft keyboard delete presses,
 * and modern Google Keyboard delete key presses.
 */
public class StripeEditText extends AppCompatEditText {

    @Nullable private DeleteEmptyListener mDeleteEmptyListener;
    @Nullable private ColorStateList mCachedColorStateList;
    private boolean mShouldShowError;
    @ColorRes private int mDefaultErrorColorResId;
    @ColorInt private int mErrorColor;

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

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        return new SoftDeleteInputConnection(super.onCreateInputConnection(outAttrs), true);
    }

    /**
     * Sets a listener that can react to the user attempting to delete the empty string.
     *
     * @param deleteEmptyListener the {@link DeleteEmptyListener} to attach to this view
     */
    public void setDeleteEmptyListener(DeleteEmptyListener deleteEmptyListener) {
        mDeleteEmptyListener = deleteEmptyListener;
    }

    /**
     * Sets whether or not the text should be put into "error mode," which displays
     * the text in an error color determined by the original text color.
     *
     * @param shouldShowError whether or not we should display text in an error state.
     */
    @SuppressWarnings("deprecation")
    public void setShouldShowError(boolean shouldShowError) {
        mShouldShowError = shouldShowError;
        if (mShouldShowError) {
            setTextColor(mErrorColor);
        } else {
            setTextColor(mCachedColorStateList);
        }

        refreshDrawableState();
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

    @ColorInt
    @SuppressWarnings("deprecation")
    int getDefaultErrorColorInt() {
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

    void setErrorColor(@ColorInt int errorColor) {
        mErrorColor = errorColor;
    }

    /**
     * A crude mechanism by which we check whether or not a color is "dark."
     * This is subject to much interpretation, but we attempt to follow traditional
     * design standards.
     *
     * @param color an integer representation of a color
     * @return {@code true} if the color is "dark," else {@link false}
     */
    static boolean isColorDark(@ColorInt int color){
        // Forumla comes from W3C standards and conventional theory
        // about how to calculate the "brightness" of a color, often
        // thought of as how far along the spectrum from white to black the
        // grayscale version would be.
        // See https://www.w3.org/TR/AERT#color-contrast and
        // http://paulbourke.net/texture_colour/colourspace/ for further reading.
        double luminescence = 0.299*Color.red(color)
                + 0.587*Color.green(color)
                + 0.114* Color.blue(color);

        // Because the colors are all hex integers.
        double luminescencePercentage = luminescence / 255;
        if (luminescencePercentage > 0.5) {
            return false;
        } else {
            return true;
        }
    }

    private void initView() {
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

    /**
     * A class that can listen for when the user attempts to delete the empty string.
     */
    public interface DeleteEmptyListener {
        void onDeleteEmpty();
    }

    private class SoftDeleteInputConnection extends InputConnectionWrapper {

        public SoftDeleteInputConnection(InputConnection target, boolean mutable) {
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
