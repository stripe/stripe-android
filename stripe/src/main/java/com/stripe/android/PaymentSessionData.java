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
    private boolean mIsPaymentReadyToCharge;
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
     * Get the whether the all the payment data is ready for making a charge. This can be used to
     * set a buy button to enabled for prompt a user to fill in more information.
     *
     * @return whether the payment data is ready for making a charge.
     */
    public boolean isPaymentReadyToCharge() {
        return mIsPaymentReadyToCharge;
    }

    /**
     * Set whether the payment data is ready for making a charge.
     *
     * @param paymentReadyToCharge whether the payment data is ready for making a charge.
     */
    public void setPaymentReadyToCharge(boolean paymentReadyToCharge) {
        mIsPaymentReadyToCharge = paymentReadyToCharge;
    }

    /**
     * Get the value of shipping items in the associated {@link PaymentSession}
     *
     * @return the current value of the shipping items in the cart
     */
    public long getShippingTotal() {
        return mShippingTotal;
    }

    /**
     * Get the {@link ShippingInformation} collected as part of the associated {@link PaymentSession}
     * payment flow.
     *
     * @return {@link ShippingInformation} where the items being purchased should be shipped.
     */
    public ShippingInformation getShippingInformation() {
        return mShippingInformation;
    }

    /**
     * Set the {@link ShippingInformation} for the associated {@link PaymentSession}
     *
     * @param shippingInformation where the items being purchased should be shipped.
     */
    public void setShippingInformation(ShippingInformation shippingInformation) {
        mShippingInformation = shippingInformation;
    }

    /**
     * Get the {@link ShippingMethod} collected as part of the associated {@link PaymentSession}
     * payment flow.
     *
     * @return {@link ShippingMethod} how the items being purchased should be shipped.
     */
    public ShippingMethod getShippingMethod() {
        return mShippingMethod;
    }

    /**
     * Set the {@link ShippingMethod} for the associated {@link PaymentSession}
     *
     * @param shippingMethod how the items being purchased should be shipped.
     */
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
        parcel.writeInt(mIsPaymentReadyToCharge ? 1 : 0);
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
        mIsPaymentReadyToCharge = in.readInt() == 1;
        mSelectedPaymentMethodId = in.readString();
        mShippingTotal = in.readLong();
        mShippingInformation = in.readParcelable(ShippingInformation.class.getClassLoader());
        mShippingMethod = in.readParcelable(ShippingMethod.class.getClassLoader());
    }
}
