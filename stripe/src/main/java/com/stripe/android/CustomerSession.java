package com.stripe.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.stripe.android.exception.APIConnectionException;
import com.stripe.android.exception.APIException;
import com.stripe.android.exception.InvalidRequestException;
import com.stripe.android.exception.StripeException;
import com.stripe.android.model.Customer;
import com.stripe.android.model.ShippingInformation;
import com.stripe.android.model.Source;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Represents a logged-in session of a single Customer.
 */
public class CustomerSession implements EphemeralKeyManager.KeyManagerListener {

    public static final String ACTION_API_EXCEPTION = "action_api_exception";
    public static final String EXTRA_EXCEPTION = "exception";

    public static final String EVENT_API_ERROR = "api_error";
    public static final String EVENT_CUSTOMER_RETRIEVED = "customer_retrieved";
    public static final String EVENT_SHIPPING_INFO_SAVED = "shipping_info_saved";
    public static final String EVENT_SOURCE_RETRIEVED = "source_retrieved";

    public static final String EXTRA_CUSTOMER_RETRIEVED = "extra_customer";
    public static final String EXTRA_ERROR_CODE = "extra_error_code";
    public static final String EXTRA_ERROR_MESSAGE = "extra_error_message";
    public static final String EXTRA_CUSTOMER_SOURCE_RETRIEVED = "extra_source";

    private static final String ACTION_ADD_SOURCE = "add_source";
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

    private @NonNull QueryableBroadcastReceiver mApiErrorBroadcastReceiver;
    private @Nullable Context mApplicationContext;
    private @Nullable Customer mCustomer;
    private long mCustomerCacheTime;
    private @Nullable CustomerRetrievalListener mCustomerRetrievalListener;
    private @Nullable SourceRetrievalListener mSourceRetrievalListener;

    private @Nullable EphemeralKey mEphemeralKey;
    private @NonNull EphemeralKeyManager mEphemeralKeyManager;

    private @NonNull Handler mUiThreadHandler;

    private @NonNull Set<String> mProductUsageTokens;
    private @Nullable Calendar mProxyNowCalendar;
    private @Nullable StripeApiProxy mStripeApiProxy;

    // A queue of Runnables for doing customer updates
    private final BlockingQueue<Runnable> mNetworkQueue = new LinkedBlockingQueue<>();

    private @NonNull ThreadPoolExecutor mThreadPoolExecutor;

    private static final int CUSTOMER_RETRIEVED = 7;
    private static final int SOURCE_RETRIEVED = 13;
    private static final int CUSTOMER_SHIPPING_INFO_SAVED = 17;

    // The maximum number of active threads we support
    private static final int THREAD_POOL_SIZE = 3;
    // Sets the amount of time an idle thread waits before terminating
    private static final int KEEP_ALIVE_TIME = 2;
    // Sets the Time Unit to seconds
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;

    private static final long KEY_REFRESH_BUFFER_IN_SECONDS = 30L;
    private static final long CUSTOMER_CACHE_DURATION_MILLISECONDS = TimeUnit.MINUTES.toMillis(1);

    private static CustomerSession mInstance;

    /**
     * Create a CustomerSession with the provided {@link EphemeralKeyProvider}.
     *
     * @deprecated Use {@link #initCustomerSession(EphemeralKeyProvider, Context)} to ensure
     * notification of all errors.
     *
     * @param keyProvider an {@link EphemeralKeyProvider} used to get
     * {@link EphemeralKey EphemeralKeys} as needed
     */
    @Deprecated
    public static void initCustomerSession(@NonNull EphemeralKeyProvider keyProvider) {
        initCustomerSession(keyProvider, null, null, null);
    }

    /**
     * Create a CustomerSession with the provided {@link EphemeralKeyProvider}.
     *
     * @param keyProvider an {@link EphemeralKeyProvider} used to get
     * {@link EphemeralKey EphemeralKeys} as needed
     * @param context a {@link Context} whose application-level context will
     *                be used to send local broadcasts
     */
    public static void initCustomerSession(
            @NonNull EphemeralKeyProvider keyProvider,
            @NonNull Context context) {
        initCustomerSession(keyProvider, context, null, null);
    }

    /**
     * Gets the singleton instance of {@link CustomerSession}. If the session has not been
     * initialized, this will throw a {@link RuntimeException}.
     *
     * @return the singleton {@link CustomerSession} instance.
     */
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
            @Nullable Context context,
            @Nullable StripeApiProxy stripeApiProxy,
            @Nullable Calendar proxyNowCalendar) {
        mInstance = new CustomerSession(
                keyProvider,
                context,
                stripeApiProxy,
                proxyNowCalendar);
    }

    @VisibleForTesting
    static void clearInstance() {
        if (mInstance == null) {
            return;
        }
        mInstance.mThreadPoolExecutor.shutdownNow();
        mInstance.unregisterBroadcastReceivers();
        mInstance = null;
    }

    private CustomerSession(
            @NonNull EphemeralKeyProvider keyProvider,
            @Nullable Context context,
            @Nullable StripeApiProxy stripeApiProxy,
            @Nullable Calendar proxyNowCalendar) {
        mThreadPoolExecutor = createThreadPoolExecutor();
        mUiThreadHandler = createMainThreadHandler();
        mStripeApiProxy = stripeApiProxy;
        mProxyNowCalendar = proxyNowCalendar;
        mProductUsageTokens = new HashSet<>();
        mEphemeralKeyManager = new EphemeralKeyManager(
                keyProvider,
                this,
                KEY_REFRESH_BUFFER_IN_SECONDS,
                proxyNowCalendar);
        mApiErrorBroadcastReceiver = new QueryableBroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.hasExtra(EXTRA_EXCEPTION)) {
                    StripeException exception = (StripeException)
                            intent.getSerializableExtra(EXTRA_EXCEPTION);
                    if (exception == null) {
                        return;
                    }

                    if (mCustomerRetrievalListener != null) {
                        mCustomerRetrievalListener.onError(
                                exception.getStatusCode(),
                                exception.getLocalizedMessage());
                        mCustomerRetrievalListener = null;
                    }
                    if (mSourceRetrievalListener != null) {
                        mSourceRetrievalListener.onError(
                                exception.getStatusCode(),
                                exception.getLocalizedMessage());
                        mSourceRetrievalListener = null;
                    }
                }
            }
        };

        if (context != null) {
            mApplicationContext = context.getApplicationContext();
            registerBroadcastReceiversIfNecessary();
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void addProductUsageTokenIfValid(String token) {
        if (token != null && VALID_TOKENS.contains(token)) {
            mProductUsageTokens.add(token);
        }
    }

    /**
     * Retrieve the current {@link Customer}. If the cached value at {@link #mCustomer} is not
     * stale, this broadcasts immediately with the cache. If not, it fetches a new value and returns
     * that to the listener.
     *
     * @param context {@link Context} to grab application context from and use for broadcasts
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void retrieveCurrentCustomer(@NonNull Context context) {
        mApplicationContext = context.getApplicationContext();
        Customer cachedCustomer = getCachedCustomer();
        if (cachedCustomer != null) {
            broadcastCustomer(cachedCustomer);
        } else {
            updateCurrentCustomer(context);
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
            mCustomerRetrievalListener = listener;
            mEphemeralKeyManager.retrieveEphemeralKey(null, null);
        }
    }

    /**
     * Force an update of the current customer, regardless of how much time has passed.
     *
     * @param context {@link Context} to grab application context from and use for broadcasts
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void updateCurrentCustomer(@NonNull Context context) {
        mApplicationContext = context.getApplicationContext();
        mCustomer = null;
        mEphemeralKeyManager.retrieveEphemeralKey(null, null);
    }

    /**
     * Force an update of the current customer, regardless of how much time has passed.
     *
     * @param listener a {@link CustomerRetrievalListener} to invoke with the result of getting
     *                 the customer from the server
     */
    public void updateCurrentCustomer(@NonNull CustomerRetrievalListener listener) {
        mCustomer = null;
        mCustomerRetrievalListener = listener;
        mEphemeralKeyManager.retrieveEphemeralKey(null, null);
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
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void addCustomerSource(
            @NonNull Context context,
            @NonNull String sourceId,
            @NonNull @Source.SourceType String sourceType) {
        mApplicationContext = context.getApplicationContext();
        registerBroadcastReceiversIfNecessary();
        Map<String, Object> arguments = new HashMap<>();
        arguments.put(KEY_SOURCE, sourceId);
        arguments.put(KEY_SOURCE_TYPE, sourceType);
        mEphemeralKeyManager.retrieveEphemeralKey(ACTION_ADD_SOURCE, arguments);
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
        mApplicationContext = context.getApplicationContext();
        registerBroadcastReceiversIfNecessary();
        Map<String, Object> arguments = new HashMap<>();
        arguments.put(KEY_SOURCE, sourceId);
        arguments.put(KEY_SOURCE_TYPE, sourceType);
        mSourceRetrievalListener = listener;
        mEphemeralKeyManager.retrieveEphemeralKey(ACTION_ADD_SOURCE, arguments);
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
        mApplicationContext = context.getApplicationContext();
        registerBroadcastReceiversIfNecessary();
        Map<String, Object> arguments = new HashMap<>();
        arguments.put(KEY_SHIPPING_INFO, shippingInformation);
        mEphemeralKeyManager.retrieveEphemeralKey(ACTION_SET_CUSTOMER_SHIPPING_INFO, arguments);
    }

    /**
     * Set the default source of the current customer object.
     *
     * @param context a {@link Context} to use for resources
     * @param sourceId the ID of the source to be set
     */
    public void setCustomerDefaultSource(
            @NonNull Context context,
            @NonNull String sourceId,
            @NonNull @Source.SourceType String sourceType) {
        mApplicationContext = context.getApplicationContext();
        registerBroadcastReceiversIfNecessary();
        Map<String, Object> arguments = new HashMap<>();
        arguments.put(KEY_SOURCE, sourceId);
        arguments.put(KEY_SOURCE_TYPE, sourceType);
        mEphemeralKeyManager.retrieveEphemeralKey(ACTION_SET_DEFAULT_SOURCE, arguments);
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
        mApplicationContext = context.getApplicationContext();
        Map<String, Object> arguments = new HashMap<>();
        arguments.put(KEY_SOURCE, sourceId);
        arguments.put(KEY_SOURCE_TYPE, sourceType);
        mCustomerRetrievalListener = listener;
        mEphemeralKeyManager.retrieveEphemeralKey(ACTION_SET_DEFAULT_SOURCE, arguments);
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

    @Nullable
    @VisibleForTesting
    EphemeralKey getEphemeralKey() {
        return mEphemeralKey;
    }

    @VisibleForTesting
    Set<String> getProductUsageTokens() {
        return mProductUsageTokens;
    }

    @VisibleForTesting
    void setStripeApiProxy(@Nullable StripeApiProxy proxy) {
        mStripeApiProxy = proxy;
    }

    private void addCustomerSource(
            @NonNull final Context applicationContext,
            @NonNull final EphemeralKey key,
            @NonNull final String sourceId,
            @NonNull final String sourceType,
            @NonNull final List<String> productUsageTokens) {
        Runnable fetchCustomerRunnable = new Runnable() {
            @Override
            public void run() {
                Source source = addCustomerSourceWithKey(
                        applicationContext,
                        key,
                        new ArrayList<>(productUsageTokens),
                        sourceId,
                        sourceType,
                        mStripeApiProxy);
                Message message = mUiThreadHandler.obtainMessage(SOURCE_RETRIEVED, source);
                mUiThreadHandler.sendMessage(message);
            }
        };

        executeRunnable(fetchCustomerRunnable);
    }

    private boolean canUseCachedCustomer() {
        long currentTime = getCalendarInstance().getTimeInMillis();
        return mCustomer != null &&
                currentTime - mCustomerCacheTime < CUSTOMER_CACHE_DURATION_MILLISECONDS;
    }

    private void setCustomerSourceDefault(
            @NonNull final Context applicationContext,
            @NonNull final EphemeralKey key,
            @NonNull final String sourceId,
            @NonNull final String sourceType,
            @NonNull final List<String> productUsageTokens) {
        Runnable fetchCustomerRunnable = new Runnable() {
            @Override
            public void run() {
                Customer customer = setCustomerSourceDefaultWithKey(
                        applicationContext,
                        key,
                        new ArrayList<>(productUsageTokens),
                        sourceId,
                        sourceType,
                        mStripeApiProxy);
                Message message = mUiThreadHandler.obtainMessage(CUSTOMER_RETRIEVED, customer);
                mUiThreadHandler.sendMessage(message);
            }
        };

        executeRunnable(fetchCustomerRunnable);
    }

    private void registerBroadcastReceiversIfNecessary() {
        if (mApplicationContext == null || mApiErrorBroadcastReceiver.isRegistered()) {
            return;
        }

        LocalBroadcastManager.getInstance(mApplicationContext)
                .registerReceiver(mApiErrorBroadcastReceiver,
                        new IntentFilter(ACTION_API_EXCEPTION));
        mApiErrorBroadcastReceiver.setRegistered();
    }

    private void setCustomerShippingInformation(
            @NonNull final Context applicationContext,
            @NonNull final EphemeralKey key,
            @NonNull final ShippingInformation shippingInformation,
            @NonNull final List<String> productUsageTokens) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Customer customer = setCustomerShippingInfoWithKey(
                        applicationContext,
                        key,
                        new ArrayList<>(productUsageTokens),
                        shippingInformation,
                        mStripeApiProxy);
                Message message = mUiThreadHandler.obtainMessage(CUSTOMER_SHIPPING_INFO_SAVED,
                        customer);
                mUiThreadHandler.sendMessage(message);
            }
        };
        executeRunnable(runnable);
    }

    private void unregisterBroadcastReceivers() {
        if (mApplicationContext == null) {
            return;
        }
        LocalBroadcastManager.getInstance(mApplicationContext)
                .unregisterReceiver(mApiErrorBroadcastReceiver);
    }

    private void updateCustomer(@NonNull final EphemeralKey key) {
        Runnable fetchCustomerRunnable = new Runnable() {
            @Override
            public void run() {
                Customer customer = retrieveCustomerWithKey(
                        mApplicationContext,
                        key,
                        mStripeApiProxy);
                Message message = mUiThreadHandler.obtainMessage(CUSTOMER_RETRIEVED, customer);
                mUiThreadHandler.sendMessage(message);
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
            @Nullable EphemeralKey ephemeralKey,
            @Nullable String actionString,
            @Nullable Map<String, Object> arguments) {
        mEphemeralKey = ephemeralKey;
        if (mEphemeralKey != null) {
            if (actionString == null) {
                updateCustomer(mEphemeralKey);
            } else if (ACTION_ADD_SOURCE.equals(actionString)
                    && mApplicationContext != null
                    && arguments != null
                    && arguments.containsKey(KEY_SOURCE)
                    && arguments.containsKey(KEY_SOURCE_TYPE)) {
                addCustomerSource(
                        mApplicationContext,
                        mEphemeralKey,
                        (String) arguments.get(KEY_SOURCE),
                        (String) arguments.get(KEY_SOURCE_TYPE),
                        new ArrayList<>(mProductUsageTokens));
                resetUsageTokens();
            } else if (ACTION_SET_DEFAULT_SOURCE.equals(actionString)
                    && mApplicationContext != null
                    && arguments != null
                    && arguments.containsKey(KEY_SOURCE)
                    && arguments.containsKey(KEY_SOURCE_TYPE)) {
                setCustomerSourceDefault(
                        mApplicationContext,
                        mEphemeralKey,
                        (String) arguments.get(KEY_SOURCE),
                        (String) arguments.get(KEY_SOURCE_TYPE),
                        new ArrayList<>(mProductUsageTokens));
                resetUsageTokens();
            } else if (ACTION_SET_CUSTOMER_SHIPPING_INFO.equals(actionString)
                    && mApplicationContext != null
                    && arguments != null
                    && arguments.containsKey(KEY_SHIPPING_INFO)) {
                setCustomerShippingInformation(
                        mApplicationContext,
                        mEphemeralKey,
                        (ShippingInformation) arguments.get(KEY_SHIPPING_INFO),
                        new ArrayList<>(mProductUsageTokens));
                resetUsageTokens();
            }
        }
    }

    @Override
    public void onKeyError(int errorCode, @Nullable String errorMessage) {
        // Broadcast the error for internal classes.
        Intent intent = new Intent(EVENT_API_ERROR);
        intent.putExtra(EXTRA_ERROR_CODE, errorCode);
        intent.putExtra(EXTRA_ERROR_MESSAGE, errorMessage);
        LocalBroadcastManager.getInstance(mApplicationContext)
                .sendBroadcast(intent);
        // Return message to listeners if something goes wrong.
        if (mCustomerRetrievalListener != null) {
            mCustomerRetrievalListener.onError(errorCode, errorMessage);
            mCustomerRetrievalListener = null;
        }

        if (mSourceRetrievalListener != null) {
            mSourceRetrievalListener.onError(errorCode, errorMessage);
            mSourceRetrievalListener = null;
        }
    }

    private void broadcastCustomerSource(@NonNull Source source) {
        if (mApplicationContext == null) {
            return;
        }
        Intent intent = new Intent(EVENT_SOURCE_RETRIEVED);
        intent.putExtra(EXTRA_CUSTOMER_SOURCE_RETRIEVED, source.toString());
        LocalBroadcastManager.getInstance(mApplicationContext)
                .sendBroadcast(intent);

    }

    private void broadcastCustomer(@NonNull Customer customer) {
        if (mApplicationContext == null) {
            return;
        }
        Intent intent = new Intent(EVENT_CUSTOMER_RETRIEVED);
        intent.putExtra(EXTRA_CUSTOMER_RETRIEVED, customer.toString());
        LocalBroadcastManager.getInstance(mApplicationContext).sendBroadcast(intent);
    }

    @SuppressWarnings("checkstyle:MissingSwitchDefault")
    private Handler createMainThreadHandler() {
        return new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                Object messageObject = msg.obj;

                switch (msg.what) {
                    case CUSTOMER_RETRIEVED:
                        if (messageObject instanceof Customer) {
                            mCustomer = (Customer) messageObject;
                            mCustomerCacheTime = getCalendarInstance().getTimeInMillis();
                            // Broadcast for internal listeners.
                            broadcastCustomer(mCustomer);
                            // Also dispatch to any external listeners.
                            if (mCustomerRetrievalListener != null) {
                                mCustomerRetrievalListener.onCustomerRetrieved(mCustomer);
                                // Eliminate reference to retrieval listener after use.
                                mCustomerRetrievalListener = null;
                            }
                        }
                        break;
                    case SOURCE_RETRIEVED:
                        if (messageObject instanceof Source) {
                            Source retrievedSource = (Source) messageObject;
                            broadcastCustomerSource(retrievedSource);
                            if (mSourceRetrievalListener != null) {
                                mSourceRetrievalListener.onSourceRetrieved((Source) messageObject);
                                mSourceRetrievalListener = null;
                            }
                        }
                        break;
                    case CUSTOMER_SHIPPING_INFO_SAVED:
                        if (messageObject instanceof Customer) {
                            mCustomer = (Customer) messageObject;
                            if (mApplicationContext == null) {
                                return;
                            }
                            Intent intent = new Intent(EVENT_SHIPPING_INFO_SAVED);
                            LocalBroadcastManager.getInstance(mApplicationContext)
                                    .sendBroadcast(intent);
                        }
                }
            }
        };
    }

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

    static Source addCustomerSourceWithKey(
            @NonNull Context applicationContext,
            @NonNull EphemeralKey key,
            @NonNull List<String> productUsageTokens,
            @NonNull String sourceId,
            @NonNull @Source.SourceType String sourceType,
            @Nullable StripeApiProxy proxy) {
        Source source = null;
        try {
            if (proxy != null) {
                return proxy.addCustomerSourceWithKey(
                        applicationContext,
                        key.getCustomerId(),
                        PaymentConfiguration.getInstance().getPublishableKey(),
                        productUsageTokens,
                        sourceId,
                        sourceType,
                        key.getSecret());
            }

            source = StripeApiHandler.addCustomerSource(
                    applicationContext,
                    key.getCustomerId(),
                    PaymentConfiguration.getInstance().getPublishableKey(),
                    productUsageTokens,
                    sourceId,
                    sourceType,
                    key.getSecret(),
                    null);
        } catch (StripeException stripeException) {
            Log.e(CustomerSession.class.getName(), stripeException.getMessage());
            LocalBroadcastManager.getInstance(applicationContext)
                    .sendBroadcast(generateErrorIntent(stripeException));
        }
        return source;
    }

    static Customer setCustomerShippingInfoWithKey(
            @NonNull Context applicationContext,
            @NonNull EphemeralKey key,
            @NonNull List<String> productUsageTokens,
            @NonNull ShippingInformation shippingInformation,
            @Nullable StripeApiProxy proxy) {
        Customer customer = null;
        try {
            if (proxy != null) {
                return proxy.setCustomerShippingInfoWithKey(
                       applicationContext,
                        key.getCustomerId(),
                        PaymentConfiguration.getInstance().getPublishableKey(),
                        productUsageTokens,
                        shippingInformation,
                        key.getSecret());
            }
            customer = StripeApiHandler.setCustomerShippingInfo(
                    applicationContext,
                    key.getCustomerId(),
                    PaymentConfiguration.getInstance().getPublishableKey(),
                    productUsageTokens,
                    shippingInformation,
                    key.getSecret(),
                    null);
        } catch (StripeException stripeException) {
            Log.e(CustomerSession.class.getName(), stripeException.getMessage());
            LocalBroadcastManager.getInstance(applicationContext)
                    .sendBroadcast(generateErrorIntent(stripeException));
        }
        return customer;
    }

    static Customer setCustomerSourceDefaultWithKey(
            @NonNull Context applicationContext,
            @NonNull EphemeralKey key,
            @NonNull List<String> productUsageTokens,
            @NonNull String sourceId,
            @NonNull @Source.SourceType String sourceType,
            @Nullable StripeApiProxy proxy) {
        Customer customer = null;
        try {
            if (proxy != null) {
                return proxy.setDefaultCustomerSourceWithKey(
                        applicationContext,
                        key.getCustomerId(),
                        PaymentConfiguration.getInstance().getPublishableKey(),
                        productUsageTokens,
                        sourceId,
                        sourceType,
                        key.getSecret());
            }
            customer = StripeApiHandler.setDefaultCustomerSource(
                    applicationContext,
                    key.getCustomerId(),
                    PaymentConfiguration.getInstance().getPublishableKey(),
                    productUsageTokens,
                    sourceId,
                    sourceType,
                    key.getSecret(),
                    null);
        } catch (StripeException stripeException) {
            Log.e(CustomerSession.class.getName(), stripeException.getMessage());
            LocalBroadcastManager.getInstance(applicationContext)
                    .sendBroadcast(generateErrorIntent(stripeException));
        }
        return customer;
    }

    /**
     * Calls the Stripe API (or a test proxy) to fetch a customer. If the provided key is expired,
     * this method <b>does not</b> update the key.
     * Use {@link #updateCustomer(EphemeralKey)} to validate the key
     * before refreshing the customer.
     *
     * @param applicationContext the application-level {@link Context}
     *                     that can be used for broadcasting errors.
     * @param key the {@link EphemeralKey} used for this access
     * @param proxy a {@link StripeApiProxy} to intercept calls to the real servers
     * @return a {@link Customer} if one can be found with this key, or {@code null} if one cannot.
     */
    @Nullable
    static Customer retrieveCustomerWithKey(
            @Nullable Context applicationContext,
            @NonNull EphemeralKey key,
            @Nullable StripeApiProxy proxy) {
        Customer customer = null;

        try {
            if (proxy != null) {
                return proxy.retrieveCustomerWithKey(key.getCustomerId(), key.getSecret());
            }
            customer = StripeApiHandler.retrieveCustomer(
                    key.getCustomerId(),
                    key.getSecret());
        } catch (StripeException stripeException) {
            Log.e(CustomerSession.class.getName(), stripeException.getMessage());
            if (applicationContext != null) {
                LocalBroadcastManager.getInstance(applicationContext)
                        .sendBroadcast(generateErrorIntent(stripeException));
            }
        }
        return customer;
    }

    @NonNull
    static Intent generateErrorIntent(@NonNull StripeException exception) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(EXTRA_EXCEPTION, exception);
        Intent intent = new Intent(ACTION_API_EXCEPTION);
        intent.putExtras(bundle);
        return intent;
    }

    public interface CustomerRetrievalListener {
        void onCustomerRetrieved(@NonNull Customer customer);
        void onError (int errorCode, @Nullable String errorMessage);
    }

    public interface SourceRetrievalListener {
        void onSourceRetrieved(@NonNull Source source);
        void onError(int errorCode, @Nullable String errorMessage);
    }

    abstract class QueryableBroadcastReceiver extends BroadcastReceiver {
        boolean mIsRegistered;

        QueryableBroadcastReceiver() {
            super();
        }

        void setRegistered() {
            mIsRegistered = true;
        }

        boolean isRegistered() {
            return mIsRegistered;
        }
    }

    interface StripeApiProxy {
        Customer retrieveCustomerWithKey(@NonNull String customerId, @NonNull String secret)
                throws InvalidRequestException, APIConnectionException, APIException;

        Source addCustomerSourceWithKey(
                @Nullable Context context,
                @NonNull String customerId,
                @NonNull String publicKey,
                @NonNull List<String> productUsageTokens,
                @NonNull String sourceId,
                @NonNull String sourceType,
                @NonNull String secret)
                throws InvalidRequestException, APIConnectionException, APIException;

        Customer setDefaultCustomerSourceWithKey(
                @Nullable Context context,
                @NonNull String customerId,
                @NonNull String publicKey,
                @NonNull List<String> productUsageTokens,
                @NonNull String sourceId,
                @NonNull String sourceType,
                @NonNull String secret)
                throws InvalidRequestException, APIConnectionException, APIException;


        Customer setCustomerShippingInfoWithKey(
                @Nullable Context context,
                @NonNull String customerId,
                @NonNull String publicKey,
                @NonNull List<String> productUsageTokens,
                @NonNull ShippingInformation shippingInformation,
                @NonNull String secret)
                throws InvalidRequestException, APIConnectionException, APIException;
    }
}
