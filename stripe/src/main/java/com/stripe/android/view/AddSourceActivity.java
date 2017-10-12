package com.stripe.android.view;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.FrameLayout;

import com.stripe.android.CustomerSession;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.R;
import com.stripe.android.SourceCallback;
import com.stripe.android.Stripe;
import com.stripe.android.model.Card;
import com.stripe.android.model.Source;
import com.stripe.android.model.SourceParams;
import com.stripe.android.model.StripePaymentSource;

import static com.stripe.android.CustomerSession.ACTION_API_EXCEPTION;
import static com.stripe.android.CustomerSession.EVENT_API_ERROR;
import static com.stripe.android.CustomerSession.EVENT_SOURCE_RETRIEVED;
import static com.stripe.android.CustomerSession.EXTRA_CUSTOMER_SOURCE_RETRIEVED;
import static com.stripe.android.CustomerSession.EXTRA_ERROR_MESSAGE;
import static com.stripe.android.PaymentSession.EXTRA_PAYMENT_SESSION_ACTIVE;
import static com.stripe.android.PaymentSession.TOKEN_PAYMENT_SESSION;

/**
 * Activity used to display a {@link CardMultilineWidget} and receive the resulting
 * {@link Source} in the {@link #onActivityResult(int, int, Intent)} of the launching activity.
 */
public class AddSourceActivity extends StripeActivity {

    public static final String EXTRA_NEW_SOURCE = "new_source";
    static final String ADD_SOURCE_ACTIVITY = "AddSourceActivity";
    static final String EXTRA_SHOW_ZIP = "show_zip";
    static final String EXTRA_PROXY_DELAY = "proxy_delay";
    static final String EXTRA_UPDATE_CUSTOMER = "update_customer";

    CardMultilineWidget mCardMultilineWidget;
    CustomerSessionProxy mCustomerSessionProxy;
    FrameLayout mErrorLayout;
    StripeProvider mStripeProvider;

    private @Nullable String mCachedSourceId;
    private BroadcastReceiver mErrorBroadcastReceiver;
    private BroadcastReceiver mSourceUpdateBroadcastReceiver;
    private boolean mStartedFromPaymentSession;
    private boolean mUpdatesCustomer;

    /**
     * Create an {@link Intent} to start a {@link AddSourceActivity}.
     *
     * @param context the {@link Context} used to launch the activity
     * @param requirePostalField {@code true} to require a postal code field
     * @param updatesCustomer {@code true} if the activity should update using an
     * already-initialized {@link CustomerSession}, or {@code false} if it should just
     * return a source.
     * @return an {@link Intent} that can be used to start this activity
     */
    public static Intent newIntent(@NonNull Context context,
                                   boolean requirePostalField,
                                   boolean updatesCustomer) {
        Intent intent = new Intent(context, AddSourceActivity.class);
        intent.putExtra(EXTRA_SHOW_ZIP, requirePostalField);
        intent.putExtra(EXTRA_UPDATE_CUSTOMER, updatesCustomer);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewStub.setLayoutResource(R.layout.activity_add_source);
        mViewStub.inflate();
        mCardMultilineWidget = findViewById(R.id.add_source_card_entry_widget);
        mErrorLayout = findViewById(R.id.add_source_error_container);
        boolean showZip = getIntent().getBooleanExtra(EXTRA_SHOW_ZIP, false);
        mUpdatesCustomer = getIntent().getBooleanExtra(EXTRA_UPDATE_CUSTOMER, false);
        mStartedFromPaymentSession =
                getIntent().getBooleanExtra(EXTRA_PAYMENT_SESSION_ACTIVE, true);
        mCardMultilineWidget.setShouldShowPostalCode(showZip);

        if (mUpdatesCustomer && !getIntent().getBooleanExtra(EXTRA_PROXY_DELAY, false)) {
            initCustomerSessionTokens();
        }

        initializeBroadcastReceivers();
        setTitle(R.string.title_add_a_card);
    }

    private void initializeBroadcastReceivers() {
        mSourceUpdateBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Source receivedSource =
                        Source.fromString(intent.getStringExtra(EXTRA_CUSTOMER_SOURCE_RETRIEVED));
                // Only finish with this source if it is the one we are expecting.
                if (receivedSource != null
                        && mCachedSourceId != null
                        && mCachedSourceId.equals(receivedSource.getId())) {
                    finishWithSource(receivedSource);
                }
            }
        };

        mErrorBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (mCachedSourceId == null) {
                    // Don't listen for random errors that this Activity can't have
                    // created.
                    return;
                }

                String errorMessage = intent.getStringExtra(EXTRA_ERROR_MESSAGE);
                String displayedError = errorMessage == null ? "" : errorMessage;
                setCommunicatingProgress(false);
                showError(displayedError);
            }
        };
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(mErrorBroadcastReceiver);
        LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(mSourceUpdateBroadcastReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mErrorBroadcastReceiver,
                new IntentFilter(EVENT_API_ERROR));
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mSourceUpdateBroadcastReceiver,
                new IntentFilter(EVENT_SOURCE_RETRIEVED));
    }

    @VisibleForTesting
    void initCustomerSessionTokens() {
        logToCustomerSessionIf(ADD_SOURCE_ACTIVITY, mUpdatesCustomer);
        logToCustomerSessionIf(TOKEN_PAYMENT_SESSION, mStartedFromPaymentSession);
    }

    @Override
    protected void onActionSave() {
        Card card = mCardMultilineWidget.getCard();
        if (card == null) {
            // In this case, the error will be displayed on the card widget itself.
            return;
        }

        card.addLoggingToken(ADD_SOURCE_ACTIVITY);
        Stripe stripe = getStripe();
        stripe.setDefaultPublishableKey(PaymentConfiguration.getInstance().getPublishableKey());

        SourceParams sourceParams = SourceParams.createCardParams(card);
        setCommunicatingProgress(true);

        stripe.createSource(sourceParams, new SourceCallback() {
            @Override
            public void onError(Exception error) {
                setCommunicatingProgress(false);
                showError(error.getLocalizedMessage());
            }

            @Override
            public void onSuccess(Source source) {
                if (mUpdatesCustomer) {
                    attachCardToCustomer(source);
                } else {
                    finishWithSource(source);
                }
            }
        });
    }

    private void attachCardToCustomer(StripePaymentSource source) {
        mCachedSourceId = source.getId();
        @Source.SourceType String sourceType;
        if (source instanceof Source) {
            sourceType = ((Source) source).getType();
        } else if (source instanceof Card){
            sourceType = Source.CARD;
        } else {
            // This isn't possible from this activity.
            sourceType = Source.UNKNOWN;
        }
        if (mCustomerSessionProxy == null) {
            CustomerSession.getInstance().addCustomerSource(
                    this,
                    source.getId(),
                    sourceType);
        } else {
            mCustomerSessionProxy.addCustomerSource(
                    this,
                    source.getId(),
                    sourceType);
        }
    }

    private void logToCustomerSessionIf(@NonNull String logToken, boolean condition) {
        if (mCustomerSessionProxy != null) {
            logToProxyIf(logToken, condition);
            return;
        }

        if (condition) {
            CustomerSession.getInstance().addProductUsageTokenIfValid(logToken);
        }
    }

    private void logToProxyIf(@NonNull String logToken, boolean condition) {
        if (mCustomerSessionProxy != null && condition) {
            mCustomerSessionProxy.addProductUsageTokenIfValid(logToken);
        }
    }

    private void finishWithSource(@NonNull Source source) {
        setCommunicatingProgress(false);
        mCachedSourceId = null;
        Intent intent = new Intent();
        intent.putExtra(EXTRA_NEW_SOURCE, source.toString());
        setResult(RESULT_OK, intent);
        finish();
    }

    private Stripe getStripe() {
        if (mStripeProvider == null) {
            return new Stripe(this);
        } else {
            return mStripeProvider.getStripe(this);
        }
    }

    @Override
    protected void setCommunicatingProgress(boolean communicating) {
        super.setCommunicatingProgress(communicating);
        if (mCardMultilineWidget != null) {
            mCardMultilineWidget.setEnabled(!communicating);
        }
    }

    @VisibleForTesting
    void setCustomerSessionProxy(CustomerSessionProxy proxy) {
        mCustomerSessionProxy = proxy;
    }

    @VisibleForTesting
    void setStripeProvider(@NonNull StripeProvider stripeProvider) {
        mStripeProvider = stripeProvider;
    }

    interface StripeProvider {
        Stripe getStripe(@NonNull Context context);
    }

    interface CustomerSessionProxy {
        void addProductUsageTokenIfValid(String token);
        void addCustomerSource(@NonNull Context context,
                               String sourceId,
                               @Source.SourceType String sourceType);
    }
}
