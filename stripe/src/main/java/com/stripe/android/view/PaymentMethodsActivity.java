package com.stripe.android.view;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.stripe.android.CustomerSession;
import com.stripe.android.R;
import com.stripe.android.StripeError;
import com.stripe.android.model.PaymentMethod;
import com.stripe.android.view.i18n.TranslatorManager;

import java.util.List;

import static com.stripe.android.PaymentSession.TOKEN_PAYMENT_SESSION;
import static com.stripe.android.view.AddPaymentMethodActivity.EXTRA_NEW_PAYMENT_METHOD;

/**
 * <p>An activity that allows a customer to select from their attach payment methods,
 * or to add new ones.</p>
 *
 * <p>This Activity is typically started through {@link com.stripe.android.PaymentSession}.
 * To directly start this activity, use {@link PaymentMethodsActivityStarter#startForResult()}.</p>
 *
 * <p>Use {@link PaymentMethodsActivityStarter.Result#fromIntent(Intent)}
 * to retrieve the result of this activity from an intent in onActivityResult().</p>
 */
public class PaymentMethodsActivity extends AppCompatActivity {

    private static final String STATE_SELECTED_PAYMENT_METHOD_ID =
            "state_selected_payment_method_id";

    public static final String TOKEN_PAYMENT_METHODS_ACTIVITY = "PaymentMethodsActivity";

    private boolean mCommunicating;
    private PaymentMethodsAdapter mAdapter;
    private ProgressBar mProgressBar;
    private boolean mStartedFromPaymentSession;
    private CustomerSession mCustomerSession;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_methods);

        final PaymentMethodsActivityStarter.Args args =
                PaymentMethodsActivityStarter.Args.create(getIntent());

        mProgressBar = findViewById(R.id.payment_methods_progress_bar);

        final RecyclerView recyclerView = findViewById(R.id.payment_methods_recycler);
        mAdapter = new PaymentMethodsAdapter();
        // init the RecyclerView
        recyclerView.setHasFixedSize(false);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(mAdapter);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        mCustomerSession = CustomerSession.getInstance();
        mStartedFromPaymentSession = args.isPaymentSessionActive;

        final ViewGroup addPaymentMethodsContainer = findViewById(R.id.add_payment_methods);

        final View addCardView = new AddPaymentMethodCardRowView(this, args);
        addPaymentMethodsContainer.addView(addCardView);
        if (args.paymentMethodTypes.contains(PaymentMethod.Type.Fpx)) {
            addPaymentMethodsContainer.addView(new AddPaymentMethodFpxRowView(this, args));
        }

        final Toolbar toolbar = findViewById(R.id.payment_methods_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        final String selectedPaymentMethodId;
        if (savedInstanceState != null &&
                savedInstanceState.containsKey(STATE_SELECTED_PAYMENT_METHOD_ID)) {
            selectedPaymentMethodId =
                    savedInstanceState.getString(STATE_SELECTED_PAYMENT_METHOD_ID);
        } else {
            selectedPaymentMethodId = args.initialPaymentMethodId;
        }

        getCustomerPaymentMethods(selectedPaymentMethodId);

        // This prevents the first click from being eaten by the focus.
        addCardView.requestFocusFromTouch();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AddPaymentMethodActivityStarter.REQUEST_CODE &&
                resultCode == RESULT_OK) {
            onPaymentMethodCreated(data);
        }
    }

    private void onPaymentMethodCreated(@Nullable Intent data) {
        initLoggingTokens();

        if (data != null && data.hasExtra(EXTRA_NEW_PAYMENT_METHOD)) {
            final PaymentMethod paymentMethod =
                    data.getParcelableExtra(EXTRA_NEW_PAYMENT_METHOD);

            final PaymentMethod.Type type = paymentMethod != null ?
                    PaymentMethod.Type.lookup(paymentMethod.type) : null;
            if (type != null && !type.isReusable) {
                // If the added Payment Method is not reusable, it also can't be attached to a
                // customer, so immediately return to the launching host with the new
                // Payment Method.
                finishWithPaymentMethod(paymentMethod);
            } else {
                // Refresh the list of Payment Methods with the new Payment Method.
                getCustomerPaymentMethods(paymentMethod != null ? paymentMethod.id : null);
            }
        } else {
            getCustomerPaymentMethods(null);
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
                new PaymentMethodsRetrievalListener(this, selectPaymentMethodId));
    }

    private void updatePaymentMethods(@NonNull List<PaymentMethod> paymentMethods,
                                      @Nullable String selectPaymentMethodId) {
        mAdapter.setPaymentMethods(paymentMethods);
        if (selectPaymentMethodId != null) {
            mAdapter.setSelectedPaymentMethod(selectPaymentMethodId);
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
        final PaymentMethod paymentMethod = mAdapter.getSelectedPaymentMethod();
        if (paymentMethod == null || paymentMethod.id == null) {
            cancelAndFinish();
            return;
        }

        finishWithPaymentMethod(paymentMethod);
    }

    private void finishWithPaymentMethod(@NonNull PaymentMethod paymentMethod) {
        setResult(RESULT_OK, new Intent()
                .putExtras(new PaymentMethodsActivityStarter.Result(paymentMethod).toBundle())
        );
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

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        final PaymentMethod paymentMethod = mAdapter.getSelectedPaymentMethod();
        if (paymentMethod != null) {
            outState.putString(STATE_SELECTED_PAYMENT_METHOD_ID, paymentMethod.id);
        }
    }

    private static final class PaymentMethodsRetrievalListener extends
            CustomerSession.ActivityPaymentMethodsRetrievalListener<PaymentMethodsActivity> {

        @Nullable final String mSelectPaymentMethodId;

        private PaymentMethodsRetrievalListener(@NonNull PaymentMethodsActivity activity,
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
}
