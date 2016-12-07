package com.stripe.example.view;

import android.content.Context;
import android.content.res.Configuration;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.Space;
import android.text.Editable;
import android.text.Layout;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.stripe.android.model.Card;
import com.stripe.android.util.StripeTextUtils;
import com.stripe.example.R;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by simonkenny on 05/12/2016.
 */

public class CreditCardView extends FrameLayout {

    public static final int ERROR_NONE = 0;
    public static final int ERROR_NUMBER = 1;
    public static final int ERROR_EXPIRY_MONTH = 2;
    public static final int ERROR_EXPIRY_YEAR = 3;
    public static final int ERROR_CVC = 4;
    public static final int ERROR_UNKNOWN = 5;

    /**
     * Callback interface, set using setCallback(Callback callback)
     *
     * Required to make this useful
     */
    public interface Callback {
        void onValidated(Card card);
        void onError(int errorCode);
        void onClearError();
    }

    //Some static helper methods, would be best to put these in a Util class

    private static int sScreenWidth = 0;

    public static int getScreenWidth(Context context) {
        if (sScreenWidth == 0) {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            DisplayMetrics dm = new DisplayMetrics();
            wm.getDefaultDisplay().getMetrics(dm);
            if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                sScreenWidth = Math.max(dm.widthPixels, dm.heightPixels);
            } else {
                sScreenWidth = Math.min(dm.widthPixels, dm.heightPixels);
            }
        }
        return sScreenWidth;
    }

    public static int getImageResForCardBrand(String brand){
        if (brand.equals(Card.AMERICAN_EXPRESS)) {
            return R.drawable.stp_card_amex;
        }
        if (brand.equals(Card.DINERS_CLUB)) {
            return R.drawable.stp_card_diners;
        }
        if (brand.equals(Card.DISCOVER)) {
            return R.drawable.stp_card_discover;
        }
        if (brand.equals(Card.JCB)) {
            return R.drawable.stp_card_jcb;
        }
        if (brand.equals(Card.MASTERCARD)) {
            return R.drawable.stp_card_mastercard;
        }
        if (brand.equals(Card.VISA)) {
            return R.drawable.stp_card_visa;
        }
        return R.drawable.stp_card_placeholder_template;

    }

    CustomHorizontalScrollView mScrollView;
    Space mSpaceInContainer;
    ImageView mIvCreditCardIcon;
    EditText mEtNumber;
    EditText mEtExpiryDate;
    EditText mEtCvcNum;

    private Callback mCallback;

    private int mScrollViewWidth;
    private int mScrollToPosition;
    private boolean mCardNumberInView = true;
    private int mColor;

    private Card mCard;
    private int mError;

    public void setCallback(Callback callback) {
        this.mCallback = callback;
    }

    public CreditCardView(Context context) {
        super(context);
        initView();
    }

    public CreditCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public CreditCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    private void bindViews() {
        mScrollView = (CustomHorizontalScrollView) findViewById(R.id.root_scroll_view);
        mSpaceInContainer = (Space) findViewById(R.id.space_in_container);
        mIvCreditCardIcon = (ImageView) findViewById(R.id.iv_credit_card_icon);
        mEtNumber = (EditText) findViewById(R.id.et_credit_card_number);
        mEtExpiryDate = (EditText) findViewById(R.id.et_expiry_date);
        mEtCvcNum = (EditText) findViewById(R.id.et_cvc_num);
    }

    private void initView() {
        inflate(getContext(), R.layout.view_credit_card, this);

        // TODO : could also do this with ButterKnife
        bindViews();

        mError = ERROR_NONE;

        mColor = ContextCompat.getColor(getContext(), android.R.color.white);
        mScrollView.setScrollingEnabled(false);

        mScrollViewWidth = (int) (getScreenWidth(getContext()) * 0.65f);

        mScrollView.getLayoutParams().width = mScrollViewWidth;
        mEtNumber.getLayoutParams().width = mScrollViewWidth;
        mSpaceInContainer.getLayoutParams().width = mScrollViewWidth;

        mScrollToPosition = mScrollViewWidth;

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());

        mEtNumber.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean focused) {
                if (focused) {
                    scrollLeft();
                }
            }
        });

        mEtNumber.addTextChangedListener(new TextWatcher() {
            boolean freeze = false;
            int beforeStringLen = 0;

            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
                if (!freeze) {
                    beforeStringLen = mEtNumber.getText().toString().length();
                }
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                if (!freeze) {
                    String curString = mEtNumber.getText().toString();
                    int curStringLen = curString.length();

                    mEtNumber.setTextColor(mColor);

                    // reformat with spaces
                    // TODO : add formatting for non-16 digit cards
                    String cleanedString = curString.replace(" ", "");
                    int cleanedStringLen = cleanedString.length();
                    String formattedString = "";
                    for (int i = 0 ; i < 4 ; i++) {
                        if (cleanedStringLen > (i * 4) + 4) {
                            formattedString += cleanedString.substring((i * 4), ((i * 4) + 4)) + " ";
                        } else {
                            formattedString += cleanedString.substring((i * 4), cleanedStringLen);
                            break;
                        }
                    }
                    freeze = true;
                    mEtNumber.setText(formattedString);
                    freeze = false;

                    // always move cursor (selection) to end
                    freeze = true;
                    mEtNumber.setSelection(mEtNumber.getText().toString().length());
                    freeze = false;

                    mCard = new Card.Builder(formattedString, 1, 0, "").build();
                    setCreditCardIconForNumber();

                    // scroll to right if entered last number of card
                    if (beforeStringLen == 18 && curStringLen == 19) {
                        if (mCard.validateNumber()) {
                            freeze = true;
                            scrollRight();
                            freeze = false;
                            if (mCallback != null) {
                                mCallback.onClearError();
                            }
                        } else {
                            mEtNumber.setTextColor(ContextCompat.getColor(getContext(), android.R.color.holo_red_dark));
                            if (mCallback != null) {
                                mCallback.onError(ERROR_NUMBER);
                            }
                        }
                    }

                    sendCardValidationCallback();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                //not used
            }
        });

        mEtExpiryDate.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (mEtExpiryDate.getText().toString().length() == 0) {
                        mEtNumber.setText(mEtNumber.getText().toString().substring(0, mEtNumber.getText().toString().length() - 1));
                        mEtNumber.setSelection(mEtNumber.getText().toString().length());
                        mEtNumber.requestFocus();
                    }
                }
                return false;
            }
        });

        mEtExpiryDate.addTextChangedListener(new TextWatcher() {
            boolean freeze = false;
            boolean hadSlash = false;

            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
                if (!freeze) {
                    hadSlash = mEtExpiryDate.getText().toString().contains("/");
                }
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                if (!freeze) {
                    String curString = mEtExpiryDate.getText().toString();

                    if (curString.isEmpty()) {
                        return;
                    }

                    if (hadSlash && !mEtExpiryDate.getText().toString().contains("/")) {
                        freeze = true;
                        mEtExpiryDate.setText("");
                        freeze = false;
                        return;
                    }

                    mEtExpiryDate.setTextColor(mColor);

                    String monthStr = null;
                    String yearStr = null;

                    String []parts = curString.split("[/]");
                    if (parts.length > 0 && !StripeTextUtils.isBlank(parts[0])) {
                        try {
                            monthStr = parts[0].substring(0, parts[0].length() > 2 ? 2 : parts[0].length());
                            int month = Integer.parseInt(monthStr);
                            mCard.setExpMonth(month);
                            if (parts[0].length() == 1 && month > 1) {
                                monthStr = String.format("%02d", month);
                            }
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                            freeze = true;
                            mEtExpiryDate.setText("");
                            freeze = false;
                            return;
                        }
                        if (mCard.validateExpMonth() && parts.length > 1) {
                            try {
                                yearStr = parts[1].substring(0, parts[1].length() > 2 ? 2 : parts[1].length());
                                int year = Integer.parseInt(yearStr);
                                mCard.setExpYear(year);
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                                freeze = true;
                                mEtExpiryDate.setText("");
                                freeze = false;
                                return;
                            }
                        }
                    }

                    freeze = true;
                    if (!StripeTextUtils.isBlank(monthStr)) {
                        if (mCard.validateExpMonth() && monthStr.length() > 1) {
                            mEtExpiryDate.setText(String.format("%s/%s", monthStr, yearStr != null ? yearStr : ""));
                        } else {
                            mEtExpiryDate.setText(String.format("%s", monthStr));
                        }
                    } else {
                        mEtExpiryDate.setText("");
                    }
                    mEtExpiryDate.setSelection(mEtExpiryDate.getText().toString().length());
                    freeze = false;

                    boolean error = false;

                    if (!StripeTextUtils.isBlank(monthStr) || !StripeTextUtils.isBlank(yearStr)) {
                        if ((!StripeTextUtils.isBlank(monthStr) && monthStr.length() == 2 && !mCard.validateExpMonth())) {
                            mEtExpiryDate.setTextColor(ContextCompat.getColor(getContext(), android.R.color.holo_red_dark));
                            if (mCallback != null) {
                                mCallback.onError(ERROR_EXPIRY_MONTH);
                            }
                            error = true;
                        } else if ((!StripeTextUtils.isBlank(yearStr) && yearStr.length() == 2 && !mCard.validateExpYear())) {
                            mEtExpiryDate.setTextColor(ContextCompat.getColor(getContext(), android.R.color.holo_red_dark));
                            if (mCallback != null) {
                                mCallback.onError(ERROR_EXPIRY_YEAR);
                            }
                            error = true;
                        } else if (!StripeTextUtils.isBlank(yearStr) && yearStr.length() == 2) { // only move to next field when year entered
                            mEtCvcNum.requestFocus();
                        }
                    }
                    if (!error) {
                        if (mCallback != null) {
                            mCallback.onClearError();
                        }
                    }

                    sendCardValidationCallback();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                //not used
            }
        });

        mEtCvcNum.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean focus) {
                if (focus) {
                    setCreditCardIconForCvc();
                } else {
                    setCreditCardIconForNumber();
                }
            }
        });

        mEtCvcNum.addTextChangedListener(new TextWatcher() {
            boolean freeze = false;
            int beforeStringLen = 0;

            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
                if (!freeze) {
                    beforeStringLen = mEtCvcNum.getText().toString().length();
                }
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                if (!freeze) {
                    String curString = mEtCvcNum.getText().toString();
                    int curStringLen = curString.length();

                    // TODO : this is deprecated, but what else should be used?
                    mCard.setCVC(curString);

                    if (beforeStringLen > curStringLen) {
                        // removing space
                        if (beforeStringLen == 1) {
                            mEtExpiryDate.requestFocus();
                            return;
                        }
                    }

                    // don't send CVC validation messages, interface does not respond to short CVC numbers
                    // however, ERROR_CVC set if isValid() called and CVC not valid

                    sendCardValidationCallback();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                //not used
            }
        });

        mEtCvcNum.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (mEtCvcNum.getText().toString().length() == 0 && mEtExpiryDate.getText().toString().length() > 0) {
                        mEtExpiryDate.setText(mEtExpiryDate.getText().toString().substring(0, mEtExpiryDate.getText().toString().length() - 1));
                        mEtExpiryDate.setSelection(mEtExpiryDate.getText().toString().length());
                        mEtExpiryDate.requestFocus();
                    }
                }
                return false;
            }
        });
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        mEtNumber.setEnabled(enabled);
        mEtExpiryDate.setEnabled(enabled);
        mEtCvcNum.setEnabled(enabled);
    }

    private void scrollRight() {
        if (!mCardNumberInView) {
            return;
        }
        mCardNumberInView = false;
        updateScrollToPosition();
        mScrollView.smoothScrollBy(mScrollToPosition, 0);
        mEtExpiryDate.setVisibility(VISIBLE);
        mEtCvcNum.setVisibility(VISIBLE);
        if (!mEtExpiryDate.getText().toString().isEmpty()) {
            mEtExpiryDate.setSelection(mEtExpiryDate.getText().toString().length());
        }
        mEtExpiryDate.requestFocus();
    }

    private void scrollLeft() {
        if (mCardNumberInView) {
            return;
        }
        mCardNumberInView = true;
        mEtNumber.setSelection(mEtNumber.getText().toString().length());
        mScrollView.smoothScrollBy(-mScrollToPosition, 0);
        mEtExpiryDate.setVisibility(INVISIBLE);
        mEtCvcNum.setVisibility(INVISIBLE);
    }

    private void updateScrollToPosition() {
        String numStr = mEtNumber.getText().toString();
        mScrollToPosition = mScrollViewWidth;
        if (!numStr.isEmpty()) {
            String scrollStr = numStr.substring(0, 14);
            mScrollToPosition = (int) Layout.getDesiredWidth(scrollStr, mEtNumber.getPaint());
        }
    }

    private void setCreditCardIconForNumber() {
        if (mCard == null) {
            return;
        }
        if (mCard.getNumber() != null && mCard.getNumber().length() >= 2) {
            String type = mCard.getBrand();
            if (!type.equals(Card.UNKNOWN)) {
                mIvCreditCardIcon.setImageResource(getImageResForCardBrand(type));
                if (mCallback != null) {
                    mCallback.onClearError();
                }
            } else {
                mIvCreditCardIcon.setImageResource(R.drawable.stp_card_placeholder);
                mEtNumber.setTextColor(ContextCompat.getColor(getContext(), android.R.color.holo_red_dark));
                if (mCallback != null) {
                    mError = ERROR_NUMBER;
                    mCallback.onError(mError);
                }
            }
        } else {
            if (mCallback != null) {
                mCallback.onClearError();
            }
            mIvCreditCardIcon.setImageResource(R.drawable.stp_card_placeholder);
        }
    }

    private void setCreditCardIconForCvc() {
        if (mCard == null) {
            return;
        }
        if (mCard.getBrand().equals(Card.AMERICAN_EXPRESS)) {
            mIvCreditCardIcon.setImageResource(R.drawable.stp_card_cvc_amex);
        } else {
            mIvCreditCardIcon.setImageResource(R.drawable.stp_card_cvc);
        }
    }

    private void sendCardValidationCallback() {
        if (isValid() && mCallback != null) {
            mCallback.onValidated(mCard);
        }
    }

    public void setTextColor(int color){
        mColor = color;
        this.mEtNumber.setTextColor(color);
        this.mEtExpiryDate.setTextColor(color);
        this.mEtCvcNum.setTextColor(color);
    }

    public void setHintTextColor(int color){
        this.mEtNumber.setHintTextColor(color);
        this.mEtExpiryDate.setHintTextColor(color);
        this.mEtCvcNum.setHintTextColor(color);
    }

    public Card getCard() {
        return mCard;
    }

    public int getError() {
        isValid(); //refresh error message
        return mError;
    }

    public boolean isValid() {
        // card should be valid now
        if (mCard != null) {
            if (!mCard.validateNumber()) {
                mError = ERROR_NUMBER;
            } else if (!mCard.validateExpMonth()) {
                mError = ERROR_EXPIRY_MONTH;
            } else if (!mCard.validateExpYear()) {
                mError = ERROR_EXPIRY_YEAR;
            } else if (!mCard.validateCVC() || mCard.getCVC().length() != 3) {
                mError = ERROR_CVC;
            } else if (!mCard.validateCard()) {
                mError = ERROR_UNKNOWN;
            } else {
                mError = ERROR_NONE;
            }
        } else {
            mError = ERROR_UNKNOWN;
        }
        return mError == ERROR_NONE;
    }
}
