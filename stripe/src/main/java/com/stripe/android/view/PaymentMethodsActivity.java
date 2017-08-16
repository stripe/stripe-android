package com.stripe.android.view;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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

import java.util.List;

public class PaymentMethodsActivity extends AppCompatActivity {

    public static final String EXTRA_SELECTED_PAYMENT = "selected_payment";
    private static final String EXTRA_CUSTOMER = "customer";

    private static final int REQUEST_CODE_ADD_CARD = 700;
    private boolean mCommunicating;
    private boolean mIsLocalOnly;
    private Customer mCustomer;
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

        if (!mIsLocalOnly) {
            try {
                CustomerSession.getInstance();
            } catch (IllegalStateException illegalState) {
                finish();
                return;
            }
        }

        mProgressBar = findViewById(R.id.payment_methods_progress_bar);
        mRecyclerView = findViewById(R.id.payment_methods_recycler);
        mAddCardView = findViewById(R.id.payment_methods_add_payment_container);
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

        Customer cachedCustomer = CustomerSession.getInstance().getCachedCustomer();
        if (cachedCustomer != null) {
            mCustomer = cachedCustomer;
            initWithCustomer();
        } else {
            initWithCustomerSession();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_ADD_CARD && resultCode == RESULT_OK) {
            CustomerSession.getInstance().updateCurrentCustomer(
                    new CustomerSession.CustomerRetrievalListener() {
                        @Override
                        public void onCustomerRetrieved(@NonNull Customer customer) {
                            mMaskedCardAdapter.updateCustomer(customer);
                        }

                        @Override
                        public void onError(int errorCode, @Nullable String errorMessage) {

                        }
                    });
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
        mProgressBar.setVisibility(View.VISIBLE);
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
        CustomerSession.getInstance().retrieveCurrentCustomer(listener);
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
            CustomerSession.getInstance().setCustomerDefaultSource(
                    selectedId,
                    new CustomerSession.CustomerRetrievalListener() {
                        @Override
                        public void onCustomerRetrieved(@NonNull Customer customer) {
                            finishWithSelection(customer.getDefaultSource());
                        }

                        @Override
                        public void onError(int errorCode, @Nullable String errorMessage) {

                        }
                    });
        }
    }

    void finishWithSelection(String selectedSourceId) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_SELECTED_PAYMENT, selectedSourceId);
        setResult(RESULT_OK, intent);
        finish();
    }

    void cancelAndFinish() {
        setResult(RESULT_CANCELED);
        finish();
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
}
