package com.stripe.wrap.pay.utils;

import android.support.annotation.NonNull;
import android.support.annotation.Size;

import java.util.List;

/**
 * Class representing an error that will prevent a {@link com.google.android.gms.wallet.Cart} from
 * being used.
 */
public class CartContentException extends Exception {

    static final String CART_ERROR_MESSAGE_START = "Cart Content Error Found:\n";

    @NonNull private final List<CartError> mCartErrors;

    CartContentException(@NonNull @Size(min = 1) List<CartError> cartErrors) {
        mCartErrors = cartErrors;
    }

    @Override
    public String getMessage() {
        StringBuilder builder = new StringBuilder();
        builder.append(CART_ERROR_MESSAGE_START);
        for (CartError error : mCartErrors) {
            builder.append(error.getMessage()).append('\n');
        }
        return builder.toString();
    }

    public List<CartError> getCartErrors() {
        return mCartErrors;
    }
}
