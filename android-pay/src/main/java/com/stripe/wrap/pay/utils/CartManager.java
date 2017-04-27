package com.stripe.wrap.pay.utils;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Size;
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

    @NonNull private LinkedHashMap<String, LineItem> mLineItemsRegular = new LinkedHashMap<>();
    @NonNull private LinkedHashMap<String, LineItem> mLineItemsShipping = new LinkedHashMap<>();

    @Nullable private LineItem mLineItemTax;

    public CartManager() {
        mCurrency = Currency.getInstance(Locale.getDefault());
    }

    public CartManager(String currencyCode) {
        mCurrency = PaymentUtils.getCurrencyByCodeOrDefault(currencyCode);
    }

    /**
     * Adds a {@link LineItem.Role#REGULAR} item to the cart with a description
     * and total price value. Currency matches the currency of the {@link CartManager}.
     *
     * @param description a line item description
     * @param totalPrice the total price of the line item, in the smallest denomination
     * @return a {@link String} UUID that can be used to access the item in this {@link CartManager}
     */
    @NonNull
    public String addLineItem(@NonNull @Size(min = 1) String description, long totalPrice) {
        String itemId = generateUuidForRole(LineItem.Role.REGULAR);
        mLineItemsRegular.put(itemId,
                new LineItemBuilder(mCurrency.getCurrencyCode())
                        .setDescription(description)
                        .setTotalPrice(totalPrice)
                        .build());
        return itemId;
    }

    /**
     * Adds a {@link LineItem.Role#SHIPPING} item to the cart with a description
     * and total price value. Currency matches the currency of the {@link CartManager}.
     *
     * @param description a line item description
     * @param totalPrice the total price of the line item, in the smallest denomination
     * @return a {@link String} UUID that can be used to access the item in this {@link CartManager}
     */
    @NonNull
    public String addShippingLineItem(@NonNull @Size(min = 1) String description, long totalPrice) {
        String itemId = generateUuidForRole(LineItem.Role.REGULAR);
        mLineItemsRegular.put(itemId,
                new LineItemBuilder(mCurrency.getCurrencyCode())
                        .setDescription(description)
                        .setTotalPrice(totalPrice)
                        .setRole(LineItem.Role.SHIPPING)
                        .build());
        return itemId;
    }

    /**
     * Adds a {@link LineItem.Role#TAX} item to the cart with a description
     * and total price value. Currency matches the currency of the {@link CartManager}.
     *
     * @param description a line item description
     * @param totalPrice the total price of the line item, in the smallest denomination
     */
    @NonNull
    public void setTaxLineItem(@NonNull @Size(min = 1) String description, long totalPrice) {
        LineItem taxLineItem = new LineItemBuilder(mCurrency.getCurrencyCode())
                .setDescription(description)
                .setTotalPrice(totalPrice)
                .setRole(LineItem.Role.TAX)
                .build();
        addLineItem(taxLineItem);
    }

    /**
     * Remove an item from the {@link CartManager}.
     *
     * @param itemId the UUID associated with the cart item to be removed
     * @return the {@link LineItem} removed, or {@code null} if no item was found
     */
    @Nullable
    public LineItem removeItem(@NonNull @Size(min = 1) String itemId) {
        LineItem removed = mLineItemsRegular.remove(itemId);
        if (removed == null) {
            removed = mLineItemsShipping.remove(itemId);
        }
        return removed;
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
     * Build the {@link Cart}. Returns {@code null} if the item set is empty.
     *
     * @return a {@link Cart}, or {@code null} if there are no line items
     * @throws CartContentException if there are invalid line items or invalid cart parameters. The
     * exception will contain a list of CartError objects specifying the problems.
     */
    @Nullable
    public Cart buildCart() throws CartContentException {
        List<LineItem> totalLineItems = new ArrayList<>();
        totalLineItems.addAll(mLineItemsRegular.values());
        totalLineItems.addAll(mLineItemsShipping.values());
        if (mLineItemTax != null) {
            totalLineItems.add(mLineItemTax);
        }

        if (totalLineItems.isEmpty()) {
            return null;
        }

        List<CartError> errors = PaymentUtils.validateLineItemList(
                totalLineItems,
                mCurrency.getCurrencyCode());
        if (errors.isEmpty()) {
            return Cart.newBuilder()
                    .setCurrencyCode(mCurrency.getCurrencyCode())
                    .setLineItems(totalLineItems)
                    .setTotalPrice(PaymentUtils.getTotalPriceString(totalLineItems, mCurrency))
                    .build();
        } else {
            throw new CartContentException(errors);
        }
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
