package com.stripe.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.stripe.android.exception.StripeException;
import com.stripe.android.model.Customer;
import com.stripe.android.model.PaymentMethod;
import com.stripe.android.model.ShippingInformation;
import com.stripe.android.model.Source;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Represents a logged-in session of a single Customer.
 *
 * See <a href="https://stripe.com/docs/mobile/android/standard#creating-ephemeral-keys">Creating ephemeral keys</a>
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class CustomerSession {

    public static final String ACTION_API_EXCEPTION = "action_api_exception";
    public static final String EXTRA_EXCEPTION = "exception";

    public static final String EVENT_SHIPPING_INFO_SAVED = "shipping_info_saved";

    static final String ACTION_ADD_SOURCE = "add_source";
    static final String ACTION_DELETE_SOURCE = "delete_source";
    static final String ACTION_ATTACH_PAYMENT_METHOD = "attach_payment_method";
    static final String ACTION_DETACH_PAYMENT_METHOD = "detach_payment_method";
    static final String ACTION_GET_PAYMENT_METHODS = "get_payment_methods";
    static final String ACTION_SET_DEFAULT_SOURCE = "default_source";
    static final String ACTION_SET_CUSTOMER_SHIPPING_INFO = "set_shipping_info";
    static final String KEY_PAYMENT_METHOD = "payment_method";
    static final String KEY_PAYMENT_METHOD_TYPE = "payment_method_type";
    static final String KEY_SOURCE = "source";
    static final String KEY_SOURCE_TYPE = "source_type";
    static final String KEY_SHIPPING_INFO = "shipping_info";

    // The maximum number of active threads we support
    private static final int THREAD_POOL_SIZE = 3;
    // Sets the amount of time an idle thread waits before terminating
    private static final int KEEP_ALIVE_TIME = 2;
    // Sets the Time Unit to seconds
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;

    private static final long KEY_REFRESH_BUFFER_IN_SECONDS = 30L;
    private static final long CUSTOMER_CACHE_DURATION_MILLISECONDS = TimeUnit.MINUTES.toMillis(1);

    private static CustomerSession mInstance;

    @Nullable private Customer mCustomer;
    private long mCustomerCacheTime;
    @NonNull private final LocalBroadcastManager mLocalBroadcastManager;
    @NonNull private final OperationIdFactory mOperationIdFactory;
    @NonNull private final EphemeralKeyManager mEphemeralKeyManager;
    @Nullable private final Calendar mProxyNowCalendar;
    @NonNull private final ThreadPoolExecutor mThreadPoolExecutor;
    @NonNull private final CustomerSessionProductUsage mProductUsage;
    @NonNull private final HashMap<String, RetrievalListener> listeners = new HashMap<>();

    /**
     * Create a CustomerSession with the provided {@link EphemeralKeyProvider}.
     *
     * <p>You must call {@link PaymentConfiguration#init(Context, String)} with your publishable key
     * before calling this method.</p>
     *
     * @param context The application context
     * @param ephemeralKeyProvider An {@link EphemeralKeyProvider} used to retrieve
     *                             {@link EphemeralKey} ephemeral keys
     * @param stripeAccountId An optional Stripe Connect account to associate with Customer-related
     *                        Stripe API Requests.
     *                        See {@link Stripe#Stripe(Context, String, String)}.
     * @param shouldPrefetchEphemeralKey If true, will immediately fetch an ephemeral key using
     *                                   {@param ephemeralKeyProvider}. Otherwise, will only fetch
     *                                   an ephemeral key when needed.
     */
    public static void initCustomerSession(@NonNull Context context,
                                           @NonNull EphemeralKeyProvider ephemeralKeyProvider,
                                           @Nullable String stripeAccountId,
                                           boolean shouldPrefetchEphemeralKey) {
        setInstance(new CustomerSession(context, ephemeralKeyProvider, Stripe.getAppInfo(),
                PaymentConfiguration.getInstance(context).getPublishableKey(),
                stripeAccountId, shouldPrefetchEphemeralKey));
    }

    /**
     * See {@link #initCustomerSession(Context, EphemeralKeyProvider, String, boolean)}
     */
    public static void initCustomerSession(@NonNull Context context,
                                           @NonNull EphemeralKeyProvider ephemeralKeyProvider,
                                           @Nullable String stripeAccountId) {
        initCustomerSession(context, ephemeralKeyProvider, stripeAccountId, true);
    }

    /**
     * See {@link #initCustomerSession(Context, EphemeralKeyProvider, String, boolean)}
     */
    public static void initCustomerSession(@NonNull Context context,
                                           @NonNull EphemeralKeyProvider ephemeralKeyProvider,
                                           boolean shouldPrefetchEphemeralKey) {
        initCustomerSession(context, ephemeralKeyProvider, null, shouldPrefetchEphemeralKey);
    }

    /**
     * See {@link #initCustomerSession(Context, EphemeralKeyProvider, String)}
     */
    public static void initCustomerSession(@NonNull Context context,
                                           @NonNull EphemeralKeyProvider ephemeralKeyProvider) {
        initCustomerSession(context, ephemeralKeyProvider, null);
    }

    /**
     * Gets the singleton instance of {@link CustomerSession}. If the session has not been
     * initialized, this will throw a {@link RuntimeException}.
     *
     * @return the singleton {@link CustomerSession} instance.
     */
    @NonNull
    public static CustomerSession getInstance() {
        if (mInstance == null) {
            throw new IllegalStateException(
                    "Attempted to get instance of CustomerSession without initialization.");
        }
        return mInstance;
    }

    @VisibleForTesting
    static void setInstance(@Nullable CustomerSession customerSession) {
        mInstance = customerSession;
    }

    /**
     * End the singleton instance of a {@link CustomerSession}.
     * Calls to {@link CustomerSession#getInstance()} will throw an {@link IllegalStateException}
     * after this call, until the user calls
     * {@link CustomerSession#initCustomerSession(Context, EphemeralKeyProvider)} again.
     */
    public static void endCustomerSession() {
        clearInstance();
    }

    @VisibleForTesting
    static void clearInstance() {
        if (mInstance != null) {
            mInstance.listeners.clear();
        }
        cancelCallbacks();
        setInstance(null);
    }

    /**
     * End any async calls in process and will not invoke callback listeners.
     * It will not clear the singleton instance of a {@link CustomerSession} so it can be
     * safely used when a view is being removed/destroyed to avoid null pointer exceptions
     * due to async operation delay.
     *
     * No need to call {@link CustomerSession#initCustomerSession(Context, EphemeralKeyProvider)}
     * again after this operation.
     */
    public static void cancelCallbacks() {
        if (mInstance == null) {
            return;
        }
        mInstance.mThreadPoolExecutor.shutdownNow();
    }

    private CustomerSession(@NonNull Context context, @NonNull EphemeralKeyProvider keyProvider,
                            @Nullable AppInfo appInfo, @NonNull String publishableKey,
                            @Nullable String stripeAccountId, boolean shouldPrefetchEphemeralKey) {
        this(context, keyProvider, null, createThreadPoolExecutor(),
                new StripeApiRepository(context, appInfo), publishableKey, stripeAccountId,
                shouldPrefetchEphemeralKey);
    }

    @VisibleForTesting
    CustomerSession(
            @NonNull Context context,
            @NonNull EphemeralKeyProvider keyProvider,
            @Nullable Calendar proxyNowCalendar,
            @NonNull ThreadPoolExecutor threadPoolExecutor,
            @NonNull StripeRepository stripeRepository,
            @NonNull String publishableKey,
            @Nullable String stripeAccountId,
            boolean shouldPrefetchEphemeralKey) {
        mOperationIdFactory = new OperationIdFactory();
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(context);
        mThreadPoolExecutor = threadPoolExecutor;
        mProxyNowCalendar = proxyNowCalendar;
        mProductUsage = new CustomerSessionProductUsage();
        final CustomerSessionEphemeralKeyManagerListener keyManagerListener =
                new CustomerSessionEphemeralKeyManagerListener(
                        new CustomerSessionRunnableFactory(
                                stripeRepository,
                                createHandler(),
                                mLocalBroadcastManager,
                                publishableKey,
                                stripeAccountId,
                                mProductUsage
                        ),
                        threadPoolExecutor, listeners, mProductUsage);
        mEphemeralKeyManager = new EphemeralKeyManager(
                keyProvider,
                keyManagerListener,
                KEY_REFRESH_BUFFER_IN_SECONDS,
                proxyNowCalendar,
                mOperationIdFactory,
                shouldPrefetchEphemeralKey
        );
    }

    @NonNull
    private Handler createHandler() {
        return new CustomerSessionHandler(new CustomerSessionHandler.Listener() {
            @Override
            public void onCustomerRetrieved(@Nullable Customer customer,
                                            @NonNull String operationId) {
                mCustomer = customer;
                mCustomerCacheTime = getCalendarInstance().getTimeInMillis();

                final CustomerRetrievalListener listener =
                        getListener(operationId);
                if (listener != null && customer != null) {
                    listener.onCustomerRetrieved(customer);
                }
            }

            @Override
            public void onSourceRetrieved(@Nullable Source source,
                                          @NonNull String operationId) {
                final SourceRetrievalListener listener = getListener(operationId);
                if (listener != null && source != null) {
                    listener.onSourceRetrieved(source);
                }
            }

            @Override
            public void onPaymentMethodRetrieved(@Nullable PaymentMethod paymentMethod,
                                                 @NonNull String operationId) {
                final PaymentMethodRetrievalListener listener =
                        getListener(operationId);
                if (listener != null && paymentMethod != null) {
                    listener.onPaymentMethodRetrieved(paymentMethod);
                }
            }

            @Override
            public void onPaymentMethodsRetrieved(@NonNull List<PaymentMethod> paymentMethods,
                                                  @NonNull String operationId) {
                final PaymentMethodsRetrievalListener listener =
                        getListener(operationId);
                if (listener != null) {
                    listener.onPaymentMethodsRetrieved(paymentMethods);
                }
            }

            @Override
            public void onCustomerShippingInfoSaved(@Nullable Customer customer) {
                mCustomer = customer;
                mLocalBroadcastManager
                        .sendBroadcast(new Intent(EVENT_SHIPPING_INFO_SAVED));
            }

            @Override
            public void onError(@NonNull StripeException exception,
                                @NonNull String operationId) {
                handleRetrievalError(operationId, exception);
            }
        });
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void addProductUsageTokenIfValid(@Nullable String token) {
        mProductUsage.add(token);
    }

    /**
     * Retrieve the current {@link Customer}. If the cached value at {@link #mCustomer} is not
     * stale, this returns immediately with the cache. If not, it fetches a new value and returns
     * that to the listener.
     *
     * @param listener a {@link CustomerRetrievalListener} to invoke with the result of getting the
     *                 customer, either from the cache or from the server
     */
    public void retrieveCurrentCustomer(@NonNull CustomerRetrievalListener listener) {
        final Customer cachedCustomer = getCachedCustomer();
        if (cachedCustomer != null) {
            listener.onCustomerRetrieved(cachedCustomer);
        } else {
            mCustomer = null;
            startOperation(null, null, listener);
        }
    }

    /**
     * Force an update of the current customer, regardless of how much time has passed.
     *
     * @param listener a {@link CustomerRetrievalListener} to invoke with the result of getting
     *                 the customer from the server
     */
    public void updateCurrentCustomer(@NonNull CustomerRetrievalListener listener) {
        mCustomer = null;
        startOperation(null, null, listener);
    }

    /**
     * Gets a cached customer, or {@code null} if the current customer has expired.
     *
     * @return the current value of {@link #mCustomer}, or {@code null} if the customer object is
     *         expired.
     */
    @Nullable
    public Customer getCachedCustomer() {
        if (canUseCachedCustomer()) {
            return mCustomer;
        } else {
            return null;
        }
    }

    /**
     * Add the Source to the current customer.
     *
     * @param sourceId the ID of the source to be added
     * @param listener a {@link SourceRetrievalListener} called when the API call completes
     *                 with the added {@link Source}.
     */
    public void addCustomerSource(
            @NonNull String sourceId,
            @NonNull @Source.SourceType String sourceType,
            @NonNull SourceRetrievalListener listener) {
        final Map<String, Object> arguments = new HashMap<>();
        arguments.put(KEY_SOURCE, sourceId);
        arguments.put(KEY_SOURCE_TYPE, sourceType);

        startOperation(ACTION_ADD_SOURCE, arguments, listener);
    }

    /**
     * Delete the Source from the current customer.
     *
     * @param sourceId the ID of the source to be deleted
     * @param listener a {@link SourceRetrievalListener} called when the API call completes
     *                 with the added {@link Source}.
     */
    public void deleteCustomerSource(
            @NonNull String sourceId,
            @NonNull SourceRetrievalListener listener) {
        final Map<String, Object> arguments = new HashMap<>();
        arguments.put(KEY_SOURCE, sourceId);
        startOperation(ACTION_DELETE_SOURCE, arguments, listener);
    }

    /**
     * Attaches a PaymentMethod to a customer.
     *
     * @param paymentMethodId the ID of the payment method to be attached
     * @param listener        a {@link PaymentMethodRetrievalListener} called when the API call
     *                        completes with the attached {@link PaymentMethod}.
     */
    public void attachPaymentMethod(
            @NonNull String paymentMethodId,
            @NonNull PaymentMethodRetrievalListener listener) {
        final Map<String, Object> arguments = new HashMap<>();
        arguments.put(KEY_PAYMENT_METHOD, paymentMethodId);
        startOperation(ACTION_ATTACH_PAYMENT_METHOD, arguments, listener);
    }

    /**
     * Detaches a PaymentMethod from a customer.
     *
     * @param paymentMethodId the ID of the payment method to be detached
     * @param listener        a {@link PaymentMethodRetrievalListener} called when the API call
     *                        completes with the detached {@link PaymentMethod}.
     */
    public void detachPaymentMethod(
            @NonNull String paymentMethodId,
            @NonNull PaymentMethodRetrievalListener listener) {
        final Map<String, Object> arguments = new HashMap<>();
        arguments.put(KEY_PAYMENT_METHOD, paymentMethodId);
        startOperation(ACTION_DETACH_PAYMENT_METHOD, arguments, listener);
    }

    /**
     * Retrieves all of the customer's PaymentMethod objects,
     * filtered by a {@link PaymentMethod.Type}.
     *
     * @param paymentMethodType the {@link PaymentMethod.Type} to filter by
     * @param listener          a {@link PaymentMethodRetrievalListener} called when the API call
     *                          completes with a list of {@link PaymentMethod} objects
     */
    public void getPaymentMethods(@NonNull PaymentMethod.Type paymentMethodType,
                                  @NonNull PaymentMethodsRetrievalListener listener) {
        final Map<String, Object> arguments = new HashMap<>();
        arguments.put(KEY_PAYMENT_METHOD_TYPE, paymentMethodType.code);
        startOperation(ACTION_GET_PAYMENT_METHODS, arguments, listener);
    }

    /**
     * Set the shipping information on the current customer.
     *
     * @param shippingInformation the data to be set
     */
    public void setCustomerShippingInformation(
            @NonNull ShippingInformation shippingInformation) {
        final Map<String, Object> arguments = new HashMap<>();
        arguments.put(KEY_SHIPPING_INFO, shippingInformation);
        startOperation(ACTION_SET_CUSTOMER_SHIPPING_INFO, arguments, null);
    }

    /**
     * Set the default Source of the current customer.
     *
     * @param sourceId the ID of the source to be set
     * @param listener a {@link CustomerRetrievalListener} called when the API call
     *                 completes with the updated customer
     */
    public void setCustomerDefaultSource(
            @NonNull String sourceId,
            @NonNull @Source.SourceType String sourceType,
            @NonNull CustomerRetrievalListener listener) {
        final Map<String, Object> arguments = new HashMap<>();
        arguments.put(KEY_SOURCE, sourceId);
        arguments.put(KEY_SOURCE_TYPE, sourceType);
        startOperation(ACTION_SET_DEFAULT_SOURCE, arguments, listener);
    }

    private void startOperation(@Nullable String action,
                                @Nullable Map<String, Object> arguments,
                                @Nullable RetrievalListener listener) {
        final String operationId = mOperationIdFactory.create();
        listeners.put(operationId, listener);
        mEphemeralKeyManager.retrieveEphemeralKey(operationId, action, arguments);
    }

    void resetUsageTokens() {
        mProductUsage.reset();
    }

    @Nullable
    @VisibleForTesting
    Customer getCustomer() {
        return mCustomer;
    }

    @VisibleForTesting
    long getCustomerCacheTime() {
        return mCustomerCacheTime;
    }

    @VisibleForTesting
    Set<String> getProductUsageTokens() {
        return mProductUsage.get();
    }

    private boolean canUseCachedCustomer() {
        final long currentTime = getCalendarInstance().getTimeInMillis();
        return mCustomer != null &&
                currentTime - mCustomerCacheTime < CUSTOMER_CACHE_DURATION_MILLISECONDS;
    }

    private void handleRetrievalError(@NonNull String operationId,
                                      @NonNull StripeException exception) {
        final RetrievalListener listener = listeners.remove(operationId);
        if (listener != null) {
            final String message = exception.getLocalizedMessage();
            listener.onError(
                    exception.getStatusCode(),
                    message != null ? message : "",
                    exception.getStripeError()
            );
        }

        resetUsageTokens();
    }

    @NonNull
    private static ThreadPoolExecutor createThreadPoolExecutor() {
        return new ThreadPoolExecutor(
                THREAD_POOL_SIZE,
                THREAD_POOL_SIZE,
                KEEP_ALIVE_TIME,
                KEEP_ALIVE_TIME_UNIT,
                new LinkedBlockingQueue<Runnable>());
    }

    @NonNull
    private Calendar getCalendarInstance() {
        return mProxyNowCalendar == null ? Calendar.getInstance() : mProxyNowCalendar;
    }

    @Nullable
    private <L extends RetrievalListener> L getListener(@NonNull String operationId) {
        return (L) listeners.remove(operationId);
    }

    public abstract static class ActivityCustomerRetrievalListener<A extends Activity>
            implements CustomerRetrievalListener {

        @NonNull private final WeakReference<A> mActivityRef;

        public ActivityCustomerRetrievalListener(@NonNull A activity) {
            mActivityRef = new WeakReference<>(activity);
        }

        @Nullable
        protected A getActivity() {
            return mActivityRef.get();
        }
    }

    public interface CustomerRetrievalListener extends RetrievalListener {
        void onCustomerRetrieved(@NonNull Customer customer);
    }

    public interface SourceRetrievalListener extends RetrievalListener {
        void onSourceRetrieved(@NonNull Source source);
    }

    public interface PaymentMethodRetrievalListener extends RetrievalListener {
        void onPaymentMethodRetrieved(@NonNull PaymentMethod paymentMethod);
    }

    public interface PaymentMethodsRetrievalListener extends RetrievalListener {
        void onPaymentMethodsRetrieved(@NonNull List<PaymentMethod> paymentMethods);
    }

    interface RetrievalListener {
        void onError(int errorCode, @NonNull String errorMessage,
                     @Nullable StripeError stripeError);
    }

    /**
     * Abstract implementation of {@link PaymentMethodsRetrievalListener} that holds a
     * {@link WeakReference} to an <code>Activity</code> object.
     */
    public abstract static class ActivityPaymentMethodsRetrievalListener<A extends Activity>
            implements PaymentMethodsRetrievalListener {
        @NonNull private final WeakReference<A> mActivityRef;

        public ActivityPaymentMethodsRetrievalListener(@NonNull A activity) {
            this.mActivityRef = new WeakReference<>(activity);
        }

        @Nullable
        protected A getActivity() {
            return mActivityRef.get();
        }
    }

    /**
     * Abstract implementation of {@link SourceRetrievalListener} that holds a
     * {@link WeakReference} to an <code>Activity</code> object.
     */
    public abstract static class ActivitySourceRetrievalListener<A extends Activity>
            implements SourceRetrievalListener {
        @NonNull private final WeakReference<A> mActivityRef;

        public ActivitySourceRetrievalListener(@NonNull A activity) {
            this.mActivityRef = new WeakReference<>(activity);
        }

        @Nullable
        protected A getActivity() {
            return mActivityRef.get();
        }
    }

    /**
     * Abstract implementation of {@link PaymentMethodRetrievalListener} that holds a
     * {@link WeakReference} to an <code>Activity</code> object.
     */
    public abstract static class ActivityPaymentMethodRetrievalListener<A extends Activity>
            implements PaymentMethodRetrievalListener {
        @NonNull private final WeakReference<A> mActivityRef;

        public ActivityPaymentMethodRetrievalListener(@NonNull A activity) {
            this.mActivityRef = new WeakReference<>(activity);
        }

        @Nullable
        protected A getActivity() {
            return mActivityRef.get();
        }
    }
}
