package com.stripe.samplestore;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

public class StoreCart implements Parcelable {

    @NonNull private final Currency mCurrency;
    @NonNull private final LinkedHashMap<String, StoreLineItem> mStoreLineItems;

    StoreCart(@NonNull Currency currency) {
        mCurrency = currency;
        // LinkedHashMap because we want iteration order to be the same.
        mStoreLineItems = new LinkedHashMap<>();
    }

    public StoreCart(@NonNull String currencyCode) {
        this(Currency.getInstance(currencyCode));
    }

    void addStoreLineItem(@NonNull String description, int quantity, long unitPrice) {
        addStoreLineItem(new StoreLineItem(description, quantity, unitPrice));
    }

    private void addStoreLineItem(@NonNull StoreLineItem storeLineItem) {
        mStoreLineItems.put(UUID.randomUUID().toString(), storeLineItem);
    }

    public boolean removeLineItem(@NonNull String itemId) {
        return mStoreLineItems.remove(itemId) != null;
    }

    @NonNull
    List<StoreLineItem> getLineItems() {
        return new ArrayList<>(mStoreLineItems.values());
    }

    int getSize() {
        return mStoreLineItems.size();
    }

    @NonNull
    Currency getCurrency() {
        return mCurrency;
    }

    long getTotalPrice() {
        long total = 0L;
        for (StoreLineItem item : mStoreLineItems.values()) {
            total += item.getTotalPrice();
        }
        return total;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mCurrency.getCurrencyCode());
        dest.writeInt(mStoreLineItems.size());
        for (String key : mStoreLineItems.keySet()) {
            dest.writeString(key);
            dest.writeParcelable(mStoreLineItems.get(key), 0);
        }
    }

    private StoreCart(@NonNull Parcel in) {
        mCurrency = Currency.getInstance(in.readString());
        int count = in.readInt();
        mStoreLineItems = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            mStoreLineItems.put(in.readString(),
                    in.readParcelable(StoreLineItem.class.getClassLoader()));
        }
    }

    public static final Parcelable.Creator<StoreCart> CREATOR =
            new Parcelable.Creator<StoreCart>() {
                @Override
                public StoreCart createFromParcel(Parcel source) {
                    return new StoreCart(source);
                }

                @Override
                public StoreCart[] newArray(int size) {
                    return new StoreCart[size];
                }
            };
}
