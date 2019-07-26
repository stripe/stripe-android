package com.stripe.samplestore;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Size;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.jakewharton.rxbinding2.view.RxView;
import com.stripe.android.ApiResultCallback;
import com.stripe.android.CustomerSession;
import com.stripe.android.PayWithGoogleUtils;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.PaymentIntentResult;
import com.stripe.android.PaymentSession;
import com.stripe.android.PaymentSessionConfig;
import com.stripe.android.PaymentSessionData;
import com.stripe.android.SetupIntentResult;
import com.stripe.android.Stripe;
import com.stripe.android.StripeError;
import com.stripe.android.model.Address;
import com.stripe.android.model.Customer;
import com.stripe.android.model.PaymentIntent;
import com.stripe.android.model.PaymentMethod;
import com.stripe.android.model.SetupIntent;
import com.stripe.android.model.ShippingInformation;
import com.stripe.android.model.ShippingMethod;
import com.stripe.android.model.StripeIntent;
import com.stripe.samplestore.service.StripeService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;

import static com.stripe.android.view.PaymentFlowExtras.EVENT_SHIPPING_INFO_PROCESSED;
import static com.stripe.android.view.PaymentFlowExtras.EVENT_SHIPPING_INFO_SUBMITTED;
import static com.stripe.android.view.PaymentFlowExtras.EXTRA_DEFAULT_SHIPPING_METHOD;
import static com.stripe.android.view.PaymentFlowExtras.EXTRA_IS_SHIPPING_INFO_VALID;
import static com.stripe.android.view.PaymentFlowExtras.EXTRA_SHIPPING_INFO_DATA;
import static com.stripe.android.view.PaymentFlowExtras.EXTRA_VALID_SHIPPING_METHODS;

public class PaymentActivity extends AppCompatActivity {

    private static final String EXTRA_CART = "extra_cart";

    @NonNull private final CompositeDisposable mCompositeDisposable = new CompositeDisposable();

    private BroadcastReceiver mBroadcastReceiver;
    private LinearLayout mCartItemLayout;
    private TextView mEnterShippingInfo;
    private TextView mEnterPaymentInfo;
    private ProgressBar mProgressBar;

    private Button mConfirmPaymentButton;
    private Button mSetupPaymentCredentialsButton;

    private Stripe mStripe;
    private PaymentSession mPaymentSession;
    private StripeService mService;

    private StoreCart mStoreCart;
    private long mShippingCosts = 0L;

    @NonNull
    public static Intent createIntent(@NonNull Activity activity, @NonNull StoreCart cart) {
        return new Intent(activity, PaymentActivity.class)
                .putExtra(EXTRA_CART, cart);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        mStripe = new Stripe(this, PaymentConfiguration.getInstance().getPublishableKey());
        if (Settings.STRIPE_ACCOUNT_ID != null) {
            mStripe.setStripeAccount(Settings.STRIPE_ACCOUNT_ID);
        }

        mService = RetrofitFactory.getInstance().create(StripeService.class);

        final Bundle extras = getIntent().getExtras();
        mStoreCart = extras != null ? extras.getParcelable(EXTRA_CART) : null;

        mProgressBar = findViewById(R.id.progress_bar);
        mCartItemLayout = findViewById(R.id.cart_list_items);
        mConfirmPaymentButton = findViewById(R.id.btn_confirm_payment);
        mSetupPaymentCredentialsButton = findViewById(R.id.btn_setup_intent);

        setupPaymentSession();

        addCartItems();

        updateConfirmPaymentButton();
        mEnterShippingInfo = findViewById(R.id.shipping_info);
        mEnterPaymentInfo = findViewById(R.id.payment_source);
        mCompositeDisposable.add(RxView.clicks(mEnterShippingInfo)
                .subscribe(aVoid -> mPaymentSession.presentShippingFlow()));
        mCompositeDisposable.add(RxView.clicks(mEnterPaymentInfo)
                .subscribe(aVoid -> mPaymentSession.presentPaymentMethodSelection(true)));

        final CustomerSession customerSession = CustomerSession.getInstance();
        mCompositeDisposable.add(RxView.clicks(mConfirmPaymentButton)
                .subscribe(aVoid -> customerSession.retrieveCurrentCustomer(
                        new PaymentIntentCustomerRetrievalListener(PaymentActivity.this))));
        mCompositeDisposable.addAll(RxView.clicks(mSetupPaymentCredentialsButton)
                        .subscribe(aVoid -> customerSession.retrieveCurrentCustomer(
                                new SetupIntentCustomerRetrievalListener(PaymentActivity.this))));
        final LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final ShippingInformation shippingInformation = intent
                        .getParcelableExtra(EXTRA_SHIPPING_INFO_DATA);
                final Intent shippingInfoProcessedIntent =
                        new Intent(EVENT_SHIPPING_INFO_PROCESSED);
                if (!isShippingInfoValid(shippingInformation)) {
                    shippingInfoProcessedIntent.putExtra(EXTRA_IS_SHIPPING_INFO_VALID, false);
                } else {
                    final ArrayList<ShippingMethod> shippingMethods =
                            getValidShippingMethods(shippingInformation);
                    shippingInfoProcessedIntent.putExtra(EXTRA_IS_SHIPPING_INFO_VALID, true);
                    shippingInfoProcessedIntent.putParcelableArrayListExtra(
                            EXTRA_VALID_SHIPPING_METHODS, shippingMethods);
                    shippingInfoProcessedIntent
                            .putExtra(EXTRA_DEFAULT_SHIPPING_METHOD, shippingMethods.get(0));
                }
                localBroadcastManager.sendBroadcast(shippingInfoProcessedIntent);
            }
        };
        localBroadcastManager.registerReceiver(mBroadcastReceiver,
                new IntentFilter(EVENT_SHIPPING_INFO_SUBMITTED));
    }

    private boolean isShippingInfoValid(@NonNull ShippingInformation shippingInfo) {
        return shippingInfo.getAddress() != null &&
                Locale.US.getCountry().equals(shippingInfo.getAddress().getCountry());
    }

    @NonNull
    private ShippingInformation getExampleShippingInfo() {
        final Address address = new Address.Builder()
                .setCity("San Francisco")
                .setCountry("US")
                .setLine1("123 Market St")
                .setLine2("#345")
                .setPostalCode("94107")
                .setState("CA")
                .build();
        return new ShippingInformation(address, "Fake Name", "(555) 555-5555");
    }

    /*
     * Cleaning up all Rx subscriptions in onDestroy.
     */
    @Override
    protected void onDestroy() {
        mCompositeDisposable.dispose();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        mPaymentSession.onDestroy();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        final boolean isPaymentIntentResult = mStripe.onPaymentResult(
                requestCode, data,
                new ApiResultCallback<PaymentIntentResult>() {
                    @Override
                    public void onSuccess(@NonNull PaymentIntentResult result) {
                        stopLoading();
                        processStripeIntent(result.getIntent());
                    }

                    @Override
                    public void onError(@NonNull Exception e) {
                        stopLoading();
                        displayError(e.getMessage());
                    }
                });

        if (isPaymentIntentResult) {
            startLoading();
        } else {
            final boolean isSetupIntentResult = mStripe.onSetupResult(requestCode, data,
                    new ApiResultCallback<SetupIntentResult>() {
                        @Override
                        public void onSuccess(@NonNull SetupIntentResult result) {
                            stopLoading();
                            processStripeIntent(result.getIntent());
                        }

                        @Override
                        public void onError(@NonNull Exception e) {
                            stopLoading();
                            displayError(e.getMessage());
                        }
                    });
            if (!isSetupIntentResult) {
                mPaymentSession.handlePaymentData(requestCode, resultCode, data);
            }
        }
    }

    private void updateConfirmPaymentButton() {
        final long price = mPaymentSession.getPaymentSessionData().getCartTotal();

        mConfirmPaymentButton.setText(
                getString(R.string.pay_label, StoreUtils.getPriceString(price, null)));
    }

    private void addCartItems() {
        mCartItemLayout.removeAllViewsInLayout();
        final String currencySymbol = mStoreCart.getCurrency().getSymbol(Locale.US);

        addLineItems(currencySymbol, mStoreCart.getLineItems()
                .toArray(new StoreLineItem[mStoreCart.getSize()]));

        addLineItems(currencySymbol,
                new StoreLineItem(getString(R.string.checkout_shipping_cost_label), 1,
                        mShippingCosts));

        final View totalView = getLayoutInflater()
                .inflate(R.layout.cart_item, mCartItemLayout, false);
        setupTotalPriceView(totalView, currencySymbol);
        mCartItemLayout.addView(totalView);
    }

    private void addLineItems(@NonNull String currencySymbol, @NonNull StoreLineItem... items) {
        for (StoreLineItem item : items) {
            final View view = getLayoutInflater().inflate(
                    R.layout.cart_item, mCartItemLayout, false);
            fillOutCartItemView(item, view, currencySymbol);
            mCartItemLayout.addView(view);
        }
    }

    private void setupTotalPriceView(@NonNull View view, @NonNull String currencySymbol) {
        final TextView[] itemViews = getItemViews(view);
        final long totalPrice = mPaymentSession.getPaymentSessionData().getCartTotal();
        itemViews[0].setText(getString(R.string.checkout_total_cost_label));
        final String price = PayWithGoogleUtils.getPriceString(totalPrice,
                mStoreCart.getCurrency());
        final String displayPrice = currencySymbol + price;
        itemViews[3].setText(displayPrice);
    }

    private void fillOutCartItemView(@NonNull StoreLineItem item, @NonNull View view,
                                     @NonNull String currencySymbol) {
        final TextView[] itemViews = getItemViews(view);

        itemViews[0].setText(item.getDescription());

        if (!getString(R.string.checkout_shipping_cost_label).equals(item.getDescription())) {
            String quantityPriceString = "X " + item.getQuantity() + " @";
            itemViews[1].setText(quantityPriceString);

            String unitPriceString = currencySymbol +
                    PayWithGoogleUtils.getPriceString(item.getUnitPrice(),
                            mStoreCart.getCurrency());
            itemViews[2].setText(unitPriceString);
        }

        String totalPriceString = currencySymbol +
                PayWithGoogleUtils.getPriceString(item.getTotalPrice(), mStoreCart.getCurrency());
        itemViews[3].setText(totalPriceString);
    }

    @Size(value = 4)
    @NonNull
    private TextView[] getItemViews(@NonNull View view) {
        final TextView labelView = view.findViewById(R.id.tv_cart_emoji);
        final TextView quantityView = view.findViewById(R.id.tv_cart_quantity);
        final TextView unitPriceView = view.findViewById(R.id.tv_cart_unit_price);
        final TextView totalPriceView = view.findViewById(R.id.tv_cart_total_price);
        return new TextView[]{labelView, quantityView, unitPriceView, totalPriceView};
    }

    @NonNull
    private Map<String, Object> createCapturePaymentParams(@NonNull PaymentSessionData data,
                                                           @NonNull String customerId,
                                                           @Nullable String stripeAccountId) {
        final AbstractMap<String, Object> params = new HashMap<>();
        params.put("amount", Long.toString(data.getCartTotal()));
        params.put("payment_method", Objects.requireNonNull(data.getPaymentMethod()).id);
        params.put("customer_id", customerId);
        params.put("shipping", data.getShippingInformation() != null ?
                data.getShippingInformation().toMap() : null);
        params.put("return_url", "stripe://payment-auth-return");
        if (stripeAccountId != null) {
            params.put("stripe_account", stripeAccountId);
        }
        return params;
    }

    @NonNull
    private Map<String, Object> createSetupIntentParams(@NonNull PaymentSessionData data,
                                                        @NonNull String customerId,
                                                        @Nullable String stripeAccountId) {
        final AbstractMap<String, Object> params = new HashMap<>();
        params.put("payment_method", Objects.requireNonNull(data.getPaymentMethod()).id);
        params.put("customer_id", customerId);
        params.put("return_url", "stripe://payment-auth-return");
        if (stripeAccountId != null) {
            params.put("stripe_account", stripeAccountId);
        }
        return params;
    }

    private void capturePayment(@NonNull String customerId) {
        if (mPaymentSession.getPaymentSessionData().getPaymentMethod() == null) {
            displayError("No payment method selected");
            return;
        }

        final Observable<ResponseBody> stripeResponse = mService.capturePayment(
                createCapturePaymentParams(mPaymentSession.getPaymentSessionData(), customerId,
                        Settings.STRIPE_ACCOUNT_ID));
        mCompositeDisposable.add(stripeResponse
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(disposable -> startLoading())
                .doFinally(this::stopLoading)
                .subscribe(this::onStripeIntentClientSecretResponse,
                        throwable -> displayError(throwable.getLocalizedMessage())));
    }

    private void createSetupIntent(@NonNull String customerId) {
        if (mPaymentSession.getPaymentSessionData().getPaymentMethod() == null) {
            displayError("No payment method selected");
            return;
        }

        final Observable<ResponseBody> stripeResponse = mService.createSetupIntent(
                createSetupIntentParams(mPaymentSession.getPaymentSessionData(), customerId,
                        Settings.STRIPE_ACCOUNT_ID));
        mCompositeDisposable.add(stripeResponse
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(disposable -> startLoading())
                .doFinally(this::stopLoading)
                .subscribe(this::onStripeIntentClientSecretResponse,
                        throwable -> displayError(throwable.getLocalizedMessage())));
    }

    private void displayError(@NonNull String errorMessage) {
        final AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle("Error");
        alertDialog.setMessage(errorMessage);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                (dialog, which) -> dialog.dismiss());
        alertDialog.show();
    }

    private void processStripeIntent(@NonNull StripeIntent stripeIntent) {
        if (stripeIntent.requiresAction()) {
            mStripe.authenticatePayment(this,
                    Objects.requireNonNull(stripeIntent.getClientSecret()));
        } else if (stripeIntent.requiresConfirmation()) {
            confirmStripeIntent(Objects.requireNonNull(stripeIntent.getId()),
                    Settings.STRIPE_ACCOUNT_ID);
        } else if (stripeIntent.getStatus() == StripeIntent.Status.Succeeded) {
            if (stripeIntent instanceof PaymentIntent) {
                finishPayment();
            } else if (stripeIntent instanceof SetupIntent) {
                finishSetup();
            }
        } else if (stripeIntent.getStatus() == StripeIntent.Status.RequiresPaymentMethod) {
            // reset payment method and shipping if authentication fails
            setupPaymentSession();
            mEnterPaymentInfo.setText(getString(R.string.add_credit_card));
            mEnterShippingInfo.setText(getString(R.string.add_shipping_details));
        } else {
            displayError("Unhandled Payment Intent Status: " +
                    Objects.requireNonNull(stripeIntent.getStatus()).toString());
        }
    }

    private void confirmStripeIntent(@NonNull String stripeIntentId,
                                     @Nullable String stripeAccountId) {
        final Map<String, Object> params = new HashMap<>();
        params.put("payment_intent_id", stripeIntentId);
        if (stripeAccountId != null) {
            params.put("stripe_account", stripeAccountId);
        }

        mCompositeDisposable.add(mService.confirmPayment(params)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(disposable -> startLoading())
                .doFinally(this::stopLoading)
                .subscribe(
                        this::onStripeIntentClientSecretResponse,
                        throwable -> displayError(throwable.getLocalizedMessage())));
    }

    private void onStripeIntentClientSecretResponse(@NonNull ResponseBody responseBody)
            throws IOException, JSONException {
        final String clientSecret = new JSONObject(responseBody.string()).getString("secret");
        mCompositeDisposable.add(
                Observable
                        .fromCallable(() -> {
                            if (clientSecret.startsWith("pi_")) {
                                return mStripe.retrievePaymentIntentSynchronous(clientSecret);
                            } else if (clientSecret.startsWith("seti_")) {
                                return mStripe.retrieveSetupIntentSynchronous(clientSecret);
                            } else {
                                throw new IllegalArgumentException(
                                        "Invalid client_secret: " + clientSecret);
                            }
                        })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSubscribe(disposable -> startLoading())
                        .doFinally(this::stopLoading)
                        .subscribe(this::processStripeIntent)
        );
    }

    private void finishPayment() {
        mPaymentSession.onCompleted();
        final Intent data = StoreActivity.createPurchaseCompleteIntent(
                mStoreCart.getTotalPrice() + mShippingCosts);
        setResult(RESULT_OK, data);
        finish();
    }

    private void finishSetup() {
        mPaymentSession.onCompleted();
        setResult(RESULT_OK, new Intent().putExtras(new Bundle()));
        finish();
    }

    private void setupPaymentSession() {
        mPaymentSession = new PaymentSession(this);
        mPaymentSession.init(new PaymentSessionListenerImpl(this),
                new PaymentSessionConfig.Builder()
                        .setPrepopulatedShippingInfo(getExampleShippingInfo()).build());
        mPaymentSession.setCartTotal(mStoreCart.getTotalPrice());

        final boolean isPaymentReadyToCharge = mPaymentSession
                .getPaymentSessionData().isPaymentReadyToCharge();
        mConfirmPaymentButton.setEnabled(isPaymentReadyToCharge);
        mSetupPaymentCredentialsButton.setEnabled(isPaymentReadyToCharge);
    }

    private void startLoading() {
        mProgressBar.setVisibility(View.VISIBLE);
        mEnterPaymentInfo.setEnabled(false);
        mEnterShippingInfo.setEnabled(false);

        mConfirmPaymentButton.setTag(mConfirmPaymentButton.isEnabled());
        mConfirmPaymentButton.setEnabled(false);

        mSetupPaymentCredentialsButton.setTag(mSetupPaymentCredentialsButton.isEnabled());
        mSetupPaymentCredentialsButton.setEnabled(false);
    }

    private void stopLoading() {
        mProgressBar.setVisibility(View.INVISIBLE);
        mEnterPaymentInfo.setEnabled(true);
        mEnterShippingInfo.setEnabled(true);

        mConfirmPaymentButton.setEnabled(Boolean.TRUE.equals(mConfirmPaymentButton.getTag()));
        mSetupPaymentCredentialsButton
                .setEnabled(Boolean.TRUE.equals(mSetupPaymentCredentialsButton.getTag()));
    }

    @Nullable
    private String formatSourceDescription(@NonNull PaymentMethod paymentMethod) {
        if (paymentMethod.card != null) {
            return paymentMethod.card.brand + "-" + paymentMethod.card.last4;
        }
        return null;
    }

    @NonNull
    private ArrayList<ShippingMethod> getValidShippingMethods(
            @NonNull ShippingInformation shippingInformation) {
        final ArrayList<ShippingMethod> shippingMethods = new ArrayList<>();
        shippingMethods.add(new ShippingMethod("UPS Ground", "ups-ground",
                "Arrives in 3-5 days", 0, "USD"));
        shippingMethods.add(new ShippingMethod("FedEx", "fedex",
                "Arrives tomorrow", 599, "USD"));
        if (shippingInformation.getAddress() != null &&
                "94110".equals(shippingInformation.getAddress().getPostalCode())) {
            shippingMethods.add(new ShippingMethod("1 Hour Courier", "courier",
                    "Arrives in the next hour", 1099, "USD"));
        }
        return shippingMethods;
    }

    private void onPaymentSessionDataChanged(@NonNull PaymentSessionData data) {
        if (data.getShippingMethod() != null) {
            mEnterShippingInfo.setText(data.getShippingMethod().getLabel());
            mShippingCosts = data.getShippingMethod().getAmount();
            mPaymentSession.setCartTotal(mStoreCart.getTotalPrice() + mShippingCosts);
            addCartItems();
            updateConfirmPaymentButton();
        }

        if (data.getPaymentMethod() != null) {
            mEnterPaymentInfo.setText(formatSourceDescription(data.getPaymentMethod()));
        }

        if (data.isPaymentReadyToCharge()) {
            mConfirmPaymentButton.setEnabled(true);
            mSetupPaymentCredentialsButton.setEnabled(true);
        }
    }

    private static final class PaymentSessionListenerImpl
            extends PaymentSession.ActivityPaymentSessionListener<PaymentActivity> {
        private PaymentSessionListenerImpl(@NonNull PaymentActivity activity) {
            super(activity);
        }

        @Override
        public void onCommunicatingStateChanged(boolean isCommunicating) {
        }

        @Override
        public void onError(int errorCode, @NonNull String errorMessage) {
            final PaymentActivity activity = getListenerActivity();
            if (activity == null) {
                return;
            }

            activity.displayError(errorMessage);
        }

        @Override
        public void onPaymentSessionDataChanged(@NonNull PaymentSessionData data) {
            final PaymentActivity activity = getListenerActivity();
            if (activity == null) {
                return;
            }

            activity.onPaymentSessionDataChanged(data);
        }
    }

    private static final class PaymentIntentCustomerRetrievalListener
            extends CustomerSession.ActivityCustomerRetrievalListener<PaymentActivity> {
        private PaymentIntentCustomerRetrievalListener(@NonNull PaymentActivity activity) {
            super(activity);
        }

        @Override
        public void onCustomerRetrieved(@NonNull Customer customer) {
            final PaymentActivity activity = getActivity();
            if (activity == null) {
                return;
            }

            activity.capturePayment(Objects.requireNonNull(customer.getId()));
        }

        @Override
        public void onError(int httpCode, @NonNull String errorMessage,
                            @Nullable StripeError stripeError) {
            final PaymentActivity activity = getActivity();
            if (activity == null) {
                return;
            }

            activity.displayError("Error getting payment method:. " + errorMessage);
        }
    }

    private static final class SetupIntentCustomerRetrievalListener
            extends CustomerSession.ActivityCustomerRetrievalListener<PaymentActivity> {
        private SetupIntentCustomerRetrievalListener(@NonNull PaymentActivity activity) {
            super(activity);
        }

        @Override
        public void onCustomerRetrieved(@NonNull Customer customer) {
            final PaymentActivity activity = getActivity();
            if (activity == null) {
                return;
            }

            activity.createSetupIntent(Objects.requireNonNull(customer.getId()));
        }

        @Override
        public void onError(int httpCode, @NonNull String errorMessage,
                            @Nullable StripeError stripeError) {
            final PaymentActivity activity = getActivity();
            if (activity == null) {
                return;
            }

            activity.displayError("Error getting payment method:. " + errorMessage);
        }
    }
}
