package com.stripe.android.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.AppCompatImageView;
import android.text.InputFilter;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.stripe.android.R;
import com.stripe.android.model.Card;
import com.stripe.android.CardUtils;
import com.stripe.android.StripeTextUtils;

import static com.stripe.android.model.Card.BRAND_RESOURCE_MAP;
import static com.stripe.android.model.Card.CVC_LENGTH_AMERICAN_EXPRESS;
import static com.stripe.android.model.Card.CVC_LENGTH_COMMON;
import static com.stripe.android.view.CardInputListener.FocusField.FOCUS_CARD;
import static com.stripe.android.view.CardInputListener.FocusField.FOCUS_CVC;
import static com.stripe.android.view.CardInputListener.FocusField.FOCUS_EXPIRY;
import static com.stripe.android.view.CardInputListener.FocusField.FOCUS_POSTAL;

public class CardMultilineWidget extends LinearLayout {

    static final String CARD_MULTILINE_TOKEN = "CardMultilineView";
    private CardInputListener mCardInputListener;
    private CardNumberEditText mCardNumberEditText;
    private ExpiryDateEditText mExpiryDateEditText;
    private StripeEditText mCvcEditText;
    private StripeEditText mPostalCodeEditText;
    private TextInputLayout mCvcTextInputLayout;

    private boolean mShouldShowPostalCode;
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
     */
    public boolean validateAllFields() {
        mCardNumberEditText.setShouldShowError(
                !CardUtils.isValidCardNumber(mCardNumberEditText.getCardNumber()));
        mExpiryDateEditText.setShouldShowError(
                mExpiryDateEditText.getValidDateFields() == null ||
                        !mExpiryDateEditText.isDateValid());
        mCvcEditText.setShouldShowError(!isCvcMaximalLength(mCardBrand,
                mCvcEditText.getText().toString()));
        boolean postalCodeIsValidOrGone;
        if (mShouldShowPostalCode) {
            mPostalCodeEditText.setShouldShowError(!isPostalCodeMaximalLength(true,
                    mPostalCodeEditText.getText().toString()));
            postalCodeIsValidOrGone = mPostalCodeEditText.getShouldShowError();
        } else {
            postalCodeIsValidOrGone = true;
        }

        return !mCardNumberEditText.getShouldShowError()
                && !mExpiryDateEditText.getShouldShowError()
                && !mCvcEditText.getShouldShowError()
                && postalCodeIsValidOrGone;
    }

    void initView(AttributeSet attrs) {
        setOrientation(VERTICAL);
        inflate(getContext(), R.layout.card_multiline_widget, this);

        mShouldShowPostalCode = true;
        mCardNumberEditText = findViewById(R.id.et_add_source_card_number_ml);
        mExpiryDateEditText = findViewById(R.id.et_add_source_expiry_ml);
        mCvcEditText = findViewById(R.id.et_add_source_cvc_ml);
        mPostalCodeEditText = findViewById(R.id.et_add_source_zip_ml);
        mTintColorInt = mCardNumberEditText.getHintTextColors().getDefaultColor();

        TextInputLayout cardInputLayout = findViewById(R.id.tl_add_source_card_number_ml);
        TextInputLayout expiryInputLayout = findViewById(R.id.tl_add_source_expiry_ml);
        mCvcTextInputLayout = findViewById(R.id.tl_add_source_cvc_ml);
        TextInputLayout postalInputLayout = findViewById(R.id.tl_add_source_zip_ml);

        mCardNumberEditText.setErrorMessage(getContext().getString(R.string.invalid_card_number));
        mExpiryDateEditText.setErrorMessage(getContext().getString(R.string.invalid_expiry_year));
        mCvcEditText.setErrorMessage(getContext().getString(R.string.invalid_cvc));
        mPostalCodeEditText.setErrorMessage(getContext().getString(R.string.invalid_zip));

        if (attrs != null) {
            TypedArray a = getContext().getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.CardMultilineWidget,
                    0, 0);

            try {
                mShouldShowPostalCode =
                        a.getBoolean(R.styleable.CardMultilineWidget_shouldShowZip, true);
            } finally {
                a.recycle();
            }
        }

        mCardNumberEditText.setErrorMessageListener(new ErrorListener(cardInputLayout));
        mCardNumberEditText.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    if (mCardInputListener != null) {
                        mCardInputListener.onFocusChange(FOCUS_CARD);
                    }
                }
            }
        });

        mExpiryDateEditText.setErrorMessageListener(new ErrorListener(expiryInputLayout));
        mExpiryDateEditText.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    if (mCardInputListener != null) {
                        mCardInputListener.onFocusChange(FOCUS_EXPIRY);
                    }
                }
            }
        });

        mCvcEditText.setErrorMessageListener(new ErrorListener(mCvcTextInputLayout));
        mCvcEditText.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    if (mCardInputListener != null) {
                        mCardInputListener.onFocusChange(FOCUS_CVC);
                    }
                }
            }
        });

        mPostalCodeEditText.setErrorMessageListener(new ErrorListener(postalInputLayout));
        mPostalCodeEditText.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    if (mCardInputListener != null) {
                        mCardInputListener.onFocusChange(FOCUS_POSTAL);
                    }
                }
            }
        });

        mExpiryDateEditText.setDeleteEmptyListener(
                new BackUpFieldDeleteListener(mCardNumberEditText));

        mCvcEditText.setDeleteEmptyListener(
                new BackUpFieldDeleteListener(mExpiryDateEditText));

        mPostalCodeEditText.setDeleteEmptyListener(
                new BackUpFieldDeleteListener(mCvcEditText));

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
                        if (isCvcMaximalLength(mCardBrand, text)) {
                            mPostalCodeEditText.requestFocus();
                            if (mCardInputListener != null) {
                                mCardInputListener.onCvcComplete();
                            }
                        }
                        mCvcEditText.setShouldShowError(false);
                    }
                });

        if (!mShouldShowPostalCode) {
            mCvcEditText.setNextFocusForwardId(NO_ID);
            mCvcEditText.setNextFocusDownId(NO_ID);
            postalInputLayout.setVisibility(View.GONE);
            LinearLayout secondRowLayout = findViewById(R.id.second_row_layout);
            secondRowLayout.removeView(postalInputLayout);
            LinearLayout.LayoutParams linearParams = (LinearLayout.LayoutParams) mCvcTextInputLayout.getLayoutParams();
            linearParams.setMargins(0,0, 0, 0);
            mCvcTextInputLayout.setLayoutParams(linearParams);
        }

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

        updateBrand(Card.UNKNOWN);
    }

    private static boolean isCvcMaximalLength(
            @NonNull @Card.CardBrand String cardBrand,
            @Nullable String cvcText) {
        if (cvcText == null) {
            return false;
        }

        if (Card.AMERICAN_EXPRESS.equals(cardBrand)) {
            return cvcText.length() == CVC_LENGTH_AMERICAN_EXPRESS;
        } else {
            return cvcText.length() == CVC_LENGTH_COMMON;
        }
    }

    private static boolean isPostalCodeMaximalLength(boolean isZip, @Nullable String text) {
        return isZip && text != null && text.length() == 5;
    }

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
        icon.setBounds(original.copyBounds());
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
                    new InputFilter[] {new InputFilter.LengthFilter(Card.CVC_LENGTH_COMMON)});
            mCvcTextInputLayout.setHint(getResources().getString(R.string.cvc_amex_hint));
        } else {
            mCvcEditText.setFilters(
                    new InputFilter[] {
                            new InputFilter.LengthFilter(Card.CVC_LENGTH_AMERICAN_EXPRESS)});
            mCvcTextInputLayout.setHint(getResources().getString(R.string.cvc_number_hint));
        }
    }

    /**
     * Class used to encapsulate the functionality of "backing up" via the delete/backspace key
     * from one text field to the previous. We use this to simulate multiple fields being all part
     * of the same EditText, so a delete key entry from field N+1 deletes the last character in
     * field N. Each BackUpFieldDeleteListener is attached to the N+1 field, from which it gets
     * its {@link #onDeleteEmpty()} call, and given a reference to the N field, upon which
     * it will be acting.
     */
    private class BackUpFieldDeleteListener implements StripeEditText.DeleteEmptyListener {

        private StripeEditText backUpTarget;

        BackUpFieldDeleteListener(StripeEditText backUpTarget) {
            this.backUpTarget = backUpTarget;
        }

        @Override
        public void onDeleteEmpty() {
            String fieldText = backUpTarget.getText().toString();
            if (fieldText.length() > 1) {
                backUpTarget.setText(
                        fieldText.substring(0, fieldText.length() - 1));
            }
            backUpTarget.requestFocus();
            backUpTarget.setSelection(backUpTarget.length());
        }
    }

    private class ErrorListener implements StripeEditText.ErrorMessageListener {

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
