package com.stripe.samplestore;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

/**
 * Represents a single line item for purchase in this store.
 */
public class StoreLineItem implements Parcelable {

    @NonNull private final String mDescription;
    private final int mQuantity;
    private final long mUnitPrice;

    StoreLineItem(@NonNull String description, int quantity, long unitPrice) {
        mDescription = description;
        mQuantity = quantity;
        mUnitPrice = unitPrice;
    }

    @NonNull
    String getDescription() {
        return mDescription;
    }

    int getQuantity() {
        return mQuantity;
    }

    long getUnitPrice() {
        return mUnitPrice;
    }

    long getTotalPrice() {
        return mUnitPrice * mQuantity;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(this.mDescription);
        dest.writeInt(this.mQuantity);
        dest.writeLong(this.mUnitPrice);
    }

    protected StoreLineItem(@NonNull Parcel in) {
        this.mDescription = in.readString();
        this.mQuantity = in.readInt();
        this.mUnitPrice = in.readLong();
    }

    public static final Parcelable.Creator<StoreLineItem> CREATOR =
            new Parcelable.Creator<StoreLineItem>() {
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
