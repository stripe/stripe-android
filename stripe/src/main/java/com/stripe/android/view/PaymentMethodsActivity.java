package com.stripe.android.view;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.transition.Fade;
import android.support.transition.TransitionManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.stripe.android.CustomerSession;
import com.stripe.android.R;
import com.stripe.android.model.Customer;
import com.stripe.android.model.CustomerSource;

import java.util.List;

public class PaymentMethodsActivity extends AppCompatActivity {

    public static final String EXTRA_SELECTED_PAYMENT = "selected_payment";
    static final String EXTRA_PROXY_DELAY = "proxy_delay";
    private static final String EXTRA_CUSTOMER = "customer";

    static final int REQUEST_CODE_ADD_CARD = 700;
    private static final long FADE_DURATION_MS = 100L;
    private boolean mCommunicating;
    private boolean mIsLocalOnly;
    private Customer mCustomer;
    private CustomerSessionProxy mCustomerSessionProxy;
    private TextView mErrorTextView;
    private FrameLayout mErrorLayout;
    private MaskedCardAdapter mMaskedCardAdapter;
    private ProgressBar mProgressBar;
    private RecyclerView mRecyclerView;
    private View mAddCardView;
    private boolean mRecyclerViewUpdated;

    public static Intent newIntent(Context context) {
        return new Intent(context, PaymentMethodsActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_methods);

        Intent startingIntent = getIntent();
        String customerString = startingIntent.hasExtra(EXTRA_CUSTOMER)
                ? startingIntent.getStringExtra(EXTRA_CUSTOMER)
                : null;

        mIsLocalOnly = !TextUtils.isEmpty(customerString);
        Customer localCustomer = Customer.fromString(customerString);

        mProgressBar = findViewById(R.id.payment_methods_progress_bar);
        mRecyclerView = findViewById(R.id.payment_methods_recycler);
        mErrorLayout = findViewById(R.id.payment_methods_error_container);
        mErrorTextView = findViewById(R.id.tv_payment_methods_error);
        mAddCardView = findViewById(R.id.payment_methods_add_payment_container);

        if (mIsLocalOnly) {
            mAddCardView.setVisibility(View.GONE);
        }
        mAddCardView.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        startActivityForResult(
                                AddSourceActivity.newIntent(PaymentMethodsActivity.this,
                                        false, !mIsLocalOnly),
                                REQUEST_CODE_ADD_CARD);
                    }
                });

        Toolbar toolbar = findViewById(R.id.payment_methods_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        if (localCustomer != null) {
            mCustomer = localCustomer;
            initWithCustomer();
            return;
        }

        boolean waitForProxy = getIntent().getBooleanExtra(EXTRA_PROXY_DELAY, false);
        if (waitForProxy) {
            return;
        }
        initializeData();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_ADD_CARD && resultCode == RESULT_OK) {
            setCommunicatingProgress(true);
            CustomerSession.CustomerRetrievalListener listener =
                    new CustomerSession.CustomerRetrievalListener() {
                        @Override
                        public void onCustomerRetrieved(@NonNull Customer customer) {
                            addSelectionIfNecessaryAndUpdateCustomer(customer);
                        }

                        @Override
                        public void onError(int errorCode, @Nullable String errorMessage) {
                            String displayedError = errorMessage == null ? "" : errorMessage;
                            showError(displayedError, mCustomerSessionProxy == null);
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
        Drawable tintedIcon = ViewUtils.getTintedIcon(
                this,
                R.drawable.ic_checkmark,
                android.R.color.primary_text_dark);
        saveItem.setIcon(tintedIcon);
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

    void addSelectionIfNecessaryAndUpdateCustomer(@NonNull Customer customer) {
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
                        String displayedError = errorMessage == null ? "" : errorMessage;
                        showError(displayedError, mCustomerSessionProxy == null);
                        setCommunicatingProgress(false);
                    }
                };

        // We only activate this if there is a single source in the list
        String sourceId = customer.getSources().get(0).getId();
        if (sourceId == null) {
            updateAdapterWithCustomer(customer);
            return;
        }

        if (mCustomerSessionProxy == null) {
            CustomerSession.getInstance().setCustomerDefaultSource(sourceId, listener);
        } else {
            mCustomerSessionProxy.setCustomerDefaultSource(sourceId, listener);
        }
    }

    void initializeData() {
        Customer cachedCustomer;
        if (mCustomerSessionProxy == null) {
            cachedCustomer = CustomerSession.getInstance().getCachedCustomer();
        } else {
            cachedCustomer = mCustomerSessionProxy.getCachedCustomer();
        }

        if (cachedCustomer != null) {
            mCustomer = cachedCustomer;
            initWithCustomer();
        } else {
            initWithCustomerSession();
        }
    }

    void initWithCustomer() {
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

    void initWithCustomerSession() {
        CustomerSession.CustomerRetrievalListener listener =
                new CustomerSession.CustomerRetrievalListener() {
                    @Override
                    public void onCustomerRetrieved(@NonNull Customer customer) {
                        mCustomer = customer;
                        initWithCustomer();
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

    void setSelectionAndFinish() {
        if (mMaskedCardAdapter == null) {
            cancelAndFinish();
            return;
        }

        String selectedId = mMaskedCardAdapter.getSelectedSource();
        if (selectedId == null) {
            cancelAndFinish();
            return;
        }

        if (mIsLocalOnly) {
            finishWithSelection(selectedId);
        } else {
            CustomerSession.CustomerRetrievalListener listener =
                    new CustomerSession.CustomerRetrievalListener() {
                        @Override
                        public void onCustomerRetrieved(@NonNull Customer customer) {
                            finishWithSelection(customer.getDefaultSource());
                            setCommunicatingProgress(false);
                        }

                        @Override
                        public void onError(int errorCode, @Nullable String errorMessage) {
                            String displayedError = errorMessage == null ? "" : errorMessage;
                            showError(displayedError, mCustomerSessionProxy == null);
                            setCommunicatingProgress(false);
                        }
                    };
            if (mCustomerSessionProxy == null) {
                CustomerSession.getInstance().setCustomerDefaultSource(selectedId, listener);
            } else {
                mCustomerSessionProxy.setCustomerDefaultSource(selectedId, listener);
            }
            setCommunicatingProgress(true);
        }
    }

    void finishWithSelection(String selectedSourceId) {
        CustomerSource customerSource = mCustomer.getSourceById(selectedSourceId);
        Intent intent = new Intent();
        intent.putExtra(EXTRA_SELECTED_PAYMENT, customerSource.toString());
        setResult(RESULT_OK, intent);
        finish();
    }

    void cancelAndFinish() {
        setResult(RESULT_CANCELED);
        finish();
    }

    @VisibleForTesting
    void setCustomerSessionProxy(CustomerSessionProxy proxy) {
        mCustomerSessionProxy = proxy;
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

    private void showError(@NonNull String error, boolean shouldAnimate) {
        mErrorTextView.setText(error);
        if (shouldAnimate) {
            Fade fadeIn = new Fade(Fade.IN);
            fadeIn.setDuration(FADE_DURATION_MS);
            TransitionManager.beginDelayedTransition(mErrorLayout, fadeIn);
        }
        mErrorTextView.setVisibility(View.VISIBLE);
    }

    private void updateAdapterWithCustomer(@NonNull Customer customer) {
        mMaskedCardAdapter.updateCustomer(customer);
        setCommunicatingProgress(false);
    }

    interface CustomerSessionProxy {
        @Nullable
        Customer getCachedCustomer();
        void retrieveCurrentCustomer(@NonNull CustomerSession.CustomerRetrievalListener listener);
        void setCustomerDefaultSource(@NonNull String sourceId,
                                      @Nullable CustomerSession.CustomerRetrievalListener listener);
        void updateCurrentCustomer(@NonNull CustomerSession.CustomerRetrievalListener listener);
    }
}
