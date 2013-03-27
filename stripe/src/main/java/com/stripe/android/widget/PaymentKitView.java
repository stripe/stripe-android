package com.stripe.android.widget;

import java.util.HashSet;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Layout;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.stripe.android.R;
import com.stripe.android.model.Card;
import com.stripe.android.util.CardExpiry;
import com.stripe.android.util.CardNumberFormatter;
import com.stripe.android.util.TextUtils;

public class PaymentKitView extends FrameLayout {
    private static final long SLIDING_DURATION_MS = 500;

    private ImageView cardImageView;
    private ClippingEditText cardNumberView;
    private EditText expiryView;
    private EditText cvcView;

    private float cardNumberSlidingDelta = 0;
    private boolean isCardNumberCollapsed = false;

    private Card card = new Card(null, null, null, null);
    private int textColor;
    private int errorColor;

    private InputFilter cvcEmptyFilter = new InputFilter() {
        @Override
        public CharSequence filter(CharSequence source, int start, int end,
                Spanned dest, int dstart, int dend) {
            int n = (dstart - 0) + (end - start) + (dest.length() - dend);
            if (n == 0) {
                expiryView.requestFocus();
            }
            return null;
        }
    };
    private InputFilter[] lengthOf3 = new InputFilter[] {
            new InputFilter.LengthFilter(3), cvcEmptyFilter };
    private InputFilter[] lengthOf4 = new InputFilter[] {
            new InputFilter.LengthFilter(4), cvcEmptyFilter };

    private boolean lastValidationResult = false;

    private HashSet<OnValidationChangeListener> listeners
        = new HashSet<OnValidationChangeListener>();

    private Animation fadeInAnimation;

    public void registerListener(OnValidationChangeListener listener) {
        listeners.add(listener);
    }

    public void unregisterListener(OnValidationChangeListener listener) {
        listeners.remove(listener);
    }

    public interface OnValidationChangeListener {
        public void onChange(boolean valid);
    }

    public PaymentKitView(Context context) {
        super(context);
        init();
    }

    public PaymentKitView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PaymentKitView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public Card getCard() {
        return card;
    }

    private void init() {
        LayoutInflater inflater = (LayoutInflater) getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View parent = inflater.inflate(R.layout.__pk_view, this);

        cardImageView = (ImageView) parent.findViewById(R.id.__pk_card_image);
        cardNumberView = (ClippingEditText) parent.findViewById(R.id.__pk_card_number);
        expiryView = (EditText) parent.findViewById(R.id.__pk_expiry);
        cvcView = (EditText) parent.findViewById(R.id.__pk_cvc);

        textColor = cvcView.getCurrentTextColor();
        errorColor = getContext().getResources().getColor(R.color.__pk_error_color);

        cardImageView.setTag(R.drawable.__pk_placeholder);
        fadeInAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.fade_in);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        card.setNumber(cardNumberView.getText().toString());

        CardExpiry cardExpiry = new CardExpiry();
        cardExpiry.updateFromString(expiryView.getText().toString());
        card.setExpMonth(cardExpiry.getMonth());
        card.setExpYear(cardExpiry.getYear());

        card.setCVC(TextUtils.nullIfBlank(cvcView.getText().toString()));

        updateFields(false);
        notifyValidationChange();

        setupTextWatchers();

        if (card.validateNumber()) {
            if (card.validateExpiryDate()) {
                cvcView.requestFocus();
            } else {
                expiryView.requestFocus();
            }
        }
    }

    private void setupTextWatchers() {
         CardNumberWatcher cardNumberWatcher = new CardNumberWatcher();
         cardNumberView.addTextChangedListener(cardNumberWatcher);
         cardNumberView.setOnFocusChangeListener(new OnFocusChangeListener() {
             @Override
             public void onFocusChange(View v, boolean hasFocus) {
                 if (hasFocus && isCardNumberCollapsed) {
                     expandCardNumber();
                 }
             }
         });

         ExpiryWatcher expiryWatcher = new ExpiryWatcher();
         expiryView.setFilters(new InputFilter[] { expiryWatcher });
         expiryView.addTextChangedListener(expiryWatcher);

         cvcView.setFilters(lengthOf3);
         cvcView.addTextChangedListener(new CvcWatcher());
         cvcView.setOnFocusChangeListener(new OnFocusChangeListener() {
             @Override
             public void onFocusChange(View v, boolean hasFocus) {
                 if (hasFocus) {
                     updateCvcType();
                 } else {
                     updateCardType();
                 }
             }
         });
    }

    private void updateFields(boolean animate) {
        cardNumberView.setTextColor(textColor);
        if (card.validateNumberLength()) {
            if (card.validateNumber()) {
                collapseCardNumber(animate);
            } else {
                cardNumberView.setTextColor(errorColor);
            }
        }

        if ("American Express".equals(card.getType())) {
            cvcView.setFilters(lengthOf4);
        } else {
            cvcView.setFilters(lengthOf3);
        }

        updateCardType();
    }

    private void computeCardNumberSlidingDelta() {
        Layout layout = cardNumberView.getLayout();
        if (layout == null) {
            return;
        }

        String number = cardNumberView.getText().toString();
        card.setNumber(number);
        int suffixLength = "American Express".equals(card.getType()) ? 5 : 4;

        cardNumberSlidingDelta = layout.getPrimaryHorizontal(number.length() - suffixLength);
    }

    private void collapseCardNumber(boolean animate) {
        computeCardNumberSlidingDelta();
        isCardNumberCollapsed = true;
        if (animate) {
            animateCardNumber();
        } else {
            showExpiryAndCvc();
        }
    }

    private void expandCardNumber() {
        isCardNumberCollapsed = false;
        animateCardNumber();
    }

    private void animateCardNumber() {
        float fromXDelta = isCardNumberCollapsed ? 0
                : cardNumberSlidingDelta;
        float toXDelta = isCardNumberCollapsed ? cardNumberSlidingDelta : 0;
        ClippingAnimation anim = new ClippingAnimation(cardNumberView, fromXDelta, toXDelta);
        anim.setDuration(SLIDING_DURATION_MS);
        anim.setAnimationListener(mAnimationListener);
        cardNumberView.startAnimation(anim);
    }

    private void showExpiryAndCvc() {
        cardNumberView.setClipX((int) cardNumberSlidingDelta);
        expiryView.setVisibility(View.VISIBLE);
        cvcView.setVisibility(View.VISIBLE);
    }

    private AnimationListener mAnimationListener = new AnimationListener() {
        public void onAnimationEnd(Animation animation) {
            if (!isCardNumberCollapsed) {
                return;
            }
            showExpiryAndCvc();
            expiryView.requestFocus();
        }

        public void onAnimationRepeat(Animation animation) {
            // not needed
        }

        public void onAnimationStart(Animation animation) {
            if (isCardNumberCollapsed) {
                return;
            }

            expiryView.setVisibility(View.GONE);
            cvcView.setVisibility(View.GONE);
        }
    };

    private void notifyValidationChange() {
        boolean valid = card.validateCard();
        if (valid != lastValidationResult) {
            for (OnValidationChangeListener listener : listeners) {
                listener.onChange(valid);
            }
        }
        lastValidationResult = valid;
    }

    private int getImageResourceForCardType() {
        String cardType = card.getType();

        if ("American Express".equals(cardType)) {
            return R.drawable.__pk_amex;
        }

        if ("Discover".equals(cardType)) {
            return R.drawable.__pk_discover;
        }

        if ("JCB".equals(cardType)) {
            return R.drawable.__pk_jcb;
        }

        if ("Diners Club".equals(cardType)) {
            return R.drawable.__pk_diners;
        }

        if ("Visa".equals(cardType)) {
            return R.drawable.__pk_visa;
        }

        if ("MasterCard".equals(cardType)) {
            return R.drawable.__pk_mastercard;
        }

        return R.drawable.__pk_placeholder;
    }

    private void updateCardType() {
        int resId = getImageResourceForCardType();
        updateCardImage(resId);
    }

    private void updateCvcType() {
        boolean isAmex = "American Express".equals(card.getType());
        int resId = isAmex ? R.drawable.__pk_cvc_amex : R.drawable.__pk_cvc;
        updateCardImage(resId);
    }

    private void updateCardImage(int resId) {
        Integer oldResId = (Integer) cardImageView.getTag();
        if (oldResId == resId) {
            return;
        }
        cardImageView.setTag(resId);
        cardImageView.setImageResource(resId);
        cardImageView.startAnimation(fadeInAnimation);
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        final SavedState myState = new SavedState(superState);
        myState.cardNumberSlidingDelta = cardNumberSlidingDelta;
        return myState;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (!state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        // Restore the instance state
        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        cardNumberSlidingDelta = myState.cardNumberSlidingDelta;
    }

    // Save cardNumberSlidingDelta so we can update the layout for the card number field during
    // onAttachWindow, when the layout of the EditText is not yet initialized.
    private static class SavedState extends BaseSavedState {
        float cardNumberSlidingDelta;

        public SavedState(Parcel source) {
            super(source);
            cardNumberSlidingDelta = source.readFloat();
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeFloat(cardNumberSlidingDelta);
        }

        @SuppressWarnings("unused")
        public static final Parcelable.Creator<SavedState> CREATOR
            = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    private class CardNumberWatcher implements TextWatcher {
        private boolean isInserting = false;

        @Override
        public void afterTextChanged(Editable s) {
            String number = s.toString();

            String formattedNumber = CardNumberFormatter.format(number, isInserting);
            if (!number.equals(formattedNumber)) {
                s.replace(0, s.length(), formattedNumber);
                return;
            }

            card.setNumber(number);
            updateFields(true);

            notifyValidationChange();
        }
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            isInserting = (after > count);
        }
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // not needed
        }
    }

    private class ExpiryWatcher implements InputFilter, TextWatcher {
        private static final int EXPIRY_MAX_LENGTH = 5;

        private CardExpiry cardExpiry = new CardExpiry();
        private boolean isInserting = false;

        @Override
        public CharSequence filter(CharSequence source, int start, int end,
                Spanned dest, int dstart, int dend) {
            StringBuffer buf = new StringBuffer();
            buf.append(dest.subSequence(0, dstart));
            buf.append(source.subSequence(start, end));
            buf.append(dest.subSequence(dend, dest.length()));
            String str = buf.toString();
            if (str.length() == 0) {
                cardNumberView.requestFocus();
                return null;
            }
            if (str.length() > EXPIRY_MAX_LENGTH) {
                return "";
            }
            cardExpiry.updateFromString(str);
            return cardExpiry.isPartiallyValid() ? null : "";
        }

        @Override
        public void afterTextChanged(Editable s) {
            String str = s.toString();
            cardExpiry.updateFromString(str);
            card.setExpMonth(cardExpiry.getMonth());
            card.setExpYear(cardExpiry.getYear());
            if (cardExpiry.isPartiallyValid()) {
                String formattedString = isInserting ?
                        cardExpiry.toStringWithTrail() : cardExpiry.toString();
                if (!str.equals(formattedString)) {
                    s.replace(0, s.length(), formattedString);
                }
            }
            if (cardExpiry.isValid()) {
                cvcView.requestFocus();
            }

            notifyValidationChange();
        }
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            isInserting = (after > count);
        }
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // not needed
        }
    }

    private class CvcWatcher implements TextWatcher {
        @Override
        public void afterTextChanged(Editable s) {
            card.setCVC(TextUtils.nullIfBlank(s.toString()));
            notifyValidationChange();
        }
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // not needed
        }
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // not needed
        }
    }
}