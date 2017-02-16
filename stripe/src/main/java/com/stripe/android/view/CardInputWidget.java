package com.stripe.android.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.text.InputFilter;
import android.text.Layout;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.Transformation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.stripe.android.R;
import com.stripe.android.model.Card;
import com.stripe.android.util.CardUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * A card input widget that handles all animation on its own.
 */
public class CardInputWidget extends LinearLayout {

    private static final int END_INDEX_COMMON = 14;
    private static final int END_INDEX_AMEX = 11;

    private static final String PEEK_TEXT_COMMON = "4242";
    private static final String PEEK_TEXT_DINERS = "88";
    private static final String PEEK_TEXT_AMEX = "34343";

    private static final String CVC_PLACEHOLDER_COMMON = "CVC";
    private static final String CVC_PLACEHOLDER_AMEX = "2345";

    // These intentionally include a space at the end.
    private static final String HIDDEN_TEXT_AMEX = "3434 343434 ";
    private static final String HIDDEN_TEXT_COMMON = "4242 4242 4242 ";

    private static final Map<String , Integer> BRAND_RESOURCE_MAP =
            new HashMap<String , Integer>() {{
                put(Card.AMERICAN_EXPRESS, R.drawable.stp_card_amex);
                put(Card.DINERS_CLUB, R.drawable.stp_card_diners);
                put(Card.DISCOVER, R.drawable.stp_card_discover);
                put(Card.JCB, R.drawable.stp_card_jcb);
                put(Card.MASTERCARD, R.drawable.stp_card_mastercard);
                put(Card.VISA, R.drawable.stp_card_visa);
                put(Card.UNKNOWN, R.drawable.stp_card_placeholder_template);
            }};

    private ImageView mCardIconImageView;
    private CardNumberEditText mCardNumberEditText;
    private CardInputView.CustomWidthSetter mCustomWidthSetter;
    private StripeEditText mCvcNumberEditText;
    private ExpiryDateEditText mExpiryDateEditText;

    private FrameLayout mFrameLayout;

    private boolean mCardNumberIsViewed = true;
    private @ColorInt int mErrorColorInt;
    private @ColorInt int mTintColorInt;

    private boolean mInitFlag;

    private PlacementParameters mPlacementParameters;

    public CardInputWidget(Context context) {
        super(context);
        initView(null);
    }

    public CardInputWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(attrs);
    }

    public CardInputWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(attrs);
    }

    private void initView(AttributeSet attrs) {
        inflate(getContext(), R.layout.card_input_alternate_view, this);

        mPlacementParameters = new PlacementParameters();
        mCardIconImageView = (ImageView) findViewById(R.id.iv_card_icon);
        mCardNumberEditText = (CardNumberEditText) findViewById(R.id.et_card_number);
        mExpiryDateEditText = (ExpiryDateEditText) findViewById(R.id.et_expiry_date);
        mCvcNumberEditText = (StripeEditText) findViewById(R.id.et_cvc_number);

        mCardNumberIsViewed = true;

        mFrameLayout = (FrameLayout) findViewById(R.id.frame_container);
        mErrorColorInt = mCardNumberEditText.getDefaultErrorColorInt();
        mTintColorInt = mCardNumberEditText.getHintTextColors().getDefaultColor();
        if (attrs != null) {
            TypedArray a = getContext().getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.CardInputView,
                    0, 0);

            try {
                mErrorColorInt =
                        a.getColor(R.styleable.CardInputView_cardTextErrorColor, mErrorColorInt);
                mTintColorInt =
                        a.getColor(R.styleable.CardInputView_cardTint, mTintColorInt);
            } finally {
                a.recycle();
            }
        }

        mCardNumberEditText.setErrorColor(mErrorColorInt);
        mExpiryDateEditText.setErrorColor(mErrorColorInt);
        mCvcNumberEditText.setErrorColor(mErrorColorInt);

        mCardNumberEditText.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    scrollLeft();
                }
            }
        });

        mExpiryDateEditText.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    scrollRight();
                }
            }
        });

        mCvcNumberEditText.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    scrollRight();
                }
            }
        });

        mExpiryDateEditText.setDeleteEmptyListener(
                new CardInputWidget.BackUpFieldDeleteListener(mCardNumberEditText));

        mCvcNumberEditText.setDeleteEmptyListener(
                new CardInputWidget.BackUpFieldDeleteListener(mExpiryDateEditText));

        mCvcNumberEditText.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                updateIconCvc(mCardNumberEditText.getCardBrand(), hasFocus);
            }
        });

        mCardNumberEditText.setCardNumberCompleteListener(
                new CardNumberEditText.CardNumberCompleteListener() {
                    @Override
                    public void onCardNumberComplete() {
                        scrollRight();
                    }
                });

        mCardNumberEditText.setCardBrandChangeListener(
                new CardNumberEditText.CardBrandChangeListener() {
                    @Override
                    public void onCardBrandChanged(@NonNull @Card.CardBrand String brand) {
                        updateIcon(brand);
                        updateCvc(brand);
                    }
                });

        mExpiryDateEditText.setExpiryDateEditListener(new ExpiryDateEditText.ExpiryDateEditListener() {
            @Override
            public void onExpiryDateComplete() {
                mCvcNumberEditText.requestFocus();
            }
        });

        mCardNumberEditText.requestFocus();
    }

    private void scrollLeft() {
        if (mCardNumberIsViewed) {
            return;
        }

        final int dateStartPosition =
                mPlacementParameters.peekCardWidth + mPlacementParameters.cardDateSeparation;
        final int cvcStartPosition =
                dateStartPosition
                        + mPlacementParameters.dateWidth + mPlacementParameters.dateCvcSeparation;

        updateSpaceSizes(true);

        final int startPoint = ((FrameLayout.LayoutParams)
                mCardNumberEditText.getLayoutParams()).leftMargin;
        Animation slideCardLeftAnimation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                super.applyTransformation(interpolatedTime, t);
                FrameLayout.LayoutParams params =
                        (FrameLayout.LayoutParams) mCardNumberEditText.getLayoutParams();
                params.leftMargin = (int) (startPoint * (1 - interpolatedTime));
                mCardNumberEditText.setLayoutParams(params);
            }
        };

        final int dateDestination =
                mPlacementParameters.cardWidth + mPlacementParameters.cardDateSeparation;
        Animation slideDateLeftAnimation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                super.applyTransformation(interpolatedTime, t);
                int tempValue =
                        (int) (interpolatedTime * dateDestination
                                + (1 - interpolatedTime) * dateStartPosition);
                FrameLayout.LayoutParams params =
                        (FrameLayout.LayoutParams) mExpiryDateEditText.getLayoutParams();
                params.leftMargin = tempValue;
                mExpiryDateEditText.setLayoutParams(params);
            }
        };

        final int cvcDestination = cvcStartPosition + (dateDestination - dateStartPosition);
        Animation slideCvcLeftAnimation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                super.applyTransformation(interpolatedTime, t);
                int tempValue =
                        (int) (interpolatedTime * cvcDestination
                                + (1 - interpolatedTime) * cvcStartPosition);
                FrameLayout.LayoutParams params =
                        (FrameLayout.LayoutParams) mCvcNumberEditText.getLayoutParams();
                params.leftMargin = tempValue;
                params.rightMargin = 0;
                params.width = mPlacementParameters.cvcWidth;
                mCvcNumberEditText.setLayoutParams(params);
            }
        };

        slideCardLeftAnimation.setAnimationListener(new AnimationEndListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                mCardNumberEditText.requestFocus();
            }
        });

        slideCardLeftAnimation.setDuration(150L);
        slideDateLeftAnimation.setDuration(150L);
        slideCvcLeftAnimation.setDuration(150L);

        AnimationSet animationSet = new AnimationSet(true);
        animationSet.addAnimation(slideCardLeftAnimation);
        animationSet.addAnimation(slideDateLeftAnimation);
        animationSet.addAnimation(slideCvcLeftAnimation);
        mFrameLayout.startAnimation(animationSet);
        mCardNumberIsViewed = true;
    }

    private void scrollRight() {
        if (!mCardNumberIsViewed) {
            return;
        }

        final int dateStartMargin = mPlacementParameters.cardWidth
                + mPlacementParameters.cardDateSeparation;

        updateSpaceSizes(false);

        Animation slideCardRightAnimation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                super.applyTransformation(interpolatedTime, t);
                FrameLayout.LayoutParams cardParams =
                        (FrameLayout.LayoutParams) mCardNumberEditText.getLayoutParams();
                cardParams.leftMargin =
                        (int) (-1 * mPlacementParameters.hiddenCardWidth * interpolatedTime);
                mCardNumberEditText.setLayoutParams(cardParams);
            }
        };

        final int dateDestination =
                mPlacementParameters.peekCardWidth
                + mPlacementParameters.cardDateSeparation;

        Animation slideDateRightAnimation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                super.applyTransformation(interpolatedTime, t);
                int tempValue =
                        (int) (interpolatedTime * dateDestination
                                + (1 - interpolatedTime) * dateStartMargin);
                FrameLayout.LayoutParams dateParams =
                        (FrameLayout.LayoutParams) mExpiryDateEditText.getLayoutParams();
                dateParams.leftMargin = tempValue;
                mExpiryDateEditText.setLayoutParams(dateParams);
            }
        };


        final int cvcDestination =
                mPlacementParameters.peekCardWidth
                + mPlacementParameters.cardDateSeparation
                + mPlacementParameters.dateWidth
                + mPlacementParameters.dateCvcSeparation;
        final int cvcStartMargin = cvcDestination + (dateStartMargin - dateDestination);

        Animation slideCvcRightAnimation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                super.applyTransformation(interpolatedTime, t);
                int tempValue =
                        (int) (interpolatedTime * cvcDestination
                                + (1 - interpolatedTime) * cvcStartMargin);
                FrameLayout.LayoutParams cardParams =
                        (FrameLayout.LayoutParams) mCvcNumberEditText.getLayoutParams();
                cardParams.leftMargin = tempValue;
                cardParams.rightMargin = 0;
                cardParams.width = mPlacementParameters.cvcWidth;
                mCvcNumberEditText.setLayoutParams(cardParams);
            }
        };

        slideCardRightAnimation.setDuration(150L);
        slideDateRightAnimation.setDuration(150L);
        slideCvcRightAnimation.setDuration(150L);

        slideCardRightAnimation.setAnimationListener(new AnimationEndListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                mExpiryDateEditText.requestFocus();
            }
        });

        AnimationSet animationSet = new AnimationSet(true);
        animationSet.addAnimation(slideCardRightAnimation);
        animationSet.addAnimation(slideDateRightAnimation);
        animationSet.addAnimation(slideCvcRightAnimation);

        mFrameLayout.startAnimation(animationSet);
        mCardNumberIsViewed = false;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (!mInitFlag && getWidth() != 0) {
            mInitFlag = true;
            int totalWidth = mFrameLayout.getWidth();

            updateSpaceSizes(true);

            FrameLayout.LayoutParams cardParams =
                    (FrameLayout.LayoutParams) mCardNumberEditText.getLayoutParams();
            cardParams.leftMargin = 0;
            cardParams.width = mPlacementParameters.cardWidth;
            mCardNumberEditText.setLayoutParams(cardParams);

            FrameLayout.LayoutParams editTextParams =
                    (FrameLayout.LayoutParams) mExpiryDateEditText.getLayoutParams();
            editTextParams.leftMargin =
                    mPlacementParameters.cardWidth + mPlacementParameters.cardDateSeparation;
            editTextParams.width = mPlacementParameters.dateWidth;
            mExpiryDateEditText.setLayoutParams(editTextParams);

            FrameLayout.LayoutParams cvcParams =
                    (FrameLayout.LayoutParams) mCvcNumberEditText.getLayoutParams();
            cvcParams.width = mPlacementParameters.cvcWidth;
            cvcParams.leftMargin = totalWidth;
            cvcParams.rightMargin = 0;
            mCvcNumberEditText.setLayoutParams(cvcParams);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (hasWindowFocus) {
            applyTint();
        }
    }

    private void updateSpaceSizes(boolean isCardViewed) {
        int frameWidth = mFrameLayout.getWidth();

        mPlacementParameters.cardWidth =
                (int) Layout.getDesiredWidth("4242 4242 4242 4242", mCardNumberEditText.getPaint());

        mPlacementParameters.dateWidth =
                (int) Layout.getDesiredWidth("MM/YY", mExpiryDateEditText.getPaint());

        mPlacementParameters.hiddenCardWidth =
                (int) Layout.getDesiredWidth(
                        getHiddenTextForBrand(mCardNumberEditText.getCardBrand()),
                        mCardNumberEditText.getPaint());
        mPlacementParameters.cvcWidth =
                (int) Layout.getDesiredWidth(
                        getCvcPlaceHolderForBrand(mCardNumberEditText.getCardBrand()),
                        mCvcNumberEditText.getPaint());
        mPlacementParameters.peekCardWidth =
                (int) Layout.getDesiredWidth(
                        getPeekCardTextForBrand(mCardNumberEditText.getCardBrand()),
                        mCardNumberEditText.getPaint());

        if (isCardViewed) {
            mPlacementParameters.cardDateSeparation = frameWidth
                    - mPlacementParameters.cardWidth - mPlacementParameters.dateWidth;
        } else {
            mPlacementParameters.cardDateSeparation = frameWidth / 2
                    - mPlacementParameters.peekCardWidth
                    - mPlacementParameters.dateWidth / 2;
            mPlacementParameters.dateCvcSeparation = frameWidth
                    - mPlacementParameters.peekCardWidth
                    - mPlacementParameters.cardDateSeparation
                    - mPlacementParameters.dateWidth
                    - mPlacementParameters.cvcWidth;
        }
    }

    private String getHiddenTextForBrand(@NonNull @Card.CardBrand String brand) {
        if (Card.AMERICAN_EXPRESS.equals(brand)) {
            return HIDDEN_TEXT_AMEX;
        } else {
            return HIDDEN_TEXT_COMMON;
        }
    }

    private String getCvcPlaceHolderForBrand(@NonNull @Card.CardBrand String brand) {
        if (Card.AMERICAN_EXPRESS.equals(brand)) {
            return CVC_PLACEHOLDER_AMEX;
        } else {
            return CVC_PLACEHOLDER_COMMON;
        }
    }

    private String getPeekCardTextForBrand(@NonNull @Card.CardBrand String brand) {
        if (Card.AMERICAN_EXPRESS.equals(brand)) {
            return PEEK_TEXT_AMEX;
        } else if (Card.DINERS_CLUB.equals(brand)) {
            return PEEK_TEXT_DINERS;
        } else {
            return PEEK_TEXT_COMMON;
        }
    }

    private void applyTint() {
        if (Card.UNKNOWN.equals(mCardNumberEditText.getCardBrand())) {
            Drawable icon = mCardIconImageView.getDrawable();
            Drawable compatIcon = DrawableCompat.wrap(icon);
            DrawableCompat.setTint(compatIcon, mTintColorInt);
            mCardIconImageView.setImageDrawable(DrawableCompat.unwrap(compatIcon));
        }
    }

    private void updateCvc(@NonNull @Card.CardBrand String brand) {
        if (Card.AMERICAN_EXPRESS.equals(brand)) {
            mCvcNumberEditText.setFilters(
                    new InputFilter[] {new InputFilter.LengthFilter(CardUtils.CVC_LENGTH_AMEX)});
            mCvcNumberEditText.setHint(R.string.cvc_amex_hint);
        } else {
            mCvcNumberEditText.setFilters(
                    new InputFilter[] {new InputFilter.LengthFilter(CardUtils.CVC_LENGTH_COMMON)});
            mCvcNumberEditText.setHint(R.string.cvc_number_hint);
        }
    }

    private void updateIcon(@NonNull @Card.CardBrand String brand) {
        if (Card.UNKNOWN.equals(brand)) {
            Drawable icon  = getResources().getDrawable(R.drawable.stp_card_placeholder_template);
            mCardIconImageView.setImageDrawable(icon);
            applyTint();
        } else {
            mCardIconImageView.setImageResource(BRAND_RESOURCE_MAP.get(brand));
        }
    }

    private void updateIconCvc(@NonNull @Card.CardBrand String brand, boolean isEntering) {
        if (isEntering) {
            if (Card.AMERICAN_EXPRESS.equals(brand)) {
                mCardIconImageView.setImageResource(R.drawable.stp_card_cvc_amex);
            } else {
                mCardIconImageView.setImageResource(R.drawable.stp_card_cvc);
            }
        } else {
            updateIcon(brand);
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

    /**
     * A data-dump class.
     */
    private class PlacementParameters {
        private int cardWidth;
        private int hiddenCardWidth;
        private int peekCardWidth;
        private int cardDateSeparation;
        private int dateWidth;
        private int dateCvcSeparation;
        private int cvcWidth;

        @Override
        public String toString() {
            return String.format("CardWidth = %d\n" +
                    "HiddenCardWidth = %d\n" +
                    "PeekCardWidth = %d\n" +
                    "CardDateSeparation = %d\n" +
                    "DateWidth = %d\n" +
                    "DateCvcSeparation = %d\n" +
                    "CvcWidth = %d",
                    cardWidth,
                    hiddenCardWidth,
                    peekCardWidth,
                    cardDateSeparation,
                    dateWidth,
                    dateCvcSeparation,
                    cvcWidth);
        }
    }

    /**
     * A convenience class for when we only want to listen for when an animation ends.
     */
    private abstract class AnimationEndListener implements Animation.AnimationListener {
        @Override
        public void onAnimationStart(Animation animation) {
            // Intentional No-op
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
            // Intentional No-op
        }
    }

}
