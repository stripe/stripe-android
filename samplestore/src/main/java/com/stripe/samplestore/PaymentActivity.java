package com.stripe.samplestore;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
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

import com.jakewharton.rxbinding.view.RxView;
import com.stripe.android.CustomerSession;
import com.stripe.android.PayWithGoogleUtils;
import com.stripe.android.PaymentSession;
import com.stripe.android.PaymentSessionConfig;
import com.stripe.android.PaymentSessionData;
import com.stripe.android.StripeError;
import com.stripe.android.model.Customer;
import com.stripe.android.model.CustomerSource;
import com.stripe.android.model.ShippingInformation;
import com.stripe.android.model.ShippingMethod;
import com.stripe.android.model.Source;
import com.stripe.android.model.SourceCardData;
import com.stripe.samplestore.service.StripeService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import retrofit2.Retrofit;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

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

    @NonNull private final CompositeSubscription mCompositeSubscription =
            new CompositeSubscription();

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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        Bundle extras = getIntent().getExtras();
        mStoreCart = extras.getParcelable(EXTRA_CART);

        mCartItemLayout = findViewById(R.id.cart_list_items);

        addCartItems();

        mProgressDialogFragment = ProgressDialogFragment
                .newInstance(getString(R.string.completing_purchase));

        mConfirmPaymentButton = findViewById(R.id.btn_purchase);
        updateConfirmPaymentButton();
        mEnterShippingInfo = findViewById(R.id.shipping_info);
        mEnterPaymentInfo = findViewById(R.id.payment_source);
        RxView.clicks(mEnterShippingInfo)
                .subscribe(new Action1<Void>() {
                    @Override
                    public void call(Void aVoid) {
                        mPaymentSession.presentShippingFlow();
                    }
                });
        RxView.clicks(mEnterPaymentInfo)
                .subscribe(new Action1<Void>() {
                    @Override
                    public void call(Void aVoid) {
                        mPaymentSession.presentPaymentMethodSelection();
                    }
                });
        RxView.clicks(mConfirmPaymentButton)
                .subscribe(new Action1<Void>() {
                    @Override
                    public void call(Void aVoid) {
                        attemptPurchase();
                    }
                });

        setupPaymentSession();

        if (!mPaymentSession.getPaymentSessionData().isPaymentReadyToCharge()) {
            mConfirmPaymentButton.setEnabled(false);
        }

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
                    ArrayList<ShippingMethod> shippingMethods = getValidShippingMethods(shippingInformation);
                    shippingInfoProcessedIntent.putExtra(EXTRA_IS_SHIPPING_INFO_VALID, true);
                    shippingInfoProcessedIntent.putParcelableArrayListExtra(EXTRA_VALID_SHIPPING_METHODS, shippingMethods);
                    shippingInfoProcessedIntent
                            .putExtra(EXTRA_DEFAULT_SHIPPING_METHOD, shippingMethods.get(0));
                }
                LocalBroadcastManager.getInstance(PaymentActivity.this)
                        .sendBroadcast(shippingInfoProcessedIntent);
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver,
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
        super.onDestroy();
        mCompositeSubscription.unsubscribe();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        mPaymentSession.onDestroy();
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
    private TextView[] getItemViews(View view) {
        TextView labelView = view.findViewById(R.id.tv_cart_emoji);
        TextView quantityView = view.findViewById(R.id.tv_cart_quantity);
        TextView unitPriceView = view.findViewById(R.id.tv_cart_unit_price);
        TextView totalPriceView = view.findViewById(R.id.tv_cart_total_price);
        return new TextView[]{labelView, quantityView, unitPriceView, totalPriceView};
    }

    private void attemptPurchase() {
        CustomerSession.getInstance().retrieveCurrentCustomer(new CustomerSession.CustomerRetrievalListener() {
            @Override
            public void onCustomerRetrieved(@NonNull Customer customer) {
                String sourceId = customer.getDefaultSource();
                if (sourceId == null) {
                    displayError("No payment method selected");
                    return;
                }
                CustomerSource source = customer.getSourceById(sourceId);
                proceedWithPurchaseIf3DSCheckIsNotNecessary(source.asSource(), customer.getId());
            }

            @Override
            public void onError(int httpCode, @Nullable String errorMessage,
                                @Nullable StripeError stripeError) {
                displayError("Error getting payment method");
            }
        });

    }

    private void proceedWithPurchaseIf3DSCheckIsNotNecessary(Source source, String customerId) {
        if (source == null || !Source.CARD.equals(source.getType())) {
            displayError("Something went wrong - this should be rare");
            return;
        }

        SourceCardData cardData = (SourceCardData) source.getSourceTypeModel();
        if (SourceCardData.REQUIRED.equals(cardData.getThreeDSecureStatus())) {
            // In this case, you would need to ask the user to verify the purchase.
            // You can see an example of how to do this in the 3DS example application.
            // In stripe-android/example.
        } else {
            // If 3DS is not required, you can charge the source.
            completePurchase(source.getId(), customerId);
        }
    }

    @NonNull
    private Map<String, Object> createParams(long price, String sourceId, String customerId,
                                             ShippingInformation shippingInformation) {
        Map<String, Object> params = new HashMap<>();
        params.put("amount", Long.toString(price));
        params.put("source", sourceId);
        params.put("customer_id", customerId);
        params.put("shipping", shippingInformation.toMap());
        return params;
    }

    private void completePurchase(String sourceId, String customerId) {
        Retrofit retrofit = RetrofitFactory.getInstance();
        StripeService stripeService = retrofit.create(StripeService.class);
        long price = mStoreCart.getTotalPrice() + mShippingCosts;

        ShippingInformation shippingInformation = mPaymentSession.getPaymentSessionData().getShippingInformation();

        final Observable<Void> stripeResponse = stripeService.createQueryCharge(
                createParams(price, sourceId, customerId, shippingInformation));
        final FragmentManager fragmentManager = getSupportFragmentManager();
        mCompositeSubscription.add(stripeResponse
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(
                        new Action0() {
                            @Override
                            public void call() {
                                if (mProgressDialogFragment != null &&
                                        !mProgressDialogFragment.isAdded())
                                    mProgressDialogFragment.show(fragmentManager, "progress");
                            }
                        })
                .doOnUnsubscribe(
                        new Action0() {
                            @Override
                            public void call() {
                                if (mProgressDialogFragment != null
                                        && mProgressDialogFragment.isVisible()) {
                                    mProgressDialogFragment.dismiss();
                                }
                            }
                        })
                .subscribe(
                        new Action1<Void>() {
                            @Override
                            public void call(Void aVoid) {
                                finishCharge();
                            }
                        },
                        new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                displayError(throwable.getLocalizedMessage());
                            }
                        }));
    }

    private void displayError(String errorMessage) {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle("Error");
        alertDialog.setMessage(errorMessage);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }

    private void finishCharge() {
        Intent data = StoreActivity.createPurchaseCompleteIntent(
                mStoreCart.getTotalPrice() + mShippingCosts);
        setResult(RESULT_OK, data);
        finish();
    }

    private void setupPaymentSession() {
        mPaymentSession = new PaymentSession(this);
        mPaymentSession.init(new PaymentSessionListenerImpl(this),
                new PaymentSessionConfig.Builder().build());
    }

    private String formatSourceDescription(Source source) {
        if (Source.CARD.equals(source.getType())) {
            final SourceCardData sourceCardData = (SourceCardData) source.getSourceTypeModel();
            return sourceCardData.getBrand() + getString(R.string.ending_in) +
                    sourceCardData.getLast4();
        }
        return source.getType();
    }

    @NonNull
    private ArrayList<ShippingMethod> getValidShippingMethods(@NonNull ShippingInformation shippingInformation) {
        ArrayList<ShippingMethod> shippingMethods = new ArrayList<>();
        shippingMethods.add(new ShippingMethod("UPS Ground", "ups-ground", "Arrives in 3-5 days", 0, "USD"));
        shippingMethods.add(new ShippingMethod("FedEx", "fedex", "Arrives tomorrow", 599, "USD"));
        if (shippingInformation.getAddress() != null &&
                shippingInformation.getAddress().getPostalCode().equals("94110")) {
            shippingMethods.add(new ShippingMethod("1 Hour Courier", "courier", "Arrives in the next hour", 1099, "USD"));
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
                    new CustomerSession.CustomerRetrievalListener() {
                        @Override
                        public void onCustomerRetrieved(@NonNull Customer customer) {
                            final String sourceId = customer.getDefaultSource();
                            if (sourceId == null) {
                                displayError("No payment method selected");
                                return;
                            }
                            final CustomerSource source = customer.getSourceById(sourceId);
                            mEnterPaymentInfo.setText(formatSourceDescription(source.asSource()));
                        }

                        @Override
                        public void onError(int httpCode, @Nullable String errorMessage,
                                            @Nullable StripeError stripeError) {
                            displayError(errorMessage);
                        }
                    });
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
}
