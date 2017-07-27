package com.stripe.android;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import com.stripe.android.exception.APIConnectionException;
import com.stripe.android.exception.APIException;
import com.stripe.android.exception.InvalidRequestException;
import com.stripe.android.exception.StripeException;
import com.stripe.android.model.Customer;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Represents a logged-in session of a single Customer.
 */
public class CustomerSession implements Parcelable {

    private @NonNull Customer mCustomer;
    private @NonNull EphemeralKey mEphemeralKey;
    private @NonNull EphemeralKeyProvider mKeyProvider;

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

    private CustomerSession(Parcel in) {
        ClassLoader keyProviderLoader =
                PaymentConfiguration.getInstance().getEphemeralKeyProviderClassLoader();
        if (keyProviderLoader == null) {
            throw new IllegalStateException("Cannot create CustomerSession objects without " +
                    "a KeyProvider with proper ClassLoader");
        }

        // Initialize Threads and Handlers
        initializeMainThreadHandler();
        initializeThreadPoolExecutor();

        mKeyProvider = in.readParcelable(keyProviderLoader);

        // The Ephemeral Key class is part of this package, so we use this classloader.
        mEphemeralKey = in.readParcelable(getClass().getClassLoader());

        Customer customer = Customer.fromString(in.readString());
        mCustomer = customer == null ? Customer.getEmptyCustomer() : customer;
    }

    /**
     * Create a CustomerSession with the provided {@link EphemeralKeyProvider}.
     *
     * @param keyProvider an {@link EphemeralKeyProvider} used to get
     * {@link EphemeralKey EphemeralKeys} as needed
     */
    public CustomerSession(@NonNull EphemeralKeyProvider keyProvider) {
        this(keyProvider, null, null);
    }

    @VisibleForTesting
    CustomerSession(
            @NonNull EphemeralKeyProvider keyProvider,
            @Nullable StripeApiProxy stripeApiProxy,
            @Nullable Calendar proxyNowCalendar) {
        mKeyProvider = keyProvider;
        mEphemeralKey = EphemeralKey.getEmptyKey();
        mCustomer = Customer.getEmptyCustomer();
        mStripeApiProxy = stripeApiProxy;
        mProxyNowCalendar = proxyNowCalendar;
        PaymentConfiguration.getInstance().setEphemeralKeyProviderClassLoader(
                keyProvider.getClassLoader());
        initializeThreadPoolExecutor();
        initializeMainThreadHandler();
        updateKeyIfNecessary(mEphemeralKey, mProxyNowCalendar);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(mKeyProvider, i);
        parcel.writeParcelable(mEphemeralKey, i);
        // Using JSON serialization of the Customer
        parcel.writeString(mCustomer.toString());
    }

    /**
     * Get the currently cached {@link Customer}. Note that this may be an empty
     * customer value (equivalent to {@link Customer#getEmptyCustomer()} if there is no
     * valid current customer.
     *
     * @return the current {@link Customer} object
     */
    @NonNull
    public Customer getCustomer() {
        return mCustomer;
    }

    @NonNull
    @VisibleForTesting
    EphemeralKey getEphemeralKey() {
        return mEphemeralKey;
    }

    void updateCustomerIfNecessary(
            @NonNull final EphemeralKey key,
            @Nullable final Calendar nowCalendar) {
        if (key.getCustomerId().equals(mCustomer.getId())) {
            return;
        }

        Runnable fetchCustomerRunnable = new Runnable() {
            @Override
            public void run() {
                Customer customer = retrieveCustomerWithKey(key, mStripeApiProxy, nowCalendar);
                Message message = mUiThreadHandler.obtainMessage(CUSTOMER_RETRIEVED, customer);
                mUiThreadHandler.sendMessage(message);
            }
        };

        executeRunnable(fetchCustomerRunnable);
    }

    void updateKeyIfNecessary(@NonNull final EphemeralKey key, @Nullable Calendar nowCalendar) {
        if (!isTimeInPast(key.getExpires(), nowCalendar)) {
            return;
        }

        mKeyProvider.fetchEphemeralKey(
                StripeApiHandler.API_VERSION,
                new CustomerKeyUpdateListener(this));
    }

    void executeRunnable(@NonNull Runnable runnable) {

        // In automation, run on the main thread.
        if (mStripeApiProxy != null) {
            runnable.run();
            return;
        }

        mThreadPoolExecutor.execute(runnable);
    }

    void onKeyUpdated(String rawKey) {
        EphemeralKey key = EphemeralKey.fromString(rawKey);
        mEphemeralKey = key == null ? EphemeralKey.getEmptyKey() : key;
        updateCustomerIfNecessary(mEphemeralKey, mProxyNowCalendar);
    }

    void onKeyError(int code, @Nullable String errorMessage) {
        // Not yet handling this case
    }

    private void initializeMainThreadHandler() {
        mUiThreadHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                Object messageObject = msg.obj;

                switch (msg.what) {
                    case CUSTOMER_RETRIEVED:
                        if (messageObject instanceof Customer) {
                            mCustomer = (Customer) messageObject;
                        }
                        break;
                }
            }
        };
    }

    private void initializeThreadPoolExecutor() {
        mThreadPoolExecutor = new ThreadPoolExecutor(
                THREAD_POOL_SIZE,
                THREAD_POOL_SIZE,
                KEEP_ALIVE_TIME,
                KEEP_ALIVE_TIME_UNIT,
                mNetworkQueue);
    }

    /**
     * Calls the Stripe API (or a test proxy) to fetch a customer. If the provided key is expired,
     * this method <b>does not</b> update the key.
     * Use {@link #updateCustomerIfNecessary(EphemeralKey, Calendar)} to validate the key
     * before refreshing the customer.
     *
     * @param key the {@link EphemeralKey} used for this access
     * @param proxy a {@link StripeApiProxy} to intercept calls to the real servers
     * @return a {@link Customer} if one can be found with this key, or {@code null} if one cannot.
     */
    @Nullable
    static Customer retrieveCustomerWithKey(
            @NonNull EphemeralKey key,
            @Nullable StripeApiProxy proxy,
            @Nullable Calendar nowCalendar) {
        Customer customer = null;
        if (isTimeInPast(key.getExpires(), nowCalendar)) {
            return null;
        }

        try {
            if (proxy != null) {
                return proxy.retrieveCustomerWithKey(key.getCustomerId(), key.getSecret());
            }
            customer = StripeApiHandler.retrieveCustomerWithKey(
                    key.getCustomerId(),
                    key.getSecret());
        } catch (InvalidRequestException invalidException) {
            // Then the key is invalid
        } catch (StripeException stripeException) {
            Log.e(CustomerSession.class.getName(), stripeException.getMessage());
        }
        return customer;
    }

    static boolean isTimeInPast(long milliSeconds, @Nullable Calendar nowCalendar) {
        if (milliSeconds == 0L) {
            return true;
        }

        Calendar now = nowCalendar == null ? Calendar.getInstance() : nowCalendar;
        return now.getTimeInMillis() > milliSeconds;
    }

    static final Parcelable.Creator<CustomerSession> CREATOR
            = new Parcelable.Creator<CustomerSession>() {

        @Override
        public CustomerSession createFromParcel(Parcel in) {
            return new CustomerSession(in);
        }

        @Override
        public CustomerSession[] newArray(int size) {
            return new CustomerSession[size];
        }
    };

    private static class CustomerKeyUpdateListener implements EphemeralKeyUpdateListener {

        private @NonNull WeakReference<CustomerSession> mCustomerSessionReference;

        CustomerKeyUpdateListener(@NonNull CustomerSession session) {
            mCustomerSessionReference = new WeakReference<>(session);
        }

        @Override
        public void onKeyUpdate(@NonNull String rawKey) {
            final CustomerSession session = mCustomerSessionReference.get();
            if (session != null) {
                session.onKeyUpdated(rawKey);
            }
        }

        @Override
        public void onKeyUpdateFailure(int responseCode, @Nullable String message) {
            final CustomerSession session = mCustomerSessionReference.get();
            if (session != null) {
                session.onKeyError(responseCode, message);
            }
        }
    }

    interface StripeApiProxy {
        Customer retrieveCustomerWithKey(@NonNull String customerId, @NonNull String secret)
                throws InvalidRequestException, APIConnectionException, APIException;
    }

}
