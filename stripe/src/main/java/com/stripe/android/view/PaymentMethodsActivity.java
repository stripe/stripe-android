package com.stripe.android.view;

import android.content.Context;
import android.content.Intent;
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
import com.stripe.android.model.Source;

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

    public static Intent newIntent(Context context) {
        return new Intent(context, PaymentMethodsActivity.class);
    }

    // Revert this to package private before committing.
    public static Intent newIntent(Context context, @Nullable Customer customer) {
        Intent intent = new Intent(context, PaymentMethodsActivity.class);
        if (customer != null) {
            intent.putExtra(EXTRA_CUSTOMER, customer.toString());
        }
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_methods);

        Intent startingIntent = getIntent();
        String customerString = startingIntent.hasExtra(EXTRA_CUSTOMER)
                ? startingIntent.getStringExtra(EXTRA_CUSTOMER)
                : null;

        // Allows for demonstration without initializing a CustomerSession.
        mIsLocalOnly = !TextUtils.isEmpty(customerString);
        Customer localCustomer = Customer.fromString(customerString);

        CustomerSession session;
        if (!mIsLocalOnly) {
            try {
                session = CustomerSession.getInstance();
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
                            AddSourceActivity.newIntent(PaymentMethodsActivity.this, false),
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

        if (CustomerSession.getInstance().canUseCachedCustomer()) {
            mCustomer = CustomerSession.getInstance().getCachedCustomer();
            initWithCustomer();
        } else {
            initWithCustomerSession();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_ADD_CARD && resultCode == RESULT_OK) {
            String sourceString = data.getStringExtra(AddSourceActivity.EXTRA_NEW_SOURCE);
            addSourceToCustomer(Source.fromString(sourceString));
        }
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

    void addSourceToCustomer(@Nullable Source source) {
        if (source == null) {
            return;
        }

        if (mIsLocalOnly) {
            CustomerSource customerSource = CustomerSource.fromJson(source.toJson());
            if (customerSource != null) {
                mMaskedCardAdapter.addCustomerSource(customerSource);
            }
        } else {
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
            CustomerSession.getInstance().addCustomerSource(source.getId(), listener);
        }
    }

    void initWithCustomer() {
        setCommunicatingProgress(false);
        if (mCustomer == null) {
            return;
        }

        List<CustomerSource> customerSourceList = mCustomer.getSources();
        mMaskedCardAdapter = new MaskedCardAdapter(customerSourceList);
        // init the RecyclerView
        RecyclerView.LayoutManager linearLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setHasFixedSize(false);
        mRecyclerView.setLayoutManager(linearLayoutManager);
        mRecyclerView.setAdapter(mMaskedCardAdapter);

        String defaultSource = mCustomer.getDefaultSource();
        if (!TextUtils.isEmpty(defaultSource)) {
            mMaskedCardAdapter.setSelectedSource(defaultSource);
        }
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

        Intent intent = new Intent();
        intent.putExtra(EXTRA_SELECTED_PAYMENT, selectedId);
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
