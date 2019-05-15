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

import static com.stripe.android.PaymentSession.EXTRA_PAYMENT_SESSION_ACTIVE;
import static com.stripe.android.PaymentSession.TOKEN_PAYMENT_SESSION;

/**
 * An activity that allows a user to select from a customer's available payment methods, or
 * to add new ones.
 */
public class PaymentMethodsActivity extends AppCompatActivity {

    public static final String EXTRA_SELECTED_PAYMENT = "selected_payment";
    private static final String PAYMENT_METHODS_ACTIVITY = "PaymentMethodsActivity";

    static final int REQUEST_CODE_ADD_CARD = 700;
    private boolean mCommunicating;
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
                final Intent addSourceIntent = AddPaymentMethodActivity.newIntent(
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

        getCustomerPaymentMethods(null);

        // This prevents the first click from being eaten by the focus.
        addCardView.requestFocusFromTouch();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_ADD_CARD && resultCode == RESULT_OK) {
            setCommunicatingProgress(true);
            initLoggingTokens();
            mCustomerSession.getPaymentMethods(PaymentMethod.Type.Card,
                    new GetPaymentMethodsRetrievalListener(this));
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

    private void getCustomerPaymentMethods() {
        mCustomerSession.getPaymentMethods(PaymentMethod.Type.Card,
                new GetPaymentMethodsRetrievalListener(this));
    }

    private void updatePaymentMethods(@NonNull List<PaymentMethod> paymentMethods) {
        if (!mRecyclerViewUpdated) {
            mMaskedCardAdapter = new MaskedCardAdapter(paymentMethods);
            // init the RecyclerView
            mRecyclerView.setHasFixedSize(false);
            mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            mRecyclerView.setAdapter(mMaskedCardAdapter);
            mRecyclerViewUpdated = true;
        } else {
            mMaskedCardAdapter.setPaymentMethods(paymentMethods);
        }
    }

    private void initLoggingTokens() {
        if (mStartedFromPaymentSession) {
            mCustomerSession.addProductUsageTokenIfValid(TOKEN_PAYMENT_SESSION);
        }
        mCustomerSession.addProductUsageTokenIfValid(PAYMENT_METHODS_ACTIVITY);
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

        final Intent intent = new Intent().putExtra(EXTRA_SELECTED_PAYMENT,
                paymentMethod.toJson().toString());
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

        GetPaymentMethodsRetrievalListener(@NonNull PaymentMethodsActivity activity) {
            super(activity);
        }

        @Override
        public void onPaymentMethodsRetrieved(@NonNull List<PaymentMethod> paymentMethods) {
            final PaymentMethodsActivity activity = getActivity();
            if (activity == null) {
                return;
            }

            activity.updatePaymentMethods(paymentMethods);
            activity.setCommunicatingProgress(false);
        }

        @Override
        public void onError(int errorCode, @Nullable String errorMessage,
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
}
