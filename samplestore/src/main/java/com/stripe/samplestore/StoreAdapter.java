package com.stripe.samplestore;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;


public class StoreAdapter extends RecyclerView.Adapter<StoreAdapter.ViewHolder> {

    static final int[] EMOJI_CLOTHES = {
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

    static final int[] EMOJI_PRICES = {
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

    static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView mEmojiTextView;
        private TextView mPriceTextView;

        ViewHolder(LinearLayout pollingLayout) {
            super(pollingLayout);
            mEmojiTextView = (TextView) pollingLayout.findViewById(R.id.tv_emoji);
            mPriceTextView = (TextView) pollingLayout.findViewById(R.id.tv_price);
        }

        void setEmoji(int emojiUnicode) {
            mEmojiTextView.setText(getEmojiByUnicode(emojiUnicode));
        }

        void setPrice(int priceInPennies) {
            mPriceTextView.setText(getPriceString(priceInPennies));
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 0;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LinearLayout pollingView = (LinearLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.store_item, parent, false);

        ViewHolder vh = new ViewHolder(pollingView);
        return vh;
    }

    static String getEmojiByUnicode(int unicode){
        return new String(Character.toChars(unicode));
    }

    static String getPriceString(int priceInPennies) {
        int cents = priceInPennies % 100;
        int dollars = priceInPennies / 100;
        return String.format(Locale.ENGLISH, "%d.%d", dollars, cents);
    };
}
