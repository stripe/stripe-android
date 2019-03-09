package com.stripe.samplestore;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

/**
 * Represents a single line item for purchase in this store.
 */
public class StoreLineItem implements Parcelable {

    @NonNull private String mDescription;
    private int mQuantity;
    private long mUnitPrice;

    public StoreLineItem(@NonNull String description, int quantity, long unitPrice) {
        mDescription = description;
        mQuantity = quantity;
        mUnitPrice = unitPrice;
    }

    @NonNull
    public String getDescription() {
        return mDescription;
    }

    public int getQuantity() {
        return mQuantity;
    }

    public long getUnitPrice() {
        return mUnitPrice;
    }

    public long getTotalPrice() {
        return mUnitPrice * mQuantity;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mDescription);
        dest.writeInt(this.mQuantity);
        dest.writeLong(this.mUnitPrice);
    }

    protected StoreLineItem(Parcel in) {
        this.mDescription = in.readString();
        this.mQuantity = in.readInt();
        this.mUnitPrice = in.readLong();
    }

    public static final Parcelable.Creator<StoreLineItem> CREATOR = new Parcelable.Creator<StoreLineItem>() {
        @Override
        public StoreLineItem createFromParcel(Parcel source) {
            return new StoreLineItem(source);
        }

        @Override
        public StoreLineItem[] newArray(int size) {
            return new StoreLineItem[size];
        }
    };
}
