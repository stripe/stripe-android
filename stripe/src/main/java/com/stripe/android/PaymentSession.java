package com.stripe.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.Fragment;

import com.stripe.android.model.Customer;
import com.stripe.android.model.PaymentMethod;
import com.stripe.android.view.ActivityStarter;
import com.stripe.android.view.PaymentFlowActivity;
import com.stripe.android.view.PaymentFlowActivityStarter;
import com.stripe.android.view.PaymentMethodsActivity;
import com.stripe.android.view.PaymentMethodsActivityStarter;

import java.lang.ref.WeakReference;

/**
 * Represents a single start-to-finish payment operation.
 */
public class PaymentSession {

    public static final String TOKEN_PAYMENT_SESSION = "PaymentSession";
    public static final String EXTRA_PAYMENT_SESSION_ACTIVE = "payment_session_active";

    static final int PAYMENT_SHIPPING_DETAILS_REQUEST = 3004;
    static final int PAYMENT_METHOD_REQUEST = 3003;

    public static final String PAYMENT_SESSION_DATA_KEY = "payment_session_data";

    @NonNull
    private final ActivityStarter<PaymentMethodsActivity, PaymentMethodsActivityStarter.Args>
            mPaymentMethodsActivityStarter;
    @NonNull
    private final ActivityStarter<PaymentFlowActivity, PaymentFlowActivityStarter.Args>
            mPaymentFlowActivityStarter;

    @NonNull private final CustomerSession mCustomerSession;
    @NonNull private final PaymentSessionPrefs mPaymentSessionPrefs;
    private PaymentSessionData mPaymentSessionData;
    @Nullable private PaymentSessionListener mPaymentSessionListener;
    private PaymentSessionConfig mPaymentSessionConfig;

    /**
     * Create a PaymentSession attached to the given host Activity.
     *
     * @param activity an <code>Activity</code> from which to launch other Stripe Activities. This
     *                     Activity will receive results in
     *                     <code>Activity#onActivityResult(int, int, Intent)</code> that should be
     *                     passed back to this session.
     */
    public PaymentSession(@NonNull Activity activity) {
        this(CustomerSession.getInstance(),
                new PaymentMethodsActivityStarter(activity),
                new PaymentFlowActivityStarter(activity),
                new PaymentSessionData(),
                new PaymentSessionPrefs(activity));
    }

    public PaymentSession(@NonNull Fragment fragment) {
        this(CustomerSession.getInstance(),
                new PaymentMethodsActivityStarter(fragment),
                new PaymentFlowActivityStarter(fragment),
                new PaymentSessionData(),
                new PaymentSessionPrefs(fragment.requireActivity()));
    }

    @VisibleForTesting
    PaymentSession(
            @NonNull CustomerSession customerSession,
            @NonNull ActivityStarter<PaymentMethodsActivity, PaymentMethodsActivityStarter.Args>
                    paymentMethodsActivityStarter,
            @NonNull ActivityStarter<PaymentFlowActivity, PaymentFlowActivityStarter.Args>
                    paymentFlowActivityStarter,
            @NonNull PaymentSessionData paymentSessionData,
            @NonNull PaymentSessionPrefs paymentSessionPrefs) {
        mCustomerSession = customerSession;
        mPaymentMethodsActivityStarter = paymentMethodsActivityStarter;
        mPaymentFlowActivityStarter = paymentFlowActivityStarter;
        mPaymentSessionData = paymentSessionData;
        mPaymentSessionPrefs = paymentSessionPrefs;
    }

    /**
     * Notify this payment session that it is complete
     */
    public void onCompleted() {
        mCustomerSession.resetUsageTokens();
    }

    /**
     * Method to handle Activity results from Stripe activities. Pass data here from your
     * host's <code>#onActivityResult(int, int, Intent)</code> function.
     *
     * @param requestCode the request code used to open the resulting activity
     * @param resultCode a result code representing the success of the intended action
     * @param data an {@link Intent} with the resulting data from the Activity
     * @return {@code true} if the activity result was handled by this function,
     * otherwise {@code false}
     */
    public boolean handlePaymentData(int requestCode, int resultCode, @NonNull Intent data) {
        if (requestCode != PAYMENT_METHOD_REQUEST &&
                requestCode != PAYMENT_SHIPPING_DETAILS_REQUEST) {
            return false;
        }

        if (resultCode == Activity.RESULT_CANCELED) {
            fetchCustomer();
            return false;
        } else if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case PAYMENT_METHOD_REQUEST: {
                    final PaymentMethod paymentMethod =
                            data.getParcelableExtra(PaymentMethodsActivity.EXTRA_SELECTED_PAYMENT);
                    if (paymentMethod != null) {
                        persistPaymentMethod(paymentMethod);
                        mPaymentSessionData.setPaymentMethod(paymentMethod);
                        mPaymentSessionData.updateIsPaymentReadyToCharge(mPaymentSessionConfig);
                        if (mPaymentSessionListener != null) {
                            mPaymentSessionListener
                                    .onPaymentSessionDataChanged(mPaymentSessionData);
                            mPaymentSessionListener.onCommunicatingStateChanged(false);
                        }
                    }
                    return true;
                }
                case PAYMENT_SHIPPING_DETAILS_REQUEST: {
                    final PaymentSessionData paymentSessionData = data.getParcelableExtra(
                            PAYMENT_SESSION_DATA_KEY);
                    paymentSessionData.updateIsPaymentReadyToCharge(mPaymentSessionConfig);
                    mPaymentSessionData = paymentSessionData;
                    if (mPaymentSessionListener != null) {
                        mPaymentSessionListener.onPaymentSessionDataChanged(paymentSessionData);
                    }
                    return true;
                }
                default: {
                    break;
                }
            }
        }
        return false;
    }

    private void persistPaymentMethod(@NonNull PaymentMethod paymentMethod) {
        final Customer customer = mCustomerSession.getCachedCustomer();
        final String customerId = customer != null ? customer.getId() : null;
        if (customerId != null && paymentMethod.id != null) {
            mPaymentSessionPrefs
                    .saveSelectedPaymentMethodId(customerId, paymentMethod.id);
        }
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
     * @param savedInstanceState a <code>Bundle</code> containing the saved state of a
     *                           PaymentSession that was stored in
     *                           {@link #savePaymentSessionInstanceState(Bundle)}
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
                mCustomerSession.resetUsageTokens();
            }
            mCustomerSession.addProductUsageTokenIfValid(TOKEN_PAYMENT_SESSION);
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
        fetchCustomer();
        return true;
    }

    /**
     * See {@link #presentPaymentMethodSelection(boolean, String)}
     */
    public void presentPaymentMethodSelection() {
        presentPaymentMethodSelection(false, null);
    }

    /**
     * See {@link #presentPaymentMethodSelection(boolean, String)}
     */
    public void presentPaymentMethodSelection(boolean shouldRequirePostalCode) {
        presentPaymentMethodSelection(shouldRequirePostalCode, null);
    }

    /**
     * See {@link #presentPaymentMethodSelection(boolean, String)}
     */
    public void presentPaymentMethodSelection(@NonNull String selectedPaymentMethodId) {
        presentPaymentMethodSelection(false, selectedPaymentMethodId);
    }

    /**
     * Launch the {@link PaymentMethodsActivity} to allow the user to select a payment method,
     * or to add a new one.
     *
     * <p>The initial selected Payment Method ID uses the following logic.</p>
     * <ol>
     * <li>If {@param userSelectedPaymentMethodId} is specified, use that</li>
     * <li>If the instance's {@link PaymentSessionData#getPaymentMethod()} is non-null,
     * use that</li>
     * <li>If the instance's {@link PaymentSessionPrefs#getSelectedPaymentMethodId(String)}
     * is non-null, use that</li>
     * <li>Otherwise, choose the most recently added Payment Method</li>
     * </ol>
     *
     * See {@link #getSelectedPaymentMethodId(String)}
     *
     * @param shouldRequirePostalCode if true, require postal code when adding a payment method
     * @param userSelectedPaymentMethodId if non-null, the ID of the Payment Method that should be
     *                                    initially selected on the Payment Method selection screen
     */
    public void presentPaymentMethodSelection(boolean shouldRequirePostalCode,
                                              @Nullable String userSelectedPaymentMethodId) {
        mPaymentMethodsActivityStarter.startForResult(PAYMENT_METHOD_REQUEST,
                new PaymentMethodsActivityStarter.Args.Builder()
                        .setInitialPaymentMethodId(
                                getSelectedPaymentMethodId(userSelectedPaymentMethodId))
                        .setShouldRequirePostalCode(shouldRequirePostalCode)
                        .setIsPaymentSessionActive(true)
                        .setPaymentConfiguration(PaymentConfiguration.getInstance())
                        .build()
        );
    }

    @Nullable
    @VisibleForTesting
    String getSelectedPaymentMethodId(@Nullable String userSelectedPaymentMethodId) {
        final String selectedPaymentMethodId;
        if (userSelectedPaymentMethodId != null) {
            selectedPaymentMethodId = userSelectedPaymentMethodId;
        } else if (mPaymentSessionData.getPaymentMethod() != null) {
            selectedPaymentMethodId = mPaymentSessionData.getPaymentMethod().id;
        } else {
            final Customer customer = mCustomerSession.getCachedCustomer();
            final String customerId = customer != null ? customer.getId() : null;
            selectedPaymentMethodId = customerId != null ?
                    mPaymentSessionPrefs.getSelectedPaymentMethodId(customerId) : null;
        }

        return selectedPaymentMethodId;
    }

    /**
     * Save the data associated with this PaymentSession. This should be called in the host's
     * <code>onSaveInstanceState(Bundle)</code> method.
     *
     * @param outState the host activity's outgoing <code>Bundle</code>
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
        mPaymentFlowActivityStarter.startForResult(PAYMENT_SHIPPING_DETAILS_REQUEST,
                new PaymentFlowActivityStarter.Args.Builder()
                        .setPaymentSessionConfig(mPaymentSessionConfig)
                        .setPaymentSessionData(mPaymentSessionData)
                        .setIsPaymentSessionActive(true)
                        .build());
    }

    /**
     * @return the data associated with the instance of this class.
     */
    public PaymentSessionData getPaymentSessionData() {
        return mPaymentSessionData;
    }

    /**
     * Should be called during the host <code>Activity</code>'s onDestroy to detach listeners.
     */
    public void onDestroy() {
        mPaymentSessionListener = null;
    }

    private void fetchCustomer() {
        if (mPaymentSessionListener != null) {
            mPaymentSessionListener.onCommunicatingStateChanged(true);
        }
        mCustomerSession.retrieveCurrentCustomer(
                new CustomerSession.CustomerRetrievalListener() {
                    @Override
                    public void onCustomerRetrieved(@NonNull Customer customer) {
                        mPaymentSessionData.updateIsPaymentReadyToCharge(mPaymentSessionConfig);
                        if (mPaymentSessionListener != null) {
                            mPaymentSessionListener
                                    .onPaymentSessionDataChanged(mPaymentSessionData);
                            mPaymentSessionListener.onCommunicatingStateChanged(false);
                        }
                    }

                    @Override
                    public void onError(int httpCode, @NonNull String errorMessage,
                                        @Nullable StripeError stripeError) {
                        if (mPaymentSessionListener != null) {
                            mPaymentSessionListener.onError(httpCode, errorMessage);
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
        void onError(int errorCode, @NonNull String errorMessage);

        /**
         * Notification method called when the {@link PaymentSessionData} for this
         * session has changed.
         *
         * @param data the updated {@link PaymentSessionData}
         */
        void onPaymentSessionDataChanged(@NonNull PaymentSessionData data);
    }

    /**
     * Abstract implementation of {@link PaymentSessionListener} that holds a
     * {@link WeakReference} to an <code>Activity</code> object.
     */
    public abstract static class ActivityPaymentSessionListener<A extends Activity>
            implements PaymentSessionListener {

        @NonNull
        private final WeakReference<A> mActivityRef;

        public ActivityPaymentSessionListener(@NonNull A activity) {
            this.mActivityRef = new WeakReference<>(activity);
        }

        @Nullable
        protected A getListenerActivity() {
            return mActivityRef.get();
        }
    }
}
