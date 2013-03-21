package com.stripe.android.widget;

import com.stripe.android.R;

import android.content.Context;
import android.text.Editable;
import android.text.Layout;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.widget.EditText;
import android.widget.FrameLayout;

public class PaymentKitView extends FrameLayout {
    private static final long SLIDING_DURATION_MS = 500;

    private EditText cardNumberView;
    private EditText expiryView;
    private EditText cvcView;
    private EditText zipCodeView;

    private float cardNumberSlidingDelta = 0;
    private boolean isCardNumberCollapsed = false;
    private int cardNumberOriginalLeftMargin = -1;

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

    private void init() {
        LayoutInflater inflater = (LayoutInflater) getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View parent = inflater.inflate(R.layout.__pk_view, this);

        cardNumberView = (EditText) parent.findViewById(R.id.__pk_card_number);
        expiryView = (EditText) parent.findViewById(R.id.__pk_expiry);
        cvcView = (EditText) parent.findViewById(R.id.__pk_cvc);
        zipCodeView = (EditText) parent.findViewById(R.id.__pk_zipcode);

        cardNumberView.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (cardNumberView.length() == 19) {
                    collapseCardNumber();
                }
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // not needed
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // not needed
            }
        });
        cardNumberView.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus && isCardNumberCollapsed) {
                    expandCardNumber();
                }
            }
        });

        FrameLayout.LayoutParams params
            = (FrameLayout.LayoutParams) cardNumberView.getLayoutParams();
        cardNumberOriginalLeftMargin = params.leftMargin;
    }

    private float computeCardNumberSlidingDelta() {
        Layout layout = cardNumberView.getLayout();
        return layout.getPrimaryHorizontal(15);
    }

    private void collapseCardNumber() {
        cardNumberSlidingDelta = computeCardNumberSlidingDelta();
        isCardNumberCollapsed = true;
        animateCardNumber();
    }

    private void expandCardNumber() {
        isCardNumberCollapsed = false;
        animateCardNumber();
    }

    private void animateCardNumber() {
        float fromXDelta = isCardNumberCollapsed ? 0
                : -cardNumberSlidingDelta;
        float toXDelta = isCardNumberCollapsed ? -cardNumberSlidingDelta : 0;
        TranslateAnimation anim = new TranslateAnimation(fromXDelta, toXDelta,
                0, 0);
        anim.setDuration(SLIDING_DURATION_MS);
        anim.setAnimationListener(mAnimationListener);
        cardNumberView.startAnimation(anim);
    }

    private AnimationListener mAnimationListener = new AnimationListener() {
        public void onAnimationEnd(Animation animation) {
            if (!isCardNumberCollapsed) {
                return;
            }

            FrameLayout.LayoutParams params
                = (FrameLayout.LayoutParams) cardNumberView.getLayoutParams();
            params.leftMargin = (int) (cardNumberOriginalLeftMargin - cardNumberSlidingDelta);
            cardNumberView.setLayoutParams(params);

            expiryView.setVisibility(View.VISIBLE);
            cvcView.setVisibility(View.VISIBLE);
            zipCodeView.setVisibility(View.VISIBLE);

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
            zipCodeView.setVisibility(View.GONE);

            FrameLayout.LayoutParams params
                = (FrameLayout.LayoutParams) cardNumberView.getLayoutParams();
            params.leftMargin = cardNumberOriginalLeftMargin;
            cardNumberView.setLayoutParams(params);
        }
    };
}