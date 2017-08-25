package com.stripe.samplestore;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Size;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.identity.intents.model.UserAddress;
import com.google.android.gms.wallet.Cart;
import com.google.android.gms.wallet.FullWallet;
import com.google.android.gms.wallet.FullWalletRequest;
import com.google.android.gms.wallet.LineItem;
import com.google.android.gms.wallet.MaskedWallet;
import com.google.android.gms.wallet.fragment.SupportWalletFragment;
import com.google.android.gms.wallet.fragment.WalletFragmentStyle;
import com.jakewharton.rxbinding.view.RxView;
import com.stripe.android.Stripe;
import com.stripe.android.model.Card;
import com.stripe.android.model.Source;
import com.stripe.android.model.SourceCardData;
import com.stripe.android.model.SourceParams;
import com.stripe.android.model.StripePaymentSource;
import com.stripe.android.view.CardInputWidget;
import com.stripe.wrap.pay.AndroidPayConfiguration;
import com.stripe.wrap.pay.activity.StripeAndroidPayActivity;
import com.stripe.wrap.pay.utils.CartContentException;
import com.stripe.wrap.pay.utils.CartManager;
import com.stripe.wrap.pay.utils.PaymentUtils;

import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.Callable;

import retrofit2.Retrofit;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class PaymentActivity extends StripeAndroidPayActivity {

    private static final String TOTAL_LABEL = "Total:";
    private static final Locale LOC = Locale.US;

    private CartManager mCartManager;
    private CardInputWidget mCardInputWidget;
    private CompositeSubscription mCompositeSubscription;
    private ProgressDialogFragment mProgressDialogFragment;
    private Stripe mStripe;

    private View mAndroidPayGroupContainer;
    private View mAndroidPayDetailsContainer;
    private View mAndroidPayButtonContainer;
    private View mAndroidPayChangeDetailsContainer;

    private TextView mTvAndroidPayCard;
    private TextView mTvAndroidPayAddress;
    private TextView mTvOr;

    private LinearLayout mCartItemLayout;

    private String mCurrentShippingKey;
    private Button mConfirmPaymentButton;

    public static Intent createIntent(@NonNull Context context, @NonNull Cart cart) {
        Intent intent = new Intent(context, PaymentActivity.class);
        intent.putExtra(StripeAndroidPayActivity.EXTRA_CART, cart);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        Bundle extras = getIntent().getExtras();
        Cart cart = extras.getParcelable(EXTRA_CART);
        mCartManager = new CartManager(cart);

        mCartItemLayout = (LinearLayout) findViewById(R.id.cart_list_items);

        addCartItems();
        mCompositeSubscription = new CompositeSubscription();

        mCardInputWidget = (CardInputWidget) findViewById(R.id.card_input_widget);
        mProgressDialogFragment =
                ProgressDialogFragment.newInstance(R.string.completing_purchase);

        mAndroidPayGroupContainer = findViewById(R.id.group_android_pay);
        mAndroidPayDetailsContainer = findViewById(R.id.group_android_pay_details);
        mAndroidPayButtonContainer = findViewById(R.id.android_pay_button_container);
        mAndroidPayChangeDetailsContainer = findViewById(R.id.android_pay_change_container);

        Button cancelChangeDetails = (Button) findViewById(R.id.btn_android_pay_change_cancel);
        RxView.clicks(cancelChangeDetails).subscribe(new Action1<Void>() {
            @Override
            public void call(Void aVoid) {
                mAndroidPayChangeDetailsContainer.setVisibility(View.GONE);
                mAndroidPayDetailsContainer.setVisibility(View.VISIBLE);
            }
        });

        RxView.clicks(findViewById(R.id.btn_android_pay_confirm)).subscribe(new Action1<Void>() {
            @Override
            public void call(Void aVoid) {
                attemptPurchaseWithAndroidPay();
            }
        });
        mTvAndroidPayAddress = (TextView) findViewById(R.id.tv_android_pay_address);
        mTvAndroidPayCard = (TextView) findViewById(R.id.tv_android_pay_card);
        mTvOr = (TextView) findViewById(R.id.tv_cart_or);

        mConfirmPaymentButton = (Button) findViewById(R.id.btn_purchase);
        updateConfirmPaymentButton();

        RxView.clicks(mConfirmPaymentButton)
                .subscribe(new Action1<Void>() {
                    @Override
                    public void call(Void aVoid) {
                        attemptPurchase();
                    }
                });

        RxView.clicks(findViewById(R.id.btn_android_pay_change))
                .subscribe(new Action1<Void>() {
                    @Override
                    public void call(Void aVoid) {
                        if (getMaskedWallet() != null) {
                            createAndAddConfirmationWalletFragment(getMaskedWallet());
                        }
                    }
                });

        mStripe = new Stripe(this);
        mAndroidPayDetailsContainer.setVisibility(View.GONE);
        mAndroidPayChangeDetailsContainer.setVisibility(View.GONE);
    }

    /*
     * This is where you would handle the various google api errors.
     * @param errorCode the error code returned from the {@link GoogleApiClient}
     */
    @Override
    protected void handleError(int errorCode) {
        super.handleError(errorCode);
    }

    /*
     * Make sure your Android Pay container is visible in this method.
     */
    @Override
    protected void onAndroidPayAvailable() {
        createAndAddBuyButtonWalletFragment();
        mAndroidPayGroupContainer.setVisibility(View.VISIBLE);
        mTvOr.setVisibility(View.VISIBLE);
        mTvOr.requestFocus();
    }

    /*
     * Here is where you want to turn off your Android Pay options if the user does not have that
     * on their phone. You can show a button to enable Android Pay if you so choose.
     */
    @Override
    protected void onAndroidPayNotAvailable() {
        mAndroidPayGroupContainer.setVisibility(View.GONE);
        mTvOr.setVisibility(View.GONE);
    }

    /*
     * Here is where you tell your activity where to display the Buy Button Fragment. You can
     * override this to be a no-op or ignore this method entirely if you don't intend to show
     * the buy button.
     */
    @Override
    protected void addBuyButtonWalletFragment(@NonNull SupportWalletFragment walletFragment) {
        FragmentTransaction buttonTransaction = getSupportFragmentManager().beginTransaction();
        buttonTransaction.add(R.id.android_pay_button_container, walletFragment).commit();
    }

    /*
     * Here is where you tell your activity where to display the Confirmation Fragment. You can
     * ignore this method entirely if you don't intend to show the change details / confirmation
     * fragment with this activity.
     */
    @Override
    protected void addConfirmationWalletFragment(@NonNull SupportWalletFragment walletFragment) {
        mAndroidPayChangeDetailsContainer.setVisibility(View.VISIBLE);
        mAndroidPayDetailsContainer.setVisibility(View.GONE);
        FragmentTransaction confirmationTransaction =
                getSupportFragmentManager().beginTransaction();
        confirmationTransaction.replace(R.id.android_pay_confirmation_container, walletFragment)
                .commit();
    }

    /*
     * A default confirmation wallet fragment style is provided by the API, but you can change
     * the style by overriding this method.
     */
    @NonNull
    @Override
    protected WalletFragmentStyle getWalletFragmentConfirmationStyle() {
        return new WalletFragmentStyle()
                .setMaskedWalletDetailsLogoImageType(WalletFragmentStyle.LogoImageType.ANDROID_PAY)
                .setStyleResourceId(R.style.AppTheme)
                .setMaskedWalletDetailsTextAppearance(
                        android.R.style.TextAppearance_DeviceDefault_Medium)
                .setMaskedWalletDetailsHeaderTextAppearance(
                        android.R.style.TextAppearance_DeviceDefault_Large);
    }

    /*
     * This method is called when a masked wallet is first retrieved. You can use the shipping
     * information contained here to calculate shipping costs and taxes.
     */
    @Override
    protected void onMaskedWalletRetrieved(@Nullable MaskedWallet maskedWallet) {
        super.onMaskedWalletRetrieved(maskedWallet);
        if (maskedWallet == null) {
            return;
        }

        mAndroidPayButtonContainer.setVisibility(View.GONE);

        updatePaymentInformation(maskedWallet);
        updateShippingAndTax(maskedWallet);

        try {
            setCart(mCartManager.buildCart());
            updateCartTotals();
            updateConfirmPaymentButton();
        } catch (CartContentException unexpected) {
            // ignore for now
        }
    }

    /*
     * This method is called when a masked wallet update is retrieved from the confirmation
     * fragment. Note that if this method has a null argument, that indicates that no changes were
     * made, not that the wallet is now null.
     */
    @Override
    protected void onChangedMaskedWalletRetrieved(@Nullable MaskedWallet maskedWallet) {
        super.onChangedMaskedWalletRetrieved(maskedWallet);
        mAndroidPayChangeDetailsContainer.setVisibility(View.GONE);
        mAndroidPayDetailsContainer.setVisibility(View.VISIBLE);

        if (maskedWallet == null) {
            return;
        }

        updatePaymentInformation(maskedWallet);
        updateShippingAndTax(maskedWallet);

        try {
            setCart(mCartManager.buildCart());
            updateCartTotals();
            updateConfirmPaymentButton();
        } catch (CartContentException unexpected) {
            // ignore for now
        }
    }

    /**
     * This is where the chargeable Stripe object is returned. You can send the ID of the
     * {@link StripePaymentSource} to your server to make a charge.
     *
     * @param wallet the {@link FullWallet} returned from Google.
     * @param paymentSource the {@link StripePaymentSource} with chargeable ID
     */
    @Override
    protected void onStripePaymentSourceReturned(FullWallet wallet,
                                                 StripePaymentSource paymentSource) {
        super.onStripePaymentSourceReturned(wallet, paymentSource);
        completePurchase(paymentSource.getId());
    }

    /*
     * Cleaning up all Rx subscriptions in onDestroy.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCompositeSubscription != null) {
            mCompositeSubscription.unsubscribe();
            mCompositeSubscription = null;
        }
    }

    private void updatePaymentInformation(@NonNull MaskedWallet maskedWallet) {
        mAndroidPayDetailsContainer.setVisibility(View.VISIBLE);
        if (maskedWallet.getPaymentDescriptions() != null
                && maskedWallet.getPaymentDescriptions().length > 0) {
            String cardText = String.format(LOC, "Card: %s", maskedWallet.getPaymentDescriptions()[0]);
            mTvAndroidPayCard.setText(cardText);
        }

        if (maskedWallet.getBuyerShippingAddress() != null) {
            mTvAndroidPayAddress.setText(maskedWallet.getBuyerShippingAddress().getAddress1());
        }
    }

    private void updateShippingAndTax(@NonNull MaskedWallet maskedWallet) {
        UserAddress shippingAddress = maskedWallet.getBuyerShippingAddress();
        UserAddress billingAddress = maskedWallet.getBuyerBillingAddress();
        if (mCurrentShippingKey != null) {
            mCartManager.removeLineItem(mCurrentShippingKey);
            mCurrentShippingKey = null;
        }

        mCurrentShippingKey = mCartManager.addShippingLineItem("Shipping", determineShippingCost(shippingAddress));
        mCartManager.setTaxLineItem("Tax", determineTax(billingAddress));
    }

    private void updateCartTotals() {
        addCartItems();
    }

    private void updateConfirmPaymentButton() {
        Long price = mCartManager.getTotalPrice();

        if (price != null) {
            mConfirmPaymentButton.setText(String.format(Locale.ENGLISH,
                    "Pay %s", StoreUtils.getPriceString(price, null)));
        }
    }

    private void addCartItems() {
        mCartItemLayout.removeAllViewsInLayout();
        String currencySymbol = AndroidPayConfiguration.getInstance()
                .getCurrency().getSymbol(Locale.US);

        Collection<LineItem> items = mCartManager.getLineItemsRegular().values();
        addLineItems(currencySymbol, items.toArray(new LineItem[items.size()]));

        items = mCartManager.getLineItemsShipping().values();
        addLineItems(currencySymbol, items.toArray(new LineItem[items.size()]));

        if (mCartManager.getLineItemTax() != null) {
            addLineItems(currencySymbol, mCartManager.getLineItemTax());
        }

        View totalView = LayoutInflater.from(this).inflate(
                R.layout.cart_item, mCartItemLayout, false);
        boolean shouldDisplayTotal = fillOutTotalView(totalView, currencySymbol);
        if (shouldDisplayTotal) {
            mCartItemLayout.addView(totalView);
        }
    }

    private void addLineItems(String currencySymbol, LineItem... items) {
        for (LineItem item : items) {
            View view = LayoutInflater.from(this).inflate(
                    R.layout.cart_item, mCartItemLayout, false);
            fillOutCartItemView(item, view, currencySymbol);
            mCartItemLayout.addView(view);
        }
    }

    /**
     * This is just a toy way to determine a fake shipping cost. You would apply
     * your genuine costs here.
     *
     * @param address the {@link UserAddress} object returned from Android Pay
     * @return a shipping cost in the currency used, in its lowest denomination
     */
    private long determineShippingCost(UserAddress address) {
        if (address == null) {
            return 200L;
        }
        return address.getAddress1().length() * 7L;
    }

    /**
     * Again, this is a toy way to determine a fake tax amount. You may need to determine
     * taxes based on the shipping address, billing address, cost of the
     * {@link LineItem.Role#REGULAR} items in your cart, or some combination of the three.
     *
     * @param address the {@link UserAddress} object returned from Android Pay
     * @return a tax amount in the currency used, in its lowest denomination
     */
    private long determineTax(UserAddress address) {
        if (address == null) {
            return 200L;
        }
        return address.getAddress1().length() * 3L;
    }

    private boolean fillOutTotalView(View view, String currencySymbol) {
        TextView[] itemViews = getItemViews(view);
        Long totalPrice = mCartManager.getTotalPrice();
        if (totalPrice != null) {
            itemViews[0].setText(TOTAL_LABEL);
            String priceString = PaymentUtils.getPriceString(totalPrice,
                    AndroidPayConfiguration.getInstance().getCurrency());
            priceString = currencySymbol + priceString;
            itemViews[3].setText(priceString);
            return true;
        }
        return false;
    }

    private void fillOutCartItemView(LineItem item, View view, String currencySymbol) {
        TextView[] itemViews = getItemViews(view);

        itemViews[0].setText(item.getDescription());
        if (!TextUtils.isEmpty(item.getQuantity())) {
            String quantityPriceString = "X " + item.getQuantity() + " @";
            itemViews[1].setText(quantityPriceString);
        }

        if (!TextUtils.isEmpty(item.getUnitPrice())) {
            String unitPriceString = currencySymbol + item.getUnitPrice();
            itemViews[2].setText(unitPriceString);
        }

        if (!TextUtils.isEmpty(item.getTotalPrice())) {
            String totalPriceString = currencySymbol + item.getTotalPrice();
            itemViews[3].setText(totalPriceString);
        }
    }

    @Size(value = 4)
    private TextView[] getItemViews(View view) {
        TextView labelView = (TextView) view.findViewById(R.id.tv_cart_emoji);
        TextView quantityView = (TextView) view.findViewById(R.id.tv_cart_quantity);
        TextView unitPriceView = (TextView) view.findViewById(R.id.tv_cart_unit_price);
        TextView totalPriceView = (TextView) view.findViewById(R.id.tv_cart_total_price);
        TextView[] itemViews = { labelView, quantityView, unitPriceView, totalPriceView };
        return itemViews;
    }

    private void attemptPurchaseWithAndroidPay() {
        MaskedWallet maskedWallet = getMaskedWallet();
        Cart cart = getCart();
        if (maskedWallet == null || cart == null) {
            return;
        }

        FullWalletRequest fullWalletRequest = AndroidPayConfiguration.generateFullWalletRequest(
                maskedWallet.getGoogleTransactionId(), cart);
        loadFullWallet(fullWalletRequest);
    }

    private void attemptPurchase() {
        Card card = mCardInputWidget.getCard();
        if (card == null) {
            displayError("Card Input Error");
            return;
        }
        dismissKeyboard();

        final SourceParams cardParams = SourceParams.createCardParams(card);
        Observable<Source> cardSourceObservable =
                Observable.fromCallable(new Callable<Source>() {
                    @Override
                    public Source call() throws Exception {
                        return mStripe.createSourceSynchronous(
                                cardParams,
                                AndroidPayConfiguration.getInstance().getPublicApiKey());
                    }
                });

        final FragmentManager fragmentManager = this.getSupportFragmentManager();
        mCompositeSubscription.add(cardSourceObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(
                        new Action0() {
                            @Override
                            public void call() {
                                mProgressDialogFragment.show(fragmentManager, "progress");
                            }
                        })
                .subscribe(
                        new Action1<Source>() {
                            @Override
                            public void call(Source source) {
                                proceedWithPurchaseIf3DSCheckIsNotNecessary(source);
                            }
                        },
                        new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                if (mProgressDialogFragment != null) {
                                    mProgressDialogFragment.dismiss();
                                }
                                displayError(throwable.getLocalizedMessage());
                            }
                        }));
    }

    private void proceedWithPurchaseIf3DSCheckIsNotNecessary(Source source) {
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
            completePurchase(source.getId());
        }
    }

    private void completePurchase(String sourceId) {
        Retrofit retrofit = RetrofitFactory.getInstance();
        StripeService stripeService = retrofit.create(StripeService.class);
        Long price = mCartManager.getTotalPrice();

        if (price == null) {
            // This should be rare, and only occur if there is somehow a mix of currencies in
            // the CartManager (only possible if those are put in as LineItem objects manually).
            // If this is the case, you can put in a cart total price manually by calling
            // CartManager.setTotalPrice.
            return;
        }

        Observable<Void> stripeResponse = stripeService.createQueryCharge(price, sourceId);
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
        Long price = mCartManager.getTotalPrice();

        if (price == null) {
            return;
        }

        Intent data = StoreActivity.createPurchaseCompleteIntent(price);
        setResult(RESULT_OK, data);
        finish();
    }

    private void dismissKeyboard() {
        InputMethodManager inputManager =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.toggleSoftInput(0, 0);
    }

}
