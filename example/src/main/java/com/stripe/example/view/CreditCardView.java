package com.stripe.example.view;

import android.content.Context;
import android.content.res.Configuration;
import android.support.annotation.DrawableRes;
import android.support.annotation.IntDef;
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
 * Widget to facilitate simple entry of entire card information that users expect, with
 * card number formatting, number and date formatting built in.
 *
 * Callback interface provided for containing validation event triggers. Validation and Card object
 * can also be accessed directly using class methods.
 *
 * Created by simon-marino on 05/12/2016.
 */

public class CreditCardView extends FrameLayout {

    @IntDef({ERROR_NONE, ERROR_NUMBER, ERROR_EXPIRY_MONTH, ERROR_EXPIRY_YEAR,
            ERROR_CVC, ERROR_UNKNOWN})
    public @interface ErrorCode {}
    public static final int ERROR_NONE = 0;
    public static final int ERROR_NUMBER = 1;
    public static final int ERROR_EXPIRY_MONTH = 2;
    public static final int ERROR_EXPIRY_YEAR = 3;
    public static final int ERROR_CVC = 4;
    public static final int ERROR_UNKNOWN = 5;

    /**
     * Callback interface for validation events.
     *
     * Add as listener using setCallback(Callback callback)
     */
    public interface Callback {
        void onValidated(Card card);
        void onError(@ErrorCode int errorCode);
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

    public static @DrawableRes int getImageResForCardBrand(String brand){
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

    private static final float SCROLL_VIEW_SIZE_WIDTH_RATIO = 0.65f;

    private CustomHorizontalScrollView mScrollView;
    private Space mSpaceInContainer;
    private ImageView mCreditCardIconImageView;
    private EditText mCvcEditText;
    private EditText mExpiryDateEditText;
    private EditText mNumberEditText;

    private Callback mCallback;

    private int mScrollViewWidth;
    private int mScrollToPosition;
    private boolean mCardNumberInView = true;
    private int mColor;

    private Card mCard;
    private @ErrorCode int mError;

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
        mCreditCardIconImageView = (ImageView) findViewById(R.id.iv_credit_card_icon);
        mNumberEditText = (EditText) findViewById(R.id.et_credit_card_number);
        mExpiryDateEditText = (EditText) findViewById(R.id.et_expiry_date);
        mCvcEditText = (EditText) findViewById(R.id.et_cvc_num);
    }

    private void initView() {
        inflate(getContext(), R.layout.view_credit_card, this);

        bindViews();

        mError = ERROR_NONE;

        mColor = ContextCompat.getColor(getContext(), android.R.color.white);
        mScrollView.setScrollingEnabled(false);

        mScrollViewWidth = (int) (getScreenWidth(getContext()) * SCROLL_VIEW_SIZE_WIDTH_RATIO);

        mScrollView.getLayoutParams().width = mScrollViewWidth;
        mNumberEditText.getLayoutParams().width = mScrollViewWidth;
        mSpaceInContainer.getLayoutParams().width = mScrollViewWidth;

        mScrollToPosition = mScrollViewWidth;

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());

        mNumberEditText.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean focused) {
                if (focused) {
                    scrollLeft();
                }
            }
        });

        mNumberEditText.addTextChangedListener(new TextWatcher() {
            boolean freeze = false;
            int beforeStringLength = 0;

            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
                if (!freeze) {
                    beforeStringLength = mNumberEditText.getText().toString().length();
                }
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                if (!freeze) {
                    String curString = mNumberEditText.getText().toString();
                    int curStringLength = curString.length();

                    mNumberEditText.setTextColor(mColor);

                    // reformat with spaces
                    // TODO : add formatting for non-16 digit cards
                    String cleanedString = curString.replace(" ", "");
                    int cleanedStringLength = cleanedString.length();
                    String formattedString = "";
                    for (int i = 0; i < 4; i++) {
                        if (cleanedStringLength > (i * 4) + 4) {
                            formattedString += cleanedString.substring((i * 4), ((i * 4) + 4)) + " ";
                        } else {
                            formattedString += cleanedString.substring((i * 4), cleanedStringLength);
                            break;
                        }
                    }
                    freeze = true;
                    mNumberEditText.setText(formattedString);
                    freeze = false;

                    // always move cursor (selection) to end
                    freeze = true;
                    mNumberEditText.setSelection(mNumberEditText.getText().toString().length());
                    freeze = false;

                    mCard = new Card.Builder(formattedString, 1, 0, "").build();
                    setCreditCardIconForNumber();

                    // scroll to right if entered last number of card
                    if (beforeStringLength == 18 && curStringLength == 19) {
                        if (mCard.validateNumber()) {
                            freeze = true;
                            scrollRight();
                            freeze = false;
                            if (mCallback != null) {
                                mCallback.onClearError();
                            }
                        } else {
                            mNumberEditText.setTextColor(ContextCompat.getColor(getContext(), android.R.color.holo_red_dark));
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

        mExpiryDateEditText.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (mExpiryDateEditText.getText().toString().length() == 0) {
                        mNumberEditText.setText(mNumberEditText.getText().toString().substring(0, mNumberEditText.getText().toString().length() - 1));
                        mNumberEditText.setSelection(mNumberEditText.getText().toString().length());
                        mNumberEditText.requestFocus();
                    }
                }
                return false;
            }
        });

        mExpiryDateEditText.addTextChangedListener(new TextWatcher() {
            boolean freeze = false;
            boolean hadSlash = false;

            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
                if (!freeze) {
                    hadSlash = mExpiryDateEditText.getText().toString().contains("/");
                }
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                if (!freeze) {
                    String curString = mExpiryDateEditText.getText().toString();

                    if (curString.isEmpty()) {
                        return;
                    }

                    if (hadSlash && !mExpiryDateEditText.getText().toString().contains("/")) {
                        freeze = true;
                        mExpiryDateEditText.setText("");
                        freeze = false;
                        return;
                    }

                    mExpiryDateEditText.setTextColor(mColor);

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
                            mExpiryDateEditText.setText("");
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
                                mExpiryDateEditText.setText("");
                                freeze = false;
                                return;
                            }
                        }
                    }

                    freeze = true;
                    if (!StripeTextUtils.isBlank(monthStr)) {
                        if (mCard.validateExpMonth() && monthStr.length() > 1) {
                            mExpiryDateEditText.setText(String.format("%s/%s", monthStr, yearStr != null ? yearStr : ""));
                        } else {
                            mExpiryDateEditText.setText(String.format("%s", monthStr));
                        }
                    } else {
                        mExpiryDateEditText.setText("");
                    }
                    mExpiryDateEditText.setSelection(mExpiryDateEditText.getText().toString().length());
                    freeze = false;

                    boolean error = false;

                    if (!StripeTextUtils.isBlank(monthStr) || !StripeTextUtils.isBlank(yearStr)) {
                        if ((!StripeTextUtils.isBlank(monthStr) && monthStr.length() == 2 && !mCard.validateExpMonth())) {
                            mExpiryDateEditText.setTextColor(ContextCompat.getColor(getContext(), android.R.color.holo_red_dark));
                            if (mCallback != null) {
                                mCallback.onError(ERROR_EXPIRY_MONTH);
                            }
                            error = true;
                        } else if ((!StripeTextUtils.isBlank(yearStr) && yearStr.length() == 2 && !mCard.validateExpYear())) {
                            mExpiryDateEditText.setTextColor(ContextCompat.getColor(getContext(), android.R.color.holo_red_dark));
                            if (mCallback != null) {
                                mCallback.onError(ERROR_EXPIRY_YEAR);
                            }
                            error = true;
                        } else if (!StripeTextUtils.isBlank(yearStr) && yearStr.length() == 2) { // only move to next field when year entered
                            mCvcEditText.requestFocus();
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

        mCvcEditText.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean focus) {
                if (focus) {
                    setCreditCardIconForCvc();
                } else {
                    setCreditCardIconForNumber();
                }
            }
        });

        mCvcEditText.addTextChangedListener(new TextWatcher() {
            boolean freeze = false;
            int beforeStringLength = 0;

            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
                if (!freeze) {
                    beforeStringLength = mCvcEditText.getText().toString().length();
                }
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                if (!freeze) {
                    String curString = mCvcEditText.getText().toString();
                    int curStringLength = curString.length();

                    // TODO : this is deprecated, but what else should be used?
                    mCard.setCVC(curString);

                    if (beforeStringLength > curStringLength) {
                        // removing space
                        if (beforeStringLength == 1) {
                            mExpiryDateEditText.requestFocus();
                            return;
                        }
                    } else if (curStringLength == 3) {
                        setCreditCardIconForNumber();
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

        mCvcEditText.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (mCvcEditText.getText().toString().length() == 0 && mExpiryDateEditText.getText().toString().length() > 0) {
                        mExpiryDateEditText.setText(mExpiryDateEditText.getText().toString().substring(0, mExpiryDateEditText.getText().toString().length() - 1));
                        mExpiryDateEditText.setSelection(mExpiryDateEditText.getText().toString().length());
                        mExpiryDateEditText.requestFocus();
                    }
                }
                return false;
            }
        });
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        mNumberEditText.setEnabled(enabled);
        mExpiryDateEditText.setEnabled(enabled);
        mCvcEditText.setEnabled(enabled);
    }

    private void scrollRight() {
        if (!mCardNumberInView) {
            return;
        }
        mCardNumberInView = false;
        updateScrollToPosition();
        mScrollView.smoothScrollBy(mScrollToPosition, 0);
        mExpiryDateEditText.setVisibility(VISIBLE);
        mCvcEditText.setVisibility(VISIBLE);
        if (!mExpiryDateEditText.getText().toString().isEmpty()) {
            mExpiryDateEditText.setSelection(mExpiryDateEditText.getText().toString().length());
        }
        mExpiryDateEditText.requestFocus();
    }

    private void scrollLeft() {
        if (mCardNumberInView) {
            return;
        }
        mCardNumberInView = true;
        mNumberEditText.setSelection(mNumberEditText.getText().toString().length());
        mScrollView.smoothScrollBy(-mScrollToPosition, 0);
        mExpiryDateEditText.setVisibility(INVISIBLE);
        mCvcEditText.setVisibility(INVISIBLE);
    }

    private void updateScrollToPosition() {
        String numStr = mNumberEditText.getText().toString();
        mScrollToPosition = mScrollViewWidth;
        if (!numStr.isEmpty()) {
            String scrollStr = numStr.substring(0, 14);
            mScrollToPosition = (int) Layout.getDesiredWidth(scrollStr, mNumberEditText.getPaint());
        }
    }

    private void setCreditCardIconForNumber() {
        if (mCard == null) {
            return;
        }
        if (mCard.getNumber() != null) {
            String type = mCard.getBrand();
            if (!type.equals(Card.UNKNOWN)) {
                mCreditCardIconImageView.setImageResource(getImageResForCardBrand(type));
                if (mCallback != null) {
                    mCallback.onClearError();
                }
            } else {
                mCreditCardIconImageView.setImageResource(R.drawable.stp_card_placeholder);
                if (mCard.getNumber().length() >= 4) {
                    mNumberEditText.setTextColor(ContextCompat.getColor(getContext(), android.R.color.holo_red_dark));
                    if (mCallback != null) {
                        mError = ERROR_NUMBER;
                        mCallback.onError(mError);
                    }
                }
            }
        } else {
            if (mCallback != null) {
                mCallback.onClearError();
            }
            mCreditCardIconImageView.setImageResource(R.drawable.stp_card_placeholder);
        }
    }

    private void setCreditCardIconForCvc() {
        if (mCard == null) {
            return;
        }
        if (mCard.getBrand().equals(Card.AMERICAN_EXPRESS)) {
            mCreditCardIconImageView.setImageResource(R.drawable.stp_card_cvc_amex);
        } else {
            mCreditCardIconImageView.setImageResource(R.drawable.stp_card_cvc);
        }
    }

    private void sendCardValidationCallback() {
        if (isValid() && mCallback != null) {
            mCallback.onValidated(mCard);
        }
    }

    public void setTextColor(int color){
        mColor = color;
        this.mNumberEditText.setTextColor(color);
        this.mExpiryDateEditText.setTextColor(color);
        this.mCvcEditText.setTextColor(color);
    }

    public void setHintTextColor(int color){
        this.mNumberEditText.setHintTextColor(color);
        this.mExpiryDateEditText.setHintTextColor(color);
        this.mCvcEditText.setHintTextColor(color);
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
