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
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.jakewharton.rxbinding2.view.RxView;
import com.stripe.android.CustomerSession;
import com.stripe.android.PayWithGoogleUtils;
import com.stripe.android.PaymentSession;
import com.stripe.android.PaymentSessionConfig;
import com.stripe.android.PaymentSessionData;
import com.stripe.android.StripeError;
import com.stripe.android.model.Customer;
import com.stripe.android.model.CustomerSource;
import com.stripe.android.model.PaymentIntent;
import com.stripe.android.model.ShippingInformation;
import com.stripe.android.model.ShippingMethod;
import com.stripe.android.model.Source;
import com.stripe.android.model.SourceCardData;
import com.stripe.samplestore.service.StripeService;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

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
    private static final String TOTAL_LABEL = "Total:";
    private static final String SHIPPING = "Shipping";

    @NonNull private final CompositeDisposable mCompositeDisposable = new CompositeDisposable();

    private BroadcastReceiver mBroadcastReceiver;
    private ProgressDialogFragment mProgressDialogFragment;

    private LinearLayout mCartItemLayout;

    private Button mConfirmPaymentButton;
    private TextView mEnterShippingInfo;
    private TextView mEnterPaymentInfo;

    private PaymentSession mPaymentSession;

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

        final Bundle extras = getIntent().getExtras();
        mStoreCart = extras != null ? extras.getParcelable(EXTRA_CART) : null;

        mCartItemLayout = findViewById(R.id.cart_list_items);

        addCartItems();

        mProgressDialogFragment = ProgressDialogFragment
                .newInstance(getString(R.string.completing_purchase));

        mConfirmPaymentButton = findViewById(R.id.btn_purchase);
        updateConfirmPaymentButton();
        mEnterShippingInfo = findViewById(R.id.shipping_info);
        mEnterPaymentInfo = findViewById(R.id.payment_source);
        mCompositeDisposable.add(RxView.clicks(mEnterShippingInfo)
                .subscribe(aVoid -> mPaymentSession.presentShippingFlow()));
        mCompositeDisposable.add(RxView.clicks(mEnterPaymentInfo)
                .subscribe(aVoid -> mPaymentSession.presentPaymentMethodSelection()));
        mCompositeDisposable.add(RxView.clicks(mConfirmPaymentButton)
                .subscribe(aVoid -> CustomerSession.getInstance().retrieveCurrentCustomer(
                        new AttemptPurchaseCustomerRetrievalListener(
                                PaymentActivity.this))));

        setupPaymentSession();

        if (!mPaymentSession.getPaymentSessionData().isPaymentReadyToCharge()) {
            mConfirmPaymentButton.setEnabled(false);
        }

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
        mPaymentSession.handlePaymentData(requestCode, resultCode, data);
    }


    private void updateConfirmPaymentButton() {
        long price = mStoreCart.getTotalPrice();

        mConfirmPaymentButton.setText(String.format(Locale.ENGLISH,
                "Pay %s", StoreUtils.getPriceString(price, null)));
    }

    private void addCartItems() {
        mCartItemLayout.removeAllViewsInLayout();
        String currencySymbol = mStoreCart.getCurrency().getSymbol(Locale.US);

        addLineItems(currencySymbol, mStoreCart.getLineItems()
                .toArray(new StoreLineItem[mStoreCart.getSize()]));

        addLineItems(currencySymbol,
                new StoreLineItem(SHIPPING, 1, mShippingCosts));

        View totalView = LayoutInflater.from(this).inflate(
                R.layout.cart_item, mCartItemLayout, false);
        boolean shouldDisplayTotal = fillOutTotalView(totalView, currencySymbol);
        if (shouldDisplayTotal) {
            mCartItemLayout.addView(totalView);
        }
    }

    private void addLineItems(String currencySymbol, StoreLineItem... items) {
        for (StoreLineItem item : items) {
            View view = LayoutInflater.from(this).inflate(
                    R.layout.cart_item, mCartItemLayout, false);
            fillOutCartItemView(item, view, currencySymbol);
            mCartItemLayout.addView(view);
        }
    }

    private boolean fillOutTotalView(View view, String currencySymbol) {
        TextView[] itemViews = getItemViews(view);
        long totalPrice = mStoreCart.getTotalPrice() + mShippingCosts;
        itemViews[0].setText(TOTAL_LABEL);
        String priceString = PayWithGoogleUtils.getPriceString(
                totalPrice,
                mStoreCart.getCurrency());
        priceString = currencySymbol + priceString;
        itemViews[3].setText(priceString);
        return true;
    }

    private void fillOutCartItemView(StoreLineItem item, View view, String currencySymbol) {
        TextView[] itemViews = getItemViews(view);

        itemViews[0].setText(item.getDescription());

        if (!SHIPPING.equals(item.getDescription())) {
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
        TextView labelView = view.findViewById(R.id.tv_cart_emoji);
        TextView quantityView = view.findViewById(R.id.tv_cart_quantity);
        TextView unitPriceView = view.findViewById(R.id.tv_cart_unit_price);
        TextView totalPriceView = view.findViewById(R.id.tv_cart_total_price);
        return new TextView[]{labelView, quantityView, unitPriceView, totalPriceView};
    }

    @NonNull
    private Map<String, Object> createParams(long price,
                                             @Nullable String sourceId,
                                             @Nullable String customerId,
                                             @Nullable ShippingInformation shippingInformation) {
        final AbstractMap<String, Object> params = new HashMap<>();
        params.put("amount", Long.toString(price));
        params.put("source", sourceId);
        params.put("customer_id", customerId);
        params.put("shipping", shippingInformation != null ? shippingInformation.toMap() : null);
        params.put("return_url", "stripe://payment-auth-return");
        return params;
    }

    private void capturePayment(@Nullable String sourceId, @Nullable String customerId) {
        final StripeService stripeService = RetrofitFactory.getInstance()
                .create(StripeService.class);
        final long price = mStoreCart.getTotalPrice() + mShippingCosts;

        final ShippingInformation shippingInformation = mPaymentSession.getPaymentSessionData()
                .getShippingInformation();

        final Observable<ResponseBody> stripeResponse = stripeService.capturePayment(
                createParams(price, sourceId, customerId, shippingInformation));
        final FragmentManager fragmentManager = getSupportFragmentManager();
        mCompositeDisposable.add(stripeResponse
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(disposable -> {
                    if (mProgressDialogFragment != null && !mProgressDialogFragment.isAdded()) {
                        mProgressDialogFragment.show(fragmentManager, "progress");
                    }
                })
                .doOnDispose(() -> {
                    if (mProgressDialogFragment != null &&
                            mProgressDialogFragment.isVisible()) {
                        mProgressDialogFragment.dismiss();
                    }
                })
                .subscribe(
                        response -> handlePaymentIntentCapture(response.string()),
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

    private void handlePaymentIntentCapture(@NonNull String responseBody) {
        final PaymentIntent paymentIntent = PaymentIntent.fromString(responseBody);
        if (paymentIntent == null) {
            Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (paymentIntent.requiresAction()) {
            startActivity(new Intent(Intent.ACTION_VIEW, paymentIntent.getRedirectUrl()));
            return;
        }

        final Intent data = StoreActivity.createPurchaseCompleteIntent(
                mStoreCart.getTotalPrice() + mShippingCosts);
        setResult(RESULT_OK, data);
        finish();
    }

    private void setupPaymentSession() {
        mPaymentSession = new PaymentSession(this);
        mPaymentSession.init(new PaymentSessionListenerImpl(this),
                new PaymentSessionConfig.Builder().build());
    }

    @Nullable
    private String formatSourceDescription(@NonNull Source source) {
        if (Source.CARD.equals(source.getType())) {
            final SourceCardData sourceCardData = (SourceCardData) source.getSourceTypeModel();
            if (sourceCardData != null) {
                return sourceCardData.getBrand() + getString(R.string.ending_in) +
                        sourceCardData.getLast4();
            }
        }
        return source.getType();
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

    private void onCommunicatingStateChanged(boolean isCommunicating) {
        if (isCommunicating) {
            mProgressDialogFragment.show(getSupportFragmentManager(), "progress");
        } else {
            mProgressDialogFragment.dismiss();
        }
    }

    private void onPaymentSessionDataChanged(@NonNull PaymentSessionData data) {
        if (data.getShippingMethod() != null) {
            mEnterShippingInfo.setText(data.getShippingMethod().getLabel());
            mShippingCosts = data.getShippingMethod().getAmount();
            addCartItems();
            updateConfirmPaymentButton();
        }

        if (data.getSelectedPaymentMethodId() != null) {
            CustomerSession.getInstance().retrieveCurrentCustomer(
                    new PaymentSessionChangedCustomerRetrievalListener(this));
        }

        if (data.isPaymentReadyToCharge()) {
            mConfirmPaymentButton.setEnabled(true);
        }
    }

    private static final class PaymentSessionListenerImpl
            extends PaymentSession.ActivityPaymentSessionListener<PaymentActivity> {
        private PaymentSessionListenerImpl(@NonNull PaymentActivity activity) {
            super(activity);
        }

        @Override
        public void onCommunicatingStateChanged(boolean isCommunicating) {
            final PaymentActivity activity = getListenerActivity();
            if (activity == null) {
                return;
            }

            activity.onCommunicatingStateChanged(isCommunicating);
        }

        @Override
        public void onError(int errorCode, @Nullable String errorMessage) {
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

    private static final class PaymentSessionChangedCustomerRetrievalListener
            extends CustomerSession.ActivityCustomerRetrievalListener<PaymentActivity> {
        private PaymentSessionChangedCustomerRetrievalListener(@NonNull PaymentActivity activity) {
            super(activity);
        }

        @Override
        public void onCustomerRetrieved(@NonNull Customer customer) {
            final PaymentActivity activity = getActivity();
            if (activity == null) {
                return;
            }

            final String sourceId = customer.getDefaultSource();
            if (sourceId == null) {
                activity.displayError("No payment method selected");
                return;
            }

            final CustomerSource customerSource = customer.getSourceById(sourceId);
            final Source source = customerSource != null ? customerSource.asSource() : null;
            if (source != null) {
                activity.mEnterPaymentInfo.setText(activity.formatSourceDescription(source));
            }
        }

        @Override
        public void onError(int httpCode, @Nullable String errorMessage,
                            @Nullable StripeError stripeError) {
            final PaymentActivity activity = getActivity();
            if (activity == null) {
                return;
            }

            activity.displayError(errorMessage);
        }
    }

    private static final class AttemptPurchaseCustomerRetrievalListener
            extends CustomerSession.ActivityCustomerRetrievalListener<PaymentActivity> {
        private AttemptPurchaseCustomerRetrievalListener(@NonNull PaymentActivity activity) {
            super(activity);
        }

        @Override
        public void onCustomerRetrieved(@NonNull Customer customer) {
            final PaymentActivity activity = getActivity();
            if (activity == null) {
                return;
            }

            final String sourceId = customer.getDefaultSource();
            if (sourceId == null) {
                activity.displayError("No payment method selected");
                return;
            }

            final CustomerSource customerSource = customer.getSourceById(sourceId);
            final Source source = customerSource != null ? customerSource.asSource() : null;

            if (source == null || !Source.CARD.equals(source.getType())) {
                activity.displayError("Something went wrong - this should be rare");
                return;
            }

            activity.capturePayment(source.getId(), customer.getId());
        }

        @Override
        public void onError(int httpCode, @Nullable String errorMessage,
                            @Nullable StripeError stripeError) {
            final PaymentActivity activity = getActivity();
            if (activity == null) {
                return;
            }

            activity.displayError("Error getting payment method");
        }
    }
}
