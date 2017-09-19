package com.stripe.android;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.model.ShippingInformation;
import com.stripe.android.model.ShippingMethod;

/**
 * A data class representing the state of the associated {@link PaymentSession}.
 */
public class PaymentSessionData implements Parcelable {

    private static final String NO_PAYMENT = "NO_PAYMENT";

    private long mCartTotal = 0L;
    @NonNull private String mSelectedPaymentMethodId = NO_PAYMENT;
    private long mShippingTotal = 0L;
    private ShippingInformation mShippingInformation;
    private ShippingMethod mShippingMethod;

    public PaymentSessionData() { }

    /**
     * Get the selected payment method ID for the associated {@link PaymentSession}.
     * @return
     */
    @Nullable
    public String getSelectedPaymentMethodId() {
        return mSelectedPaymentMethodId.equals(NO_PAYMENT) ? null : mSelectedPaymentMethodId;
    }

    /**
     * Get the cart total value, excluding shipping and tax items.
     *
     * @return the current value of the items in the cart
     */
    public long getCartTotal() {
        return mCartTotal;
    }

    /**
     * Get the value of shipping items in the associated {@link PaymentSession}
     *
     * @return the current value of the shipping items in the cart
     */
    public long getShippingTotal() {
        return mShippingTotal;
    }

    public ShippingInformation getShippingInformation() {
        return mShippingInformation;
    }

    public void setShippingInformation(ShippingInformation shippingInformation) {
        mShippingInformation = shippingInformation;
    }

    public ShippingMethod getShippingMethod() {
        return mShippingMethod;
    }

    public void setShippingMethod(ShippingMethod shippingMethod) {
        mShippingMethod = shippingMethod;
    }

    void setCartTotal(long cartTotal) {
        mCartTotal = cartTotal;
    }

    void setSelectedPaymentMethodId(@Nullable String selectedPaymentMethodId) {
        mSelectedPaymentMethodId = selectedPaymentMethodId == null
                ? NO_PAYMENT
                : selectedPaymentMethodId;
    }

    void setShippingTotal(long shippingTotal) {
        mShippingTotal = shippingTotal;
    }

    /************** Parcelable *********************/
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(mCartTotal);
        parcel.writeString(mSelectedPaymentMethodId);
        parcel.writeLong(mShippingTotal);
        parcel.writeParcelable(mShippingInformation, i);
        parcel.writeParcelable(mShippingMethod, i);
    }

    public static final Parcelable.Creator<PaymentSessionData> CREATOR
            = new Parcelable.Creator<PaymentSessionData>() {
        public PaymentSessionData createFromParcel(Parcel in) {
            return new PaymentSessionData(in);
        }

        public PaymentSessionData[] newArray(int size) {
            return new PaymentSessionData[size];
        }
    };

    private PaymentSessionData(Parcel in) {
        mCartTotal = in.readLong();
        mSelectedPaymentMethodId = in.readString();
        mShippingTotal = in.readLong();
        mShippingInformation = in.readParcelable(ShippingInformation.class.getClassLoader());
        mShippingMethod = in.readParcelable(ShippingMethod.class.getClassLoader());
    }
}
