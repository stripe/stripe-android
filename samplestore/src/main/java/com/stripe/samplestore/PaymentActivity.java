package com.stripe.samplestore;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.wallet.Cart;
import com.google.android.gms.wallet.LineItem;
import com.jakewharton.rxbinding.view.RxView;
import com.stripe.android.Stripe;
import com.stripe.android.model.Card;
import com.stripe.android.model.Source;
import com.stripe.android.model.SourceCardData;
import com.stripe.android.model.SourceParams;
import com.stripe.android.view.CardInputWidget;
import com.stripe.wrap.pay.AndroidPayConfiguration;
import com.stripe.wrap.pay.activity.StripeAndroidPayActivity;
import com.stripe.wrap.pay.utils.CartManager;

import java.util.Currency;
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

    private static final String EXTRA_CART = "EXTRA_CART";

    private CartManager mCartManager;
    private CardInputWidget mCardInputWidget;
    private CompositeSubscription mCompositeSubscription;
    private ProgressDialogFragment mProgressDialogFragment;
    private ShoppingCartAdapter mShoppingCartAdapter;
    private Stripe mStripe;

    private LinearLayout mCartItemLayout;

    public static Intent createIntent(@NonNull Context context, @NonNull Cart cart) {
        Intent intent = new Intent(context, PaymentActivity.class);
        intent.putExtra(EXTRA_CART, cart);
        return intent;
    }

    @Override
    protected void onAndroidPayAvailable() {

    }

    @Override
    protected void onAndroidPayNotAvailable() {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        Bundle extras = getIntent().getExtras();
        Cart cart = extras.getParcelable(EXTRA_CART);
        mCartManager = new CartManager(cart);

        mCartItemLayout = (LinearLayout) findViewById(R.id.cart_list_items);
//        ItemDivider dividerDecoration = new ItemDivider(this, R.drawable.item_divider);
//        RecyclerView.LayoutManager linearLayoutManager = new LinearLayoutManager(this);
//        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.rv_payment);
////        recyclerView.setHasFixedSize(true);
//        recyclerView.setNestedScrollingEnabled(false);
//        recyclerView.setLayoutManager(linearLayoutManager);
//        recyclerView.addItemDecoration(dividerDecoration);
//        mShoppingCartAdapter = new ShoppingCartAdapter(cart);
//        recyclerView.setAdapter(mShoppingCartAdapter);

        addCartItems();
        mCompositeSubscription = new CompositeSubscription();

        mCardInputWidget = (CardInputWidget) findViewById(R.id.card_input_widget);
        mProgressDialogFragment =
                ProgressDialogFragment.newInstance(R.string.completing_purchase);
        Button payButton = (Button) findViewById(R.id.btn_purchase);
//        Long price = mShoppingCartAdapter.getCartManager().getTotalPrice();

        Long price = mCartManager.getTotalPrice();

        if (price != null) {
            payButton.setText(String.format(Locale.ENGLISH,
                    "Pay %s", StoreUtils.getPriceString(price, null)));
        }

        RxView.clicks(payButton)
                .subscribe(new Action1<Void>() {
                    @Override
                    public void call(Void aVoid) {
                        attemptPurchase();
                    }
                });

        mStripe = new Stripe(this);
    }

    private void addCartItems() {
        mCartItemLayout.removeAllViewsInLayout();
        String currencySymbol = AndroidPayConfiguration.getInstance()
                .getCurrency().getSymbol(Locale.US);
        for (LineItem item : mCartManager.getLineItemsRegular().values()) {
            View view = LayoutInflater.from(this).inflate(
                    R.layout.cart_item, mCartItemLayout, false);
            fillOutCartItemView(item, view, currencySymbol);
            mCartItemLayout.addView(view);
        }
    }

    private void fillOutCartItemView(LineItem item, View view, String currencySymbol) {
        TextView labelView = (TextView) view.findViewById(R.id.tv_cart_emoji);
        TextView quantityView = (TextView) view.findViewById(R.id.tv_cart_quantity);
        TextView unitPriceView = (TextView) view.findViewById(R.id.tv_cart_unit_price);
        TextView totalPriceView = (TextView) view.findViewById(R.id.tv_cart_total_price);
        labelView.setText(item.getDescription());
        if (!TextUtils.isEmpty(item.getQuantity())) {
            String quantityPriceString = "X " + item.getQuantity() + " @";
            quantityView.setText(quantityPriceString);
        }

        if (!TextUtils.isEmpty(item.getUnitPrice())) {
            String unitPriceString = currencySymbol + item.getUnitPrice();
            unitPriceView.setText(unitPriceString);
        }

        if (!TextUtils.isEmpty(item.getTotalPrice())) {
            String totalPriceString = currencySymbol + item.getTotalPrice();
            totalPriceView.setText(totalPriceString);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCompositeSubscription != null) {
            mCompositeSubscription.unsubscribe();
            mCompositeSubscription = null;
        }
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
            completePurchase(source);
        }
    }

    private void completePurchase(Source source) {
        Retrofit retrofit = RetrofitFactory.getInstance();
        StripeService stripeService = retrofit.create(StripeService.class);
//        Long price = mShoppingCartAdapter.getCartManager().getTotalPrice();
        Long price = mCartManager.getTotalPrice();

        if (price == null) {
            return;
        }
        Observable<Void> stripeResponse = stripeService.createQueryCharge(price, source.getId());

        mCompositeSubscription.add(stripeResponse
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnUnsubscribe(
                        new Action0() {
                            @Override
                            public void call() {
                                if (mProgressDialogFragment != null) {
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
//        Long price = mShoppingCartAdapter.getCartManager().getTotalPrice();
        Long price = mCartManager.getTotalPrice();

        if (price == null) {
            return;
        }
        Intent data = StoreActivity.createPurchaseCompleteIntent(0x1F458, price);
        setResult(RESULT_OK, data);
        finish();
    }

    private void dismissKeyboard() {
        InputMethodManager inputManager =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.toggleSoftInput(0, 0);

//        mEmojiView.requestFocus();
    }

}
