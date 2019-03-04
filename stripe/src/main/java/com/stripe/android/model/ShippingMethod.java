package com.stripe.android.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;

import com.stripe.android.utils.ObjectUtils;

import org.json.JSONObject;

import java.util.AbstractMap;
import java.util.Currency;
import java.util.HashMap;
import java.util.Map;

import static com.stripe.android.model.StripeJsonUtils.putLongIfNotNull;
import static com.stripe.android.model.StripeJsonUtils.putStringIfNotNull;

/**
 * Model representing a shipping method in the Android SDK.
 */
public class ShippingMethod extends StripeJsonModel implements Parcelable {

    public static final Parcelable.Creator<ShippingMethod> CREATOR
            = new Parcelable.Creator<ShippingMethod>() {
        public ShippingMethod createFromParcel(Parcel in) {
            return new ShippingMethod(in);
        }

        public ShippingMethod[] newArray(int size) {
            return new ShippingMethod[size];
        }
    };

    private static final String FIELD_AMOUNT = "amount";
    /*ISO Currency Code*/
    private static final String FIELD_CURRENCY_CODE = "currency_code";
    private static final String FIELD_DETAIL = "detail";
    private static final String FIELD_IDENTIFIER = "identifier";
    private static final String FIELD_LABEL = "label";

    private final long mAmount;
    @NonNull @Size(min = 0, max = 3) private final String mCurrencyCode;
    @Nullable private final String mDetail;
    @NonNull private final String mIdentifier;
    @NonNull private final String mLabel;

    public ShippingMethod(@NonNull String label,
                          @NonNull String identifier,
                          long amount,
                          @NonNull @Size(min = 0, max = 3) String
                                  currencyCode) {
        this(label, identifier, null, amount, currencyCode);
    }

    public ShippingMethod(@NonNull String label,
                          @NonNull String identifier,
                          @Nullable String detail,
                          long amount,
                          @NonNull @Size(min = 0, max = 3) String currencyCode) {
        mLabel = label;
        mIdentifier = identifier;
        mDetail = detail;
        mAmount = amount;
        mCurrencyCode = currencyCode;
    }

    private ShippingMethod(@NonNull Parcel in) {
        mAmount = in.readLong();
        mCurrencyCode = in.readString();
        mDetail = in.readString();
        mIdentifier = in.readString();
        mLabel = in.readString();
    }

    /**
     * @return the currency that the specified amount will be rendered in.
     */
    @NonNull
    public Currency getCurrency() {
        return Currency.getInstance(mCurrencyCode);
    }

    /**
     * @return The cost in minor unit of the currency provided in the
     * {@link com.stripe.android.PaymentConfiguration}. For example, cents in the USA and yen in
     * Japan.
     */
    public long getAmount() {
        return mAmount;
    }

    /**
     * @return Human friendly label specifying the shipping method that can be shown in the UI.
     */
    @NonNull
    public String getLabel() {
        return mLabel;
    }

    /**
     * @return Human friendly information such as estimated shipping times that can be shown in
     * the UI
     */
    @Nullable
    public String getDetail() {
        return mDetail;
    }

    /**
     * @return Identifier for the shipping method.
     */
    @NonNull
    public String getIdentifier() {
        return mIdentifier;
    }

    @NonNull
    @Override
    public JSONObject toJson() {
        final JSONObject jsonObject = new JSONObject();
        putLongIfNotNull(jsonObject, FIELD_AMOUNT, mAmount);
        putStringIfNotNull(jsonObject, FIELD_CURRENCY_CODE, mCurrencyCode);
        putStringIfNotNull(jsonObject, FIELD_DETAIL, mDetail);
        putStringIfNotNull(jsonObject, FIELD_IDENTIFIER, mIdentifier);
        putStringIfNotNull(jsonObject, FIELD_LABEL, mLabel);
        return jsonObject;
    }

    @NonNull
    @Override
    public Map<String, Object> toMap() {
        final AbstractMap<String, Object> map = new HashMap<>();
        map.put(FIELD_AMOUNT, mAmount);
        map.put(FIELD_CURRENCY_CODE, mCurrencyCode);
        map.put(FIELD_DETAIL, mDetail);
        map.put(FIELD_IDENTIFIER, mIdentifier);
        map.put(FIELD_LABEL, mLabel);
        return map;
    }

    /************** Parcelable *********************/
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(mAmount);
        parcel.writeString(mCurrencyCode);
        parcel.writeString(mDetail);
        parcel.writeString(mIdentifier);
        parcel.writeString(mLabel);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return this == obj || (obj instanceof ShippingMethod && typedEquals((ShippingMethod) obj));
    }

    private boolean typedEquals(@NonNull ShippingMethod shippingMethod) {
        return mAmount == shippingMethod.mAmount
                && ObjectUtils.equals(mCurrencyCode, shippingMethod.mCurrencyCode)
                && ObjectUtils.equals(mDetail, shippingMethod.mDetail)
                && ObjectUtils.equals(mIdentifier, shippingMethod.mIdentifier)
                && ObjectUtils.equals(mLabel, shippingMethod.mLabel);
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hash(mAmount, mCurrencyCode, mDetail, mIdentifier, mLabel);
    }
}
