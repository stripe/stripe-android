package com.stripe.android.view;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.stripe.android.ApiResultCallback;
import com.stripe.android.CustomerSession;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.R;
import com.stripe.android.Stripe;
import com.stripe.android.StripeError;
import com.stripe.android.model.PaymentMethod;
import com.stripe.android.model.PaymentMethodCreateParams;
import com.stripe.android.model.Source;

import java.lang.ref.WeakReference;
import java.util.Objects;

import static com.stripe.android.PaymentSession.TOKEN_PAYMENT_SESSION;

/**
 * Activity used to display a {@link CardMultilineWidget} and receive the resulting
 * {@link Source} in the {@link #onActivityResult(int, int, Intent)} of the launching activity.
 *
 * <p>Can be started with {@link AddPaymentMethodActivityStarter}
 */
public class AddPaymentMethodActivity extends StripeActivity {

    public static final String TOKEN_ADD_PAYMENT_METHOD_ACTIVITY = "AddPaymentMethodActivity";

    public static final String EXTRA_NEW_PAYMENT_METHOD = "new_payment_method";

    @Nullable private CardMultilineWidget mCardMultilineWidget;
    @Nullable private Stripe mStripe;

    private boolean mStartedFromPaymentSession;
    private boolean mUpdatesCustomer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final AddPaymentMethodActivityStarter.Args args =
                AddPaymentMethodActivityStarter.Args.create(getIntent());
        final PaymentConfiguration paymentConfiguration;
        if (args.paymentConfiguration != null) {
            paymentConfiguration = args.paymentConfiguration;
        } else {
            paymentConfiguration = PaymentConfiguration.getInstance();
        }
        mStripe = new Stripe(getApplicationContext(), paymentConfiguration.getPublishableKey());

        mViewStub.setLayoutResource(R.layout.activity_add_source);
        mViewStub.inflate();
        mCardMultilineWidget = findViewById(R.id.add_source_card_entry_widget);
        initEnterListeners(mCardMultilineWidget);
        final boolean shouldShowPostalCode = args.shouldRequirePostalCode;
        mUpdatesCustomer = args.shouldUpdateCustomer;
        mStartedFromPaymentSession = args.isPaymentSessionActive;
        mCardMultilineWidget.setShouldShowPostalCode(shouldShowPostalCode);

        if (mUpdatesCustomer && args.shouldInitCustomerSessionTokens) {
            initCustomerSessionTokens();
        }

        setTitle(R.string.title_add_a_card);
    }

    private void initEnterListeners(@NonNull View cardMultilineWidget) {
        final TextView.OnEditorActionListener listener = new OnEditorActionListenerImpl(this,
                (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE));
        ((TextView) cardMultilineWidget.findViewById(R.id.et_add_source_card_number_ml))
                .setOnEditorActionListener(listener);
        ((TextView) cardMultilineWidget.findViewById(R.id.et_add_source_expiry_ml))
                .setOnEditorActionListener(listener);
        ((TextView) cardMultilineWidget.findViewById(R.id.et_add_source_cvc_ml))
                .setOnEditorActionListener(listener);
        ((TextView) cardMultilineWidget.findViewById(R.id.et_add_source_postal_ml))
                .setOnEditorActionListener(listener);
    }

    @VisibleForTesting
    void initCustomerSessionTokens() {
        logToCustomerSessionIf(TOKEN_ADD_PAYMENT_METHOD_ACTIVITY, mUpdatesCustomer);
        logToCustomerSessionIf(TOKEN_PAYMENT_SESSION, mStartedFromPaymentSession);
    }

    @Override
    protected void onActionSave() {
        if (mCardMultilineWidget == null) {
            return;
        }

        createPaymentMethod(Objects.requireNonNull(mStripe));
    }

    @VisibleForTesting
    void createPaymentMethod(@NonNull Stripe stripe) {
        final PaymentMethodCreateParams.Card card =
                Objects.requireNonNull(mCardMultilineWidget).getPaymentMethodCard();
        final PaymentMethod.BillingDetails billingDetails =
                mCardMultilineWidget.getPaymentMethodBillingDetails();

        if (card == null) {
            // In this case, the error will be displayed on the card widget itself.
            return;
        }

        final PaymentMethodCreateParams paymentMethodCreateParams =
                PaymentMethodCreateParams.create(card, billingDetails);

        setCommunicatingProgress(true);
        stripe.createPaymentMethod(paymentMethodCreateParams,
                new PaymentMethodCallbackImpl(this, mUpdatesCustomer));
    }

    private void attachPaymentMethodToCustomer(@NonNull final PaymentMethod paymentMethod) {
        final CustomerSession.PaymentMethodRetrievalListener listener =
                new PaymentMethodRetrievalListenerImpl(this);

        CustomerSession.getInstance()
                .attachPaymentMethod(Objects.requireNonNull(paymentMethod.id), listener);
    }

    private void logToCustomerSessionIf(@NonNull String logToken, boolean condition) {
        if (condition) {
            CustomerSession.getInstance().addProductUsageTokenIfValid(logToken);
        }
    }

    private void finishWithPaymentMethod(@NonNull PaymentMethod paymentMethod) {
        setCommunicatingProgress(false);
        setResult(RESULT_OK, new Intent().putExtra(EXTRA_NEW_PAYMENT_METHOD, paymentMethod));
        finish();
    }

    boolean hasValidCard() {
        return Objects.requireNonNull(mCardMultilineWidget).getCard() != null;
    }

    @Nullable
    IBinder getWindowToken() {
        return mViewStub.getWindowToken();
    }

    @Override
    protected void setCommunicatingProgress(boolean communicating) {
        super.setCommunicatingProgress(communicating);
        if (mCardMultilineWidget != null) {
            mCardMultilineWidget.setEnabled(!communicating);
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

    @VisibleForTesting
    static final class OnEditorActionListenerImpl
            implements TextView.OnEditorActionListener {
        @NonNull private final AddPaymentMethodActivity mActivity;
        @NonNull private final InputMethodManager mInputMethodManager;

        OnEditorActionListenerImpl(@NonNull AddPaymentMethodActivity activity,
                                   @NonNull InputMethodManager inputMethodManager) {
            mActivity = activity;
            mInputMethodManager = inputMethodManager;

        }

        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if (mActivity.hasValidCard()) {
                    mInputMethodManager.hideSoftInputFromWindow(mActivity.getWindowToken(), 0);
                }
                mActivity.onActionSave();
                return true;
            }
            return false;
        }
    }
}
