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
    @NonNull @PaymentResultListener.PaymentResult private String mPaymentResult =
            PaymentResultListener.INCOMPLETE;
    @Nullable private ShippingInformation mShippingInformation;
    @Nullable private ShippingMethod mShippingMethod;

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

    void setSelectedPaymentMethodId(@Nullable String selectedPaymentMethodId) {
        mSelectedPaymentMethodId = selectedPaymentMethodId == null
                ? NO_PAYMENT
                : selectedPaymentMethodId;
    }

    void setShippingTotal(long shippingTotal) {
        mShippingTotal = shippingTotal;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PaymentSessionData that = (PaymentSessionData) o;

        if (mCartTotal != that.mCartTotal) return false;
        if (mIsPaymentReadyToCharge != that.mIsPaymentReadyToCharge) return false;
        if (mShippingTotal != that.mShippingTotal) return false;
        if (!mSelectedPaymentMethodId.equals(that.mSelectedPaymentMethodId)) return false;
        if (!mPaymentResult.equals(that.mPaymentResult)) return false;
        if (mShippingInformation != null
                ? !mShippingInformation.equals(that.mShippingInformation)
                : that.mShippingInformation != null) {
            return false;
        }
        return mShippingMethod != null
                ? mShippingMethod.equals(that.mShippingMethod)
                : that.mShippingMethod == null;
    }

    @Override
    public int hashCode() {
        int result = (int) (mCartTotal ^ (mCartTotal >>> 32));
        result = 31 * result + (mIsPaymentReadyToCharge ? 1 : 0);
        result = 31 * result + mSelectedPaymentMethodId.hashCode();
        result = 31 * result + (int) (mShippingTotal ^ (mShippingTotal >>> 32));
        result = 31 * result + mPaymentResult.hashCode();
        result = 31 * result + (mShippingInformation != null ? mShippingInformation.hashCode() : 0);
        result = 31 * result + (mShippingMethod != null ? mShippingMethod.hashCode() : 0);
        return result;
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
        parcel.writeString(mSelectedPaymentMethodId);
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

    private PaymentSessionData(Parcel in) {
        mCartTotal = in.readLong();
        mIsPaymentReadyToCharge = in.readInt() == 1;
        mPaymentResult = PaymentSessionUtils.paymentResultFromString(in.readString());
        mSelectedPaymentMethodId = in.readString();
        mShippingInformation = in.readParcelable(ShippingInformation.class.getClassLoader());
        mShippingMethod = in.readParcelable(ShippingMethod.class.getClassLoader());
        mShippingTotal = in.readLong();
    }
}
