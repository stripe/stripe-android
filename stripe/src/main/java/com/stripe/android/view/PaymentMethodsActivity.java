package com.stripe.android.view;

import android.content.Context;
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
import com.stripe.android.model.Customer;
import com.stripe.android.model.CustomerSource;

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
    private CustomerSessionProxy mCustomerSessionProxy;
    private MaskedCardAdapter mMaskedCardAdapter;
    private ProgressBar mProgressBar;
    private RecyclerView mRecyclerView;
    private boolean mRecyclerViewUpdated;
    private boolean mStartedFromPaymentSession;

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
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_ADD_CARD && resultCode == RESULT_OK) {
            setCommunicatingProgress(true);
            initLoggingTokens();
            CustomerSession.CustomerRetrievalListener listener =
                    new CustomerSession.CustomerRetrievalListener() {
                        @Override
                        public void onCustomerRetrieved(@NonNull Customer customer) {
                            updateCustomerAndSetDefaultSourceIfNecessary(customer);
                        }

                        @Override
                        public void onError(int errorCode, @Nullable String errorMessage) {
                            String displayedError = errorMessage == null ? "" : errorMessage;
                            showError(displayedError);
                            setCommunicatingProgress(false);
                        }
                    };
            if (mCustomerSessionProxy == null) {
                CustomerSession.getInstance().updateCurrentCustomer(listener);
            } else {
                mCustomerSessionProxy.updateCurrentCustomer(listener);
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
            if (!handled) {
                onBackPressed();
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
        if (!TextUtils.isEmpty(customer.getDefaultSource()) || customer.getSources().size() != 1) {
            updateAdapterWithCustomer(customer);
            return;
        }

        CustomerSession.CustomerRetrievalListener listener =
                new CustomerSession.CustomerRetrievalListener() {
                    @Override
                    public void onCustomerRetrieved(@NonNull Customer customer) {
                        updateAdapterWithCustomer(customer);
                    }

                    @Override
                    public void onError(int errorCode, @Nullable String errorMessage) {
                        // Note: if this Activity is changed to subclass StripeActivity,
                        // this code will make the error message show twice, since StripeActivity
                        // will listen to the broadcast version of the error
                        // coming from CustomerSession
                        String displayedError = errorMessage == null ? "" : errorMessage;
                        showError(displayedError);
                        setCommunicatingProgress(false);
                    }
                };

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

        if (mCustomerSessionProxy == null) {
            CustomerSession.getInstance().setCustomerDefaultSource(
                    this,
                    customerSource.getId(),
                    customerSource.getSourceType(),
                    listener);
        } else {
            mCustomerSessionProxy.setCustomerDefaultSource(
                    customerSource.getId(),
                    customerSource.getSourceType(),
                    listener);
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
        CustomerSession.CustomerRetrievalListener listener =
                new CustomerSession.CustomerRetrievalListener() {
                    @Override
                    public void onCustomerRetrieved(@NonNull Customer customer) {
                        mCustomer = customer;
                        createListFromCustomerSources();
                    }

                    @Override
                    public void onError(int errorCode, @Nullable String errorMessage) {
                        setCommunicatingProgress(false);
                    }
                };

        setCommunicatingProgress(true);
        if (mCustomerSessionProxy == null) {
            CustomerSession.getInstance().retrieveCurrentCustomer(listener);
        } else {
            mCustomerSessionProxy.retrieveCurrentCustomer(listener);
        }
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
        CustomerSession.CustomerRetrievalListener listener =
                new CustomerSession.CustomerRetrievalListener() {
                    @Override
                    public void onCustomerRetrieved(@NonNull Customer customer) {
                        mCustomer = customer;
                        finishWithSelection(customer.getDefaultSource());
                        setCommunicatingProgress(false);
                    }

                    @Override
                    public void onError(int errorCode, @Nullable String errorMessage) {
                        String displayedError = errorMessage == null ? "" : errorMessage;
                        showError(displayedError);
                        setCommunicatingProgress(false);
                    }
                };
        if (selectedSource == null || selectedSource.getId() == null) {
            return;
        }
        if (mCustomerSessionProxy == null) {
            CustomerSession.getInstance().setCustomerDefaultSource(
                    this, selectedSource.getId(), selectedSource.getSourceType(), listener);
        } else {
            mCustomerSessionProxy.setCustomerDefaultSource(
                    selectedSource.getId(),
                    selectedSource.getSourceType(),
                    listener);
        }
        setCommunicatingProgress(true);
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

        @Nullable
        Customer getCachedCustomer();

        void retrieveCurrentCustomer(@NonNull CustomerSession.CustomerRetrievalListener listener);

        void setCustomerDefaultSource(@NonNull String sourceId,
                                      @NonNull String sourceType,
                                      @Nullable CustomerSession.CustomerRetrievalListener listener);

        void updateCurrentCustomer(@NonNull CustomerSession.CustomerRetrievalListener listener);
    }
}
