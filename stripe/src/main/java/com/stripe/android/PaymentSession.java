package com.stripe.android;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.view.PaymentMethodsActivity;

/**
 * Represents a single start-to-finish payment operation.
 */
public class PaymentSession {

    public static final int PAYMENT_METHOD_REQUEST = 3003;
    @NonNull Activity mHostActivity;
    @Nullable private PaymentSessionListener mPaymentSessionListener;

    private static final String TOKEN_PAYMENT_SESSION = "PaymentSession";

    /**
     * Create a PaymentSession attached to the given host Activity.
     *
     * @param hostActivity an {@link Activity} from which to launch other Stripe Activities. This
     *                     Activity will receive results in
     *                     {@link Activity#onActivityResult(int, int, Intent)} that should be passed
     *                     back to this session.
     */
    public PaymentSession(@NonNull Activity hostActivity) {
        mHostActivity = hostActivity;
    }

    public boolean init(@NonNull PaymentSessionListener listener) {
        try {
            mPaymentSessionListener = listener;
            CustomerSession.getInstance().addProductUsageTokenIfValid(TOKEN_PAYMENT_SESSION);
            PaymentConfiguration.getInstance();
        } catch (IllegalStateException illegalState) {
            mPaymentSessionListener = null;
            return false;
        }
        return true;
    }

    /**
     * Method to handle Activity results from Stripe activities. Pass data here from your
     * host Activity's {@link Activity#onActivityResult(int, int, Intent)} function.
     *
     * @param requestCode the request code used to open the resulting activity
     * @param resultCode a result code representing the success of the intended action
     * @param data an {@link Intent} with the resulting data from the Activity
     * @return {@code true} if the activity result was handled by this function,
     * otherwise {@code false}
     */
    public boolean handlePaymentData(int requestCode, int resultCode, @NonNull Intent data) {
        if (resultCode == Activity.RESULT_CANCELED) {
            return false;
        } else if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case PAYMENT_METHOD_REQUEST:
                    break;
                default:
                    break;
            }
        }

        return false;
    }

    public void selectPaymentMethod() {
        mHostActivity.startActivityForResult(
                PaymentMethodsActivity.newIntent(mHostActivity),
                PAYMENT_METHOD_REQUEST);
    }

    /**
     * Represents a listener for PaymentSession actions, used to update the host activity
     * when necessary.
     */
    public interface PaymentSessionListener {
        void onPaymentMethodSelected();
        void onShippingAddressUpdated();
        void onShippingOptionsSelected();
    }
}
