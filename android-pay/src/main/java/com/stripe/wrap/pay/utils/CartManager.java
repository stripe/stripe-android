package com.stripe.wrap.pay.utils;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.wallet.Cart;
import com.google.android.gms.wallet.LineItem;

import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

/**
 * A wrapper for {@link Cart.Builder} that aids in the generation of new {@link LineItem}
 * objects.
 */
public class CartManager {

    static final String TAG = "Stripe:CartManager";

    private final Currency mCurrency;
    private List<LineItem> mLineItemsRegular = new ArrayList<>();
    private List<LineItem> mLineItemsShipping = new ArrayList<>();
    private LineItem mLineItemTax;

    public CartManager() {
        mCurrency = Currency.getInstance(Locale.getDefault());
    }

    public CartManager(String currencyCode) {
        mCurrency = PaymentUtils.getCurrencyByCodeOrDefault(currencyCode);
    }

    /**
     * Add a {@link LineItem} to the cart.
     *
     * @param item the {@link LineItem} to be added
     */
    public CartManager addLineItem(@NonNull LineItem item) {
        switch (item.getRole()) {
            case LineItem.Role.REGULAR:
                mLineItemsRegular.add(item);
                break;
            case LineItem.Role.SHIPPING:
                mLineItemsShipping.add(item);
                break;
            case LineItem.Role.TAX:
                if (mLineItemTax != null) {
                    Log.w(TAG, String.format(Locale.ENGLISH,
                            "Adding a tax line item, but a tax line item " +
                            "already exists. Old tax of %s is being overwritten " +
                            "to maintain a valid cart.",
                            mLineItemTax.getTotalPrice()));
                }
                mLineItemTax = item;
                break;
            default:
                Log.w(TAG, String.format(Locale.ENGLISH,
                        "Line item with unknown role added to cart. Treated as regular. " +
                        "Unknown role is of code %d",
                        item.getRole()));
                mLineItemsRegular.add(item);
                break;
        }
        return this;
    }

    /**
     * Build the {@link Cart}. Returns {@code null} if the item set is invalid.
     *
     * @return a {@link Cart}, or {@code null} if any of the items are invalid
     */
    @Nullable
    public Cart build() {
        List<LineItem> totalLineItems = new ArrayList<>();
        totalLineItems.addAll(mLineItemsRegular);
        totalLineItems.addAll(mLineItemsShipping);
        totalLineItems.add(mLineItemTax);

        if (PaymentUtils.isLineItemListValid(
                totalLineItems,
                mCurrency.getCurrencyCode())) {
            return Cart.newBuilder()
                    .setCurrencyCode(mCurrency.getCurrencyCode())
                    .setLineItems(totalLineItems)
                    .setTotalPrice(PaymentUtils.getTotalPriceString(totalLineItems, mCurrency))
                    .build();
        } else {
            return null;
        }
    }

    @NonNull
    public List<LineItem> getLineItemsRegular() {
        return mLineItemsRegular;
    }

    @NonNull
    public List<LineItem> getLineItemsShipping() {
        return mLineItemsShipping;
    }

    @Nullable
    public LineItem getLineItemTax() {
        return mLineItemTax;
    }
}
