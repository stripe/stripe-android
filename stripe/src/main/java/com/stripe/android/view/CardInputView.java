package com.stripe.android.view;

import android.content.Context;
import android.support.v4.widget.Space;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.widget.EditText;
import android.widget.FrameLayout;

import com.stripe.android.R;

/**
 * Custom view to accept credit card numbers information.
 */
public class CardInputView extends FrameLayout {

    private EditText mCardNumberEditText;
    private EditText mCvcNumberEditText;
    private EditText mExpiryDateEditText;
    private LockableHorizontalScrollView mScrollView;
    private Space mCardNumberSpace;

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
        mCardNumberEditText = (EditText) findViewById(R.id.et_card_number);
        mExpiryDateEditText = (EditText) findViewById(R.id.et_expiry_date);
        mCvcNumberEditText = (EditText) findViewById(R.id.et_cvc_number);
        mCardNumberSpace = (Space) findViewById(R.id.space_in_container);
    }
}
