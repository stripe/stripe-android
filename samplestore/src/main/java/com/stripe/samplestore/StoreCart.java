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

    @NonNull private Currency mCurrency;
    @NonNull private LinkedHashMap<String, StoreLineItem> mStoreLineItems;

    public StoreCart(@NonNull Currency currency) {
        mCurrency = currency;
        // LinkedHashMap because we want iteration order to be the same.
        mStoreLineItems = new LinkedHashMap<>();
    }

    public StoreCart(@NonNull String currencyCode) {
        this(Currency.getInstance(currencyCode));
    }

    public String addStoreLineItem(@NonNull String description, int quantity, long unitPrice) {
        return addStoreLineItem(new StoreLineItem(description, quantity, unitPrice));
    }

    @NonNull
    public String addStoreLineItem(@NonNull StoreLineItem storeLineItem) {
        String uuid = UUID.randomUUID().toString();
        mStoreLineItems.put(uuid, storeLineItem);
        return uuid;
    }

    public boolean removeLineItem(@NonNull String itemId) {
        return mStoreLineItems.remove(itemId) != null;
    }

    @NonNull
    public List<StoreLineItem> getLineItems() {
        return new ArrayList<>(mStoreLineItems.values());
    }

    public int getSize() {
        return mStoreLineItems.size();
    }

    @NonNull
    public Currency getCurrency() {
        return mCurrency;
    }

    public long getTotalPrice() {
        long total = 0L;
        for(StoreLineItem item : mStoreLineItems.values()) {
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
        dest.writeSerializable(mCurrency);
        dest.writeInt(mStoreLineItems.size());
        for (String key : mStoreLineItems.keySet()) {
            dest.writeString(key);
            dest.writeParcelable(mStoreLineItems.get(key), 0);
        }
    }

    protected StoreCart(Parcel in) {
        mCurrency = (Currency) in.readSerializable();
        int count = in.readInt();
        mStoreLineItems = new LinkedHashMap<>();
        for(int i = 0; i < count; i++) {
            mStoreLineItems.put(in.readString(),
                    (StoreLineItem) in.readParcelable(StoreLineItem.class.getClassLoader()));
        }
    }

    public static final Parcelable.Creator<StoreCart> CREATOR = new Parcelable.Creator<StoreCart>() {
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
