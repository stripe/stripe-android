package com.stripe.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.stripe.android.model.Customer;
import com.stripe.android.view.PaymentMethodsActivity;

/**
 * Represents a single start-to-finish payment operation.
 */
public class PaymentSession {

    static final int PAYMENT_METHOD_REQUEST = 3003;
    static final String TOKEN_PAYMENT_SESSION = "PaymentSession";

    private static final String PAYMENT_SESSION_DATA_KEY = "payment_session_data";

    @NonNull private Activity mHostActivity;
    @NonNull private PaymentSessionData mPaymentSessionData;
    @Nullable private PaymentSessionListener mPaymentSessionListener;

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
        mPaymentSessionData = new PaymentSessionData();
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
                    fetchCustomer();
                    return true;
                default:
                    break;
            }
        }

        return false;
    }

    /**
     * Initialize the PaymentSession with a {@link PaymentSessionListener} to be notified of
     * data changes.
     *
     * @param listener a {@link PaymentSessionListener} that will receive notifications of changes
     *                 in payment session status, including networking status
     * @return {@code true} if the PaymentSession is initialized, {@code false} if a state error
     * occurs. Failure can only occur if there is no initialized {@link CustomerSession}.
     */
    public boolean init(@NonNull PaymentSessionListener listener) {
        return init(listener, null);
    }

    /**
     * Initialize the PaymentSession with a {@link PaymentSessionListener} to be notified of
     * data changes.
     *
     * @param listener a {@link PaymentSessionListener} that will receive notifications of changes
     *                 in payment session status, including networking status
     * @param savedInstanceState a {@link Bundle} containing the saved state of a PaymentSession
     *                           that was stored in {@link #savePaymentSessionInstanceState(Bundle)}
     * @return {@code true} if the PaymentSession is initialized, {@code false} if a state error
     * occurs. Failure can only occur if there is no initialized {@link CustomerSession}.
     */
    public boolean init(
            @NonNull PaymentSessionListener listener,
            @Nullable Bundle savedInstanceState) {

        // Checking to make sure that there is a valid CustomerSession -- the getInstance() call
        // will throw a runtime exception if none is ready.
        try {
            CustomerSession.getInstance().addProductUsageTokenIfValid(TOKEN_PAYMENT_SESSION);
        } catch (IllegalStateException illegalState) {
            mPaymentSessionListener = null;
            return false;
        }

        mPaymentSessionListener = listener;
        if (savedInstanceState != null) {
            PaymentSessionData data =
                    savedInstanceState.getParcelable(PAYMENT_SESSION_DATA_KEY);
            if (data != null) {
                mPaymentSessionData = data;
            }
        }
        fetchCustomer();
        return true;
    }

    /**
     * Launch the {@link PaymentMethodsActivity} to allow the user to select a payment method,
     * or to add a new one.
     */
    public void selectPaymentMethod() {
        mHostActivity.startActivityForResult(
                PaymentMethodsActivity.newIntent(mHostActivity),
                PAYMENT_METHOD_REQUEST);
    }

    /**
     * Save the data associated with this PaymentSession. This should be called in the host Activity
     * {@link Activity#onSaveInstanceState(Bundle)} method.
     *
     * @param outState the host activity's outgoing {@link Bundle}
     */
    public void savePaymentSessionInstanceState(@NonNull Bundle outState) {
        outState.putParcelable(PAYMENT_SESSION_DATA_KEY, mPaymentSessionData);
    }

    /**
     * Set the cart total for this PaymentSession. This should not include shipping costs.
     *
     * @param cartTotal the current total price for all non-shipping and non-tax items in
     *                  a customer's cart
     */
    public void setCartTotal(@IntRange(from = 0) long cartTotal) {
        mPaymentSessionData.setCartTotal(cartTotal);
    }

    private void fetchCustomer() {
        if (mPaymentSessionListener != null) {
            mPaymentSessionListener.onCommunicatingStateChanged(true);
        }
        CustomerSession.getInstance().retrieveCurrentCustomer(
                new CustomerSession.CustomerRetrievalListener() {
                    @Override
                    public void onCustomerRetrieved(@NonNull Customer customer) {
                        String paymentId = customer.getDefaultSource();
                        mPaymentSessionData.setSelectedPaymentMethodId(paymentId);

                        if (mPaymentSessionListener != null) {
                            mPaymentSessionListener
                                    .onPaymentSessionDataChanged(mPaymentSessionData);
                            mPaymentSessionListener.onCommunicatingStateChanged(false);
                        }
                    }

                    @Override
                    public void onError(int errorCode, @Nullable String errorMessage) {
                        if (mPaymentSessionListener != null) {
                            mPaymentSessionListener.onError(errorCode, errorMessage);
                            mPaymentSessionListener.onCommunicatingStateChanged(false);
                        }
                    }
                });
    }

    /**
     * Represents a listener for PaymentSession actions, used to update the host activity
     * when necessary.
     */
    public interface PaymentSessionListener {
        /**
         * Notification method called when network communication is beginning or ending.
         *
         * @param isCommunicating {@code true} if communication is starting, {@code false} if it
         * is stopping.
         */
        void onCommunicatingStateChanged(boolean isCommunicating);

        /**
         * Notification method called when an error has occurred.
         *
         * @param errorCode a network code associated with the error
         * @param errorMessage a message associated with the error
         */
        void onError(int errorCode, @Nullable String errorMessage);

        /**
         * Notification method called when the {@link PaymentSessionData} for this
         * session has changed.
         *
         * @param data the updated {@link PaymentSessionData}
         */
        void onPaymentSessionDataChanged(@NonNull PaymentSessionData data);
    }

}
