package com.stripe.samplestore;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Currency;

public class StoreAdapter extends RecyclerView.Adapter<StoreAdapter.ViewHolder> {

    private static final int[] EMOJI_CLOTHES = {
            0x1F455,
            0x1F456,
            0x1F457,
            0x1F458,
            0x1F459,
            0x1F45A,
            0x1F45B,
            0x1F45C,
            0x1F45D,
            0x1F45E,
            0x1F45F,
            0x1F460,
            0x1F461,
            0x1F462
    };

    private static final int[] EMOJI_PRICES = {
            2000,
            4000,
            3000,
            700,
            600,
            1000,
            2000,
            2500,
            800,
            3000,
            2000,
            5000,
            5500,
            6000
    };

    class ViewHolder extends RecyclerView.ViewHolder {
        private Currency mCurrency;
        private TextView mEmojiTextView;
        private TextView mPriceTextView;

        ViewHolder(final LinearLayout pollingLayout, Currency currency) {
            super(pollingLayout);
            mEmojiTextView = (TextView) pollingLayout.findViewById(R.id.tv_emoji);
            mPriceTextView = (TextView) pollingLayout.findViewById(R.id.tv_price);
            mCurrency = currency;
            pollingLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    v.setPressed(true);
                    launchPurchaseActivityForIndex(getAdapterPosition());
                }
            });
        }

        void setEmoji(int emojiUnicode) {
            mEmojiTextView.setText(StoreUtils.getEmojiByUnicode(emojiUnicode));
        }

        void setPrice(int price) {
            mPriceTextView.setText(StoreUtils.getPriceString(price, mCurrency));
        }
    }

    // Storing an activity here only so we can launch for result
    private Activity mActivity;
    private Currency mCurrency;

    public StoreAdapter(Activity activity) {
        mActivity = activity;
        mCurrency = Currency.getInstance("USD");
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.setEmoji(EMOJI_CLOTHES[position]);
        holder.setPrice(EMOJI_PRICES[position]);
    }

    @Override
    public int getItemCount() {
        return EMOJI_CLOTHES.length;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LinearLayout pollingView = (LinearLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.store_item, parent, false);

        ViewHolder vh = new ViewHolder(pollingView, mCurrency);
        return vh;
    }

    private void launchPurchaseActivityForIndex(int index) {
        Intent paymentLaunchIntent = PaymentActivity.createIntent(
                mActivity,
                EMOJI_CLOTHES[index],
                EMOJI_PRICES[index],
                mCurrency);
        mActivity.startActivityForResult(
                paymentLaunchIntent, StoreActivity.PURCHASE_REQUEST);
    }
}
