package com.stripe.wrap.pay.utils;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.wallet.Cart;
import com.google.android.gms.wallet.LineItem;

import java.util.ArrayList;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * A wrapper for {@link Cart.Builder} that aids in the generation of new {@link LineItem}
 * objects.
 */
public class CartManager {

    static final String REGULAR_ID = "REG";
    static final String SHIPPING_ID = "SHIP";
    static final String TAG = "Stripe:CartManager";

    private final Currency mCurrency;

    private LinkedHashMap<String, LineItem> mLineItemsRegular = new LinkedHashMap<>();
    private LinkedHashMap<String, LineItem> mLineItemsShipping = new LinkedHashMap<>();

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
     * @return a {@link String} UUID that can be used to access the item in this {@link CartManager}
     */
    @Nullable
    public String addLineItem(@NonNull LineItem item) {
        String itemId = null;
        switch (item.getRole()) {
            case LineItem.Role.REGULAR:
                itemId = generateUuidForRole(LineItem.Role.REGULAR);
                mLineItemsRegular.put(itemId, item);
                break;
            case LineItem.Role.SHIPPING:
                itemId = generateUuidForRole(LineItem.Role.SHIPPING);
                mLineItemsShipping.put(itemId, item);
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
                itemId = generateUuidForRole(LineItem.Role.REGULAR);
                mLineItemsRegular.put(itemId, item);
                break;
        }
        return itemId;
    }

    /**
     * Build the {@link Cart}. Returns {@code null} if the item set is invalid.
     *
     * @return a {@link Cart}, or {@code null} if any of the items are invalid
     */
    @Nullable
    public Cart build() throws CartContentException {
        List<LineItem> totalLineItems = new ArrayList<>();
        totalLineItems.addAll(mLineItemsRegular.values());
        totalLineItems.addAll(mLineItemsShipping.values());
        totalLineItems.add(mLineItemTax);

        PaymentUtils.validateLineItemList(totalLineItems, mCurrency.getCurrencyCode());
        return Cart.newBuilder()
                .setCurrencyCode(mCurrency.getCurrencyCode())
                .setLineItems(totalLineItems)
                .setTotalPrice(PaymentUtils.getTotalPriceString(totalLineItems, mCurrency))
                .build();
    }

    @NonNull
    public Map<String, LineItem> getLineItemsRegular() {
        return mLineItemsRegular;
    }

    @NonNull
    public Map<String, LineItem> getLineItemsShipping() {
        return mLineItemsShipping;
    }

    @Nullable
    public LineItem getLineItemTax() {
        return mLineItemTax;
    }

    @NonNull
    static String generateUuidForRole(int role) {
        String baseId = UUID.randomUUID().toString();
        String base = null;
        if (role == LineItem.Role.REGULAR) {
            base = REGULAR_ID;
        } else if (role == LineItem.Role.SHIPPING) {
            base = SHIPPING_ID;
        }

        StringBuilder builder = new StringBuilder();
        if (base != null) {
            return builder.append(base)
                    .append('-')
                    .append(baseId.substring(base.length()))
                    .toString();
        }
        return baseId;
    }
}
