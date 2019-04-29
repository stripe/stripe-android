package com.stripe.android;

import android.app.Activity;
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

import com.stripe.android.exception.StripeException;
import com.stripe.android.model.Customer;
import com.stripe.android.model.PaymentMethod;
import com.stripe.android.model.ShippingInformation;
import com.stripe.android.model.Source;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Represents a logged-in session of a single Customer.
 */
public class CustomerSession {

    public static final String ACTION_API_EXCEPTION = "action_api_exception";
    public static final String EXTRA_EXCEPTION = "exception";

    public static final String EVENT_SHIPPING_INFO_SAVED = "shipping_info_saved";

    private static final String ACTION_ADD_SOURCE = "add_source";
    private static final String ACTION_DELETE_SOURCE = "delete_source";
    private static final String ACTION_ATTACH_PAYMENT_METHOD = "attach_payment_method";
    private static final String ACTION_DETACH_PAYMENT_METHOD = "detach_payment_method";
    private static final String ACTION_SET_DEFAULT_SOURCE = "default_source";
    private static final String ACTION_SET_CUSTOMER_SHIPPING_INFO = "set_shipping_info";
    private static final String KEY_PAYMENT_METHOD = "payment_method";
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
    private static final int PAYMENT_METHOD_RETRIEVED = 15;
    private static final int SOURCE_ERROR = 17;
    private static final int PAYMENT_METHOD_ERROR = 21;
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
    @NonNull private final LocalBroadcastManager mLocalBroadcastManager;
    @NonNull private final Map<String, CustomerRetrievalListener> mCustomerRetrievalListeners =
            new HashMap<>();
    @NonNull private final Map<String, SourceRetrievalListener> mSourceRetrievalListeners =
            new HashMap<>();
    @NonNull private final Map<String, PaymentMethodRetrievalListener>
            mPaymentMethodRetrievalListeners = new HashMap<>();

    @NonNull private final EphemeralKeyManager mEphemeralKeyManager;
    @NonNull private final Handler mUiThreadHandler;
    @NonNull private final Set<String> mProductUsageTokens;
    @Nullable private final Calendar mProxyNowCalendar;
    @NonNull private final ThreadPoolExecutor mThreadPoolExecutor;
    @NonNull private final StripeApiHandler mApiHandler;

    /**
     * Create a CustomerSession with the provided {@link EphemeralKeyProvider}.
     *
     * @param context application context
     * @param keyProvider an {@link EphemeralKeyProvider} used to get
     * {@link CustomerEphemeralKey EphemeralKeys} as needed
     */
    public static void initCustomerSession(@NonNull Context context,
                                           @NonNull EphemeralKeyProvider keyProvider) {
        setInstance(new CustomerSession(context, keyProvider));
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
            mInstance.mCustomerRetrievalListeners.clear();
            mInstance.mSourceRetrievalListeners.clear();
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

    private CustomerSession(@NonNull Context context, @NonNull EphemeralKeyProvider keyProvider) {
        this(context, keyProvider, null, createThreadPoolExecutor(),
                new StripeApiHandler(context));
    }

    @VisibleForTesting
    CustomerSession(
            @NonNull Context context,
            @NonNull EphemeralKeyProvider keyProvider,
            @Nullable Calendar proxyNowCalendar,
            @NonNull ThreadPoolExecutor threadPoolExecutor,
            @NonNull StripeApiHandler apiHandler) {
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(context);
        mThreadPoolExecutor = threadPoolExecutor;
        mProxyNowCalendar = proxyNowCalendar;
        mProductUsageTokens = new HashSet<>();
        mApiHandler = apiHandler;
        mUiThreadHandler = new CustomerSessionHandler(new CustomerSessionHandler.Listener() {
            @Override
            public void onCustomerRetrieved(@Nullable Customer customer,
                                            @Nullable String operationId) {
                mCustomer = customer;
                mCustomerCacheTime = getCalendarInstance().getTimeInMillis();

                final CustomerRetrievalListener listener =
                        getCustomerRetrievalListener(operationId);
                if (listener != null && customer != null) {
                    listener.onCustomerRetrieved(customer);
                }
            }

            @Override
            public void onSourceRetrieved(@Nullable Source source, @Nullable String operationId) {
                final SourceRetrievalListener listener =
                        getSourceRetrievalListener(operationId);
                if (listener != null && source != null) {
                    listener.onSourceRetrieved(source);
                }
            }

            @Override
            public void onPaymentMethodRetrieved(@Nullable PaymentMethod paymentMethod,
                                                 @Nullable String operationId) {
                final PaymentMethodRetrievalListener listener =
                        getPaymentMethodRetrievalListener(operationId);
                if (listener != null && paymentMethod != null) {
                    listener.onPaymentMethodRetrieved(paymentMethod);
                }
            }

            @Override
            public void onCustomerShippingInfoSaved(@Nullable Customer customer) {
                mCustomer = customer;
                mLocalBroadcastManager
                        .sendBroadcast(new Intent(EVENT_SHIPPING_INFO_SAVED));
            }

            @Override
            public void onCustomerError(@NonNull StripeException exception,
                                        @Nullable String operationId) {
                handleRetrievalError(operationId, exception, CUSTOMER_ERROR);
            }

            @Override
            public void onSourceError(@NonNull StripeException exception,
                                      @Nullable String operationId) {
                handleRetrievalError(operationId, exception, SOURCE_ERROR);
            }

            @Override
            public void onPaymentMethodError(@NonNull StripeException exception,
                                             @Nullable String operationId) {
                handleRetrievalError(operationId, exception, PAYMENT_METHOD_ERROR);
            }
        });
        mEphemeralKeyManager = new EphemeralKeyManager<>(
                keyProvider,
                createKeyListener(),
                KEY_REFRESH_BUFFER_IN_SECONDS,
                proxyNowCalendar,
                CustomerEphemeralKey.class);
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
        final Customer cachedCustomer = getCachedCustomer();
        if (cachedCustomer != null) {
            listener.onCustomerRetrieved(cachedCustomer);
        } else {
            mCustomer = null;

            final String operationId = UUID.randomUUID().toString();
            mCustomerRetrievalListeners.put(operationId, listener);
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
        mCustomerRetrievalListeners.put(operationId, listener);
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
     * @param sourceId the ID of the source to be added
     * @param listener a {@link SourceRetrievalListener} to be notified when the api call is
     *                 complete
     */
    public void addCustomerSource(
            @NonNull String sourceId,
            @NonNull @Source.SourceType String sourceType,
            @Nullable SourceRetrievalListener listener) {
        final Map<String, Object> arguments = new HashMap<>();
        arguments.put(KEY_SOURCE, sourceId);
        arguments.put(KEY_SOURCE_TYPE, sourceType);

        final String operationId = UUID.randomUUID().toString();
        if (listener != null) {
            mSourceRetrievalListeners.put(operationId, listener);
        }
        mEphemeralKeyManager.retrieveEphemeralKey(operationId, ACTION_ADD_SOURCE, arguments);
    }

    /**
     * Delete the source from the current customer object.
     * @param sourceId the ID of the source to be deleted
     * @param listener a {@link SourceRetrievalListener} to be notified when the api call is
     *                 complete. The api call will return the removed source.
     */
    public void deleteCustomerSource(
            @NonNull String sourceId,
            @Nullable SourceRetrievalListener listener) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put(KEY_SOURCE, sourceId);

        final String operationId = UUID.randomUUID().toString();
        if (listener != null) {
            mSourceRetrievalListeners.put(operationId, listener);
        }
        mEphemeralKeyManager.retrieveEphemeralKey(operationId, ACTION_DELETE_SOURCE, arguments);
    }

    /**
     * Attaches a PaymentMethod to a Customer.
     *
     * @param paymentMethodId the ID of the payment method to be attached
     * @param listener        a {@link PaymentMethodRetrievalListener} to be notified when the
     *                        api call is complete
     */
    public void attachPaymentMethod(
            @NonNull String paymentMethodId,
            @Nullable PaymentMethodRetrievalListener listener) {
        final Map<String, Object> arguments = new HashMap<>();
        arguments.put(KEY_PAYMENT_METHOD, paymentMethodId);

        final String operationId = UUID.randomUUID().toString();
        if (listener != null) {
            mPaymentMethodRetrievalListeners.put(operationId, listener);
        }
        mEphemeralKeyManager
                .retrieveEphemeralKey(operationId, ACTION_ATTACH_PAYMENT_METHOD, arguments);
    }

    /**
     * Detaches a PaymentMethod from a Customer.
     *
     * @param paymentMethodId the ID of the payment method to be detached
     * @param listener        a {@link PaymentMethodRetrievalListener} to be notified when the
     *                        api call is complete. The api call will return the removed source.
     */
    public void detachPaymentMethod(
            @NonNull String paymentMethodId,
            @Nullable PaymentMethodRetrievalListener listener) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put(KEY_PAYMENT_METHOD, paymentMethodId);

        final String operationId = UUID.randomUUID().toString();
        if (listener != null) {
            mPaymentMethodRetrievalListeners.put(operationId, listener);
        }
        mEphemeralKeyManager
                .retrieveEphemeralKey(operationId, ACTION_DETACH_PAYMENT_METHOD, arguments);
    }

    /**
     * Set the shipping information on the current customer object.
     *
     * @param shippingInformation the data to be set
     */
    public void setCustomerShippingInformation(
            @NonNull ShippingInformation shippingInformation) {
        final Map<String, Object> arguments = new HashMap<>();
        arguments.put(KEY_SHIPPING_INFO, shippingInformation);
        mEphemeralKeyManager.retrieveEphemeralKey(null, ACTION_SET_CUSTOMER_SHIPPING_INFO,
                arguments);
    }

    /**
     * Set the default source of the current customer object.
     *
     * @param sourceId the ID of the source to be set
     * @param listener a {@link CustomerRetrievalListener} to be notified about an update to the
     *                 customer
     */
    public void setCustomerDefaultSource(
            @NonNull String sourceId,
            @NonNull @Source.SourceType String sourceType,
            @Nullable CustomerRetrievalListener listener) {
        final Map<String, Object> arguments = new HashMap<>();
        arguments.put(KEY_SOURCE, sourceId);
        arguments.put(KEY_SOURCE_TYPE, sourceType);

        final String operationId = UUID.randomUUID().toString();
        if (listener != null) {
            mCustomerRetrievalListeners.put(operationId, listener);
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
                            addCustomerSourceWithKey(key, sourceId, sourceType)
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
            @NonNull final CustomerEphemeralKey key,
            @NonNull final String sourceId,
            @Nullable final String operationId) {
        final Runnable fetchCustomerRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    final SourceMessage sourceMessage = new SourceMessage(
                            operationId,
                            deleteCustomerSourceWithKey(key, sourceId)
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

    private void attachPaymentMethod(
            @NonNull final CustomerEphemeralKey key,
            @NonNull final String paymentMethodId,
            @Nullable final String operationId) {
        final Runnable fetchCustomerRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    final PaymentMethodMessage paymentMethodMessage = new PaymentMethodMessage(
                            operationId,
                            attachCustomerPaymentMethodWithKey(key, paymentMethodId)
                    );
                    mUiThreadHandler.sendMessage(mUiThreadHandler
                            .obtainMessage(PAYMENT_METHOD_RETRIEVED, paymentMethodMessage));
                } catch (StripeException stripeEx) {
                    mUiThreadHandler
                            .sendMessage(mUiThreadHandler.obtainMessage(PAYMENT_METHOD_ERROR,
                                    new ExceptionMessage(operationId, stripeEx)));
                    sendErrorIntent(stripeEx);
                }
            }
        };

        executeRunnable(fetchCustomerRunnable);
    }

    private void detachPaymentMethod(
            @NonNull final CustomerEphemeralKey key,
            @NonNull final String paymentMethodId,
            @Nullable final String operationId) {
        final Runnable fetchCustomerRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    final PaymentMethodMessage paymentMethodMessage = new PaymentMethodMessage(
                            operationId,
                            detachCustomerPaymentMethodWithKey(key, paymentMethodId)
                    );
                    mUiThreadHandler.sendMessage(mUiThreadHandler
                            .obtainMessage(PAYMENT_METHOD_RETRIEVED, paymentMethodMessage));

                } catch (StripeException stripeEx) {
                    mUiThreadHandler.sendMessage(mUiThreadHandler
                            .obtainMessage(PAYMENT_METHOD_ERROR,
                                    new ExceptionMessage(operationId, stripeEx)));
                    sendErrorIntent(stripeEx);
                }
            }
        };
        executeRunnable(fetchCustomerRunnable);
    }

    private void setCustomerSourceDefault(
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
                            setCustomerSourceDefaultWithKey(key, sourceId, sourceType)
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
            @NonNull final CustomerEphemeralKey key,
            @NonNull final ShippingInformation shippingInformation,
            @Nullable final String operationId) {
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    final CustomerMessage customerMessage = new CustomerMessage(
                            operationId,
                            setCustomerShippingInfoWithKey(key, shippingInformation)
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
        mThreadPoolExecutor.execute(runnable);
    }

    @NonNull
    private EphemeralKeyManager.KeyManagerListener<CustomerEphemeralKey> createKeyListener() {
        return new EphemeralKeyManager.KeyManagerListener<CustomerEphemeralKey>() {
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

                if (arguments == null) {
                    return;
                }

                if (ACTION_ADD_SOURCE.equals(actionString) && arguments.containsKey(KEY_SOURCE) &&
                        arguments.containsKey(KEY_SOURCE_TYPE)) {
                    addCustomerSource(
                            ephemeralKey,
                            (String) arguments.get(KEY_SOURCE),
                            (String) arguments.get(KEY_SOURCE_TYPE),
                            operationId
                    );
                    resetUsageTokens();
                } else if (ACTION_DELETE_SOURCE.equals(actionString) &&
                        arguments.containsKey(KEY_SOURCE)) {
                    deleteCustomerSource(
                            ephemeralKey,
                            (String) arguments.get(KEY_SOURCE),
                            operationId);
                    resetUsageTokens();
                } else if (ACTION_ATTACH_PAYMENT_METHOD.equals(actionString) &&
                        arguments.containsKey(KEY_PAYMENT_METHOD)) {
                    attachPaymentMethod(
                            ephemeralKey,
                            (String) arguments.get(KEY_PAYMENT_METHOD),
                            operationId
                    );
                    resetUsageTokens();
                } else if (ACTION_DETACH_PAYMENT_METHOD.equals(actionString) &&
                        arguments.containsKey(KEY_PAYMENT_METHOD)) {
                    detachPaymentMethod(
                            ephemeralKey,
                            (String) arguments.get(KEY_PAYMENT_METHOD),
                            operationId);
                    resetUsageTokens();
                } else if (ACTION_SET_DEFAULT_SOURCE.equals(actionString) &&
                        arguments.containsKey(KEY_SOURCE) &&
                        arguments.containsKey(KEY_SOURCE_TYPE)) {
                    setCustomerSourceDefault(
                            ephemeralKey,
                            (String) arguments.get(KEY_SOURCE),
                            (String) arguments.get(KEY_SOURCE_TYPE),
                            operationId);
                    resetUsageTokens();
                } else if (ACTION_SET_CUSTOMER_SHIPPING_INFO.equals(actionString) &&
                        arguments.containsKey(KEY_SHIPPING_INFO)) {
                    setCustomerShippingInformation(
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

                final PaymentMethodRetrievalListener  paymentMethodRetrievalListener =
                        getPaymentMethodRetrievalListener(operationId);
                if (paymentMethodRetrievalListener != null) {
                    paymentMethodRetrievalListener.onError(httpCode, errorMessage, null);
                }
            }
        };
    }

    private void handleRetrievalError(@Nullable String operationId,
                                      @NonNull StripeException exception,
                                      int errorType) {
        final RetrievalListener listener;
        if (errorType == SOURCE_ERROR) {
            listener = mSourceRetrievalListeners.remove(operationId);
        } else if (errorType == CUSTOMER_ERROR) {
            listener = mCustomerRetrievalListeners.remove(operationId);
        } else if (errorType == PAYMENT_METHOD_ERROR) {
            listener = mPaymentMethodRetrievalListeners.remove(operationId);
        } else {
            listener = null;
        }

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
    private Source addCustomerSourceWithKey(
            @NonNull CustomerEphemeralKey key,
            @NonNull String sourceId,
            @NonNull @Source.SourceType String sourceType) throws StripeException {
        return mApiHandler.addCustomerSource(
                key.getCustomerId(),
                PaymentConfiguration.getInstance().getPublishableKey(),
                new ArrayList<>(mProductUsageTokens),
                sourceId,
                sourceType,
                key.getSecret()
        );
    }

    @Nullable
    private Source deleteCustomerSourceWithKey(
            @NonNull CustomerEphemeralKey key,
            @NonNull String sourceId) throws StripeException {
        return mApiHandler.deleteCustomerSource(
                key.getCustomerId(),
                PaymentConfiguration.getInstance().getPublishableKey(),
                new ArrayList<>(mProductUsageTokens),
                sourceId,
                key.getSecret()
        );
    }

    @Nullable
    private PaymentMethod attachCustomerPaymentMethodWithKey(
            @NonNull CustomerEphemeralKey key,
            @NonNull String paymentMethodId) throws StripeException {
        return mApiHandler.attachPaymentMethod(
                key.getCustomerId(),
                PaymentConfiguration.getInstance().getPublishableKey(),
                new ArrayList<>(mProductUsageTokens),
                paymentMethodId,
                key.getSecret()
        );
    }

    @Nullable
    private PaymentMethod detachCustomerPaymentMethodWithKey(
            @NonNull CustomerEphemeralKey key,
            @NonNull String paymentMethodId) throws StripeException {
        return mApiHandler.detachPaymentMethod(
                PaymentConfiguration.getInstance().getPublishableKey(),
                new ArrayList<>(mProductUsageTokens),
                paymentMethodId,
                key.getSecret()
        );
    }

    @Nullable
    private Customer setCustomerShippingInfoWithKey(
            @NonNull CustomerEphemeralKey key,
            @NonNull ShippingInformation shippingInformation) throws StripeException {
        return mApiHandler.setCustomerShippingInfo(
                key.getCustomerId(),
                PaymentConfiguration.getInstance().getPublishableKey(),
                new ArrayList<>(mProductUsageTokens),
                shippingInformation,
                key.getSecret()
        );
    }

    @Nullable
    private Customer setCustomerSourceDefaultWithKey(
            @NonNull CustomerEphemeralKey key,
            @NonNull String sourceId,
            @NonNull @Source.SourceType String sourceType) throws StripeException {
        return mApiHandler.setDefaultCustomerSource(
                key.getCustomerId(),
                PaymentConfiguration.getInstance().getPublishableKey(),
                new ArrayList<>(mProductUsageTokens),
                sourceId,
                sourceType,
                key.getSecret()
        );
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
        return mApiHandler.retrieveCustomer(key.getCustomerId(), key.getSecret());
    }

    private void sendErrorIntent(@NonNull StripeException exception) {
        final Bundle bundle = new Bundle();
        bundle.putSerializable(EXTRA_EXCEPTION, exception);
        final Intent intent = new Intent(ACTION_API_EXCEPTION)
                .putExtras(bundle);
        mLocalBroadcastManager.sendBroadcast(intent);
    }

    @Nullable
    private CustomerRetrievalListener getCustomerRetrievalListener(@Nullable String operationId) {
        return mCustomerRetrievalListeners.remove(operationId);
    }

    @Nullable
    private SourceRetrievalListener getSourceRetrievalListener(@Nullable String operationId) {
        return mSourceRetrievalListeners.remove(operationId);
    }

    @Nullable
    private PaymentMethodRetrievalListener getPaymentMethodRetrievalListener(
            @Nullable String operationId) {
        return mPaymentMethodRetrievalListeners.remove(operationId);
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

    interface RetrievalListener {
        void onError(int errorCode, @Nullable String errorMessage,
                     @Nullable StripeError stripeError);
    }

    /**
     * Abstract implementation of {@link SourceRetrievalListener} that holds a
     * {@link WeakReference} to an {@link Activity} object.
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
     * {@link WeakReference} to an {@link Activity} object.
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

    private static class PaymentMethodMessage {
        @Nullable private final String operationId;
        @Nullable private final PaymentMethod paymentMethod;

        private PaymentMethodMessage(@Nullable String operationId,
                                     @Nullable PaymentMethod paymentMethod) {
            this.operationId = operationId;
            this.paymentMethod = paymentMethod;
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

    private static final class CustomerSessionHandler extends Handler {
        @NonNull private final Listener mListener;

        CustomerSessionHandler(@NonNull Listener listener) {
            super(Looper.getMainLooper());
            mListener = listener;
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case CUSTOMER_RETRIEVED: {
                    if (msg.obj instanceof CustomerMessage) {
                        final CustomerMessage customerMessage = (CustomerMessage) msg.obj;
                        mListener.onCustomerRetrieved(customerMessage.customer,
                                customerMessage.operationId);
                    }
                    break;
                }
                case SOURCE_RETRIEVED: {
                    if (msg.obj instanceof SourceMessage) {
                        final SourceMessage sourceMessage = (SourceMessage) msg.obj;
                        mListener.onSourceRetrieved(sourceMessage.source,
                                sourceMessage.operationId);
                    }

                    break;
                }
                case PAYMENT_METHOD_RETRIEVED: {
                    if (msg.obj instanceof PaymentMethodMessage) {
                        final PaymentMethodMessage paymentMethodMessage =
                                (PaymentMethodMessage) msg.obj;
                        mListener.onPaymentMethodRetrieved(paymentMethodMessage.paymentMethod,
                                paymentMethodMessage.operationId);
                    }

                    break;
                }
                case CUSTOMER_SHIPPING_INFO_SAVED: {
                    if (msg.obj instanceof CustomerMessage) {
                        final CustomerMessage customerMessage = (CustomerMessage) msg.obj;
                        mListener.onCustomerShippingInfoSaved(customerMessage.customer);
                    }
                    break;
                }
                case CUSTOMER_ERROR: {
                    if (msg.obj instanceof ExceptionMessage) {
                        final ExceptionMessage exceptionMessage = (ExceptionMessage) msg.obj;
                        mListener.onCustomerError(exceptionMessage.exception,
                                exceptionMessage.operationId);
                    }
                    break;
                }
                case SOURCE_ERROR: {
                    if (msg.obj instanceof ExceptionMessage) {
                        final ExceptionMessage exceptionMessage = (ExceptionMessage) msg.obj;
                        mListener.onSourceError(exceptionMessage.exception,
                                exceptionMessage.operationId);
                    }

                    break;
                }
                case PAYMENT_METHOD_ERROR: {
                    if (msg.obj instanceof ExceptionMessage) {
                        final ExceptionMessage exceptionMessage = (ExceptionMessage) msg.obj;
                        mListener.onPaymentMethodError(exceptionMessage.exception,
                                exceptionMessage.operationId);
                    }

                    break;
                }
                default: {
                    break;
                }
            }
        }

        interface Listener {
            void onCustomerRetrieved(@Nullable Customer customer, @Nullable String operationId);

            void onSourceRetrieved(@Nullable Source source, @Nullable String operationId);

            void onPaymentMethodRetrieved(@Nullable PaymentMethod paymentMethod,
                                          @Nullable String operationId);

            void onCustomerShippingInfoSaved(@Nullable Customer customer);

            void onCustomerError(@NonNull StripeException exception, @Nullable String operationId);

            void onSourceError(@NonNull StripeException exception, @Nullable String operationId);

            void onPaymentMethodError(@NonNull StripeException exception,
                                      @Nullable String operationId);
        }
    }
}
