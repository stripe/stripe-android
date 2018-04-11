package com.stripe.android.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.VisibleForTesting;
import android.support.design.widget.TextInputLayout;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import com.stripe.android.CardUtils;
import com.stripe.android.R;
import com.stripe.android.model.Card;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.stripe.android.model.Card.BRAND_RESOURCE_MAP;
import static com.stripe.android.view.CardInputListener.FocusField.FOCUS_CARD;
import static com.stripe.android.view.CardInputListener.FocusField.FOCUS_CVC;
import static com.stripe.android.view.CardInputListener.FocusField.FOCUS_EXPIRY;
import static com.stripe.android.view.CardInputListener.FocusField.FOCUS_POSTAL;

/**
 * A multiline card input widget using the support design library's {@link TextInputLayout}
 * to match Material Design.
 */
public class CardMultilineWidget extends LinearLayout {

    static final String CARD_MULTILINE_TOKEN = "CardMultilineView";
    static final long CARD_NUMBER_HINT_DELAY = 120L;
    static final long COMMON_HINT_DELAY = 90L;

    private @Nullable CardInputListener mCardInputListener;
    private CardNumberEditText mCardNumberEditText;
    private ExpiryDateEditText mExpiryDateEditText;
    private StripeEditText mCvcEditText;
    private StripeEditText mPostalCodeEditText;
    private TextInputLayout mCardNumberTextInputLayout;
    private TextInputLayout mExpiryTextInputLayout;
    private TextInputLayout mCvcTextInputLayout;
    private TextInputLayout mPostalInputLayout;

    private boolean mIsEnabled;
    private boolean mShouldShowPostalCode;
    private boolean mHasAdjustedDrawable;

    private @Card.CardBrand String mCardBrand;
    private @ColorInt int mTintColorInt;

    public CardMultilineWidget(Context context) {
        super(context);
        initView(null);
    }

    public CardMultilineWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(attrs);
    }

    public CardMultilineWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(attrs);
    }

    @VisibleForTesting
    CardMultilineWidget(Context context, boolean shouldShowPostalCode) {
        super(context);
        mShouldShowPostalCode = shouldShowPostalCode;
        initView(null);
    }

    /**
     * Clear all entered data and hide all error messages.
     */
    public void clear() {
        mCardNumberEditText.setText("");
        mExpiryDateEditText.setText("");
        mCvcEditText.setText("");
        mPostalCodeEditText.setText("");
        mCardNumberEditText.setShouldShowError(false);
        mExpiryDateEditText.setShouldShowError(false);
        mCvcEditText.setShouldShowError(false);
        mPostalCodeEditText.setShouldShowError(false);
        updateBrand(Card.UNKNOWN);
    }

    /**
     * @param cardInputListener A {@link CardInputListener} to be notified of changes
     *                          to the user's focused field
     */
    public void setCardInputListener(@Nullable CardInputListener cardInputListener) {
        mCardInputListener = cardInputListener;
    }

    /**
     * Gets a {@link Card} object from the user input, if all fields are valid. If not, returns
     * {@code null}.
     *
     * @return a valid {@link Card} object based on user input, or {@code null} if any field is
     * invalid
     */
    @Nullable
    public Card getCard() {
        if (validateAllFields()) {
            String cardNumber = mCardNumberEditText.getCardNumber();
            int[] cardDate = mExpiryDateEditText.getValidDateFields();
            String cvcValue = mCvcEditText.getText().toString();

            Card card = new Card(cardNumber, cardDate[0], cardDate[1], cvcValue);
            if (mShouldShowPostalCode) {
                card.setAddressZip(mPostalCodeEditText.getText().toString());
            }
            return card.addLoggingToken(CARD_MULTILINE_TOKEN);
        }

        return null;
    }

    /**
     * Validates all fields and shows error messages if appropriate.
     *
     * @return {@code true} if all shown fields are valid, {@code false} otherwise
     */
    public boolean validateAllFields() {
        boolean cardNumberIsValid =
                CardUtils.isValidCardNumber(mCardNumberEditText.getCardNumber());
        boolean expiryIsValid = mExpiryDateEditText.getValidDateFields() != null &&
                mExpiryDateEditText.isDateValid();
        boolean cvcIsValid = isCvcLengthValid();
        mCardNumberEditText.setShouldShowError(!cardNumberIsValid);
        mExpiryDateEditText.setShouldShowError(!expiryIsValid);
        mCvcEditText.setShouldShowError(!cvcIsValid);
        boolean postalCodeIsValidOrGone;
        if (mShouldShowPostalCode) {
            postalCodeIsValidOrGone = isPostalCodeMaximalLength(true,
                    mPostalCodeEditText.getText().toString());
            mPostalCodeEditText.setShouldShowError(!postalCodeIsValidOrGone);
        } else {
            postalCodeIsValidOrGone = true;
        }

        return cardNumberIsValid
                && expiryIsValid
                && cvcIsValid
                && postalCodeIsValidOrGone;
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (hasWindowFocus) {
            updateBrand(mCardBrand);
        }
    }

    public void setShouldShowPostalCode(boolean shouldShowPostalCode) {
        mShouldShowPostalCode = shouldShowPostalCode;
        adjustViewForPostalCodeAttribute();
    }

    /**
     * Expose a text watcher to receive updates when the card number is changed.
     *
     * @param cardNumberTextWatcher
     */
    public void setCardNumberTextWatcher(TextWatcher cardNumberTextWatcher) {
        mCardNumberEditText.addTextChangedListener(cardNumberTextWatcher);
    }

    /**
     * Expose a text watcher to receive updates when the expiry date is changed.
     *
     * @param expiryDateTextWatcher
     */
    public void setExpiryDateTextWatcher(TextWatcher expiryDateTextWatcher) {
        mExpiryDateEditText.addTextChangedListener(expiryDateTextWatcher);
    }

    /**
     * Expose a text watcher to receive updates when the cvc number is changed.
     *
     * @param cvcNumberTextWatcher
     */
    public void setCvcNumberTextWatcher(TextWatcher cvcNumberTextWatcher) {
        mCvcEditText.addTextChangedListener(cvcNumberTextWatcher);
    }

    /**
     * Expose a text watcher to receive updates when the cvc number is changed.
     *
     * @param postalCodeTextWatcher
     */
    public void setPostalCodeTextWatcher(TextWatcher postalCodeTextWatcher) {
        mPostalCodeEditText.addTextChangedListener(postalCodeTextWatcher);
    }

    @Override
    public boolean isEnabled() {
        return mIsEnabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        mExpiryTextInputLayout.setEnabled(enabled);
        mCardNumberTextInputLayout.setEnabled(enabled);
        mCvcTextInputLayout.setEnabled(enabled);
        mPostalInputLayout.setEnabled(enabled);
        mIsEnabled = enabled;
    }

    void adjustViewForPostalCodeAttribute() {
        // Set the label/hint to the shorter value if we have three things in a row.
        @StringRes int expiryLabel = mShouldShowPostalCode
                ? R.string.expiry_label_short
                : R.string.acc_label_expiry_date;
        mExpiryTextInputLayout.setHint(getResources().getString(expiryLabel));

        @IdRes int focusForward = mShouldShowPostalCode
                ? R.id.et_add_source_postal_ml
                : NO_ID;
        mCvcEditText.setNextFocusForwardId(focusForward);
        mCvcEditText.setNextFocusDownId(focusForward);

        int visibility = mShouldShowPostalCode ? View.VISIBLE : View.GONE;
        mPostalInputLayout.setVisibility(visibility);

        int marginPixels = mShouldShowPostalCode
                ? getResources().getDimensionPixelSize(R.dimen.add_card_expiry_middle_margin)
                : 0;
        LinearLayout.LayoutParams linearParams =
                (LinearLayout.LayoutParams) mCvcTextInputLayout.getLayoutParams();
        linearParams.setMargins(0, 0, marginPixels, 0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            linearParams.setMarginEnd(marginPixels);
        }

        mCvcTextInputLayout.setLayoutParams(linearParams);
    }

    static boolean isPostalCodeMaximalLength(boolean isZip, @Nullable String text) {
        return isZip && text != null && text.length() == 5;
    }

    private boolean isCvcLengthValid() {
        int cvcLength = mCvcEditText.getText().toString().trim().length();
        if (TextUtils.equals(Card.AMERICAN_EXPRESS, mCardBrand)
                && cvcLength == Card.CVC_LENGTH_AMERICAN_EXPRESS) {
            return true;
        }
        if (cvcLength == Card.CVC_LENGTH_COMMON) {
            return true;
        }
        return false;
    }

    private void checkAttributeSet(AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = getContext().getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.CardMultilineWidget,
                    0, 0);

            try {
                mShouldShowPostalCode =
                        a.getBoolean(R.styleable.CardMultilineWidget_shouldShowPostalCode, false);
            } finally {
                a.recycle();
            }
        }
    }

    private void flipToCvcIconIfNotFinished() {
        if (ViewUtils.isCvcMaximalLength(mCardBrand, mCvcEditText.getText().toString())) {
            return;
        }

        @DrawableRes int resourceId = Card.AMERICAN_EXPRESS.equals(mCardBrand)
                ? R.drawable.ic_cvc_amex
                : R.drawable.ic_cvc;

        updateDrawable(resourceId, true);
    }

    @StringRes
    private int getCvcHelperText() {
        return Card.AMERICAN_EXPRESS.equals(mCardBrand)
                ? R.string.cvc_multiline_helper_amex
                : R.string.cvc_multiline_helper;
    }

    private int getDynamicBufferInPixels() {
        float pixelsToAdjust = getResources()
                .getDimension(R.dimen.card_icon_multiline_padding_bottom);
        BigDecimal bigDecimal = new BigDecimal(pixelsToAdjust);
        BigDecimal pixels = bigDecimal.setScale(0, RoundingMode.HALF_DOWN);
        return pixels.intValue();
    }

    private void initView(AttributeSet attrs) {
        setOrientation(VERTICAL);
        inflate(getContext(), R.layout.card_multiline_widget, this);

        mCardNumberEditText = findViewById(R.id.et_add_source_card_number_ml);
        mExpiryDateEditText = findViewById(R.id.et_add_source_expiry_ml);
        mCvcEditText = findViewById(R.id.et_add_source_cvc_ml);
        mPostalCodeEditText = findViewById(R.id.et_add_source_postal_ml);
        mTintColorInt = mCardNumberEditText.getHintTextColors().getDefaultColor();

        mCardBrand = Card.UNKNOWN;
        // This sets the value of mShouldShowPostalCode
        checkAttributeSet(attrs);

        mCardNumberTextInputLayout = findViewById(R.id.tl_add_source_card_number_ml);
        mExpiryTextInputLayout = findViewById(R.id.tl_add_source_expiry_ml);
        // We dynamically set the hint of the CVC field, so we need to keep a reference.
        mCvcTextInputLayout = findViewById(R.id.tl_add_source_cvc_ml);
        mPostalInputLayout = findViewById(R.id.tl_add_source_postal_ml);

        if (mShouldShowPostalCode) {
            // Set the label/hint to the shorter value if we have three things in a row.
            mExpiryTextInputLayout.setHint(getResources().getString(R.string.expiry_label_short));
        }

        initTextInputLayoutErrorHandlers(
                mCardNumberTextInputLayout,
                mExpiryTextInputLayout,
                mCvcTextInputLayout,
                mPostalInputLayout);

        initErrorMessages();
        initFocusChangeListeners();
        initDeleteEmptyListeners();

        mCardNumberEditText.setCardBrandChangeListener(
                new CardNumberEditText.CardBrandChangeListener() {
                    @Override
                    public void onCardBrandChanged(@NonNull @Card.CardBrand String brand) {
                        updateBrand(brand);
                    }
                });

        mCardNumberEditText.setCardNumberCompleteListener(
                new CardNumberEditText.CardNumberCompleteListener() {
                    @Override
                    public void onCardNumberComplete() {
                        mExpiryDateEditText.requestFocus();
                        if (mCardInputListener != null) {
                            mCardInputListener.onCardComplete();
                        }
                    }
                });

        mExpiryDateEditText.setExpiryDateEditListener(
                new ExpiryDateEditText.ExpiryDateEditListener() {
                    @Override
                    public void onExpiryDateComplete() {
                        mCvcEditText.requestFocus();
                        if (mCardInputListener != null) {
                            mCardInputListener.onExpirationComplete();
                        }
                    }
                });

        mCvcEditText.setAfterTextChangedListener(
                new StripeEditText.AfterTextChangedListener() {
                    @Override
                    public void onTextChanged(String text) {
                        if (ViewUtils.isCvcMaximalLength(mCardBrand, text)) {
                            updateBrand(mCardBrand);
                            if (mShouldShowPostalCode) {
                                mPostalCodeEditText.requestFocus();
                            }
                            if (mCardInputListener != null) {
                                mCardInputListener.onCvcComplete();
                            }
                        } else {
                            flipToCvcIconIfNotFinished();
                        }
                        mCvcEditText.setShouldShowError(false);
                    }
                });

        adjustViewForPostalCodeAttribute();

        mPostalCodeEditText.setAfterTextChangedListener(
                new StripeEditText.AfterTextChangedListener() {
                    @Override
                    public void onTextChanged(String text) {
                        if (isPostalCodeMaximalLength(true, text)
                                && mCardInputListener != null) {
                            mCardInputListener.onPostalCodeComplete();
                        }
                        mPostalCodeEditText.setShouldShowError(false);
                    }
                });

        mCardNumberEditText.updateLengthFilter();
        updateBrand(Card.UNKNOWN);
        setEnabled(true);
    }

    private void initDeleteEmptyListeners() {
        mExpiryDateEditText.setDeleteEmptyListener(
                new BackUpFieldDeleteListener(mCardNumberEditText));

        mCvcEditText.setDeleteEmptyListener(
                new BackUpFieldDeleteListener(mExpiryDateEditText));

        // It doesn't matter whether or not the postal code is shown;
        // we can still say where you go when you delete an empty field from it.
        if (mPostalCodeEditText == null) {
            return;
        }
        mPostalCodeEditText.setDeleteEmptyListener(
                new BackUpFieldDeleteListener(mCvcEditText));
    }

    private void initErrorMessages() {
        mCardNumberEditText.setErrorMessage(getContext().getString(R.string.invalid_card_number));
        mExpiryDateEditText.setErrorMessage(getContext().getString(R.string.invalid_expiry_year));
        mCvcEditText.setErrorMessage(getContext().getString(R.string.invalid_cvc));
        mPostalCodeEditText.setErrorMessage(getContext().getString(R.string.invalid_zip));
    }

    private void initFocusChangeListeners() {
        mCardNumberEditText.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    mCardNumberEditText.setHintDelayed(
                            R.string.card_number_hint, CARD_NUMBER_HINT_DELAY);
                    if (mCardInputListener != null) {
                        mCardInputListener.onFocusChange(FOCUS_CARD);
                    }
                } else {
                    mCardNumberEditText.setHint("");
                }
            }
        });

        mExpiryDateEditText.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    mExpiryDateEditText.setHintDelayed(
                            R.string.expiry_date_hint, COMMON_HINT_DELAY);
                    if (mCardInputListener != null) {
                        mCardInputListener.onFocusChange(FOCUS_EXPIRY);
                    }
                } else {
                    mExpiryDateEditText.setHint("");
                }
            }
        });

        mCvcEditText.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    flipToCvcIconIfNotFinished();
                    @StringRes int helperText = getCvcHelperText();
                    mCvcEditText.setHintDelayed(helperText, COMMON_HINT_DELAY);
                    if (mCardInputListener != null) {
                        mCardInputListener.onFocusChange(FOCUS_CVC);
                    }
                } else {
                    updateBrand(mCardBrand);
                    mCvcEditText.setHint("");
                }
            }
        });

        if (mPostalCodeEditText == null) {
            return;
        }

        mPostalCodeEditText.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!mShouldShowPostalCode) {
                    return;
                }
                if (hasFocus) {
                    mPostalCodeEditText.setHintDelayed(R.string.zip_helper, COMMON_HINT_DELAY);
                    if (mCardInputListener != null) {
                        mCardInputListener.onFocusChange(FOCUS_POSTAL);
                    }
                } else {
                    mPostalCodeEditText.setHint("");
                }
            }
        });
    }

    private void initTextInputLayoutErrorHandlers(
            TextInputLayout cardInputLayout,
            TextInputLayout expiryInputLayout,
            TextInputLayout cvcTextInputLayout,
            TextInputLayout postalInputLayout) {

        mCardNumberEditText.setErrorMessageListener(new ErrorListener(cardInputLayout));
        mExpiryDateEditText.setErrorMessageListener(new ErrorListener(expiryInputLayout));
        mCvcEditText.setErrorMessageListener(new ErrorListener(cvcTextInputLayout));
        if (mPostalCodeEditText == null) {
            return;
        }
        mPostalCodeEditText.setErrorMessageListener(new ErrorListener(postalInputLayout));
    }

    private void updateBrand(@NonNull @Card.CardBrand String brand) {
        mCardBrand = brand;
        updateCvc(mCardBrand);
        updateDrawable(BRAND_RESOURCE_MAP.get(brand), Card.UNKNOWN.equals(brand));
    }

    private void updateCvc(@NonNull @Card.CardBrand String brand) {
        if (Card.AMERICAN_EXPRESS.equals(brand)) {
            mCvcEditText.setFilters(
                    new InputFilter[]{
                            new InputFilter.LengthFilter(Card.CVC_LENGTH_AMERICAN_EXPRESS)
                    });
            mCvcTextInputLayout.setHint(getResources().getString(R.string.cvc_amex_hint));
        } else {
            mCvcEditText.setFilters(
                    new InputFilter[]{
                            new InputFilter.LengthFilter(Card.CVC_LENGTH_COMMON)});
            mCvcTextInputLayout.setHint(getResources().getString(R.string.cvc_number_hint));
        }
    }

    @SuppressWarnings("deprecation")
    private void updateDrawable(
            @DrawableRes int iconResourceId,
            boolean needsTint) {

        Drawable icon;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            icon = getResources().getDrawable(iconResourceId, null);
        } else {
            // This method still triggers the "deprecation" warning, despite the other
            // one not being allowed for SDK < 21
            icon = getResources().getDrawable(iconResourceId);
        }

        Drawable[] drawables = mCardNumberEditText.getCompoundDrawables();
        Drawable original = drawables[0];
        if (original == null) {
            return;
        }

        Rect copyBounds = new Rect();
        original.copyBounds(copyBounds);

        int iconPadding = mCardNumberEditText.getCompoundDrawablePadding();

        if (!mHasAdjustedDrawable) {
            copyBounds.top = copyBounds.top - getDynamicBufferInPixels();
            copyBounds.bottom = copyBounds.bottom - getDynamicBufferInPixels();
            mHasAdjustedDrawable = true;
        }

        icon.setBounds(copyBounds);
        Drawable compatIcon = DrawableCompat.wrap(icon);
        if (needsTint) {
            DrawableCompat.setTint(compatIcon.mutate(), mTintColorInt);
        }

        mCardNumberEditText.setCompoundDrawablePadding(iconPadding);
        mCardNumberEditText.setCompoundDrawables(compatIcon, null, null, null);
    }

}
