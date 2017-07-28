package com.stripe.android;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import com.stripe.android.exception.APIConnectionException;
import com.stripe.android.exception.APIException;
import com.stripe.android.exception.InvalidRequestException;
import com.stripe.android.exception.StripeException;
import com.stripe.android.model.Customer;

import java.util.Calendar;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Represents a logged-in session of a single Customer.
 */
public class CustomerSession implements EphemeralKeyManager.KeyManagerListener {

    private @Nullable Customer mCustomer;
    private long mCustomerCacheTime;
    private @Nullable CustomerRetrievalListener mCustomerRetrievalListener;

    private @Nullable EphemeralKey mEphemeralKey;
    private @NonNull EphemeralKeyManager mEphemeralKeyManager;

    private @NonNull Handler mUiThreadHandler;

    private @Nullable Calendar mProxyNowCalendar;
    private @Nullable StripeApiProxy mStripeApiProxy;

    // A queue of Runnables for doing customer updates
    private final BlockingQueue<Runnable> mNetworkQueue = new LinkedBlockingQueue<>();

    private @NonNull ThreadPoolExecutor mThreadPoolExecutor;

    private static final int CUSTOMER_RETRIEVED = 7;

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
     * @param keyProvider an {@link EphemeralKeyProvider} used to get
     * {@link EphemeralKey EphemeralKeys} as needed
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
    public static CustomerSession getInstance() {
        if (mInstance == null) {
            throw new IllegalStateException(
                    "Attempted to get instance of CustomerSession without initialization.");
        }
        return mInstance;
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
        mInstance = null;
    }

    private CustomerSession(
            @NonNull EphemeralKeyProvider keyProvider,
            @Nullable StripeApiProxy stripeApiProxy,
            @Nullable Calendar proxyNowCalendar) {
        mThreadPoolExecutor = createThreadPoolExecutor();
        mUiThreadHandler = createMainThreadHandler();
        mStripeApiProxy = stripeApiProxy;
        mProxyNowCalendar = proxyNowCalendar;

        mEphemeralKeyManager = new EphemeralKeyManager(
                keyProvider,
                this,
                KEY_REFRESH_BUFFER_IN_SECONDS,
                proxyNowCalendar);
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
        long currentTime = getCalendarInstance().getTimeInMillis();
        if (mCustomer == null ||
                currentTime - mCustomerCacheTime > CUSTOMER_CACHE_DURATION_MILLISECONDS) {
            mCustomer = null;
            mCustomerRetrievalListener = listener;
            mEphemeralKeyManager.retrieveEphemeralKey();
        } else {
            listener.onCustomerRetrieved(mCustomer);
        }
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

    private void updateCustomer(@NonNull final EphemeralKey key) {
        Runnable fetchCustomerRunnable = new Runnable() {
            @Override
            public void run() {
                Customer customer = retrieveCustomerWithKey(key, mStripeApiProxy);
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
    public void onKeyUpdate(@Nullable EphemeralKey ephemeralKey) {
        mEphemeralKey = ephemeralKey;
        if (mEphemeralKey != null) {
            updateCustomer(mEphemeralKey);
        }
    }

    @Override
    public void onKeyError(int errorCode, @Nullable String errorMessage) {
        if (mCustomerRetrievalListener != null) {
            mCustomerRetrievalListener.onError(errorCode, errorMessage);
            // Only keep the customer listener object for one server round trip
            mCustomerRetrievalListener = null;
        }
    }

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
                            if (mCustomerRetrievalListener != null) {
                                mCustomerRetrievalListener.onCustomerRetrieved(mCustomer);
                                // Eliminate reference to retrival listener after use.
                                mCustomerRetrievalListener = null;
                            }
                        }
                        break;
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

    /**
     * Calls the Stripe API (or a test proxy) to fetch a customer. If the provided key is expired,
     * this method <b>does not</b> update the key.
     * Use {@link #updateCustomer(EphemeralKey)} to validate the key
     * before refreshing the customer.
     *
     * @param key the {@link EphemeralKey} used for this access
     * @param proxy a {@link StripeApiProxy} to intercept calls to the real servers
     * @return a {@link Customer} if one can be found with this key, or {@code null} if one cannot.
     */
    @Nullable
    static Customer retrieveCustomerWithKey(
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
        } catch (InvalidRequestException invalidException) {
            // Then the key is invalid
        } catch (StripeException stripeException) {
            Log.e(CustomerSession.class.getName(), stripeException.getMessage());
        }
        return customer;
    }

    public interface CustomerRetrievalListener {
        void onCustomerRetrieved(@NonNull Customer customer);
        void onError(int errorCode, @Nullable String errorMessage);
    }

    interface StripeApiProxy {
        Customer retrieveCustomerWithKey(@NonNull String customerId, @NonNull String secret)
                throws InvalidRequestException, APIConnectionException, APIException;
    }
}
