package com.stripe.android.view;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
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

    private PaymentMethodsAdapter mAdapter;
    private ProgressBar mProgressBar;
    private boolean mStartedFromPaymentSession;
    private CustomerSession mCustomerSession;
    private CardDisplayTextFactory mCardDisplayTextFactory;

    @Nullable private PaymentMethod mTappedPaymentMethod = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_methods);

        final PaymentMethodsActivityStarter.Args args =
                PaymentMethodsActivityStarter.Args.create(getIntent());
        mCardDisplayTextFactory = CardDisplayTextFactory.create(this);
        mProgressBar = findViewById(R.id.payment_methods_progress_bar);

        final String initiallySelectedPaymentMethodId;
        if (savedInstanceState != null &&
                savedInstanceState.containsKey(STATE_SELECTED_PAYMENT_METHOD_ID)) {
            initiallySelectedPaymentMethodId =
                    savedInstanceState.getString(STATE_SELECTED_PAYMENT_METHOD_ID);
        } else {
            initiallySelectedPaymentMethodId = args.initialPaymentMethodId;
        }
        final RecyclerView recyclerView = findViewById(R.id.payment_methods_recycler);
        mAdapter = new PaymentMethodsAdapter(
                initiallySelectedPaymentMethodId,
                args,
                args.paymentMethodTypes
        );
        mAdapter.setListener(new PaymentMethodsAdapter.Listener() {
            @Override
            public void onClick(@NonNull PaymentMethod paymentMethod) {
                mTappedPaymentMethod = paymentMethod;
            }
        });

        // init the RecyclerView
        recyclerView.setHasFixedSize(false);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(mAdapter);
        recyclerView.setItemAnimator(new DefaultItemAnimator() {
            @Override
            public void onAnimationFinished(@NonNull RecyclerView.ViewHolder viewHolder) {
                super.onAnimationFinished(viewHolder);

                // wait until post-tap animations are completed before finishing activity
                if (mTappedPaymentMethod != null) {
                    setSelectionAndFinish(mTappedPaymentMethod);
                    mTappedPaymentMethod = null;
                }
            }
        });

        final ItemTouchHelper itemTouchHelper = new ItemTouchHelper(
                new PaymentMethodSwipeCallback(this, mAdapter,
                        new SwipeToDeleteCallbackListener(this))
        );
        itemTouchHelper.attachToRecyclerView(recyclerView);

        mCustomerSession = CustomerSession.getInstance();
        mStartedFromPaymentSession = args.isPaymentSessionActive;

        final Toolbar toolbar = findViewById(R.id.payment_methods_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        fetchCustomerPaymentMethods();

        // This prevents the first click from being eaten by the focus.
        recyclerView.requestFocusFromTouch();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AddPaymentMethodActivityStarter.REQUEST_CODE &&
                resultCode == RESULT_OK) {
            onPaymentMethodCreated(data);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        setSelectionAndFinish(mAdapter.getSelectedPaymentMethod());
        return true;
    }

    private void onPaymentMethodCreated(@Nullable Intent data) {
        initLoggingTokens();

        if (data != null && data.hasExtra(EXTRA_NEW_PAYMENT_METHOD)) {
            final PaymentMethod paymentMethod =
                    data.getParcelableExtra(EXTRA_NEW_PAYMENT_METHOD);
            onAddedPaymentMethod(paymentMethod);
        } else {
            fetchCustomerPaymentMethods();
        }
    }

    private void onAddedPaymentMethod(@Nullable PaymentMethod paymentMethod) {
        final PaymentMethod.Type type = paymentMethod != null ?
                PaymentMethod.Type.lookup(paymentMethod.type) : null;
        if (type != null && !type.isReusable) {
            // If the added Payment Method is not reusable, it also can't be attached to a
            // customer, so immediately return to the launching host with the new
            // Payment Method.
            finishWithPaymentMethod(paymentMethod);
        } else {
            // Refresh the list of Payment Methods with the new Payment Method.
            fetchCustomerPaymentMethods();

            if (paymentMethod != null) {
                showSnackbar(paymentMethod, R.string.added);
            }
        }
    }

    private void onDeletedPaymentMethod(@NonNull PaymentMethod paymentMethod) {
        mAdapter.deletePaymentMethod(paymentMethod);

        if (paymentMethod.id != null) {
            mCustomerSession.detachPaymentMethod(
                    paymentMethod.id,
                    new PaymentMethodDeleteListener()
            );
        }

        showSnackbar(paymentMethod, R.string.removed);
    }

    private void showSnackbar(@NonNull PaymentMethod paymentMethod, @StringRes int stringRes) {
        final String snackbarText;
        if (paymentMethod.card != null) {
            snackbarText = getString(
                    stringRes,
                    mCardDisplayTextFactory.createUnstyled(paymentMethod.card)
            );
        } else {
            snackbarText = null;
        }

        if (snackbarText != null) {
            Snackbar.make(
                    findViewById(R.id.payment_methods_coordinator),
                    snackbarText,
                    Snackbar.LENGTH_SHORT
            ).show();
        }
    }

    @Override
    public void onBackPressed() {
        setSelectionAndFinish(mAdapter.getSelectedPaymentMethod());
    }

    private void fetchCustomerPaymentMethods() {
        setCommunicatingProgress(true);
        mCustomerSession.getPaymentMethods(PaymentMethod.Type.Card,
                new PaymentMethodsRetrievalListener(this));
    }

    private void updatePaymentMethods(@NonNull List<PaymentMethod> paymentMethods) {
        mAdapter.setPaymentMethods(paymentMethods);
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
        mProgressBar.setVisibility(communicating ? View.VISIBLE : View.GONE);
        supportInvalidateOptionsMenu();
    }

    @VisibleForTesting
    void setSelectionAndFinish(@Nullable PaymentMethod paymentMethod) {
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
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        final PaymentMethod paymentMethod = mAdapter.getSelectedPaymentMethod();
        if (paymentMethod != null) {
            outState.putString(STATE_SELECTED_PAYMENT_METHOD_ID, paymentMethod.id);
        }
    }

    private void confirmDeletePaymentMethod(@NonNull final PaymentMethod paymentMethod) {
        final String message = paymentMethod.card != null ?
                mCardDisplayTextFactory.createUnstyled(paymentMethod.card) : null;
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_payment_method)
                .setMessage(message)
                .setPositiveButton(
                        android.R.string.yes,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                onDeletedPaymentMethod(paymentMethod);
                            }
                        })
                .setNegativeButton(
                        android.R.string.no,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                mAdapter.resetPaymentMethod(paymentMethod);
                            }
                        })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        mAdapter.resetPaymentMethod(paymentMethod);
                    }
                })
                .create()
                .show();
    }

    private static final class PaymentMethodsRetrievalListener extends
            CustomerSession.ActivityPaymentMethodsRetrievalListener<PaymentMethodsActivity> {

        private PaymentMethodsRetrievalListener(@NonNull PaymentMethodsActivity activity) {
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

    private static final class PaymentMethodDeleteListener
            implements CustomerSession.PaymentMethodRetrievalListener {

        @Override
        public void onPaymentMethodRetrieved(@NonNull PaymentMethod paymentMethod) {

        }

        @Override
        public void onError(int errorCode, @NonNull String errorMessage,
                            @Nullable StripeError stripeError) {
        }
    }

    private static final class SwipeToDeleteCallbackListener
            implements PaymentMethodSwipeCallback.Listener {

        private final PaymentMethodsActivity mActivity;

        private SwipeToDeleteCallbackListener(@NonNull PaymentMethodsActivity activity) {
            mActivity = activity;
        }

        @Override
        public void onSwiped(@NonNull PaymentMethod paymentMethod) {
            mActivity.confirmDeletePaymentMethod(paymentMethod);
        }
    }
}
