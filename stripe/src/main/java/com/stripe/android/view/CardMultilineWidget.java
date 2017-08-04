package com.stripe.android.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.VisibleForTesting;
import android.support.design.widget.TextInputLayout;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.text.InputFilter;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;

import com.stripe.android.R;
import com.stripe.android.model.Card;
import com.stripe.android.CardUtils;

import java.util.Locale;

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
    // On XHDPI, this works out to be approximately 4px
    private static final float CARD_ICON_PADDING_DP = 1.52380955f;

    private @Nullable CardInputListener mCardInputListener;
    private CardNumberEditText mCardNumberEditText;
    private ExpiryDateEditText mExpiryDateEditText;
    private StripeEditText mCvcEditText;
    private StripeEditText mPostalCodeEditText;
    private TextInputLayout mCvcTextInputLayout;

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
        String cardNumber = mCardNumberEditText.getCardNumber();
        int[] cardDate = mExpiryDateEditText.getValidDateFields();
        if (cardNumber == null || cardDate == null || cardDate.length != 2) {
            return null;
        }

        String cvcValue = mCvcEditText.getText().toString();
        if (validateAllFields()) {
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
        boolean cvcIsValid = ViewUtils.isCvcMaximalLength(
                mCardBrand, mCvcEditText.getText().toString());
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

    static void adjustViewForPostalCodeAttribute(
            @NonNull StripeEditText navigationEditText,
            @NonNull TextInputLayout postalInputLayout,
            @NonNull LinearLayout postalParentLayout,
            @NonNull TextInputLayout paddedMiddleTextInputLayout) {
        navigationEditText.setNextFocusForwardId(NO_ID);
        navigationEditText.setNextFocusDownId(NO_ID);
        postalInputLayout.setVisibility(View.GONE);
        postalParentLayout.removeView(postalInputLayout);
        LinearLayout.LayoutParams linearParams =
                (LinearLayout.LayoutParams) paddedMiddleTextInputLayout.getLayoutParams();
        linearParams.setMargins(0, 0, 0, 0);
        paddedMiddleTextInputLayout.setLayoutParams(linearParams);
    }

    static boolean isPostalCodeMaximalLength(boolean isZip, @Nullable String text) {
        return isZip && text != null && text.length() == 5;
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

    private void initView(AttributeSet attrs) {
        setOrientation(VERTICAL);
        inflate(getContext(), R.layout.card_multiline_widget, this);

        mCardNumberEditText = findViewById(R.id.et_add_source_card_number_ml);
        mExpiryDateEditText = findViewById(R.id.et_add_source_expiry_ml);
        mCvcEditText = findViewById(R.id.et_add_source_cvc_ml);
        mPostalCodeEditText = findViewById(R.id.et_add_source_postal_ml);
        mTintColorInt = mCardNumberEditText.getHintTextColors().getDefaultColor();

        // This sets the value of mShouldShowPostalCode
        checkAttributeSet(attrs);


        TextInputLayout cardInputLayout = findViewById(R.id.tl_add_source_card_number_ml);
        TextInputLayout expiryInputLayout = findViewById(R.id.tl_add_source_expiry_ml);
        // We dynamically set the hint of the CVC field, so we need to keep a reference.
        mCvcTextInputLayout = findViewById(R.id.tl_add_source_cvc_ml);
        TextInputLayout postalInputLayout = findViewById(R.id.tl_add_source_postal_ml);

        initTextInputLayoutErrorHandlers(
                cardInputLayout,
                expiryInputLayout,
                mCvcTextInputLayout,
                postalInputLayout);

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
                            mPostalCodeEditText.requestFocus();
                            if (mCardInputListener != null) {
                                mCardInputListener.onCvcComplete();
                            }
                        }
                        mCvcEditText.setShouldShowError(false);
                    }
                });

        if (!mShouldShowPostalCode) {
            LinearLayout postalParentLayout = findViewById(R.id.second_row_layout);
            adjustViewForPostalCodeAttribute(
                    mCvcEditText,
                    postalInputLayout,
                    postalParentLayout,
                    mCvcTextInputLayout);
        } else {
            mPostalCodeEditText.setAfterTextChangedListener(
                    new StripeEditText.AfterTextChangedListener() {
                        @Override
                        public void onTextChanged(String text) {
                            if (isPostalCodeMaximalLength(true, text) && mCardInputListener != null) {
                                mCardInputListener.onPostalCodeComplete();
                            }
                            mPostalCodeEditText.setShouldShowError(false);
                        }
                    });
        }

        mCardNumberEditText.updateLengthFilter();
        updateBrand(Card.UNKNOWN);
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
                            R.string.card_number_hint, 120L);
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
                            R.string.expiry_date_hint, 80L);
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
                    @StringRes int helperText = getCvcHelperText();
                    mCvcEditText.setHintDelayed(helperText, 80L);
                    if (mCardInputListener != null) {
                        mCardInputListener.onFocusChange(FOCUS_CVC);
                    }
                } else {
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
                    mPostalCodeEditText.setHintDelayed(R.string.zip_helper, 80L);
                    if (mCardInputListener != null) {
                        mCardInputListener.onFocusChange(FOCUS_POSTAL);
                    }
                } else {
                    mPostalCodeEditText.setHint("");
                }
            }
        });
    }

    @StringRes
    private int getCvcHelperText() {
        return Card.AMERICAN_EXPRESS.equals(mCardBrand)
                ? R.string.cvc_multiline_helper_amex
                : R.string.cvc_multiline_helper;
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


    @SuppressWarnings("deprecation")
    private void updateBrand(@NonNull @Card.CardBrand String brand) {
        mCardBrand = brand;
        updateCvc(mCardBrand);

        int iconPadding = mCardNumberEditText.getCompoundDrawablePadding();
        Drawable[] drawables = mCardNumberEditText.getCompoundDrawables();
        Drawable original = drawables[0];
        if (original == null) {
            return;
        }

        Drawable icon = getResources().getDrawable(BRAND_RESOURCE_MAP.get(brand));
        Rect originalBounds = original.copyBounds();

        Log.d("chewie", String.format(Locale.ENGLISH,
                "I should be moving things %.8f",
                ViewUtils.convertPixelsToDp(4f)));

        float someDimension = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, 4f, getResources().getDisplayMetrics());
        Log.d("chewie", String.format(Locale.ENGLISH,
                "The other dimension is %.4f", someDimension));
        if (!mHasAdjustedDrawable) {
            originalBounds.top = originalBounds.top - 4;
            originalBounds.bottom = originalBounds.bottom - 4;
            mHasAdjustedDrawable = true;
        }

        icon.setBounds(originalBounds);
        Drawable compatIcon = DrawableCompat.wrap(icon);
        if (Card.UNKNOWN.equals(brand)) {
            DrawableCompat.setTint(compatIcon.mutate(), mTintColorInt);
        }

        mCardNumberEditText.setCompoundDrawablePadding(iconPadding);
        mCardNumberEditText.setCompoundDrawables(compatIcon, null, null, null);
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

    private static class ErrorListener implements StripeEditText.ErrorMessageListener {

        TextInputLayout textInputLayout;

        ErrorListener(TextInputLayout textInputLayout) {
            this.textInputLayout = textInputLayout;
        }

        @Override
        public void displayErrorMessage(@Nullable String message) {
            if (message == null) {
                textInputLayout.setErrorEnabled(false);
            } else {
                textInputLayout.setError(message);
            }
        }
    }
}
