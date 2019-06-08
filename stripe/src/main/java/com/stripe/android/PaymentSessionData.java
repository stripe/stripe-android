package com.stripe.android;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.model.PaymentMethod;
import com.stripe.android.model.ShippingInformation;
import com.stripe.android.model.ShippingMethod;
import com.stripe.android.utils.ObjectUtils;

/**
 * A data class representing the state of the associated {@link PaymentSession}.
 */
public class PaymentSessionData implements Parcelable {

    private long mCartTotal = 0L;
    private boolean mIsPaymentReadyToCharge;
    private long mShippingTotal = 0L;
    @NonNull @PaymentResultListener.PaymentResult private String mPaymentResult =
            PaymentResultListener.INCOMPLETE;
    @Nullable private ShippingInformation mShippingInformation;
    @Nullable private ShippingMethod mShippingMethod;
    @Nullable private PaymentMethod mPaymentMethod;

    public PaymentSessionData() { }

    /**
     * @return the selected payment method for the associated {@link PaymentSession}
     */
    @Nullable
    public PaymentMethod getPaymentMethod() {
        return mPaymentMethod;
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
     * Get the payment result for this PaymentSession.
     *
     * @return the current payment result for this session, or INCOMPLETE if not yet finished
     */
    @NonNull
    @PaymentResultListener.PaymentResult
    public String getPaymentResult() {
        return mPaymentResult;
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
     * Get the {@link ShippingInformation} collected as part of the associated
     * {@link PaymentSession}
     * payment flow.
     *
     * @return {@link ShippingInformation} where the items being purchased should be shipped.
     */
    @Nullable
    public ShippingInformation getShippingInformation() {
        return mShippingInformation;
    }

    /**
     * Set the {@link ShippingInformation} for the associated {@link PaymentSession}
     *
     * @param shippingInformation where the items being purchased should be shipped.
     */
    public void setShippingInformation(@Nullable ShippingInformation shippingInformation) {
        mShippingInformation = shippingInformation;
    }

    /**
     * Get the {@link ShippingMethod} collected as part of the associated {@link PaymentSession}
     * payment flow.
     *
     * @return {@link ShippingMethod} how the items being purchased should be shipped.
     */
    @Nullable
    public ShippingMethod getShippingMethod() {
        return mShippingMethod;
    }

    /**
     * Set the {@link ShippingMethod} for the associated {@link PaymentSession}
     *
     * @param shippingMethod how the items being purchased should be shipped.
     */
    public void setShippingMethod(@Nullable ShippingMethod shippingMethod) {
        mShippingMethod = shippingMethod;
    }

    void setCartTotal(long cartTotal) {
        mCartTotal = cartTotal;
    }

    void setPaymentResult(@NonNull @PaymentResultListener.PaymentResult String result) {
        mPaymentResult = result;
    }

    void setPaymentMethod(@Nullable PaymentMethod paymentMethod) {
        mPaymentMethod = paymentMethod;
    }

    void setShippingTotal(long shippingTotal) {
        mShippingTotal = shippingTotal;
    }

    /**
     * Function that looks at the {@link PaymentSessionConfig} and determines whether the data is
     * ready to charge.
     *
     * @param config specifies what data is required
     * @return whether the data is ready to charge
     */
    public boolean updateIsPaymentReadyToCharge(@NonNull PaymentSessionConfig config) {
        if (getPaymentMethod() == null ||
                (config.isShippingInfoRequired() && getShippingInformation() == null) ||
                (config.isShippingMethodRequired() && getShippingMethod() == null)) {
            setPaymentReadyToCharge(false);
            return false;
        }
        setPaymentReadyToCharge(true);
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof PaymentSessionData && typedEquals(
                (PaymentSessionData) obj));
    }

    private boolean typedEquals(@NonNull PaymentSessionData data) {
        return ObjectUtils.equals(mCartTotal, data.mCartTotal)
                && ObjectUtils.equals(mIsPaymentReadyToCharge, data.mIsPaymentReadyToCharge)
                && ObjectUtils.equals(mShippingTotal, data.mShippingTotal)
                && ObjectUtils.equals(mShippingInformation, data.mShippingInformation)
                && ObjectUtils.equals(mShippingMethod, data.mShippingMethod)
                && ObjectUtils.equals(mPaymentMethod, data.mPaymentMethod)
                && ObjectUtils.equals(mPaymentResult, data.mPaymentResult);
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hash(mCartTotal, mIsPaymentReadyToCharge, mPaymentMethod,
                mShippingTotal, mPaymentResult, mShippingInformation, mShippingMethod);
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
        parcel.writeString(mPaymentResult);
        parcel.writeParcelable(mPaymentMethod, i);
        parcel.writeParcelable(mShippingInformation, i);
        parcel.writeParcelable(mShippingMethod, i);
        parcel.writeLong(mShippingTotal);

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

    private PaymentSessionData(@NonNull Parcel in) {
        mCartTotal = in.readLong();
        mIsPaymentReadyToCharge = in.readInt() == 1;
        mPaymentResult = PaymentSessionUtils.paymentResultFromString(in.readString());
        mPaymentMethod = in.readParcelable(PaymentMethod.class.getClassLoader());
        mShippingInformation = in.readParcelable(ShippingInformation.class.getClassLoader());
        mShippingMethod = in.readParcelable(ShippingMethod.class.getClassLoader());
        mShippingTotal = in.readLong();
    }
}
