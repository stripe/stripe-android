package com.stripe.android.view;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.stripe.android.CustomerSession;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.R;
import com.stripe.android.SourceCallback;
import com.stripe.android.Stripe;
import com.stripe.android.StripeError;
import com.stripe.android.model.Card;
import com.stripe.android.model.Source;
import com.stripe.android.model.SourceParams;
import com.stripe.android.model.StripePaymentSource;

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

    @Nullable private CardMultilineWidget mCardMultilineWidget;
    @Nullable private CustomerSessionProxy mCustomerSessionProxy;
    private FrameLayout mErrorLayout;
    @Nullable private StripeProvider mStripeProvider;

    private boolean mStartedFromPaymentSession;
    private boolean mUpdatesCustomer;

    @NonNull private final TextView.OnEditorActionListener mOnEditorActionListener =
            new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        if (mCardMultilineWidget.getCard() != null) {
                            ((InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE))
                                    .hideSoftInputFromWindow(mViewStub.getWindowToken(), 0);
                        }
                        onActionSave();
                        return true;
                    }
                    return false;
                }
            };

    @NonNull private final SourceCallback mSourceCallback = new SourceCallback() {
        @Override
        public void onError(@NonNull Exception error) {
            setCommunicatingProgress(false);
            // This error is independent of the CustomerSession, so
            // we have to surface it here.
            showError(error.getLocalizedMessage());
        }

        @Override
        public void onSuccess(@NonNull Source source) {
            if (mUpdatesCustomer) {
                attachCardToCustomer(source);
            } else {
                finishWithSource(source);
            }
        }
    };

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
    @NonNull
    public static Intent newIntent(@NonNull Context context,
                                   boolean requirePostalField,
                                   boolean updatesCustomer) {
        return new Intent(context, AddSourceActivity.class)
                .putExtra(EXTRA_SHOW_ZIP, requirePostalField)
                .putExtra(EXTRA_UPDATE_CUSTOMER, updatesCustomer);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewStub.setLayoutResource(R.layout.activity_add_source);
        mViewStub.inflate();
        mCardMultilineWidget = findViewById(R.id.add_source_card_entry_widget);
        initEnterListeners();
        mErrorLayout = findViewById(R.id.add_source_error_container);
        boolean showZip = getIntent().getBooleanExtra(EXTRA_SHOW_ZIP, false);
        mUpdatesCustomer = getIntent().getBooleanExtra(EXTRA_UPDATE_CUSTOMER, false);
        mStartedFromPaymentSession =
                getIntent().getBooleanExtra(EXTRA_PAYMENT_SESSION_ACTIVE, true);
        mCardMultilineWidget.setShouldShowPostalCode(showZip);

        if (mUpdatesCustomer && !getIntent().getBooleanExtra(EXTRA_PROXY_DELAY, false)) {
            initCustomerSessionTokens();
        }

        setTitle(R.string.title_add_a_card);
    }

    private void initEnterListeners() {
        ((TextView) mCardMultilineWidget.findViewById(R.id.et_add_source_card_number_ml))
                .setOnEditorActionListener(mOnEditorActionListener);
        ((TextView) mCardMultilineWidget.findViewById(R.id.et_add_source_expiry_ml))
                .setOnEditorActionListener(mOnEditorActionListener);
        ((TextView) mCardMultilineWidget.findViewById(R.id.et_add_source_cvc_ml))
                .setOnEditorActionListener(mOnEditorActionListener);
        ((TextView) mCardMultilineWidget.findViewById(R.id.et_add_source_postal_ml))
                .setOnEditorActionListener(mOnEditorActionListener);
    }

    @VisibleForTesting
    void initCustomerSessionTokens() {
        logToCustomerSessionIf(ADD_SOURCE_ACTIVITY, mUpdatesCustomer);
        logToCustomerSessionIf(TOKEN_PAYMENT_SESSION, mStartedFromPaymentSession);
    }

    @Override
    protected void onActionSave() {
        final Card card = mCardMultilineWidget.getCard();
        if (card == null) {
            // In this case, the error will be displayed on the card widget itself.
            return;
        }

        card.addLoggingToken(ADD_SOURCE_ACTIVITY);
        final Stripe stripe = getStripe();
        stripe.setDefaultPublishableKey(PaymentConfiguration.getInstance().getPublishableKey());

        final SourceParams sourceParams = SourceParams.createCardParams(card);
        setCommunicatingProgress(true);

        stripe.createSource(sourceParams, mSourceCallback);

    }

    private void attachCardToCustomer(@NonNull final StripePaymentSource source) {
        final CustomerSession.SourceRetrievalListener listener =
                new CustomerSession.SourceRetrievalListener() {
                    @Override
                    public void onSourceRetrieved(@NonNull Source source) {
                        finishWithSource(source);
                    }

                    @Override
                    public void onError(int errorCode, @Nullable String errorMessage,
                                        @Nullable StripeError stripeError) {
                        // No need to show this error, because it will be broadcast
                        // from the CustomerSession
                        setCommunicatingProgress(false);
                    }
                };

        if (mCustomerSessionProxy == null) {
            @Source.SourceType final String sourceType;
            if (source instanceof Source) {
                sourceType = ((Source) source).getType();
            } else if (source instanceof Card){
                sourceType = Source.CARD;
            } else {
                // This isn't possible from this activity.
                sourceType = Source.UNKNOWN;
            }

            CustomerSession.getInstance().addCustomerSource(
                    this,
                    source.getId(),
                    sourceType,
                    listener);
        } else {
            mCustomerSessionProxy.addCustomerSource(source.getId(), listener);
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
        Intent intent = new Intent();
        intent.putExtra(EXTRA_NEW_SOURCE, source.toJson().toString());
        setResult(RESULT_OK, intent);
        finish();
    }

    @NonNull
    private Stripe getStripe() {
        if (mStripeProvider == null) {
            return new Stripe(getApplicationContext());
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
        @NonNull Stripe getStripe(@NonNull Context context);
    }

    interface CustomerSessionProxy {
        void addProductUsageTokenIfValid(@NonNull String token);

        void addCustomerSource(String sourceId,
                               @NonNull CustomerSession.SourceRetrievalListener listener);
    }
}
