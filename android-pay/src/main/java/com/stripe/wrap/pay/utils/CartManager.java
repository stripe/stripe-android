package com.stripe.wrap.pay.utils;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Size;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.wallet.Cart;
import com.google.android.gms.wallet.LineItem;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static com.stripe.wrap.pay.utils.PaymentUtils.getPriceString;
import static com.stripe.wrap.pay.utils.PaymentUtils.getTotalPrice;

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
    @Nullable private Long mManualTotalPrice;

    public CartManager(String currencyCode) {
        mCurrency = PaymentUtils.getCurrencyByCodeOrDefault(currencyCode);
    }

    /**
     * Create a {@link CartManager} from an old {@link Cart} instance. Can be used to
     * alter old {@link Cart} instances that need to update shipping or tax information.
     * By default, {@link LineItem LineItems} in this cart are only copied over if their
     * role is {@link LineItem.Role#REGULAR}.
     *
     * @param oldCart a {@link Cart} from which to copy the regular {@link LineItem LineItems} and
     *                currency code.
     */
    public CartManager(@NonNull Cart oldCart) {
        this(oldCart, false, false);
    }

    /**
     * Create a {@link CartManager} from an old {@link Cart} instance. Can be used to
     * alter old {@link Cart} instances that need to update shipping or tax information.
     * By default, {@link LineItem LineItems} in this cart are only copied over if their
     * role is {@link LineItem.Role#REGULAR}.
     *
     * @param oldCart a {@link Cart} from which to copy the currency code and line items
     * @param shouldKeepShipping {@code true} if items with role {@link LineItem.Role#SHIPPING}
     *                           should be copied, {@code false} if not
     * @param shouldKeepTax {@code true} if items with role {@link LineItem.Role#TAX} should be
     *                      should be copied. Note: constructor does not check to see if the input
     *                      {@link Cart} is valid, so multiple tax items will overwrite each other,
     *                      and only the last one will be kept
     */
    public CartManager(@NonNull Cart oldCart, boolean shouldKeepShipping, boolean shouldKeepTax) {
        mCurrency = PaymentUtils.getCurrencyByCodeOrDefault(oldCart.getCurrencyCode());
        for (LineItem item : oldCart.getLineItems()) {
            switch (item.getRole()) {
                case LineItem.Role.REGULAR:
                    addLineItem(item);
                    break;
                case LineItem.Role.SHIPPING:
                    if (shouldKeepShipping) {
                        addLineItem(item);
                    }
                    break;
                case LineItem.Role.TAX:
                    if (shouldKeepTax) {
                        setTaxLineItem(item);
                    }
                    break;
                default:
                    // Unknown type. Treating as REGULAR. Will trigger log warning in additem.
                    addLineItem(item);
                    break;
            }
        }
    }

    /**
     * Adds a {@link LineItem.Role#REGULAR} item to the cart with a description
     * and total price value. Currency matches the currency of the {@link CartManager}.
     *
     * @param description a line item description
     * @param totalPrice the total price of the line item, in the smallest denomination
     * @return a {@link String} UUID that can be used to access the item in this {@link CartManager}
     *
     */
    @Nullable
    public String addLineItem(@NonNull @Size(min = 1) String description, long totalPrice) {
        return addLineItem(description, totalPrice, LineItem.Role.REGULAR);
    }

    /**
     * Adds a {@link LineItem.Role#REGULAR} item to the cart with a description
     * and total price value. Currency matches the currency of the {@link CartManager}.
     *
     * @param description a line item description
     * @param totalPrice the total price of the line item, in the smallest denomination
     * @param role the {@link LineItem.Role} for this item
     * @return a {@link String} UUID that can be used to access the item in this {@link CartManager}
     * or {@code null} if this is a {@link LineItem.Role#TAX} item
     */
    @Nullable
    public String addLineItem(
            @NonNull @Size(min = 1) String description,
            long totalPrice,
            int role) {
        return addLineItem(new LineItemBuilder(mCurrency.getCurrencyCode())
                .setDescription(description)
                .setTotalPrice(totalPrice)
                .setRole(role)
                .build());
    }

    /**
     * Adds a line item with unit price and quantity. Total price is calculated and added to the
     * line item.
     *
     * @param description a line item description
     * @param quantity the quantity of the line item
     * @param unitPrice the unit price of the line item
     * @param role the {@link LineItem.Role} of the added item
     * @return a {@link String} UUID that can be used to access the item in this {@link CartManager}
     */
    @Nullable
    public String addLineItem(@NonNull @Size(min = 1) String description,
                              double quantity,
                              long unitPrice,
                              int role) {
        BigDecimal roundedQuantity = new BigDecimal(quantity).setScale(1, BigDecimal.ROUND_DOWN);
        long totalPrice = roundedQuantity.multiply(new BigDecimal(unitPrice)).longValue();

        return addLineItem(new LineItemBuilder(mCurrency.getCurrencyCode())
                .setDescription(description)
                .setTotalPrice(totalPrice)
                .setUnitPrice(unitPrice)
                .setQuantity(roundedQuantity)
                .setRole(role)
                .build());
    }

    /**
     * Adds a line item with unit price and quantity. Total price is calculated and added to the
     * line item.
     *
     * @param description a line item description
     * @param quantity the quantity of the line item
     * @param unitPrice the unit price of the line item
     * @return a {@link String} UUID that can be used to access the item in this {@link CartManager}
     */
    @Nullable
    public String addLineItem(@NonNull @Size(min = 1) String description,
                              double quantity,
                              long unitPrice) {
        BigDecimal roundedQuantity = new BigDecimal(quantity).setScale(1, BigDecimal.ROUND_DOWN);
        long totalPrice = roundedQuantity.multiply(new BigDecimal(unitPrice)).longValue();

        return addLineItem(new LineItemBuilder(mCurrency.getCurrencyCode())
                .setDescription(description)
                .setTotalPrice(totalPrice)
                .setUnitPrice(unitPrice)
                .setQuantity(roundedQuantity)
                .setRole(LineItem.Role.REGULAR)
                .build());
    }

    /**
     * Adds a {@link LineItem.Role#SHIPPING} item to the cart with a description
     * and total price value. Currency matches the currency of the {@link CartManager}.
     *
     * @param description a line item description
     * @param totalPrice the total price of the line item, in the smallest denomination
     * @return a {@link String} UUID that can be used to access the item in this {@link CartManager}
     */
    @Nullable
    public String addShippingLineItem(@NonNull @Size(min = 1) String description, long totalPrice) {
        return addLineItem(new LineItemBuilder(mCurrency.getCurrencyCode())
                .setDescription(description)
                .setTotalPrice(totalPrice)
                .setRole(LineItem.Role.SHIPPING)
                .build());
    }

    /**
     * Calculate the total price of all {@link LineItem.Role#REGULAR}
     * line items, if possible.
     *
     * @return the total price of regular items, or {@code null} if that cannot
     * be calculated because of mixed currencies.
     */
    @Nullable
    public Long calculateRegularItemTotal() {
        return PaymentUtils.getTotalPrice(mLineItemsRegular.values(), mCurrency);
    }

    /**
     * Calculate the total price of all {@link LineItem.Role#SHIPPING}
     * line items, if possible.
     *
     * @return the total price of shipping items, or {@code null} if that cannot
     * be calculated because of mixed currencies.
     */
    @Nullable
    public Long calculateShippingItemTotal() {
        return PaymentUtils.getTotalPrice(mLineItemsShipping.values(), mCurrency);
    }

    public Long calculateTax() {
        if (mLineItemTax == null) {
            return null;
        }
        return PaymentUtils.getPriceLong(mLineItemTax.getTotalPrice(), mCurrency);
    }

    /**
     * Adds a {@link LineItem.Role#TAX} item to the cart with a description
     * and total price value. Currency matches the currency of the {@link CartManager}.
     *
     * @param description a line item description
     * @param totalPrice the total price of the line item, in the smallest denomination
     */
    public void setTaxLineItem(@NonNull @Size(min = 1) String description, long totalPrice) {
        LineItem taxLineItem = new LineItemBuilder(mCurrency.getCurrencyCode())
                .setDescription(description)
                .setTotalPrice(totalPrice)
                .setRole(LineItem.Role.TAX)
                .build();
        addLineItem(taxLineItem);
    }

    /**
     * Setter for the total price. Can be used if you want the price of the cart to differ
     * from the sum of the prices of the items within the cart.
     *
     * @param totalPrice a number representing the price, in the lowest possible denomination
     *                   of the cart's currency
     */
    public void setTotalPrice(@NonNull Long totalPrice) {
        mManualTotalPrice = totalPrice;
    }

    /**
     * Sets the tax line item in this cart manager. Can be used to clear the tax item by using
     * {@code null} input. If the input {@link LineItem} has a role other than
     * {@link LineItem.Role#TAX}, the input is ignored.
     *
     * @param item a {@link LineItem} with role {@link LineItem.Role#TAX}, or {@code null}
     */
    public void setTaxLineItem(@Nullable LineItem item) {
        if (item == null) {
            mLineItemTax = item;
        } else {
            addLineItem(item);
        }
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
                // We're swapping out the tax item, so we have to remove the old one.
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

        Long totalPrice = mManualTotalPrice == null
                ? getTotalPrice(totalLineItems, mCurrency)
                : mManualTotalPrice;

        String totalPriceString = totalPrice == null ? null : getPriceString(totalPrice, mCurrency);

        if (!TextUtils.isEmpty(totalPriceString)) {
            // If a manual value has been set for the total price string, then we don't need
            // to calculate this on our own, and mixed currency line items are not an error state.
            errors = PaymentUtils.removeErrorType(errors, CartError.LINE_ITEM_CURRENCY);
        }

        if (errors.isEmpty()) {
            return Cart.newBuilder()
                    .setCurrencyCode(mCurrency.getCurrencyCode())
                    .setLineItems(totalLineItems)
                    .setTotalPrice(totalPriceString)
                    .build();
        } else {
            throw new CartContentException(errors);
        }
    }

    @NonNull
    public String getCurrencyCode() {
        return mCurrency.getCurrencyCode();
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
