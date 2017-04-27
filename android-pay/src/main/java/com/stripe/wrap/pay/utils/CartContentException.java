package com.stripe.wrap.pay.utils;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;

import com.google.android.gms.wallet.LineItem;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Class representing an error that will prevent a {@link com.google.android.gms.wallet.Cart} from
 * being used.
 */
public class CartContentException extends Exception {

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            CART_CURRENCY,
            DUPLICATE_TAX,
            INVALID_PRICE,
            INVALID_CHARACTERS,
            LINE_ITEM_CURRENCY,
            LINE_ITEM_PRICE,
            LINE_ITEM_QUANTITY
    })
    @interface CartErrorType { }
    public static final String CART_CURRENCY = "cart_currency";
    public static final String DUPLICATE_TAX = "duplicate_tax";
    public static final String INVALID_PRICE = "invalid_price";
    public static final String INVALID_CHARACTERS = "invalid_characters";
    public static final String LINE_ITEM_CURRENCY = "line_item_currency";
    public static final String LINE_ITEM_PRICE = "line_item_price";
    public static final String LINE_ITEM_QUANTITY = "line_item_quantity";

    @NonNull private final String mErrorMessage;
    @NonNull private final @CartErrorType String mErrorType;
    @Nullable private final LineItem mLineItem;

    public CartContentException(
            @NonNull @CartErrorType String errorType,
            @NonNull String errorMessage) {
        this(errorType, errorMessage, null);
    }

    public CartContentException(
            @NonNull @CartErrorType String errorType,
            @NonNull String errorMessage,
            @Nullable LineItem errorLineItem) {
        mErrorType = errorType;
        mErrorMessage = errorMessage;
        mLineItem = errorLineItem;
    }

    @Override
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
