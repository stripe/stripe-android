package com.stripe.android.widget;

import java.util.HashSet;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Layout;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
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

    private ImageView mCardImageView;
    private ImageView mCardImageBottomView;  // for cross-fading

    private ClippingEditText mCardNumberView;
    private View mOtherFieldsContainer;
    private EditText mExpiryView;
    private EditText mCvcView;

    private float mCardNumberSlidingDelta = 0;
    private boolean mIsCardNumberCollapsed = false;

    private Card mCard = new Card(null, null, null, null);
    private int mTextColor;
    private int mErrorColor;

    private OnKeyListener mEmptyListener = new EmptyOnKeyListener();

    private InputFilter mCvcEmptyFilter = new CvcEmptyFilter();
    private InputFilter[] mCvcLengthOf3 = new InputFilter[] {
            new InputFilter.LengthFilter(3), mCvcEmptyFilter };
    private InputFilter[] mCvcLengthOf4 = new InputFilter[] {
            new InputFilter.LengthFilter(4), mCvcEmptyFilter };

    private boolean mLastValidationResult = false;

    private HashSet<OnValidationChangeListener> mListeners
        = new HashSet<OnValidationChangeListener>();

    private Animation mScaleInAnimation;
    private Animation mScaleOutAnimation;

    private int mMinWidth;

    public void registerListener(OnValidationChangeListener listener) {
        mListeners.add(listener);
    }

    public void unregisterListener(OnValidationChangeListener listener) {
        mListeners.remove(listener);
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
        return mCard;
    }

    private void init() {
        LayoutInflater inflater = (LayoutInflater) getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View parent = inflater.inflate(R.layout.__pk_view, this);

        mCardImageView = (ImageView) parent.findViewById(R.id.__pk_card_image);
        mCardImageBottomView = (ImageView) parent.findViewById(R.id.__pk_card_image_bottom);
        mCardNumberView = (ClippingEditText) parent.findViewById(R.id.__pk_card_number);
        mExpiryView = (EditText) parent.findViewById(R.id.__pk_expiry);
        mCvcView = (EditText) parent.findViewById(R.id.__pk_cvc);
        mOtherFieldsContainer = parent.findViewById(R.id.__pk_other_fields);

        mCardNumberView.setRawInputType(InputType.TYPE_CLASS_NUMBER);

        mTextColor = mCvcView.getCurrentTextColor();
        mErrorColor = getContext().getResources().getColor(R.color.__pk_error_color);
        computeMinWidth();

        mCardImageView.setTag(R.drawable.__pk_placeholder);
        mScaleOutAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.scale_out);
        mScaleInAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.scale_in);
        mScaleInAnimation.setAnimationListener(mCardImageAnimationListener);
    }

    private void computeMinWidth() {
        Rect bounds = new Rect();
        Paint textPaint = mCardNumberView.getPaint();
        textPaint.getTextBounds("4", 0, 1, bounds);    // widest digit
        int cardNumberMinWidth = bounds.width() * 21;  // wide enough for 21 digits

        int marginLeft = getContext().getResources().getDimensionPixelSize(
                R.dimen.__pk_margin_left);

        mMinWidth = getPaddingLeft() + marginLeft + cardNumberMinWidth + getPaddingRight();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        mCard.setNumber(mCardNumberView.getText().toString());

        CardExpiry cardExpiry = new CardExpiry();
        cardExpiry.updateFromString(mExpiryView.getText().toString());
        mCard.setExpMonth(cardExpiry.getMonth());
        mCard.setExpYear(cardExpiry.getYear());

        mCard.setCVC(TextUtils.nullIfBlank(mCvcView.getText().toString()));

        updateFields(false);
        notifyValidationChange();

        mCardImageView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mIsCardNumberCollapsed) {
                    expandCardNumber();
                } else {
                    collapseCardNumber(true);
                }
            }
        });

        setupTextWatchers();

        if (mCard.validateNumber()) {
            if (mCard.validateExpiryDate()) {
                mCvcView.requestFocus();
            } else {
                mExpiryView.requestFocus();
            }
        }
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // We want to be at least minWidth wide
        int width = Math.max(mMinWidth, getMeasuredWidth());

        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.AT_MOST) {
            // If the measure spec gives us an upper bound, do a tight fit.
            width = Math.min(mMinWidth, getMeasuredWidth());
        }

        setMeasuredDimension(width, getMeasuredHeight());
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        mCardImageView.setEnabled(enabled);
        mCardNumberView.setEnabled(enabled);
        mExpiryView.setEnabled(enabled);
        mCvcView.setEnabled(enabled);
    }

    private void setupTextWatchers() {
        CardNumberWatcher cardNumberWatcher = new CardNumberWatcher();
        mCardNumberView.setFilters(new InputFilter[] {
                new InputFilter.LengthFilter(19), cardNumberWatcher });
        mCardNumberView.addTextChangedListener(cardNumberWatcher);
        mCardNumberView.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus && mIsCardNumberCollapsed) {
                    expandCardNumber();
                }
            }
        });

        ExpiryWatcher expiryWatcher = new ExpiryWatcher();
        mExpiryView.setFilters(new InputFilter[] { expiryWatcher });
        mExpiryView.addTextChangedListener(expiryWatcher);
        mExpiryView.setOnKeyListener(mEmptyListener);

        mCvcView.setFilters(mCvcLengthOf3);
        mCvcView.addTextChangedListener(new CvcWatcher());
        mCvcView.setOnKeyListener(mEmptyListener);
        mCvcView.setOnFocusChangeListener(new OnFocusChangeListener() {
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
        mCardNumberView.setTextColor(mTextColor);
        if (mCard.validateNumberLength()) {
            if (mCard.validateNumber()) {
                collapseCardNumber(animate);
            } else {
                mCardNumberView.setTextColor(mErrorColor);
            }
        }

        if ("American Express".equals(mCard.getType())) {
            mCvcView.setFilters(mCvcLengthOf4);
        } else {
            mCvcView.setFilters(mCvcLengthOf3);
        }

        updateCardType();
    }

    private void computeCardNumberSlidingDelta() {
        Layout layout = mCardNumberView.getLayout();
        if (layout == null) {
            return;
        }

        String number = mCardNumberView.getText().toString();
        mCard.setNumber(number);
        int suffixLength = "American Express".equals(mCard.getType()) ? 5 : 4;

        mCardNumberSlidingDelta = layout.getPrimaryHorizontal(number.length() - suffixLength);
    }

    private void collapseCardNumber(boolean animate) {
        if (!mCard.validateNumber()) {
            return;
        }
        computeCardNumberSlidingDelta();
        mIsCardNumberCollapsed = true;
        if (animate) {
            animationFields();
        } else {
            showExpiryAndCvc();
        }
    }

    private void expandCardNumber() {
        mIsCardNumberCollapsed = false;
        animationFields();
    }

    private void animationFields() {
        animateCardNumber();
        animateOtherFields();
    }

    private void animateCardNumber() {
        float fromXDelta = mIsCardNumberCollapsed ? 0
                : mCardNumberSlidingDelta;
        float toXDelta = mIsCardNumberCollapsed ? mCardNumberSlidingDelta : 0;
        ClippingAnimation anim = new ClippingAnimation(mCardNumberView, fromXDelta, toXDelta);
        anim.setDuration(SLIDING_DURATION_MS);
        anim.setAnimationListener(mCardNumberAnimationListener);
        anim.setInterpolator(new DecelerateInterpolator());
        mCardNumberView.startAnimation(anim);
    }

    private void animateOtherFields() {
        float delta = mOtherFieldsContainer.getWidth() - mExpiryView.getLeft();
        float fromXDelta = mIsCardNumberCollapsed ? delta : 0;
        float toXDelta = mIsCardNumberCollapsed ? 0 : delta;
        TranslateAnimation anim = new TranslateAnimation(fromXDelta, toXDelta, 0, 0);
        anim.setDuration(SLIDING_DURATION_MS);
        anim.setFillBefore(true);
        anim.setFillAfter(true);
        anim.setFillEnabled(true);
        anim.setInterpolator(new DecelerateInterpolator());
        mOtherFieldsContainer.startAnimation(anim);
    }

    private void showExpiryAndCvc() {
        mCardNumberView.setClipX((int) mCardNumberSlidingDelta);
        mExpiryView.setVisibility(View.VISIBLE);
        mCvcView.setVisibility(View.VISIBLE);
    }

    private AnimationListener mCardImageAnimationListener = new AnimationListener() {
        public void onAnimationStart(Animation animation) {
            mCardImageBottomView.setVisibility(View.VISIBLE);
        }

        public void onAnimationEnd(Animation animation) {
            Integer resId = (Integer) mCardImageView.getTag();
            mCardImageView.setImageResource(resId);
            mCardImageBottomView.setVisibility(View.INVISIBLE);
        }

        public void onAnimationRepeat(Animation animation) {
            // not needed
        }
    };

    private AnimationListener mCardNumberAnimationListener = new AnimationListener() {
        public void onAnimationStart(Animation animation) {
            mExpiryView.setVisibility(View.VISIBLE);
            mCvcView.setVisibility(View.VISIBLE);
            if (mIsCardNumberCollapsed) {
                mExpiryView.requestFocus();
            }
        }

        public void onAnimationEnd(Animation animation) {
            if (mIsCardNumberCollapsed) {
                showExpiryAndCvc();
            } else {
                mExpiryView.setVisibility(View.GONE);
                mCvcView.setVisibility(View.GONE);
            }
        }

        public void onAnimationRepeat(Animation animation) {
            // not needed
        }
    };

    private void notifyValidationChange() {
        boolean valid = mCard.validateCard();
        if (valid != mLastValidationResult) {
            for (OnValidationChangeListener listener : mListeners) {
                listener.onChange(valid);
            }
        }
        mLastValidationResult = valid;
    }

    private int getImageResourceForCardType() {
        String cardType = mCard.getType();

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
        boolean isAmex = "American Express".equals(mCard.getType());
        int resId = isAmex ? R.drawable.__pk_cvc_amex : R.drawable.__pk_cvc;
        updateCardImage(resId);
    }

    private void updateCardImage(int resId) {
        Integer oldResId = (Integer) mCardImageView.getTag();
        if (oldResId == resId) {
            return;
        }
        mCardImageView.setTag(resId);
        mCardImageBottomView.setImageResource(resId);
        mCardImageView.startAnimation(mScaleOutAnimation);
        mCardImageBottomView.startAnimation(mScaleInAnimation);
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        final SavedState myState = new SavedState(superState);
        myState.mSlidingDelta = mCardNumberSlidingDelta;
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
        mCardNumberSlidingDelta = myState.mSlidingDelta;
    }

    // Save mCardNumberSlidingDelta so we can update the layout for the card number field during
    // onAttachWindow, when the layout of the EditText is not yet initialized.
    private static class SavedState extends BaseSavedState {
        float mSlidingDelta;

        public SavedState(Parcel source) {
            super(source);
            mSlidingDelta = source.readFloat();
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeFloat(mSlidingDelta);
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

    private class CardNumberWatcher implements InputFilter, TextWatcher {
        private boolean mIsUserInput = true;
        private boolean mIsInserting = false;

        private boolean isAllowed(char c) {
            if (c >= '0' && c <= '9') {
                return true;
            }
            return (c == ' ');
        }

        @Override
        public CharSequence filter(CharSequence source, int start, int end,
                Spanned dest, int dstart, int dend) {
            for (int i = start; i < end; ++i) {
                if (!isAllowed(source.charAt(i))) {
                    return "";
                }
            }
            return null;
        }

        @Override
        public void afterTextChanged(Editable s) {
            String number = s.toString();

            String formattedNumber = CardNumberFormatter.format(number, mIsInserting);
            if (!number.equals(formattedNumber)) {
                mIsUserInput = false;
                s.replace(0, s.length(), formattedNumber);
                return;
            }

            mCard.setNumber(number);
            updateFields(true);

            notifyValidationChange();

            mIsUserInput = true;
        }
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            if (mIsUserInput) {
                mIsInserting = (after > count);
            }
        }
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // not needed
        }
    }

    private class ExpiryWatcher implements InputFilter, TextWatcher {
        private static final int EXPIRY_MAX_LENGTH = 5;

        private CardExpiry mCardExpiry = new CardExpiry();
        private boolean mIsInserting = false;

        @Override
        public CharSequence filter(CharSequence source, int start, int end,
                Spanned dest, int dstart, int dend) {
            StringBuffer buf = new StringBuffer();
            buf.append(dest.subSequence(0, dstart));
            buf.append(source.subSequence(start, end));
            buf.append(dest.subSequence(dend, dest.length()));
            String str = buf.toString();
            if (str.length() == 0) {
                // Jump to previous field when user empties this one
                mCardNumberView.requestFocus();
                return null;
            }
            if (str.length() > EXPIRY_MAX_LENGTH) {
                return "";
            }
            mCardExpiry.updateFromString(str);
            return mCardExpiry.isPartiallyValid() ? null : "";
        }

        @Override
        public void afterTextChanged(Editable s) {
            String str = s.toString();
            mCardExpiry.updateFromString(str);
            mCard.setExpMonth(mCardExpiry.getMonth());
            mCard.setExpYear(mCardExpiry.getYear());
            if (mCardExpiry.isPartiallyValid()) {
                String formattedString = mIsInserting ?
                        mCardExpiry.toStringWithTrail() : mCardExpiry.toString();
                if (!str.equals(formattedString)) {
                    s.replace(0, s.length(), formattedString);
                }
            }
            if (mCardExpiry.isValid()) {
                mCvcView.requestFocus();
            }

            notifyValidationChange();
        }
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            mIsInserting = (after > count);
        }
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // not needed
        }
    }

    private class CvcWatcher implements TextWatcher {
        @Override
        public void afterTextChanged(Editable s) {
            mCard.setCVC(TextUtils.nullIfBlank(s.toString()));
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

    // Jump to previous field when user hits backspace on an empty field
    private class EmptyOnKeyListener implements OnKeyListener {
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (keyCode != KeyEvent.KEYCODE_DEL) {
                return false;
            }
            if (event.getAction() != KeyEvent.ACTION_UP) {
                return false;
            }
            if (v.getId() == R.id.__pk_expiry) {
                EditText editText = (EditText) v;
                if (editText.getText().length() == 0) {
                    mCardNumberView.requestFocus();
                    return false;
                }
            }
            if (v.getId() == R.id.__pk_cvc) {
                EditText editText = (EditText) v;
                if (editText.getText().length() == 0) {
                    mExpiryView.requestFocus();
                    return false;
                }
            }
            return false;
        }
    };

    // Jump to expiry date field when user empties the cvc field
    private class CvcEmptyFilter implements InputFilter {
        @Override
        public CharSequence filter(CharSequence source, int start, int end,
                Spanned dest, int dstart, int dend) {
            int n = (dstart - 0) + (end - start) + (dest.length() - dend);
            if (n == 0) {
                mExpiryView.requestFocus();
            }
            return null;
        }
    }
}