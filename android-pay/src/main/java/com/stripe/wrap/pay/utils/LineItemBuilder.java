package com.stripe.wrap.pay.utils;

import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.wallet.LineItem;
import com.stripe.wrap.pay.AndroidPayConfiguration;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Currency;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * A wrapper for {@link LineItem.Builder} that allows you to use price as a {@link Long} and
 * to easily set default currency values.
 */
public class LineItemBuilder {

    public static final Set<Integer> VALID_ROLES = new HashSet<Integer>(){{
        add(LineItem.Role.TAX);
        add(LineItem.Role.REGULAR);
        add(LineItem.Role.SHIPPING);
    }};

    static final String TAG = "Stripe:LineItemBuilder";

    private static final double CONSISTENCY_THRESHOLD = 0.01;
    private static final int CONSISTENCY_SCALE = 3;

    private Currency mCurrency;
    private Long mUnitPrice;
    private Long mTotalPrice;
    private BigDecimal mQuantity;
    private String mDescription;
    private int mRole;

    /**
     * Construct a {@link LineItem} using {@link LineItem.Role#REGULAR} and the currency code
     * from {@link AndroidPayConfiguration#getCurrencyCode()}.
     */
    LineItemBuilder() {
        this(AndroidPayConfiguration.getInstance().getCurrencyCode());
    }

    /**
     * Construct a {@link LineItem} using a customized currency code. Role is initially set to
     * {@link LineItem.Role#REGULAR}.
     *
     * @param currencyCode
     */
    LineItemBuilder(String currencyCode) {
        setCurrencyCode(currencyCode);
        mRole = LineItem.Role.REGULAR;
    }

    /**
     * Sets the ISO 4217 currency code of the line item. If the input currency is invalid,
     * currency is set to the default for the phone's locale.
     *
     * @param currencyCode the currency code to set
     * @return {@code this}, for chaining purposes
     */
    public LineItemBuilder setCurrencyCode(String currencyCode) {
        mCurrency = Currency.getInstance(currencyCode.toUpperCase());
        return this;
    }

    public LineItemBuilder setUnitPrice(long unitPrice) {
        mUnitPrice = unitPrice;
        return this;
    }

    public LineItemBuilder setTotalPrice(long totalPrice) {
        mTotalPrice = totalPrice;
        return this;
    }

    /**
     * Sets the quantity for this line item. Note: the quantity may have at most one number
     * after the decimal place. Further precision will be rounded away.
     *
     * @param quantity the quantity of this line item
     * @return {@code this}, for chaining purposes
     */
    public LineItemBuilder setQuantity(BigDecimal quantity) {
        if (quantity.scale() > 1) {
            mQuantity = quantity.setScale(1, BigDecimal.ROUND_HALF_EVEN);
            Log.w(TAG, String.format(
                    Locale.ENGLISH,
                    "Tried to create quantity %.2f, but Android Pay quantity" +
                            " may only have one digit after decimal. Value was rounded to %s",
                    quantity,
                    mQuantity.toString()));
        } else {
            mQuantity = quantity;
        }
        return this;
    }

    /**
     * Sets the quantity for this line item. Note: the quantity may have at most one number
     * after the decimal place. Further precision will be rounded away.
     *
     * @param quantity the quantity of this line item
     * @return {@code this}, for chaining purposes
     */
    public LineItemBuilder setQuantity(double quantity) {
        BigDecimal fullQuantity = BigDecimal.valueOf(quantity);
        mQuantity = BigDecimal.valueOf(quantity).setScale(1, BigDecimal.ROUND_HALF_EVEN);
        if (fullQuantity.scale() > 1) {
            Log.w(TAG, String.format(
                    Locale.ENGLISH,
                    "Tried to create quantity %.2f, but Android Pay quantity" +
                            " may only have one digit after decimal. Value was rounded to %s",
                    quantity,
                    mQuantity.toString()));
        }
        return this;
    }

    public LineItemBuilder setDescription(String description) {
        mDescription = description;
        return this;
    }

    /**
     * Sets the {@link LineItem.Role} of this line item, if the input is a member of
     * {@link #VALID_ROLES}.
     *
     * @param role the {@link LineItem.Role} of this item
     * @return {@code this}, for chaining purposes
     */
    public LineItemBuilder setRole(int role) {
        if (VALID_ROLES.contains(role)) {
            mRole = role;
        }
        return this;
    }

    public LineItem build() {
        LineItem.Builder androidPayBuilder = LineItem.newBuilder();
        androidPayBuilder.setCurrencyCode(mCurrency.getCurrencyCode()).setRole(mRole);

        if (mTotalPrice != null) {
            androidPayBuilder.setTotalPrice(PaymentUtils.getPriceString(mTotalPrice, mCurrency));
        }

        if (mUnitPrice != null) {
            androidPayBuilder.setUnitPrice(PaymentUtils.getPriceString(mUnitPrice, mCurrency));
        }

        if (mQuantity != null) {
            if (isWholeNumber(mQuantity)) {
                androidPayBuilder.setQuantity(mQuantity.toBigInteger().toString());
            } else {
                androidPayBuilder.setQuantity(mQuantity.toString());
            }
        }

        if (mTotalPrice == null && mQuantity != null && mUnitPrice != null) {
            mTotalPrice = mQuantity.multiply(
                    BigDecimal.valueOf(mUnitPrice),
                    MathContext.DECIMAL64).longValue();
            androidPayBuilder.setTotalPrice(PaymentUtils.getPriceString(mTotalPrice, mCurrency));
        } else {
            if (!isPriceBreakdownConsistent(mUnitPrice, mQuantity, mTotalPrice)) {
                Log.w(TAG, String.format(Locale.ENGLISH,
                        "Price breakdown of %d * %.1f = %d is off by more than 1 percent",
                        mUnitPrice, mQuantity.floatValue(), mTotalPrice));
            }
        }

        if (mRole != LineItem.Role.REGULAR) {
            androidPayBuilder.setRole(mRole);
        }

        if (!TextUtils.isEmpty(mDescription)) {
            androidPayBuilder.setDescription(mDescription);
        }

        return androidPayBuilder.build();
    }

    static boolean isWholeNumber(BigDecimal number) {
        return number.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Checks to see if the unit * quantity expected price is within 1% of the
     * listed totalPrice. Returns {@code true} if any value is null.
     *
     * @param unitPrice the listed price per unit of the line item
     * @param quantity the listed quantity of the line item
     * @param totalPrice the listed total price of the line item
     * @return {@code true} if the quantity or unit price is zero, or if any item is null.
     * Otherwise, {@code true} if and only if totalPrice / (unitPrice * quantity) is between
     * 0.99 and 1.01.
     */
    static boolean isPriceBreakdownConsistent(
            Long unitPrice,
            BigDecimal quantity,
            Long totalPrice) {
        if (unitPrice == null || quantity == null || totalPrice == null) {
            return true;
        }

        BigDecimal expectedPrice = quantity.multiply(BigDecimal.valueOf(unitPrice));
        BigDecimal actualPrice = BigDecimal.valueOf(totalPrice);

        if (expectedPrice.compareTo(BigDecimal.ZERO) == 0) {
            return totalPrice == 0;
        }

        double ratio = actualPrice.divide(
                expectedPrice,
                CONSISTENCY_SCALE,
                BigDecimal.ROUND_HALF_EVEN).doubleValue();

        return Math.abs(ratio - 1.0) < CONSISTENCY_THRESHOLD;
    }
}
