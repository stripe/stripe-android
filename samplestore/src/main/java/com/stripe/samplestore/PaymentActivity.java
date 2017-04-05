package com.stripe.samplestore;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.jakewharton.rxbinding.view.RxView;
import com.stripe.android.Stripe;
import com.stripe.android.model.Card;
import com.stripe.android.model.SourceParams;
import com.stripe.android.view.CardInputWidget;

import java.util.Currency;

import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;

public class PaymentActivity extends AppCompatActivity {

    private static final String FUNCTIONAL_SOURCE_PUBLISHABLE_KEY =
            "pk_test_vOo1umqsYxSrP5UXfOeL3ecm";

    private static final String EXTRA_EMOJI_INT = "EXTRA_EMOJI_INT";
    private static final String EXTRA_PRICE = "EXTRA_PRICE";
    private static final String EXTRA_CURRENCY = "EXTRA_CURRENCY";

    private CardInputWidget mCardInputWidget;
    private CompositeSubscription mCompositeSubscription;
    private Currency mCurrency;
    private long mPrice;
    private Stripe mStripe;

    public static Intent createIntent(
            @NonNull Context context,
            int emojiUnicode,
            long price,
            @NonNull Currency currency) {
        Intent intent = new Intent(context, PaymentActivity.class);
        intent.putExtra(EXTRA_EMOJI_INT, emojiUnicode);
        intent.putExtra(EXTRA_PRICE, price);
        intent.putExtra(EXTRA_CURRENCY, currency);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        int emojiUnicode = getIntent().getExtras().getInt(EXTRA_EMOJI_INT);
        TextView emojiDisplay = (TextView) findViewById(R.id.tv_emoji_display);
        emojiDisplay.setText(StoreUtils.getEmojiByUnicode(emojiUnicode));

        mPrice = getIntent().getExtras().getLong(EXTRA_PRICE);
        mCurrency = (Currency) getIntent().getExtras().getSerializable(EXTRA_CURRENCY);
        TextView priceDisplay = (TextView) findViewById(R.id.tv_price_display);
        priceDisplay.setText(StoreUtils.getPriceString(mPrice, mCurrency));
        mCompositeSubscription = new CompositeSubscription();

        mCardInputWidget = (CardInputWidget) findViewById(R.id.card_input_widget);

        RxView.clicks(findViewById(R.id.btn_purchase))
                .subscribe(new Action1<Void>() {
                    @Override
                    public void call(Void aVoid) {
                        attemptPurchase();
                    }
                });

        mStripe = new Stripe(this)
                .setDefaultPublishableKey(FUNCTIONAL_SOURCE_PUBLISHABLE_KEY);
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
            return;
        }

        SourceParams cardParams = SourceParams.createCardParams(card);


    }
}
