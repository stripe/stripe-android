package com.stripe.wrap.pay.utils;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;

import com.google.android.gms.wallet.LineItem;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Class representing a single cart error, either with a line item or
 * the cart as a whole.
 */
public class CartError {

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            CART_CURRENCY,
            DUPLICATE_TAX,
            LINE_ITEM_CURRENCY,
            LINE_ITEM_PRICE,
            LINE_ITEM_QUANTITY
    })
    @interface CartErrorType { }
    public static final String CART_CURRENCY = "cart_currency";
    public static final String DUPLICATE_TAX = "duplicate_tax";
    public static final String LINE_ITEM_CURRENCY = "line_item_currency";
    public static final String LINE_ITEM_PRICE = "line_item_price";
    public static final String LINE_ITEM_QUANTITY = "line_item_quantity";

    @NonNull private final String mErrorMessage;
    @NonNull private final @CartErrorType String mErrorType;
    @Nullable private final LineItem mLineItem;

    public CartError(@CartErrorType String errorType,
            @NonNull String errorMessage) {
        this(errorType, errorMessage, null);
    }

    public CartError(
            @NonNull @CartErrorType String errorType,
            @NonNull String errorMessage,
            @Nullable LineItem errorLineItem) {
        mErrorType = errorType;
        mErrorMessage = errorMessage;
        mLineItem = errorLineItem;
    }

    public String getMessage() {
        return mErrorMessage;
    }

    @NonNull
    @CartErrorType
    public String getErrorType() {
        return mErrorType;
    }

    @Nullable
    public LineItem getLineItem() {
        return mLineItem;
    }
}
