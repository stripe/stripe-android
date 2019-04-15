package com.stripe.android.view;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import com.stripe.android.CustomerSession;
import com.stripe.android.R;
import com.stripe.android.StripeError;
import com.stripe.android.model.Customer;
import com.stripe.android.model.CustomerSource;
import com.stripe.android.view.i18n.TranslatorManager;

import java.util.List;

import static com.stripe.android.PaymentSession.EXTRA_PAYMENT_SESSION_ACTIVE;
import static com.stripe.android.PaymentSession.TOKEN_PAYMENT_SESSION;

/**
 * An activity that allows a user to select from a customer's available payment methods, or
 * to add new ones.
 */
public class PaymentMethodsActivity extends AppCompatActivity {

    public static final String EXTRA_SELECTED_PAYMENT = "selected_payment";
    static final String EXTRA_PROXY_DELAY = "proxy_delay";
    static final String PAYMENT_METHODS_ACTIVITY = "PaymentMethodsActivity";

    static final int REQUEST_CODE_ADD_CARD = 700;
    private boolean mCommunicating;
    private Customer mCustomer;
    private MaskedCardAdapter mMaskedCardAdapter;
    private ProgressBar mProgressBar;
    private RecyclerView mRecyclerView;
    private boolean mRecyclerViewUpdated;
    private boolean mStartedFromPaymentSession;

    private CustomerSession mCustomerSession;

    /**
     * @deprecated use {@link PaymentMethodsActivityStarter#newIntent()}
     */
    @Deprecated
    @NonNull
    public static Intent newIntent(@NonNull Activity activity) {
        return new PaymentMethodsActivityStarter(activity).newIntent();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_methods);

        mProgressBar = findViewById(R.id.payment_methods_progress_bar);
        mRecyclerView = findViewById(R.id.payment_methods_recycler);
        final View addCardView = findViewById(R.id.payment_methods_add_payment_container);

        mCustomerSession = CustomerSession.getInstance();
        mStartedFromPaymentSession = getIntent().hasExtra(EXTRA_PAYMENT_SESSION_ACTIVE);

        addCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(@NonNull View view) {
                final Intent addSourceIntent = AddSourceActivity.newIntent(
                        PaymentMethodsActivity.this,
                        false,
                        true);
                if (mStartedFromPaymentSession) {
                    addSourceIntent.putExtra(EXTRA_PAYMENT_SESSION_ACTIVE, true);
                }
                startActivityForResult(addSourceIntent, REQUEST_CODE_ADD_CARD);
            }
        });

        final Toolbar toolbar = findViewById(R.id.payment_methods_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        boolean waitForProxy = getIntent().getBooleanExtra(EXTRA_PROXY_DELAY, false);
        if (!waitForProxy) {
            initializeCustomerSourceData();
        }
        // This prevents the first click from being eaten by the focus.
        addCardView.requestFocusFromTouch();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_ADD_CARD && resultCode == RESULT_OK) {
            setCommunicatingProgress(true);
            initLoggingTokens();
            mCustomerSession.updateCurrentCustomer(new AddCardCustomerRetrievalListener(this));
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        final MenuItem saveItem = menu.findItem(R.id.action_save);
        final Drawable compatIcon = ViewUtils.getTintedIconWithAttribute(
                this,
                getTheme(),
                R.attr.titleTextColor,
                R.drawable.ic_checkmark);
        saveItem.setIcon(compatIcon);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.add_source_menu, menu);
        menu.findItem(R.id.action_save).setEnabled(!mCommunicating);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_save) {
            setSelectionAndFinish();
            return true;
        } else {
            final boolean handled = super.onOptionsItemSelected(item);
            if (!handled) {
                onBackPressed();
            }
            return handled;
        }
    }

    @VisibleForTesting
    void initializeCustomerSourceData() {
        final Customer cachedCustomer = mCustomerSession.getCachedCustomer();

        if (cachedCustomer != null) {
            mCustomer = cachedCustomer;
            createListFromCustomerSources();
        } else {
            getCustomerFromSession();
        }
    }

    private void initLoggingTokens() {
        if (mStartedFromPaymentSession) {
            mCustomerSession.addProductUsageTokenIfValid(TOKEN_PAYMENT_SESSION);
        }
        mCustomerSession.addProductUsageTokenIfValid(PAYMENT_METHODS_ACTIVITY);
    }

    /**
     * Update our currently displayed customer data. If the customer only has one
     * {@link CustomerSource} and does not have a {@link Customer#mDefaultSource defaultSource},
     * then set that one source to be the default source before updating displayed source data.
     *
     * @param customer the new {@link Customer} object whose sources should be displayed
     */
    private void updateCustomerAndSetDefaultSourceIfNecessary(@NonNull Customer customer) {
        // An inverted early return - we don't need to talk to the CustomerSession if there is
        // already a default source selected or we have no or more than one customer sources in our
        // list.
        if (!TextUtils.isEmpty(customer.getDefaultSource()) || customer.getSources().size() != 1) {
            updateAdapterWithCustomer(customer);
            return;
        }

        // We only activate this if there is a single source in the list
        final CustomerSource customerSource = customer.getSources().get(0);
        if (customerSource == null || customerSource.getId() == null) {
            // If the source ID is null for the only source we have, then there is nothing
            // we can do but update the display. This should not happen. It is only possible
            // for a CustomerSource to have null ID because a Card is a customer source, and
            // before those are sent to Stripe, they haven't yet been assigned an ID.
            updateAdapterWithCustomer(customer);
            return;
        }

        mCustomerSession.setCustomerDefaultSource(this, customerSource.getId(),
                customerSource.getSourceType(),
                new PostUpdateCustomerRetrievalListener(this));
    }

    private void createListFromCustomerSources() {
        setCommunicatingProgress(false);
        if (mCustomer == null) {
            return;
        }

        final List<CustomerSource> customerSourceList = mCustomer.getSources();
        if (!mRecyclerViewUpdated) {
            mMaskedCardAdapter = new MaskedCardAdapter(customerSourceList);
            // init the RecyclerView
            mRecyclerView.setHasFixedSize(false);
            mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            mRecyclerView.setAdapter(mMaskedCardAdapter);
            mRecyclerViewUpdated = true;
        } else {
            mMaskedCardAdapter.setCustomerSourceList(customerSourceList);
        }

        final String defaultSource = mCustomer.getDefaultSource();
        if (defaultSource != null && !TextUtils.isEmpty(defaultSource)) {
            mMaskedCardAdapter.setSelectedSource(defaultSource);
        }
        mMaskedCardAdapter.notifyDataSetChanged();
    }

    private void cancelAndFinish() {
        setResult(RESULT_CANCELED);
        finish();
    }

    private void finishWithSelection(String selectedSourceId) {
        final CustomerSource customerSource = mCustomer.getSourceById(selectedSourceId);
        if (customerSource != null) {
            Intent intent = new Intent();
            intent.putExtra(EXTRA_SELECTED_PAYMENT, customerSource.toJson().toString());
            setResult(RESULT_OK, intent);
        } else {
            setResult(RESULT_CANCELED);
        }

        finish();
    }

    private void getCustomerFromSession() {
        setCommunicatingProgress(true);
        mCustomerSession.retrieveCurrentCustomer(
                new GetCustomerFromSessionCustomerRetrievalListener(this));
    }

    private void setCommunicatingProgress(boolean communicating) {
        mCommunicating = communicating;
        if (communicating) {
            mProgressBar.setVisibility(View.VISIBLE);
        } else {
            mProgressBar.setVisibility(View.GONE);
        }
        supportInvalidateOptionsMenu();
    }

    private void setSelectionAndFinish() {
        if (mMaskedCardAdapter == null || mMaskedCardAdapter.getSelectedSource() == null) {
            cancelAndFinish();
            return;
        }

        final CustomerSource selectedSource = mMaskedCardAdapter.getSelectedSource();
        if (selectedSource == null || selectedSource.getId() == null) {
            return;
        }
        mCustomerSession.setCustomerDefaultSource(this, selectedSource.getId(),
                selectedSource.getSourceType(), new FinishCustomerRetrievalListener(this));
        setCommunicatingProgress(true);
    }

    private void showError(@NonNull String error) {
        new AlertDialog.Builder(this)
                .setMessage(error)
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .create()
                .show();
    }

    private void updateAdapterWithCustomer(@NonNull Customer customer) {
        if (mMaskedCardAdapter == null) {
            createListFromCustomerSources();
            if (mCustomer == null) {
                return;
            }
        }
        mMaskedCardAdapter.updateCustomer(customer);
        setCommunicatingProgress(false);
    }

    private static final class FinishCustomerRetrievalListener
            extends CustomerSession.ActivityCustomerRetrievalListener<PaymentMethodsActivity> {
        private FinishCustomerRetrievalListener(@NonNull PaymentMethodsActivity activity) {
            super(activity);
        }

        @Override
        public void onCustomerRetrieved(@NonNull Customer customer) {
            final PaymentMethodsActivity activity = getActivity();
            if (activity == null) {
                return;
            }

            activity.mCustomer = customer;
            activity.finishWithSelection(customer.getDefaultSource());
            activity.setCommunicatingProgress(false);
        }

        @Override
        public void onError(int httpCode, @Nullable String errorMessage,
                            @Nullable StripeError stripeError) {
            final PaymentMethodsActivity activity = getActivity();
            if (activity == null) {
                return;
            }

            final String displayedError = TranslatorManager.getErrorMessageTranslator()
                    .translate(httpCode, errorMessage, stripeError);
            activity.showError(displayedError);
            activity.setCommunicatingProgress(false);
        }
    }


    private static final class AddCardCustomerRetrievalListener
            extends CustomerSession.ActivityCustomerRetrievalListener<PaymentMethodsActivity> {
        private AddCardCustomerRetrievalListener(@NonNull PaymentMethodsActivity activity) {
            super(activity);
        }

        @Override
        public void onCustomerRetrieved(@NonNull Customer customer) {
            final PaymentMethodsActivity activity = getActivity();
            if (activity == null) {
                return;
            }

            activity.updateCustomerAndSetDefaultSourceIfNecessary(customer);
        }

        @Override
        public void onError(int httpCode, @Nullable String errorMessage,
                            @Nullable StripeError stripeError) {
            final PaymentMethodsActivity activity = getActivity();
            if (activity == null) {
                return;
            }

            final String displayedError = TranslatorManager
                    .getErrorMessageTranslator()
                    .translate(httpCode, errorMessage, stripeError);
            activity.showError(displayedError);
            activity.setCommunicatingProgress(false);
        }
    }

    private static final class PostUpdateCustomerRetrievalListener
            extends CustomerSession.ActivityCustomerRetrievalListener<PaymentMethodsActivity> {
        private PostUpdateCustomerRetrievalListener(@NonNull PaymentMethodsActivity activity) {
            super(activity);
        }

        @Override
        public void onCustomerRetrieved(@NonNull Customer customer) {
            final PaymentMethodsActivity activity = getActivity();
            if (activity == null) {
                return;
            }

            activity.updateAdapterWithCustomer(customer);
        }

        @Override
        public void onError(int httpCode, @Nullable String errorMessage,
                            @Nullable StripeError stripeError) {
            final PaymentMethodsActivity activity = getActivity();
            if (activity == null) {
                return;
            }

            // Note: if this Activity is changed to subclass StripeActivity,
            // this code will make the error message show twice, since StripeActivity
            // will listen to the broadcast version of the error
            // coming from CustomerSession
            final String displayedError = TranslatorManager.getErrorMessageTranslator()
                    .translate(httpCode, errorMessage, stripeError);
            activity.showError(displayedError);
            activity.setCommunicatingProgress(false);
        }
    }

    private static final class GetCustomerFromSessionCustomerRetrievalListener
            extends CustomerSession.ActivityCustomerRetrievalListener<PaymentMethodsActivity> {
        private GetCustomerFromSessionCustomerRetrievalListener(
                @NonNull PaymentMethodsActivity activity) {
            super(activity);
        }

        @Override
        public void onCustomerRetrieved(@NonNull Customer customer) {
            final PaymentMethodsActivity activity = getActivity();
            if (activity == null) {
                return;
            }

            activity.mCustomer = customer;
            activity.createListFromCustomerSources();
        }

        @Override
        public void onError(int httpCode, @Nullable String errorMessage,
                            @Nullable StripeError stripeError) {
            final PaymentMethodsActivity activity = getActivity();
            if (activity == null) {
                return;
            }

            activity.setCommunicatingProgress(false);
        }
    }
}
