package com.stripe.android.view;

import android.content.Context;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.Space;
import android.text.Editable;
import android.text.Layout;
import android.text.TextWatcher;
import android.text.method.KeyListener;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;

import com.stripe.android.R;

/**
 * Custom view to accept credit card numbers information.
 */
public class CardInputView extends FrameLayout {

    private static final int END_INDEX_COMMON = 14;
    private static final int END_INDEX_AMEX = 11;

    private CardNumberEditText mCardNumberEditText;
    private DeleteWatchEditText mCvcNumberEditText;
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

        mScrollView = (LockableHorizontalScrollView) findViewById(R.id.root_scroll_view);
        mCardNumberEditText = (CardNumberEditText) findViewById(R.id.et_card_number);
        mExpiryDateEditText = (ExpiryDateEditText) findViewById(R.id.et_expiry_date);
        mCvcNumberEditText = (DeleteWatchEditText) findViewById(R.id.et_cvc_number);
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
                new DeleteWatchEditText.DeleteEmptyListener() {
                    @Override
                    public void onDeleteEmpty() {
                        mCardNumberEditText.requestFocus();
                    }
                });

        mCvcNumberEditText.setDeleteEmptyListener(
                new DeleteWatchEditText.DeleteEmptyListener() {
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
