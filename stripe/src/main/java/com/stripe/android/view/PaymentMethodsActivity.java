package com.stripe.android.view;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import com.stripe.android.CustomerSession;
import com.stripe.android.R;
import com.stripe.android.StripeError;
import com.stripe.android.model.PaymentMethod;
import com.stripe.android.view.i18n.TranslatorManager;

import java.util.List;

import static com.stripe.android.PaymentSession.TOKEN_PAYMENT_SESSION;
import static com.stripe.android.view.AddPaymentMethodActivity.EXTRA_NEW_PAYMENT_METHOD;

/**
 * An activity that allows a user to select from a customer's available payment methods, or
 * to add new ones.
 */
public class PaymentMethodsActivity extends AppCompatActivity {

    private static final String STATE_SELECTED_PAYMENT_METHOD = "state_selected_payment_method";

    public static final String EXTRA_SELECTED_PAYMENT = "selected_payment";
    public static final String TOKEN_PAYMENT_METHODS_ACTIVITY = "PaymentMethodsActivity";

    static final int REQUEST_CODE_ADD_CARD = 700;
    private boolean mCommunicating;
    @Nullable private MaskedCardAdapter mMaskedCardAdapter;
    private ProgressBar mProgressBar;
    private RecyclerView mRecyclerView;
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
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_methods);

        final PaymentMethodsActivityStarter.Args args =
                PaymentMethodsActivityStarter.Args.create(getIntent());

        mProgressBar = findViewById(R.id.payment_methods_progress_bar);
        mRecyclerView = findViewById(R.id.payment_methods_recycler);
        final View addCardView = findViewById(R.id.payment_methods_add_payment_container);

        mCustomerSession = CustomerSession.getInstance();
        mStartedFromPaymentSession = args.isPaymentSessionActive;

        final boolean shouldShowPostalField = args.shouldRequirePostalCode;
        addCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(@NonNull View view) {
                new AddPaymentMethodActivityStarter(PaymentMethodsActivity.this)
                        .startForResult(REQUEST_CODE_ADD_CARD,
                                new AddPaymentMethodActivityStarter.Args.Builder()
                                        .setShouldUpdateCustomer(true)
                                        .setShouldRequirePostalCode(shouldShowPostalField)
                                        .setIsPaymentSessionActive(mStartedFromPaymentSession)
                                        .setPaymentMethodType(PaymentMethod.Type.Card)
                                        .setPaymentConfiguration(args.paymentConfiguration)
                                        .build());
            }
        });

        final Toolbar toolbar = findViewById(R.id.payment_methods_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        final String selectedPaymentMethodId;

        if (savedInstanceState != null &&
                savedInstanceState.containsKey(STATE_SELECTED_PAYMENT_METHOD)) {
            selectedPaymentMethodId = savedInstanceState.getString(STATE_SELECTED_PAYMENT_METHOD);
        } else {
            selectedPaymentMethodId = args.initialPaymentMethodId;
        }

        getCustomerPaymentMethods(selectedPaymentMethodId);

        // This prevents the first click from being eaten by the focus.
        addCardView.requestFocusFromTouch();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_ADD_CARD && resultCode == RESULT_OK) {
            initLoggingTokens();

            if (data.hasExtra(EXTRA_NEW_PAYMENT_METHOD)) {
                final PaymentMethod paymentMethod =
                        data.getParcelableExtra(EXTRA_NEW_PAYMENT_METHOD);
                getCustomerPaymentMethods(paymentMethod != null ? paymentMethod.id : null);
            } else {
                getCustomerPaymentMethods(null);
            }
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
        getMenuInflater().inflate(R.menu.add_payment_method, menu);
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

    private void getCustomerPaymentMethods(@Nullable String selectPaymentMethodId) {
        setCommunicatingProgress(true);
        mCustomerSession.getPaymentMethods(PaymentMethod.Type.Card,
                new GetPaymentMethodsRetrievalListener(this, selectPaymentMethodId));
    }

    private void updatePaymentMethods(@NonNull List<PaymentMethod> paymentMethods,
                                      @Nullable String selectPaymentMethodId) {
        if (mMaskedCardAdapter == null) {
            mMaskedCardAdapter = new MaskedCardAdapter(paymentMethods);
            // init the RecyclerView
            mRecyclerView.setHasFixedSize(false);
            mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            mRecyclerView.setAdapter(mMaskedCardAdapter);
        } else {
            mMaskedCardAdapter.setPaymentMethods(paymentMethods);
        }
        if (selectPaymentMethodId != null) {
            mMaskedCardAdapter.setSelectedPaymentMethod(selectPaymentMethodId);
        }
    }

    private void initLoggingTokens() {
        if (mStartedFromPaymentSession) {
            mCustomerSession.addProductUsageTokenIfValid(TOKEN_PAYMENT_SESSION);
        }
        mCustomerSession.addProductUsageTokenIfValid(TOKEN_PAYMENT_METHODS_ACTIVITY);
    }

    private void cancelAndFinish() {
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

    private void setSelectionAndFinish() {
        if (mMaskedCardAdapter == null || mMaskedCardAdapter.getSelectedPaymentMethod() == null) {
            cancelAndFinish();
            return;
        }

        final PaymentMethod paymentMethod = mMaskedCardAdapter.getSelectedPaymentMethod();
        if (paymentMethod == null || paymentMethod.id == null) {
            cancelAndFinish();
            return;
        }

        final Intent intent = new Intent().putExtra(EXTRA_SELECTED_PAYMENT, paymentMethod);
        setResult(RESULT_OK, intent);
        finish();
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

    private static final class GetPaymentMethodsRetrievalListener extends
            CustomerSession.ActivityPaymentMethodsRetrievalListener<PaymentMethodsActivity> {

        @Nullable final String mSelectPaymentMethodId;

        GetPaymentMethodsRetrievalListener(@NonNull PaymentMethodsActivity activity,
                                           @Nullable String selectPaymentMethodId) {
            super(activity);
            mSelectPaymentMethodId = selectPaymentMethodId;
        }

        @Override
        public void onPaymentMethodsRetrieved(@NonNull List<PaymentMethod> paymentMethods) {
            final PaymentMethodsActivity activity = getActivity();
            if (activity == null) {
                return;
            }

            activity.updatePaymentMethods(paymentMethods, mSelectPaymentMethodId);
            activity.setCommunicatingProgress(false);
        }

        @Override
        public void onError(int errorCode, @NonNull String errorMessage,
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
                    .translate(errorCode, errorMessage, stripeError);
            activity.showError(displayedError);
            activity.setCommunicatingProgress(false);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mMaskedCardAdapter != null) {
            outState.putString(STATE_SELECTED_PAYMENT_METHOD,
                    mMaskedCardAdapter.getSelectedPaymentMethodId());
        }
    }
}
