package com.stripe.android;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;
import android.support.v4.content.LocalBroadcastManager;

import com.stripe.android.exception.APIConnectionException;
import com.stripe.android.exception.APIException;
import com.stripe.android.exception.InvalidRequestException;
import com.stripe.android.exception.StripeException;
import com.stripe.android.model.Customer;
import com.stripe.android.model.ShippingInformation;
import com.stripe.android.model.Source;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Represents a logged-in session of a single Customer.
 */
public class CustomerSession
        implements EphemeralKeyManager.KeyManagerListener<CustomerEphemeralKey> {

    public static final String ACTION_API_EXCEPTION = "action_api_exception";
    public static final String EXTRA_EXCEPTION = "exception";

    public static final String EVENT_SHIPPING_INFO_SAVED = "shipping_info_saved";

    private static final String ACTION_ADD_SOURCE = "add_source";
    private static final String ACTION_DELETE_SOURCE = "delete_source";
    private static final String ACTION_SET_DEFAULT_SOURCE = "default_source";
    private static final String ACTION_SET_CUSTOMER_SHIPPING_INFO = "set_shipping_info";
    private static final String KEY_SOURCE = "source";
    private static final String KEY_SOURCE_TYPE = "source_type";
    private static final String KEY_SHIPPING_INFO = "shipping_info";
    private static final String TOKEN_PAYMENT_SESSION = "PaymentSession";
    private static final Set<String> VALID_TOKENS =
            new HashSet<>(Arrays.asList("AddSourceActivity",
                    "PaymentMethodsActivity",
                    "PaymentFlowActivity",
                    TOKEN_PAYMENT_SESSION,
                    "ShippingInfoScreen",
                    "ShippingMethodScreen"));

    private static final int CUSTOMER_RETRIEVED = 7;
    private static final int CUSTOMER_ERROR = 11;
    private static final int SOURCE_RETRIEVED = 13;
    private static final int SOURCE_ERROR = 17;
    private static final int CUSTOMER_SHIPPING_INFO_SAVED = 19;

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
    @Nullable private Context mContext;
    @NonNull private final Map<String, WeakReference<CustomerRetrievalListener>>
            mCustomerRetrievalListenerRefs = new HashMap<>();
    @NonNull private final Map<String, WeakReference<SourceRetrievalListener>>
            mSourceRetrievalListenerRefs = new HashMap<>();

    @NonNull private final EphemeralKeyManager mEphemeralKeyManager;
    @NonNull private final Handler mUiThreadHandler;
    @NonNull private final Set<String> mProductUsageTokens;
    @Nullable private final Calendar mProxyNowCalendar;
    @Nullable private final StripeApiProxy mStripeApiProxy;

    // A queue of Runnables for doing customer updates
    @NonNull private final BlockingQueue<Runnable> mNetworkQueue = new LinkedBlockingQueue<>();
    @NonNull private final ThreadPoolExecutor mThreadPoolExecutor;
    @NonNull private final StripeApiHandler mApiHandler;

    /**
     * Create a CustomerSession with the provided {@link EphemeralKeyProvider}.
     *
     * @param keyProvider an {@link EphemeralKeyProvider} used to get
     * {@link CustomerEphemeralKey EphemeralKeys} as needed
     */
    public static void initCustomerSession(@NonNull EphemeralKeyProvider keyProvider) {
        initCustomerSession(keyProvider, null, null);
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

    /**
     * End the singleton instance of a {@link CustomerSession}.
     * Calls to {@link CustomerSession#getInstance()} will throw an {@link IllegalStateException}
     * after this call, until the user calls
     * {@link CustomerSession#initCustomerSession(EphemeralKeyProvider)} again.
     */
    public static void endCustomerSession() {
        clearInstance();
    }

    @VisibleForTesting
    static void initCustomerSession(
            @NonNull EphemeralKeyProvider keyProvider,
            @Nullable StripeApiProxy stripeApiProxy,
            @Nullable Calendar proxyNowCalendar) {
        mInstance = new CustomerSession(keyProvider, stripeApiProxy, proxyNowCalendar);
    }

    @VisibleForTesting
    static void clearInstance() {
        if (mInstance != null) {
            mInstance.mCustomerRetrievalListenerRefs.clear();
            mInstance.mSourceRetrievalListenerRefs.clear();
        }
        cancelCallbacks();
        mInstance = null;
    }

    /**
     * End any async calls in process and will not invoke callback listeners.
     * It will not clear the singleton instance of a {@link CustomerSession} so it can be
     * safely used when a view is being removed/destroyed to avoid null pointer exceptions
     * due to async operation delay.
     * No need to call {@link CustomerSession#initCustomerSession(EphemeralKeyProvider)} again
     * after this operation.
     */
    public static void cancelCallbacks() {
        if (mInstance == null) {
            return;
        }
        mInstance.mThreadPoolExecutor.shutdownNow();
    }

    private CustomerSession(
            @NonNull EphemeralKeyProvider keyProvider,
            @Nullable StripeApiProxy stripeApiProxy,
            @Nullable Calendar proxyNowCalendar) {
        mThreadPoolExecutor = createThreadPoolExecutor();
        mUiThreadHandler = createMainThreadHandler();
        mStripeApiProxy = stripeApiProxy;
        mProxyNowCalendar = proxyNowCalendar;
        mProductUsageTokens = new HashSet<>();
        mEphemeralKeyManager = new EphemeralKeyManager<>(
                keyProvider,
                this,
                KEY_REFRESH_BUFFER_IN_SECONDS,
                proxyNowCalendar,
                CustomerEphemeralKey.class);
        mApiHandler = new StripeApiHandler();
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void addProductUsageTokenIfValid(@Nullable String token) {
        if (VALID_TOKENS.contains(token)) {
            mProductUsageTokens.add(token);
        }
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
        if (canUseCachedCustomer()) {
            listener.onCustomerRetrieved(getCachedCustomer());
        } else {
            mCustomer = null;

            final String operationId = UUID.randomUUID().toString();
            mCustomerRetrievalListenerRefs.put(operationId, new WeakReference<>(listener));
            mEphemeralKeyManager.retrieveEphemeralKey(operationId, null, null);
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

        final String operationId = UUID.randomUUID().toString();
        mCustomerRetrievalListenerRefs.put(operationId, new WeakReference<>(listener));
        mEphemeralKeyManager.retrieveEphemeralKey(operationId, null, null);
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
     * Add the input source to the current customer object.
     *
     * @param context the {@link Context} to use for resources
     * @param sourceId the ID of the source to be added
     * @param listener a {@link SourceRetrievalListener} to be notified when the api call is
     *                 complete
     */
    public void addCustomerSource(
            @NonNull Context context,
            @NonNull String sourceId,
            @NonNull @Source.SourceType String sourceType,
            @Nullable SourceRetrievalListener listener) {
        mContext = context.getApplicationContext();
        final Map<String, Object> arguments = new HashMap<>();
        arguments.put(KEY_SOURCE, sourceId);
        arguments.put(KEY_SOURCE_TYPE, sourceType);

        final String operationId = UUID.randomUUID().toString();
        if (listener != null) {
            mSourceRetrievalListenerRefs.put(operationId, new WeakReference<>(listener));
        }
        mEphemeralKeyManager.retrieveEphemeralKey(operationId, ACTION_ADD_SOURCE, arguments);
    }

    /**
     * Delete the source from the current customer object.
     * @param context the {@link Context} to use for resources
     * @param sourceId the ID of the source to be deleted
     * @param listener a {@link SourceRetrievalListener} to be notified when the api call is
     *                 complete. The api call will return the removed source.
     */
    public void deleteCustomerSource(
            @NonNull Context context,
            @NonNull String sourceId,
            @Nullable SourceRetrievalListener listener) {
        mContext = context.getApplicationContext();
        Map<String, Object> arguments = new HashMap<>();
        arguments.put(KEY_SOURCE, sourceId);

        final String operationId = UUID.randomUUID().toString();
        if (listener != null) {
            mSourceRetrievalListenerRefs.put(operationId, new WeakReference<>(listener));
        }
        mEphemeralKeyManager.retrieveEphemeralKey(operationId, ACTION_DELETE_SOURCE, arguments);
    }

    /**
     * Set the shipping information on the current customer object.
     *
     * @param context a {@link Context} to use for resources
     * @param shippingInformation the data to be set
     */
    public void setCustomerShippingInformation(
            @NonNull Context context,
            @NonNull ShippingInformation shippingInformation) {
        mContext = context.getApplicationContext();
        final Map<String, Object> arguments = new HashMap<>();
        arguments.put(KEY_SHIPPING_INFO, shippingInformation);
        mEphemeralKeyManager.retrieveEphemeralKey(null, ACTION_SET_CUSTOMER_SHIPPING_INFO,
                arguments);
    }

    /**
     * Set the default source of the current customer object.
     *
     * @param context a {@link Context} to use for resources
     * @param sourceId the ID of the source to be set
     * @param listener a {@link CustomerRetrievalListener} to be notified about an update to the
     *                 customer
     */
    public void setCustomerDefaultSource(
            @NonNull Context context,
            @NonNull String sourceId,
            @NonNull @Source.SourceType String sourceType,
            @Nullable CustomerRetrievalListener listener) {
        mContext = context.getApplicationContext();
        final Map<String, Object> arguments = new HashMap<>();
        arguments.put(KEY_SOURCE, sourceId);
        arguments.put(KEY_SOURCE_TYPE, sourceType);

        final String operationId = UUID.randomUUID().toString();
        if (listener != null) {
            mCustomerRetrievalListenerRefs.put(operationId, new WeakReference<>(listener));
        }
        mEphemeralKeyManager.retrieveEphemeralKey(operationId, ACTION_SET_DEFAULT_SOURCE,
                arguments);
    }

    void resetUsageTokens() {
        mProductUsageTokens.clear();
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
        return mProductUsageTokens;
    }

    private void addCustomerSource(
            @NonNull final Context context,
            @NonNull final CustomerEphemeralKey key,
            @NonNull final String sourceId,
            @NonNull final String sourceType,
            @Nullable final String operationId) {
        final Runnable fetchCustomerRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    final SourceMessage sourceMessage = new SourceMessage(
                            operationId,
                            addCustomerSourceWithKey(context, key, sourceId, sourceType)
                    );
                    mUiThreadHandler.sendMessage(
                            mUiThreadHandler.obtainMessage(SOURCE_RETRIEVED, sourceMessage));
                } catch (StripeException stripeEx) {
                    mUiThreadHandler
                            .sendMessage(mUiThreadHandler.obtainMessage(SOURCE_ERROR,
                                    new ExceptionMessage(operationId, stripeEx)));
                    sendErrorIntent(stripeEx);
                }
            }
        };

        executeRunnable(fetchCustomerRunnable);
    }

    private boolean canUseCachedCustomer() {
        final long currentTime = getCalendarInstance().getTimeInMillis();
        return mCustomer != null &&
                currentTime - mCustomerCacheTime < CUSTOMER_CACHE_DURATION_MILLISECONDS;
    }

    private void deleteCustomerSource(
            @NonNull final Context context,
            @NonNull final CustomerEphemeralKey key,
            @NonNull final String sourceId,
            @Nullable final String operationId) {
        final Runnable fetchCustomerRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    final SourceMessage sourceMessage = new SourceMessage(
                            operationId,
                            deleteCustomerSourceWithKey(context, key, sourceId)
                    );
                    mUiThreadHandler.sendMessage(
                            mUiThreadHandler.obtainMessage(SOURCE_RETRIEVED, sourceMessage));

                } catch (StripeException stripeEx) {
                    mUiThreadHandler.sendMessage(
                            mUiThreadHandler.obtainMessage(SOURCE_ERROR,
                                    new ExceptionMessage(operationId, stripeEx)));
                    sendErrorIntent(stripeEx);
                }
            }
        };
        executeRunnable(fetchCustomerRunnable);
    }

    private void setCustomerSourceDefault(
            @NonNull final Context context,
            @NonNull final CustomerEphemeralKey key,
            @NonNull final String sourceId,
            @NonNull final String sourceType,
            @Nullable final String operationId) {
        final Runnable fetchCustomerRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    final CustomerMessage customerMessage = new CustomerMessage(
                            operationId,
                            setCustomerSourceDefaultWithKey(context, key, sourceId, sourceType)
                    );
                    mUiThreadHandler.sendMessage(
                            mUiThreadHandler.obtainMessage(CUSTOMER_RETRIEVED, customerMessage));
                } catch (StripeException stripeEx) {
                    mUiThreadHandler.sendMessage(
                            mUiThreadHandler.obtainMessage(CUSTOMER_ERROR,
                                    new ExceptionMessage(operationId, stripeEx)));
                    sendErrorIntent(stripeEx);
                }
            }
        };

        executeRunnable(fetchCustomerRunnable);
    }

    private void setCustomerShippingInformation(
            @NonNull final Context context,
            @NonNull final CustomerEphemeralKey key,
            @NonNull final ShippingInformation shippingInformation,
            @Nullable final String operationId) {
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    final CustomerMessage customerMessage = new CustomerMessage(
                            operationId,
                            setCustomerShippingInfoWithKey(context, key, shippingInformation)
                    );
                    mUiThreadHandler.sendMessage(mUiThreadHandler
                            .obtainMessage(CUSTOMER_SHIPPING_INFO_SAVED, customerMessage));
                } catch (StripeException stripeEx) {
                    mUiThreadHandler.sendMessage(
                            mUiThreadHandler.obtainMessage(CUSTOMER_ERROR,
                                    new ExceptionMessage(operationId, stripeEx)));
                    sendErrorIntent(stripeEx);
                }
            }
        };
        executeRunnable(runnable);
    }

    private void updateCustomer(@NonNull final CustomerEphemeralKey key,
                                @Nullable final String operationId) {
        final Runnable fetchCustomerRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    final CustomerMessage customerMessage = new CustomerMessage(
                            operationId,
                            retrieveCustomerWithKey(key)
                    );
                    mUiThreadHandler.sendMessage(
                            mUiThreadHandler.obtainMessage(CUSTOMER_RETRIEVED, customerMessage));
                } catch (StripeException stripeEx) {
                    mUiThreadHandler.sendMessage(
                            mUiThreadHandler.obtainMessage(CUSTOMER_ERROR,
                                    new ExceptionMessage(operationId, stripeEx)));
                }
            }
        };

        executeRunnable(fetchCustomerRunnable);
    }

    private void executeRunnable(@NonNull Runnable runnable) {
        // In automation, run on the main thread.
        if (mStripeApiProxy != null) {
            runnable.run();
            return;
        }

        mThreadPoolExecutor.execute(runnable);
    }

    @Override
    public void onKeyUpdate(
            @NonNull CustomerEphemeralKey ephemeralKey,
            @Nullable String operationId,
            @Nullable String actionString,
            @Nullable Map<String, Object> arguments) {
        if (actionString == null) {
            updateCustomer(ephemeralKey, operationId);
            return;
        }

        if (arguments == null || mContext == null) {
            return;
        }

        if (ACTION_ADD_SOURCE.equals(actionString) && arguments.containsKey(KEY_SOURCE) &&
                arguments.containsKey(KEY_SOURCE_TYPE)) {
            addCustomerSource(mContext,
                    ephemeralKey,
                    (String) arguments.get(KEY_SOURCE),
                    (String) arguments.get(KEY_SOURCE_TYPE),
                    operationId
            );
            resetUsageTokens();
        } else if (ACTION_DELETE_SOURCE.equals(actionString) &&
                arguments.containsKey(KEY_SOURCE)) {
            deleteCustomerSource(mContext,
                    ephemeralKey,
                    (String) arguments.get(KEY_SOURCE),
                    operationId);
            resetUsageTokens();
        } else if (ACTION_SET_DEFAULT_SOURCE.equals(actionString)
                && arguments.containsKey(KEY_SOURCE) && arguments.containsKey(KEY_SOURCE_TYPE)) {
            setCustomerSourceDefault(mContext,
                    ephemeralKey,
                    (String) arguments.get(KEY_SOURCE),
                    (String) arguments.get(KEY_SOURCE_TYPE),
                    operationId);
            resetUsageTokens();
        } else if (ACTION_SET_CUSTOMER_SHIPPING_INFO.equals(actionString) &&
                arguments.containsKey(KEY_SHIPPING_INFO)) {
            setCustomerShippingInformation(mContext,
                    ephemeralKey,
                    (ShippingInformation) arguments.get(KEY_SHIPPING_INFO),
                    operationId);
            resetUsageTokens();
        }
    }

    @Override
    public void onKeyError(@Nullable String operationId, int httpCode,
                           @Nullable String errorMessage) {
        // Any error eliminates all listeners
        final CustomerRetrievalListener customerRetrievalListener =
                getCustomerRetrievalListener(operationId);
        if (customerRetrievalListener != null) {
            customerRetrievalListener.onError(httpCode, errorMessage, null);
        }

        final SourceRetrievalListener sourceRetrievalListener =
                getSourceRetrievalListener(operationId);
        if (sourceRetrievalListener != null) {
            sourceRetrievalListener.onError(httpCode, errorMessage, null);
        }
    }

    @NonNull
    private Handler createMainThreadHandler() {
        return new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);

                switch (msg.what) {
                    case CUSTOMER_RETRIEVED: {
                        if (msg.obj instanceof CustomerMessage) {
                            final CustomerMessage customerMessage = (CustomerMessage) msg.obj;
                            final String operationId = customerMessage.operationId;
                            mCustomer = customerMessage.customer;
                            mCustomerCacheTime = getCalendarInstance().getTimeInMillis();

                            final CustomerRetrievalListener listener =
                                    getCustomerRetrievalListener(operationId);
                            if (listener != null && mCustomer != null) {
                                listener.onCustomerRetrieved(mCustomer);
                            }
                        }
                        break;
                    }
                    case SOURCE_RETRIEVED: {
                        if (msg.obj instanceof SourceMessage) {
                            final SourceMessage sourceMessage = (SourceMessage) msg.obj;
                            final String operationId = sourceMessage.operationId;

                            final SourceRetrievalListener listener =
                                    getSourceRetrievalListener(operationId);
                            if (listener != null && sourceMessage.source != null) {
                                listener.onSourceRetrieved(sourceMessage.source);
                            }
                        }

                        // Clear our context reference so we don't use a stale one.
                        mContext = null;
                        break;
                    }
                    case CUSTOMER_SHIPPING_INFO_SAVED: {
                        if (mContext != null && msg.obj instanceof CustomerMessage) {
                            final CustomerMessage customerMessage = (CustomerMessage) msg.obj;
                            mCustomer = customerMessage.customer;
                            LocalBroadcastManager.getInstance(mContext)
                                    .sendBroadcast(new Intent(EVENT_SHIPPING_INFO_SAVED));
                        }
                        break;
                    }
                    case CUSTOMER_ERROR: {
                        if (msg.obj instanceof ExceptionMessage) {
                            final ExceptionMessage exceptionMessage = (ExceptionMessage) msg.obj;
                            handleRetrievalError(exceptionMessage.operationId,
                                    exceptionMessage.exception, CUSTOMER_ERROR);
                        }
                        break;
                    }
                    case SOURCE_ERROR: {
                        if (msg.obj instanceof ExceptionMessage) {
                            final ExceptionMessage exceptionMessage = (ExceptionMessage) msg.obj;
                            handleRetrievalError(exceptionMessage.operationId,
                                    exceptionMessage.exception, SOURCE_ERROR);
                        }

                        break;
                    }
                    default: {
                        break;
                    }
                }
            }
        };
    }

    private void handleRetrievalError(@Nullable String operationId,
                                      @NonNull StripeException exception,
                                      int errorType) {
        final WeakReference<? extends RetrievalListener> listenerRef;
        if (errorType == SOURCE_ERROR) {
            listenerRef = mSourceRetrievalListenerRefs.remove(operationId);
        } else if (errorType == CUSTOMER_ERROR) {
            listenerRef = mCustomerRetrievalListenerRefs.remove(operationId);
        } else {
            listenerRef = null;
        }

        final RetrievalListener listener = listenerRef != null ? listenerRef.get() : null;
        if (listener != null) {
            final int errorCode = exception.getStatusCode() == null
                    ? 400
                    : exception.getStatusCode();
            listener.onError(errorCode,
                    exception.getLocalizedMessage(),
                    exception.getStripeError());
        }

        resetUsageTokens();
    }

    @NonNull
    private ThreadPoolExecutor createThreadPoolExecutor() {
        return new ThreadPoolExecutor(
                THREAD_POOL_SIZE,
                THREAD_POOL_SIZE,
                KEEP_ALIVE_TIME,
                KEEP_ALIVE_TIME_UNIT,
                mNetworkQueue);
    }

    @NonNull
    private Calendar getCalendarInstance() {
        return mProxyNowCalendar == null ? Calendar.getInstance() : mProxyNowCalendar;
    }

    @Nullable
    private Source addCustomerSourceWithKey(
            @NonNull Context context,
            @NonNull CustomerEphemeralKey key,
            @NonNull String sourceId,
            @NonNull @Source.SourceType String sourceType) throws StripeException {
        if (mStripeApiProxy != null) {
            return mStripeApiProxy.addCustomerSourceWithKey(
                    context,
                    key.getCustomerId(),
                    PaymentConfiguration.getInstance().getPublishableKey(),
                    new ArrayList<>(mProductUsageTokens),
                    sourceId,
                    sourceType,
                    key.getSecret());
        } else {
            return mApiHandler.addCustomerSource(
                    context,
                    key.getCustomerId(),
                    PaymentConfiguration.getInstance().getPublishableKey(),
                    new ArrayList<>(mProductUsageTokens),
                    sourceId,
                    sourceType,
                    key.getSecret(),
                    null);
        }
    }

    @Nullable
    private Source deleteCustomerSourceWithKey(
            @NonNull Context context,
            @NonNull CustomerEphemeralKey key,
            @NonNull String sourceId) throws StripeException {
        if (mStripeApiProxy != null) {
            return mStripeApiProxy.deleteCustomerSourceWithKey(
                    context,
                    key.getCustomerId(),
                    PaymentConfiguration.getInstance().getPublishableKey(),
                    new ArrayList<>(mProductUsageTokens),
                    sourceId,
                    key.getSecret());
        } else {
            return mApiHandler.deleteCustomerSource(
                    context,
                    key.getCustomerId(),
                    PaymentConfiguration.getInstance().getPublishableKey(),
                    new ArrayList<>(mProductUsageTokens),
                    sourceId,
                    key.getSecret(),
                    null);
        }
    }

    @Nullable
    private Customer setCustomerShippingInfoWithKey(
            @NonNull Context context,
            @NonNull CustomerEphemeralKey key,
            @NonNull ShippingInformation shippingInformation) throws StripeException {
        if (mStripeApiProxy != null) {
            return mStripeApiProxy.setCustomerShippingInfoWithKey(
                    context,
                    key.getCustomerId(),
                    PaymentConfiguration.getInstance().getPublishableKey(),
                    new ArrayList<>(mProductUsageTokens),
                    shippingInformation,
                    key.getSecret());
        } else {
            return mApiHandler.setCustomerShippingInfo(
                    context,
                    key.getCustomerId(),
                    PaymentConfiguration.getInstance().getPublishableKey(),
                    new ArrayList<>(mProductUsageTokens),
                    shippingInformation,
                    key.getSecret(),
                    null);
        }
    }

    @Nullable
    private Customer setCustomerSourceDefaultWithKey(
            @NonNull Context context,
            @NonNull CustomerEphemeralKey key,
            @NonNull String sourceId,
            @NonNull @Source.SourceType String sourceType) throws StripeException {
        if (mStripeApiProxy != null) {
            return mStripeApiProxy.setDefaultCustomerSourceWithKey(
                    context,
                    key.getCustomerId(),
                    PaymentConfiguration.getInstance().getPublishableKey(),
                    new ArrayList<>(mProductUsageTokens),
                    sourceId,
                    sourceType,
                    key.getSecret());
        } else {
            return mApiHandler.setDefaultCustomerSource(
                    context,
                    key.getCustomerId(),
                    PaymentConfiguration.getInstance().getPublishableKey(),
                    new ArrayList<>(mProductUsageTokens),
                    sourceId,
                    sourceType,
                    key.getSecret(),
                    null);
        }
    }

    /**
     * Calls the Stripe API (or a test proxy) to fetch a customer. If the provided key is expired,
     * this method <b>does not</b> update the key.
     * Use {@link #updateCustomer(CustomerEphemeralKey, String)} to validate the key
     * before refreshing the customer.
     *
     * @param key the {@link CustomerEphemeralKey} used for this access
     * @return a {@link Customer} if one can be found with this key, or {@code null} if one cannot.
     */
    @Nullable
    private Customer retrieveCustomerWithKey(@NonNull CustomerEphemeralKey key)
            throws StripeException {
        if (mStripeApiProxy != null) {
            return mStripeApiProxy.retrieveCustomerWithKey(key.getCustomerId(), key.getSecret());
        } else {
            return mApiHandler.retrieveCustomer(key.getCustomerId(), key.getSecret());
        }
    }

    private void sendErrorIntent(@NonNull StripeException exception) {
        if (mContext == null) {
            return;
        }
        final Bundle bundle = new Bundle();
        bundle.putSerializable(EXTRA_EXCEPTION, exception);
        final Intent intent = new Intent(ACTION_API_EXCEPTION)
                .putExtras(bundle);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }

    @Nullable
    private CustomerRetrievalListener getCustomerRetrievalListener(@Nullable String operationId) {
        final Reference<CustomerRetrievalListener> listenerRef =
                mCustomerRetrievalListenerRefs.remove(operationId);
        return listenerRef != null ? listenerRef.get() : null;
    }

    @Nullable
    private SourceRetrievalListener getSourceRetrievalListener(@Nullable String operationId) {
        final Reference<SourceRetrievalListener> listenerRef =
                mSourceRetrievalListenerRefs.remove(operationId);
        return listenerRef != null ? listenerRef.get() : null;
    }

    public interface CustomerRetrievalListener extends RetrievalListener {
        void onCustomerRetrieved(@NonNull Customer customer);
    }

    public interface SourceRetrievalListener extends RetrievalListener {
        void onSourceRetrieved(@NonNull Source source);
    }

    interface RetrievalListener {
        void onError(int errorCode, @Nullable String errorMessage,
                     @Nullable StripeError stripeError);
    }

    interface StripeApiProxy {
        @Nullable Customer retrieveCustomerWithKey(@NonNull String customerId,
                                                   @NonNull String secret)
                throws InvalidRequestException, APIConnectionException, APIException;

        @Nullable Source addCustomerSourceWithKey(
                @Nullable Context context,
                @NonNull String customerId,
                @NonNull String publicKey,
                @NonNull List<String> productUsageTokens,
                @NonNull String sourceId,
                @NonNull String sourceType,
                @NonNull String secret)
                throws InvalidRequestException, APIConnectionException, APIException;

        @Nullable Source deleteCustomerSourceWithKey(
                @Nullable Context context,
                @NonNull String customerId,
                @NonNull String publicKey,
                @NonNull List<String> productUsageTokens,
                @NonNull String sourceId,
                @NonNull String secret)
                throws InvalidRequestException, APIConnectionException, APIException;

        @Nullable Customer setDefaultCustomerSourceWithKey(
                @Nullable Context context,
                @NonNull String customerId,
                @NonNull String publicKey,
                @NonNull List<String> productUsageTokens,
                @NonNull String sourceId,
                @NonNull String sourceType,
                @NonNull String secret)
                throws InvalidRequestException, APIConnectionException, APIException;


        @Nullable Customer setCustomerShippingInfoWithKey(
                @Nullable Context context,
                @NonNull String customerId,
                @NonNull String publicKey,
                @NonNull List<String> productUsageTokens,
                @NonNull ShippingInformation shippingInformation,
                @NonNull String secret)
                throws InvalidRequestException, APIConnectionException, APIException;
    }

    private static class CustomerMessage {
        @Nullable private final String operationId;
        @Nullable private final Customer customer;

        private CustomerMessage(@Nullable String operationId, @Nullable Customer customer) {
            this.operationId = operationId;
            this.customer = customer;
        }
    }

    private static class SourceMessage {
        @Nullable private final String operationId;
        @Nullable private final Source source;

        private SourceMessage(@Nullable String operationId, @Nullable Source source) {
            this.operationId = operationId;
            this.source = source;
        }
    }

    private static class ExceptionMessage {
        @Nullable private final String operationId;
        @NonNull private final StripeException exception;

        private ExceptionMessage(@Nullable String operationId, @NonNull StripeException exception) {
            this.operationId = operationId;
            this.exception = exception;
        }
    }
}
