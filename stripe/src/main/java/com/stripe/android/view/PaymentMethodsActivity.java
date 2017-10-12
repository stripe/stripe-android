package com.stripe.android.view;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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
import com.stripe.android.model.Customer;
import com.stripe.android.model.CustomerSource;
import com.stripe.android.model.Source;

import java.util.List;

import static com.stripe.android.CustomerSession.EVENT_API_ERROR;
import static com.stripe.android.CustomerSession.EVENT_CUSTOMER_RETRIEVED;
import static com.stripe.android.CustomerSession.EXTRA_CUSTOMER_RETRIEVED;
import static com.stripe.android.CustomerSession.EXTRA_ERROR_MESSAGE;
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
    private BroadcastReceiver mCustomerBroadcastReceiver;
    private CustomerSessionProxy mCustomerSessionProxy;
    private BroadcastReceiver mErrorBroadcastReceiver;
    private boolean mExpectingDefaultUpdate;
    private MaskedCardAdapter mMaskedCardAdapter;
    private int mOutgoingCommunicationAttempts;
    private ProgressBar mProgressBar;
    private RecyclerView mRecyclerView;
    private boolean mRecyclerViewUpdated;
    private boolean mStartedFromPaymentSession;
    private boolean mWaitingToFinish;

    public static Intent newIntent(Context context) {
        return new Intent(context, PaymentMethodsActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_methods);

        mProgressBar = findViewById(R.id.payment_methods_progress_bar);
        mRecyclerView = findViewById(R.id.payment_methods_recycler);
        View addCardView = findViewById(R.id.payment_methods_add_payment_container);

        addCardView.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent addSourceIntent = AddSourceActivity.newIntent(
                                PaymentMethodsActivity.this,
                                false,
                                true);
                        if (mStartedFromPaymentSession) {
                            addSourceIntent.putExtra(EXTRA_PAYMENT_SESSION_ACTIVE, true);
                        }
                        startActivityForResult(addSourceIntent, REQUEST_CODE_ADD_CARD);
                    }
                });

        Toolbar toolbar = findViewById(R.id.payment_methods_toolbar);
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
        mStartedFromPaymentSession = getIntent().hasExtra(EXTRA_PAYMENT_SESSION_ACTIVE);
        initializeBroadcastReceivers();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_ADD_CARD && resultCode == RESULT_OK) {
            updateOutgoingMessages(true);
            initLoggingTokens();
            if (mCustomerSessionProxy == null) {
                CustomerSession.getInstance().updateCurrentCustomer(this);
            } else {
                mCustomerSessionProxy.updateCurrentCustomer(this);
            }
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem saveItem = menu.findItem(R.id.action_save);
        Drawable compatIcon =
                ViewUtils.getTintedIconWithAttribute(
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
            boolean handled = super.onOptionsItemSelected(item);
            if (!handled && mCommunicating) {
                onBackPressed();
            } else {
                setSelectionAndFinish();
                return true;
            }
            return handled;
        }
    }

    @VisibleForTesting
    void initializeCustomerSourceData() {
        Customer cachedCustomer = mCustomerSessionProxy == null
                ? CustomerSession.getInstance().getCachedCustomer()
                : mCustomerSessionProxy.getCachedCustomer();

        if (cachedCustomer != null) {
            mCustomer = cachedCustomer;
            createListFromCustomerSources();
        } else {
            getCustomerFromSession();
        }
    }

    @VisibleForTesting
    void setCustomerSessionProxy(CustomerSessionProxy proxy) {
        mCustomerSessionProxy = proxy;
    }

    private void initializeBroadcastReceivers() {
        mCustomerBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Customer receivedCustomer =
                        Customer.fromString(intent.getStringExtra(EXTRA_CUSTOMER_RETRIEVED));
                if (receivedCustomer == null) {
                    return;
                }
                mCustomer = receivedCustomer;
                updateOutgoingMessages(false);
                // Only finish with this source if it is the one we are expecting.
                if (mExpectingDefaultUpdate) {
                    mExpectingDefaultUpdate = false;
                    updateAdapterWithCustomer(mCustomer);
                } else if (mWaitingToFinish) {
                    finishWithSelection(mCustomer.getDefaultSource());
                } else {
                    updateCustomerAndSetDefaultSourceIfNecessary(mCustomer);
                }
            }
        };

        mErrorBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateOutgoingMessages(false);
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
                .unregisterReceiver(mCustomerBroadcastReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mErrorBroadcastReceiver,
                new IntentFilter(EVENT_API_ERROR));
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mCustomerBroadcastReceiver,
                new IntentFilter(EVENT_CUSTOMER_RETRIEVED));
    }

    private void initLoggingTokens() {
        if (mCustomerSessionProxy == null) {
            if (mStartedFromPaymentSession) {
                CustomerSession.getInstance().addProductUsageTokenIfValid(TOKEN_PAYMENT_SESSION);
            }
            CustomerSession.getInstance().addProductUsageTokenIfValid(PAYMENT_METHODS_ACTIVITY);
        } else {
            if (mStartedFromPaymentSession) {
                mCustomerSessionProxy.addProductUsageTokenIfValid(TOKEN_PAYMENT_SESSION);
            }
            mCustomerSessionProxy.addProductUsageTokenIfValid(PAYMENT_METHODS_ACTIVITY);
        }
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
        if (!TextUtils.isEmpty(customer.getDefaultSource())
                || customer.getSources().size() != 1) {
            updateAdapterWithCustomer(customer);
            return;
        }

        // We only activate this if there is a single source in the list
        CustomerSource customerSource = customer.getSources().get(0);
        if (customerSource == null || customerSource.getId() == null) {
            // If the source ID is null for the only source we have, then there is nothing
            // we can do but update the display. This should not happen. It is only possible
            // for a CustomerSource to have null ID because a Card is a customer source, and
            // before those are sent to Stripe, they haven't yet been assigned an ID.
            updateAdapterWithCustomer(customer);
            return;
        }

        updateOutgoingMessages(true);
        mExpectingDefaultUpdate = true;
        if (mCustomerSessionProxy == null) {
            CustomerSession.getInstance().setCustomerDefaultSource(
                    this,
                    customerSource.getId(),
                    customerSource.getSourceType());
        } else {
            mCustomerSessionProxy.setCustomerDefaultSource(
                    this,
                    customerSource.getId(),
                    customerSource.getSourceType());
        }
    }

    private void createListFromCustomerSources() {
        setCommunicatingProgress(false);
        if (mCustomer == null) {
            return;
        }

        List<CustomerSource> customerSourceList = mCustomer.getSources();

        if (!mRecyclerViewUpdated) {
            mMaskedCardAdapter = new MaskedCardAdapter(customerSourceList);
            // init the RecyclerView
            RecyclerView.LayoutManager linearLayoutManager = new LinearLayoutManager(this);
            mRecyclerView.setHasFixedSize(false);
            mRecyclerView.setLayoutManager(linearLayoutManager);
            mRecyclerView.setAdapter(mMaskedCardAdapter);
            mRecyclerViewUpdated = true;
        } else {
            mMaskedCardAdapter.setCustomerSourceList(customerSourceList);
        }

        String defaultSource = mCustomer.getDefaultSource();
        if (!TextUtils.isEmpty(defaultSource)) {
            mMaskedCardAdapter.setSelectedSource(defaultSource);
        }
        mMaskedCardAdapter.notifyDataSetChanged();
    }

    private void cancelAndFinish() {
        setResult(RESULT_CANCELED);
        finish();
    }

    private void finishWithSelection(String selectedSourceId) {
        CustomerSource customerSource = mCustomer.getSourceById(selectedSourceId);
        if (customerSource != null) {
            Intent intent = new Intent();
            intent.putExtra(EXTRA_SELECTED_PAYMENT, customerSource.toString());
            setResult(RESULT_OK, intent);
        } else {
            setResult(RESULT_CANCELED);
        }

        finish();
    }

    private void getCustomerFromSession() {
        updateOutgoingMessages(true);
        if (mCustomerSessionProxy == null) {
            CustomerSession.getInstance().retrieveCurrentCustomer(this);
        } else {
            mCustomerSessionProxy.retrieveCurrentCustomer(this);
        }
    }

    /**
     * Add or remove from the count of outgoing messages, and update the communication status
     * accordingly.
     *
     * @param starting {@code true} if we are sending a new message, otherwise {@code false}.
     */
    private void updateOutgoingMessages(boolean starting) {
        mOutgoingCommunicationAttempts = starting
                ? mOutgoingCommunicationAttempts + 1
                : mOutgoingCommunicationAttempts - 1;
        if (mOutgoingCommunicationAttempts < 0) {
            mOutgoingCommunicationAttempts = 0;
        }
        setCommunicatingProgress(mOutgoingCommunicationAttempts > 0);
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

        CustomerSource selectedSource = mMaskedCardAdapter.getSelectedSource();
        if (selectedSource == null || selectedSource.getId() == null) {
            return;
        }
        mWaitingToFinish = true;
        updateOutgoingMessages(true);
        if (mCustomerSessionProxy == null) {
            CustomerSession.getInstance().setCustomerDefaultSource(
                    this, selectedSource.getId(), selectedSource.getSourceType());
        } else {
            mCustomerSessionProxy.setCustomerDefaultSource(
                    this,
                    selectedSource.getId(),
                    selectedSource.getSourceType());
        }
    }

    private void showError(@NonNull String error) {
        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setMessage(error)
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .create();
        alertDialog.show();
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

    interface CustomerSessionProxy {
        void addProductUsageTokenIfValid(String token);
        @Nullable Customer getCachedCustomer();
        void retrieveCurrentCustomer(@NonNull Context context);
        void setCustomerDefaultSource(
                @NonNull Context context,
                @NonNull String sourceId,
                @NonNull String sourceType);
        void updateCurrentCustomer(@NonNull Context context);
    }
}
