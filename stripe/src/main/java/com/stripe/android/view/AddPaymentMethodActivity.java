package com.stripe.android.view;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.VisibleForTesting;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.stripe.android.ApiResultCallback;
import com.stripe.android.CustomerSession;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.R;
import com.stripe.android.Stripe;
import com.stripe.android.StripeError;
import com.stripe.android.model.PaymentMethod;
import com.stripe.android.model.PaymentMethodCreateParams;

import java.lang.ref.WeakReference;
import java.util.Objects;

import static com.stripe.android.PaymentSession.TOKEN_PAYMENT_SESSION;

/**
 * Activity used to display a {@link AddPaymentMethodView} and receive the resulting
 * {@link PaymentMethod} in the <code>Activity#onActivityResult(int, int, Intent)</code> of the
 * launching Activity.
 *
 * <p>Should be started with {@link AddPaymentMethodActivityStarter}.</p>
 */
public class AddPaymentMethodActivity extends StripeActivity {

    public static final String TOKEN_ADD_PAYMENT_METHOD_ACTIVITY = "AddPaymentMethodActivity";

    public static final String EXTRA_NEW_PAYMENT_METHOD = "new_payment_method";

    @Nullable private AddPaymentMethodView mAddPaymentMethodView;
    @Nullable private Stripe mStripe;

    private PaymentMethod.Type mPaymentMethodType;
    private boolean mStartedFromPaymentSession;
    private boolean mShouldAttachToCustomer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final AddPaymentMethodActivityStarter.Args args =
                AddPaymentMethodActivityStarter.Args.create(getIntent());
        final PaymentConfiguration paymentConfiguration;
        if (args.paymentConfiguration != null) {
            paymentConfiguration = args.paymentConfiguration;
        } else {
            paymentConfiguration = PaymentConfiguration.getInstance(this);
        }
        mStripe = new Stripe(getApplicationContext(), paymentConfiguration.getPublishableKey());
        mPaymentMethodType = args.paymentMethodType;

        configureView(args);

        mShouldAttachToCustomer = mPaymentMethodType.isReusable && args.shouldAttachToCustomer;
        mStartedFromPaymentSession = args.isPaymentSessionActive;

        if (mShouldAttachToCustomer && args.shouldInitCustomerSessionTokens) {
            initCustomerSessionTokens();
        }
    }

    private void configureView(@NonNull AddPaymentMethodActivityStarter.Args args) {
        getViewStub().setLayoutResource(R.layout.add_payment_method_layout);
        final ViewGroup contentRoot = (ViewGroup) getViewStub().inflate();

        mAddPaymentMethodView = createPaymentMethodView(args);
        contentRoot.addView(mAddPaymentMethodView);

        setTitle(getTitleStringRes());

        if (mPaymentMethodType == PaymentMethod.Type.Card) {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
    }

    @StringRes
    private int getTitleStringRes() {
        switch (mPaymentMethodType) {
            case Card: {
                return R.string.title_add_a_card;
            }
            case Fpx: {
                return R.string.title_bank_account;
            }
            default: {
                throw new IllegalArgumentException(
                        "Unsupported Payment Method type: " + mPaymentMethodType.code);
            }
        }
    }

    @NonNull
    private AddPaymentMethodView createPaymentMethodView(
            @NonNull AddPaymentMethodActivityStarter.Args args) {
        switch (mPaymentMethodType) {
            case Card: {
                return AddPaymentMethodCardView.create(this, args.shouldRequirePostalCode);
            }
            case Fpx: {
                return AddPaymentMethodFpxView.create(this);
            }
            default: {
                throw new IllegalArgumentException(
                        "Unsupported Payment Method type: " + mPaymentMethodType.code);
            }
        }
    }

    @VisibleForTesting
    void initCustomerSessionTokens() {
        CustomerSession.getInstance()
                .addProductUsageTokenIfValid(TOKEN_ADD_PAYMENT_METHOD_ACTIVITY);
        if (mStartedFromPaymentSession) {
            CustomerSession.getInstance().addProductUsageTokenIfValid(TOKEN_PAYMENT_SESSION);
        }
    }

    @Override
    protected void onActionSave() {
        createPaymentMethod(Objects.requireNonNull(mStripe),
                Objects.requireNonNull(mAddPaymentMethodView).getCreateParams());
    }

    @VisibleForTesting
    void createPaymentMethod(@NonNull Stripe stripe, @Nullable PaymentMethodCreateParams params) {
        if (params == null) {
            return;
        }

        setCommunicatingProgress(true);
        stripe.createPaymentMethod(params,
                new PaymentMethodCallbackImpl(this, mShouldAttachToCustomer));
    }

    private void attachPaymentMethodToCustomer(@NonNull final PaymentMethod paymentMethod) {
        CustomerSession.getInstance()
                .attachPaymentMethod(Objects.requireNonNull(paymentMethod.id),
                        new PaymentMethodRetrievalListenerImpl(this));
    }

    private void finishWithPaymentMethod(@NonNull PaymentMethod paymentMethod) {
        setCommunicatingProgress(false);
        setResult(RESULT_OK, new Intent().putExtra(EXTRA_NEW_PAYMENT_METHOD, paymentMethod));
        finish();
    }

    @Nullable
    IBinder getWindowToken() {
        return getViewStub().getWindowToken();
    }

    @Override
    protected void setCommunicatingProgress(boolean communicating) {
        super.setCommunicatingProgress(communicating);
        if (mAddPaymentMethodView != null) {
            mAddPaymentMethodView.setCommunicatingProgress(communicating);
        }
    }

    private static final class PaymentMethodCallbackImpl
            extends ActivityPaymentMethodCallback<AddPaymentMethodActivity> {

        private final boolean mUpdatesCustomer;

        private PaymentMethodCallbackImpl(@NonNull AddPaymentMethodActivity activity,
                                          boolean updatesCustomer) {
            super(activity);
            mUpdatesCustomer = updatesCustomer;
        }

        @Override
        public void onError(@NonNull Exception error) {
            final AddPaymentMethodActivity activity = getActivity();
            if (activity == null) {
                return;
            }

            activity.setCommunicatingProgress(false);
            // This error is independent of the CustomerSession, so we have to surface it here.
            activity.showError(error.getLocalizedMessage());
        }

        @Override
        public void onSuccess(@NonNull PaymentMethod paymentMethod) {
            final AddPaymentMethodActivity activity = getActivity();
            if (activity == null) {
                return;
            }

            if (mUpdatesCustomer) {
                activity.attachPaymentMethodToCustomer(paymentMethod);
            } else {
                activity.finishWithPaymentMethod(paymentMethod);
            }
        }
    }

    private static final class PaymentMethodRetrievalListenerImpl extends
            CustomerSession.ActivityPaymentMethodRetrievalListener<AddPaymentMethodActivity> {
        private PaymentMethodRetrievalListenerImpl(@NonNull AddPaymentMethodActivity activity) {
            super(activity);
        }

        @Override
        public void onPaymentMethodRetrieved(@NonNull PaymentMethod paymentMethod) {
            final AddPaymentMethodActivity activity = getActivity();
            if (activity == null) {
                return;
            }

            activity.finishWithPaymentMethod(paymentMethod);
        }

        @Override
        public void onError(int errorCode, @NonNull String errorMessage,
                            @Nullable StripeError stripeError) {
            final AddPaymentMethodActivity activity = getActivity();
            if (activity == null) {
                return;
            }

            // No need to show this error, because it will be broadcast
            // from the CustomerSession
            activity.setCommunicatingProgress(false);
        }
    }

    /**
     * Abstract implementation of {@link ApiResultCallback} that holds a {@link WeakReference}
     * to an <code>Activity</code> object.
     */
    private abstract static class ActivityPaymentMethodCallback<A extends Activity>
            implements ApiResultCallback<PaymentMethod> {
        @NonNull private final WeakReference<A> mActivityRef;

        ActivityPaymentMethodCallback(@NonNull A activity) {
            mActivityRef = new WeakReference<>(activity);
        }

        @Nullable
        public A getActivity() {
            return mActivityRef.get();
        }
    }
}
