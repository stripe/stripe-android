package com.stripe.android;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import com.stripe.android.model.Customer;
import com.stripe.android.model.CustomerSource;
import com.stripe.android.view.PaymentFlowActivity;
import com.stripe.android.view.PaymentMethodsActivity;

import static com.stripe.android.CustomerSession.EVENT_API_ERROR;
import static com.stripe.android.CustomerSession.EVENT_CUSTOMER_RETRIEVED;
import static com.stripe.android.CustomerSession.EXTRA_CUSTOMER_RETRIEVED;
import static com.stripe.android.CustomerSession.EXTRA_ERROR_CODE;
import static com.stripe.android.CustomerSession.EXTRA_ERROR_MESSAGE;

/**
 * Represents a single start-to-finish payment operation.
 */
public class PaymentSession {

    public static final String TOKEN_PAYMENT_SESSION = "PaymentSession";
    public static final String EXTRA_PAYMENT_SESSION_ACTIVE = "payment_session_active";

    static final int PAYMENT_SHIPPING_DETAILS_REQUEST = 3004;
    static final int PAYMENT_METHOD_REQUEST = 3003;

    public static final String PAYMENT_SESSION_DATA_KEY = "payment_session_data";
    public static final String PAYMENT_SESSION_CONFIG = "payment_session_config";

    @Nullable private BroadcastReceiver mCustomerReceiver;
    @Nullable private BroadcastReceiver mErrorReceiver;
    @NonNull private Activity mHostActivity;
    @NonNull private PaymentSessionData mPaymentSessionData;
    @Nullable private PaymentSessionListener mPaymentSessionListener;
    @NonNull private PaymentSessionConfig mPaymentSessionConfig;

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
     * Complete a payment using the given provider.
     * @param provider a {@link PaymentCompletionProvider} that connects to a server and completes
     *                 a charge on a background thread.
     */
    public void completePayment(@NonNull PaymentCompletionProvider provider) {
        provider.completePayment(mPaymentSessionData,
                new PaymentResultListener() {
                    @Override
                    public void onPaymentResult(@NonNull @PaymentResult String paymentResult) {
                        mPaymentSessionData.setPaymentResult(paymentResult);
                        CustomerSession.getInstance().resetUsageTokens();
                        if (mPaymentSessionListener != null) {
                            mPaymentSessionListener
                                    .onPaymentSessionDataChanged(mPaymentSessionData);
                        }
                    }
                });
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
            fetchCustomer();
            return false;
        } else if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case PAYMENT_METHOD_REQUEST:
                    fetchCustomer();
                    return true;
                case PAYMENT_SHIPPING_DETAILS_REQUEST:
                    PaymentSessionData paymentSessionData = data.getParcelableExtra(
                            PAYMENT_SESSION_DATA_KEY);
                    updateIsPaymentReadyToCharge(mPaymentSessionConfig, paymentSessionData);
                    mPaymentSessionData = paymentSessionData;
                    mPaymentSessionListener.onPaymentSessionDataChanged(paymentSessionData);
                    return true;
                default:
                    break;
            }
        }
        return false;
    }

    /**
     * Function that looks at the {@link PaymentSessionConfig} and determines whether the data in
     * the provided {@link PaymentSessionData} is ready to charge.
     * Return with whether the data is ready to charge.
     *
     * @param paymentSessionConfig specifies what data is required.
     * @param paymentSessionData holds the data that has been collected.
     * @return whether the data in the provided {@link PaymentSessionData} is ready to charge.
     */
    public boolean updateIsPaymentReadyToCharge(PaymentSessionConfig paymentSessionConfig,
                                                PaymentSessionData paymentSessionData) {
        if (StripeTextUtils.isBlank(paymentSessionData.getSelectedPaymentMethodId()) ||
                (paymentSessionConfig.isShippingInfoRequired() &&
                        paymentSessionData.getShippingInformation() == null) ||
                (paymentSessionConfig.isShippingMethodRequired() &&
                        paymentSessionData.getShippingMethod() == null)) {
            paymentSessionData.setPaymentReadyToCharge(false);
            return false;
        }
        paymentSessionData.setPaymentReadyToCharge(true);
        return true;
    }

    /**
     * Initialize the PaymentSession with a {@link PaymentSessionListener} to be notified of
     * data changes.
     *
     * @param listener a {@link PaymentSessionListener} that will receive notifications of changes
     *                 in payment session status, including networking status
     * @param paymentSessionConfig a {@link PaymentSessionConfig} used to decide which items are
     *                             necessary in the PaymentSession.
     * @return {@code true} if the PaymentSession is initialized, {@code false} if a state error
     * occurs. Failure can only occur if there is no initialized {@link CustomerSession}.
     */
    public boolean init(@NonNull PaymentSessionListener listener,
                        @NonNull PaymentSessionConfig paymentSessionConfig) {
        return init(listener, paymentSessionConfig, null);
    }

    /**
     * Initialize the PaymentSession with a {@link PaymentSessionListener} to be notified of
     * data changes.
     *
     * @param listener a {@link PaymentSessionListener} that will receive notifications of changes
     *                 in payment session status, including networking status
     * @param paymentSessionConfig a {@link PaymentSessionConfig} used to decide which items are
     *                             necessary in the PaymentSession.
     * @param savedInstanceState a {@link Bundle} containing the saved state of a PaymentSession
     *                           that was stored in {@link #savePaymentSessionInstanceState(Bundle)}
     * @return {@code true} if the PaymentSession is initialized, {@code false} if a state error
     * occurs. Failure can only occur if there is no initialized {@link CustomerSession}.
     */
    public boolean init(
            @NonNull PaymentSessionListener listener,
            @NonNull PaymentSessionConfig paymentSessionConfig,
            @Nullable Bundle savedInstanceState) {

        // Checking to make sure that there is a valid CustomerSession -- the getInstance() call
        // will throw a runtime exception if none is ready.
        try {
            if (savedInstanceState == null) {
                CustomerSession.getInstance().resetUsageTokens();
            }
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
        mPaymentSessionConfig = paymentSessionConfig;
        initializeReceivers();
        registerReceivers();
        fetchCustomer();
        return true;
    }

    /**
     * Launch the {@link PaymentMethodsActivity} to allow the user to select a payment method,
     * or to add a new one.
     */
    public void presentPaymentMethodSelection() {
        Intent paymentMethodsIntent = PaymentMethodsActivity.newIntent(mHostActivity);
        paymentMethodsIntent.putExtra(EXTRA_PAYMENT_SESSION_ACTIVE, true);
        mHostActivity.startActivityForResult(paymentMethodsIntent, PAYMENT_METHOD_REQUEST);
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

    /**
     * Launch the {@link PaymentFlowActivity} to allow the user to fill in payment details.
     */
    public void presentShippingFlow() {
        Intent intent = new Intent(mHostActivity, PaymentFlowActivity.class);
        intent.putExtra(PAYMENT_SESSION_CONFIG, mPaymentSessionConfig);
        intent.putExtra(PAYMENT_SESSION_DATA_KEY, mPaymentSessionData);
        intent.putExtra(EXTRA_PAYMENT_SESSION_ACTIVE, true);
        mHostActivity.startActivityForResult(
                intent,
                PAYMENT_SHIPPING_DETAILS_REQUEST);
    }

    /**
     * @return the data associated with the instance of this class.
     */
    @NonNull
    public PaymentSessionData getPaymentSessionData() {
        return mPaymentSessionData;
    }

    /**
     * Should be called during the host {@link Activity}'s onDestroy to detach listeners.
     */
    public void onDestroy() {
        mPaymentSessionListener = null;
        LocalBroadcastManager.getInstance(mHostActivity)
                .unregisterReceiver(mErrorReceiver);
        LocalBroadcastManager.getInstance(mHostActivity)
                .unregisterReceiver(mCustomerReceiver);
    }

    private void fetchCustomer() {
        if (mPaymentSessionListener != null) {
            mPaymentSessionListener.onCommunicatingStateChanged(true);
        }
        CustomerSession.getInstance().retrieveCurrentCustomer(mHostActivity);
    }

    private void initializeReceivers() {
        mCustomerReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Customer customer = Customer.fromString(
                        intent.getStringExtra(EXTRA_CUSTOMER_RETRIEVED));
                if (customer == null) {
                    return;
                }

                String paymentId = customer.getDefaultSource();
                CustomerSource source = customer.getSourceById(paymentId);
                mPaymentSessionData.setSelectedPaymentSource(source);
                updateIsPaymentReadyToCharge(mPaymentSessionConfig, mPaymentSessionData);
                if (mPaymentSessionListener != null) {
                    mPaymentSessionListener
                            .onPaymentSessionDataChanged(mPaymentSessionData);
                    mPaymentSessionListener.onCommunicatingStateChanged(false);
                }
            }
        };

        mErrorReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int errorCode = intent.getIntExtra(EXTRA_ERROR_CODE, -1);
                String errorMessage = intent.getStringExtra(EXTRA_ERROR_MESSAGE);
                if (mPaymentSessionListener != null) {
                    mPaymentSessionListener.onError(errorCode, errorMessage);
                    mPaymentSessionListener.onCommunicatingStateChanged(false);
                }
            }
        };

    }

    private void registerReceivers() {
        // In case these are pre-registered, remove them.
        LocalBroadcastManager.getInstance(mHostActivity).unregisterReceiver(mErrorReceiver);
        LocalBroadcastManager.getInstance(mHostActivity).unregisterReceiver(mCustomerReceiver);
        LocalBroadcastManager.getInstance(mHostActivity).registerReceiver(
                mErrorReceiver,
                new IntentFilter(EVENT_API_ERROR));
        LocalBroadcastManager.getInstance(mHostActivity).registerReceiver(
                mCustomerReceiver,
                new IntentFilter(EVENT_CUSTOMER_RETRIEVED));
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
