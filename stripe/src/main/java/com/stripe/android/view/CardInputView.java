package com.stripe.android.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.text.InputFilter;
import android.text.Layout;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.stripe.android.R;
import com.stripe.android.model.Card;
import com.stripe.android.util.CardUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Custom view to accept credit card numbers information.
 */
public class CardInputView extends FrameLayout {

    private static final int END_INDEX_COMMON = 14;
    private static final int END_INDEX_AMEX = 11;

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
    private StripeEditText mCvcNumberEditText;
    private ExpiryDateEditText mExpiryDateEditText;
    private LockableHorizontalScrollView mScrollView;
    private View mCardNumberSpace;
    private int mScrollViewWidth;
    private int mScrollToPostion;
    private boolean mCardNumberIsViewed;
    private boolean mIsAmex = false;
    private boolean mInitializedFlag;

    public CardInputView(Context context) {
        super(context);
        initView();
    }

    public CardInputView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public CardInputView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    private void initView() {
        inflate(getContext(), R.layout.card_input_view, this);

        mCardIconImageView = (ImageView) findViewById(R.id.iv_card_icon);
        mScrollView = (LockableHorizontalScrollView) findViewById(R.id.root_scroll_view);
        mCardNumberEditText = (CardNumberEditText) findViewById(R.id.et_card_number);
        mExpiryDateEditText = (ExpiryDateEditText) findViewById(R.id.et_expiry_date);
        mCvcNumberEditText = (StripeEditText) findViewById(R.id.et_cvc_number);
        mCardNumberSpace = findViewById(R.id.space_in_container);
        mCardNumberIsViewed = true;

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
                new StripeEditText.DeleteEmptyListener() {
                    @Override
                    public void onDeleteEmpty() {
                        mCardNumberEditText.requestFocus();
                    }
                });

        mCvcNumberEditText.setDeleteEmptyListener(
                new StripeEditText.DeleteEmptyListener() {
                    @Override
                    public void onDeleteEmpty() {
                        mExpiryDateEditText.requestFocus();
                    }
                }
        );

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

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        // We set this length the first time the control is laid out, because the
        // value for the actual width of mScrollView prior to this will be zero.
        if (!mInitializedFlag) {
            mInitializedFlag = true;
            mScrollViewWidth = mScrollView.getMeasuredWidth();
            mScrollToPostion = mScrollViewWidth;
            ViewGroup.LayoutParams widthParams = mCardNumberSpace.getLayoutParams();
            widthParams.width = mScrollViewWidth;
            mCardNumberSpace.setLayoutParams(widthParams);
        }
    }

    private void scrollRight() {
        if (!mCardNumberIsViewed) {
            return;
        }
        mCardNumberIsViewed = false;
        updateScrollToPosition();
        mScrollView.smoothScrollBy(mScrollToPostion, 0);
        mExpiryDateEditText.setVisibility(View.VISIBLE);
        mCvcNumberEditText.setVisibility(View.VISIBLE);
        mExpiryDateEditText.requestFocus();
    }

    private void scrollLeft() {
        if (mCardNumberIsViewed) {
            return;
        }
        mCardNumberIsViewed = true;
        updateScrollToPosition();
        mCardNumberEditText.setSelection(mCardNumberEditText.getText().length());
        mScrollView.smoothScrollBy(-1*mScrollToPostion, 0);
        mExpiryDateEditText.setVisibility(View.INVISIBLE);
        mCvcNumberEditText.setVisibility(View.INVISIBLE);
        mCardNumberEditText.requestFocus();
    }

    private void updateIcon(@NonNull @Card.CardBrand String brand) {
        if (Card.UNKNOWN.equals(brand)) {
            Drawable icon  = getResources().getDrawable(R.drawable.stp_card_placeholder_template);
            Drawable compatIcon = DrawableCompat.wrap(icon);
            DrawableCompat.setTintList(compatIcon, mCardNumberEditText.getHintTextColors());
            mCardIconImageView.setImageDrawable(compatIcon);
        } else {
            mCardIconImageView.setImageResource(BRAND_RESOURCE_MAP.get(brand));
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

    private void updateScrollToPosition() {
        if (!mCardNumberIsViewed) {
            int endScrollIndex = mIsAmex ? END_INDEX_AMEX : END_INDEX_COMMON;
            String cardString = mCardNumberEditText.getText().toString();
            if (cardString.length() >= endScrollIndex) {
                String hiddenString = cardString.substring(0, endScrollIndex);
                mScrollToPostion =
                        (int) Layout.getDesiredWidth(hiddenString, mCardNumberEditText.getPaint());
            }
        } else {
            mScrollToPostion = mScrollViewWidth;
        }
    }
}
